package org.systemsbiology.chem.app;

import org.systemsbiology.chem.*;
import org.systemsbiology.chem.scripting.*;
import org.systemsbiology.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;

public class SimulationLauncher
{
    private static final int OUTPUT_TEXT_AREA_NUM_ROWS = 20;
    private static final int OUTPUT_TEXT_AREA_NUM_COLS = 40;

    private ClassRegistry mSimulatorRegistry;
    private Component mMainFrame;
    private Model mModel;
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
    private SimulationRunParameters mSimulationRunParameters;
    private String mAppName;
    private MainApp mMainApp;

    public SimulationLauncher(String pAppName,
                              Model pModel,
                              MainApp pApp) throws ClassNotFoundException, IOException
    {
        setMainApp(pApp);
        JFrame frame = new JFrame();
        createLauncher(frame, pAppName, pModel);
        frame.setVisible(true);
    }

    public SimulationLauncher(String pAppName,
                              Model pModel,
                              JDesktopPane pContainingPane,
                              MainApp pApp) throws ClassNotFoundException, IOException
    {
        setMainApp(pApp);
        JInternalFrame internalFrame = new JInternalFrame();
        pContainingPane.add(internalFrame);
        createLauncher(internalFrame, pAppName, pModel);
        internalFrame.setVisible(true);
    }

    void setMainApp(MainApp pMainApp)
    {
        mMainApp = pMainApp;
    }

    private MainApp getMainApp()
    {
        return(mMainApp);
    }

    public Set getSimulatorAliasesCopy()
    {
        return(getSimulatorRegistry().getRegistryAliasesCopy());
    }

    private ClassRegistry getSimulatorRegistry()
    {
        return(mSimulatorRegistry);
    }

    private void setSimulatorRegistry(ClassRegistry pSimulatorRegistry)
    {
        mSimulatorRegistry = pSimulatorRegistry;
    }

    private SimulationRunParameters getSimulationRunParameters()
    {
        return(mSimulationRunParameters);
    }

    private void setSimulationRunParameters(SimulationRunParameters pSimulationRunParameters)
    {
        mSimulationRunParameters = pSimulationRunParameters;
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
        return(null != getSimulationRunParameters());
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

    private void handleOutput(OutputType pOutputType,
                              String pOutputFileName,
                              String []pRequestedSymbolNames,
                              double []pTimeValues,
                              Object []pSymbolValues) throws IOException
    {
        StringWriter outputBuffer = null;
        PrintWriter printWriter = null;
        if(! pOutputType.equals(OutputType.FILE))
        {
            outputBuffer = new StringWriter();
            printWriter = new PrintWriter(outputBuffer);
        }
        else
        {
            assert (null != pOutputFileName) : "null output file name";
            File file = new File(pOutputFileName);
            FileWriter fileWriter = new FileWriter(file);
            printWriter = new PrintWriter(fileWriter);
        }
            
        TimeSeriesSymbolValuesReporter.reportTimeSeriesSymbolValues(printWriter,
                                                                    pRequestedSymbolNames,
                                                                    pTimeValues,
                                                                    pSymbolValues);

        if(pOutputType.equals(OutputType.PRINT))
        {
            JTextArea outputTextArea = new JTextArea(OUTPUT_TEXT_AREA_NUM_ROWS,
                                                     OUTPUT_TEXT_AREA_NUM_COLS);
            outputTextArea.append(outputBuffer.toString());
            outputTextArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(outputTextArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            JPanel outputTextAreaPane = new JPanel();
            outputTextAreaPane.add(scrollPane);
            JOptionPane optionPane = new JOptionPane(outputTextAreaPane);
            JDialog dialog = optionPane.createDialog(mMainFrame, "simulation output");
            dialog.show();
        }
        else if(pOutputType.equals(OutputType.PLOT))
        {
            Plotter plotter = new Plotter(mMainFrame);
            plotter.plot(outputBuffer.toString());
        }
        else
        {
            printWriter.flush();
            JOptionPane optionPane = new JOptionPane("output saved to file:\n" + pOutputFileName);
            JDialog dialog = optionPane.createDialog(mMainFrame, "output saved");
            dialog.show();
        }
    }

    private void runSimulation(SimulationRunParameters pSimulationRunParameters)
    {
        updateSimulationControlButtons();

        try
        {
            ISimulator simulator = pSimulationRunParameters.mSimulator;

            simulator.simulate(pSimulationRunParameters.mStartTime,
                               pSimulationRunParameters.mEndTime,
                               pSimulationRunParameters.mNumTimePoints,
                               pSimulationRunParameters.mNumSteps,
                               pSimulationRunParameters.mRequestedSymbolNames,
                               pSimulationRunParameters.mRetTimeValues,
                               pSimulationRunParameters.mRetSymbolValues);

            handleOutput(pSimulationRunParameters.mOutputType,
                         pSimulationRunParameters.mOutputFileName,
                         pSimulationRunParameters.mRequestedSymbolNames, 
                         pSimulationRunParameters.mRetTimeValues, 
                         pSimulationRunParameters.mRetSymbolValues);
        }
                
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame,
                                                                                             "Failure running simulation",
                                                                                             e);
            dialog.show();
        }

        setSimulationRunParameters(null);
        updateSimulationControlButtons();
    }

