package isb.chem.app;

import isb.chem.*;
import isb.chem.scripting.*;
import isb.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

public class SimulationLauncher
{
    private Component mMainFrame;
    private Component mThisFrame;
    private Model mModel;
    private SpeciesPopulations mInitialSpeciesPopulations;
    private JTextField mStoreNameField;
    private JTextField mFileNameField;
    private JTextField mStartTimeField;
    private JTextField mStopTimeField;
    private JTextField mNumPointsField;
    private JList mSimulatorsList;
    private JList mSpeciesList;
    private JTextField mEnsembleField;
    private String mOutputType;
    private JButton mStartButton;
    private JButton mStopButton;
    private JButton mResumeButton;
    private JButton mCancelButton;
    private SimulationController mSimulationController;
    private Thread mSimulationRunnerThread;
    private SimulationRunner mSimulationRunner;

    public SimulationLauncher(Component pMainFrame, Model pModel, SpeciesPopulations pInitialSpeciesPopulations)
    {
        mMainFrame = pMainFrame;
        mModel = pModel;
        mInitialSpeciesPopulations = pInitialSpeciesPopulations;
        MainApp app = MainApp.getApp();
        mSimulationController = app.getSimulationController();
        app.setEnableClearRuntime(false);
        createController();
    }

    private JButton createCancelButton()
    {
        JButton cancelButton = new JButton("cancel");
        cancelButton.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleCancelButton();
            }
        } );
        return(cancelButton);
    }

    private void showCancelledSimulationDialog()
    {
        SimpleDialog messageDialog = new SimpleDialog(mMainFrame, "Simulation cancelled", 
                                                      "Your simulation has been cancelled");
        messageDialog.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        messageDialog.show();
    }

    private boolean mSimulationInProgress;


    private boolean getSimulationInProgress()
    {
        return(null != getSimulationScript());
    }

    private SimulationController getSimulationController()
    {
        return(mSimulationController);
    }

    private void updateSimulationControlButtons()
    {
        if(! getSimulationInProgress())
        {
            mStartButton.setEnabled(true);
            mStopButton.setEnabled(false);
            mCancelButton.setEnabled(false);
            mResumeButton.setEnabled(false);
        }
        else
        {
            if(getSimulationController().getStopped())
            {
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(false);
                mCancelButton.setEnabled(true);
                mResumeButton.setEnabled(true);
            }
            else
            {
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(true);
                mCancelButton.setEnabled(true);
                mResumeButton.setEnabled(false);
            }
        }
    }


    private void handleResumeButton()
    {
        if(getSimulationInProgress())
        {
            SimulationController simulationController = getSimulationController();
            if(simulationController.getStopped())
            {
                simulationController.setStopped(false);
                updateSimulationControlButtons();
            }
        }
    }

    private void handleStopButton()
    {
        if(getSimulationInProgress())
        {
            SimulationController simulationController = getSimulationController();
            if(! simulationController.getStopped())
            {
                simulationController.setStopped(true);
                updateSimulationControlButtons();
            }
        }
    }

    private void handleCancelButton()
    {
        if(getSimulationInProgress())
        {
            SimulationController simulationController = getSimulationController();
            if(! simulationController.getCancelled())
            {
                simulationController.setCancelled(true);
            }
            updateSimulationControlButtons();
            showCancelledSimulationDialog();
        }
    }

    private void runSimulation(Script pSimulationScript)
    {
        updateSimulationControlButtons();

        MainApp app = MainApp.getApp();
        ScriptRuntime scriptRuntime = app.getScriptRuntime();

        try
        {
            app.setEnableClearOutputLog(false);
            scriptRuntime.execute(pSimulationScript);
            app.setEnableClearOutputLog(true);
            app.updateMainPanelFromRuntime();
            if(mOutputType.equals(OutputType.PLOT.toString()))
            {
                String outputText = app.getRuntimeOutputLog().toString();
                if(null != outputText && outputText.trim().length() > 0)
                {
                    Plotter plotter = new Plotter(mMainFrame);
                    plotter.plot(outputText);
                }
            }
            app.updateOutputText();
        }
                
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame,
                                                                                             "Failure running simulation",
                                                                                             e);
            dialog.show();
        }

        setSimulationScript(null);
        updateSimulationControlButtons();
    }

    private Script prepareSimulationScript(String pSimulationCommandString)
    {
        MainApp app = MainApp.getApp();
        ScriptBuilder scriptBuilder = app.getScriptBuilder();
        String parserAlias = CommandLanguageParser.CLASS_ALIAS;
        StringReader stringReaderCommand = new StringReader(pSimulationCommandString);
        BufferedReader bufferedStringReaderCommand = new BufferedReader(stringReaderCommand);
        Script script = null;

        try
        {
            script = scriptBuilder.buildFromInputStream(parserAlias,
                                                        bufferedStringReaderCommand);
                
        }
       
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame,
                                                                                             "Failure preparing simulation command",
                                                                                             e);
            dialog.show();
        }

        return(script);
    }

    private Script mSimulationScript;
    
    private synchronized void setSimulationScript(Script pSimulationScript)
    {
        mSimulationScript = pSimulationScript;
    }

    private synchronized Script getSimulationScript()
    {
        return(mSimulationScript);
    }

    class SimulationRunner implements Runnable
    {
        public void run()
        {
            while(true)
            {
                Script simulationScript = getSimulationScript();
                if(null != simulationScript)
                {
                    runSimulation(simulationScript);
                }
                else
                {
                    try
                    {
                        synchronized(this)
                        {
                            this.wait();
                        }
                    }
                    catch(InterruptedException e)
                    {
                        // :TODO: figure out what to do here
                        e.printStackTrace(System.err);
                    }
                }
            }
        }
    }

    private void handleStartButton() 
    {
        if(! getSimulationInProgress())
        {
            SimulationController simulationController = getSimulationController();

            simulationController.setCancelled(false);
            simulationController.setStopped(false);

            String simulationCommandStr = getSimulationCommandString();
            if(null != simulationCommandStr)
            {
                Script simulationScript = prepareSimulationScript(simulationCommandStr);
                if(null != simulationScript)
                {
                    setSimulationScript(simulationScript);
                    synchronized(mSimulationRunner)
                    {
                        mSimulationRunner.notifyAll();
                    }
                }
            }
        }
    }

    private JButton createResumeButton()
    {
        JButton resumeButton = new JButton("resume");
        resumeButton.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleResumeButton();
            }
        } );
        return(resumeButton);
    }

    private JButton createStartButton() 
    {
        JButton startButton = new JButton("start");
        startButton.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleStartButton();
            }
        } );
        return(startButton);
    }

    private JButton createStopButton()
    {
        JButton stopButton = new JButton("stop");
        stopButton.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleStopButton();
            }
        } );
        return(stopButton);
    }    

    private JList createSimulatorsList()
    {
        MainApp app = MainApp.getApp();
        ScriptRuntime scriptRuntime = app.getScriptRuntime();
        Set simulatorAliases = scriptRuntime.getSimulatorAliasesCopy();
        assert (simulatorAliases.size() > 0) : "no simulators found";
        java.util.List simulatorAliasesList = new LinkedList(simulatorAliases);
        Collections.sort(simulatorAliasesList);
        Object []simulatorAliasObjects = simulatorAliasesList.toArray();
        JList simulatorsList = new JList();
        mSimulatorsList = simulatorsList;
        simulatorsList.setVisibleRowCount(4);
        simulatorsList.setListData(simulatorAliasObjects);
        simulatorsList.setSelectedIndex(0);
        simulatorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return(simulatorsList);
    }

    private JPanel createSimulatorsListPanel()
    {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("simulators:");
        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(label);
        JList list = createSimulatorsList();
        JScrollPane scrollPane = new JScrollPane(list);
        box.add(scrollPane);
        panel.add(box);
        panel.setBorder(BorderFactory.createEtchedBorder());
        return(panel);
    }

    private JPanel createButtonPanel()
    {
        JPanel panel = new JPanel();
        Box box = new Box(BoxLayout.Y_AXIS);
        JLabel label = new JLabel("simulation:");
        box.add(label);

        JPanel padding2 = new JPanel();
        padding2.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        box.add(padding2);
        JButton startButton = createStartButton();
        padding2.add(startButton);
        mStartButton = startButton;

        JPanel padding1 = new JPanel();
        padding1.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        JButton cancelButton = createCancelButton();
        padding1.add(cancelButton);
        box.add(padding1);
        mCancelButton = cancelButton;

        JPanel padding3 = new JPanel();
        padding3.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        box.add(padding3);
        JButton stopButton = createStopButton();
        padding3.add(stopButton);
        mStopButton = stopButton;

        JPanel padding4 = new JPanel();
        padding4.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        box.add(padding4);
        JButton resumeButton = createResumeButton();
        padding4.add(resumeButton);
        mResumeButton = resumeButton;

        panel.add(box);
        panel.setBorder(BorderFactory.createEtchedBorder());
        return(panel);
    }

    private JList createSpeciesList()
    {
        JList speciesListBox = new JList();
        mSpeciesList = speciesListBox;
        Set speciesSet = mModel.getSpeciesSetCopy();
        java.util.List speciesList = new LinkedList();
        Iterator speciesIter = speciesSet.iterator();
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            String speciesName = species.getName();
            speciesList.add(speciesName);
        }
        Collections.sort(speciesList);
        Object []speciesArray = speciesList.toArray();
        speciesListBox.setListData(speciesArray);
        speciesListBox.setVisibleRowCount(6);
        speciesListBox.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return(speciesListBox);
    }

    private JPanel createSpeciesListPanel()
    {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("view species:");
        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(label);
        JList list = createSpeciesList();
        JScrollPane scrollPane = new JScrollPane(list);
        box.add(scrollPane);
        panel.add(box);
        panel.setBorder(BorderFactory.createEtchedBorder());
        return(panel);
    }

    private static final int NUM_COLUMNS_TIME_FIELD = 10;

    private JPanel createStartStopTimePanel()
    {
        JPanel panel = new JPanel();
        Box box = new Box(BoxLayout.Y_AXIS);

        JPanel startPanel = new JPanel();
        JLabel startLabel = new JLabel("start time:");
        startPanel.add(startLabel);
        JTextField startField = new JTextField("0.0", NUM_COLUMNS_TIME_FIELD);
        mStartTimeField = startField;
        startPanel.add(startField);
        box.add(startPanel);

        JPanel stopPanel = new JPanel();
        JLabel stopLabel = new JLabel("stop time:");
        stopPanel.add(stopLabel);
        JTextField stopField = new JTextField(NUM_COLUMNS_TIME_FIELD);
        mStopTimeField = stopField;
        stopPanel.add(stopField);
        box.add(stopPanel);

        JPanel numPointsPanel = new JPanel();
        JLabel numPointsLabel = new JLabel("num samples:");
        numPointsPanel.add(numPointsLabel);
        JTextField numPointsField = new JTextField("100", NUM_COLUMNS_TIME_FIELD);
        mNumPointsField = numPointsField;
        numPointsPanel.add(numPointsField);
        box.add(numPointsPanel);

        JPanel ensemblePanel = new JPanel();
        JPanel ensembleLabelPanel = new JPanel();
        Box ensembleLabelBox = new Box(BoxLayout.Y_AXIS);
        JLabel ensembleLabel1 = new JLabel("ensemble average:");
        JLabel ensembleLabel2 = new JLabel("(optional field)");
        ensembleLabelBox.add(ensembleLabel1);
        ensembleLabelBox.add(ensembleLabel2);
        ensembleLabelPanel.add(ensembleLabelBox);
        ensemblePanel.add(ensembleLabelPanel);
        JTextField ensembleField = new JTextField(NUM_COLUMNS_TIME_FIELD);
        mEnsembleField = ensembleField;
        ensemblePanel.add(ensembleField);
        box.add(ensemblePanel);

        panel.add(box);
        panel.setBorder(BorderFactory.createEtchedBorder());

        return(panel);
    }

    private static final int NUM_COLUMNS_STORE_NAME = 12;
    private static final int NUM_COLUMNS_FILE_NAME = 12;

    static class OutputType
    {
        private final String mName;
        private static HashMap mMap;

        static
        {
            mMap = new HashMap();
        }

        private OutputType(String pName)
        {
            mName = pName;
            mMap.put(mName, this);
        }
        public static OutputType get(String pName)
        {
            return((OutputType) mMap.get(pName));
        }
        public String toString()
        {
            return(mName);
        }
        public static final OutputType PRINT = new OutputType("print");
        public static final OutputType PLOT = new OutputType("plot");
        public static final OutputType FILE = new OutputType("file");
        public static final OutputType STORE = new OutputType("store");
    }

    private void handleBadInput(String pTitle, String pMessage)
    {
        SimpleDialog dialog = new SimpleDialog(mThisFrame,
                                               pTitle,
                                               pMessage);
        dialog.show();
    }

    private String getSimulationCommandString()
    {
        StringBuffer sb = new StringBuffer();
        String retVal = null;
        sb.append("simulate \"" + mModel.getName() + "\": speciesPopulations \"" + mInitialSpeciesPopulations.getName() + "\"");

        String startTimeStr = mStartTimeField.getText();
        Double startTime = null;
        if(null != startTimeStr)
        {
            try
            {
                startTime = new Double(startTimeStr);
            }
            catch(NumberFormatException e)
            {
                // do nothing
            }
        }
        if(null == startTime)
        {
            handleBadInput("invalid start time", "The start time that you specified is invalid.\nPlease enter a numeric start time.");
            return(retVal);
        }
        double startTimeVal = startTime.doubleValue();
        sb.append(", startTime " + startTimeVal);

        String stopTimeStr = mStopTimeField.getText();
        Double stopTime = null;
        if(null != stopTimeStr)
        {
            try
            {
                stopTime = new Double(stopTimeStr);
            }
            catch(NumberFormatException e)
            {
                // do nothing
            }
        }
        if(null == stopTime || stopTime.doubleValue() <= startTimeVal)
        {
            handleBadInput("invalid stop time", "The stop time that you specified is invalid.\nPlease enter a numeric stop time.");
            return(retVal);
        }
        double stopTimeVal = stopTime.doubleValue();
        sb.append(", stopTime " + stopTimeVal);

        String numPointsStr = mNumPointsField.getText();
        Integer numPoints = null;
        if(null != numPointsStr)
        {
            try
            {
                numPoints = new Integer(numPointsStr);
            }
            catch(NumberFormatException e)
            {
                // do nothing
            }
        }
        if(null == numPoints)
        {
            handleBadInput("invalid number of samples", "The number of samples specified is invalid.\nPlease enter an integer number of samples.");
            return(retVal);
        }
        int numPointsVal = numPoints.intValue();
        sb.append(", numTimePoints " + numPointsVal);

        int simulatorIndex = mSimulatorsList.getSelectedIndex();
        String simulatorAlias = null;
        if(simulatorIndex != -1)
        {
            simulatorAlias = (String) mSimulatorsList.getModel().getElementAt(simulatorIndex);
        }
        if(simulatorAlias == null)
        {
            handleBadInput("no simulator was selected", "Please select a simulator to use");
            return(retVal);
        }
        sb.append(", simulator \"" + simulatorAlias + "\"");

        String ensembleStr = mEnsembleField.getText();
        
        Integer ensembleSize = null;
        if(null != ensembleStr && ensembleStr.trim().length() > 0)
        {
            try
            {
                ensembleSize = new Integer(ensembleStr);
            }
            catch(NumberFormatException e)
            {
                // do nothing
            }
            if(null == ensembleSize || ensembleSize.intValue() <= 0)
            {
                handleBadInput("invalid ensemble size", "The ensemble size you specified is invalid");
                return(retVal);
            }
        }
        if(ensembleSize != null)
        {
            sb.append(", ensembleSize " + ensembleSize);
        }

        sb.append(", viewSpecies (");

        Object []speciesSelected = mSpeciesList.getSelectedValues();
        if(speciesSelected.length == 0)
        {
            handleBadInput("no species selected", "Please select at least one species to observe");
            return(retVal);
        }
        int numSpecies = speciesSelected.length;
        for(int ctr = 0; ctr < numSpecies; ctr++)
        {
            String speciesName = (String) speciesSelected[ctr];
            sb.append("\"" + speciesName + "\"");
            if(ctr < numSpecies - 1)
            {
                sb.append(" ");
            }
        }
        sb.append(")");
        
        String outputTypeStr = mOutputType;
        OutputType outputType = OutputType.get(outputTypeStr);
        if(outputType.equals(OutputType.STORE))
        {
            String storeName = mStoreNameField.getText();
            if(null == storeName || storeName.trim().length() == 0)
            {
                handleBadInput("store variable name not specified", "Please specify the variable name for storing the output");
                return(retVal);
            }

            if(MainApp.getApp().getScriptRuntime().getSpeciesPopulationsNamesCopy().contains(storeName))
            {
                handleBadInput("store variable name already exists", "The store variable name you specified already exists: " + storeName + "\nPlease specify a different variable name.");
                return(retVal);
            }

            if(! MathExpression.isValidSymbol(storeName))
            {
                handleBadInput("store variable name not valid", "The store variable name you specified is not a valid name: " + storeName + "\nPlease specify a different variable name, like \"s1\" or \"myOutput\".");
                return(retVal);
                
            }

            if(null != ensembleSize)
            {
                mEnsembleField.setText("");
                handleBadInput("ensemble average not allowed with storing", "Ensemble averaging is not allowed when storing results.\nYour ensemble average specification has been cleared.\nTo allow ensemble averaging, you must change the output type.");
                return(retVal);
            }
            
            sb.append(", output store, storeName \"" + storeName + "\"");
        }
        else if(outputType.equals(OutputType.FILE))
        {
            String fileName = mFileNameField.getText();
            if(null == fileName || fileName.trim().length() == 0)
            {
                handleBadInput("output file name was not specified", "Saving the results to a file requires specifying a file name");
                return(retVal);
            }

            File outputFile = new File(fileName);
            if(outputFile.exists())
            {
                SimpleTextArea textArea = new SimpleTextArea("The results file you selected already exists:\n" + fileName + "\nThe simulation operation will overwrite this file.\nAre you sure you want to proceed?");
                
                SimpleDialog messageDialog = new SimpleDialog(mMainFrame, 
                                                              "Overwrite existing file?",
                                                              textArea);
                messageDialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
                messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
                messageDialog.show();
                Integer response = (Integer) messageDialog.getValue();
                if(null != response &&
                   response.intValue() == JOptionPane.YES_OPTION)
                {
                    // yes, the user wants to proceed
                }
                else
                {
                    if(null == response)
                    {
                        showCancelledSimulationDialog();
                    }
                    return(retVal);
                }
            }

            File outputFileDir = outputFile.getParentFile();
            if(null != outputFileDir && ! outputFileDir.exists())
            {
                handleBadInput("output file in a nonexistant directory", "You specified an output file in a nonexistant directory:\n" + outputFile.getAbsolutePath());
                return(retVal);
            }

            sb.append(", output print, outputFile \"" + fileName + "\"");
        }
        else if(outputType.equals(OutputType.PRINT) ||
                outputType.equals(OutputType.PLOT))
        {
            sb.append(", output print");
        }

        sb.append(";");
        retVal = sb.toString();
        return(retVal);
    }

    private void handleButtonEvent(ActionEvent e)
    {
        String outputTypeStr = e.getActionCommand();
        mOutputType = outputTypeStr;
        OutputType outputType = OutputType.get(outputTypeStr);
        if(null != outputType)
        {
            if(outputType.equals(OutputType.PRINT))
            {
                mStoreNameField.setEnabled(false);
                mFileNameField.setEnabled(false);
                mEnsembleField.setEnabled(true);
            }
            else if(outputType.equals(OutputType.PLOT))
            {
                mStoreNameField.setEnabled(false);
                mFileNameField.setEnabled(false);
                mEnsembleField.setEnabled(true);
            }
            else if(outputType.equals(OutputType.FILE))
            {
                mStoreNameField.setEnabled(false);
                mFileNameField.setEnabled(true);
                mEnsembleField.setEnabled(true);
            }
            else if(outputType.equals(OutputType.STORE))
            {
                mStoreNameField.setEnabled(true);
                mFileNameField.setEnabled(false);
                mEnsembleField.setEnabled(false);
            }
            else
            {
                assert false: "unknown output type";
            }
        }
    }

    private JPanel createOutputPanel()
    {
        ButtonGroup buttonGroup = new ButtonGroup();
        
        JPanel outputPanel = new JPanel();
        outputPanel.setBorder(BorderFactory.createEtchedBorder());

        Box outputBox = new Box(BoxLayout.Y_AXIS);
        
        JLabel outputLabel = new JLabel("Output Type -- specify what do do with the simulation results:");
        outputBox.add(outputLabel);

        ActionListener buttonListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleButtonEvent(e);
            }
        };

        JPanel printPlotPanel = new JPanel();
        JRadioButton printButton = new JRadioButton(OutputType.PRINT.toString(), true);
        printButton.addActionListener(buttonListener);
        printButton.setSelected(true);
        mOutputType = OutputType.PRINT.toString();
        buttonGroup.add(printButton);
        printPlotPanel.add(printButton);

        JRadioButton plotButton = new JRadioButton(OutputType.PLOT.toString(), true);
        plotButton.addActionListener(buttonListener);
        buttonGroup.add(plotButton);
        printPlotPanel.add(plotButton);
        outputBox.add(printPlotPanel);

        JPanel storePanel = new JPanel();
        JRadioButton storeButton = new JRadioButton(OutputType.STORE.toString(), false);
        storeButton.addActionListener(buttonListener);
        buttonGroup.add(storeButton);
        storePanel.add(storeButton);
        JPanel storeNamePanel = new JPanel();
        Box storeBox = new Box(BoxLayout.Y_AXIS);
        JLabel storeLabel = new JLabel("store results under variable name:");
        JTextField storeNameField = new JTextField(NUM_COLUMNS_STORE_NAME);
        storeBox.add(storeLabel);
        storeBox.add(storeNameField);
        storeNamePanel.add(storeBox);
        mStoreNameField = storeNameField; 
        mStoreNameField.setEnabled(false);
        storePanel.add(storeNamePanel);
        outputBox.add(storePanel);

        JPanel filePanel = new JPanel();
        JRadioButton fileButton = new JRadioButton(OutputType.FILE.toString(), false);
        fileButton.addActionListener(buttonListener);
        buttonGroup.add(fileButton);
        filePanel.add(fileButton);
        JPanel fileNamePanel = new JPanel();
        Box fileBox = new Box(BoxLayout.Y_AXIS);
        JLabel fileLabel = new JLabel("save results as a text (CSV) file:");
        JTextField fileNameField = new JTextField(NUM_COLUMNS_FILE_NAME);
        fileBox.add(fileLabel);
        fileBox.add(fileNameField);
        fileNamePanel.add(fileBox);
        mFileNameField = fileNameField;
        mFileNameField.setEnabled(false);
        filePanel.add(fileNamePanel);
        outputBox.add(filePanel);

        outputPanel.add(outputBox);
        return(outputPanel);
    }

    private JPanel createModelSpeciesLabelPanel()
    {
        JPanel labelPanel = new JPanel();
        labelPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel modelLabel = new JLabel("model name: [" + mModel.getName() + "]");
        JLabel speciesLabel = new JLabel("initial species populations name: [" + mInitialSpeciesPopulations.getName() + "]");
        labelPanel.add(modelLabel);
        JPanel padding = new JPanel();
        padding.setBorder(BorderFactory.createEmptyBorder(1, 20, 1, 20));
        labelPanel.add(padding);
        labelPanel.add(speciesLabel);
        return(labelPanel);
    }

    private void createSimulationRunnerThread()
    {
        SimulationRunner simulationRunner = new SimulationRunner();
        Thread simulationRunnerThread = new Thread(simulationRunner);
        simulationRunnerThread.setDaemon(true);
        simulationRunnerThread.start();
        mSimulationRunnerThread = simulationRunnerThread;
        mSimulationRunner = simulationRunner;
    }

    private void handleCloseSimulationLauncher()
    {
        handleCancelButton();
        MainApp app = MainApp.getApp();
        app.setSimulator(null);
        app.updateMainPanelFromRuntime();
        app.setEnableClearRuntime(true);
        app.setEnableClearOutputLog(true);
    }

    private void createController()
    {
        JFrame frame = new JFrame(MainApp.getApp().getAppConfig().getAppName() + ": simulator");
        JPanel controllerPanel = new JPanel();
        Box box = new Box(BoxLayout.Y_AXIS);
        
        // Add listener for "window-close" event
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleCloseSimulationLauncher();
            }
        });

        JPanel labelPanel = createModelSpeciesLabelPanel();
        box.add(labelPanel);

        JPanel midPanel = new JPanel();
        JPanel buttonPanel = createButtonPanel();
        midPanel.add(buttonPanel);
        JPanel simulatorsListPanel = createSimulatorsListPanel();
        midPanel.add(simulatorsListPanel);
        JPanel startStopTimePanel = createStartStopTimePanel();
        midPanel.add(startStopTimePanel);
        JPanel speciesListPanel = createSpeciesListPanel();
        midPanel.add(speciesListPanel);

        box.add(midPanel);

        JPanel outputPanel = createOutputPanel();
        box.add(outputPanel);

        controllerPanel.add(box);

        frame.getContentPane().add(controllerPanel);
        frame.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        frame.setLocation((screenSize.width - frameSize.width) / 2,
                          (screenSize.height - frameSize.height) / 2);

        mThisFrame = frame;

        setSimulationScript(null);
        updateSimulationControlButtons();

        createSimulationRunnerThread();

        frame.setVisible(true);
    }
}
    
