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

import static org.hisp.dhis.common.IdentifiableProperty.CODE;
import static org.hisp.dhis.common.IdentifiableProperty.UID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.mock.batchhandler.MockBatchHandler;
import org.hisp.dhis.mock.batchhandler.MockBatchHandlerFactory;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DataValueSetServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private DataElementCategoryService categoryService;
    
    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    @Autowired
    private PeriodService periodService;
    
    @Autowired
    private DataValueSetService dataValueSetService;
    
    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    private DataElementCategoryOptionCombo ocDef;
    private DataElementCategoryOption categoryOptionA;
    private DataElementCategoryOption categoryOptionB;
    private DataElementCategory categoryA;
    private DataElementCategoryCombo categoryComboDef;
    private DataElementCategoryCombo categoryComboA;
    private DataElementCategoryOptionCombo ocA;
    private DataElementCategoryOptionCombo ocB;
    
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    private DataSet dsA;
    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private Period peA;
    private Period peB;
    
    private User user;
    
    private InputStream in;

    private MockBatchHandler<DataValue> mockDataValueBatchHandler = null;
    private MockBatchHandlerFactory mockBatchHandlerFactory = null;
    
    @Override
    public void setUpTest()
    {
        mockDataValueBatchHandler = new MockBatchHandler<>();
        mockBatchHandlerFactory = new MockBatchHandlerFactory();
        mockBatchHandlerFactory.registerBatchHandler( DataValueBatchHandler.class, mockDataValueBatchHandler );
        setDependency( dataValueSetService, "batchHandlerFactory", mockBatchHandlerFactory );
        
        categoryOptionA = createCategoryOption( 'A' );
        categoryOptionB = createCategoryOption( 'B' );
        categoryA = createDataElementCategory( 'A', categoryOptionA, categoryOptionB );
        categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryComboDef = categoryService.getDefaultDataElementCategoryCombo();
        ocDef = categoryService.getDefaultDataElementCategoryOptionCombo();
        
        ocA = createCategoryOptionCombo( categoryComboA, categoryOptionA );
        ocB = createCategoryOptionCombo( categoryComboA, categoryOptionB );
        deA = createDataElement( 'A', categoryComboDef );
        deB = createDataElement( 'B', categoryComboDef );
        deC = createDataElement( 'C', categoryComboDef );
        deD = createDataElement( 'D', categoryComboDef );
        dsA = createDataSet( 'A', new MonthlyPeriodType() );
        dsA.setCategoryCombo( categoryComboDef );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        peA = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 1, 1 ), getDate( 2012, 1, 31 ) );
        peB = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 2, 1 ), getDate( 2012, 2, 29 ) );

        ocA.setUid( "kjuiHgy67hg" );
        ocB.setUid( "Gad33qy67g5" );
        deA.setUid( "f7n9E0hX8qk" );
        deB.setUid( "Ix2HsbDMLea" );
        deC.setUid( "eY5ehpbEsB7" );
        dsA.setUid( "pBOMPrpg1QX" );
        ouA.setUid( "DiszpKrYNg8" );
        ouB.setUid( "BdfsJfj87js" );
        ouC.setUid( "j7Hg26FpoIa" );

        ocA.setCode( "OC_A" );
        ocB.setCode( "OC_B" );
        deA.setCode( "DE_A" );
        deB.setCode( "DE_B" );
        deC.setCode( "DE_C" );
        deD.setCode( "DE_D" );
        dsA.setCode( "DS_A" );
        ouA.setCode( "OU_A" );
        ouB.setCode( "OU_B" );
        ouC.setCode( "OU_C" );

        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategoryCombo( categoryComboA );
        categoryService.addDataElementCategoryOptionCombo( ocA );
        categoryService.addDataElementCategoryOptionCombo( ocB );
        
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        
        dsA.addDataElement( deA );
        dsA.addDataElement( deB );
        dsA.addDataElement( deC );
        dsA.addDataElement( deD );        
        
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        
        dsA.addOrganisationUnit( ouA );
        dsA.addOrganisationUnit( ouC );
        
        dataSetService.addDataSet( dsA );
        periodService.addPeriod( peA );
        periodService.addPeriod( peB );
        
        user = createUser( 'A' );
        user.setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        setDependency( dataValueSetService, "currentUserService", currentUserService );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testImportDataValueSetXml()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in );
        
        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        
        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocDef ) ) );
        
        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA, ocDef );
        
        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );
    }

    @Test
    public void testImportDataValuesXmlWithCodeA()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetACode.xml" ).getInputStream();
            
        ImportSummary summary = dataValueSetService.saveDataValueSet( in );
        
        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocDef ) ) );
        
        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA, ocDef );
        
        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );
    }

    @Test
    public void testImportDataValuesXml()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        
        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesXmlWithCodeB()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBcode.xml" ).getInputStream();
        
        ImportOptions options = new ImportOptions( CODE, CODE, CODE );
        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        
        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesXmlWithCodePreheatCacheFalse()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBcode.xml" ).getInputStream();
        
        ImportOptions options = new ImportOptions( CODE, CODE, CODE ).setPreheatCache( false );
        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        
        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesCsv()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.csv" ).getInputStream();
        
        ImportSummary summary = dataValueSetService.saveDataValueSetCsv( in, null, null );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() ); // Header row
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() ); // Header row
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        
        assertImportDataValues( summary );
    }
    
    @Test
    public void testImportDataValuesXmlDryRun()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();
        
        ImportOptions options = new ImportOptions( UID, UID, UID ).setDryRun( true );
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        
        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }
    
    @Test
    public void testImportDataValuesXmlUpdatesOnly()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();
        
        ImportOptions options = new ImportOptions( UID, UID, UID ).setImportStrategy( ImportStrategy.UPDATES );
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 12, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        
        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }

    @Test
    public void testImportDataValuesWithNewPeriod()
        throws Exception
    {
        ImportSummary summary = dataValueSetService.saveDataValueSet( new ClassPathResource( "datavalueset/dataValueSetC.xml" ).getInputStream() );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 3, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        
        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
    }
    
    @Test
    public void testImportDataValuesWithAttributeOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetD.xml" ).getInputStream();
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        
        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocA ) ) );        
    }

    @Test
    public void testImportDataValuesWithOrgUnitOutsideHierarchy()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetE.xml" ).getInputStream();
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        
        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
    }

    @Test
    public void testImportDataValuesWithInvalidAttributeOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetF.xml" ).getInputStream();
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( ImportStatus.ERROR, summary.getStatus() );
        
        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );  
    }

    @Test
    public void testImportDataValuesWithNonExistingDataElementOrgUnit()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetG.xml" ).getInputStream();
        
        ImportSummary summary = dataValueSetService.saveDataValueSet( in );
        
        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 3, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() ); 
    }

    @Test
    public void testImportDataValuesWithStrictPeriods()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictPeriods( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithStrictCategoryOptionCombos()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictCategoryOptionCombos( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithStrictAttributeOptionCombos()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictAttributeOptionCombos( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithRequiredCategoryOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setRequireCategoryOptionCombo( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithRequiredAttributeOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setRequireAttributeOptionCombo( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithStrictOrganisationUnits()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictOrganisationUnits( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void assertImportDataValues( ImportSummary summary )
    {
        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        
        assertNotNull( dataValues );
        assertEquals( 12, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peB, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peB, ouB, ocDef, ocDef ) ) );        
    }
}
