package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;
import org.systemsbiology.util.*;
import org.systemsbiology.chem.scripting.*;
import org.systemsbiology.chem.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.*;
import java.io.File;
import java.io.FileNotFoundException;

public class MainApp
{
    private JFrame mMainFrame;
    private static MainApp mApp;
    private MainMenu mMainMenu;
    private SimulationLauncher mSimulationLauncher;
    private AppConfig mAppConfig;
    private File mAppDir;
    private ClassRegistry mModelBuilderRegistry;
    private ClassRegistry mModelExporterRegistry;
    private EditorPane mEditorPane;
    private int mOriginalWidthPixels;
    private int mOriginalHeightPixels;

    EditorPane getEditorPane()
    {
        return(mEditorPane);
    }

    File getAppDir()
    {
        return(mAppDir);
    }

    ClassRegistry getModelBuilderRegistry()
    {
        return(mModelBuilderRegistry);
    }

    void setModelBuilderRegistry(ClassRegistry pModelBuilderRegistry)
    {
        mModelBuilderRegistry = pModelBuilderRegistry;
    }

    ClassRegistry getModelExporterRegistry()
    {
        return(mModelExporterRegistry);
    }

    void setModelExporterRegistry(ClassRegistry pModelExporterRegistry)
    {
        mModelExporterRegistry = pModelExporterRegistry;
    }


    private void setAppDir(File pAppDir)
    {
        mAppDir = pAppDir;
    }

    private void setAppConfig(AppConfig pAppConfig)
    {
        mAppConfig = pAppConfig;
    }

    AppConfig getAppConfig()
    {
        return(mAppConfig);
    }

    void setSimulationLauncher(SimulationLauncher pSimulationLauncher)
    {
        mSimulationLauncher = pSimulationLauncher;
    }
    
    SimulationLauncher getSimulationLauncher()
    {
        return(mSimulationLauncher);
    }

    private void setMainFrame(JFrame pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    JFrame getMainFrame()
    {
        return(mMainFrame);
    }

    void handleQuit()
    {
        System.exit(0);
    }

    void handleAbout()
    {
        AboutDialog aboutDialog = new AboutDialog(getMainFrame());
        aboutDialog.show();
    }

    void handleHelpBrowser()
    {
        HelpBrowser helpBrowser = new HelpBrowser(getMainFrame());
        helpBrowser.displayHelpBrowser();
    }

    void handleExport()
    {
        Model model = null;
        try
        {
            model = mEditorPane.processModel();
        }
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled errorDialog = new ExceptionDialogOperationCancelled(mMainFrame, "unable to export model", e);
            errorDialog.show();
        }
        if(null != model)
        {
            ModelExporter exporter = new ModelExporter(getMainFrame());
            exporter.exportModel(model, mModelExporterRegistry);
        }
    }

