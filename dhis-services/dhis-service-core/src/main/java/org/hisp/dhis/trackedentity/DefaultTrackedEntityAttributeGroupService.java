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

import static org.hisp.dhis.i18n.I18nUtils.i18n;

import java.util.List;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.i18n.I18nService;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
public class DefaultTrackedEntityAttributeGroupService
    implements TrackedEntityAttributeGroupService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<TrackedEntityAttributeGroup> attributeGroupStore;

    public void setAttributeGroupStore( GenericIdentifiableObjectStore<TrackedEntityAttributeGroup> attributeGroupStore )
    {
        this.attributeGroupStore = attributeGroupStore;
    }

    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int addTrackedEntityAttributeGroup( TrackedEntityAttributeGroup attributeGroup )
    {
        return attributeGroupStore.save( attributeGroup );
    }

    @Override
    public void deleteTrackedEntityAttributeGroup( TrackedEntityAttributeGroup attributeGroup )
    {
        attributeGroupStore.delete( attributeGroup );
    }

    @Override
    public void updateTrackedEntityAttributeGroup( TrackedEntityAttributeGroup attributeGroup )
    {
        attributeGroupStore.update( attributeGroup );
    }

    @Override
    public TrackedEntityAttributeGroup getTrackedEntityAttributeGroup( int id )
    {
        return i18n( i18nService, attributeGroupStore.get( id ) );
    }

    @Override
    public TrackedEntityAttributeGroup getTrackedEntityAttributeGroup( String uid )
    {
        return i18n( i18nService, attributeGroupStore.getByUid( uid ) );
    }

    @Override
    public TrackedEntityAttributeGroup getTrackedEntityAttributeGroupByName( String name )
    {
        return i18n( i18nService, attributeGroupStore.getByName( name ) );
    }

    @Override
    public List<TrackedEntityAttributeGroup> getAllTrackedEntityAttributeGroups()
    {
        return i18n( i18nService, attributeGroupStore.getAll() );
    }

    @Override
    public Integer getTrackedEntityAttributeGroupCountByName( String name )
    {
        return attributeGroupStore.getCountLikeName( name );
    }

    @Override
    public List<TrackedEntityAttributeGroup> getTrackedEntityAttributeGroupsBetweenByName( String name, int min, int max )
    {
        return attributeGroupStore.getAllLikeName( name, min, max );
    }

    @Override
    public Integer getTrackedEntityAttributeGroupCount()
    {
        return attributeGroupStore.getCount();
    }

    @Override
    public List<TrackedEntityAttributeGroup> getTrackedEntityAttributeGroupsBetween( int min, int max )
    {
        return attributeGroupStore.getAllOrderedName( min, max );
    }

}
