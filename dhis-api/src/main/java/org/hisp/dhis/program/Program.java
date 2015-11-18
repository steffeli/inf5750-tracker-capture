package org.hisp.dhis.program;

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
import com.google.common.collect.Sets;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.common.annotation.Scanned;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.validation.ValidationCriteria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "program", namespace = DxfNamespaces.DXF_2_0 )
public class Program
    extends BaseIdentifiableObject
    implements VersionedObject
{
    private String description;

    private int version;

    private String enrollmentDateLabel;

    private String incidentDateLabel;

    @Scanned
    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    @Scanned
    private Set<ProgramStage> programStages = new HashSet<>();

    @Scanned
    private Set<ValidationCriteria> validationCriteria = new HashSet<>();

    private ProgramType programType;

    private Boolean displayIncidentDate = true;

    private Boolean ignoreOverdueEvents = false;

    private List<ProgramTrackedEntityAttribute> programAttributes = new ArrayList<>();

    @Scanned
    private Set<UserAuthorityGroup> userRoles = new HashSet<>();

    @Scanned
    private Set<ProgramIndicator> programIndicators = new HashSet<>();

    private Boolean onlyEnrollOnce = false;

    @Scanned
    private Set<TrackedEntityInstanceReminder> instanceReminders = new HashSet<>();

    private Boolean selectEnrollmentDatesInFuture = false;

    private Boolean selectIncidentDatesInFuture = false;

    private String relationshipText;

    private RelationshipType relationshipType;

    private Boolean relationshipFromA = false;

    private Program relatedProgram;

    private Boolean dataEntryMethod = false;

    private TrackedEntity trackedEntity;

    /**
     * Set of the dynamic attributes values that belong to this data element.
     */
    private Set<AttributeValue> attributeValues = new HashSet<>();

    private DataEntryForm dataEntryForm;

    /**
     * The CategoryCombo used for data attributes.
     */
    private DataElementCategoryCombo categoryCombo;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Program()
    {

    }

    public Program( String name, String description )
    {
        this.name = name;
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Logic methods
    // -------------------------------------------------------------------------

    public void addOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.add( organisationUnit );
        organisationUnit.getPrograms().add( this );
    }

    public void removeOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.remove( organisationUnit );
        organisationUnit.getPrograms().remove( this );
    }

    public void updateOrganisationUnits( Set<OrganisationUnit> updates )
    {
        Set<OrganisationUnit> toRemove = Sets.difference( organisationUnits, updates );
        Set<OrganisationUnit> toAdd = Sets.difference( updates, organisationUnits );

        toRemove.parallelStream().forEach( u -> u.getPrograms().remove( this ) );
        toAdd.parallelStream().forEach( u -> u.getPrograms().add( this ) );

        organisationUnits.clear();
        organisationUnits.addAll( updates );
    }

    /**
     * Returns the ProgramTrackedEntityAttribute of this Program which contains
     * the given TrackedEntityAttribute.
     */
    public ProgramTrackedEntityAttribute getAttribute( TrackedEntityAttribute attribute )
    {
        for ( ProgramTrackedEntityAttribute programAttribute : programAttributes )
        {
            if ( programAttribute != null && programAttribute.getAttribute().equals( attribute ) )
            {
                return programAttribute;
            }
        }

        return null;
    }

    /**
     * Returns all data elements which are part of the stages of this program.
     */
    public Set<DataElement> getAllDataElements()
    {
        Set<DataElement> elements = new HashSet<>();

        for ( ProgramStage stage : programStages )
        {
            elements.addAll( stage.getAllDataElements() );
        }

        return elements;
    }

    /**
     * Returns data elements which are part of the stages of this program which
     * have a legend set and is of numeric value type.
     */
    public Set<DataElement> getDataElementsWithLegendSet()
    {
        Set<DataElement> elements = new HashSet<>();

        for ( DataElement element : getAllDataElements() )
        {
            if ( element != null && element.hasLegendSet() && element.isNumericType() )
            {
                elements.add( element );
            }
        }

        return elements;
    }

    /**
     * Returns TrackedEntityAttributes from ProgramTrackedEntityAttributes. Use
     * getAttributes() to access the persisted attribute list.
     */
    public List<TrackedEntityAttribute> getTrackedEntityAttributes()
    {
        List<TrackedEntityAttribute> attributes = new ArrayList<>();

        for ( ProgramTrackedEntityAttribute programAttribute : programAttributes )
        {
            attributes.add( programAttribute.getAttribute() );
        }

        return attributes;
    }

    /**
     * Returns TrackedEntityAttributes from ProgramTrackedEntityAttributes which
     * have a legend set and is of numeric value type.
     */
    public List<TrackedEntityAttribute> getTrackedEntityAttributesWithLegendSet()
    {
        List<TrackedEntityAttribute> attributes = new ArrayList<>();

        for ( TrackedEntityAttribute attribute : getTrackedEntityAttributes() )
        {
            if ( attribute != null && attribute.hasLegendSet() && attribute.isNumericType() )
            {
                attributes.add( attribute );
            }
        }

        return attributes;
    }

    public ProgramStage getProgramStageByStage( int stage )
    {
        int count = 1;

        for ( ProgramStage programStage : programStages )
        {
            if ( count == stage )
            {
                return programStage;
            }

            count++;
        }

        return null;
    }

    public boolean isSingleProgramStage()
    {
        return programStages != null && programStages.size() == 1;
    }

    @Override
    public int increaseVersion()
    {
        return ++version;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Override
    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getVersion()
    {
        return version;
    }

    @Override
    public void setVersion( int version )
    {
        this.version = version;
    }

    @JsonProperty( "organisationUnits" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    @JsonProperty( "programStages" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "programStages", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programStage", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramStage> getProgramStages()
    {
        return programStages;
    }

    public void setProgramStages( Set<ProgramStage> programStages )
    {
        this.programStages = programStages;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getEnrollmentDateLabel()
    {
        return enrollmentDateLabel;
    }

    public void setEnrollmentDateLabel( String enrollmentDateLabel )
    {
        this.enrollmentDateLabel = enrollmentDateLabel;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getIncidentDateLabel()
    {
        return incidentDateLabel;
    }

    public void setIncidentDateLabel( String incidentDateLabel )
    {
        this.incidentDateLabel = incidentDateLabel;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramType getProgramType()
    {
        return programType;
    }

    public void setProgramType( ProgramType programType )
    {
        this.programType = programType;
    }

    @JsonProperty( "validationCriterias" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "validationCriterias", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "validationCriteria", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ValidationCriteria> getValidationCriteria()
    {
        return validationCriteria;
    }

    public void setValidationCriteria( Set<ValidationCriteria> validationCriteria )
    {
        this.validationCriteria = validationCriteria;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayIncidentDate()
    {
        return displayIncidentDate;
    }

    public void setDisplayIncidentDate( Boolean displayIncidentDate )
    {
        this.displayIncidentDate = displayIncidentDate;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getIgnoreOverdueEvents()
    {
        return ignoreOverdueEvents;
    }

    public void setIgnoreOverdueEvents( Boolean ignoreOverdueEvents )
    {
        this.ignoreOverdueEvents = ignoreOverdueEvents;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRegistration()
    {
        return programType == ProgramType.WITH_REGISTRATION;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isWithoutRegistration()
    {
        return programType == ProgramType.WITHOUT_REGISTRATION;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "userRoles", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userRole", namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserAuthorityGroup> getUserRoles()
    {
        return userRoles;
    }

    public void setUserRoles( Set<UserAuthorityGroup> userRoles )
    {
        this.userRoles = userRoles;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "programIndicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramIndicator> getProgramIndicators()
    {
        return programIndicators;
    }

    public void setProgramIndicators( Set<ProgramIndicator> programIndicators )
    {
        this.programIndicators = programIndicators;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getOnlyEnrollOnce()
    {
        return onlyEnrollOnce;
    }

    public void setOnlyEnrollOnce( Boolean onlyEnrollOnce )
    {
        this.onlyEnrollOnce = onlyEnrollOnce;
    }

    @JsonProperty( "trackedEntityInstanceReminders" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "trackedEntityInstanceReminders", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntityInstanceReminder", namespace = DxfNamespaces.DXF_2_0 )
    public Set<TrackedEntityInstanceReminder> getInstanceReminders()
    {
        return instanceReminders;
    }

    public void setInstanceReminders( Set<TrackedEntityInstanceReminder> instanceReminders )
    {
        this.instanceReminders = instanceReminders;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getSelectEnrollmentDatesInFuture()
    {
        return selectEnrollmentDatesInFuture;
    }

    public void setSelectEnrollmentDatesInFuture( Boolean selectEnrollmentDatesInFuture )
    {
        this.selectEnrollmentDatesInFuture = selectEnrollmentDatesInFuture;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getSelectIncidentDatesInFuture()
    {
        return selectIncidentDatesInFuture;
    }

    public void setSelectIncidentDatesInFuture( Boolean selectIncidentDatesInFuture )
    {
        this.selectIncidentDatesInFuture = selectIncidentDatesInFuture;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getRelationshipText()
    {
        return relationshipText;
    }

    public void setRelationshipText( String relationshipText )
    {
        this.relationshipText = relationshipText;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public RelationshipType getRelationshipType()
    {
        return relationshipType;
    }

    public void setRelationshipType( RelationshipType relationshipType )
    {
        this.relationshipType = relationshipType;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getRelatedProgram()
    {
        return relatedProgram;
    }

    public void setRelatedProgram( Program relatedProgram )
    {
        this.relatedProgram = relatedProgram;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getRelationshipFromA()
    {
        return relationshipFromA;
    }

    public void setRelationshipFromA( Boolean relationshipFromA )
    {
        this.relationshipFromA = relationshipFromA;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDataEntryMethod()
    {
        return dataEntryMethod;
    }

    public void setDataEntryMethod( Boolean dataEntryMethod )
    {
        this.dataEntryMethod = dataEntryMethod;
    }

    @JsonProperty( "programTrackedEntityAttributes" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "programTrackedEntityAttributes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programTrackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramTrackedEntityAttribute> getProgramAttributes()
    {
        return programAttributes;
    }

    public void setProgramAttributes( List<ProgramTrackedEntityAttribute> programAttributes )
    {
        this.programAttributes = programAttributes;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "trackedEntity", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntity", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntity getTrackedEntity()
    {
        return trackedEntity;
    }

    public void setTrackedEntity( TrackedEntity trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    @JsonProperty( "attributeValues" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "attributeValues", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attributeValue", namespace = DxfNamespaces.DXF_2_0 )
    public Set<AttributeValue> getAttributeValues()
    {
        return attributeValues;
    }

    public void setAttributeValues( Set<AttributeValue> attributeValues )
    {
        this.attributeValues = attributeValues;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( localName = "dataEntryForm", namespace = DxfNamespaces.DXF_2_0 )
    public DataEntryForm getDataEntryForm()
    {
        return dataEntryForm;
    }

    public void setDataEntryForm( DataEntryForm dataEntryForm )
    {
        this.dataEntryForm = dataEntryForm;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public void setCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }

    /**
     * Indicates whether this program has a category combination which is different
     * from the default category combination.
     */
    public boolean hasCategoryCombo()
    {
        return categoryCombo != null && !DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME.equals( categoryCombo.getName() );
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            Program program = (Program) other;

            version = program.getVersion();

            if ( strategy.isReplace() )
            {
                description = program.getDescription();
                enrollmentDateLabel = program.getEnrollmentDateLabel();
                incidentDateLabel = program.getIncidentDateLabel();
                programType = program.getProgramType();
                displayIncidentDate = program.getDisplayIncidentDate();
                ignoreOverdueEvents = program.getIgnoreOverdueEvents();
                onlyEnrollOnce = program.getOnlyEnrollOnce();
                selectEnrollmentDatesInFuture = program.getSelectEnrollmentDatesInFuture();
                selectIncidentDatesInFuture = program.getSelectIncidentDatesInFuture();
                relationshipText = program.getRelationshipText();
                relationshipType = program.getRelationshipType();
                relationshipFromA = program.getRelationshipFromA();
                relatedProgram = program.getRelatedProgram();
                dataEntryMethod = program.getDataEntryMethod();
                trackedEntity = program.getTrackedEntity();
            }
            else if ( strategy.isMerge() )
            {
                description = program.getDescription() == null ? description : program.getDescription();
                enrollmentDateLabel = program.getEnrollmentDateLabel() == null ? enrollmentDateLabel : program.getEnrollmentDateLabel();
                incidentDateLabel = program.getIncidentDateLabel() == null ? incidentDateLabel : program.getIncidentDateLabel();
                programType = program.getProgramType() == null ? programType : program.getProgramType();
                displayIncidentDate = program.getDisplayIncidentDate() == null ? displayIncidentDate : program.getDisplayIncidentDate();
                ignoreOverdueEvents = program.getIgnoreOverdueEvents() == null ? ignoreOverdueEvents : program.getIgnoreOverdueEvents();
                onlyEnrollOnce = program.getOnlyEnrollOnce() == null ? onlyEnrollOnce : program.getOnlyEnrollOnce();
                selectEnrollmentDatesInFuture = program.getSelectEnrollmentDatesInFuture() == null ? selectEnrollmentDatesInFuture : program.getSelectEnrollmentDatesInFuture();
                selectIncidentDatesInFuture = program.getSelectIncidentDatesInFuture() == null ? selectIncidentDatesInFuture : program.getSelectIncidentDatesInFuture();
                relationshipText = program.getRelationshipText() == null ? relationshipText : program.getRelationshipText();
                relationshipType = program.getRelationshipType() == null ? relationshipType : program.getRelationshipType();
                relationshipFromA = program.getRelationshipFromA() == null ? relationshipFromA : program.getRelationshipFromA();
                relatedProgram = program.getRelatedProgram() == null ? relatedProgram : program.getRelatedProgram();
                dataEntryMethod = program.getDataEntryMethod() == null ? dataEntryMethod : program.getDataEntryMethod();
                trackedEntity = program.getTrackedEntity() == null ? trackedEntity : program.getTrackedEntity();
            }

            organisationUnits.clear();
            organisationUnits.addAll( program.getOrganisationUnits() );

            programStages.clear();

            for ( ProgramStage programStage : program.getProgramStages() )
            {
                programStages.add( programStage );
                programStage.setProgram( this );
            }

            validationCriteria.clear();
            validationCriteria.addAll( program.getValidationCriteria() );

            programAttributes.clear();
            programAttributes.addAll( program.getProgramAttributes() );

            userRoles.clear();
            userRoles.addAll( program.getUserRoles() );

            instanceReminders.clear();
            instanceReminders.addAll( program.getInstanceReminders() );

            attributeValues.clear();
            attributeValues.addAll( program.getAttributeValues() );
        }
    }
}
