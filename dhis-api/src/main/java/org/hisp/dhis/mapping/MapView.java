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

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.DimensionalView;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * For analytical data, organisation units and indicators/data elements are
 * dimensions, and period is filter.
 *
 * @author Jan Henrik Overland
 */
@JacksonXmlRootElement( localName = "mapView", namespace = DxfNamespaces.DXF_2_0 )
public class MapView
    extends BaseAnalyticalObject
{
    public static final String LAYER_BOUNDARY = "boundary";
    public static final String LAYER_FACILITY = "facility";
    public static final String LAYER_SYMBOL = "symbol";
    public static final String LAYER_THEMATIC1 = "thematic1";
    public static final String LAYER_THEMATIC2 = "thematic2";
    public static final String LAYER_THEMATIC3 = "thematic3";
    public static final String LAYER_THEMATIC4 = "thematic4";

    public static final Integer METHOD_EQUAL_INTERVALS = 2;
    public static final Integer METHOD_EQUAL_COUNTS = 3;

    public static final List<String> DATA_LAYERS = Arrays.asList(
        LAYER_THEMATIC1, LAYER_THEMATIC2, LAYER_THEMATIC3, LAYER_THEMATIC4 );

    private String layer;

    private Integer method;

    private Integer classes;

    private String colorLow;

    private String colorHigh;

    private LegendSet legendSet;

    private Integer radiusLow;

    private Integer radiusHigh;

    private Double opacity;

    private OrganisationUnitGroupSet organisationUnitGroupSet;

    private Integer areaRadius;

    private Boolean hidden;

    private Boolean labels;

    private String labelFontSize;

    private String labelFontWeight;

    private String labelFontStyle;

    private String labelFontColor;

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    private transient I18nFormat format;

    private transient String parentGraph;

    private transient int parentLevel;

    private transient List<OrganisationUnit> organisationUnitsAtLevel = new ArrayList<>();

    private transient List<OrganisationUnit> organisationUnitsInGroups = new ArrayList<>();

    public MapView()
    {
    }

    // -------------------------------------------------------------------------
    // Analytical
    // -------------------------------------------------------------------------

    @Override
    public void init( User user, Date date, OrganisationUnit organisationUnit,
        List<OrganisationUnit> organisationUnitsAtLevel, List<OrganisationUnit> organisationUnitsInGroups, I18nFormat format )
    {
        this.user = user;
        this.relativePeriodDate = date;
        this.relativeOrganisationUnit = organisationUnit;
        this.organisationUnitsAtLevel = organisationUnitsAtLevel;
        this.organisationUnitsInGroups = organisationUnitsInGroups;
    }

    @Override
    public void populateAnalyticalProperties()
    {
        columns.add( getDimensionalObject( DimensionalObject.DATA_X_DIM_ID ) );
        rows.add( getDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID ) );
        
        if ( !periods.isEmpty() || hasRelativePeriods() )
        {
            filters.add( getDimensionalObject( DimensionalObject.PERIOD_DIM_ID ) );
        }
    }

    public List<OrganisationUnit> getAllOrganisationUnits()
    {
        DimensionalObject object = getDimensionalObject( ORGUNIT_DIM_ID, relativePeriodDate, user, true, organisationUnitsAtLevel, organisationUnitsInGroups, format );

        return object != null ? NameableObjectUtils.asTypedList( object.getItems(), OrganisationUnit.class ) : null;
    }

    public boolean hasLegendSet()
    {
        return legendSet != null;
    }

    public boolean hasColors()
    {
        return colorLow != null && !colorLow.trim().isEmpty() && colorHigh != null && !colorHigh.trim().isEmpty();
    }

    public boolean isDataLayer()
    {
        return DATA_LAYERS.contains( layer );
    }

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @Override
    public String getName()
    {
        if ( !dataDimensionItems.isEmpty() && dataDimensionItems.get( 0 ).getNameableObject() != null )
        {
            return dataDimensionItems.get( 0 ).getNameableObject().getName();
        }
        
        return uid;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLayer()
    {
        return layer;
    }

    public void setLayer( String layer )
    {
        this.layer = layer;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getMethod()
    {
        return method;
    }

    public void setMethod( Integer method )
    {
        this.method = method;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getClasses()
    {
        return classes;
    }

    public void setClasses( Integer classes )
    {
        this.classes = classes;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.COLOR )
    public String getColorLow()
    {
        return colorLow;
    }

    public void setColorLow( String colorLow )
    {
        this.colorLow = colorLow;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.COLOR )
    public String getColorHigh()
    {
        return colorHigh;
    }

    public void setColorHigh( String colorHigh )
    {
        this.colorHigh = colorHigh;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getRadiusLow()
    {
        return radiusLow;
    }

    public void setRadiusLow( Integer radiusLow )
    {
        this.radiusLow = radiusLow;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getRadiusHigh()
    {
        return radiusHigh;
    }

    public void setRadiusHigh( Integer radiusHigh )
    {
        this.radiusHigh = radiusHigh;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getOpacity()
    {
        return opacity;
    }

    public void setOpacity( Double opacity )
    {
        this.opacity = opacity;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnitGroupSet getOrganisationUnitGroupSet()
    {
        return organisationUnitGroupSet;
    }

    public void setOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        this.organisationUnitGroupSet = organisationUnitGroupSet;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getAreaRadius()
    {
        return areaRadius;
    }

    public void setAreaRadius( Integer areaRadius )
    {
        this.areaRadius = areaRadius;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getHidden()
    {
        return hidden;
    }

    public void setLabels( Boolean labels )
    {
        this.labels = labels;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getLabels()
    {
        return labels;
    }

    public void setHidden( Boolean hidden )
    {
        this.hidden = hidden;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontSize()
    {
        return labelFontSize;
    }

    public void setLabelFontSize( String labelFontSize )
    {
        this.labelFontSize = labelFontSize;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontWeight()
    {
        return labelFontWeight;
    }

    public void setLabelFontWeight( String labelFontWeight )
    {
        this.labelFontWeight = labelFontWeight;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontStyle()
    {
        return labelFontStyle;
    }

    public void setLabelFontStyle( String labelFontStyle )
    {
        this.labelFontStyle = labelFontStyle;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class, DimensionalView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontColor()
    {
        return labelFontColor;
    }

    public void setLabelFontColor( String labelFontColor )
    {
        this.labelFontColor = labelFontColor;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getParentGraph()
    {
        return parentGraph;
    }

    public void setParentGraph( String parentGraph )
    {
        this.parentGraph = parentGraph;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getParentLevel()
    {
        return parentLevel;
    }

    public void setParentLevel( int parentLevel )
    {
        this.parentLevel = parentLevel;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            MapView mapView = (MapView) other;

            if ( strategy.isReplace() )
            {
                layer = mapView.getLayer();
                method = mapView.getMethod();
                classes = mapView.getClasses();
                colorLow = mapView.getColorLow();
                colorHigh = mapView.getColorHigh();
                legendSet = mapView.getLegendSet();
                radiusLow = mapView.getRadiusLow();
                radiusHigh = mapView.getRadiusHigh();
                opacity = mapView.getOpacity();
                organisationUnitGroupSet = mapView.getOrganisationUnitGroupSet();
                areaRadius = mapView.getAreaRadius();
                hidden = mapView.getHidden();
                labels = mapView.getLabels();
                labelFontSize = mapView.getLabelFontSize();
                labelFontWeight = mapView.getLabelFontWeight();
                labelFontStyle = mapView.getLabelFontStyle();
                labelFontColor = mapView.getLabelFontColor();
            }
            else if ( strategy.isMerge() )
            {
                layer = mapView.getLayer() == null ? layer : mapView.getLayer();
                method = mapView.getMethod() == null ? method : mapView.getMethod();
                classes = mapView.getClasses() == null ? classes : mapView.getClasses();
                colorLow = mapView.getColorLow() == null ? colorLow : mapView.getColorLow();
                colorHigh = mapView.getColorHigh() == null ? colorHigh : mapView.getColorHigh();
                legendSet = mapView.getLegendSet() == null ? legendSet : mapView.getLegendSet();
                radiusLow = mapView.getRadiusLow() == null ? radiusLow : mapView.getRadiusLow();
                radiusHigh = mapView.getRadiusHigh() == null ? radiusHigh : mapView.getRadiusHigh();
                opacity = mapView.getOpacity() == null ? opacity : mapView.getOpacity();
                organisationUnitGroupSet = mapView.getOrganisationUnitGroupSet() == null ? organisationUnitGroupSet : mapView.getOrganisationUnitGroupSet();
                areaRadius = mapView.getAreaRadius() == null ? areaRadius : mapView.getAreaRadius();
                hidden = mapView.getHidden() == null ? hidden : mapView.getHidden();
                labels = mapView.getLabels() == null ? labels : mapView.getLabels();
                labelFontSize = mapView.getLabelFontSize() == null ? labelFontSize : mapView.getLabelFontSize();
                labelFontWeight = mapView.getLabelFontWeight() == null ? labelFontWeight : mapView.getLabelFontWeight();
                labelFontStyle = mapView.getLabelFontStyle() == null ? labelFontStyle : mapView.getLabelFontStyle();
                labelFontColor = mapView.getLabelFontColor() == null ? labelFontColor : mapView.getLabelFontColor();
            }
        }
    }
}