    void handleViewInCytoscape()
    {
        try
        {
            Model model = mEditorPane.processModel();
            CytoscapeViewer cv = new CytoscapeViewer();
            cv.viewModelInCytoscape(model);
        }
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled errorDialog = new ExceptionDialogOperationCancelled(mMainFrame, "unable export model to Cytoscape", e);
            errorDialog.show();            
        }
    }

    private void loadModel()
    {
        Model model = mEditorPane.processModel();
        SimulationLauncher simulationLauncher = getSimulationLauncher();
        if(null == simulationLauncher)
        {
            throw new IllegalStateException("simulation launcher window does not exist; cannot reload model");
        }
        SimulationLauncher.SetModelResult result = simulationLauncher.setModel(model);
        if(result == SimulationLauncher.SetModelResult.FAILED_RUNNING)
        {
            SimpleDialog dialog = new SimpleDialog(mMainFrame, "unable to reload model", 
                                                   "Sorry, the model cannot be reloaded while a simulation is running.  Please wait for the simulation to complete and then try again.");
            dialog.show();
        }
        else if(result == SimulationLauncher.SetModelResult.FAILED_CLOSED)
        {
            throw new IllegalStateException("unexpected condition:  setModel() called after the simulation launcher has closed");
        }
        else if(result == SimulationLauncher.SetModelResult.SUCCESS)
        {
            simulationLauncher.toFront();
        }
        else
        {
            throw new IllegalStateException("unexpected condition:  unknown result returned from setModel(); result is: " + result.toString());
        }
    }
    
    void handleReload()
    {
        try
        {
            loadModel();
        }
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled errorDialog = new ExceptionDialogOperationCancelled(mMainFrame, "unable to reload the mode", e);
            errorDialog.show();            
        }
    }

    void handleSimulate()
    {
        try
        {
            String appName = getName();
            Model model = mEditorPane.processModel();
            enableSimulateMenuItem(false);
            SimulationLauncher simulationLauncher = new SimulationLauncher(appName, model, false);
            setSimulationLauncher(simulationLauncher);
            enableReloadMenuItem(true);
            simulationLauncher.addListener(new SimulationLauncher.Listener()
            {
                public void simulationLauncherClosing()
                {
                    enableSimulateMenuItem(true);
                    enableReloadMenuItem(false);
                }
                public void simulationStarting()
                {
                    enableReloadMenuItem(false);
                }
                public void simulationEnding()
                {
                    enableReloadMenuItem(true);
                }
            });
        }
        catch(Exception e)
        {
            UnexpectedErrorDialog errorDialog = new UnexpectedErrorDialog(mMainFrame, "unable to create the simulation launcher window");
            enableSimulateMenuItem(true);
            errorDialog.show();            
        }
    }

    private void enableMenu(MainMenu.Menu pMenu, boolean pEnabled)
    {
        try
        {
            mMainMenu.setEnabledFlag(pMenu, pEnabled);
        }
        catch(DataNotFoundException e)
        {
            throw new IllegalStateException("could not find menu " + pMenu.toString());
        }
    }

    private void enableMenuItem(MainMenu.MenuItem pMenuItem, boolean pEnabled)
    {
        try
        {
            mMainMenu.setEnabledFlag(pMenuItem, pEnabled);
        }
        catch(DataNotFoundException e)
        {
            throw new IllegalStateException("could not find menu item " + pMenuItem.toString());
        }
    }

    void enableReloadMenuItem(boolean pEnabled)
    {
        enableMenuItem(MainMenu.MenuItem.TOOLS_RELOAD, pEnabled);
    }

    void enableSaveMenuItem(boolean pEnabled)
    {
        enableMenuItem(MainMenu.MenuItem.FILE_SAVE, pEnabled);
    }

    void enableCloseMenuItem(boolean pEnabled)
    {
        enableMenuItem(MainMenu.MenuItem.FILE_CLOSE, pEnabled);
    }
    
    void enableSimulateMenuItem(boolean pEnabled)
    {
        boolean enabled = false;
        if(pEnabled)
        {
            if(! mEditorPane.editorBufferIsEmpty())
            {
                enabled = true;
            }
        }
        enableMenuItem(MainMenu.MenuItem.TOOLS_SIMULATE, enabled);
    }

    void enableToolsMenu(boolean pEnabled) 
    {
        boolean enabled = false;
        if(pEnabled)
        {
            if(! mEditorPane.editorBufferIsEmpty())
            {
                enabled = true;
            }
        }
        enableMenu(MainMenu.Menu.TOOLS, enabled);
    }

    private void initializeAppConfig(String pAppDir) throws DataNotFoundException, InvalidInputException, FileNotFoundException
    {
        
        AppConfig appConfig = null;
        if(null == pAppDir)
        {
            // we don't know where we are installed, so punt and look for 
            // the config file as a class resource:
            appConfig = new AppConfig(MainApp.class);
        }
        else
        {
            File appDirFile = new File(pAppDir);
            if(! appDirFile.exists())
            {
                throw new DataNotFoundException("could not find application directory: " + pAppDir);
            }
            setAppDir(appDirFile);
            String configFileName = appDirFile.getAbsolutePath() + "/config/" + AppConfig.CONFIG_FILE_NAME;
            appConfig = new AppConfig(new File(configFileName));
        }
        setAppConfig(appConfig);
    }

    String getName()
    {
        return(getAppConfig().getAppName());
    }

    private Container createComponents()
    {
        JPanel mainPane = new JPanel();
        LayoutManager layoutManager = new BoxLayout(mainPane, BoxLayout.Y_AXIS);
        mainPane.setLayout(layoutManager);

        EditorPane editorPane = new EditorPane(mainPane);
        mEditorPane = editorPane;

        return(mainPane);
    }

    private void initializeMainMenu(JFrame pFrame) throws DataNotFoundException
    {
        MainMenu mainMenu = new MainMenu(this);
        mMainMenu = mainMenu;
        enableToolsMenu(false);
        enableSimulateMenuItem(false);
        enableReloadMenuItem(false);
        enableSaveMenuItem(false);
        enableCloseMenuItem(false);
        pFrame.setJMenuBar(mainMenu);
    }

    private void initializeMainFrame() throws DataNotFoundException
    {
        JFrame frame = new JFrame(getName());
        setMainFrame(frame);

        initializeMainMenu(frame);

        Container mainPane = createComponents();
        frame.setContentPane(mainPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.pack();
        frame.setLocation((screenSize.width - frame.getWidth())/2,
                          (screenSize.height - frame.getHeight())/2);
        mOriginalWidthPixels = frame.getWidth();
        mOriginalHeightPixels = frame.getHeight();
        frame.addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                int heightPixels = mMainFrame.getHeight();
                int widthPixels = mMainFrame.getWidth();
                int changeWidthPixels = widthPixels - mOriginalWidthPixels;
                int changeHeightPixels = heightPixels - mOriginalHeightPixels;
                mEditorPane.handleResize(widthPixels - mOriginalWidthPixels,
                                         heightPixels - mOriginalHeightPixels);
            }
        });
        frame.setVisible(true);
    }

    private static String handleWindowsArgumentMangling(String pAppDir)
    {
        String retString = pAppDir;
        if(retString.endsWith("\""))
        {
            retString = retString.substring(0, retString.length() - 1);
            if(! retString.endsWith(File.separator) &&
               ! retString.endsWith("\\") &&
               ! retString.endsWith("/"))
            {
                retString += File.separator;
            }
        }
        return(retString);
    }

    public MainApp(String []pArgs) throws IllegalStateException, ClassNotFoundException, IOException, DataNotFoundException, InvalidInputException
    {
        if(null != mApp)
        {
            throw new IllegalStateException("only one instance of MainApp can exist at a time");
        }
        mApp = this;
        
        String appDir = null;
        if(pArgs.length > 0)
        {
            appDir = pArgs[0];
            appDir = handleWindowsArgumentMangling(appDir);
        }

        initializeAppConfig(appDir);

        ClassRegistry modelBuilderRegistry = new ClassRegistry(org.systemsbiology.chem.scripting.IModelBuilder.class);
        modelBuilderRegistry.buildRegistry();
        setModelBuilderRegistry(modelBuilderRegistry);

        ClassRegistry modelExporterRegistry = new ClassRegistry(org.systemsbiology.chem.scripting.IModelExporter.class);
        modelExporterRegistry.buildRegistry();
        setModelExporterRegistry(modelExporterRegistry);
        setSimulationLauncher(null);

        initializeMainFrame();
    }

    public static MainApp getApp()
    {
        return(mApp);
    }

    public void exit(int pCode)
    {
        System.exit(pCode);
    }

    public static void main(String []pArgs)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            MainApp app = new MainApp(pArgs);
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
