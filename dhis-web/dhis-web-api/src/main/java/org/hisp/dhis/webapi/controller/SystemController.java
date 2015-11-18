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

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataintegrity.DataIntegrityReport;
import org.hisp.dhis.dataintegrity.FlattenedDataIntegrityReport;
import org.hisp.dhis.dxf2.metadata.ImportSummary;
import org.hisp.dhis.dxf2.render.RenderService;
import org.hisp.dhis.node.exception.InvalidTypeException;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.StyleManager;
import org.hisp.dhis.setting.StyleObject;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = SystemController.RESOURCE_PATH )
public class SystemController
{
    public static final String RESOURCE_PATH = "/system";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private StyleManager styleManager;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private Notifier notifier;

    @Autowired
    private RenderService renderService;

    //--------------------------------------------------------------------------
    // UID Generator
    //--------------------------------------------------------------------------

    @RequestMapping( value = { "/uid", "/id" }, method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode getUid( @RequestParam( required = false, defaultValue = "1" ) Integer limit )
        throws IOException, InvalidTypeException
    {
        limit = Math.min( limit, 10000 );

        RootNode rootNode = new RootNode( "codes" );
        CollectionNode collectionNode = rootNode.addChild( new CollectionNode( "codes" ) );
        collectionNode.setWrapping( false );

        for ( int i = 0; i < limit; i++ )
        {
            collectionNode.addChild( new SimpleNode( "code", CodeGenerator.generateCode() ) );
        }

        return rootNode;
    }

    @RequestMapping( value = "/uuid", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode getUuid( @RequestParam( required = false, defaultValue = "1" ) Integer limit )
        throws IOException, InvalidTypeException
    {
        limit = Math.min( limit, 10000 );

        RootNode rootNode = new RootNode( "codes" );
        CollectionNode collectionNode = rootNode.addChild( new CollectionNode( "codes" ) );
        collectionNode.setWrapping( false );

        for ( int i = 0; i < limit; i++ )
        {
            collectionNode.addChild( new SimpleNode( "code", UUID.randomUUID().toString() ) );
        }

        return rootNode;
    }

    @RequestMapping( value = "/tasks/{category}", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    public void getTaskJson( @PathVariable( "category" ) String category,
        @RequestParam( required = false ) String lastId, HttpServletResponse response ) throws IOException
    {
        List<Notification> notifications = new ArrayList<>();

        if ( category != null )
        {
            TaskCategory taskCategory = TaskCategory.valueOf( category.toUpperCase() );

            TaskId taskId = new TaskId( taskCategory, currentUserService.getCurrentUser() );

            notifications = notifier.getNotifications( taskId, lastId );
        }

        renderService.toJson( response.getOutputStream(), notifications );
    }

    @RequestMapping( value = "/taskSummaries/{category}", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    public void getTaskSummaryJson( HttpServletResponse response, @PathVariable( "category" ) String category ) throws IOException
    {
        if ( category != null )
        {
            TaskCategory taskCategory = TaskCategory.valueOf( category.toUpperCase() );

            TaskId taskId = new TaskId( taskCategory, currentUserService.getCurrentUser() );

            // TODO Make task summary generic. (We use tasks for more than importing data).
            if ( taskCategory.equals( TaskCategory.DATAINTEGRITY ) )
            {
                DataIntegrityReport dataIntegrityReport = (DataIntegrityReport) notifier.getTaskSummary( taskId );
                renderService.toJson( response.getOutputStream(), new FlattenedDataIntegrityReport( dataIntegrityReport ) );
                return;
            }
            else
            {
                ImportSummary importSummary = (ImportSummary) notifier.getTaskSummary( taskId );
                renderService.toJson( response.getOutputStream(), importSummary );
                return;
            }
        }

        renderService.toJson( response.getOutputStream(), new ImportSummary() );
    }

    @RequestMapping( value = "/info", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public String getSystemInfo( Model model, HttpServletRequest request )
    {
        SystemInfo info = systemService.getSystemInfo();

        info.setContextPath( ContextUtils.getContextPath( request ) );
        info.setUserAgent( request.getHeader( ContextUtils.HEADER_USER_AGENT ) );

        if ( !currentUserService.currentUserIsSuper() )
        {
            info.clearSensitiveInfo();
        }

        model.addAttribute( "model", info );

        return "info";
    }

    @RequestMapping( value = "/ping", method = RequestMethod.GET, produces = "text/plain" )
    public @ResponseBody String ping()
    {
        return "pong";
    }

    @RequestMapping( value = "/flags", method = RequestMethod.GET, produces = { "application/json" } )
    public @ResponseBody List<StyleObject> getFlags()
    {
        return systemSettingManager.getFlagObjects();
    }

    @RequestMapping( value = "/styles", method = RequestMethod.GET, produces = { "application/json" } )
    public @ResponseBody List<StyleObject> getStyles()
    {
        return styleManager.getStyles();
    }
}
