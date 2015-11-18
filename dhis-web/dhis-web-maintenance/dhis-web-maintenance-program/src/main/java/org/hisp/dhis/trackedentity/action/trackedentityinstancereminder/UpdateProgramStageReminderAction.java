package org.hisp.dhis.trackedentity.action.trackedentityinstancereminder;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminderService;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * 
 * @version $ UpdateProgramReminderAction.java Jan 5, 2014 11:05:21 PM $
 */
public class UpdateProgramStageReminderAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependency
    // -------------------------------------------------------------------------

    private ProgramStageService programStageService;

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    private TrackedEntityInstanceReminderService reminderService;

    public void setReminderService( TrackedEntityInstanceReminderService reminderService )
    {
        this.reminderService = reminderService;
    }

    private UserGroupService userGroupService;

    public void setUserGroupService( UserGroupService userGroupService )
    {
        this.userGroupService = userGroupService;
    }

    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------

    private int id;

    public void setId( int id )
    {
        this.id = id;
    }

    private int programStageId;

    public void setProgramStageId( int programStageId )
    {
        this.programStageId = programStageId;
    }

    public int getProgramStageId()
    {
        return programStageId;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private Integer daysAllowedSendMessage;

    public void setDaysAllowedSendMessage( Integer daysAllowedSendMessage )
    {
        this.daysAllowedSendMessage = daysAllowedSendMessage;
    }

    private String templateMessage;

    public void setTemplateMessage( String templateMessage )
    {
        this.templateMessage = templateMessage;
    }

    private String datesToCompare;

    public void setDatesToCompare( String datesToCompare )
    {
        this.datesToCompare = datesToCompare;
    }

    private Integer sendTo;

    public void setSendTo( Integer sendTo )
    {
        this.sendTo = sendTo;
    }

    private Integer whenToSend;

    public void setWhenToSend( Integer whenToSend )
    {
        this.whenToSend = whenToSend;
    }

    private Integer messageType;

    public void setMessageType( Integer messageType )
    {
        this.messageType = messageType;
    }

    private Integer userGroup;

    public void setUserGroup( Integer userGroup )
    {
        this.userGroup = userGroup;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        ProgramStage programStage = programStageService.getProgramStage( programStageId );

        TrackedEntityInstanceReminder reminder = reminderService.getReminder( id );
        reminder.setName( StringUtils.trimToNull( name ) );
        reminder.setDaysAllowedSendMessage( daysAllowedSendMessage );
        reminder.setTemplateMessage( StringUtils.trimToNull( templateMessage ) );
        reminder.setDateToCompare( datesToCompare );
        reminder.setSendTo( sendTo );
        reminder.setWhenToSend( whenToSend );
        reminder.setMessageType( messageType );

        if ( reminder.getSendTo() == TrackedEntityInstanceReminder.SEND_TO_USER_GROUP )
        {
            UserGroup selectedUserGroup = userGroupService.getUserGroup( userGroup );
            reminder.setUserGroup( selectedUserGroup );
        }
        else
        {
            reminder.setUserGroup( null );
        }

        programStageService.updateProgramStage( programStage );

        return SUCCESS;
    }
}
