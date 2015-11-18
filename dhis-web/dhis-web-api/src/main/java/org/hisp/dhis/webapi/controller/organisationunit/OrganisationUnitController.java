package org.hisp.dhis.webapi.controller.organisationunit;

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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitByLevelComparator;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.version.VersionService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetaData;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = OrganisationUnitSchemaDescriptor.API_ENDPOINT )
public class OrganisationUnitController
    extends AbstractCrudController<OrganisationUnit>
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private VersionService versionService;

    @Override
    @SuppressWarnings( "unchecked" )
    protected List<OrganisationUnit> getEntityList( WebMetaData metaData, WebOptions options, List<String> filters, List<Order> orders )
    {
        List<OrganisationUnit> entityList;
        boolean hasFilters = !filters.isEmpty();
        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders );
        query.setDefaultOrder();

        Integer level = options.getInt( "level" );
        Integer maxLevel = options.getInt( "maxLevel" );
        boolean levelSorted = options.isTrue( "levelSorted" );

        if ( options.isTrue( "userOnly" ) )
        {
            entityList = new ArrayList<>( currentUserService.getCurrentUser().getOrganisationUnits() );
        }
        else if ( options.isTrue( "userDataViewOnly" ) )
        {
            entityList = new ArrayList<>( currentUserService.getCurrentUser().getDataViewOrganisationUnits() );
        }
        else if ( options.isTrue( "userDataViewFallback" ) )
        {
            User user = currentUserService.getCurrentUser();

            if ( user != null && user.hasDataViewOrganisationUnit() )
            {
                entityList = new ArrayList<>( user.getDataViewOrganisationUnits() );
            }
            else
            {
                entityList = new ArrayList<>( organisationUnitService.getOrganisationUnitsAtLevel( 1 ) );
            }
        }
        else if ( ObjectUtils.firstNonNull( level, maxLevel ) != null )
        {
            OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
            params.setLevel( level );
            params.setMaxLevels( maxLevel );

            entityList = organisationUnitService.getOrganisationUnitsByQuery( params );
        }
        else if ( levelSorted )
        {
            entityList = new ArrayList<>( manager.getAll( getEntityClass() ) );
            Collections.sort( entityList, OrganisationUnitByLevelComparator.INSTANCE );
        }
        else if ( options.hasPaging() && !hasFilters )
        {
            int count = queryService.count( query );

            Pager pager = new Pager( options.getPage(), count, options.getPageSize() );
            metaData.setPager( pager );

            query.setFirstResult( pager.getOffset() );
            query.setMaxResults( pager.getPageSize() );
            entityList = (List<OrganisationUnit>) queryService.query( query ).getItems();
        }
        else
        {
            entityList = (List<OrganisationUnit>) queryService.query( query ).getItems();
        }

        return entityList;
    }

    @Override
    protected List<OrganisationUnit> getEntity( String uid, WebOptions options )
    {
        OrganisationUnit organisationUnit = manager.get( getEntityClass(), uid );

        List<OrganisationUnit> organisationUnits = Lists.newArrayList();

        if ( organisationUnit == null )
        {
            return organisationUnits;
        }

        if ( options.contains( "includeChildren" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            organisationUnits.add( organisationUnit );
            organisationUnits.addAll( organisationUnit.getChildren() );
        }
        else if ( options.contains( "includeDescendants" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitWithChildren( uid ) );
        }
        else if ( options.contains( "includeAncestors" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            organisationUnits.add( organisationUnit );
            List<OrganisationUnit> ancestors = organisationUnit.getAncestors();
            Collections.reverse( ancestors );
            organisationUnits.addAll( ancestors );
        }
        else if ( options.contains( "level" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            int level = options.getInt( "level" );
            int ouLevel = organisationUnit.getLevel();
            int targetLevel = ouLevel + level;
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitsAtLevel( targetLevel, organisationUnit ) );
        }
        else
        {
            organisationUnits.add( organisationUnit );
        }

        return organisationUnits;
    }

    @RequestMapping( value = "/{uid}/parents", method = RequestMethod.GET )
    public List<OrganisationUnit> getEntityList( @PathVariable( "uid" ) String uid,
        @RequestParam Map<String, String> parameters, Model model, TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        OrganisationUnit organisationUnit = manager.get( getEntityClass(), uid );
        List<OrganisationUnit> organisationUnits = Lists.newArrayList();
        translate( organisationUnits, translateParams );

        if ( organisationUnit != null )
        {
            OrganisationUnit organisationUnitParent = organisationUnit.getParent();

            while ( organisationUnitParent != null )
            {
                organisationUnits.add( organisationUnitParent );
                organisationUnitParent = organisationUnitParent.getParent();
            }
        }

        WebMetaData metaData = new WebMetaData();
        metaData.setOrganisationUnits( organisationUnits );
        WebOptions options = new WebOptions( parameters );

        model.addAttribute( "model", metaData );
        model.addAttribute( "viewClass", options.getViewClass( "basic" ) );

        return organisationUnits;
    }

    @RequestMapping( value = "", method = RequestMethod.GET, produces = { "application/json+geo", "application/json+geojson" } )
    public void getGeoJson(
        @RequestParam( value = "level", required = false ) List<Integer> rpLevels,
        @RequestParam( value = "parent", required = false ) List<String> rpParents,
        @RequestParam( value = "properties", required = false, defaultValue = "true" ) boolean rpProperties,
        HttpServletResponse response ) throws IOException
    {
        rpLevels = rpLevels != null ? rpLevels : new ArrayList<>();
        rpParents = rpParents != null ? rpParents : new ArrayList<>();

        List<OrganisationUnit> parents = new ArrayList<>( manager.getByUid( OrganisationUnit.class, rpParents ) );

        if ( rpLevels.isEmpty() )
        {
            rpLevels.add( 1 );
        }

        if ( parents.isEmpty() )
        {
            parents.addAll( organisationUnitService.getRootOrganisationUnits() );
        }

        List<OrganisationUnit> organisationUnits = new ArrayList<>( organisationUnitService.getOrganisationUnitsAtLevels( rpLevels, parents ) );

        response.setContentType( "application/json" );

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator generator = jsonFactory.createGenerator( response.getOutputStream() );

        generator.writeStartObject();
        generator.writeStringField( "type", "FeatureCollection" );
        generator.writeArrayFieldStart( "features" );

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            writeFeature( generator, organisationUnit, rpProperties );
        }

        generator.writeEndArray();
        generator.writeEndObject();

        generator.close();
    }

    public void writeFeature( JsonGenerator generator, OrganisationUnit organisationUnit, boolean includeProperties ) throws IOException
    {
        if ( organisationUnit.getFeatureType() == null || organisationUnit.getCoordinates() == null )
        {
            return;
        }

        FeatureType featureType = organisationUnit.getFeatureType();

        // if featureType is anything other than Point, just assume MultiPolygon
        if ( !(featureType == FeatureType.POINT) )
        {
            featureType = FeatureType.MULTI_POLYGON;
        }

        generator.writeStartObject();

        generator.writeStringField( "type", "Feature" );
        generator.writeStringField( "id", organisationUnit.getUid() );

        generator.writeObjectFieldStart( "geometry" );
        generator.writeObjectField( "type", featureType.value() );

        generator.writeFieldName( "coordinates" );
        generator.writeRawValue( organisationUnit.getCoordinates() );

        generator.writeEndObject();

        generator.writeObjectFieldStart( "properties" );

        if ( includeProperties )
        {
            Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

            generator.writeStringField( "code", organisationUnit.getCode() );
            generator.writeStringField( "name", organisationUnit.getName() );
            generator.writeStringField( "level", String.valueOf( organisationUnit.getLevel() ) );
            generator.writeStringField( "parent", organisationUnit.getParent().getUid() );
            generator.writeStringField( "parentGraph", organisationUnit.getParentGraph( roots ) );
        }

        generator.writeEndObject();

        generator.writeEndObject();
    }

    @Override
    protected void postCreateEntity( OrganisationUnit entity )
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }

    @Override
    protected void postUpdateEntity( OrganisationUnit entity )
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }

    @Override
    protected void postDeleteEntity()
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }
}
