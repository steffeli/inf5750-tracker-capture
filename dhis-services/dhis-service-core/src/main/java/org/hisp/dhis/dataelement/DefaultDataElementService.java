package org.hisp.dhis.dataelement;

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

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.GenericNameableObjectStore;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.comparator.DataElementCategoryComboSizeComparator;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nService;
import org.hisp.dhis.period.PeriodType;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.i18n.I18nUtils.*;

/**
 * @author Kristian Nordal
 */
@Transactional
public class DefaultDataElementService
    implements DataElementService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementStore dataElementStore;

    public void setDataElementStore( DataElementStore dataElementStore )
    {
        this.dataElementStore = dataElementStore;
    }

    private GenericNameableObjectStore<DataElementGroup> dataElementGroupStore;

    public void setDataElementGroupStore( GenericNameableObjectStore<DataElementGroup> dataElementGroupStore )
    {
        this.dataElementGroupStore = dataElementGroupStore;
    }

    private GenericDimensionalObjectStore<DataElementGroupSet> dataElementGroupSetStore;

    public void setDataElementGroupSetStore( GenericDimensionalObjectStore<DataElementGroupSet> dataElementGroupSetStore )
    {
        this.dataElementGroupSetStore = dataElementGroupSetStore;
    }

    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
    }

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    @Override
    public int addDataElement( DataElement dataElement )
    {
        return dataElementStore.save( dataElement );
    }

    @Override
    public void updateDataElement( DataElement dataElement )
    {
        dataElementStore.update( dataElement );
    }

    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        dataElementStore.delete( dataElement );
    }

    @Override
    public DataElement getDataElement( int id )
    {
        return i18n( i18nService, dataElementStore.get( id ) );
    }

    @Override
    public DataElement getDataElement( String uid )
    {
        return i18n( i18nService, dataElementStore.getByUid( uid ) );
    }

    @Override
    public DataElement getDataElementByCode( String code )
    {
        return i18n( i18nService, dataElementStore.getByCode( code ) );
    }

    @Override
    public boolean isOpenFuturePeriods( int id )
    {
        DataElement dataElement = getDataElement( id );
        
        return dataElement != null && dataElement.getOpenFuturePeriods() >= 0;
    }
    
    @Override
    public List<DataElement> getAllDataElements()
    {
        return i18n( i18nService, dataElementStore.getAll() );
    }

    @Override
    public List<DataElement> getDataElementsByUid( Collection<String> uids )
    {
        return i18n( i18nService, dataElementStore.getByUid( uids ) );
    }

    @Override
    public void setZeroIsSignificantForDataElements( Collection<Integer> dataElementIds )
    {
        if ( dataElementIds != null )
        {
            dataElementStore.setZeroIsSignificantForDataElements( dataElementIds );
        }
    }

    @Override
    public List<DataElement> getDataElementsByZeroIsSignificant( boolean zeroIsSignificant )
    {
        return dataElementStore.getDataElementsByZeroIsSignificant( zeroIsSignificant );
    }

    @Override
    public Set<DataElement> getDataElementsByZeroIsSignificantAndGroup( boolean zeroIsSignificant, DataElementGroup dataElementGroup )
    {
        Set<DataElement> dataElements = new HashSet<>( dataElementGroup.getMembers() );

        return dataElements.stream().filter( p -> p.isZeroIsSignificant() ).collect( Collectors.toSet() );
    }

    @Override
    public List<DataElement> getAggregateableDataElements()
    {
        return i18n( i18nService, dataElementStore.getAggregateableDataElements() );
    }

    @Override
    public DataElement getDataElementByName( String name )
    {
        List<DataElement> dataElements = dataElementStore.getAllEqName( name );

        if ( dataElements.isEmpty() )
        {
            return null;
        }

        return i18n( i18nService, dataElements.get( 0 ) );
    }

    @Override
    public List<DataElement> searchDataElementsByName( String key )
    {
        return i18n( i18nService, dataElementStore.searchDataElementsByName( key ) );
    }

    @Override
    public DataElement getDataElementByShortName( String shortName )
    {
        List<DataElement> dataElements = dataElementStore.getAllEqShortName( shortName );

        return !dataElements.isEmpty() ? i18n( i18nService, dataElements.get( 0 ) ) : null;
    }

    @Override
    public List<DataElement> getDataElementsByAggregationType( AggregationType aggregationType )
    {
        return i18n( i18nService, dataElementStore.getDataElementsByAggregationType( aggregationType ) );
    }

    @Override
    public List<DataElement> getDataElementsByValueTypes( Collection<ValueType> valueTypes )
    {
        return i18n( i18nService, dataElementStore.getDataElementsByValueTypes( valueTypes ) );
    }

    @Override
    public List<DataElement> getDataElementsByValueType( ValueType valueType )
    {
        return i18n( i18nService, dataElementStore.getDataElementsByValueType( valueType ) );
    }

    @Override
    public List<DataElement> getDataElementsByPeriodType( final PeriodType periodType )
    {
        return getAllDataElements().stream().filter( p -> p.getPeriodType() != null && p.getPeriodType().equals( periodType ) ).collect( Collectors.toList() );
    }

    @Override
    public List<DataElement> getDataElementsByDomainType( DataElementDomain domainType )
    {
        return i18n( i18nService, dataElementStore.getDataElementsByDomainType( domainType ) );
    }

    @Override
    public List<DataElement> getDataElementsByDomainType( DataElementDomain domainType, int first, int max )
    {
        return i18n( i18nService, dataElementStore.getDataElementsByDomainType( domainType, first, max ) );
    }

    @Override
    public List<DataElement> getDataElementByCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        return i18n( i18nService, dataElementStore.getDataElementByCategoryCombo( categoryCombo ) );
    }

    @Override
    public Map<DataElementCategoryCombo, List<DataElement>> getGroupedDataElementsByCategoryCombo(
        List<DataElement> dataElements )
    {
        Map<DataElementCategoryCombo, List<DataElement>> mappedDataElements = new HashMap<>();

        for ( DataElement dataElement : dataElements )
        {
            if ( mappedDataElements.containsKey( dataElement.getCategoryCombo() ) )
            {
                mappedDataElements.get( dataElement.getCategoryCombo() ).add( dataElement );
            }
            else
            {
                List<DataElement> des = new ArrayList<>();
                des.add( dataElement );

                mappedDataElements.put( dataElement.getCategoryCombo(), des );
            }
        }

        return mappedDataElements;
    }

    @Override
    public List<DataElementCategoryCombo> getDataElementCategoryCombos( List<DataElement> dataElements )
    {
        Set<DataElementCategoryCombo> categoryCombos = new HashSet<>();

        for ( DataElement dataElement : dataElements )
        {
            categoryCombos.add( dataElement.getCategoryCombo() );
        }

        List<DataElementCategoryCombo> listCategoryCombos = new ArrayList<>( categoryCombos );

        Collections.sort( listCategoryCombos, new DataElementCategoryComboSizeComparator() );

        return listCategoryCombos;
    }

    @Override
    public List<DataElement> getDataElementsWithGroupSets()
    {
        return i18n( i18nService, dataElementStore.getDataElementsWithGroupSets() );
    }

    @Override
    public List<DataElement> getDataElementsWithoutGroups()
    {
        return i18n( i18nService, dataElementStore.getDataElementsWithoutGroups() );
    }

    @Override
    public List<DataElement> getDataElementsWithoutDataSets()
    {
        return i18n( i18nService, dataElementStore.getDataElementsWithoutDataSets() );
    }

    @Override
    public List<DataElement> getDataElementsWithDataSets()
    {
        return i18n( i18nService, dataElementStore.getDataElementsWithDataSets() );
    }

    @Override
    public List<DataElement> getDataElementsLikeName( String name )
    {
        return getObjectsByName( i18nService, dataElementStore, name );
    }

    @Override
    public int getDataElementCount()
    {
        return dataElementStore.getCount();
    }

    @Override
    public int getDataElementCountByName( String name )
    {
        return getCountByName( i18nService, dataElementStore, name );
    }

    @Override
    public int getDataElementCountByDomainType( DataElementDomain domainType )
    {
        return dataElementStore.getCountByDomainType( domainType );
    }

    @Override
    public List<DataElement> getDataElementsBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, dataElementStore, first, max );
    }

    @Override
    public List<DataElement> getDataElementsBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, dataElementStore, name, first, max );
    }

    @Override
    public List<DataElement> getDataElementsByDataSets( Collection<DataSet> dataSets )
    {
        return i18n( i18nService, dataElementStore.getDataElementsByDataSets( dataSets ) );
    }

    @Override
    public List<DataElement> getDataElementsByAggregationLevel( int aggregationLevel )
    {
        return i18n( i18nService, dataElementStore.getDataElementsByAggregationLevel( aggregationLevel ) );
    }

    @Override
    public ListMap<String, String> getDataElementCategoryOptionComboMap( Set<String> dataElementUids )
    {
        return dataElementStore.getDataElementCategoryOptionComboMap( dataElementUids );
    }

    @Override
    public Map<String, Integer> getDataElementUidIdMap()
    {
        Map<String, Integer> map = new HashMap<>();

        for ( DataElement dataElement : getAllDataElements() )
        {
            map.put( dataElement.getUid(), dataElement.getId() );
        }

        return map;
    }

    @Override
    public List<DataElement> getDataElements( DataSet dataSet, String key, Integer max )
    {
        return i18n( i18nService, dataElementStore.get( dataSet, key, max ) );
    }

    // -------------------------------------------------------------------------
    // DataElementGroup
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementGroup( DataElementGroup dataElementGroup )
    {
        int id = dataElementGroupStore.save( dataElementGroup );

        return id;
    }

    @Override
    public void updateDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.update( dataElementGroup );
    }

    @Override
    public void deleteDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.delete( dataElementGroup );
    }

    @Override
    public DataElementGroup getDataElementGroup( int id )
    {
        return i18n( i18nService, dataElementGroupStore.get( id ) );
    }

    @Override
    public DataElementGroup getDataElementGroup( int id, boolean i18nDataElements )
    {
        DataElementGroup group = getDataElementGroup( id );

        if ( i18nDataElements )
        {
            i18n( i18nService, group.getMembers() );
        }

        return group;
    }

    @Override
    public List<DataElementGroup> getDataElementGroupsByUid( Collection<String> uids )
    {
        return i18n( i18nService, dataElementGroupStore.getByUid( uids ) );
    }

    @Override
    public DataElementGroup getDataElementGroup( String uid )
    {
        return i18n( i18nService, dataElementGroupStore.getByUid( uid ) );
    }

    @Override
    public List<DataElementGroup> getAllDataElementGroups()
    {
        return i18n( i18nService, dataElementGroupStore.getAll() );
    }

    @Override
    public DataElementGroup getDataElementGroupByName( String name )
    {
        List<DataElementGroup> dataElementGroups = dataElementGroupStore.getAllEqName( name );

        return !dataElementGroups.isEmpty() ? i18n( i18nService, dataElementGroups.get( 0 ) ) : null;
    }

    @Override
    public DataElementGroup getDataElementGroupByShortName( String shortName )
    {
        List<DataElementGroup> dataElementGroups = dataElementGroupStore.getAllEqShortName( shortName );

        if ( dataElementGroups.isEmpty() )
        {
            return null;
        }

        return i18n( i18nService, dataElementGroups.get( 0 ) );
    }

    @Override
    public DataElementGroup getDataElementGroupByCode( String code )
    {
        return i18n( i18nService, dataElementGroupStore.getByCode( code ) );
    }

    @Override
    public Set<DataElement> getDataElementsByGroupId( int groupId )
    {
        return i18n( i18nService, dataElementGroupStore.get( groupId ).getMembers() );
    }

    @Override
    public int getDataElementGroupCount()
    {
        return dataElementGroupStore.getCount();
    }

    @Override
    public int getDataElementGroupCountByName( String name )
    {
        return getCountByName( i18nService, dataElementGroupStore, name );
    }

    @Override
    public List<DataElementGroup> getDataElementGroupsBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, dataElementGroupStore, first, max );
    }

    @Override
    public List<DataElementGroup> getDataElementGroupsBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, dataElementGroupStore, name, first, max );
    }

    // -------------------------------------------------------------------------
    // DataElementGroupSet
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementGroupSet( DataElementGroupSet groupSet )
    {
        return dataElementGroupSetStore.save( groupSet );
    }

    @Override
    public void updateDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.update( groupSet );
    }

    @Override
    public void deleteDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.delete( groupSet );
    }

    @Override
    public DataElementGroupSet getDataElementGroupSet( int id )
    {
        return i18n( i18nService, dataElementGroupSetStore.get( id ) );
    }

    @Override
    public DataElementGroupSet getDataElementGroupSet( int id, boolean i18nGroups )
    {
        DataElementGroupSet groupSet = getDataElementGroupSet( id );

        if ( i18nGroups )
        {
            i18n( i18nService, groupSet.getDataElements() );
        }

        return groupSet;
    }

    @Override
    public DataElementGroupSet getDataElementGroupSet( String uid )
    {
        return i18n( i18nService, dataElementGroupSetStore.getByUid( uid ) );
    }

    @Override
    public DataElementGroupSet getDataElementGroupSetByName( String name )
    {
        List<DataElementGroupSet> dataElementGroupSets = dataElementGroupSetStore.getAllEqName( name );

        return !dataElementGroupSets.isEmpty() ? i18n( i18nService, dataElementGroupSets.get( 0 ) ) : null;
    }

    @Override
    public List<DataElementGroupSet> getCompulsoryDataElementGroupSetsWithMembers()
    {
        return getAllDataElementGroupSets().stream().filter( p -> p.isCompulsory() && p.hasDataElementGroups() ).collect( Collectors.toList() );
    }

    @Override
    public List<DataElementGroupSet> getAllDataElementGroupSets()
    {
        return i18n( i18nService, dataElementGroupSetStore.getAll() );
    }

    @Override
    public List<DataElementGroupSet> getDataElementGroupSetsByUid( Collection<String> uids )
    {
        return i18n( i18nService, dataElementGroupSetStore.getByUid( uids ) );
    }

    @Override
    public int getDataElementGroupSetCount()
    {
        return dataElementGroupSetStore.getCount();
    }

    @Override
    public int getDataElementGroupSetCountByName( String name )
    {
        return getCountByName( i18nService, dataElementGroupSetStore, name );
    }

    @Override
    public List<DataElementGroupSet> getDataElementGroupSetsBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, dataElementGroupSetStore, first, max );
    }

    @Override
    public List<DataElementGroupSet> getDataElementGroupSetsBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, dataElementGroupSetStore, name, first, max );
    }
}
