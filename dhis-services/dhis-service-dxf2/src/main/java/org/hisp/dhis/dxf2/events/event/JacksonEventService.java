package org.hisp.dhis.dxf2.events.event;

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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of EventService that uses Jackson for serialization and deserialization.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class JacksonEventService extends AbstractEventService
{
    private static final Log log = LogFactory.getLog( JacksonEventService.class );

    // -------------------------------------------------------------------------
    // EventService Impl
    // -------------------------------------------------------------------------

    private final static ObjectMapper XML_MAPPER = new XmlMapper();

    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    @SuppressWarnings( "unchecked" )
    private static <T> T fromXml( String input, Class<?> clazz ) throws IOException
    {
        return (T) XML_MAPPER.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromJson( String input, Class<?> clazz ) throws IOException
    {
        return (T) JSON_MAPPER.readValue( input, clazz );
    }

    static
    {
        XML_MAPPER.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true );
        XML_MAPPER.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        XML_MAPPER.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );
        JSON_MAPPER.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true );
        JSON_MAPPER.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        JSON_MAPPER.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        return addEventsXml( inputStream, null, importOptions );
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, TaskId taskId, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<Event> events = new ArrayList<>();

        try
        {
            Events fromXml = fromXml( input, Events.class );
            events.addAll( fromXml.getEvents() );
        }
        catch ( Exception ex )
        {
            Event fromXml = fromXml( input, Event.class );
            events.add( fromXml );
        }

        return addEvents( events, taskId, importOptions );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        return addEventsJson( inputStream, null, importOptions );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, TaskId taskId, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<Event> events = new ArrayList<>();

        try
        {
            Events fromJson = fromJson( input, Events.class );
            events.addAll( fromJson.getEvents() );
        }
        catch ( Exception ex )
        {
            Event fromJson = fromJson( input, Event.class );
            events.add( fromJson );
        }

        return addEvents( events, taskId, importOptions );
    }

    private ImportSummaries addEvents( List<Event> events, TaskId taskId, ImportOptions importOptions )
    {
        ImportSummaries importSummaries;

        notifier.clear( taskId ).notify( taskId, "Importing events" );
        Timer timer = new SystemTimer().start();

        List<Event> create = new ArrayList<>();
        List<Event> update = new ArrayList<>();

        if ( importOptions.getImportStrategy().isCreate() )
        {
            create.addAll( events );
        }
        else if ( importOptions.getImportStrategy().isCreateAndUpdate() )
        {
            for ( Event event : events )
            {
                if ( StringUtils.isEmpty( event.getEvent() ) )
                {
                    create.add( event );
                }
                else
                {
                    if ( !programStageInstanceService.programStageInstanceExists( event.getEvent() ) )
                    {
                        create.add( event );
                    }
                    else
                    {
                        update.add( event );
                    }
                }
            }
        }

        importSummaries = addEvents( create, importOptions );
        updateEvents( update, false );

        if ( taskId != null )
        {
            notifier.notify( taskId, NotificationLevel.INFO, "Import done. Completed in " + timer.toString() + ".", true ).
                addTaskSummary( taskId, importSummaries );
        }
        else
        {
            log.info( "Import done. Completed in " + timer.toString() + "." );
        }

        return importSummaries;
    }
}
