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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.annotation.Scanned;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Aselefew
 */
@JacksonXmlRootElement( localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementCategoryCombo
    extends BaseIdentifiableObject
{
    public static final String DEFAULT_CATEGORY_COMBO_NAME = "default";

    /**
     * A set with categories.
     */
    @Scanned
    private List<DataElementCategory> categories = new ArrayList<>();

    /**
     * A set of category option combos. Use getSortedOptionCombos() to get a
     * sorted list of category option combos.
     */
    private Set<DataElementCategoryOptionCombo> optionCombos = new HashSet<>();

    private DataDimensionType dataDimensionType;

    private boolean skipTotal;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementCategoryCombo()
    {
    }

    public DataElementCategoryCombo( String name )
    {
        this.name = name;
    }

    public DataElementCategoryCombo( String name, List<DataElementCategory> categories )
    {
        this( name );
        this.categories = categories;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @JsonProperty( "isDefault" )
    @JsonView( { DetailedView.class } )
    public boolean isDefault()
    {
        return name.equals( DEFAULT_CATEGORY_COMBO_NAME );
    }

    /**
     * Indicates whether this category combo has at least one category, has at
     * least one category option combo and that all categories have at least one
     * category option.
     */
    public boolean isValid()
    {
        if ( categories == null || categories.isEmpty() || optionCombos == null || optionCombos.isEmpty() )
        {
            return false;
        }

        for ( DataElementCategory category : categories )
        {
            if ( category == null || category.getCategoryOptions() == null || category.getCategoryOptions().isEmpty() )
            {
                return false;
            }
        }

        return true;
    }

    public List<DataElementCategoryOption> getCategoryOptions()
    {
        final List<DataElementCategoryOption> categoryOptions = new ArrayList<>();

        for ( DataElementCategory category : categories )
        {
            categoryOptions.addAll( category.getCategoryOptions() );
        }

        return categoryOptions;
    }

    public boolean doTotal()
    {
        return optionCombos != null && optionCombos.size() > 1 && !skipTotal;
    }

    public boolean doSubTotals()
    {
        return categories != null && categories.size() > 1;
    }

    public DataElementCategoryOption[][] getCategoryOptionsAsArray()
    {
        DataElementCategoryOption[][] arrays = new DataElementCategoryOption[categories.size()][];

        int i = 0;

        for ( DataElementCategory category : categories )
        {
            arrays[i++] = new ArrayList<>(
                category.getCategoryOptions() ).toArray( new DataElementCategoryOption[0] );
        }

        return arrays;
    }

    public List<DataElementCategoryOptionCombo> generateOptionCombosList()
    {
        List<DataElementCategoryOptionCombo> list = new ArrayList<>();

        CombinationGenerator<DataElementCategoryOption> generator =
            new CombinationGenerator<>( getCategoryOptionsAsArray() );

        while ( generator.hasNext() )
        {
            DataElementCategoryOptionCombo optionCombo = new DataElementCategoryOptionCombo();
            optionCombo.setCategoryOptions( new HashSet<>( generator.getNext() ) );
            optionCombo.setCategoryCombo( this );
            list.add( optionCombo );
        }

        return list;
    }

    public List<DataElementCategoryOptionCombo> getSortedOptionCombos()
    {
        List<DataElementCategoryOptionCombo> list = new ArrayList<>();

        CombinationGenerator<DataElementCategoryOption> generator =
            new CombinationGenerator<>( getCategoryOptionsAsArray() );

        while ( generator.hasNext() )
        {
            List<DataElementCategoryOption> categoryOptions = generator.getNext();

            Set<DataElementCategoryOption> categoryOptionSet = new HashSet<>( categoryOptions );

            for ( DataElementCategoryOptionCombo optionCombo : optionCombos )
            {
                if ( optionCombo.getCategoryOptions() != null && optionCombo.getCategoryOptions().equals( categoryOptionSet ) )
                {
                    list.add( optionCombo );
                    continue;
                }
            }
        }

        return list;
    }

    public void generateOptionCombos()
    {
        this.optionCombos = new HashSet<>( generateOptionCombosList() );

        for ( DataElementCategoryOptionCombo optionCombo : optionCombos )
        {
            for ( DataElementCategoryOption categoryOption : optionCombo.getCategoryOptions() )
            {
                categoryOption.addCategoryOptionCombo( optionCombo );
            }
        }
    }

    public boolean hasOptionCombos()
    {
        return optionCombos != null && !optionCombos.isEmpty();
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public boolean isAutoGenerated()
    {
        return name != null && name.equals( DEFAULT_CATEGORY_COMBO_NAME );
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElementCategory( DataElementCategory dataElementCategory )
    {
        categories.add( dataElementCategory );
    }

    public void removeDataElementCategory( DataElementCategory dataElementCategory )
    {
        categories.remove( dataElementCategory );
    }

    public void removeAllDataElementCategories()
    {
        categories.clear();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "categories", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "category", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElementCategory> getCategories()
    {
        return categories;
    }

    public void setCategories( List<DataElementCategory> categories )
    {
        this.categories = categories;
    }

    @JsonProperty( "categoryOptionCombos" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class } )
    @JacksonXmlElementWrapper( localName = "categoryOptionCombos", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementCategoryOptionCombo> getOptionCombos()
    {
        return optionCombos;
    }

    public void setOptionCombos( Set<DataElementCategoryOptionCombo> optionCombos )
    {
        this.optionCombos = optionCombos;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataDimensionType getDataDimensionType()
    {
        return dataDimensionType;
    }

    public void setDataDimensionType( DataDimensionType dataDimensionType )
    {
        this.dataDimensionType = dataDimensionType;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipTotal()
    {
        return skipTotal;
    }

    public void setSkipTotal( boolean skipTotal )
    {
        this.skipTotal = skipTotal;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            DataElementCategoryCombo categoryCombo = (DataElementCategoryCombo) other;

            skipTotal = categoryCombo.isSkipTotal();

            if ( strategy.isReplace() )
            {
                dataDimensionType = categoryCombo.getDataDimensionType();
            }
            else if ( strategy.isMerge() )
            {
                dataDimensionType = categoryCombo.getDataDimensionType() == null ? dataDimensionType : categoryCombo.getDataDimensionType();
            }

            removeAllDataElementCategories();

            for ( DataElementCategory dataElementCategory : categoryCombo.getCategories() )
            {
                addDataElementCategory( dataElementCategory );
            }
        }
    }
}
