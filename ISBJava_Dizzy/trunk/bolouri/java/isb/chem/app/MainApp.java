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
    private StringWriter mRuntimeOutputLog;
    private JTextArea mOutputTextArea;
    private ModelNamesList mModelNamesListBox;
    private SpeciesPopulationNamesList mSpeciesPopulationNamesListBox;
    private SimulationController mSimulationController;
    private MainMenu mMainMenu;
    private ModelInstanceSimulator mModelInstanceSimulator;
    private JButton mClearRuntimeButton;
    private JButton mClearOutputButton;
    private AppConfig mAppConfig;
    private File mAppDir;

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

    private void setRuntimeOutputLog(StringWriter pRuntimeOutputLog)
    {
        mRuntimeOutputLog = pRuntimeOutputLog;
    }

    StringWriter getRuntimeOutputLog()
    {
        return(mRuntimeOutputLog);
    }

    private void setOutputTextArea(JTextArea pOutputTextArea)
    {
        mOutputTextArea = pOutputTextArea;
    }

    JTextArea getOutputTextArea()
    {
        return(mOutputTextArea);
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

    private JFrame getMainFrame()
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

    void handleHelpUserManual()
    {
        HelpUserManual helpUserManual = new HelpUserManual(getMainFrame());
        helpUserManual.displayUserManual();
    }

    void handleExport()
    {
        String modelName = mModelNamesListBox.getSelectedModelName();
        String speciesPopulationsName = mSpeciesPopulationNamesListBox.getSelectedSpeciesPopulationName();
        ModelInstanceExporter exporter = new ModelInstanceExporter(getMainFrame());
        exporter.exportModelInstance(modelName, speciesPopulationsName);
    }

    void handleSimulate()
    {
        String modelName = mModelNamesListBox.getSelectedModelName();
        String speciesPopulationsName = mSpeciesPopulationNamesListBox.getSelectedSpeciesPopulationName();
        ModelInstanceSimulator simulator = new ModelInstanceSimulator(getMainFrame());
        setSimulator(simulator);
        updateMainPanelFromRuntime();

        boolean success = simulator.simulateModelInstance(modelName, speciesPopulationsName);
        if(! success)
        {
            setSimulator(null);
            updateMainPanelFromRuntime();
        }
    }

    void handleOpen()
    {
        FileOpenChooser chooser = new FileOpenChooser(getMainFrame());
        chooser.show();
        String fileName = chooser.getFileName();
        String parserAlias = chooser.getParserAlias();
        if(null != fileName && null != parserAlias)
        {
            FileLoader loader = new FileLoader(getMainFrame());
            loader.loadFile(fileName, parserAlias);
        }
    }

    private static final int OUTPUT_TEXT_AREA_NUM_ROWS = 24;
    private static final int OUTPUT_TEXT_AREA_NUM_COLS = 80;

    private void initializeOutputTextArea(Container pMainPane)
    {
        JPanel outputTextPane = new JPanel();
        outputTextPane.setBorder(BorderFactory.createEtchedBorder());
        outputTextPane.setLayout(new FlowLayout());

        JPanel labelButtonPane = new JPanel();
        BoxLayout layout = new BoxLayout(labelButtonPane, BoxLayout.Y_AXIS);
        labelButtonPane.setLayout(layout);

        JButton clearButton = new JButton("clear runtime output log");
        mClearOutputButton = clearButton;
        clearButton.addActionListener( new ActionListener()
                                      {
                                          public void actionPerformed(ActionEvent e)
                                          {
                                              clearOutputText();
                                          }
                                      });

        labelButtonPane.add(clearButton);
        JPanel labelPanel = new JPanel();
        JLabel outputTextLabel = new JLabel("runtime output log:");
        labelPanel.add(outputTextLabel);
        labelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelButtonPane.add(labelPanel);

        outputTextPane.add(labelButtonPane);

        JTextArea outputTextArea = new JTextArea(OUTPUT_TEXT_AREA_NUM_ROWS,
                                                 OUTPUT_TEXT_AREA_NUM_COLS);
        outputTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        outputTextPane.add(scrollPane);
        setOutputTextArea(outputTextArea);

        pMainPane.add(outputTextPane);
    }

    void updateMainPanelFromRuntime()
    {
        int numModels = mModelNamesListBox.updateModelNames();
        int numSpeciesPops = mSpeciesPopulationNamesListBox.updateSpeciesPopulationNames();
        if(numModels > 0 && numSpeciesPops > 0)
        {
            mMainMenu.getExportMenuItem().setEnabled(true);
            if(null == getSimulator())
            {
                mMainMenu.getSimulateMenuItem().setEnabled(true);
            }
            else
            {
                mMainMenu.getSimulateMenuItem().setEnabled(false);
            }
        }
        else
        {
            mMainMenu.getExportMenuItem().setEnabled(false);
            mMainMenu.getSimulateMenuItem().setEnabled(false);
        }
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
        updateMainPanelFromRuntime();
    }

    void clearRuntimeOutputLog()
    {
        StringWriter runtimeOutputLog = getRuntimeOutputLog();
        StringBuffer buffer = runtimeOutputLog.getBuffer();
        buffer.delete(0, buffer.toString().length());
    }

    void updateOutputText()
    {
        StringWriter runtimeOutputLog = getRuntimeOutputLog();
        String newOutputText = runtimeOutputLog.toString();
        getOutputTextArea().append(newOutputText);
        clearRuntimeOutputLog();
    }

    void clearOutputText()
    {
        int textLen = getOutputTextArea().getText().length();
        getOutputTextArea().replaceRange(null, 0, textLen);
        clearRuntimeOutputLog();
    }

    private void initializeSpeciesPopulationNamesList(Container pPane)
    {
        SpeciesPopulationNamesList speciesPopulationNamesList = new SpeciesPopulationNamesList(pPane);
        mSpeciesPopulationNamesListBox = speciesPopulationNamesList;
    }

    private void initializeModelNamesList(Container pPane)
    {
        ModelNamesList modelNamesList = new ModelNamesList(pPane);
        mModelNamesListBox = modelNamesList;
    }

    private void initializeRuntimePane(Container pMainPane)
    {
        JPanel runtimePanel = new JPanel();
        runtimePanel.setBorder(BorderFactory.createEtchedBorder());
        LayoutManager layoutManager = new FlowLayout();
        runtimePanel.setLayout(layoutManager);
        initializeClearRuntimeButton(runtimePanel);
        initializeModelNamesList(runtimePanel);
        initializeSpeciesPopulationNamesList(runtimePanel);
        pMainPane.add(runtimePanel);
    }

    private void initializeClearRuntimeButton(Container pPane)
    {
        JButton clearButton = new JButton("clear runtime variables");
        mClearRuntimeButton = clearButton;

        clearButton.addActionListener( new ActionListener()
                                      {
                                          public void actionPerformed(ActionEvent e)
                                          {
                                              clearRuntime();
                                          }
                                      });
        pPane.add(clearButton);
    }

    void setEnableClearRuntime(boolean pEnable)
    {
        mClearRuntimeButton.setEnabled(pEnable);
    }

    void setEnableClearOutputLog(boolean pEnable)
    {
        mClearOutputButton.setEnabled(pEnable);
    }


    private Container createComponents()
    {
        JPanel mainPane = new JPanel();
        LayoutManager layoutManager = new BoxLayout(mainPane, BoxLayout.Y_AXIS);
        mainPane.setLayout(layoutManager);
        initializeOutputTextArea(mainPane);
        initializeRuntimePane(mainPane);
        return(mainPane);
    }

    private void initializeMainFrame()
    {
        JFrame frame = new JFrame(getAppConfig().getAppName());
        setMainFrame(frame);
        Container mainPane = createComponents();
        frame.setContentPane(mainPane);
        MainMenu mainMenu = new MainMenu(this);
        frame.setJMenuBar(mainMenu);
        mMainMenu = mainMenu;
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
        StringWriter outputLog = new StringWriter();
        PrintWriter outputLogWriter = new PrintWriter(outputLog);
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        scriptRuntime.setOutputWriter(outputLogWriter);
        scriptRuntime.setSimulationController(simulationController);
        setScriptRuntime(scriptRuntime);
        setRuntimeOutputLog(outputLog);
        setSimulationController(simulationController);
        setScriptBuilder(scriptBuilder);
        setSimulator(null);
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
