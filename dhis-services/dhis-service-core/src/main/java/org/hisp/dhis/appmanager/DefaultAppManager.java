package org.hisp.dhis.appmanager;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.ant.compress.taskdefs.Unzip;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.datavalue.DefaultDataValueService;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.setting.Setting;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Saptarshi Purkayastha
 */
public class DefaultAppManager
    implements AppManager
{
    private static final Log log = LogFactory.getLog( DefaultDataValueService.class );

    /**
     * In-memory singleton list holding state for apps.
     */
    private List<App> apps = new ArrayList<>();

    /**
     * Mapping dataStore-namespaces and apps
     */
    private HashMap<String, App> appNamespaces = new HashMap<>();

    @PostConstruct
    private void init()
    {
        reloadApps();
    }

    @Autowired
    private SystemSettingManager appSettingManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private KeyJsonValueService keyJsonValueService;

    // -------------------------------------------------------------------------
    // AppManagerService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<App> getApps()
    {
        String baseUrl = getAppBaseUrl();

        for ( App app : apps )
        {
            app.setBaseUrl( baseUrl );
        }

        return apps;
    }

    @Override
    public App getApp( String key )
    {
        List<App> apps = getApps();

        for ( App app : apps )
        {
            if ( key.equals( app.getKey() ) )
            {
                return app;
            }
        }

        return null;
    }

    @Override
    public List<App> getAccessibleApps()
    {
        return getApps().stream().filter( this::isAccessible ).collect( Collectors.toList() );
    }

    @Override
    public AppStatus installApp( File file, String fileName, String rootPath )
    {
        try
        {

            // ---------------------------------------------------------------------
            // Parse zip file and it's manifest.webapp file.
            // ---------------------------------------------------------------------

            ZipFile zip = new ZipFile( file );

            ZipEntry entry = zip.getEntry( "manifest.webapp" );
            InputStream inputStream = zip.getInputStream( entry );
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

            App app = mapper.readValue( inputStream, App.class );

            // ---------------------------------------------------------------------
            // Check for namespace and if it's already taken by another app
            // ---------------------------------------------------------------------

            String appNamespace = app.getActivities().getDhis().getNamespace();
            if ( appNamespace != null && (this.appNamespaces.containsKey( appNamespace ) &&
                !app.equals( appNamespaces.get( appNamespace ) )) )
            {
                zip.close();
                return AppStatus.NAMESPACE_TAKEN;
            }

            // ---------------------------------------------------------------------
            // Delete if app is already installed.
            // Assuming app-update, so no data is deleted.
            // ---------------------------------------------------------------------

            deleteApp( app.getName(), false );

            // ---------------------------------------------------------------------
            // Unzip the app
            // ---------------------------------------------------------------------

            String dest = getAppFolderPath() + File.separator + fileName.substring( 0, fileName.lastIndexOf( '.' ) );
            Unzip unzip = new Unzip();
            unzip.setSrc( file );
            unzip.setDest( new File( dest ) );
            unzip.execute();

            // ---------------------------------------------------------------------
            // Set dhis server location
            // ---------------------------------------------------------------------

            File updateManifest = new File( dest + File.separator + "manifest.webapp" );
            App installedApp = mapper.readValue( updateManifest, App.class );

            if ( installedApp.getActivities() != null && installedApp.getActivities().getDhis() != null )
            {
                if ( "*".equals( installedApp.getActivities().getDhis().getHref() ) )
                {
                    installedApp.getActivities().getDhis().setHref( rootPath );
                    mapper.writeValue( updateManifest, installedApp );
                }
            }

            // ---------------------------------------------------------------------
            // Installation complete. Closing zip, reloading apps and return OK
            // ---------------------------------------------------------------------

            zip.close();

            reloadApps();

            return AppStatus.OK;

        }
        catch ( ZipException e )
        {
            return AppStatus.INVALID_ZIP_FORMAT;
        }
        catch ( JsonParseException e )
        {
            return AppStatus.INVALID_MANIFEST_JSON;
        }
        catch ( JsonMappingException e )
        {
            return AppStatus.INVALID_MANIFEST_JSON;
        }
        catch ( IOException e )
        {
            return AppStatus.INSTALLATION_FAILED;
        }
    }

    @Override
    public boolean exists( String appName )
    {
        for ( App app : getApps() )
        {
            if ( app.getName().equals( appName ) || app.getFolderName().equals( appName ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean deleteApp( String name, boolean deleteAppData )
    {
        for ( App app : getApps() )
        {
            if ( app.getName().equals( name ) || app.getFolderName().equals( name ) )
            {
                try
                {
                    String folderPath = getAppFolderPath() + File.separator + app.getFolderName();
                    FileUtils.forceDelete( new File( folderPath ) );

                    // If deleteAppData is true and a namespace associated with the app exists, delete it.
                    if ( deleteAppData && appNamespaces.containsValue( app ) )
                    {
                        appNamespaces.forEach( ( namespace, app1 ) -> {
                            if ( app1 == app )
                            {
                                keyJsonValueService.deleteNamespace( namespace );
                            }
                        } );
                    }

                    return true;
                }
                catch ( IOException ex )
                {
                    log.error( "Could not delete app: " + name, ex );
                    return false;
                }
                finally
                {
                    reloadApps(); // Reload app state
                }
            }
        }

        return false;
    }

    @Override
    public String getAppFolderPath()
    {
        return StringUtils.trimToNull( (String) appSettingManager.getSystemSetting( Setting.APP_FOLDER_PATH ) );
    }

    @Override
    public void setAppFolderPath( String appFolderPath )
    {
        if ( !appFolderPath.isEmpty() )
        {
            try
            {
                File folder = new File( appFolderPath );
                if ( !folder.exists() )
                {
                    FileUtils.forceMkdir( folder );
                }
            }
            catch ( IOException ex )
            {
                log.error( ex.getLocalizedMessage(), ex );
            }
        }

        appSettingManager.saveSystemSetting( Setting.APP_FOLDER_PATH, appFolderPath );
    }

    @Override
    public String getAppBaseUrl()
    {
        return StringUtils.trimToNull( (String) appSettingManager.getSystemSetting( Setting.APP_BASE_URL ) );
    }

    @Override
    public void setAppBaseUrl( String appBaseUrl )
    {
        appSettingManager.saveSystemSetting( Setting.APP_BASE_URL, appBaseUrl );
    }

    @Override
    public String getAppStoreUrl()
    {
        return StringUtils.trimToNull( (String) appSettingManager.getSystemSetting( Setting.APP_STORE_URL ) );
    }

    @Override
    public void setAppStoreUrl( String appStoreUrl )
    {
        appSettingManager.saveSystemSetting( Setting.APP_STORE_URL, appStoreUrl );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Sets the list of apps with detected apps from the file system.
     */
    @Override
    public void reloadApps()
    {
        List<App> appList = new ArrayList<>();
        HashMap<String, App> appNamespaces = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

        if ( null != getAppFolderPath() )
        {
            File appFolderPath = new File( getAppFolderPath() );

            if ( appFolderPath.isDirectory() )
            {
                File[] listFiles = appFolderPath.listFiles();

                for ( File folder : listFiles )
                {
                    if ( folder.isDirectory() )
                    {
                        File appManifest = new File( folder, "manifest.webapp" );

                        if ( appManifest.exists() )
                        {
                            try
                            {
                                App app = mapper.readValue( appManifest, App.class );
                                app.setFolderName( folder.getName() );
                                appList.add( app );

                                // Add namespace
                                String appNamespace = app.getActivities().getDhis().getNamespace();
                                if ( appNamespace != null )
                                {
                                    appNamespaces.put( appNamespace, app );
                                }
                            }
                            catch ( IOException ex )
                            {
                                log.error( ex.getLocalizedMessage(), ex );
                            }
                        }
                    }
                }
            }
        }

        this.apps = appList;
        this.appNamespaces = appNamespaces;

        log.info( "Detected apps: " + apps );
    }

    @Override
    public boolean isAccessible( App app )
    {
        return isAccessible( app, currentUserService.getCurrentUser() );
    }

    @Override
    public boolean isAccessible( App app, User user )
    {
        if ( user == null || user.getUserCredentials() == null || app == null || app.getName() == null )
        {
            return false;
        }

        UserCredentials userCredentials = user.getUserCredentials();

        return userCredentials.getAllAuthorities().contains( "ALL" ) ||
            userCredentials.getAllAuthorities().contains( "M_dhis-web-maintenance-appmanager" ) ||
            userCredentials.getAllAuthorities().contains( "See " + app.getName().trim() );
    }

    @Override
    public App getAppByNamespace( String namespace )
    {
        return appNamespaces.get( namespace );
    }
}
