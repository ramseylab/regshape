package isb.chem.app;

import java.util.*;
import isb.util.*;
import isb.chem.scripting.*;
import isb.chem.*;
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
    private ScriptRuntime mScriptRuntime;
    private static MainApp mApp;
    private ScriptBuilder mScriptBuilder;
    private SimulationController mSimulationController;
    private MainMenu mMainMenu;
    private ModelInstanceSimulator mModelInstanceSimulator;
    private AppConfig mAppConfig;
    private File mAppDir;

    private EditorPane mEditorPane;
    private RuntimePane mRuntimePane;
    private RuntimeOutputPane mRuntimeOutputPane;

    EditorPane getEditorPane()
    {
        return(mEditorPane);
    }

    File getAppDir()
    {
        return(mAppDir);
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

    void setSimulator(ModelInstanceSimulator pModelInstanceSimulator)
    {
        mModelInstanceSimulator = pModelInstanceSimulator;
    }
    
    ModelInstanceSimulator getSimulator()
    {
        return(mModelInstanceSimulator);
    }

    private void setSimulationController(SimulationController pSimulationController)
    {
        mSimulationController = pSimulationController;
    }

    SimulationController getSimulationController()
    {
        return(mSimulationController);
    }

    private void setScriptBuilder(ScriptBuilder pScriptBuilder)
    {
        mScriptBuilder = pScriptBuilder;
    }

    ScriptBuilder getScriptBuilder()
    {
        return(mScriptBuilder);
    }

    private void setScriptRuntime(ScriptRuntime pScriptRuntime)
    {
        mScriptRuntime = pScriptRuntime;
    }

    ScriptRuntime getScriptRuntime()
    {
        return(mScriptRuntime);
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
        String modelName = mRuntimePane.getSelectedModelName();
        String speciesPopulationsName = mRuntimePane.getSelectedSpeciesPopulationName();
        ModelInstanceExporter exporter = new ModelInstanceExporter(getMainFrame());
        exporter.exportModelInstance(modelName, speciesPopulationsName);
    }

    void setEnableClearOutputLog(boolean pEnabled)
    {
        mRuntimeOutputPane.setEnableClearOutputLog(pEnabled);
    }

    void updateRuntimePane()
    {
        mRuntimePane.updateMainPanelFromRuntime();
    }

    void clearOutputText()
    {
        mRuntimeOutputPane.clearOutputText();
    }

    void handleSimulate()
    {
        String modelName = mRuntimePane.getSelectedModelName();
        String speciesPopulationsName = mRuntimePane.getSelectedSpeciesPopulationName();
        ModelInstanceSimulator simulator = new ModelInstanceSimulator(getMainFrame());
        setSimulator(simulator);
        mRuntimePane.updateMainPanelFromRuntime();

        boolean success = simulator.simulateModelInstance(modelName, speciesPopulationsName);
        if(! success)
        {
            setSimulator(null);
            mRuntimePane.updateMainPanelFromRuntime();
        }
    }

    void enableExportMenuItem(boolean pEnabled)
    {
        mMainMenu.getExportMenuItem().setEnabled(pEnabled);
    }

    void enableSaveMenuItem(boolean pEnabled)
    {
        mMainMenu.getSaveMenuItem().setEnabled(pEnabled);
    }

    void enableCloseMenuItem(boolean pEnabled)
    {
        mMainMenu.getCloseMenuItem().setEnabled(pEnabled);
    }
    
    void enableProcessMenuItem(boolean pEnabled)
    {
        mMainMenu.getProcessMenuItem().setEnabled(pEnabled);
    }

    void enableSimulateMenuItem(boolean pEnabled)
    {
        mMainMenu.getSimulateMenuItem().setEnabled(pEnabled);
    }

    void setEnableClearRuntime(boolean pEnabled)
    {
        mRuntimePane.setEnableClearRuntime(pEnabled);
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
            if(! appDirFile.exists() ||
               ! appDirFile.isDirectory())
            {
                throw new DataNotFoundException("could not find application directory: " + pAppDir);
            }
            setAppDir(appDirFile);
            String configFileName = appDirFile.getAbsolutePath() + "/config/" + AppConfig.CONFIG_FILE_NAME;
            appConfig = new AppConfig(new File(configFileName));
        }
        setAppConfig(appConfig);
    }

    void clearRuntime()
    {
        getScriptRuntime().clear();
        mRuntimePane.updateMainPanelFromRuntime();
    }

    private Container createComponents()
    {
        JPanel mainPane = new JPanel();
        LayoutManager layoutManager = new BoxLayout(mainPane, BoxLayout.Y_AXIS);
        mainPane.setLayout(layoutManager);

        EditorPane editorPane = new EditorPane(mainPane);
        mEditorPane = editorPane;

        RuntimePane runtimePane = new RuntimePane(mainPane);
        mRuntimePane = runtimePane;

        RuntimeOutputPane runtimeOutputPane = new RuntimeOutputPane(mainPane);
        mRuntimeOutputPane = runtimeOutputPane;

        return(mainPane);
    }

    void appendToOutputLog(String pText)
    {
        mRuntimeOutputPane.appendToOutputLog(pText);
    }

    private void initializeMainFrame()
    {
        JFrame frame = new JFrame(getAppConfig().getAppName());
        setMainFrame(frame);
        MainMenu mainMenu = new MainMenu(this);
        frame.setJMenuBar(mainMenu);
        mMainMenu = mainMenu;
        Container mainPane = createComponents();
        frame.setContentPane(mainPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        frame.setLocation((screenSize.width - frameSize.width) / 2,
                          (screenSize.height - frameSize.height) / 2);

        frame.setVisible(true);
    }

    public MainApp(String []pArgs) throws ScriptRuntimeException, IllegalStateException, ClassNotFoundException, IOException, DataNotFoundException, InvalidInputException
    {
        if(null != mApp)
        {
            throw new IllegalStateException("only one instance of MainApp can exist at a time");
        }
        mApp = this;
        
        String appDir = null;
        if(pArgs.length > 0)
        {
            // argument is the main application directory
            appDir = pArgs[0];
        }

        initializeAppConfig(appDir);

        ScriptRuntime scriptRuntime = new ScriptRuntime();
        SimulationController simulationController = new SimulationController();
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        scriptRuntime.setSimulationController(simulationController);
        setScriptRuntime(scriptRuntime);
        setSimulationController(simulationController);
        setScriptBuilder(scriptBuilder);
        setSimulator(null);

        initializeMainFrame();

        StringWriter runtimeOutputLog = getRuntimeOutputLog();
        PrintWriter outputLogWriter = new PrintWriter(runtimeOutputLog);
        scriptRuntime.setOutputWriter(outputLogWriter);
    }

    void updateOutputText()
    {
        mRuntimeOutputPane.updateOutputText();
    }

    StringWriter getRuntimeOutputLog()
    {
        return(mRuntimeOutputPane.getRuntimeOutputLog());
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
