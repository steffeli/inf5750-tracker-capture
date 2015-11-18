package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dxf2.common.Options;
import org.hisp.dhis.dxf2.metadata.ExportService;
import org.hisp.dhis.dxf2.metadata.MetaData;
import org.hisp.dhis.dxf2.render.RenderService;
import org.hisp.dhis.dxf2.schema.SchemaValidator;
import org.hisp.dhis.dxf2.schema.ValidationViolation;
import org.hisp.dhis.maintenance.MaintenanceService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = MaintenanceController.RESOURCE_PATH )
public class MaintenanceController
{
    public static final String RESOURCE_PATH = "/maintenance";

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private HibernateCacheManager cacheManager;

    @Autowired
    private PartitionManager partitionManager;

    @Autowired
    private SchemaValidator schemaValidator;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private ResourceTableService resourceTableService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @RequestMapping( value = "/ouPathUpdate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void forceUpdatePaths()
    {
        organisationUnitService.forceUpdatePaths();
    }

    @RequestMapping( value = "/periodPruning", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void prunePeriods()
    {
        maintenanceService.prunePeriods();
    }

    @RequestMapping( value = "/zeroDataValueRemoval", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void deleteZeroDataValues()
    {
        maintenanceService.deleteZeroDataValues();
    }

    @RequestMapping( value = "/dropSqlViews", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void dropSqlViews()
    {
        resourceTableService.dropAllSqlViews();
    }

    @RequestMapping( value = "/createSqlViews", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void createSqlViews()
    {
        resourceTableService.createAllSqlViews();
    }

    @RequestMapping( value = "/categoryOptionComboUpdate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void updateCategoryOptionCombos()
    {
        categoryService.updateAllOptionCombos();
    }

    @RequestMapping( value = "/cache", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void clearCache()
    {
        cacheManager.clearCache();
        partitionManager.clearCaches();
    }

    @RequestMapping( value = "/metadataValidation", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void runValidateMetadata( HttpServletResponse response ) throws InvocationTargetException, IllegalAccessException, IOException
    {
        Options options = new Options();
        options.setAssumeTrue( true );

        MetaData metaData = exportService.getMetaData( options );
        Schema schema = schemaService.getDynamicSchema( MetaData.class );

        Map<String, Map<String, List<ValidationViolation>>> output = new HashMap<>();

        for ( Property property : schema.getProperties() )
        {
            if ( !property.isCollection() || !property.isIdentifiableObject() )
            {
                continue;
            }

            output.put( property.getName(), new HashMap<>() );

            Collection<?> collection = (Collection<?>) property.getGetterMethod().invoke( metaData );

            for ( Object object : collection )
            {
                List<ValidationViolation> validationViolations = schemaValidator.validate( object );

                if ( !validationViolations.isEmpty() )
                {
                    output.get( property.getName() ).put( ((IdentifiableObject) object).getUid(), validationViolations );
                }
            }
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), output );
    }
}