    class SimulationRunParameters
    {
        ISimulator mSimulator;
        double mStartTime;
        double mEndTime;
        int mNumTimePoints;
        int mNumSteps;
        OutputType mOutputType;
        String mOutputFileName;
        String []mRequestedSymbolNames;
        double []mRetTimeValues;
        Object []mRetSymbolValues;
    }

    class SimulationRunner implements Runnable
    {
        public void run()
        {
            while(true)
            {
                SimulationRunParameters simulationRunParameters = getSimulationRunParameters();
                if(null != simulationRunParameters)
                {
                    runSimulation(simulationRunParameters);
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

            SimulationRunParameters simulationRunParameters = createSimulationRunParameters();
            if(null != simulationRunParameters)
            {
                setSimulationRunParameters(simulationRunParameters);
                synchronized(mSimulationRunner)
                {
                    mSimulationRunner.notifyAll();
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
        Set simulatorAliases = getSimulatorAliasesCopy();
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
        Object []speciesArray = mModel.getOrderedSpeciesNamesArray();
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
        JLabel ensembleLabel1 = new JLabel("number of ensembles /");
        JLabel ensembleLabel2 = new JLabel("number of timesteps");
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
    }

    private void handleBadInput(String pTitle, String pMessage)
    {
        SimpleDialog dialog = new SimpleDialog(mMainFrame,
                                               pTitle,
                                               pMessage);
        dialog.show();
    }

    private SimulationRunParameters createSimulationRunParameters()
    {
        SimulationRunParameters srp = new SimulationRunParameters();
        
        String startTimeStr = mStartTimeField.getText();
        Double startTime = null;
        SimulationRunParameters retVal = null;
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

        srp.mStartTime = startTimeVal;

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

        srp.mEndTime = stopTimeVal;

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
        int numTimePoints = numPoints.intValue();

        srp.mNumTimePoints = numTimePoints;
 
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

        ISimulator simulator = null;

        try
        {
            simulator = (ISimulator) getSimulatorRegistry().getInstance(simulatorAlias);
            if(! simulator.isInitialized())
            {
                simulator.initialize(mModel,
                                     mSimulationController);
            }
        }
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame,
                                                                                             "Failed to instantiate simulator",
                                                                                             e);
            dialog.show();
            return(retVal);
        }

        srp.mSimulator = simulator;

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
        if(ensembleSize == null)
        {
            ensembleSize = new Integer(1);
        }
        srp.mNumSteps = ensembleSize.intValue();

        Object []speciesSelected = mSpeciesList.getSelectedValues();
        if(speciesSelected.length == 0)
        {
            handleBadInput("no species selected", "Please select at least one species to observe");
            return(retVal);
        }
        int numSpecies = speciesSelected.length;
        String []speciesSelectedNames = new String[numSpecies];
        for(int ctr = 0; ctr < numSpecies; ctr++)
        {
            String speciesName = (String) speciesSelected[ctr];
            speciesSelectedNames[ctr] = speciesName;
        }
        srp.mRequestedSymbolNames = speciesSelectedNames;

        double []timeValues = new double[numTimePoints];
        srp.mRetTimeValues = timeValues;

        Object []symbolValues = new Object[numTimePoints];
        srp.mRetSymbolValues = symbolValues;
     
        String outputTypeStr = mOutputType;
        OutputType outputType = OutputType.get(outputTypeStr);
        assert (null != outputType) : "null output type";
            
        srp.mOutputType = outputType;
        if(srp.mOutputType.equals(OutputType.FILE))
        {
            String fileName = mFileNameField.getText();
            if(null == fileName || fileName.trim().length() == 0)
            {
                handleBadInput("output file name was not specified", "Saving the results to a file requires specifying a file name");
                return(retVal);
            }
            srp.mOutputFileName = fileName;
        }

        retVal = srp;
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
                mFileNameField.setEnabled(false);
                mEnsembleField.setEnabled(true);
            }
            else if(outputType.equals(OutputType.PLOT))
            {
                mFileNameField.setEnabled(false);
                mEnsembleField.setEnabled(true);
            }
            else if(outputType.equals(OutputType.FILE))
            {
                mFileNameField.setEnabled(true);
                mEnsembleField.setEnabled(true);
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
        labelPanel.add(modelLabel);
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
        MainApp app = getMainApp();
        if(null != app)
        {
            app.enableSimulateMenuItem(true);
        }
        else
        {
            System.exit(0);
        }
        handleCancelButton();
    }

    private void createLauncher(Component pMainFrame, 
                                String pAppName,
                                Model pModel) throws IOException, ClassNotFoundException
    {
        Component frame = pMainFrame;
        mModel = pModel;
        mAppName = pAppName;
        String appTitle = pAppName + ": simulator";

        MainApp app = getMainApp();
        if(null != app)
        {
            app.enableSimulateMenuItem(false);
        }

        // create simulator aliases
        ClassRegistry classRegistry = new ClassRegistry(org.systemsbiology.chem.ISimulator.class);
        classRegistry.buildRegistry();
        mSimulatorRegistry = classRegistry;

        SimulationController controller = new SimulationController();
        mSimulationController = controller;

        JPanel controllerPanel = new JPanel();
        Box box = new Box(BoxLayout.Y_AXIS);

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

        Container contentPane = null;
        // Add listener for "window-close" event
        if(frame instanceof JFrame)
        {
            JFrame myFrame = (JFrame) frame;
            myFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    handleCloseSimulationLauncher();
                }
            });
            myFrame.setTitle(appTitle);
            contentPane = myFrame.getContentPane();
            contentPane.add(controllerPanel);
            myFrame.pack();
        }
        else if(frame instanceof JInternalFrame)
        {
            JInternalFrame myFrame = (JInternalFrame) frame;
            myFrame.addInternalFrameListener(new InternalFrameAdapter() {
                public void internalFarmeClosed(InternalFrameEvent e) {
                    handleCloseSimulationLauncher();
                }
            });
            myFrame.setTitle(appTitle);
            contentPane = myFrame.getContentPane();
            contentPane = myFrame.getContentPane();
            myFrame.pack();
        }
        else
        {
            throw new IllegalArgumentException("unknown container type");
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        frame.setLocation((screenSize.width - frameSize.width) / 2,
                          (screenSize.height - frameSize.height) / 2);

        setSimulationRunParameters(null);
        updateSimulationControlButtons();

        createSimulationRunnerThread();
    }
}
    
