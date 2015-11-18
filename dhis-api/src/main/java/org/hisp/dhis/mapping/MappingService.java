package org.hisp.dhis.mapping;

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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Jan Henrik Overland
 * @version $Id$
 */
public interface MappingService
{
    String ID = MappingService.class.getName();

    String GEOJSON_DIR = "geojson";

    String MAP_LEGEND_SYMBOLIZER_COLOR = "color";
    String MAP_LEGEND_SYMBOLIZER_IMAGE = "image";

    String KEY_MAP_DATE_TYPE = "dateType";

    String MAP_DATE_TYPE_FIXED = "fixed";
    String MAP_DATE_TYPE_START_END = "start-end";

    String ORGANISATION_UNIT_SELECTION_TYPE_PARENT = "parent";
    String ORGANISATION_UNIT_SELECTION_TYPE_LEVEL = "level";

    String MAP_LAYER_TYPE_BASELAYER = "baselayer";
    String MAP_LAYER_TYPE_OVERLAY = "overlay";

    // -------------------------------------------------------------------------
    // Map
    // -------------------------------------------------------------------------

    int addMap( Map map );

    void updateMap( Map map );

    Map getMap( int id );

    Map getMap( String uid );

    Map getMapNoAcl( String uid );

    void deleteMap( Map map );

    List<Map> getMapsBetweenLikeName( String name, int first, int max );

    List<Map> getAllMaps();

    // -------------------------------------------------------------------------
    // MapView
    // -------------------------------------------------------------------------

    int addMapView( MapView mapView );

    void updateMapView( MapView mapView );

    void deleteMapView( MapView view );

    MapView getMapView( int id );

    MapView getMapView( String uid );

    MapView getMapViewByName( String name );

    MapView getIndicatorLastYearMapView( String indicatorUid, String organisationUnitUid, int level );

    List<MapView> getAllMapViews();

    List<MapView> getMapViewsBetweenByName( String name, int first, int max );

    // -------------------------------------------------------------------------
    // MapLayer
    // -------------------------------------------------------------------------

    int addMapLayer( MapLayer mapLayer );

    void updateMapLayer( MapLayer mapLayer );

    void addOrUpdateMapLayer( String name, String type, String url, String layers, String time, String fillColor,
        double fillOpacity, String strokeColor, int strokeWidth );

    void deleteMapLayer( MapLayer mapLayer );

    MapLayer getMapLayer( int id );

    MapLayer getMapLayer( String uid );

    MapLayer getMapLayerByName( String name );

    List<MapLayer> getMapLayersByType( String type );

    MapLayer getMapLayerByMapSource( String mapSource );

    List<MapLayer> getAllMapLayers();

    int countMapViewMaps( MapView mapView );

    int countDataSetMapViews( DataSet dataSet );

    int countIndicatorMapViews( Indicator indicator );

    int countDataElementMapViews( DataElement dataElement );
    
    int countPeriodMapViews( Period period );
    
    int countOrganisationUnitMapViews( OrganisationUnit organisationUnit );
}