package org.hisp.dhis.webapi.controller.user;

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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.ImportTypeSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.schema.descriptors.UserSchemaDescriptor;
import org.hisp.dhis.security.RestoreOptions;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.Users;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebMetaData;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = UserSchemaDescriptor.API_ENDPOINT )
public class UserController
    extends AbstractCrudController<User>
{
    public static final String INVITE_PATH = "/invite";
    public static final String BULK_INVITE_PATH = "/invites";

    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @Override
    protected List<User> getEntityList( WebMetaData metaData, WebOptions options, List<String> filters, List<Order> orders )
    {
        UserQueryParams params = new UserQueryParams();
        params.setQuery( options.get( "query" ) );
        params.setPhoneNumber( options.get( "phoneNumber" ) );
        params.setCanManage( options.isTrue( "canManage" ) );
        params.setAuthSubset( options.isTrue( "authSubset" ) );
        params.setLastLogin( options.getDate( "lastLogin" ) );
        params.setInactiveMonths( options.getInt( "inactiveMonths" ) );
        params.setInactiveSince( options.getDate( "inactiveSince" ) );
        params.setSelfRegistered( options.isTrue( "selfRegistered" ) );
        params.setInvitationStatus( UserInvitationStatus.fromValue( options.get( "invitationStatus" ) ) );

        String ou = options.get( "ou" );

        if ( ou != null )
        {
            params.setOrganisationUnit( organisationUnitService.getOrganisationUnit( ou ) );
        }

        if ( options.isManage() )
        {
            params.setCanManage( true );
            params.setAuthSubset( true );
        }

        int count = userService.getUserCount( params );

        if ( options.hasPaging() && filters.isEmpty() )
        {
            Pager pager = new Pager( options.getPage(), count, options.getPageSize() );
            metaData.setPager( pager );
            params.setFirst( pager.getOffset() );
            params.setMax( pager.getPageSize() );
        }

        return userService.getUsers( params );
    }

    @Override
    protected List<User> getEntity( String uid, WebOptions options )
    {
        List<User> users = Lists.newArrayList();
        Optional<User> user = Optional.fromNullable( userService.getUser( uid ) );

        if ( user.isPresent() )
        {
            users.add( user.get() );
        }

        return users;
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    public void postXmlObject( ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromXml( request.getInputStream(), getEntityClass() );

        if ( !validateCreateUser( user, response ) )
        {
            return;
        }

        renderService.toXml( response.getOutputStream(), createUser( user, response ) );
    }

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonObject( ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromJson( request.getInputStream(), getEntityClass() );

        if ( !validateCreateUser( user, response ) )
        {
            return;
        }

        renderService.toJson( response.getOutputStream(), createUser( user, response ) );
    }

    @RequestMapping( value = INVITE_PATH, method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    public void postXmlInvite( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromXml( request.getInputStream(), getEntityClass() );

        if ( !validateInviteUser( user, response ) )
        {
            return;
        }

        renderService.toXml( response.getOutputStream(), inviteUser( user, request, response ) );
    }

    @RequestMapping( value = INVITE_PATH, method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonInvite( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromJson( request.getInputStream(), getEntityClass() );

        if ( !validateInviteUser( user, response ) )
        {
            return;
        }

        renderService.toJson( response.getOutputStream(), inviteUser( user, request, response ) );
    }

    @RequestMapping( value = BULK_INVITE_PATH, method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    public void postXmlInvites( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Users users = renderService.fromXml( request.getInputStream(), Users.class );

        for ( User user : users.getUsers() )
        {
            if ( !validateInviteUser( user, response ) )
            {
                return;
            }
        }

        for ( User user : users.getUsers() )
        {
            inviteUser( user, request, response );
        }
    }

    @RequestMapping( value = "/{id}" + INVITE_PATH, method = RequestMethod.POST )
    public void resendInvite( @PathVariable String id, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = userService.getUser( id );

        if ( user == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User not found: " + id ) );
        }

        if ( user.getUserCredentials() == null || !user.getUserCredentials().isInvitation() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User account is not an invitation: " + id ) );
        }

        String valid = securityService.validateRestore( user.getUserCredentials() );

        if ( valid != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( valid ) );
        }

        boolean isInviteUsername = securityService.isInviteUsername( user.getUsername() );

        RestoreOptions restoreOptions = isInviteUsername ? RestoreOptions.INVITE_WITH_USERNAME_CHOICE : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

        securityService.sendRestoreMessage( user.getUserCredentials(), ContextUtils.getContextPath( request ), restoreOptions );
    }

    @RequestMapping( value = BULK_INVITE_PATH, method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonInvites( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Users users = renderService.fromJson( request.getInputStream(), Users.class );

        for ( User user : users.getUsers() )
        {
            if ( !validateInviteUser( user, response ) )
            {
                return;
            }
        }

        for ( User user : users.getUsers() )
        {
            inviteUser( user, request, response );
        }
    }

    @SuppressWarnings( "unchecked" )
    @PreAuthorize( "hasRole('ALL')" )
    @RequestMapping( value = "/{uid}/replica", method = RequestMethod.POST )
    public void replicateUser( @PathVariable String uid,
        HttpServletRequest request, HttpServletResponse response ) throws IOException, WebMessageException
    {
        User existingUser = userService.getUser( uid );

        if ( existingUser == null || existingUser.getUserCredentials() == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User not found: " + uid ) );
        }

        if ( !validateCreateUser( existingUser, response ) )
        {
            return;
        }

        Map<String, String> auth = renderService.fromJson( request.getInputStream(), Map.class );

        String username = StringUtils.trimToNull( auth != null ? auth.get( KEY_USERNAME ) : null );
        String password = StringUtils.trimToNull( auth != null ? auth.get( KEY_PASSWORD ) : null );

        if ( auth == null || username == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Username must be specified" ) );
        }

        if ( userService.getUserCredentialsByUsername( username ) != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Username already taken: " + username ) );
        }

        if ( password == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Password must be specified" ) );
        }

        if ( !ValidationUtils.passwordIsValid( password ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Password must have at least 8 characters, one digit, one uppercase" ) );
        }

        User userReplica = new User();
        userReplica.mergeWith( existingUser, MergeStrategy.MERGE_IF_NOT_NULL );
        userReplica.setUid( CodeGenerator.generateCode() );
        userReplica.setCreated( new Date() );

        UserCredentials credentialsReplica = new UserCredentials();
        credentialsReplica.mergeWith( existingUser.getUserCredentials(), MergeStrategy.MERGE_IF_NOT_NULL );

        credentialsReplica.setUsername( username );
        userService.encodeAndSetPassword( credentialsReplica, password );

        userReplica.setUserCredentials( credentialsReplica );
        credentialsReplica.setUserInfo( userReplica );

        userService.addUser( userReplica );
        userService.addUserCredentials( credentialsReplica );
        userGroupService.addUserToGroups( userReplica, IdentifiableObjectUtils.getUids( existingUser.getGroups() ) );

        response.addHeader( "Location", UserSchemaDescriptor.API_ENDPOINT + "/" + userReplica.getUid() );
        webMessageService.send( WebMessageUtils.created( "User replica created" ), response, request );
    }

    // -------------------------------------------------------------------------
    // PUT
    // -------------------------------------------------------------------------

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = { "application/xml", "text/xml" } )
    public void putXmlObject( ImportOptions importOptions, @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        List<User> users = getEntity( pvUid, NO_WEB_OPTIONS );

        if ( users.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( getEntityName() + " does not exist: " + pvUid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), users.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this user." );
        }

        User parsed = renderService.fromXml( request.getInputStream(), getEntityClass() );
        parsed.setUid( pvUid );

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( parsed.getGroups() ) ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        importOptions.setStrategy( ImportStrategy.UPDATE );
        ImportTypeSummary importTypeSummary = importService.importObject( currentUserService.getCurrentUser().getUid(), parsed, importOptions );

        if ( importTypeSummary.isStatus( ImportStatus.SUCCESS ) && importTypeSummary.getImportCount().getUpdated() == 1 )
        {
            User user = userService.getUser( pvUid );

            userGroupService.updateUserGroups( user, IdentifiableObjectUtils.getUids( parsed.getGroups() ) );
        }

        renderService.toXml( response.getOutputStream(), importTypeSummary );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    public void putJsonObject( ImportOptions importOptions, @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        List<User> users = getEntity( pvUid, NO_WEB_OPTIONS );

        if ( users.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( getEntityName() + " does not exist: " + pvUid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), users.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this user." );
        }

        User parsed = renderService.fromJson( request.getInputStream(), getEntityClass() );
        parsed.setUid( pvUid );

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( parsed.getGroups() ) ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        importOptions.setStrategy( ImportStrategy.UPDATE );
        ImportTypeSummary importTypeSummary = importService.importObject( currentUserService.getCurrentUser().getUid(), parsed, importOptions );

        if ( importTypeSummary.isStatus( ImportStatus.SUCCESS ) && importTypeSummary.getImportCount().getUpdated() == 1 )
        {
            User user = userService.getUser( pvUid );

            userGroupService.updateUserGroups( user, IdentifiableObjectUtils.getUids( parsed.getGroups() ) );
        }

        renderService.toJson( response.getOutputStream(), importTypeSummary );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Validates whether the given user can be created.
     *
     * @param user     the user.
     * @param response the response.
     */
    private boolean validateCreateUser( User user, HttpServletResponse response ) throws WebMessageException
    {
        if ( !aclService.canCreate( currentUserService.getCurrentUser(), getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( user.getGroups() ) ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        List<String> uids = IdentifiableObjectUtils.getUids( user.getGroups() );

        for ( String uid : uids )
        {
            if ( !userGroupService.canAddOrRemoveMember( uid ) )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "You don't have permissions to add user to user group: " + uid ) );
            }
        }

        return true;
    }

    /**
     * Creates a user.
     *
     * @param user     user object parsed from the POST request.
     * @param response the response.
     */
    private ImportSummary createUser( User user, HttpServletResponse response ) throws Exception
    {
        user.getUserCredentials().getCogsDimensionConstraints().addAll(
            currentUserService.getCurrentUser().getUserCredentials().getCogsDimensionConstraints() );

        user.getUserCredentials().getCatDimensionConstraints().addAll(
            currentUserService.getCurrentUser().getUserCredentials().getCatDimensionConstraints() );

        ImportOptions importOptions = new ImportOptions();
        importOptions.setStrategy( ImportStrategy.CREATE );
        importOptions.setMergeStrategy( MergeStrategy.MERGE );
        ImportTypeSummary importTypeSummary = importService.importObject( currentUserService.getCurrentUser().getUid(), user, importOptions );

        if ( importTypeSummary.isStatus( ImportStatus.SUCCESS ) && importTypeSummary.getImportCount().getImported() == 1 )
        {
            userGroupService.addUserToGroups( user, IdentifiableObjectUtils.getUids( user.getGroups() ) );
        }

        return importTypeSummary;
    }

    /**
     * Validates whether a user can be invited / created.
     *
     * @param user     the user.
     * @param response the response.
     */
    private boolean validateInviteUser( User user, HttpServletResponse response ) throws WebMessageException
    {
        if ( !validateCreateUser( user, response ) )
        {
            return false;
        }

        UserCredentials credentials = user.getUserCredentials();

        if ( credentials == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User credentials is not present" ) );
        }

        credentials.setUserInfo( user );

        List<UserAuthorityGroup> userRoles = userService.getUserRolesByUid( getUids( credentials.getUserAuthorityGroups() ) );

        for ( UserAuthorityGroup role : userRoles )
        {
            if ( role != null && role.hasCriticalAuthorities() )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "User cannot be invited with user role which has critical authorities: " + role ) );
            }
        }

        String valid = securityService.validateInvite( user.getUserCredentials() );

        if ( valid != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( valid + ": " + user.getUserCredentials() ) );
        }

        return true;
    }

    /**
     * Creates a user invitation and invites the user.
     *
     * @param user     user object parsed from the POST request.
     * @param response the response.
     */
    private ImportSummary inviteUser( User user, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        RestoreOptions restoreOptions = user.getUsername() == null || user.getUsername().isEmpty() ?
            RestoreOptions.INVITE_WITH_USERNAME_CHOICE : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

        securityService.prepareUserForInvite( user );

        ImportSummary summary = createUser( user, response );

        if ( summary.isStatus( ImportStatus.SUCCESS ) && summary.getImportCount().getImported() == 1 )
        {
            securityService.sendRestoreMessage( user.getUserCredentials(), ContextUtils.getContextPath( request ), restoreOptions );
        }

        return summary;
    }
}
