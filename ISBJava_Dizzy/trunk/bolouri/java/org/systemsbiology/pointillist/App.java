/*
 * Copyright (C) 2004 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.pointillist;

import org.systemsbiology.gui.*;
import org.systemsbiology.util.*;
import org.systemsbiology.data.*;
import javax.swing.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.prefs.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class App extends JFrame
{
    private static final String APP_NAME = "Pointillist";
    private Preferences mPreferences;
    private FramePlacer mFramePlacer;
    private MenuBar mMenu;
    private MatlabConnectionManager mMatlabConnectionManager;
    private AppConfig mAppConfig;
    private File mInstallationDir;
    private File mWorkingDirectory;
    
    public static final String PREFERENCES_KEY_MATLAB_LOCATION = "matlabLocation";
    public static final String PREFERENCES_KEY_DATA_FILE_DELIMITER = "dataFileDelimiter";
    public static final String PREFERENCES_KEY_MISSING_DATA_VALUE = "missingDataValue";
    public static final String PREFERENCES_KEY_MATLAB_SCRIPTS_LOCATION = "matlabScriptsLocation";
    public static final DataFileDelimiter DEFAULT_DATA_FILE_DELIMITER = DataFileDelimiter.TAB;
    
    public File getWorkingDirectory()
    {
        return mWorkingDirectory;
    }
    
    public void setWorkingDirectory(File pWorkingDirectory)
    {
        mWorkingDirectory = pWorkingDirectory;
    }
    
    private void initializeAppConfig(String []pArgs) throws InvalidInputException, FileNotFoundException, DataNotFoundException
    {
        mInstallationDir = null;
        File appDir = null;
        if(pArgs.length > 0)
        {
            String appDirName = pArgs[0];
            appDir = new File(appDirName);
            if(! appDir.exists() || ! appDir.isDirectory())
            {
                System.err.println("invalid installation directory: " + appDir.getAbsolutePath());
                System.exit(1);
            }
        }
        mInstallationDir = appDir;        
        AppConfig appConfig = null;
        if(null == appDir)
        {
            // we don't know where we are installed, so punt and look for 
            // the config file as a class resource:
            appConfig = new AppConfig(App.class);
        }
        else
        {
            String configFileName = appDir.getAbsolutePath() + File.separator + "config" + File.separator + 
                                    AppConfig.CONFIG_FILE_NAME;
            appConfig = new AppConfig(new File(configFileName));
        }
        mAppConfig = appConfig;
    }
    
    private void configureDefaultMatlabScriptsLocation()
    {
        String matlabScriptsLocation = mPreferences.get(PREFERENCES_KEY_MATLAB_SCRIPTS_LOCATION, "");
        if(matlabScriptsLocation.length() == 0 && null != mInstallationDir)
        {
            matlabScriptsLocation = new String(mInstallationDir.getAbsolutePath() + File.separator + "matlab");
            File matlabScriptsLocationFile = new File(matlabScriptsLocation);
            if(matlabScriptsLocationFile.exists() && matlabScriptsLocationFile.isDirectory())
            {
                mPreferences.put(PREFERENCES_KEY_MATLAB_SCRIPTS_LOCATION,
                                 matlabScriptsLocationFile.getAbsolutePath());
            }
        }
    }
    
    public App(String pName)
    {
        super(pName);
    }
    
    public void configureDefaultDataFileDelimiterPreference()
    {
        String preferredDataFileDelimiterName = mPreferences.get(App.PREFERENCES_KEY_DATA_FILE_DELIMITER, "");
        int stringLength = preferredDataFileDelimiterName.length();
        if(preferredDataFileDelimiterName.length() == 0 || null == DataFileDelimiter.forName(preferredDataFileDelimiterName))
        {
            boolean warnUser = false;
            if(preferredDataFileDelimiterName.length() > 0)
            {
                warnUser = true;
            }
            preferredDataFileDelimiterName = DEFAULT_DATA_FILE_DELIMITER.getName();
            mPreferences.put(App.PREFERENCES_KEY_DATA_FILE_DELIMITER, preferredDataFileDelimiterName);
            if(warnUser)
            {
                JOptionPane.showMessageDialog(this, "reverted to default data file delimiter preference: " + 
                                              preferredDataFileDelimiterName,
                                              "reverted to default preference", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    public Preferences getPreferences()
    {
        return(mPreferences);
    }
    
    public FramePlacer getFramePlacer()
    {
        return(mFramePlacer);
    }
    
    public MatlabConnectionManager getMatlabConnectionManager()
    {
        return(mMatlabConnectionManager);
    }
    
    public MenuBar getMenu()
    {
        return(mMenu);
    }
    
    public String getName()
    {
        return(APP_NAME);
    }
    
    private void initializeFrame()
    {
        MenuBar menu = new MenuBar(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                QuitAction quitAction = new QuitAction(App.this);
                quitAction.doAction();
            }
        });
        mMenu = menu;
        setJMenuBar(menu);
        pack();
        mFramePlacer = new FramePlacer();
        FramePlacer.placeInCenterOfScreen(this);
    }
    
    public AppConfig getAppConfig()
    {
        return mAppConfig;
    }
    
    public void run(String []pArgs)
    {
        mPreferences = Preferences.userNodeForPackage(this.getClass());
        mMatlabConnectionManager = new MatlabConnectionManager(mPreferences);
        setWorkingDirectory(null);
        
        initializeFrame();

        try
        {
            configureDefaultDataFileDelimiterPreference();
            initializeAppConfig(pArgs);
            configureDefaultMatlabScriptsLocation();
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
            optionPane.createDialog(this, "unable to load application configuration").show();
            System.exit(1);
        }
        
        setVisible(true);
    }
    
    public void handleNotConnectedToMatlab()
    {
        JOptionPane.showMessageDialog(this, "Please connect to Matlab first, using the \"Connections\" menu", 
                "Not connected to Matlab", JOptionPane.WARNING_MESSAGE);
    }
    
    public static final void main(String []pArgs)
    {
        App app = new App(APP_NAME);
        app.run(pArgs);
    }
}
