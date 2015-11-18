package org.hisp.dhis.dxf2.datavalueset;

/*
 * Copyright (c) 2004-2015, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.common.IdentifiableProperty.UUID;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;
import static org.hisp.dhis.system.util.DateUtils.getDefaultDate;
import static org.hisp.dhis.system.util.DateUtils.parseDate;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.amplecode.quick.BatchHandler;
import org.amplecode.quick.BatchHandlerFactory;
import org.amplecode.staxwax.factory.XMLFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.dxf2.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.common.JacksonUtils;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.pdfform.PdfDataEntryFormUtil;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.Setting;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.callable.CategoryOptionComboAclCallable;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.system.callable.PeriodCallable;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.csvreader.CsvReader;

/**
 * @author Lars Helge Overland
 */
public class DefaultDataValueSetService
    implements DataValueSetService
{
    private static final Log log = LogFactory.getLog( DefaultDataValueSetService.class );

    private static final String ERROR_OBJECT_NEEDED_TO_COMPLETE = "Must be provided to complete data set";

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;
    
    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private DataValueSetStore dataValueSetStore;
    
    @Autowired
    private SystemSettingManager systemSettingManager;
    
    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private Notifier notifier;

    // Set methods for test purposes
    
    public void setBatchHandlerFactory( BatchHandlerFactory batchHandlerFactory )
    {
        this.batchHandlerFactory = batchHandlerFactory;
    }

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    //--------------------------------------------------------------------------
    // DataValueSet implementation
    //--------------------------------------------------------------------------

    @Override
    public DataExportParams getFromUrl( Set<String> dataSets, Set<String> periods, Date startDate, Date endDate, 
        Set<String> organisationUnits, boolean includeChildren, Date lastUpdated, Integer limit, IdSchemes idSchemes )
    {
        DataExportParams params = new DataExportParams();
        
        if ( dataSets != null )
        {
            params.getDataSets().addAll( identifiableObjectManager.getByUid( DataSet.class, dataSets ) );
        }
        
        if ( periods != null && !periods.isEmpty() )
        {
            params.getPeriods().addAll( periodService.reloadIsoPeriods( new ArrayList<>( periods ) ) );
        }
        else if ( startDate != null && endDate != null )
        {
            params.setStartDate( startDate );
            params.setEndDate( endDate );
        }
        
        if ( organisationUnits != null )
        {
            params.getOrganisationUnits().addAll( identifiableObjectManager.getByUid( OrganisationUnit.class, organisationUnits ) );
        }

        params.setIncludeChildren( includeChildren );
        params.setLastUpdated( lastUpdated );
        params.setLimit( limit );
        params.setIdSchemes( idSchemes );
        
        return params;
    }
    
    @Override
    public void validate( DataExportParams params )
    {
        String violation = null;
        
        if ( params == null )
        {
            throw new IllegalArgumentException( "Params cannot be null" );
        }
        
        if ( params.getDataSets().isEmpty() )
        {
            violation = "At least one valid data set must be specified";
        }
        
        if ( params.getPeriods().isEmpty() && !params.hasStartEndDate() ) 
        {
            violation = "At least one valid period or start/end dates must be specified";
        }
        
        if ( params.hasStartEndDate() && params.getStartDate().after( params.getEndDate() ) )
        {
            violation = "Start date must be before end date";
        }
        
        if ( params.getOrganisationUnits().isEmpty() )
        {
            violation = "At least one valid organisation unit must be specified";
        }
        
        if ( params.hasLimit() && params.getLimit() < 0 )
        {
            violation = "Limit cannot be less than zero: " + params.getLimit();
        }
        
        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );
            
            throw new IllegalArgumentException( violation );
        }
    }

    @Override
    public void decideAccess( DataExportParams params )
    {
        for ( OrganisationUnit unit : params.getOrganisationUnits() )
        {
            if ( !organisationUnitService.isInUserHierarchy( unit ) )
            {
                throw new IllegalQueryException( "User is not allowed to view org unit: " + unit.getUid() );
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // Write
    //--------------------------------------------------------------------------

    @Override
    public void writeDataValueSetXml( DataExportParams params, OutputStream out )
    {
        decideAccess( params );
        validate( params );

        dataValueSetStore.writeDataValueSetXml( params, getCompleteDate( params ), out );
    }
    
    @Override
    public void writeDataValueSetJson( DataExportParams params, OutputStream out )
    {
        decideAccess( params );
        validate( params );

        dataValueSetStore.writeDataValueSetJson( params, getCompleteDate( params ), out );
    }

    @Override
    public void writeDataValueSetJson( Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes )
    {        
        dataValueSetStore.writeDataValueSetJson( lastUpdated, outputStream, idSchemes );
    }

    @Override
    public void writeDataValueSetCsv( DataExportParams params, Writer writer )
    {
        decideAccess( params );
        validate( params );
        
        dataValueSetStore.writeDataValueSetCsv( params, getCompleteDate( params ), writer );
    }

    private Date getCompleteDate( DataExportParams params )
    {
        if ( params.isSingleDataValueSet() )
        {
            DataElementCategoryOptionCombo optionCombo = categoryService.getDefaultDataElementCategoryOptionCombo(); //TODO
    
            CompleteDataSetRegistration registration = registrationService
                .getCompleteDataSetRegistration( params.getFirstDataSet(), params.getFirstPeriod(), params.getFirstOrganisationUnit(), optionCombo );
    
            return registration != null ? registration.getDate() : null;
        }
        
        return null;
    }

    //--------------------------------------------------------------------------
    // Template
    //--------------------------------------------------------------------------

    @Override
    public RootNode getDataValueSetTemplate( DataSet dataSet, Period period, List<String> orgUnits,
        boolean writeComments, String ouScheme, String deScheme )
    {
        RootNode rootNode = new RootNode( "dataValueSet" );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setComment( "Data set: " + dataSet.getDisplayName() + " (" + dataSet.getUid() + ")" );

        CollectionNode collectionNode = rootNode.addChild( new CollectionNode( "dataValues" ) );
        collectionNode.setWrapping( false );

        if ( orgUnits.isEmpty() )
        {
            for ( DataElement dataElement : dataSet.getDataElements() )
            {
                CollectionNode collection = getDataValueTemplate( dataElement, deScheme, null, ouScheme, period,
                    writeComments );
                collectionNode.addChildren( collection.getChildren() );
            }
        }
        else
        {
            for ( String orgUnit : orgUnits )
            {
                OrganisationUnit organisationUnit = identifiableObjectManager.search( OrganisationUnit.class, orgUnit );

                if ( organisationUnit == null )
                {
                    continue;
                }

                for ( DataElement dataElement : dataSet.getDataElements() )
                {
                    CollectionNode collection = getDataValueTemplate( dataElement, deScheme, organisationUnit, ouScheme,
                        period, writeComments );
                    collectionNode.addChildren( collection.getChildren() );
                }
            }
        }

        return rootNode;
    }

    private CollectionNode getDataValueTemplate( DataElement dataElement, String deScheme,
        OrganisationUnit organisationUnit, String ouScheme, Period period, boolean comment )
    {
        CollectionNode collectionNode = new CollectionNode( "dataValues" );
        collectionNode.setWrapping( false );

        for ( DataElementCategoryOptionCombo categoryOptionCombo : dataElement.getCategoryCombo().getSortedOptionCombos() )
        {
            ComplexNode complexNode = collectionNode.addChild( new ComplexNode( "dataValue" ) );

            String label = dataElement.getDisplayName();

            if ( !categoryOptionCombo.isDefault() )
            {
                label += " " + categoryOptionCombo.getDisplayName();
            }

            if ( comment )
            {
                complexNode.setComment( "Data element: " + label );
            }

            if ( IdentifiableProperty.CODE.toString().toLowerCase()
                .equals( deScheme.toLowerCase() ) )
            {
                SimpleNode simpleNode = complexNode.addChild( new SimpleNode( "dataElement", dataElement.getCode() ) );
                simpleNode.setAttribute( true );
            }
            else
            {
                SimpleNode simpleNode = complexNode.addChild( new SimpleNode( "dataElement", dataElement.getUid() ) );
                simpleNode.setAttribute( true );
            }

            SimpleNode simpleNode = complexNode.addChild( new SimpleNode( "categoryOptionCombo", categoryOptionCombo.getUid() ) );
            simpleNode.setAttribute( true );

            simpleNode = complexNode.addChild( new SimpleNode( "period", period != null ? period.getIsoDate() : "" ) );
            simpleNode.setAttribute( true );

            if ( organisationUnit != null )
            {
                if ( IdentifiableProperty.CODE.toString().toLowerCase().equals( ouScheme.toLowerCase() ) )
                {
                    simpleNode = complexNode.addChild( new SimpleNode( "orgUnit", organisationUnit.getCode() == null ? "" : organisationUnit.getCode() ) );
                    simpleNode.setAttribute( true );
                }
                else
                {
                    simpleNode = complexNode.addChild( new SimpleNode( "orgUnit", organisationUnit.getUid() == null ? "" : organisationUnit.getUid() ) );
                    simpleNode.setAttribute( true );
                }
            }

            simpleNode = complexNode.addChild( new SimpleNode( "value", "" ) );
            simpleNode.setAttribute( true );
        }

        return collectionNode;
    }

    //--------------------------------------------------------------------------
    // Save
    //--------------------------------------------------------------------------

    @Override
    public ImportSummary saveDataValueSet( InputStream in )
    {
        return saveDataValueSet( in, ImportOptions.getDefaultImportOptions(), null );
    }

    @Override
    public ImportSummary saveDataValueSetJson( InputStream in )
    {
        return saveDataValueSetJson( in, ImportOptions.getDefaultImportOptions(), null );
    }

    @Override
    public ImportSummary saveDataValueSet( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSet( in, importOptions, null );
    }

    @Override
    public ImportSummary saveDataValueSetJson( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSetJson( in, importOptions, null );
    }

    @Override
    public ImportSummary saveDataValueSetCsv( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSetCsv( in, importOptions, null );
    }

    @Override
    public ImportSummary saveDataValueSet( InputStream in, ImportOptions importOptions, TaskId id )
    {
        try
        {
            DataValueSet dataValueSet = new StreamingDataValueSet( XMLFactory.getXMLReader( in ) );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    @Override
    public ImportSummary saveDataValueSetJson( InputStream in, ImportOptions importOptions, TaskId id )
    {
        try
        {
            DataValueSet dataValueSet = JacksonUtils.fromJson( in, DataValueSet.class );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    @Override
    public ImportSummary saveDataValueSetCsv( InputStream in, ImportOptions importOptions, TaskId id )
    {
        try
        {
            DataValueSet dataValueSet = new StreamingCsvDataValueSet( new CsvReader( in, Charset.forName( "UTF-8" ) ) );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.clear( id ).notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    @Override
    public ImportSummary saveDataValueSetPdf( InputStream in, ImportOptions importOptions, TaskId id )
    {
        try
        {
            DataValueSet dataValueSet = PdfDataEntryFormUtil.getDataValueSet( in );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.clear( id ).notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    /**
     * There are specific id schemes for data elements and organisation units and
     * a generic id scheme for all objects. The specific id schemes will take
     * precedence over the generic id scheme. The generic id scheme also applies
     * to data set and category option combo.
     * <p/>
     * The id schemes uses the following order of precedence:
     * <p/>
     * <ul>
     * <li>Id scheme from the data value set</li>
     * <li>Id scheme from the import options</li>
     * <li>Default id scheme which is UID</li>
     * <ul>
     * <p/>
     * If id scheme is specific in the data value set, any id schemes in the import
     * options will be ignored.
     *
     * @param importOptions
     * @param id
     * @param dataValueSet
     * @return
     */
    private ImportSummary saveDataValueSet( ImportOptions importOptions, TaskId id, DataValueSet dataValueSet )
    {
        Clock clock = new Clock( log ).startClock().logTime( "Starting data value import, options: " + importOptions );
        notifier.clear( id ).notify( id, "Process started" );        
        
        ImportSummary summary = new ImportSummary();

        I18n i18n = i18nManager.getI18n();

        //----------------------------------------------------------------------
        // Get import options
        //----------------------------------------------------------------------

        importOptions = importOptions != null ? importOptions : ImportOptions.getDefaultImportOptions();

        log.info( "Import options: " + importOptions );

        IdentifiableProperty dvSetIdScheme = dataValueSet.getIdSchemeProperty();
        IdentifiableProperty dvSetDataElementIdScheme = dataValueSet.getDataElementIdSchemeProperty();
        IdentifiableProperty dvSetOrgUnitIdScheme = dataValueSet.getOrgUnitIdSchemeProperty();

        log.info( "Data value set scheme: " + dvSetIdScheme + ", data element scheme: " + dvSetDataElementIdScheme + ", org unit scheme: " + dvSetOrgUnitIdScheme );

        IdentifiableProperty idScheme = dvSetIdScheme != null ? dvSetIdScheme : importOptions.getIdScheme();
        IdentifiableProperty dataElementIdScheme = dvSetDataElementIdScheme != null ? dvSetDataElementIdScheme : importOptions.getDataElementIdScheme();
        IdentifiableProperty orgUnitIdScheme = dvSetOrgUnitIdScheme != null ? dvSetOrgUnitIdScheme : importOptions.getOrgUnitIdScheme();

        log.info( "Scheme: " + idScheme + ", data element scheme: " + dataElementIdScheme + ", org unit scheme: " + orgUnitIdScheme );

        ImportStrategy strategy = dataValueSet.getStrategy() != null ?
            ImportStrategy.valueOf( dataValueSet.getStrategy() ) : importOptions.getImportStrategy();

        boolean dryRun = dataValueSet.getDryRun() != null ? dataValueSet.getDryRun() : importOptions.isDryRun();
        boolean skipExistingCheck = importOptions.isSkipExistingCheck();        
        boolean strictPeriods = importOptions.isStrictPeriods() || (Boolean) systemSettingManager.getSystemSetting( Setting.DATA_IMPORT_STRICT_PERIODS );
        boolean strictCategoryOptionCombos = importOptions.isStrictCategoryOptionCombos() || (Boolean) systemSettingManager.getSystemSetting( Setting.DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS );
        boolean strictAttrOptionCombos = importOptions.isStrictAttributeOptionCombos() || (Boolean) systemSettingManager.getSystemSetting( Setting.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS );
        boolean strictOrgUnits = importOptions.isStrictOrganisationUnits() || (Boolean) systemSettingManager.getSystemSetting( Setting.DATA_IMPORT_STRICT_ORGANISATION_UNITS );
        boolean requireCategoryOptionCombo = importOptions.isRequireCategoryOptionCombo() || (Boolean) systemSettingManager.getSystemSetting( Setting.DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO );
        boolean requireAttrOptionCombo = importOptions.isRequireAttributeOptionCombo() || (Boolean) systemSettingManager.getSystemSetting( Setting.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO );
        
        //----------------------------------------------------------------------
        // Create meta-data maps
        //----------------------------------------------------------------------

        CachingMap<String, DataElement> dataElementMap = new CachingMap<>();
        CachingMap<String, OrganisationUnit> orgUnitMap = new CachingMap<>();
        CachingMap<String, DataElementCategoryOptionCombo> optionComboMap = new CachingMap<>();
        CachingMap<String, Period> periodMap = new CachingMap<>();
        CachingMap<String, Set<PeriodType>> dataElementPeriodTypesMap = new CachingMap<>();
        CachingMap<String, Set<DataElementCategoryOptionCombo>> dataElementCategoryOptionComboMap = new CachingMap<>();
        CachingMap<String, Set<DataElementCategoryOptionCombo>> dataElementAttrOptionComboMap = new CachingMap<>();
        CachingMap<String, Boolean> dataElementOrgUnitMap = new CachingMap<>();
        CachingMap<String, Boolean> dataElementOpenFuturePeriodsMap = new CachingMap<>();
        CachingMap<String, Boolean> orgUnitInHierarchyMap = new CachingMap<>();

        //----------------------------------------------------------------------
        // Load meta-data maps
        //----------------------------------------------------------------------

        if ( importOptions.isPreheatCache() )
        {
            notifier.notify( id, "Loading data elements and organisation units" );
            dataElementMap.putAll( identifiableObjectManager.getIdMap( DataElement.class, dataElementIdScheme ) );
            orgUnitMap.putAll( getOrgUnitMap( orgUnitIdScheme ) );            
            clock.logTime( "Preheated data element and organisation unit caches" );
        }
        
        IdentifiableObjectCallable<DataElement> dataElementCallable = new IdentifiableObjectCallable<>( 
            identifiableObjectManager, DataElement.class, dataElementIdScheme, null );
        IdentifiableObjectCallable<OrganisationUnit> orgUnitCallable = new IdentifiableObjectCallable<>( 
            identifiableObjectManager, OrganisationUnit.class, orgUnitIdScheme, trimToNull( dataValueSet.getOrgUnit() ) );
        IdentifiableObjectCallable<DataElementCategoryOptionCombo> optionComboCallable = new CategoryOptionComboAclCallable( 
            categoryService, idScheme, null );
        IdentifiableObjectCallable<Period> periodCallable = new PeriodCallable( 
            periodService, null, trimToNull( dataValueSet.getPeriod() ) );
        
        //----------------------------------------------------------------------
        // Get outer meta-data
        //----------------------------------------------------------------------

        DataSet dataSet = dataValueSet.getDataSet() != null ? identifiableObjectManager.getObject( DataSet.class, idScheme, dataValueSet.getDataSet() ) : null;
        
        Date completeDate = getDefaultDate( dataValueSet.getCompleteDate() );

        Period outerPeriod = periodMap.get( trimToNull( dataValueSet.getPeriod() ), periodCallable );

        OrganisationUnit outerOrgUnit = orgUnitMap.get( trimToNull( dataValueSet.getOrgUnit() ), orgUnitCallable );

        DataElementCategoryOptionCombo fallbackCategoryOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        DataElementCategoryOptionCombo outerAttrOptionCombo = dataValueSet.getAttributeOptionCombo() != null ? 
            optionComboMap.get( trimToNull( dataValueSet.getAttributeOptionCombo() ), optionComboCallable.setId( trimToNull( dataValueSet.getAttributeOptionCombo() ) ) ) : null;

        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        if ( dataSet == null && trimToNull( dataValueSet.getDataSet() ) != null )
        {
            summary.getConflicts().add( new ImportConflict( dataValueSet.getDataSet(), "Data set not found or not accessible" ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( outerOrgUnit == null && trimToNull( dataValueSet.getOrgUnit() ) != null )
        {
            summary.getConflicts().add( new ImportConflict( dataValueSet.getDataSet(), "Org unit not found or not accessible" ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( outerAttrOptionCombo == null && trimToNull( dataValueSet.getAttributeOptionCombo() ) != null )
        {
            summary.getConflicts().add( new ImportConflict( dataValueSet.getDataSet(), "Attribute option combo not found or not accessible" ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( ImportStatus.ERROR.equals( summary.getStatus() ) )
        {
            summary.setDescription( "Import process was aborted" );
            notifier.notify( id, INFO, "Import process aborted", true ).addTaskSummary( id, summary );
            dataValueSet.close();
            return summary;
        }

        if ( dataSet != null && completeDate != null )
        {
            notifier.notify( id, "Completing data set" );
            handleComplete( dataSet, completeDate, outerPeriod, outerOrgUnit, fallbackCategoryOptionCombo, summary ); //TODO
        }
        else
        {
            summary.setDataSetComplete( Boolean.FALSE.toString() );
        }

        final String currentUser = currentUserService.getCurrentUsername();
        final Set<OrganisationUnit> currentOrgUnits = currentUserService.getCurrentUserOrganisationUnits();
        
        BatchHandler<DataValue> batchHandler = batchHandlerFactory.createBatchHandler( DataValueBatchHandler.class ).init();

        int importCount = 0;
        int updateCount = 0;
        int totalCount = 0;

        // ---------------------------------------------------------------------
        // Data values
        // ---------------------------------------------------------------------

        Date now = new Date();

        clock.logTime( "Validated outer meta-data" );
        notifier.notify( id, "Importing data values" );

        while ( dataValueSet.hasNextDataValue() )
        {
            org.hisp.dhis.dxf2.datavalue.DataValue dataValue = dataValueSet.getNextDataValue();

            totalCount++;

            final DataElement dataElement = 
                dataElementMap.get( trimToNull( dataValue.getDataElement() ), dataElementCallable.setId( trimToNull( dataValue.getDataElement() ) ) );
            final Period period = outerPeriod != null ? outerPeriod : 
                periodMap.get( trimToNull( dataValue.getPeriod() ), periodCallable.setId( trimToNull( dataValue.getPeriod() ) ) );
            final OrganisationUnit orgUnit = outerOrgUnit != null ? outerOrgUnit : 
                orgUnitMap.get( trimToNull( dataValue.getOrgUnit() ), orgUnitCallable.setId( trimToNull( dataValue.getOrgUnit() ) ) );
            DataElementCategoryOptionCombo categoryOptionCombo = optionComboMap.get( trimToNull( dataValue.getCategoryOptionCombo() ), 
                optionComboCallable.setId( trimToNull( dataValue.getCategoryOptionCombo() ) ) );
            DataElementCategoryOptionCombo attrOptionCombo = outerAttrOptionCombo != null ? outerAttrOptionCombo :
                optionComboMap.get( trimToNull( dataValue.getAttributeOptionCombo() ), optionComboCallable.setId( trimToNull( dataValue.getAttributeOptionCombo() ) ) );

            // -----------------------------------------------------------------
            // Validation
            // -----------------------------------------------------------------

            if ( dataElement == null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getDataElement(), "Data element not found or not acccessible" ) );
                continue;
            }

            if ( period == null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getPeriod(), "Period not valid" ) );
                continue;
            }
            
            if ( orgUnit == null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getOrgUnit(), "Organisation unit not found or not acccessible" ) );
                continue;
            }

            if ( categoryOptionCombo == null && trimToNull( dataValue.getCategoryOptionCombo() ) != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getCategoryOptionCombo(), "Category option combo not found or not accessible" ) );
                continue;
            }

            if ( attrOptionCombo == null && trimToNull( dataValue.getAttributeOptionCombo() ) != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getAttributeOptionCombo(), "Attribute option combo not found or not accessible" ) );
                continue;
            }

            boolean inUserHierarchy = orgUnitInHierarchyMap.get( orgUnit.getUid(), 
                () -> organisationUnitService.isInUserHierarchy( orgUnit.getUid(), currentOrgUnits ) );
            
            if ( !inUserHierarchy )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(), "Organisation unit not in hierarchy of current user: " + currentUser ) );
                continue;
            }
            
            boolean invalidFuturePeriod = period.isFuture() && dataElementOpenFuturePeriodsMap.get( dataElement.getUid(),
                () -> dataElementService.isOpenFuturePeriods( dataElement.getId() ) );
            
            if ( invalidFuturePeriod )
            {
                summary.getConflicts().add( new ImportConflict( period.getIsoDate(), "Data element does not allow for future periods through data sets: " + dataElement.getUid() ) );
                continue;
            }

            if ( dataValue.getValue() == null && dataValue.getComment() == null )
            {
                continue;
            }

            String valueValid = ValidationUtils.dataValueIsValid( dataValue.getValue(), dataElement );

            if ( valueValid != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getValue(), i18n.getString( valueValid ) + ", must match data element type: " + dataElement.getUid() ) );
                continue;
            }

            String commentValid = ValidationUtils.commentIsValid( dataValue.getComment() );

            if ( commentValid != null )
            {
                summary.getConflicts().add( new ImportConflict( "Comment", i18n.getString( commentValid ) ) );
                continue;
            }

            // -----------------------------------------------------------------
            // Constraints
            // -----------------------------------------------------------------

            if ( categoryOptionCombo == null )
            {
                if ( requireCategoryOptionCombo )
                {
                    summary.getConflicts().add( new ImportConflict( dataValue.getValue(), "Category option combo is required but is not specified" ) );
                    continue;
                }
                else
                {
                    categoryOptionCombo = fallbackCategoryOptionCombo;
                }
            }

            if ( attrOptionCombo == null )
            {
                if ( requireAttrOptionCombo )
                {
                    summary.getConflicts().add( new ImportConflict( dataValue.getValue(), "Attribute option combo is required but is not specified" ) );
                    continue;
                }
                else
                {
                    attrOptionCombo = fallbackCategoryOptionCombo;
                }
            }
            
            if ( strictPeriods && !dataElementPeriodTypesMap.get( dataElement.getUid(), 
                () -> dataElement.getPeriodTypes() ).contains( period.getPeriodType() ) )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getPeriod(), 
                    "Period type of period: " + period.getIsoDate() + " not valid for data element: " + dataElement.getUid() ) );
                continue;
            }
            
            if ( strictCategoryOptionCombos && !dataElementCategoryOptionComboMap.get( dataElement.getUid(),
                () -> dataElement.getCategoryCombo().getOptionCombos() ).contains( categoryOptionCombo ) )
            {
                summary.getConflicts().add( new ImportConflict( categoryOptionCombo.getUid(), 
                    "Category option combo: " + categoryOptionCombo.getUid() + " must be part of category combo of data element: " + dataElement.getUid() ) );
                continue;
            }
            
            if ( strictAttrOptionCombos && !dataElementAttrOptionComboMap.get( dataElement.getUid(),
                () -> dataElement.getDataSetCategoryOptionCombos() ).contains( attrOptionCombo ) )
            {
                summary.getConflicts().add( new ImportConflict( attrOptionCombo.getUid(),
                    "Attribute option combo: " + attrOptionCombo.getUid() + " must be part of category combo of data sets of data element: " + dataElement.getUid() ) );
                continue;
            }
            
            if ( strictOrgUnits && BooleanUtils.isFalse( dataElementOrgUnitMap.get( dataElement.getUid() + orgUnit.getUid(),
                () -> dataElement.hasDataSetOrganisationUnit( orgUnit ) ) ) )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(),
                    "Data element: " + dataElement.getUid() + " must be assigned through data sets to organisation unit: " + orgUnit.getUid() ) );
                continue;
            }

            boolean zeroInsignificant = ValidationUtils.dataValueIsZeroAndInsignificant( dataValue.getValue(), dataElement );

            if ( zeroInsignificant )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getValue(), "Value is zero and not significant, must match data element: " + dataElement.getUid() ) );
                continue;
            }

            String storedByValid = ValidationUtils.storedByIsValid( dataValue.getStoredBy() );

            if ( storedByValid != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getStoredBy(), i18n.getString( storedByValid ) ) );
                continue;
            }
            
            String storedBy = dataValue.getStoredBy() == null || dataValue.getStoredBy().trim().isEmpty() ? currentUser : dataValue.getStoredBy();

            // -----------------------------------------------------------------
            // Create data value
            // -----------------------------------------------------------------

            DataValue internalValue = new DataValue();

            internalValue.setDataElement( dataElement );
            internalValue.setPeriod( period );
            internalValue.setSource( orgUnit );
            internalValue.setCategoryOptionCombo( categoryOptionCombo );
            internalValue.setAttributeOptionCombo( attrOptionCombo );
            internalValue.setValue( trimToNull( dataValue.getValue() ) );
            internalValue.setStoredBy( storedBy );
            internalValue.setCreated( dataValue.hasCreated() ? parseDate( dataValue.getCreated() ) : now );
            internalValue.setLastUpdated( dataValue.hasLastUpdated() ? parseDate( dataValue.getLastUpdated() ) : now );
            internalValue.setComment( trimToNull( dataValue.getComment() ) );
            internalValue.setFollowup( dataValue.getFollowup() );

            // -----------------------------------------------------------------
            // Save, update or delete data value
            // -----------------------------------------------------------------

            if ( !skipExistingCheck && batchHandler.objectExists( internalValue ) )
            {
                if ( strategy.isCreateAndUpdate() || strategy.isUpdate() )
                {
                    if ( !dryRun )
                    {
                        if ( !internalValue.isNullValue() )
                        {
                            batchHandler.updateObject( internalValue );
                        }
                        else
                        {
                            batchHandler.deleteObject( internalValue );
                        }
                    }

                    updateCount++;
                }
            }
            else
            {
                if ( strategy.isCreateAndUpdate() || strategy.isCreate() )
                {
                    if ( !dryRun && !internalValue.isNullValue() )
                    {
                        if ( batchHandler.addObject( internalValue ) )
                        {
                            importCount++;
                        }
                    }
                }
            }
        }

        batchHandler.flush();

        int ignores = totalCount - importCount - updateCount;

        summary.setImportCount( new ImportCount( importCount, updateCount, ignores, 0 ) );
        summary.setStatus( ImportStatus.SUCCESS );
        summary.setDescription( "Import process completed successfully" );

        notifier.notify( id, INFO, "Import done", true ).addTaskSummary( id, summary );
        clock.logTime( "Data value import done, total: " + totalCount + ", import: " + importCount + ", update: " + updateCount );

        dataValueSet.close();

        return summary;
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void handleComplete( DataSet dataSet, Date completeDate, Period period, OrganisationUnit orgUnit,
        DataElementCategoryOptionCombo attributeOptionCombo, ImportSummary summary )
    {
        if ( orgUnit == null )
        {
            summary.getConflicts().add( new ImportConflict( OrganisationUnit.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE ) );
            return;
        }

        if ( period == null )
        {
            summary.getConflicts().add( new ImportConflict( Period.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE ) );
            return;
        }

        period = periodService.reloadPeriod( period );

        CompleteDataSetRegistration completeAlready = registrationService
            .getCompleteDataSetRegistration( dataSet, period, orgUnit, attributeOptionCombo );

        String username = currentUserService.getCurrentUsername();

        if ( completeAlready != null )
        {
            completeAlready.setStoredBy( username );
            completeAlready.setDate( completeDate );

            registrationService.updateCompleteDataSetRegistration( completeAlready );
        }
        else
        {
            CompleteDataSetRegistration registration = new CompleteDataSetRegistration( dataSet, period, orgUnit,
                attributeOptionCombo, completeDate, username );

            registrationService.saveCompleteDataSetRegistration( registration );
        }

        summary.setDataSetComplete( DateUtils.getMediumDateString( completeDate ) );
    }

    private Map<String, OrganisationUnit> getOrgUnitMap( IdentifiableProperty orgUnitIdScheme )
    {
        return UUID.equals( orgUnitIdScheme ) ? 
            organisationUnitService.getUuidOrganisationUnitMap() : 
                identifiableObjectManager.getIdMap( OrganisationUnit.class, orgUnitIdScheme );
    }
}
