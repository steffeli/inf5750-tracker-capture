package org.hisp.dhis.dxf2.events.event;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.utils.InputUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public abstract class AbstractEventService
    implements EventService
{
    private static final Log log = LogFactory.getLog( AbstractEventService.class );

    public static final String OPTIONS_SEP = ";";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ProgramStageService programStageService;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    protected ProgramStageInstanceService programStageInstanceService;

    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected DataElementService dataElementService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected TrackedEntityDataValueService dataValueService;

    @Autowired
    protected TrackedEntityInstanceService entityInstanceService;

    @Autowired
    protected TrackedEntityCommentService commentService;

    @Autowired
    protected EventStore eventStore;

    @Autowired
    protected I18nManager i18nManager;

    @Autowired
    protected Notifier notifier;

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected DataElementCategoryService categoryService;

    @Autowired
    protected InputUtils inputUtils;

    protected static final int FLUSH_FREQUENCY = 20;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private CachingMap<String, Program> programCache = new CachingMap<>();

    private CachingMap<String, ProgramStage> programStageCache = new CachingMap<>();

    private CachingMap<String, DataElement> dataElementCache = new CachingMap<>();

    private List<Program> accessiblePrograms = new ArrayList<>();

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        User user = currentUserService.getCurrentUser();

        for ( Event event : events )
        {
            importSummaries.addImportSummary( addEvent( event, user, importOptions ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                dbmsManager.clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, TaskId taskId )
    {
        notifier.clear( taskId ).notify( taskId, "Importing events" );

        try
        {
            ImportSummaries importSummaries = addEvents( events, importOptions );

            if ( taskId != null )
            {
                notifier.notify( taskId, NotificationLevel.INFO, "Import done", true ).addTaskSummary( taskId, importSummaries );
            }

            return importSummaries;
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( taskId, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummaries().addImportSummary( new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() ) );
        }

    }

    @Override
    public ImportSummary addEvent( Event event, ImportOptions importOptions )
    {
        return addEvent( event, currentUserService.getCurrentUser(), importOptions );
    }

    protected ImportSummary addEvent( Event event, User user, ImportOptions importOptions )
    {
        Program program = getProgram( event.getProgram() );
        ProgramStage programStage = getProgramStage( event.getProgramStage() );

        ProgramInstance programInstance;
        ProgramStageInstance programStageInstance = null;

        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        if ( program == null )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event.program does not point to a valid program" );
        }

        if ( programStage == null && program.isRegistration() )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Event.programStage does not point to a valid programStage, and program is multi stage" );
        }
        else if ( programStage == null )
        {
            programStage = program.getProgramStageByStage( 1 );
        }

        Assert.notNull( program );
        Assert.notNull( programStage );

        if ( verifyProgramAccess( program, user ) )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Current user does not have permission to access this program" );
        }

        if ( program.isRegistration() )
        {
            if ( event.getTrackedEntityInstance() == null )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "No Event.trackedEntityInstance was provided for registration based program" );
            }

            org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance = entityInstanceService
                .getTrackedEntityInstance( event.getTrackedEntityInstance() );

            if ( entityInstance == null )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "Event.trackedEntityInstance does not point to a valid tracked entity instance" );
            }

            List<ProgramInstance> programInstances = new ArrayList<>( programInstanceService.getProgramInstances(
                entityInstance, program, ProgramStatus.ACTIVE ) );

            if ( programInstances.isEmpty() )
            {
                return new ImportSummary( ImportStatus.ERROR, "TrackedEntityInstance " + entityInstance.getUid()
                    + " is not enrolled in program " + program.getUid() );
            }
            else if ( programInstances.size() > 1 )
            {
                return new ImportSummary( ImportStatus.ERROR, "TrackedEntityInstance " + entityInstance.getUid()
                    + " have multiple active enrollments in program " + program.getUid() );
            }

            programInstance = programInstances.get( 0 );

            if ( program.isWithoutRegistration() )
            {
                List<ProgramStageInstance> programStageInstances = new ArrayList<>(
                    programStageInstanceService.getProgramStageInstances( programInstances, EventStatus.ACTIVE ) );

                if ( programStageInstances.isEmpty() )
                {
                    return new ImportSummary( ImportStatus.ERROR, "TrackedEntityInstance " + entityInstance.getUid()
                        + " is not enrolled in program stage " + programStage.getUid() );
                }
                else if ( programStageInstances.size() > 1 )
                {
                    return new ImportSummary( ImportStatus.ERROR, "TrackedEntityInstance " + entityInstance.getUid()
                        + " have multiple active enrollments in program stage " + programStage.getUid() );
                }

                programStageInstance = programStageInstances.get( 0 );
            }
            else
            {
                if ( !programStage.getIrregular() )
                {
                    programStageInstance = programStageInstanceService.getProgramStageInstance( programInstance,
                        programStage );
                }
                else
                {
                    if ( event.getEvent() != null )
                    {
                        programStageInstance = programStageInstanceService.getProgramStageInstance( event.getEvent() );

                        if ( programStageInstance == null )
                        {
                            if ( !CodeGenerator.isValidCode( event.getEvent() ) )
                            {
                                return new ImportSummary( ImportStatus.ERROR, "Event.event did not point to a valid event" );
                            }
                        }
                    }
                }
            }
        }
        else
        {
            List<ProgramInstance> programInstances = new ArrayList<>( programInstanceService.getProgramInstances(
                program, ProgramStatus.ACTIVE ) );

            if ( programInstances.isEmpty() )
            {
                // this is a WOR program, so just create the PI if it doesn't exist (should only be one)
                ProgramInstance pi = new ProgramInstance();
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );

                programInstanceService.addProgramInstance( pi );

                programInstances.add( pi );
            }
            else if ( programInstances.size() > 1 )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "Multiple active program instances exists for program " + program.getUid() );
            }

            programInstance = programInstances.get( 0 );

            if ( event.getEvent() != null )
            {
                programStageInstance = programStageInstanceService.getProgramStageInstance( event.getEvent() );

                if ( programStageInstance == null )
                {
                    if ( !CodeGenerator.isValidCode( event.getEvent() ) )
                    {
                        return new ImportSummary( ImportStatus.ERROR, "Event.event did not point to a valid event" );
                    }
                }
            }
        }

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getOrgUnitIdScheme(), event.getOrgUnit() );

        if ( organisationUnit == null )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event.orgUnit does not point to a valid organisation unit" );
        }

        if ( verifyProgramOrganisationUnitAssociation( program, organisationUnit ) )
        {
            return new ImportSummary( ImportStatus.ERROR, "Program is not assigned to this organisation unit" );
        }

        return saveEvent( program, programInstance, programStage, programStageInstance, organisationUnit, event,
            user, importOptions );
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    public Events getEvents( EventSearchParams params )
    {
        List<OrganisationUnit> organisationUnits = new ArrayList<>();

        OrganisationUnit orgUnit = params.getOrgUnit();
        OrganisationUnitSelectionMode orgUnitSelectionMode = params.getOrgUnitSelectionMode();

        if ( params.getOrgUnit() != null )
        {
            if ( OrganisationUnitSelectionMode.DESCENDANTS.equals( orgUnitSelectionMode ) )
            {
                organisationUnits.addAll( organisationUnitService.getOrganisationUnitWithChildren( orgUnit.getUid() ) );
            }
            else if ( OrganisationUnitSelectionMode.CHILDREN.equals( orgUnitSelectionMode ) )
            {
                organisationUnits.add( orgUnit );
                organisationUnits.addAll( orgUnit.getChildren() );
            }
            else // SELECTED
            {
                organisationUnits.add( orgUnit );
            }
        }

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        Events events = new Events();

        if ( params.isPaging() )
        {
            int count = 0;

            if ( params.isTotalPages() )
            {
                count = eventStore.getEventCount( params, organisationUnits );
            }

            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            events.setPager( pager );
        }

        List<Event> eventList = eventStore.getEvents( params, organisationUnits );

        events.setEvents( eventList );

        return events;
    }

    @Override
    public EventRows getEventRows( EventSearchParams params )
    {
        List<OrganisationUnit> organisationUnits = new ArrayList<>();

        OrganisationUnit orgUnit = params.getOrgUnit();
        OrganisationUnitSelectionMode orgUnitSelectionMode = params.getOrgUnitSelectionMode();

        if ( params.getOrgUnit() != null )
        {
            if ( OrganisationUnitSelectionMode.DESCENDANTS.equals( orgUnitSelectionMode ) )
            {
                organisationUnits.addAll( organisationUnitService.getOrganisationUnitWithChildren( orgUnit.getUid() ) );
            }
            else if ( OrganisationUnitSelectionMode.CHILDREN.equals( orgUnitSelectionMode ) )
            {
                organisationUnits.add( orgUnit );
                organisationUnits.addAll( orgUnit.getChildren() );
            }
            else // SELECTED
            {
                organisationUnits.add( orgUnit );
            }
        }

        EventRows eventRows = new EventRows();


        List<EventRow> eventRowList = eventStore.getEventRows( params, organisationUnits );

        eventRows.setEventRows( eventRowList );

        return eventRows;
    }

    @Override
    public EventSearchParams getFromUrl( String program, String programStage, ProgramStatus programStatus, Boolean followUp, String orgUnit,
        OrganisationUnitSelectionMode orgUnitSelectionMode, String trackedEntityInstance, Date startDate, Date endDate,
        EventStatus status, Date lastUpdated, DataElementCategoryOptionCombo attributeCoc, IdSchemes idSchemes, Integer page, Integer pageSize, boolean totalPages, boolean skipPaging, boolean includeAttributes )
    {
        UserCredentials userCredentials = currentUserService.getCurrentUser().getUserCredentials();
        
        EventSearchParams params = new EventSearchParams();

        Program pr = programService.getProgram( program );

        if ( StringUtils.isNotEmpty( program ) && pr == null )
        {
            throw new IllegalQueryException( "Program is specified but does not exist: " + program );
        }

        ProgramStage ps = programStageService.getProgramStage( programStage );

        if ( StringUtils.isNotEmpty( programStage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + programStage );
        }

        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnit );

        if ( StringUtils.isNotEmpty( orgUnit ) && ou == null )
        {
            throw new IllegalQueryException( "Org unit is specified but does not exist: " + orgUnit );
        }
        
        if( ou != null && !organisationUnitService.isInUserHierarchy( ou ) )
        {                
            if( !userCredentials.isSuper() && !userCredentials.isAuthorized( "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS" ) ) 
            {
                throw new IllegalQueryException( "User has no access to organisation unit: " + ou.getUid() );
            }
        }
        
        if( pr == null &&  !userCredentials.isSuper() && userCredentials.getAllPrograms().size() == 0 )
        {
            throw new IllegalQueryException( "User has no access to programs");
        }
        
        if( pr != null && userCredentials.getAllPrograms().contains( pr ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + pr.getUid() );
        }        

        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( trackedEntityInstance );

        if ( StringUtils.isNotEmpty( trackedEntityInstance ) && tei == null )
        {
            throw new IllegalQueryException( "Tracked entity instance is specified but does not exist: " + trackedEntityInstance );
        }

        if ( attributeCoc.isDefault() )
        {
            DataElementCategoryCombo cc = null;

            if ( pr != null )
            {
                cc = pr.getCategoryCombo();
            }

            if ( cc == null && ps != null )
            {
                cc = ps.getProgram().getCategoryCombo();
            }

            if ( cc != null && !cc.isDefault() )
            {
                throw new IllegalQueryException( "Default attribute option combo is specified while program has non-default attribute category combo:  " + cc.getUid() );
            }
        }

        params.setProgram( pr );
        params.setProgramStage( ps );
        params.setOrgUnit( ou );
        params.setTrackedEntityInstance( tei );
        params.setProgramStatus( programStatus );
        params.setFollowUp( followUp );
        params.setOrgUnitSelectionMode( orgUnitSelectionMode );
        params.setStartDate( startDate );
        params.setEndDate( endDate );
        params.setEventStatus( status );
        params.setLastUpdated( lastUpdated );
        params.setCategoryOptionCombo( attributeCoc );
        params.setIdSchemes( idSchemes );
        params.setPage( page );
        params.setPageSize( pageSize );
        params.setTotalPages( totalPages );
        params.setSkipPaging( skipPaging );
        params.setIncludeAttributes( includeAttributes );

        return params;
    }

    @Override
    public Event getEvent( String uid )
    {
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( uid );

        return psi != null ? convertProgramStageInstance( psi ) : null;
    }

    @Override
    public Event getEvent( ProgramStageInstance programStageInstance )
    {
        return convertProgramStageInstance( programStageInstance );
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries updateEvents( List<Event> events, boolean singleValue )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        User user = currentUserService.getCurrentUser();

        for ( Event event : events )
        {
            importSummaries.addImportSummary( updateEvent( event, user, singleValue, null ) );
        }

        return importSummaries;
    }

    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue )
    {
        return updateEvent( event, singleValue, null );
    }

    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue, ImportOptions importOptions )
    {
        return updateEvent( event, currentUserService.getCurrentUser(), singleValue, importOptions );
    }

    private ImportSummary updateEvent( Event event, User user, boolean singleValue, ImportOptions importOptions )
    {
        ImportSummary importSummary = new ImportSummary();
        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getEvent() );

        if ( programStageInstance == null )
        {
            importSummary.getConflicts().add( new ImportConflict( "Invalid Event ID.", event.getEvent() ) );
            return importSummary;
        }

        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getOrgUnitIdScheme(), event.getOrgUnit() );

        if ( organisationUnit == null )
        {
            organisationUnit = programStageInstance.getOrganisationUnit();
        }

        Date executionDate = new Date();

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
            programStageInstance.setExecutionDate( executionDate );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }

        String storedBy = getStoredBy( event, null, user );

        if ( event.getStatus() == EventStatus.ACTIVE )
        {
            programStageInstance.setStatus( EventStatus.ACTIVE );
            programStageInstance.setCompletedDate( null );
            programStageInstance.setCompletedUser( null );
        }
        else if ( event.getStatus() == EventStatus.COMPLETED )
        {
            programStageInstance.setCompletedDate( executionDate );
            programStageInstance.setStatus( EventStatus.COMPLETED );
            programStageInstance.setCompletedUser( storedBy );

            if ( !programStageInstance.isCompleted() )
            {
                programStageInstanceService.completeProgramStageInstance( programStageInstance,
                    i18nManager.getI18nFormat() );
            }
        }
        else if ( event.getStatus() == EventStatus.SKIPPED )
        {
            programStageInstance.setStatus( EventStatus.SKIPPED );
        }

        else if ( event.getStatus() == EventStatus.SCHEDULE )
        {
            programStageInstance.setStatus( EventStatus.SCHEDULE );
        }

        programStageInstance.setDueDate( dueDate );
        programStageInstance.setOrganisationUnit( organisationUnit );

        if ( !singleValue )
        {
            if ( programStageInstance.getProgramStage().getCaptureCoordinates() && event.getCoordinate().isValid() )
            {
                programStageInstance.setLatitude( event.getCoordinate().getLatitude() );
                programStageInstance.setLongitude( event.getCoordinate().getLongitude() );
            }
            else
            {
                programStageInstance.setLatitude( null );
                programStageInstance.setLongitude( null );
            }
        }

        programStageInstanceService.updateProgramStageInstance( programStageInstance );

        saveTrackedEntityComment( programStageInstance, event, storedBy );

        Set<TrackedEntityDataValue> dataValues = new HashSet<>( dataValueService.getTrackedEntityDataValues( programStageInstance ) );
        Map<String, TrackedEntityDataValue> existingDataValues = getDataElementDataValueMap( dataValues );

        for ( DataValue value : event.getDataValues() )
        {
            DataElement dataElement = getDataElement( value.getDataElement() );
            TrackedEntityDataValue dataValue = dataValueService.getTrackedEntityDataValue( programStageInstance, dataElement );

            if ( !validateDataValue( dataElement, value.getValue(), importSummary ) )
            {
                continue;
            }

            if ( dataValue != null )
            {
                dataValue.setValue( value.getValue() );
                dataValue.setProvidedElsewhere( value.getProvidedElsewhere() );
                dataValueService.updateTrackedEntityDataValue( dataValue );

                dataValues.remove( dataValue );
            }
            else
            {
                TrackedEntityDataValue existingDataValue = existingDataValues.get( value.getDataElement() );

                saveDataValue( programStageInstance, event.getStoredBy(), dataElement, value.getValue(),
                    value.getProvidedElsewhere(), existingDataValue, null );
            }
        }

        if ( !singleValue )
        {
            dataValues.forEach( dataValueService::deleteTrackedEntityDataValue );
        }

        return importSummary;
    }

    @Override
    public void updateEventForNote( Event event )
    {
        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getEvent() );

        if ( programStageInstance == null )
        {
            return;
        }

        saveTrackedEntityComment( programStageInstance, event, getStoredBy( event, null, currentUserService.getCurrentUser() ) );
    }

    @Override
    public void updateEventForEventDate( Event event )
    {
        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getEvent() );

        if ( programStageInstance == null )
        {
            return;
        }

        Date executionDate = new Date();

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }

        if ( event.getStatus() == EventStatus.COMPLETED )
        {
            programStageInstance.setStatus( EventStatus.COMPLETED );
        }
        else
        {
            programStageInstance.setStatus( EventStatus.VISITED );
        }

        OrganisationUnit organisationUnit = getOrganisationUnit( null, event.getOrgUnit() );

        if ( organisationUnit == null )
        {
            organisationUnit = programStageInstance.getOrganisationUnit();
        }

        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstanceService.updateProgramStageInstance( programStageInstance );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummary deleteEvent( String uid )
    {
        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( uid );

        if ( programStageInstance != null )
        {
            programStageInstanceService.deleteProgramStageInstance( programStageInstance );
            return new ImportSummary( ImportStatus.SUCCESS, "Deletion of event " + uid + " was successful." );
        }

        return new ImportSummary( ImportStatus.ERROR, "ID " + uid + " does not point to a valid event" );
    }

    @Override
    public ImportSummaries deleteEvents( List<String> uids )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        for ( String uid : uids )
        {
            importSummaries.addImportSummary( deleteEvent( uid ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                dbmsManager.clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Event convertProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        if ( programStageInstance == null )
        {
            return null;
        }

        Event event = new Event();

        event.setEvent( programStageInstance.getUid() );

        if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
        {
            event.setTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance().getUid() );
        }

        event.setFollowup( programStageInstance.getProgramInstance().getFollowup() );
        event.setEnrollmentStatus( EnrollmentStatus.fromProgramStatus( programStageInstance.getProgramInstance().getStatus() ) );
        event.setStatus( programStageInstance.getStatus() );
        event.setEventDate( DateUtils.getLongDateString( programStageInstance.getExecutionDate() ) );
        event.setDueDate( DateUtils.getLongDateString( programStageInstance.getDueDate() ) );
        event.setStoredBy( programStageInstance.getCompletedUser() );

        UserCredentials userCredentials = currentUserService.getCurrentUser().getUserCredentials();
        
        OrganisationUnit ou = programStageInstance.getOrganisationUnit();
        
        if ( ou != null )
        {             
            if( !organisationUnitService.isInUserHierarchy( ou ) )
            {                
                if( !userCredentials.isSuper()  && !userCredentials.isAuthorized( "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS" ) ) 
                {
                    throw new IllegalQueryException( "User has no access to organisation unit: " + ou.getUid() );
                }
            }
            
            event.setOrgUnit( ou.getUid() );
        }
        
        Program program = programStageInstance.getProgramInstance().getProgram();
        
        if( !userCredentials.isSuper() && !userCredentials.getAllPrograms().contains( program ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + program.getUid() );
        }
        
        event.setProgram( program.getUid() );        
        event.setEnrollment( programStageInstance.getProgramInstance().getUid() );
        event.setProgramStage( programStageInstance.getProgramStage().getUid() );

        if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
        {
            event.setTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance().getUid() );
        }

        if ( programStageInstance.getProgramStage().getCaptureCoordinates() )
        {
            Coordinate coordinate = null;

            if ( programStageInstance.getLongitude() != null && programStageInstance.getLatitude() != null )
            {
                coordinate = new Coordinate( programStageInstance.getLongitude(), programStageInstance.getLatitude() );

                try
                {
                    List<Double> list = OBJECT_MAPPER.readValue( coordinate.getCoordinateString(),
                        new TypeReference<List<Double>>()
                        {
                        } );

                    coordinate.setLongitude( list.get( 0 ) );
                    coordinate.setLatitude( list.get( 1 ) );
                }
                catch ( IOException ignored )
                {
                }
            }

            if ( coordinate != null && coordinate.isValid() )
            {
                event.setCoordinate( coordinate );
            }
        }

        Collection<TrackedEntityDataValue> dataValues = dataValueService.getTrackedEntityDataValues( programStageInstance );

        for ( TrackedEntityDataValue dataValue : dataValues )
        {
            DataValue value = new DataValue();
            value.setDataElement( dataValue.getDataElement().getUid() );
            value.setValue( dataValue.getValue() );
            value.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
            value.setStoredBy( dataValue.getStoredBy() );

            event.getDataValues().add( value );
        }

        List<TrackedEntityComment> comments = programStageInstance.getComments();

        for ( TrackedEntityComment comment : comments )
        {
            Note note = new Note();

            note.setValue( comment.getCommentText() );
            note.setStoredBy( comment.getCreator() );

            if ( comment.getCreatedDate() != null )
            {
                note.setStoredDate( comment.getCreatedDate().toString() );
            }

            event.getNotes().add( note );
        }

        return event;
    }

    private boolean verifyProgramOrganisationUnitAssociation( Program program, OrganisationUnit organisationUnit )
    {
        boolean assignedToOrganisationUnit = false;

        if ( program.getOrganisationUnits().contains( organisationUnit ) )
        {
            assignedToOrganisationUnit = true;
        }

        return !assignedToOrganisationUnit;
    }

    private boolean verifyProgramAccess( Program program, User user )
    {
        if ( accessiblePrograms.isEmpty() )
        {
            accessiblePrograms = new ArrayList<>( programService.getProgramsByUser( user ) );
        }

        return !accessiblePrograms.contains( program );
    }

    private boolean validateDataValue( DataElement dataElement, String value, ImportSummary importSummary )
    {
        String status = ValidationUtils.dataValueIsValid( value, dataElement );

        if ( status != null )
        {
            importSummary.getConflicts().add( new ImportConflict( dataElement.getUid(), status ) );
            importSummary.getImportCount().incrementIgnored();
            return false;
        }

        return true;
    }

    private String getStoredBy( Event event, ImportSummary importSummary, User fallbackUser )
    {
        String storedBy = event.getStoredBy();

        if ( storedBy == null )
        {
            storedBy = User.getSafeUsername( fallbackUser );
        }
        else if ( storedBy.length() >= 31 )
        {
            if ( importSummary != null )
            {
                importSummary.getConflicts().add(
                    new ImportConflict( "stored by", storedBy
                        + " is more than 31 characters, using current username instead" ) );
            }

            storedBy = User.getSafeUsername( fallbackUser );
        }
        return storedBy;
    }

    private void saveDataValue( ProgramStageInstance programStageInstance, String storedBy, DataElement dataElement,
        String value, Boolean providedElsewhere, TrackedEntityDataValue dataValue, ImportSummary importSummary )
    {
        if ( value != null && value.trim().length() == 0 )
        {
            value = null;
        }

        if ( value != null )
        {
            if ( dataValue == null )
            {
                dataValue = new TrackedEntityDataValue( programStageInstance, dataElement, new Date(), value );
                dataValue.setStoredBy( storedBy );
                dataValue.setProvidedElsewhere( providedElsewhere );

                dataValueService.saveTrackedEntityDataValue( dataValue );

                if ( importSummary != null )
                {
                    importSummary.getImportCount().incrementImported();
                }
            }
            else
            {
                dataValue.setValue( value );
                dataValue.setTimestamp( new Date() );
                dataValue.setStoredBy( storedBy );
                dataValue.setProvidedElsewhere( providedElsewhere );

                dataValueService.updateTrackedEntityDataValue( dataValue );

                if ( importSummary != null )
                {
                    importSummary.getImportCount().incrementUpdated();
                }
            }
        }
        else if ( dataValue != null )
        {
            dataValueService.deleteTrackedEntityDataValue( dataValue );

            if ( importSummary != null )
            {
                importSummary.getImportCount().incrementDeleted();
            }
        }
    }

    private ProgramStageInstance createProgramStageInstance( ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Date dueDate, Date executionDate, int status,
        Coordinate coordinate, String storedBy, String programStageInstanceUid, DataElementCategoryOptionCombo coc )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( CodeGenerator.isValidCode( programStageInstanceUid ) ? programStageInstanceUid : CodeGenerator.generateCode() );

        updateProgramStageInstance( programStage, programInstance, organisationUnit, dueDate, executionDate, status,
            coordinate, storedBy, programStageInstance, coc );

        return programStageInstance;
    }

    private void updateProgramStageInstance( ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Date dueDate, Date executionDate, int status, Coordinate coordinate,
        String storedBy, ProgramStageInstance programStageInstance, DataElementCategoryOptionCombo coc )
    {
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setDueDate( dueDate );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setAttributeOptionCombo( coc );

        if ( programStage.getCaptureCoordinates() )
        {
            if ( coordinate != null && coordinate.isValid() )
            {
                programStageInstance.setLongitude( coordinate.getLongitude() );
                programStageInstance.setLatitude( coordinate.getLatitude() );
            }
        }

        programStageInstance.setStatus( EventStatus.fromInt( status ) );

        if ( programStageInstance.getId() == 0 )
        {
            programStageInstance.setAutoFields();
            sessionFactory.getCurrentSession().save( programStageInstance );
        }
        else
        {
            sessionFactory.getCurrentSession().update( programStageInstance );
            sessionFactory.getCurrentSession().refresh( programStageInstance );
        }

        if ( programStageInstance.isCompleted() )
        {
            programStageInstance.setStatus( EventStatus.COMPLETED );
            programStageInstance.setCompletedDate( new Date() );
            programStageInstance.setCompletedUser( storedBy );
            programStageInstanceService.completeProgramStageInstance( programStageInstance, i18nManager.getI18nFormat() );
        }
    }

    private ImportSummary saveEvent( Program program, ProgramInstance programInstance, ProgramStage programStage,
        ProgramStageInstance programStageInstance, OrganisationUnit organisationUnit, Event event, User user,
        ImportOptions importOptions )
    {
        Assert.notNull( program );
        Assert.notNull( programInstance );
        Assert.notNull( programStage );

        ImportSummary importSummary = new ImportSummary();
        importSummary.setStatus( ImportStatus.SUCCESS );
        boolean dryRun = importOptions != null && importOptions.isDryRun();

        Date eventDate = DateUtils.parseDate( event.getEventDate() );

        Date dueDate = DateUtils.parseDate( event.getDueDate() );

        String storedBy = getStoredBy( event, importSummary, user );

        DataElementCategoryOptionCombo coc = categoryService.getDefaultDataElementCategoryOptionCombo();

        if ( event.getAttributeCategoryOptions() != null && program.getCategoryCombo() != null )
        {
            coc = inputUtils.getAttributeOptionCombo( program.getCategoryCombo(), event.getAttributeCategoryOptions() );

            if ( coc == null )
            {
                importSummary.getConflicts().add( new ImportConflict( "Invalid attribute option combo for option names.", event.getAttributeCategoryOptions() ) );
            }
        }

        if ( !dryRun )
        {
            if ( programStageInstance == null )
            {
                programStageInstance = createProgramStageInstance( programStage, programInstance, organisationUnit,
                    dueDate, eventDate, event.getStatus().getValue(), event.getCoordinate(), storedBy, event.getEvent(), coc );
            }
            else
            {
                updateProgramStageInstance( programStage, programInstance, organisationUnit, dueDate, eventDate, event
                    .getStatus().getValue(), event.getCoordinate(), storedBy, programStageInstance, coc );
            }

            saveTrackedEntityComment( programStageInstance, event, storedBy );

            importSummary.setReference( programStageInstance.getUid() );
        }

        Collection<TrackedEntityDataValue> existingDataValues = dataValueService.getTrackedEntityDataValues( programStageInstance );
        Map<String, TrackedEntityDataValue> dataElementValueMap = getDataElementDataValueMap( existingDataValues );

        for ( DataValue dataValue : event.getDataValues() )
        {
            DataElement dataElement;

            if ( dataElementValueMap.containsKey( dataValue.getDataElement() ) )
            {
                dataElement = dataElementValueMap.get( dataValue.getDataElement() ).getDataElement();
            }
            else
            {
                dataElement = getDataElement( dataValue.getDataElement() );
            }

            if ( dataElement != null )
            {
                if ( validateDataValue( dataElement, dataValue.getValue(), importSummary ) )
                {
                    String dataValueStoredBy = dataValue.getStoredBy() != null ? dataValue.getStoredBy() : storedBy;

                    if ( !dryRun )
                    {
                        TrackedEntityDataValue existingDataValue = dataElementValueMap.get( dataValue.getDataElement() );

                        saveDataValue( programStageInstance, dataValueStoredBy, dataElement, dataValue.getValue(),
                            dataValue.getProvidedElsewhere(), existingDataValue, importSummary );
                    }
                }
            }
            else
            {
                importSummary.getConflicts().add(
                    new ImportConflict( "dataElement", dataValue.getDataElement() + " is not a valid data element" ) );
                importSummary.getImportCount().incrementIgnored();
            }
        }

        return importSummary;
    }

    private Map<String, TrackedEntityDataValue> getDataElementDataValueMap( Collection<TrackedEntityDataValue> dataValues )
    {
        Map<String, TrackedEntityDataValue> map = new HashMap<>();

        for ( TrackedEntityDataValue value : dataValues )
        {
            map.put( value.getDataElement().getUid(), value );
        }

        return map;
    }

    private void saveTrackedEntityComment( ProgramStageInstance programStageInstance, Event event, String storedBy )
    {
        for ( Note note : event.getNotes() )
        {
            TrackedEntityComment comment = new TrackedEntityComment();
            comment.setCreator( storedBy );
            comment.setCreatedDate( new Date() );
            comment.setCommentText( note.getValue() );

            commentService.addTrackedEntityComment( comment );

            programStageInstance.getComments().add( comment );

            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }
    }

    private OrganisationUnit getOrganisationUnit( IdentifiableProperty scheme, String value )
    {
        OrganisationUnit organisationUnit = null;

        if ( StringUtils.isEmpty( value ) )
        {
            return null;
        }

        if ( organisationUnitCache.containsKey( value ) )
        {
            return organisationUnitCache.get( value );
        }

        if ( IdentifiableProperty.UUID.equals( scheme ) )
        {
            organisationUnit = organisationUnitService.getOrganisationUnitByUuid( value );
        }
        else if ( IdentifiableProperty.CODE.equals( scheme ) )
        {
            organisationUnit = organisationUnitService.getOrganisationUnitByCode( value );
        }
        else if ( IdentifiableProperty.NAME.equals( scheme ) )
        {
            List<OrganisationUnit> organisationUnitByName = organisationUnitService.getOrganisationUnitByName( value );

            if ( organisationUnitByName.size() == 1 )
            {
                organisationUnit = organisationUnitByName.get( 0 );
            }
        }
        else
        {
            organisationUnit = organisationUnitService.getOrganisationUnit( value );
        }

        if ( organisationUnit != null )
        {
            organisationUnitCache.put( value, organisationUnit );
        }

        return organisationUnit;
    }

    private Program getProgram( String programId )
    {
        return programCache.get( programId, new IdentifiableObjectCallable<>( manager, Program.class, programId ) );
    }

    private ProgramStage getProgramStage( String programStageId )
    {
        return programStageCache.get( programStageId, new IdentifiableObjectCallable<>( manager, ProgramStage.class, programStageId ) );
    }

    private DataElement getDataElement( String dataElementId )
    {
        return dataElementCache.get( dataElementId, new IdentifiableObjectCallable<>( manager, DataElement.class, dataElementId ) );
    }    
}
