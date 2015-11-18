package org.hisp.dhis.datavalue;

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

import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 * @author Halvdan Hoem Grelland
 */
@Transactional
public class DefaultDataValueService
    implements DataValueService
{
    private static final Log log = LogFactory.getLog( DefaultDataValueService.class );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataValueStore dataValueStore;

    public void setDataValueStore( DataValueStore dataValueStore )
    {
        this.dataValueStore = dataValueStore;
    }

    private DataValueAuditService dataValueAuditService;

    public void setDataValueAuditService( DataValueAuditService dataValueAuditService )
    {
        this.dataValueAuditService = dataValueAuditService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private FileResourceService fileResourceService;

    public void setFileResourceService( FileResourceService fileResourceService )
    {
        this.fileResourceService = fileResourceService;
    }

    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    @Override
    public boolean addDataValue( DataValue dataValue )
    {
        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        if ( dataValue == null || dataValue.isNullValue() )
        {
            log.info( "Data value is null" );
            return false;
        }
        
        String result = dataValueIsValid( dataValue.getValue(), dataValue.getDataElement() );
        
        if ( result != null )
        {
            log.info( "Data value is not valid: " +  result );
            return false;
        }
        
        boolean zeroInsignificant = dataValueIsZeroAndInsignificant( dataValue.getValue(), dataValue.getDataElement() );
        
        if ( zeroInsignificant )
        {
            log.info( "Data value is zero and insignificant" );
            return false;
        }

        // ---------------------------------------------------------------------
        // Save
        // ---------------------------------------------------------------------

        if ( dataValue.getCategoryOptionCombo() == null )
        {
            dataValue.setCategoryOptionCombo( categoryService.getDefaultDataElementCategoryOptionCombo() );
        }
        
        if ( dataValue.getAttributeOptionCombo() == null )
        {
            dataValue.setAttributeOptionCombo( categoryService.getDefaultDataElementCategoryOptionCombo() );
        }

        dataValue.setCreated( new Date() );

        dataValueStore.addDataValue( dataValue );
        
        return true;
    }

    @Override
    @Transactional
    public void updateDataValue( DataValue dataValue )
    {
        if ( dataValue.isNullValue() || dataValueIsZeroAndInsignificant( dataValue.getValue(), dataValue.getDataElement() ) )
        {
            deleteDataValue( dataValue );
        }
        else if ( dataValueIsValid( dataValue.getValue(), dataValue.getDataElement() ) == null )
        {
            DataValueAudit dataValueAudit = new DataValueAudit( dataValue, dataValue.getAuditValue(),
                dataValue.getStoredBy(), new Date(), AuditType.UPDATE );

            dataValueAuditService.addDataValueAudit( dataValueAudit );
            dataValueStore.updateDataValue( dataValue );
        }
    }

    @Override
    @Transactional
    public void deleteDataValue( DataValue dataValue )
    {
        DataValueAudit dataValueAudit = new DataValueAudit( dataValue, dataValue.getAuditValue(),
            currentUserService.getCurrentUsername(), new Date(), AuditType.DELETE );

        dataValueAuditService.addDataValueAudit( dataValueAudit );

        if ( dataValue.getDataElement().isFileType() )
        {
            fileResourceService.deleteFileResource( dataValue.getValue() );
        }

        dataValueStore.deleteDataValue( dataValue );
    }

    @Override
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        DataElementCategoryOptionCombo defaultOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        
        return dataValueStore.getDataValue( dataElement, period, source, categoryOptionCombo, defaultOptionCombo );
    }

    @Override
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        return dataValueStore.getDataValue( dataElement, period, source, categoryOptionCombo, attributeOptionCombo );
    }
    
    @Override
    public DataValue getDataValue( int dataElementId, int periodId, int sourceId, int categoryOptionComboId )
    {
        DataElementCategoryOptionCombo defaultOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        
        return dataValueStore.getDataValue( dataElementId, periodId, sourceId, categoryOptionComboId, defaultOptionCombo.getId() );
    }
    
    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    @Override
    public List<DataValue> getAllDataValues()
    {
        return dataValueStore.getAllDataValues();
    }

    @Override
    public List<DataValue> getDataValues( OrganisationUnit source, Period period )
    {
        return dataValueStore.getDataValues( source, period );
    }

    @Override
    public List<DataValue> getDataValues( OrganisationUnit source, DataElement dataElement )
    {
        return dataValueStore.getDataValues( source, dataElement );
    }

    @Override
    public List<DataValue> getDataValues( Collection<OrganisationUnit> sources, DataElement dataElement )
    {
        return dataValueStore.getDataValues( sources, dataElement );
    }

    @Override
    public List<DataValue> getDataValues( OrganisationUnit source, Period period, Collection<DataElement> dataElements )
    {
        return dataValueStore.getDataValues( source, period, dataElements );
    }

    @Override
    public List<DataValue> getDataValues( OrganisationUnit source, Period period,
        Collection<DataElement> dataElements, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        return dataValueStore.getDataValues( source, period, dataElements, attributeOptionCombo );
    }

    @Override
    public List<DataValue> getDataValues( OrganisationUnit source, Period period, Collection<DataElement> dataElements,
        Collection<DataElementCategoryOptionCombo> optionCombos )
    {
        return dataValueStore.getDataValues( source, period, dataElements, optionCombos );
    }

    @Override
    public List<DataValue> getDataValues( DataElement dataElement, Period period,
        Collection<OrganisationUnit> sources )
    {
        return dataValueStore.getDataValues( dataElement, period, sources );
    }

    @Override
    public List<DataValue> getDataValues( DataElement dataElement, Collection<Period> periods,
        Collection<OrganisationUnit> sources )
    {
        return dataValueStore.getDataValues( dataElement, periods, sources );
    }

    @Override
    public List<DataValue> getDataValues( DataElement dataElement, DataElementCategoryOptionCombo optionCombo,
        Collection<Period> periods, Collection<OrganisationUnit> sources )
    {
        return dataValueStore.getDataValues( dataElement, optionCombo, periods, sources );
    }

    @Override
    public List<DataValue> getDataValues( Collection<DataElementCategoryOptionCombo> optionCombos )
    {
        return dataValueStore.getDataValues( optionCombos );
    }

    @Override
    public List<DataValue> getDataValues( DataElement dataElement )
    {
        return dataValueStore.getDataValues( dataElement );
    }

    @Override
    public DataValue getLatestDataValues( DataElement dataElement, PeriodType periodType,
        OrganisationUnit organisationUnit )
    {
        return dataValueStore.getLatestDataValues( dataElement, periodType, organisationUnit );
    }

    @Override
    public int getDataValueCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return dataValueStore.getDataValueCountLastUpdatedAfter( cal.getTime() );
    }
    
    @Override
    public int getDataValueCountLastUpdatedAfter( Date date )
    {
        return dataValueStore.getDataValueCountLastUpdatedAfter( date );
    }
    
    @Override
    public MapMap<Integer, DataElementOperand, Double> getDataValueMapByAttributeCombo( Collection<DataElement> dataElements, Date date,
            OrganisationUnit source, Collection<PeriodType> periodTypes, DataElementCategoryOptionCombo attributeCombo,
            Set<CategoryOptionGroup> cogDimensionConstraints, Set<DataElementCategoryOption> coDimensionConstraints,
            MapMap<Integer, DataElementOperand, Date> lastUpdatedMap )
    {
        return dataValueStore.getDataValueMapByAttributeCombo( dataElements, date, source, periodTypes, attributeCombo,
               cogDimensionConstraints, coDimensionConstraints, lastUpdatedMap );
    }
    
    @Override
    public Collection<DeflatedDataValue> getDeflatedDataValues( int dataElementId, int periodId, Collection<Integer> sourceIds )
    {
        return dataValueStore.getDeflatedDataValues( dataElementId, periodId, sourceIds );
    }
}
