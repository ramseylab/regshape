package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * Provides a GUI interface for initiating simulations.
 */
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
    private static final int SYMBOL_LIST_BOX_ROW_COUNT = 10;
    private static final int SIMULATORS_LIST_BOX_ROW_COUNT = 10;
    private static final int OUTPUT_FILE_TEXT_FIELD_SIZE_CHARS = 20;

    private ClassRegistry mSimulatorRegistry;
    private Model mModel;
    private ArrayList mListeners;
    private boolean mHandleOutputInternally;

    // simulation runner stuff
    private Thread mSimulationRunnerThread;
    private SimulationRunner mSimulationRunner;
    private SimulationRunParameters mSimulationRunParameters;
    private SimulationController mSimulationController;
    private Queue mResultsQueue;

    // swing controls for the basic launcher
    private Component mLauncherFrame;
    private JTextField mStartTimeField;
    private JTextField mStopTimeField;
    private JTextField mNumPointsField;
    private JList mSimulatorsList;
    private JList mSymbolList;
    private JTextField mEnsembleField;
    private JLabel mEnsembleFieldLabel;
    private JTextField mNumStepsField;
    private JLabel mNumStepsFieldLabel;
    private JTextField mAllowedRelativeErrorField;
    private JLabel mAllowedRelativeErrorFieldLabel;
    private JTextField mAllowedAbsoluteErrorField;
    private JLabel mAllowedAbsoluteErrorFieldLabel;
    private JButton mStartButton;
    private JButton mStopButton;
    private JButton mResumeButton;
    private JButton mCancelButton;
    private String mAppName;
    private JLabel mModelNameLabel;

    // variables used for the "output panel", which controls what should be
    // done with the simulation results:
    private OutputType mOutputType;
    private File mOutputFile;
    private JTextField mOutputFileField;
    private JCheckBox mOutputFileAppendCheckBox;
    private JLabel mOutputFileAppendLabel;
    private Plotter mPlotter;
    private File mCurrentDirectory;

    /**
     * Enumerates the possible results of calling {@link #setModel(org.systemsbiology.chem.Model)}.
     */
    public static class SetModelResult
    {
        private final String mName;
        private SetModelResult(String pName)
        {
            mName = pName;
        }

        public static final SetModelResult FAILED_CLOSED = new SetModelResult("failed_closed");
        public static final SetModelResult SUCCESS = new SetModelResult("success");
        public static final SetModelResult FAILED_RUNNING = new SetModelResult("failed_running");
    }

    public interface Listener
    {
        public void simulationLauncherClosing();
        public void simulationStarting();
        public void simulationEnding();
    }

    /**
     * Creates a simulation launcher window in a JFrame.
     *
     * @param pAppName A string that is embedded in the title bar of the launcher frame.  It should
     * be kept short, to ensure that it displays nicely in the launcher frame title bar.
     *
     * @param pModel The {@link org.systemsbiology.chem.Model model} is a required parameter.
     * The model may be changed by calling {@link #setModel(org.systemsbiology.chem.Model)}.
     *
     * @param pHandleOutputInternally Controls whether the launcher should handle the simulation output
     * itself, or delegate that responsibility to the calling application.  If this parameter is "true",
     * the launcher will handle the output internally, and the caller will not have access to the 
     * simulation results in a structured format.  If instead the parameter is "false", the caller will
     * be able to access the simulation results in a structured format by calling {@link #getNextResults()},
     * and the launcher will not handle the simulation results data.
     * 
     */
    public SimulationLauncher(String pAppName,
                              Model pModel,
                              boolean pHandleOutputInternally) throws ClassNotFoundException, IOException, InstantiationException
    {
        mAppName = pAppName;
        mHandleOutputInternally = pHandleOutputInternally;
        mListeners = new ArrayList();
        if(mHandleOutputInternally)
        {
            mResultsQueue = null;
            mPlotter = new Plotter();
        }
        else
        {
            mResultsQueue = new Queue();
            mPlotter = null;
        }
        mOutputFile = null;
        setSimulationRunParameters(null);

        createSimulationController();
        createSimulatorRegistry();
        createSimulationRunnerThread();

        // create the launcher frame with all its controls
        createLauncherFrame(JFrame.class);

        // set the model and fill the symbol list-box
        setModel(pModel);

        // pack the launcher frame and set its location
        activateLauncherFrame();
    }


    private void createSimulatorRegistry() throws ClassNotFoundException, IOException
    {
        // create simulator aliases
        ClassRegistry classRegistry = new ClassRegistry(org.systemsbiology.chem.ISimulator.class);
        classRegistry.buildRegistry();
        mSimulatorRegistry = classRegistry;
    }

    private void createSimulationController()
    {
        SimulationController controller = new SimulationController();
        mSimulationController = controller;
    }

    private void setLauncherLocation()
    {
        Component frame = getLauncherFrame();
        Dimension frameSize = frame.getSize();
        Point location = FramePlacer.placeInCenterOfScreen(frameSize.width, frameSize.height);
        frame.setLocation(location);
    }

    private void activateLauncherFrame()
    {
        Component frame = getLauncherFrame();

        if(frame instanceof JFrame)
        {
            JFrame myFrame = (JFrame) frame;
            myFrame.pack();
        }
        else if(frame instanceof JInternalFrame)
        {
            JInternalFrame myFrame = (JInternalFrame) frame;
            myFrame.pack();
        }
        else
        {
            throw new IllegalStateException("unknown container type");
        }
        
        setLauncherLocation();
        frame.setVisible(true);
    }

    synchronized Component getLauncherFrame()
    {
        return(mLauncherFrame);
    }

    synchronized void setLauncherFrame(Component pLauncherFrame)
    {
        mLauncherFrame = pLauncherFrame;
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

    private synchronized SimulationRunParameters getSimulationRunParameters()
    {
        return(mSimulationRunParameters);
    }

    private synchronized void setSimulationRunParameters(SimulationRunParameters pSimulationRunParameters)
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
        SimpleDialog messageDialog = new SimpleDialog(getLauncherFrame(), "Simulation cancelled", 
                                                      "Your simulation has been cancelled");
        messageDialog.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        messageDialog.show();
    }

    private boolean mSimulationInProgress;


    boolean getSimulationInProgress()
    {
        return(null != getSimulationRunParameters());
    }

    private SimulationController getSimulationController()
    {
        return(mSimulationController);
    }

    private void updateSimulationControlButtons(boolean pAllowsInterrupt)
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
                mCancelButton.setEnabled(pAllowsInterrupt);
                mResumeButton.setEnabled(pAllowsInterrupt);
            }
            else
            {
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(pAllowsInterrupt);
                mCancelButton.setEnabled(pAllowsInterrupt);
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
                updateSimulationControlButtons(true);
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
                updateSimulationControlButtons(true);
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
            updateSimulationControlButtons(true);
            showCancelledSimulationDialog();
        }
    }

    private void handleOutput(OutputType pOutputType,
                              String pOutputFileName,
                              boolean pOutputFileAppend,
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
            FileWriter fileWriter = new FileWriter(file, pOutputFileAppend);
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
            JDialog dialog = optionPane.createDialog(getLauncherFrame(), "simulation output");
            dialog.show();
        }
        else if(pOutputType.equals(OutputType.PLOT))
        {
            String modelName = mModel.getName();
            String simulatorAlias = mSimulationRunParameters.mSimulatorAlias;
            mPlotter.plot(mAppName, outputBuffer.toString(), simulatorAlias, modelName);
        }
        else
        {
            printWriter.flush();
            JOptionPane optionPane = new JOptionPane("output saved to file:\n" + pOutputFileName);
            JDialog dialog = optionPane.createDialog(getLauncherFrame(), "output saved");
            dialog.show();
        }
    }

    private void runSimulation(SimulationRunParameters pSimulationRunParameters)
    {
        ISimulator simulator = pSimulationRunParameters.mSimulator;

        try
        {
            updateSimulationControlButtons(simulator.allowsInterrupt());

            long startTime = System.currentTimeMillis(); 

            simulator.simulate(pSimulationRunParameters.mStartTime,
                               pSimulationRunParameters.mEndTime,
                               pSimulationRunParameters.mSimulatorParameters,
                               pSimulationRunParameters.mNumTimePoints,
                               pSimulationRunParameters.mRequestedSymbolNames,
                               pSimulationRunParameters.mRetTimeValues,
                               pSimulationRunParameters.mRetSymbolValues);

            long deltaTime = System.currentTimeMillis() - startTime;
            System.out.println("simulation time: " + ((double) deltaTime)/1000.0 + " seconds");

            if(! mSimulationController.getCancelled())
            {
                if(mHandleOutputInternally)
                {
                    handleOutput(pSimulationRunParameters.mOutputType,
                                 pSimulationRunParameters.mOutputFileName,
                                 pSimulationRunParameters.mOutputFileAppend,
                                 pSimulationRunParameters.mRequestedSymbolNames, 
                                 pSimulationRunParameters.mRetTimeValues, 
                                 pSimulationRunParameters.mRetSymbolValues);
                }
                else
                {
                    SimulationResults simulationResults = new SimulationResults();
                    simulationResults.setSimulatorAlias(pSimulationRunParameters.mSimulatorAlias);
                    simulationResults.setStartTime(pSimulationRunParameters.mStartTime);
                    simulationResults.setEndTime(pSimulationRunParameters.mEndTime);
                    simulationResults.setResultsTimeValues(pSimulationRunParameters.mRetTimeValues);
                    simulationResults.setResultsSymbolNames(pSimulationRunParameters.mRequestedSymbolNames);
                    simulationResults.setResultsSymbolValues(pSimulationRunParameters.mRetSymbolValues);
                    mResultsQueue.add(simulationResults);
                }
            }
        }
                
        catch(Exception e)
        {
            simulationEndCleanup(simulator);
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(getLauncherFrame(),
                                                                                             "Failure running simulation",
                                                                                             e);
            dialog.show();
        }

        catch(Throwable e)
        {
            simulationEndCleanup(simulator);
            e.printStackTrace(System.err);
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(getLauncherFrame(),
                                                                                             "Failure running simulation",
                                                                                             new Exception(e.toString()));
            dialog.show();
            
        }

        simulationEndCleanup(simulator);
    }

    private void simulationEndCleanup(ISimulator pSimulator)
    {
        setSimulationRunParameters(null);

        Iterator listenerIter = mListeners.iterator();
        while(listenerIter.hasNext())
        {
            Listener listener = (Listener) listenerIter.next();
            listener.simulationEnding();
        }

        updateSimulationControlButtons(pSimulator.allowsInterrupt());
    }

    class SimulationRunParameters
    {
        ISimulator mSimulator;
        String mSimulatorAlias;
        double mStartTime;
        double mEndTime;
        SimulatorParameters mSimulatorParameters;
        int mNumTimePoints;
        OutputType mOutputType;
        String mOutputFileName;
        boolean mOutputFileAppend;
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
                        throw new RuntimeException(e);
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

                Iterator listenerIter = mListeners.iterator();
                while(listenerIter.hasNext())
                {
                    Listener listener = (Listener) listenerIter.next();
                    listener.simulationStarting();
                }

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

    private void handleSimulatorSelection(int pSimulatorIndex)
    {
        String simulatorAlias = (String) mSimulatorsList.getModel().getElementAt(pSimulatorIndex);
        if(null == simulatorAlias)
        {
            UnexpectedErrorDialog errorDialog = new UnexpectedErrorDialog(getLauncherFrame(), "no simulator selected");
            errorDialog.show();
        }
        else
        {
            ISimulator simulator = null;

            try
            {
                simulator = (ISimulator) getSimulatorRegistry().getInstance(simulatorAlias);
                SimulatorParameters simParams = simulator.getDefaultSimulatorParameters();
                Integer ensembleSize = simParams.getEnsembleSize();
                if(null != ensembleSize)
                {
                    mEnsembleField.setText(ensembleSize.toString());
                    mEnsembleField.setEnabled(true);
                    mEnsembleFieldLabel.setEnabled(true);
                }
                else
                {
                    mEnsembleField.setText("");
                    mEnsembleField.setEnabled(false);
                    mEnsembleFieldLabel.setEnabled(false);
                }
                
                Integer minNumSteps = simParams.getMinNumSteps();
                if(null != minNumSteps)
                {
                    mNumStepsField.setText(minNumSteps.toString());
                    mNumStepsField.setEnabled(true);
                    mNumStepsFieldLabel.setEnabled(true);
                }
                else
                {
                    mNumStepsField.setText("");
                    mNumStepsField.setEnabled(false);
                    mNumStepsFieldLabel.setEnabled(false);
                }



                Double maxAllowedRelativeError = simParams.getMaxAllowedRelativeError();
                if(null != maxAllowedRelativeError)
                {
                    mAllowedRelativeErrorField.setText(maxAllowedRelativeError.toString());
                    mAllowedRelativeErrorField.setEnabled(true);
                    mAllowedRelativeErrorFieldLabel.setEnabled(true);
                }
                else
                {
                    mAllowedRelativeErrorField.setText("");
                    mAllowedRelativeErrorField.setEnabled(false);
                    mAllowedRelativeErrorFieldLabel.setEnabled(false);
                }

                Double maxAllowedAbsoluteError = simParams.getMaxAllowedAbsoluteError();
                if(null != maxAllowedAbsoluteError)
                {
                    mAllowedAbsoluteErrorField.setText(maxAllowedAbsoluteError.toString());
                    mAllowedAbsoluteErrorField.setEnabled(true);
                    mAllowedAbsoluteErrorFieldLabel.setEnabled(true);
                }
                else
                {
                    mAllowedAbsoluteErrorField.setText("");
                    mAllowedAbsoluteErrorField.setEnabled(false);
                    mAllowedAbsoluteErrorFieldLabel.setEnabled(false);
                }

                boolean allowsInterrupt = simulator.allowsInterrupt();
                updateSimulationControlButtons(allowsInterrupt);
            }
            catch(Exception e)
            {
                ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(getLauncherFrame(),
                                                                                                 "Failed to instantiate simulator",
                                                                                                 e);
                dialog.show();
            }
        }
    }

    private JList createSimulatorsList()
    {
        Set simulatorAliases = getSimulatorAliasesCopy();
        assert (simulatorAliases.size() > 0) : "no simulators found";
        java.util.List simulatorAliasesList = new LinkedList(simulatorAliases);
        Collections.sort(simulatorAliasesList);
        Object []simulatorAliasObjects = simulatorAliasesList.toArray();
        final JList simulatorsList = new JList();
        mSimulatorsList = simulatorsList;
        simulatorsList.setVisibleRowCount(SIMULATORS_LIST_BOX_ROW_COUNT);
        simulatorsList.setListData(simulatorAliasObjects);
        simulatorsList.setSelectedIndex(0);
        handleSimulatorSelection(0);
        simulatorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionListener listSelectionListener = new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                if(e.getValueIsAdjusting())
                {
                    handleSimulatorSelection(simulatorsList.getSelectedIndex());
                }
            }
        };
        simulatorsList.addListSelectionListener(listSelectionListener);
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

        JLabel label = new JLabel("controller:");

        JPanel labelPanel = new JPanel();
        Box labelBox = new Box(BoxLayout.X_AXIS);
        labelBox.add(label);
        JPanel padding5 = new JPanel();
        padding5.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        labelBox.add(padding5);
        labelPanel.add(labelBox);

        box.add(labelPanel);

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

    private JList createSymbolList()
    {
        JList symbolListBox = new JList();
        mSymbolList = symbolListBox;
        symbolListBox.setVisibleRowCount(SYMBOL_LIST_BOX_ROW_COUNT);
        symbolListBox.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return(symbolListBox);
    }

    private void populateSymbolListPanel()
    {
        Object []symbolArray = mModel.getOrderedNonConstantSymbolNamesArray();
        mSymbolList.setListData(symbolArray);
    }

    private void handleSelectAllSymbolsButton()
    {
        int numSymbols = mSymbolList.getModel().getSize();
        int []selectedSymbols = new int[numSymbols];
        for(int ctr = 0; ctr < numSymbols; ++ctr)
        {
            selectedSymbols[ctr] = ctr;
        }
        mSymbolList.setSelectedIndices(selectedSymbols);
    }

    private JPanel createSymbolListPanel()
    {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("view symbol:");
        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(label);
        JList list = createSymbolList();
        JScrollPane scrollPane = new JScrollPane(list);
        box.add(scrollPane);
        JButton selectAllButton = new JButton("select all");
        selectAllButton.setMinimumSize(new Dimension(100, 50)); // this makes sure all the text is displayed
        box.add(selectAllButton);
        selectAllButton.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    handleSelectAllSymbolsButton();
                }
            }
            );
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
        JLabel ensembleLabel = new JLabel("number of ensembles:");
        ensembleLabelBox.add(ensembleLabel);
        ensembleLabelPanel.add(ensembleLabelBox);
        ensemblePanel.add(ensembleLabelPanel);
        JTextField ensembleField = new JTextField(NUM_COLUMNS_TIME_FIELD);
        mEnsembleField = ensembleField;
        mEnsembleFieldLabel = ensembleLabel;
        ensemblePanel.add(ensembleField);
        box.add(ensemblePanel);

        JPanel numStepsPanel = new JPanel();
        JPanel numStepsLabelPanel = new JPanel();
        Box numStepsLabelBox = new Box(BoxLayout.Y_AXIS);
        JLabel numStepsLabel = new JLabel("min number of timesteps:");
        numStepsLabelBox.add(numStepsLabel);
        numStepsLabelPanel.add(numStepsLabelBox);
        numStepsPanel.add(numStepsLabelPanel);
        JTextField numStepsField = new JTextField(NUM_COLUMNS_TIME_FIELD);
        mNumStepsField = numStepsField;
        mNumStepsFieldLabel = numStepsLabel;
        numStepsPanel.add(numStepsField);
        box.add(numStepsPanel);

        JPanel allowedRelativeErrorPanel = new JPanel();
        JPanel allowedRelativeErrorLabelPanel = new JPanel();
        Box allowedRelativeErrorLabelBox = new Box(BoxLayout.Y_AXIS);
        JLabel allowedRelativeErrorLabel = new JLabel("max allowed relative error:");
        allowedRelativeErrorLabelBox.add(allowedRelativeErrorLabel);
        allowedRelativeErrorLabelPanel.add(allowedRelativeErrorLabelBox);
        allowedRelativeErrorPanel.add(allowedRelativeErrorLabelPanel);
        JTextField allowedRelativeErrorField = new JTextField(NUM_COLUMNS_TIME_FIELD);
        mAllowedRelativeErrorField = allowedRelativeErrorField;
        mAllowedRelativeErrorFieldLabel = allowedRelativeErrorLabel;
        allowedRelativeErrorPanel.add(allowedRelativeErrorField);
        box.add(allowedRelativeErrorPanel);

        JPanel allowedAbsoluteErrorPanel = new JPanel();
        JPanel allowedAbsoluteErrorLabelPanel = new JPanel();
        Box allowedAbsoluteErrorLabelBox = new Box(BoxLayout.Y_AXIS);
        JLabel allowedAbsoluteErrorLabel = new JLabel("max allowed absolute error:");
        allowedAbsoluteErrorLabelBox.add(allowedAbsoluteErrorLabel);
        allowedAbsoluteErrorLabelPanel.add(allowedAbsoluteErrorLabelBox);
        allowedAbsoluteErrorPanel.add(allowedAbsoluteErrorLabelPanel);
        JTextField allowedAbsoluteErrorField = new JTextField(NUM_COLUMNS_TIME_FIELD);
        mAllowedAbsoluteErrorField = allowedAbsoluteErrorField;
        mAllowedAbsoluteErrorFieldLabel = allowedAbsoluteErrorLabel;
        allowedAbsoluteErrorPanel.add(allowedAbsoluteErrorField);
        box.add(allowedAbsoluteErrorPanel);

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
        SimpleDialog dialog = new SimpleDialog(getLauncherFrame(),
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
        if(numTimePoints < Simulator.MIN_NUM_TIME_POINTS)
        {
            handleBadInput("invalid number of samples", "The number of samples specified must be greater than or equal to " + Integer.toString(Simulator.MIN_NUM_TIME_POINTS));
            return(retVal);
        }

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
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(getLauncherFrame(),
                                                                                             "Failed to instantiate simulator",
                                                                                             e);
            dialog.show();
            return(retVal);
        }

        srp.mSimulator = simulator;
        srp.mSimulatorAlias = simulatorAlias;

        String ensembleStr = mEnsembleField.getText();
        
        SimulatorParameters simulatorParameters = simulator.getDefaultSimulatorParameters();
        srp.mSimulatorParameters = simulatorParameters;

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
        if(null != ensembleSize)
        {
            simulatorParameters.setEnsembleSize(ensembleSize.intValue());
        }

        String numStepsStr = mNumStepsField.getText();

        Integer numSteps = null;
        if(null != numStepsStr && numStepsStr.trim().length() > 0)
        {
            try
            {
                numSteps = new Integer(numStepsStr);
            }
            catch(NumberFormatException e)
            {
                handleBadInput("invalid number of steps", "The number of steps you specified is invalid");
                return(retVal);
            }
        }
        if(null != numSteps)
        {
            simulatorParameters.setMinNumSteps(numSteps.intValue());
        }

        String allowedRelativeErrorStr = mAllowedRelativeErrorField.getText();
        Double allowedRelativeError = null;
        if(null != allowedRelativeErrorStr && allowedRelativeErrorStr.trim().length() > 0)
        {
            try
            {
                allowedRelativeError = new Double(allowedRelativeErrorStr);
            }
            catch(NumberFormatException e)
            {
                handleBadInput("invalid allowed fractional error", "The allowed fractional error you specified is invalid");
                return(retVal);
            }
        }
        if(null != allowedRelativeError)
        {
            simulatorParameters.setMaxAllowedRelativeError(allowedRelativeError.doubleValue());
        }

        String allowedAbsoluteErrorStr = mAllowedAbsoluteErrorField.getText();
        Double allowedAbsoluteError = null;
        if(null != allowedAbsoluteErrorStr && allowedAbsoluteErrorStr.trim().length() > 0)
        {
            try
            {
                allowedAbsoluteError = new Double(allowedAbsoluteErrorStr);
            }
            catch(NumberFormatException e)
            {
                handleBadInput("invalid allowed fractional error", "The allowed fractional error you specified is invalid");
                return(retVal);
            }
        }
        if(null != allowedAbsoluteError)
        {
            simulatorParameters.setMaxAllowedAbsoluteError(allowedAbsoluteError.doubleValue());
        }

        Object []symbolSelected = mSymbolList.getSelectedValues();
        if(symbolSelected.length == 0)
        {
            handleBadInput("no symbol selected", "Please select at least one symbol to observe");
            return(retVal);
        }
        int numSymbol = symbolSelected.length;

        String []symbolSelectedNames = new String[numSymbol];
        for(int ctr = 0; ctr < numSymbol; ctr++)
        {
            String symbolName = (String) symbolSelected[ctr];
            symbolSelectedNames[ctr] = symbolName;
        }
        srp.mRequestedSymbolNames = symbolSelectedNames;

        double []timeValues = new double[numTimePoints];
        srp.mRetTimeValues = timeValues;

        Object []symbolValues = new Object[numTimePoints];
        srp.mRetSymbolValues = symbolValues;
     
        if(mHandleOutputInternally)
        {
            String outputTypeStr = mOutputType.toString();
            OutputType outputType = OutputType.get(outputTypeStr);
            assert (null != outputType) : "null output type";
            
            if(outputType.equals(OutputType.PLOT))
            {
                if(numSymbol > Plotter.MAX_NUM_SYMBOLS_TO_PLOT)
                {
                    handleBadInput("too many symbols to plot", "maximum number of symbols that can be plotted simultaneously is: " + Plotter.MAX_NUM_SYMBOLS_TO_PLOT);
                    return(retVal);
                }
            }
            
            srp.mOutputType = outputType;
            if(mOutputType.equals(OutputType.FILE))
            {
                File outputFile = mOutputFile;
                if(null == outputFile)
                {
                    handleBadInput("output file name was not specified", "Saving the results to a file requires specifying a file name");
                    return(retVal);
                }
                String fileName = outputFile.getAbsolutePath();
                assert (null != fileName && fileName.trim().length() > 0) : "invalid output file name";
                srp.mOutputFileName = fileName;
                boolean append = mOutputFileAppendCheckBox.isSelected();
                srp.mOutputFileAppend = append;
            }
        }
        else
        {
            srp.mOutputFileName = null;
            srp.mOutputFileAppend = false;
            srp.mOutputType = null;
        }

        retVal = srp;
        return(retVal);
    }

    private void enableOutputFieldSection(boolean pEnabled)
    {
        mOutputFileField.setEnabled(pEnabled);
        mOutputFileAppendCheckBox.setEnabled(pEnabled);
        mOutputFileAppendLabel.setEnabled(pEnabled);
        
        if(pEnabled && mOutputFileField.getText().length() == 0)
        {
            mOutputFileField.setText("[output file; click to edit]");
        }
    }

    private void handleButtonEvent(ActionEvent e)
    {
        String outputTypeStr = e.getActionCommand();
        OutputType outputType = OutputType.get(outputTypeStr);
        mOutputType = outputType;
        
        if(null != outputType)
        {
            if(outputType.equals(OutputType.PRINT))
            {
                enableOutputFieldSection(false);
            }
            else if(outputType.equals(OutputType.PLOT))
            {
                enableOutputFieldSection(false);
            }
            else if(outputType.equals(OutputType.FILE))
            {
                enableOutputFieldSection(true);
            }
            else
            {
                assert false: "unknown output type";
            }
        }
        else
        {
            throw new IllegalStateException("unknown output type: " + outputType);
        }
    }

    public void setCurrentDirectory(File pCurrentDirectory)
    {
        mCurrentDirectory = pCurrentDirectory;
    }

    private File getCurrentDirectory()
    {
        return(mCurrentDirectory);
    }

    private void handleOutputFileMouseClick()
    {
        OutputType outputType = mOutputType;
        if(outputType.equals(OutputType.FILE))
        {
            FileChooser outputFileChooser = new FileChooser(mLauncherFrame);
            outputFileChooser.setDialogTitle("Please specify the file for the simulation output");
            File currentDirectory = getCurrentDirectory();
            if(null != mOutputFile)
            {
                outputFileChooser.setSelectedFile(mOutputFile);
            }
            else if(null != currentDirectory)
            {
                outputFileChooser.setCurrentDirectory(currentDirectory);
            }
            outputFileChooser.setApproveButtonText("approve");
            outputFileChooser.show();
            File outputFile = outputFileChooser.getSelectedFile();
            if(null != outputFile)
            {
                String outputFileName = outputFile.getAbsolutePath(); 
                boolean doUpdate = true;
                if(outputFile.exists() && 
                   (null == mOutputFile ||
                    !mOutputFile.equals(outputFile)) && 
                   ! mOutputFileAppendCheckBox.isSelected())
                {
                    doUpdate = FileChooser.handleOutputFileAlreadyExists(mLauncherFrame, outputFileName);
                }
                if(doUpdate)
                {
                    mOutputFile = outputFile;
                    mOutputFileField.setText(outputFileName);
                }
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

        JRadioButton plotButton = new JRadioButton(OutputType.PLOT.toString(), true);
        plotButton.addActionListener(buttonListener);
        plotButton.setSelected(true);
        buttonGroup.add(plotButton);
        printPlotPanel.add(plotButton);
        outputBox.add(printPlotPanel);

        JRadioButton printButton = new JRadioButton(OutputType.PRINT.toString(), true);
        printButton.addActionListener(buttonListener);
        printButton.setSelected(false);
        buttonGroup.add(printButton);
        printPlotPanel.add(printButton);


        JPanel filePanel = new JPanel();
        JRadioButton fileButton = new JRadioButton(OutputType.FILE.toString(), false);
        fileButton.addActionListener(buttonListener);
        buttonGroup.add(fileButton);
        filePanel.add(fileButton);
        JPanel fileNamePanel = new JPanel();
        Box fileBox = new Box(BoxLayout.Y_AXIS);
        JTextField fileNameTextField = new JTextField();
        fileNameTextField.setColumns(OUTPUT_FILE_TEXT_FIELD_SIZE_CHARS);
        mOutputFileField = fileNameTextField;
        fileNameTextField.setEditable(false);
        fileNameTextField.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                handleOutputFileMouseClick();
            }
        });
        fileBox.add(fileNameTextField);
        fileNamePanel.add(fileBox);
        filePanel.add(fileNamePanel);
        JLabel outputFileAppendLabel = new JLabel("append:");
        filePanel.add(outputFileAppendLabel);
        JCheckBox outputFileAppendCheckBox = new JCheckBox();
        filePanel.add(outputFileAppendCheckBox);
        mOutputFileAppendCheckBox = outputFileAppendCheckBox;
        mOutputFileAppendLabel = outputFileAppendLabel;
        outputFileAppendCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean append = mOutputFileAppendCheckBox.isSelected();
                if(! append && null != mOutputFile && mOutputFile.exists())
                {
                    boolean proceed = FileChooser.handleOutputFileAlreadyExists(mLauncherFrame, 
                                                                                mOutputFile.getAbsolutePath());
                    if(! proceed)
                    {
                        mOutputFileAppendCheckBox.setSelected(true);
                    }
                }
            }
        });
        mOutputType = OutputType.PLOT;
        enableOutputFieldSection(false);

        outputBox.add(filePanel);

        outputPanel.add(outputBox);
        return(outputPanel);
    }

    private JPanel createModelSymbolLabelPanel()
    {
        JPanel labelPanel = new JPanel();
        labelPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel modelLabel = new JLabel();
        mModelNameLabel = modelLabel;
        setModelLabel("unknown");
        labelPanel.add(modelLabel);
        return(labelPanel);
    }

    private void setModelLabel(String pModelName)
    {
        mModelNameLabel.setText("model name: [" + pModelName + "]");
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
        setLauncherFrame(null);
    }

    private void createLauncherFrame(Class pFrameClass)
    {
        // create the launcher frame
        Component frame = null;
        try
        {
            frame = (Component) pFrameClass.newInstance();
        }
        catch(Exception e)
        {
            throw new IllegalStateException("unable to instantiate class: " + pFrameClass.getName() + " as an AWT container");
        }
        setLauncherFrame(frame);

        String appTitle = mAppName + ": simulator";

        JPanel controllerPanel = new JPanel();
        Box box = new Box(BoxLayout.Y_AXIS);

        JPanel labelPanel = createModelSymbolLabelPanel();
        box.add(labelPanel);

        JPanel midPanel = new JPanel();
        JPanel buttonPanel = createButtonPanel();
        midPanel.add(buttonPanel);
        JPanel startStopTimePanel = createStartStopTimePanel();
        JPanel simulatorsListPanel = createSimulatorsListPanel();
        midPanel.add(simulatorsListPanel);
        midPanel.add(startStopTimePanel);
        JPanel symbolListPanel = createSymbolListPanel();
        midPanel.add(symbolListPanel);

        box.add(midPanel);

        if(mHandleOutputInternally)
        {
            JPanel outputPanel = createOutputPanel();
            box.add(outputPanel);
        }
        else
        {
            mOutputType = null;
            mOutputFileField = null;
            mOutputFileAppendCheckBox = null;
            mOutputFileAppendLabel = null;
        }

        controllerPanel.add(box);

        Container contentPane = null;

         // Add listener for "window-close" event
        if(frame instanceof JFrame)
        {
            JFrame myFrame = (JFrame) frame;
            myFrame.setTitle(appTitle);
            contentPane = myFrame.getContentPane();
            contentPane.add(controllerPanel);
            myFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    handleCloseSimulationLauncher();
                }
            });
        }
        else if(frame instanceof JInternalFrame)
        {
            JInternalFrame myFrame = (JInternalFrame) frame;
            myFrame.addInternalFrameListener(new InternalFrameAdapter() {
                public void internalFrameClosed(InternalFrameEvent e) {
                    handleCloseSimulationLauncher();
                }
            });
            myFrame.setTitle(appTitle);
            contentPane = myFrame.getContentPane();
            contentPane.add(controllerPanel);
        }
        else
        {
            throw new IllegalStateException("unknown container type");
        }

    }

    /**
     * Registers a {@link SimulationLauncher.Listener} object to receive
     * events for this simulation launcher.
     */
    public void addListener(Listener pListener)
    {
        Component frame = getLauncherFrame();
        final Listener listener = pListener;
        mListeners.add(pListener);
        if(frame instanceof JFrame)
        {
            JFrame launcherFrame = (JFrame) frame;
            launcherFrame.addWindowListener(new WindowAdapter() 
            {
                public void windowClosing(WindowEvent e)
                {
                    listener.simulationLauncherClosing();
                }
            }
                );
        }
        else if(frame instanceof JInternalFrame)
        {
            JInternalFrame launcherFrame = (JInternalFrame) frame;
            launcherFrame.addInternalFrameListener(new InternalFrameAdapter()
            {
                public void internalFrameClosing(InternalFrameEvent e)
                {
                    listener.simulationLauncherClosing();
                }
            }
                );
        }
        else
        {
            throw new IllegalStateException("unknown listener component type");
        }
    }

    /**
     * Sets the underlying {@link org.systemsbiology.chem.Model} data structure
     * to be <code>pModel</code>.  The possible results are the enumerated class
     * {@link SimulationLauncher.SetModelResult}.
     */
    public SetModelResult setModel(Model pModel)
    {
        SetModelResult result = null;

        if(! getSimulationInProgress())
        {
            if(null != getLauncherFrame())
            {
                // set the model

                mModel = pModel;
                mSimulatorRegistry.clearInstances();
                populateSymbolListPanel();
                setModelLabel(pModel.getName());

                result = SetModelResult.SUCCESS;
            }
            else
            {
                result = SetModelResult.FAILED_CLOSED;
            }
        }
        else
        {
            result = SetModelResult.FAILED_RUNNING;
        }

        return(result);
    }

    /**
     * Brings the SimulationLauncher frame "to the front".  Does not
     * necessarily transfer focus to the SimulationLauncher (that depends 
     * on the window manager).
     */
    public void toFront()
    {
        Component launcherFrame = mLauncherFrame;
        if(launcherFrame instanceof JFrame)
        {
            ((JFrame) launcherFrame).toFront();
        }
        else if(launcherFrame instanceof JInternalFrame)
        {
            ((JInternalFrame) launcherFrame).toFront();
        }
        else
        {
            assert false : "unknown internal frame type";
        }
    }

    /**
     * Returns the next {@link org.systemsbiology.chem.SimulationResults} object in the queue.
     * If the queue is empty, null is returned.
     */
    public SimulationResults getNextResults() throws IllegalStateException
    {
        if(mHandleOutputInternally)
        {
            throw new IllegalStateException("cannot access getNextResults() if HandleOutputInternally flag was passed to the SimulationLauncher constructor");
        }

        return((SimulationResults) mResultsQueue.getNext());
    }
}
    
