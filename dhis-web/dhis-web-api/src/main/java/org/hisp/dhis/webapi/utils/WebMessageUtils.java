package org.hisp.dhis.webapi.utils;

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

import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.ImportTypeSummary;
import org.hisp.dhis.dxf2.schema.ValidationViolation;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageStatus;
import org.hisp.dhis.dxf2.webmessage.responses.ValidationViolationsWebMessageResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class WebMessageUtils
{
    public static WebMessage createWebMessage( String message, WebMessageStatus status, HttpStatus httpStatus )
    {
        WebMessage webMessage = new WebMessage( status, httpStatus );
        webMessage.setMessage( message );

        return webMessage;
    }

    public static WebMessage createWebMessage( String message, String devMessage, WebMessageStatus status, HttpStatus httpStatus )
    {
        WebMessage webMessage = new WebMessage( status, httpStatus );
        webMessage.setMessage( message );
        webMessage.setDevMessage( devMessage );

        return webMessage;
    }

    public static WebMessage ok( String message )
    {
        return createWebMessage( message, WebMessageStatus.OK, HttpStatus.OK );
    }

    public static WebMessage ok( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.OK, HttpStatus.OK );
    }

    public static WebMessage created( String message )
    {
        return createWebMessage( message, WebMessageStatus.OK, HttpStatus.CREATED );
    }

    public static WebMessage created( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.OK, HttpStatus.CREATED );
    }

    public static WebMessage notFound( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.NOT_FOUND );
    }

    public static WebMessage notFound( Class<?> klass, String id )
    {
        String message = klass.getSimpleName() + " with id " + id + " could not be found.";
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.NOT_FOUND );
    }

    public static WebMessage notFound( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.NOT_FOUND );
    }

    public static WebMessage conflict( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.CONFLICT );
    }

    public static WebMessage conflict( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.CONFLICT );
    }

    public static WebMessage error( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.INTERNAL_SERVER_ERROR );
    }

    public static WebMessage error( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.INTERNAL_SERVER_ERROR );
    }

    public static WebMessage badRequest( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.BAD_REQUEST );
    }

    public static WebMessage badRequest( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.BAD_REQUEST );
    }

    public static WebMessage forbidden( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.FORBIDDEN );
    }

    public static WebMessage forbidden( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.FORBIDDEN );
    }

    public static WebMessage serviceUnavailable( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.SERVICE_UNAVAILABLE );
    }

    public static WebMessage serviceUnavailable( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.SERVICE_UNAVAILABLE );
    }

    public static WebMessage unprocessableEntity( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.UNPROCESSABLE_ENTITY );
    }

    public static WebMessage unprocessableEntity( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.UNPROCESSABLE_ENTITY );
    }

    public static WebMessage unathorized( String message )
    {
        return createWebMessage( message, WebMessageStatus.ERROR, HttpStatus.UNAUTHORIZED );
    }

    public static WebMessage unathorized( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, WebMessageStatus.ERROR, HttpStatus.UNAUTHORIZED );
    }

    public static WebMessage importTypeSummary( ImportTypeSummary importTypeSummary )
    {
        WebMessage webMessage = new WebMessage();

        if ( importTypeSummary.isStatus( ImportStatus.ERROR ) )
        {
            webMessage.setMessage( "An error occurred, please check import summary." );
            webMessage.setStatus( WebMessageStatus.ERROR );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else if ( !importTypeSummary.getConflicts().isEmpty() )
        {
            webMessage.setMessage( "One more conflicts encountered, please check import summary." );
            webMessage.setStatus( WebMessageStatus.WARNING );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else
        {
            webMessage.setMessage( "Import was successful." );
            webMessage.setStatus( WebMessageStatus.OK );
            webMessage.setHttpStatus( HttpStatus.OK );
        }

        webMessage.setResponse( importTypeSummary );

        return webMessage;
    }

    public static WebMessage importSummary( ImportSummary importSummary )
    {
        WebMessage webMessage = new WebMessage();

        if ( importSummary.isStatus( ImportStatus.ERROR ) )
        {
            webMessage.setMessage( "An error occurred, please check import summary." );
            webMessage.setStatus( WebMessageStatus.ERROR );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else if ( !importSummary.getConflicts().isEmpty() )
        {
            webMessage.setMessage( "One more conflicts encountered, please check import summary." );
            webMessage.setStatus( WebMessageStatus.WARNING );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else
        {
            webMessage.setMessage( "Import was successful." );
            webMessage.setStatus( WebMessageStatus.OK );
            webMessage.setHttpStatus( HttpStatus.OK );
        }

        webMessage.setResponse( importSummary );

        return webMessage;
    }

    public static WebMessage importSummaries( ImportSummaries importSummaries )
    {
        WebMessage webMessage = new WebMessage();

        if ( importSummaries.getIgnored() > 0 )
        {
            webMessage.setMessage( "One more conflicts encountered, please check import summary." );
            webMessage.setStatus( WebMessageStatus.WARNING );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else
        {
            webMessage.setMessage( "Import was successful." );
            webMessage.setStatus( WebMessageStatus.OK );
            webMessage.setHttpStatus( HttpStatus.OK );
        }

        webMessage.setResponse( importSummaries );

        return webMessage;
    }

    public static WebMessage validationViolations( List<ValidationViolation> validationViolations )
    {
        WebMessage webMessage = new WebMessage();
        webMessage.setResponse( new ValidationViolationsWebMessageResponse( validationViolations ) );

        if ( !validationViolations.isEmpty() )
        {
            webMessage.setStatus( WebMessageStatus.ERROR );
            webMessage.setHttpStatus( HttpStatus.BAD_REQUEST );
        }

        return webMessage;
    }

    private WebMessageUtils()
    {
    }
}
