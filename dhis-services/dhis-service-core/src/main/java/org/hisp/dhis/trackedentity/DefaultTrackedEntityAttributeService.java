package org.hisp.dhis.trackedentity;

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

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Abyot Asalefew
 */
@Transactional
public class DefaultTrackedEntityAttributeService
    implements TrackedEntityAttributeService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private TrackedEntityAttributeStore attributeStore;

    public void setAttributeStore( TrackedEntityAttributeStore attributeStore )
    {
        this.attributeStore = attributeStore;
    }

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private UserService userService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void deleteTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        attributeStore.delete( attribute );
    }

    @Override
    public List<TrackedEntityAttribute> getAllTrackedEntityAttributes()
    {
        return attributeStore.getAll();
    }

    @Override
    public TrackedEntityAttribute getTrackedEntityAttribute( int id )
    {
        return attributeStore.get( id );
    }

    @Override
    public int addTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        return attributeStore.save( attribute );
    }

    @Override
    public void updateTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        attributeStore.update( attribute );
    }

    @Override
    public TrackedEntityAttribute getTrackedEntityAttributeByName( String name )
    {
        return attributeStore.getByName( name );
    }

    @Override
    public TrackedEntityAttribute getTrackedEntityAttributeByShortName( String shortName )
    {
        return attributeStore.getByShortName( shortName );
    }

    @Override
    public TrackedEntityAttribute getTrackedEntityAttributeByCode( String code )
    {
        return attributeStore.getByShortName( code );
    }

    @Override
    public List<TrackedEntityAttribute> getOptionalAttributesWithoutGroup()
    {
        return attributeStore.getOptionalAttributesWithoutGroup();
    }

    @Override
    public List<TrackedEntityAttribute> getTrackedEntityAttributesWithoutGroup()
    {
        return attributeStore.getWithoutGroup();
    }

    @Override
    public TrackedEntityAttribute getTrackedEntityAttribute( String uid )
    {
        return attributeStore.getByUid( uid );
    }

    @Override
    public List<TrackedEntityAttribute> getTrackedEntityAttributesByDisplayOnVisitSchedule(
        boolean displayOnVisitSchedule )
    {
        return attributeStore.getByDisplayOnVisitSchedule( displayOnVisitSchedule );
    }

    @Override
    public List<TrackedEntityAttribute> getTrackedEntityAttributesWithoutProgram()
    {
        List<TrackedEntityAttribute> result = new ArrayList<>( attributeStore.getAll() );

        List<Program> programs = programService.getAllPrograms();

        for ( Program program : programs )
        {
            result.removeAll( program.getProgramAttributes() );
        }

        return result;
    }

    @Override
    public List<TrackedEntityAttribute> getTrackedEntityAttributesDisplayInList()
    {
        return attributeStore.getDisplayInList();
    }

    @Override
    public List<TrackedEntityAttribute> getTrackedEntityAttributesBetweenByName( String name, int offset, int max )
    {
        return attributeStore.getAllLikeName( name, offset, max );
    }

    @Override
    public int getTrackedEntityAttributeCount()
    {
        return attributeStore.getCount();
    }

    @Override
    public List<TrackedEntityAttribute> getTrackedEntityAttributesBetween( int offset, int max )
    {
        return attributeStore.getAllOrderedName( offset, max );
    }

    @Override
    public int getTrackedEntityAttributeCountByName( String name )
    {
        return attributeStore.getCountLikeName( name );
    }

    @Override
    public String validateScope( TrackedEntityAttribute trackedEntityAttribute,
        String value, TrackedEntityInstance trackedEntityInstance, OrganisationUnit organisationUnit, Program program )
    {
        Assert.notNull( trackedEntityAttribute, "trackedEntityAttribute is required." );

        if ( !trackedEntityAttribute.isUnique() || value == null )
        {
            return null;
        }

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.addAttribute( new QueryItem( trackedEntityAttribute, QueryOperator.EQ, value, trackedEntityAttribute.getValueType(),
            trackedEntityAttribute.getAggregationType(), trackedEntityAttribute.getOptionSet() ) );

        if ( trackedEntityAttribute.getOrgunitScope() && trackedEntityAttribute.getProgramScope() )
        {
            Assert.notNull( program, "program is required for program scope" );
            Assert.notNull( organisationUnit, "organisationUnit is required for org unit scope" );

            if ( !program.getOrganisationUnits().contains( organisationUnit ) )
            {
                return "Given orgUnit is not assigned to program " + program.getUid();
            }

            params.setProgram( program );
            params.addOrganisationUnit( organisationUnit );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED );
        }
        else if ( trackedEntityAttribute.getOrgunitScope() )
        {
            Assert.notNull( organisationUnit, "organisationUnit is required for org unit scope" );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED );
            params.addOrganisationUnit( organisationUnit );
        }
        else if ( trackedEntityAttribute.getProgramScope() )
        {
            Assert.notNull( program, "program is required for program scope" );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
            params.setProgram( program );
        }
        else
        {
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        }

        Grid instances = trackedEntityInstanceService.getTrackedEntityInstancesGrid( params );

        if ( !(instances.getHeight() == 0) )
        {
            if ( trackedEntityInstance == null || (instances.getHeight() == 1 && !instances.getRow( 0 ).contains( trackedEntityInstance.getUid() )) )
            {
                return "Non-unique attribute value '" + value + "' for attribute " + trackedEntityAttribute.getUid();
            }
        }

        return null;
    }

    @Override
    public String validateValueType( TrackedEntityAttribute trackedEntityAttribute, String value )
    {
        Assert.notNull( trackedEntityAttribute, "trackedEntityAttribute is required." );
        ValueType valueType = trackedEntityAttribute.getValueType();

        if ( value.length() > 255 )
        {
            return "Value length is greater than 256 chars for attribute " + trackedEntityAttribute.getUid();
        }

        if ( ValueType.NUMBER == valueType && !MathUtils.isNumeric( value ) )
        {
            return "Value is not numeric for attribute " + trackedEntityAttribute.getUid();
        }
        else if ( ValueType.BOOLEAN == valueType && !MathUtils.isBool( value ) )
        {
            return "Value is not boolean for attribute " + trackedEntityAttribute.getUid();
        }
        else if ( ValueType.DATE == valueType && DateUtils.parseDate( value ) == null )
        {
            return "Value is not date for attribute " + trackedEntityAttribute.getUid();
        }
        else if ( ValueType.TRUE_ONLY == valueType && !"true".equals( value ) )
        {
            return "Value is not true (true-only value type) for attribute " + trackedEntityAttribute.getUid();
        }
        else if ( ValueType.USERNAME == valueType )
        {
            if ( userService.getUserCredentialsByUsername( value ) == null )
            {
                return "Value is not pointing to a valid username for attribute " + trackedEntityAttribute.getUid();
            }
        }
        else if ( trackedEntityAttribute.hasOptionSet() && !trackedEntityAttribute.isValidOptionValue( value ) )
        {
            return "Value is not pointing to a valid option for attribute " + trackedEntityAttribute.getUid();
        }

        return null;
    }
}
