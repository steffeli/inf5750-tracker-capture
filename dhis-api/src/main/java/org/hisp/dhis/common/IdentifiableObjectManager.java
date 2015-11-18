package org.hisp.dhis.common;

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

import org.hisp.dhis.common.NameableObject.NameableProperty;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public interface IdentifiableObjectManager
{
    String ID = IdentifiableObjectManager.class.getName();

    void save( IdentifiableObject object );

    void save( IdentifiableObject object, boolean clearSharing );

    void update( IdentifiableObject object );

    void update( List<IdentifiableObject> object );

    <T extends IdentifiableObject> T get( String uid );

    <T extends IdentifiableObject> T get( Class<T> clazz, int id );

    <T extends IdentifiableObject> T get( Class<T> clazz, String uid );

    <T extends IdentifiableObject> boolean exists( Class<T> clazz, String uid );
    
    <T extends IdentifiableObject> T get( Collection<Class<? extends IdentifiableObject>> classes, String uid );

    <T extends IdentifiableObject> T getByCode( Class<T> clazz, String code );

    <T extends IdentifiableObject> T getByName( Class<T> clazz, String name );

    <T extends IdentifiableObject> T search( Class<T> clazz, String query );

    <T extends IdentifiableObject> List<T> filter( Class<T> clazz, String query );

    <T extends IdentifiableObject> List<T> getAll( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getAllByName( Class<T> clazz, String name );

    <T extends IdentifiableObject> List<T> getAllByNameIgnoreCase( Class<T> clazz, String name );

    <T extends IdentifiableObject> List<T> getAllSorted( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getAllSortedByLastUpdated( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getByUid( Class<T> clazz, Collection<String> uids );

    <T extends IdentifiableObject> List<T> getByUidOrdered( Class<T> clazz, List<String> uids );
    
    <T extends IdentifiableObject> List<T> getLikeName( Class<T> clazz, String name );

    <T extends NameableObject> List<T> getLikeShortName( Class<T> clazz, String shortName );

    <T extends IdentifiableObject> List<T> getBetween( Class<T> clazz, int first, int max );

    <T extends IdentifiableObject> List<T> getBetweenSorted( Class<T> clazz, int first, int max );

    <T extends IdentifiableObject> List<T> getBetweenLikeName( Class<T> clazz, String name, int first, int max );

    <T extends IdentifiableObject> List<T> getBetweenLikeName( Class<T> clazz, Set<String> words, int first, int max );

    <T extends IdentifiableObject> List<T> getByLastUpdated( Class<T> clazz, Date lastUpdated );

    <T extends IdentifiableObject> List<T> getByCreated( Class<T> clazz, Date created );

    <T extends IdentifiableObject> List<T> getByLastUpdatedSorted( Class<T> clazz, Date lastUpdated );

    <T extends IdentifiableObject> List<T> getByCreatedSorted( Class<T> clazz, Date created );

    <T extends IdentifiableObject> Date getLastUpdated( Class<T> clazz );

    void delete( IdentifiableObject object );

    <T extends IdentifiableObject> Set<Integer> convertToId( Class<T> clazz, Collection<String> uids );

    <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> clazz, IdentifiableProperty property );

    <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> clazz, IdentifiableProperty property );

    <T extends NameableObject> Map<String, T> getIdMap( Class<T> clazz, NameableProperty property );

    <T extends NameableObject> Map<String, T> getIdMapNoAcl( Class<T> clazz, NameableProperty property );

    <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, IdentifiableProperty property, Collection<String> identifiers );

    <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, Collection<Integer> identifiers );
    
    <T extends IdentifiableObject> T getObject( Class<T> clazz, IdentifiableProperty property, String id );

    IdentifiableObject getObject( String uid, String simpleClassName );

    IdentifiableObject getObject( int id, String simpleClassName );

    <T extends IdentifiableObject> int getCount( Class<T> clazz );

    <T extends IdentifiableObject> int getCountByName( Class<T> clazz, String name );

    <T extends NameableObject> int getCountByShortName( Class<T> clazz, String shortName );

    <T extends IdentifiableObject> int getCountByCreated( Class<T> clazz, Date created );

    <T extends IdentifiableObject> int getCountByLastUpdated( Class<T> clazz, Date lastUpdated );

    <T extends IdentifiableObject> int getCountLikeName( Class<T> clazz, String name );

    <T extends NameableObject> int getCountLikeShortName( Class<T> clazz, String shortName );
    
    <T extends DimensionalObject> List<T> getDataDimensions( Class<T> clazz );

    <T extends DimensionalObject> List<T> getDataDimensionsNoAcl( Class<T> clazz );

    void refresh( Object object );

    void evict( Object object );

    // -------------------------------------------------------------------------
    // NO ACL
    // -------------------------------------------------------------------------

    <T extends IdentifiableObject> T getNoAcl( Class<T> clazz, String uid );
    
    <T extends IdentifiableObject> T getNoAcl( Class<T> clazz, int id );

    <T extends IdentifiableObject> void updateNoAcl( T object );

    <T extends IdentifiableObject> int getCountNoAcl( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getAllNoAcl( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getBetweenNoAcl( Class<T> clazz, int first, int max );
}
