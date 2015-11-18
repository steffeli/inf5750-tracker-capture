package org.hisp.dhis.dataentryform;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Bharath
 */
public class DataEntryFormStoreTest
    extends DhisSpringTest
{
    @Autowired
    private PeriodStore periodStore;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataEntryFormStore dataEntryFormStore;

    private PeriodType periodType;

    @Override
    public void setUpTest()
        throws Exception
    {
        periodType = periodStore.getAllPeriodTypes().iterator().next();
    }

    // -------------------------------------------------------------------------
    // DataEntryForm
    // -------------------------------------------------------------------------

    @Test
    public void testAddDataEntryForm()
    {
        DataSet dataSetA = new DataSet( "DataSet-A", periodType );

        dataSetService.addDataSet( dataSetA );

        DataEntryForm dataEntryFormA = new DataEntryForm( "DataEntryForm-A" );

        int dataEntryFormAid = dataEntryFormStore.save(dataEntryFormA );

        dataEntryFormA = dataEntryFormStore.get( dataEntryFormAid );

        assertEquals( dataEntryFormAid, dataEntryFormA.getId() );
        assertEquals( "DataEntryForm-A", dataEntryFormA.getName() );
    }

    @Test
    public void testUpdateDataEntryForm()
    {

        DataEntryForm dataEntryForm = new DataEntryForm( "DataEntryForm-A" );

        int id = dataEntryFormStore.save( dataEntryForm );

        dataEntryForm = dataEntryFormStore.get( id );

        assertEquals( "DataEntryForm-A", dataEntryForm.getName() );

        dataEntryForm.setName( "DataEntryForm-X" );

        dataEntryFormStore.update( dataEntryForm );

        dataEntryForm = dataEntryFormStore.get( id );

        assertEquals( dataEntryForm.getName(), "DataEntryForm-X" );
    }

    @Test
    public void testDeleteAndGetDataEntryForm()
    {

        DataEntryForm dataEntryForm = new DataEntryForm( "DataEntryForm-A" );

        int id = dataEntryFormStore.save( dataEntryForm );

        dataEntryForm = dataEntryFormStore.get( id );

        assertNotNull( dataEntryFormStore.get( id ) );

        dataEntryFormStore.delete( dataEntryFormStore.get( id ) );

        assertNull( dataEntryFormStore.get( id ) );
    }

    @Test
    public void testGetDataEntryFormByName()
    {
        DataEntryForm dataEntryForm = new DataEntryForm( "DataEntryForm-A" );

        int id = dataEntryFormStore.save( dataEntryForm );

        dataEntryForm = dataEntryFormStore.get( id );

        assertEquals( dataEntryFormStore.getDataEntryFormByName( "DataEntryForm-A" ), dataEntryForm );
        assertNull( dataEntryFormStore.getDataEntryFormByName( "DataEntryForm-X" ) );
    }

    @Test
    public void testGetAllDataEntryForms()
    {

        DataEntryForm dataEntryFormA = new DataEntryForm( "DataEntryForm-A" );
        DataEntryForm dataEntryFormB = new DataEntryForm( "DataEntryForm-B" );

        dataEntryFormStore.save( dataEntryFormA );
        dataEntryFormStore.save( dataEntryFormB );

        List<DataEntryForm> dataEntryForms = dataEntryFormStore.getAll();

        assertEquals( dataEntryForms.size(), 2 );
        assertTrue( dataEntryForms.contains( dataEntryFormA ) );
        assertTrue( dataEntryForms.contains( dataEntryFormB ) );
    }
}
