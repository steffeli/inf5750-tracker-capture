package org.hisp.dhis.dataintegrity.tasks;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataintegrity.DataIntegrityReport;
import org.hisp.dhis.dataintegrity.DataIntegrityService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@Async
public class DataIntegrityTask
    implements Runnable
{
    private static final Log log = LogFactory.getLog( DataIntegrityTask.class );

    private TaskId taskId;

    private DataIntegrityReport dataIntegrityReport = new DataIntegrityReport();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataIntegrityService dataIntegrityService;

    private Notifier notifier;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    
    public DataIntegrityTask( TaskId taskId, DataIntegrityService dataIntegrityService, Notifier notifier )
    {
        this.taskId = taskId;
        this.dataIntegrityService = dataIntegrityService;
        this.notifier = notifier;
    }

    // -------------------------------------------------------------------------
    // Runnable implementation
    // -------------------------------------------------------------------------

    @Override
    public void run()
    {
        Timer timer = new SystemTimer().start();

        dataIntegrityReport.setDataElementsWithoutDataSet( new ArrayList<>( dataIntegrityService.getDataElementsWithoutDataSet() ) );

        dataIntegrityReport.setDataElementsWithoutGroups( new ArrayList<>( dataIntegrityService.getDataElementsWithoutGroups() ) );
        dataIntegrityReport.setDataElementsAssignedToDataSetsWithDifferentPeriodTypes( dataIntegrityService.getDataElementsAssignedToDataSetsWithDifferentPeriodTypes() );
        dataIntegrityReport.setDataElementsViolatingExclusiveGroupSets( dataIntegrityService.getDataElementsViolatingExclusiveGroupSets() );
        dataIntegrityReport.setDataElementsInDataSetNotInForm( dataIntegrityService.getDataElementsInDataSetNotInForm() );

        log.info( "Checked data elements" );

        dataIntegrityReport.setCategoryOptionCombosNotInDataElementCategoryCombo( dataIntegrityService.getCategoryOptionCombosNotInDataElementCategoryCombo() );

        log.info( "Checked operands" );

        dataIntegrityReport.setDataSetsNotAssignedToOrganisationUnits( new ArrayList<>( dataIntegrityService.getDataSetsNotAssignedToOrganisationUnits() ) );
        dataIntegrityReport.setSectionsWithInvalidCategoryCombinations( new ArrayList<>( dataIntegrityService.getSectionsWithInvalidCategoryCombinations() ) );

        log.info( "Checked data sets" );

        dataIntegrityReport.setIndicatorsWithIdenticalFormulas( dataIntegrityService.getIndicatorsWithIdenticalFormulas() );
        dataIntegrityReport.setIndicatorsWithoutGroups( new ArrayList<>( dataIntegrityService.getIndicatorsWithoutGroups() ) );
        dataIntegrityReport.setInvalidIndicatorNumerators( dataIntegrityService.getInvalidIndicatorNumerators() );
        dataIntegrityReport.setInvalidIndicatorDenominators( dataIntegrityService.getInvalidIndicatorDenominators() );
        dataIntegrityReport.setIndicatorsViolatingExclusiveGroupSets( dataIntegrityService.getIndicatorsViolatingExclusiveGroupSets() );

        log.info( "Checked indicators" );

        dataIntegrityReport.setDuplicatePeriods( dataIntegrityService.getDuplicatePeriods() );

        log.info( "Checked periods" );

        dataIntegrityReport.setOrganisationUnitsWithCyclicReferences( new ArrayList<>( dataIntegrityService.getOrganisationUnitsWithCyclicReferences() ) );
        dataIntegrityReport.setOrphanedOrganisationUnits( new ArrayList<>( dataIntegrityService.getOrphanedOrganisationUnits() ) );
        dataIntegrityReport.setOrganisationUnitsWithoutGroups( new ArrayList<>( dataIntegrityService.getOrganisationUnitsWithoutGroups() ) );
        dataIntegrityReport.setOrganisationUnitsViolatingExclusiveGroupSets( dataIntegrityService.getOrganisationUnitsViolatingExclusiveGroupSets() );
        dataIntegrityReport.setOrganisationUnitGroupsWithoutGroupSets( new ArrayList<>( dataIntegrityService.getOrganisationUnitGroupsWithoutGroupSets() ) );
        dataIntegrityReport.setValidationRulesWithoutGroups( new ArrayList<>( dataIntegrityService.getValidationRulesWithoutGroups() ) );

        log.info( "Checked organisation units" );

        dataIntegrityReport.setInvalidValidationRuleLeftSideExpressions( dataIntegrityService.getInvalidValidationRuleLeftSideExpressions() );
        dataIntegrityReport.setInvalidValidationRuleRightSideExpressions( dataIntegrityService.getInvalidValidationRuleRightSideExpressions() );

        log.info( "Checked validation rules" );

        Collections.sort( dataIntegrityReport.getDataElementsWithoutDataSet(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getDataElementsWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getDataSetsNotAssignedToOrganisationUnits(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getSectionsWithInvalidCategoryCombinations(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getIndicatorsWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getOrganisationUnitsWithCyclicReferences(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getOrphanedOrganisationUnits(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getOrganisationUnitsWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getOrganisationUnitGroupsWithoutGroupSets(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataIntegrityReport.getValidationRulesWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );

        log.info( "Sorted results" );

        timer.stop();

        if ( taskId != null )
        {
            notifier.notify( taskId, NotificationLevel.INFO, "Data integrity checks completed in " + timer.toString() + ".", true )
                .addTaskSummary( taskId, dataIntegrityReport );
        }
    }
}
