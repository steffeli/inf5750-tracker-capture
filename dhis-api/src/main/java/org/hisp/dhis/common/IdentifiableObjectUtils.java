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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * @author Lars Helge Overland
 */
public class IdentifiableObjectUtils
{
    public static final String SEPARATOR = "-";
    private static final String SEPARATOR_JOIN = ", ";
    private static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );

    public static final Map<String, String> CLASS_ALIAS = ImmutableMap.<String, String>builder().
    	put( "CategoryOption", DataElementCategoryOption.class.getSimpleName() ).
    	put( "Category", DataElementCategory.class.getSimpleName() ).
    	put( "CategoryCombo", DataElementCategoryCombo.class.getSimpleName()).build();

    /**
     * Joins the names of the IdentifiableObjects in the given list and separates
     * them with a comma and space. Returns null if the given list is null or has
     * no elements.
     *
     * @param objects the list of IdentifiableObjects.
     * @return the joined string.
     */
    public static String join( Collection<? extends IdentifiableObject> objects )
    {
        if ( objects != null && objects.size() > 0 )
        {
            Iterator<? extends IdentifiableObject> iterator = objects.iterator();

            StringBuilder builder = new StringBuilder( iterator.next().getDisplayName() );

            while ( iterator.hasNext() )
            {
                builder.append( SEPARATOR_JOIN ).append( iterator.next().getDisplayName() );
            }

            return builder.toString();
        }

        return null;
    }

    /**
     * Returns a list of uids for the given collection of IdentifiableObjects.
     *
     * @param objects the list of IdentifiableObjects.
     * @return a list of uids.
     */
    public static <T extends IdentifiableObject> List<String> getUids( Collection<T> objects )
    {
        return objects != null ? objects.stream().map( o -> o.getUid() ).collect( Collectors.toList() ) : null;
    }

    /**
     * Returns a list of internal identifiers for the given collection of IdentifiableObjects.
     *
     * @param objects the list of IdentifiableObjects.
     * @return a list of uids.
     */
    public static <T extends IdentifiableObject> List<Integer> getIdentifiers( Collection<T> objects )
    {
        return objects != null ? objects.stream().map( o -> o.getId() ).collect( Collectors.toList() ) : null;
    }

    /**
     * Returns a list of calendar specific period identifiers for the given collection of
     * periods and calendar.
     *
     * @param periods  the list of periods.
     * @param calendar the calendar to use for generation of iso periods.
     * @return a list of iso period identifiers.
     */
    public static <T extends IdentifiableObject> List<String> getLocalPeriodIdentifiers( Collection<T> periods, Calendar calendar )
    {
        List<String> localIdentifiers = new ArrayList<>();

        for ( IdentifiableObject object : periods )
        {
            Period period = (Period) object;
            DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );
            localIdentifiers.add( period.getPeriodType().getIsoDate( dateTimeUnit ) );
        }

        return localIdentifiers;
    }

    /**
     * Returns a local period identifier for a specific date / periodType / calendar.
     *
     * @param date       Date to create from
     * @param periodType PeriodType to create from
     * @param calendar   Calendar to create from
     * @return Period identifier based on given calendar
     */
    public static String getLocalPeriodIdentifier( Date date, PeriodType periodType, Calendar calendar )
    {
        return getLocalPeriodIdentifier( periodType.createPeriod( date, calendar ), calendar );
    }

    /**
     * Returns a local period identifier for a specific period / calendar.
     *
     * @param period   the list of periods.
     * @param calendar the calendar to use for generation of iso periods.
     * @return Period identifier based on given calendar
     */
    public static String getLocalPeriodIdentifier( Period period, Calendar calendar )
    {
        if ( calendar.isIso8601() )
        {
            return period.getIsoDate();
        }

        return period.getPeriodType().getIsoDate( calendar.fromIso( period.getStartDate() ) );
    }

    /**
     * Filters the given list of IdentifiableObjects based on the given key.
     *
     * @param identifiableObjects the list of IdentifiableObjects.
     * @param key                 the key.
     * @param ignoreCase          indicates whether to ignore case when filtering.
     * @return a filtered list of IdentifiableObjects.
     */
    public static <T extends IdentifiableObject> List<T> filterNameByKey( List<T> identifiableObjects, String key,
        boolean ignoreCase )
    {
        List<T> objects = new ArrayList<>();
        ListIterator<T> iterator = identifiableObjects.listIterator();

        if ( ignoreCase )
        {
            key = key.toLowerCase();
        }

        while ( iterator.hasNext() )
        {
            T object = iterator.next();
            String name = ignoreCase ? object.getDisplayName().toLowerCase() : object.getDisplayName();

            if ( name.indexOf( key ) != -1 )
            {
                objects.add( object );
            }
        }

        return objects;
    }

    /**
     * Returns a list of IdentifiableObjects.
     *
     * @param objects the IdentifiableObjects to include in the list.
     * @return a list of IdentifiableObjects.
     */
    public static List<IdentifiableObject> getList( IdentifiableObject... objects )
    {
        List<IdentifiableObject> list = new ArrayList<>();

        if ( objects != null )
        {
            Collections.addAll( list, objects );
        }

        return list;
    }

    /**
     * Returns a list with erasure IdentifiableObject based on the given collection.
     *
     * @param collection the collection.
     * @return a list of IdentifiableObjects.
     */
    public static List<IdentifiableObject> asList( Collection<? extends IdentifiableObject> collection )
    {
        List<IdentifiableObject> list = new ArrayList<>();
        list.addAll( collection );
        return list;
    }

    /**
     * Returns a list typed with the desired erasure based on the given collection.
     * This operation implies an unchecked cast and it is the responsibility of
     * the caller to make sure the cast is valid.
     *
     * @param collection the collection.
     * @return a list.
     */
    @SuppressWarnings( "unchecked" )
    public static <T extends IdentifiableObject> List<T> asTypedList( Collection<IdentifiableObject> collection )
    {
        List<T> list = new ArrayList<>();

        if ( collection != null )
        {
            for ( IdentifiableObject object : collection )
            {
                list.add( (T) object );
            }
        }

        return list;
    }

    /**
     * Removes duplicates from the given list while maintaining the order.
     *
     * @param list the list.
     */
    public static <T extends IdentifiableObject> List<T> removeDuplicates( List<T> list )
    {
        final List<T> temp = new ArrayList<>( list );
        list.clear();

        for ( T object : temp )
        {
            if ( !list.contains( object ) )
            {
                list.add( object );
            }
        }

        return list;
    }

    /**
     * Generates a tag reflecting the date of when the most recently updated
     * IdentifiableObject in the given collection was modified.
     *
     * @param objects the collection of IdentifiableObjects.
     * @return a string tag.
     */
    public static <T extends IdentifiableObject> String getLastUpdatedTag( Collection<T> objects )
    {
        Date latest = null;

        if ( objects != null )
        {
            for ( IdentifiableObject object : objects )
            {
                if ( object != null && object.getLastUpdated() != null && (latest == null || object.getLastUpdated().after( latest )) )
                {
                    latest = object.getLastUpdated();
                }
            }
        }

        return latest != null && objects != null ? objects.size() + SEPARATOR + LONG_DATE_FORMAT.format( latest ) : null;
    }

    /**
     * Generates a tag reflecting the date of when the object was last updated.
     *
     * @param object the identifiable object.
     * @return a string tag.
     */
    public static String getLastUpdatedTag( IdentifiableObject object )
    {
        return object != null ? LONG_DATE_FORMAT.format( object.getLastUpdated() ) : null;
    }

    /**
     * Returns a mapping between the uid and the display name of the given 
     * identifiable objects.
     *
     * @param objects the identifiable objects.
     * @return mapping between the uid and the display name of the given objects.
     */
    public static Map<String, String> getUidNameMap( Collection<? extends IdentifiableObject> objects )
    {        
        return objects.stream().collect( Collectors.toMap( IdentifiableObject::getUid, IdentifiableObject::getDisplayName ) );
    }

    /**
     * Returns a mapping between the uid and the name of the given identifiable
     * objects.
     *
     * @param objects the identifiable objects.
     * @return mapping between the uid and the name of the given objects.
     */
    public static <T extends IdentifiableObject> Map<String, T> getUidObjectMap( Collection<T> objects )
    {
        return objects != null ? Maps.uniqueIndex( objects, T::getUid ) : Maps.newHashMap();
    }

    /**
     * Returns a map of identifiable properties and objects.
     * 
     * @param objects the objects.
     * @param property the identifiable property.
     * @return a map.
     */
    @SuppressWarnings( "unchecked" )
    public static <T extends IdentifiableObject> Map<String, T> getMap( List<T> objects, IdentifiableProperty property )
    {
        Map<String, T> map = new HashMap<>();
        
        for ( T object : objects )
        {
            if ( IdentifiableProperty.ID.equals( property ) )
            {
                if ( object.getId() > 0 )
                {
                    map.put( String.valueOf( object.getId() ), object );
                }
            }
            else if ( IdentifiableProperty.UID.equals( property ) )
            {
                if ( object.getUid() != null )
                {
                    map.put( object.getUid(), object );
                }
            }
            else if ( IdentifiableProperty.CODE.equals( property ) )
            {
                if ( object.getCode() != null )
                {
                    map.put( object.getCode(), object );
                }
            }
            else if ( IdentifiableProperty.NAME.equals( property ) )
            {
                if ( object.getName() != null )
                {
                    map.put( object.getName(), object );
                }
            }
            else if ( IdentifiableProperty.UUID.equals( property ) && OrganisationUnit.class.isAssignableFrom( object.getClass() ) )
            {
                OrganisationUnit organisationUnit = (OrganisationUnit) object;

                if ( !StringUtils.isEmpty( organisationUnit.getUuid() ) )
                {
                    map.put( organisationUnit.getUuid(), (T) organisationUnit );
                }
            }
        }
        
        return map;
    }
}
