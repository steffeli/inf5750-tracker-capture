package org.hisp.dhis.sms.listener;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.sms.SmsMessageSender;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.springframework.transaction.annotation.Transactional;

public class DHISMessageAlertListener
    implements IncomingSmsListener
{
    private SMSCommandService smsCommandService;

    public void setSmsCommandService( SMSCommandService smsCommandService )
    {
        this.smsCommandService = smsCommandService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    private SmsMessageSender smsMessageSender;

    public void setSmsMessageSender( SmsMessageSender smsMessageSender )
    {
        this.smsMessageSender = smsMessageSender;
    }
    
    private IncomingSmsService incomingSmsService;

    public void setIncomingSmsService( IncomingSmsService incomingSmsService )
    {
        this.incomingSmsService = incomingSmsService;
    }

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        String message = sms.getText();
        String commandString = null;

        for ( int i = 0; i < message.length(); i++ )
        {
            String c = String.valueOf( message.charAt( i ) );
            if ( c.matches( "\\W" ) )
            {
                commandString = message.substring( 0, i );
                message = message.substring( commandString.length() + 1 );
                break;
            }
        }

        return smsCommandService.getSMSCommand( commandString, ParserType.ALERT_PARSER ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        String message = sms.getText();
        String commandString = null;

        for ( int i = 0; i < message.length(); i++ )
        {
            String c = String.valueOf( message.charAt( i ) );
            if ( c.matches( "\\W" ) )
            {
                commandString = message.substring( 0, i );
                message = message.substring( commandString.length() + 1 );
                break;
            }
        }

        SMSCommand smsCommand = smsCommandService.getSMSCommand( commandString, ParserType.ALERT_PARSER );
        UserGroup userGroup = smsCommand.getUserGroup();
        String senderPhoneNumber = StringUtils.replace( sms.getOriginator(), "+", "" );

        if ( userGroup != null )
        {
            Collection<User> users = userService.getUsersByPhoneNumber( senderPhoneNumber );

            if ( users != null && users.size() > 1 )
            {
                String messageMoreThanOneUser = smsCommand.getMoreThanOneOrgUnitMessage();
                if ( messageMoreThanOneUser.trim().equals( "" ) )
                {
                    messageMoreThanOneUser = SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE;
                }
                for ( Iterator<User> i = users.iterator(); i.hasNext(); )
                {
                    User user = i.next();
                    messageMoreThanOneUser += " " + user.getName();
                    if ( i.hasNext() )
                    {
                        messageMoreThanOneUser += ",";
                    }
                }
                throw new SMSParserException( messageMoreThanOneUser );
            }
            else if ( users != null && users.size() == 1 )
            {
                User sender = users.iterator().next();

                Set<User> receivers = new HashSet<>( userGroup.getMembers() );

                // forward to user group by SMS,Dhis2 message, Email
                messageService.sendMessage( smsCommand.getName(), message, null, receivers, sender, false, false );

                // confirm SMS was received and forwarded completely
                Set<User> feedbackList = new HashSet<>();
                feedbackList.add( sender );

                String confirmMessage = smsCommand.getReceivedMessage();

                if ( confirmMessage == null )
                {
                    confirmMessage = SMSCommand.ALERT_FEEDBACK;
                }

                smsMessageSender.sendMessage( smsCommand.getName(), confirmMessage, null, null, feedbackList, true );

                sms.setParsed( true );
                sms.setStatus( SmsMessageStatus.PROCESSED );
                incomingSmsService.update( sms );
            }
            else if ( users == null || users.size() == 0 )
            {
                throw new SMSParserException(
                    "No user associated with this phone number. Please contact your supervisor." );
            }
        }
    }
}
