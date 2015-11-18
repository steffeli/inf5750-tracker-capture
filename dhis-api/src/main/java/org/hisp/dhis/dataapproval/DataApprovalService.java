package org.hisp.dhis.dataapproval;

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

import java.util.List;
import java.util.Set;

import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Jim Grace
 * @version $Id$
 */
public interface DataApprovalService
{
    String ID = DataApprovalService.class.getName();

    /**
     * Approves data.
     *
     * @param dataApprovalList describes the data to be approved.
     */
    void approveData( List<DataApproval> dataApprovalList );

    /**
     * Unapproves data.
     *
     * @param dataApprovalList describes the data to be unapproved.
     */
    void unapproveData( List<DataApproval> dataApprovalList );

    /**
     * Accepts data.
     *
     * @param dataApprovalList describes the data to be accepted.
     */
    void acceptData( List<DataApproval> dataApprovalList );

    /**
     * Unaccepts data.
     *
     * @param dataApprovalList describes the data to be unaccepted.
     */
    void unacceptData( List<DataApproval> dataApprovalList );

    /**
     * Returns the data approval status for a given data set, period,
     * organisation unit and attribute category combination.
     * If attributeOptionCombo is null, the default option combo will be used.
     * If data is approved at multiple levels, the lowest level is returned.
     *
     * @param dataSet DataSet to check for approval.
     * @param period Period to check for approval.
     * @param organisationUnit OrganisationUnit to check for approval.
     * @param attributeOptionCombo CategoryOptionCombo (if any) for approval.
     * @return the data approval status.
     */
    DataApprovalStatus getDataApprovalStatus( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo );

    /**
     * Returns the data approval status and permissions for a given data set,
     * period, organisation unit and attribute category combination.
     * If attributeOptionCombo is null, the default option combo will be used.
     * If data is approved at multiple levels, the lowest level is returned.
     *
     * @param dataSet DataSet to check for approval.
     * @param period Period to check for approval.
     * @param organisationUnit OrganisationUnit to check for approval.
     * @param attributeOptionCombo CategoryOptionCombo (if any) for approval.
     * @return the data approval status.
     */
    DataApprovalStatus getDataApprovalStatusAndPermissions( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo );

    /**
     * Returns a list of approval status and permissions for all of the
     * category option combos that the user is allowed to see.
     *
     * @param dataSets DataSets that we are getting the status for
     * @param period Period we are getting the status for
     * @param orgUnit Organisation unit we are getting the status for
     * @return list of statuses and permissions
     */
    List<DataApprovalStatus> getUserDataApprovalsAndPermissions( Set<DataSet> dataSets, Period period, OrganisationUnit orgUnit );
}
