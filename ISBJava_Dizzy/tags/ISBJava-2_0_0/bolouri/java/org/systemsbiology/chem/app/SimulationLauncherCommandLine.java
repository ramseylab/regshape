package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import org.systemsbiology.chem.*;
import org.systemsbiology.math.*;
import java.util.*;
import java.io.*;
import java.text.*;

/**
 * Command-line interface for running a simulation. 
 *
 * @see org.systemsbiology.chem.Model
 * @see SimulationLauncher
 *
 * @author Stephen Ramsey
 */
public class SimulationLauncherCommandLine extends CommandLineApp
{
    private static final double MILLISECONDS_PER_SECOND = 1000.0;
    private static final String DEBUG_ARG = "-debug";
    private static final String PARSER_ARG = "-parser";
    private static final String SIMULATOR_ARG = "-simulator";
    private static final int NUM_REQUIRED_ARGS = 1;
    private static final String DEFAULT_PARSER_ALIAS = org.systemsbiology.chem.ModelBuilderCommandLanguage.CLASS_ALIAS;
    private static final String START_TIME_ARG = "-startTime";
    private static final double DEFAULT_START_TIME = 0.0;

    private static final String STOP_TIME_ARG = "-stopTime";
    private static final String NUM_SAMPLES_ARG = "-numSamples";
    private static final int DEFAULT_NUM_SAMPLES = 100;

    private static final String ENSEMBLE_SIZE_ARG = "-ensembleSize";
    private static final String REL_TOLERANCE_ARG = "-relativeTolerance";
    private static final String ABS_TOLERANCE_ARG = "-absoluteTolerance";
    private static final String STEP_SIZE_FRACTION_ARG = "-stepSizeFraction";
    private static final String NUM_HISTORY_BINS_ARG = "-numHistoryBins";
    private static final String MODEL_FILE_ARG = "-modelFile";
    private static final String OUTPUT_FILE_ARG = "-outputFile";
    private static final String OUTPUT_FILE_FORMAT_ARG = "-outputFormat";
    private static final TimeSeriesOutputFormat DEFAULT_OUTPUT_FORMAT = TimeSeriesOutputFormat.CSV_EXCEL;
    private static final String TEST_ONLY_ARG = "-testOnly";
    private static final String PRINT_STATUS_ARG = "-printStatus";
    private static final String STATUS_SECONDS_ARG = "-statusSeconds";
    private static final String COMPUTE_FLUCTUATIONS_ARG = "-computeFluctuations";
    private static final String PRINT_PARAMETERS_ARG = "-printParameters";

    private static final Double DEFAULT_ERROR_TOLERANCE_RELATIVE = new Double(1e-6);

    private boolean mDebug;
    private String mParserAlias;
    private File mModelFile;
    private ClassRegistry mModelBuilderRegistry;
    private ClassRegistry mSimulatorRegistry;

    private boolean mPrintStatus;
    private Double mPrintStatusSeconds;

    private ISimulator mSimulator;
    private SimulatorParameters mSimulatorParameters;
    private Double mStartTime;
    private Double mStopTime;
    private Integer mNumSamples;
    private Integer mEnsembleSize;
    private Double mRelativeTolerance;
    private Double mAbsoluteTolerance;
    private Double mStepSizeFraction;
    private Integer mNumHistoryBins;

    private PrintWriter mOutputFilePrintWriter;
    private TimeSeriesOutputFormat mOutputFileFormat;
    private SimulationProgressReporter mSimulationProgressReporter;
    private boolean mComputeFluctuations;
    private SignificantDigitsCalculator mSignificantDigitsCalculator;
    private ScientificNumberFormat mScientificNumberFormat;

    private boolean getDebug()
    {
        return(mDebug);
    }

    private void setDebug(boolean pDebug)
    {
        mDebug = pDebug;
    }

    private void setParserAlias(String pParserAlias)
    {
        mParserAlias = pParserAlias;
    }

    private String getParserAlias()
    {
        return(mParserAlias);
    }

    public SimulationLauncherCommandLine() throws ClassNotFoundException, IOException
    {
        mSignificantDigitsCalculator = new SignificantDigitsCalculator();
        mScientificNumberFormat = new ScientificNumberFormat(mSignificantDigitsCalculator);

        setDebug(false);
        setParserAlias(null);
        ClassRegistry modelBuilderRegistry = new ClassRegistry(org.systemsbiology.chem.IModelBuilder.class);
        modelBuilderRegistry.buildRegistry();
        setModelBuilderRegistry(modelBuilderRegistry);
        ClassRegistry simulatorRegistry = new ClassRegistry(org.systemsbiology.chem.ISimulator.class);
        simulatorRegistry.buildRegistry();
        setSimulatorRegistry(simulatorRegistry);
    }

    private ClassRegistry getModelBuilderRegistry()
    {
        return(mModelBuilderRegistry);
    }

    private void setModelBuilderRegistry(ClassRegistry pModelBuilderRegistry)
    {
        mModelBuilderRegistry = pModelBuilderRegistry;
    }

    private void setSimulatorRegistry(ClassRegistry pSimulatorRegistry)
    {
        mSimulatorRegistry = pSimulatorRegistry;
    }

    private ClassRegistry getSimulatorRegistry()
    {
        return(mSimulatorRegistry);
    }

    protected String getSimulatorAliasesList()
    {
        StringBuffer sb = new StringBuffer();
        ClassRegistry simulatorRegistry = getSimulatorRegistry();
        Set aliasesSet = simulatorRegistry.getRegistryAliasesCopy();
        LinkedList aliasesList = new LinkedList(aliasesSet);
        Collections.sort(aliasesList);
        Iterator aliasesIter = aliasesList.iterator();
        while(aliasesIter.hasNext())
        {
            String alias = (String) aliasesIter.next();
            sb.append("  " + alias + "\n");
        }
        return(sb.toString());
    }

    protected String getTimeSeriesOutputFormatAliasesList()
    {
        StringBuffer sb = new StringBuffer();
        String []formatAliasesArray = TimeSeriesOutputFormat.getSortedFileFormatNames();
        int numAliases = formatAliasesArray.length;
        for(int i = 0; i < numAliases; ++i)
        {
            String formatAlias = formatAliasesArray[i];
            sb.append("  " + formatAlias + "\n");
        }
        return(sb.toString());
    }

    protected String getParserAliasesList()
    {
        StringBuffer sb = new StringBuffer();
        ClassRegistry modelBuilderRegistry = getModelBuilderRegistry();
        Set aliasesSet = modelBuilderRegistry.getRegistryAliasesCopy();
        LinkedList aliasesList = new LinkedList(aliasesSet);
        Collections.sort(aliasesList);
        Iterator aliasesIter = aliasesList.iterator();
        while(aliasesIter.hasNext())
        {
            String alias = (String) aliasesIter.next();
            sb.append("  " + alias + "\n");
        }
        return(sb.toString());
    }

    protected void printUsage(OutputStream pOutputStream)
    {
        PrintWriter pw = new PrintWriter(pOutputStream);
        pw.println("usage:    java " + getClass().getName() + " [-debug] [-parser <parserAlias>] [-startTime <startTime_float>] -stopTime <stopTime_float> [-numSamples <numSamples_int>] [-ensembleSize <ensembleSize_int>] [-relativeTolerance <tolerance_float>] [-absoluteTolerance <tolerance_float>] [-stepSizeFraction <numHistoryBins_double>] [-numHistoryBins <numHistoryBins_int>] -simulator <simulatorAlias> -modelFile <modelFile> [-outputFile <outputFile>] [-outputFormat <formatAlias>] [-printStatus [-statusSeconds <intervalSeconds>]] [-computeFluctuations] [-testOnly] [-printParameters]");
        pw.println("  <parserAlias>:   the alias of the class implementing the interface ");
        pw.println("                   org.systemsbiology.chem.IModelBuilder (default is determined");
        pw.println("                   by file extension");
        pw.println("  <modelFile>:     the full filename of the model definition file to be loaded");
        pw.println("[-testOnly]:       do not run an actual simulation; just parse the command-line and exit");
        pw.println("[-debug]:          print out debugging information, including all of the simulator parameter values]");
        pw.println("\nThe list of allowed values for the \"-parser\" argument is:    ");
        pw.print(getParserAliasesList());
        pw.println("(If you do not specify a parser, the file suffix is used to select one)\n");
        pw.println("The list of allowed values for the \"-simulator\" argument is:");
        pw.print(getSimulatorAliasesList());
        pw.println("\nThe list of allowed values for the \"-outputFormat\" argument is: ");
        pw.print(getTimeSeriesOutputFormatAliasesList());
        pw.println("(the default is: " + DEFAULT_OUTPUT_FORMAT.toString() + ")");
        pw.println("\nArguments can be in any order.");
        pw.println("If the argument \"-outputFile\" is not specified, the");
        pw.println("simulation results are printed to standard output.");
        pw.println("\nFor more information, please consult the user manual.");
        pw.println("To see this help screen, run with the \"-help\" option.");
        pw.println("To print the default parameters for simulator \"mysim\" and then exit, run:");
        pw.println("this program with the options \"-simulator mysim -printParameters\"");
        pw.flush();
    }

    

    private String selectParserAliasFromFileName(String pFileName) throws DataNotFoundException
    {
        ClassRegistry modelBuilderRegistry = getModelBuilderRegistry();
        Set parserAliases = modelBuilderRegistry.getRegistryAliasesCopy();

        Iterator parserAliasesIter = parserAliases.iterator();
        String retParserAlias = DEFAULT_PARSER_ALIAS;
        while(parserAliasesIter.hasNext())
        {
            String parserAlias = (String) parserAliasesIter.next();
            IModelBuilder modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(parserAlias);
            String fileRegex = modelBuilder.getFileRegex();
            if(pFileName.matches(fileRegex))
            {
                retParserAlias = parserAlias;
                break;
            }
        }

        return(retParserAlias);
    }

    protected void handleCommandLine(String []pArgs) 
    {
        checkAndHandleHelpRequest(pArgs);

        int numArgs = pArgs.length;

        if(numArgs < NUM_REQUIRED_ARGS)
        {
            handleCommandLineError("the number of command-line arguments is insufficient");
        }

        mStartTime = new Double(DEFAULT_START_TIME);
        mStopTime = null;
        mNumSamples = new Integer(DEFAULT_NUM_SAMPLES);
        mModelFile = null;
        mOutputFilePrintWriter = null;
        mPrintStatus = false;
        mOutputFileFormat = DEFAULT_OUTPUT_FORMAT;
        mPrintStatusSeconds = null;
        mComputeFluctuations = false;
        boolean testOnly = false;
        boolean printParameters = false;

        for(int argCtr = 0; argCtr < numArgs; ++argCtr)
        {
            String arg = pArgs[argCtr];

            if(arg.equals(DEBUG_ARG))
            {
                setDebug(true);
            }
            else if(arg.equals(PARSER_ARG))
            {
                if(argCtr == numArgs - 1)
                {
                    handleCommandLineError("argument \"" + PARSER_ARG + "\" has a required element after it");
                }

                ++argCtr;
                String parserAlias = pArgs[argCtr];
                setParserAlias(parserAlias);
            }
            else if(arg.equals(SIMULATOR_ARG))
            {
                String simulatorAlias = getRequiredArgumentModifier(SIMULATOR_ARG, pArgs, ++argCtr);
                try
                {
                    mSimulator = (ISimulator) mSimulatorRegistry.getInstance(simulatorAlias);
                }
                catch(DataNotFoundException e)
                {
                    handleCommandLineError("unknown simulator alias: " + simulatorAlias);
                }
                mSimulatorParameters = mSimulator.getDefaultSimulatorParameters();
            }
            else if(arg.equals(START_TIME_ARG))
            {
                mStartTime = getRequiredDoubleArgumentModifier(START_TIME_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(STOP_TIME_ARG))
            {
                mStopTime = getRequiredDoubleArgumentModifier(STOP_TIME_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(PRINT_PARAMETERS_ARG))
            {
                printParameters = true;
            }
            else if(arg.equals(NUM_SAMPLES_ARG))
            {
                mNumSamples = getRequiredIntegerArgumentModifier(NUM_SAMPLES_ARG, pArgs, ++argCtr);
                if(mNumSamples.intValue() <= 0)
                {
                    handleCommandLineError("number of samples must be a positive integer: " + mNumSamples);
                }
            }
            else if(arg.equals(ENSEMBLE_SIZE_ARG))
            {
                mEnsembleSize = getRequiredIntegerArgumentModifier(ENSEMBLE_SIZE_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(REL_TOLERANCE_ARG))
            {
                mRelativeTolerance = getRequiredDoubleArgumentModifier(REL_TOLERANCE_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(ABS_TOLERANCE_ARG))
            {
                mAbsoluteTolerance = getRequiredDoubleArgumentModifier(ABS_TOLERANCE_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(STEP_SIZE_FRACTION_ARG))
            {
                mStepSizeFraction = getRequiredDoubleArgumentModifier(STEP_SIZE_FRACTION_ARG, pArgs, ++argCtr);
            }

            else if(arg.equals(NUM_HISTORY_BINS_ARG))
            {
                mNumHistoryBins = getRequiredIntegerArgumentModifier(NUM_HISTORY_BINS_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(MODEL_FILE_ARG))
            {
                mModelFile = new File(getRequiredArgumentModifier(MODEL_FILE_ARG, pArgs, ++argCtr));
                if(! mModelFile.exists())
                {
                    handleCommandLineError("model definition file does not exist: " + mModelFile.getAbsolutePath());
                }
            }
            else if(arg.equals(PRINT_STATUS_ARG))
            {
                mPrintStatus = true;
            }
            else if(arg.equals(COMPUTE_FLUCTUATIONS_ARG))
            {
                mComputeFluctuations = true;
            }
            else if(arg.equals(STATUS_SECONDS_ARG))
            {
                mPrintStatusSeconds = getRequiredDoubleArgumentModifier(STATUS_SECONDS_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(OUTPUT_FILE_ARG))
            {
                String outputFileName = getRequiredArgumentModifier(OUTPUT_FILE_ARG, pArgs, ++argCtr);
                try
                {
                    File outputFile = new File(outputFileName);
                    FileWriter fileWriter = new FileWriter(outputFile);
                    mOutputFilePrintWriter = new PrintWriter(fileWriter);
                }
                catch(IOException e)
                {
                    handleCommandLineError("invalid output file name: " + outputFileName);
                }
            }
            else if(arg.equals(OUTPUT_FILE_FORMAT_ARG))
            {
                String outputFormatAlias = getRequiredArgumentModifier(OUTPUT_FILE_FORMAT_ARG, pArgs, ++argCtr);
                TimeSeriesOutputFormat outputFormat = TimeSeriesOutputFormat.get(outputFormatAlias);
                if(null == outputFormat)
                {
                    handleCommandLineError("invalid output format alias: " + outputFormatAlias);
                }
                mOutputFileFormat = outputFormat;
            }
            else if(arg.equals(TEST_ONLY_ARG))
            {
                testOnly = true;
            }
        }

        if(null == mSimulator)
        {
            handleCommandLineError("required argument not supplied: " + SIMULATOR_ARG);
        }

        if(null != mEnsembleSize)
        {
            mSimulatorParameters.setEnsembleSize(mEnsembleSize.intValue());
        }

        if(null != mRelativeTolerance)
        {
            mSimulatorParameters.setMaxAllowedRelativeError(mRelativeTolerance.doubleValue());
        }

        if(null != mAbsoluteTolerance)
        {
            mSimulatorParameters.setMaxAllowedAbsoluteError(mAbsoluteTolerance.doubleValue());
        }

        mSimulatorParameters.setComputeFluctuations(mComputeFluctuations);
        if(mComputeFluctuations && (mSimulator instanceof org.systemsbiology.chem.SimulatorStochasticBase)
            && null != mEnsembleSize 
            && mEnsembleSize.intValue() <= 1)
        {
            handleCommandLineError("for a stochastic simulator, an ensemble size of greater than one is required, in order to compute the final symbol fluctuations");
        }

        if(null != mStepSizeFraction)
        {
            mSimulatorParameters.setStepSizeFraction(mStepSizeFraction.doubleValue());
        }

        if(null != mNumHistoryBins)
        {
            mSimulatorParameters.setNumHistoryBins(mNumHistoryBins.intValue());
        }

        if(null == mOutputFilePrintWriter)
        {
            mOutputFilePrintWriter = new PrintWriter(System.out);
        }

        if(null != mPrintStatusSeconds && ! mPrintStatus)
        {
            handleCommandLineError("the option \"statusSeconds\" requires the option \"printStatus\"");
        }

        DecimalFormatSymbols decimalFormatSymbols = mScientificNumberFormat.getDecimalFormatSymbols();
        mOutputFileFormat.updateDecimalFormatSymbols(decimalFormatSymbols);
        mScientificNumberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        
        if(printParameters)
        {
            System.err.println(mSimulatorParameters.toString());
            System.exit(0);
        }

        if(null == mStopTime)
        {
            handleCommandLineError("required argument not supplied: " + STOP_TIME_ARG);
        }

        if(null == mModelFile)
        {
            handleCommandLineError("required model name was not specified");
        }

        if(null == getParserAlias())
        {
            try
            {
                setParserAlias(selectParserAliasFromFileName(mModelFile.getAbsolutePath()));
            }
            catch(DataNotFoundException e)
            {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

        if(getDebug())
        {
            System.err.println("using parser alias: " + getParserAlias());
            System.err.println("using simulator parameters:");
            System.err.println(mSimulatorParameters.toString());
        }

        if(mComputeFluctuations && ! mSimulator.canComputeFluctuations())
        {
            handleCommandLineError("simulator cannot compute fluctuations: " + mSimulator.getAlias());
        }
        
        if(testOnly)
        {
            try
            {
                mSimulator.checkSimulationParameters(mStartTime.doubleValue(),
                                                     mStopTime.doubleValue(),
                                                     mSimulatorParameters,
                                                     mNumSamples.intValue());
                System.exit(0);
            }
            catch(Exception e)
            {
                System.err.println("simulation parameters were invalid; here is the specific error message: ");
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    private void run(String []pArgs)
    {
        try
        {
            ClassRegistry modelBuilderRegistry = getModelBuilderRegistry();

            handleCommandLine(pArgs);

            String parserAlias = getParserAlias();

            if(getDebug())
            {
                System.err.println("building script...\n");
            }
            IModelBuilder modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(parserAlias);
            InputStream inputStream = new FileInputStream(mModelFile);
            IncludeHandler includeHandler = new IncludeHandler();
            File fileDir = mModelFile.getParentFile();
            includeHandler.setDirectory(fileDir);
            Model model = modelBuilder.buildModel(inputStream, includeHandler);
            if(getDebug())
            {
                System.err.println("script build process complete; model is: \n");
                System.err.print(model.toString());
                System.err.println("\n\nrunning simulation...\n");
            }

            String []symbolArray = model.getOrderedResultsSymbolNamesArray();
            ArrayList globalSymbolsList = new ArrayList();
            int numTotalSymbols = symbolArray.length;
            for(int i = 0; i < numTotalSymbols; ++i)
            {
                String symbolName = symbolArray[i];
                if(-1 == symbolName.indexOf(Model.NAMESPACE_IDENTIFIER))
                {
                    globalSymbolsList.add(symbolName);
                }
            }
            String []globalSymbolsArray = (String []) globalSymbolsList.toArray(new String[0]);
            int numGlobalSymbols = globalSymbolsArray.length;
            int numSamples = mNumSamples.intValue();

            ISimulator simulator = mSimulator;

            if(mPrintStatus)
            {
                SimulationProgressReporter reporter = new SimulationProgressReporter();
                if(null != mPrintStatusSeconds)
                {
                    simulator.setStatusUpdateIntervalSeconds(mPrintStatusSeconds.doubleValue());
                }
                simulator.setProgressReporter(reporter);
                mSimulationProgressReporter = reporter;
                SimulationProgressReportHandler progressReportHandler = new SimulationProgressReportHandler(new PrintWriter(System.err));
                Thread progressReporterThread = new Thread(progressReportHandler);
                progressReporterThread.setDaemon(true);
                progressReporterThread.start();
            
            }
            else
            {
                mSimulationProgressReporter = null;
            }
                

            simulator.initialize(model);

            long currentTimeStart = System.currentTimeMillis();

            SimulationResults simulationResults = simulator.simulate(mStartTime.doubleValue(),
                                                                     mStopTime.doubleValue(),
                                                                     mSimulatorParameters,
                                                                     numSamples,
                                                                     globalSymbolsArray);

            double []resultsTimeValues = simulationResults.getResultsTimeValues();
            Object []resultsSymbolValues = simulationResults.getResultsSymbolValues();

            if(mPrintStatus)
            {
                mSimulationProgressReporter.setSimulationFinished(true);
            }

            long currentTimeEnd = System.currentTimeMillis();

            double elapsedTimeSeconds = ((double) (currentTimeEnd - currentTimeStart))/MILLISECONDS_PER_SECOND;

            if(mPrintStatus || getDebug())
            {
                System.err.println("elapsed time to carry out the simulation: " + elapsedTimeSeconds + " seconds");
            }

            Double relTol = mSimulatorParameters.getMaxAllowedRelativeError();
            if(null == relTol)
            {
                relTol = DEFAULT_ERROR_TOLERANCE_RELATIVE;
            }
            mSignificantDigitsCalculator.setRelTol(relTol);
            mSignificantDigitsCalculator.setAbsTol(mSimulatorParameters.getMaxAllowedAbsoluteError());

            TimeSeriesSymbolValuesReporter.reportTimeSeriesSymbolValues(mOutputFilePrintWriter,
                                                                        globalSymbolsArray,
                                                                        resultsTimeValues,
                                                                        resultsSymbolValues,
                                                                        mScientificNumberFormat,
                                                                        mOutputFileFormat);

            if(mComputeFluctuations)
            {
                double []finalSymbolFluctuations = simulationResults.getResultsFinalSymbolFluctuations();
                if(null != finalSymbolFluctuations)
                {
                    int numRequestedSymbols = globalSymbolsArray.length;
                    for(int i = 0; i < numRequestedSymbols; ++i)
                    {
                        String speciesName = globalSymbolsArray[i];
                        double speciesFluctuations = finalSymbolFluctuations[i];
                        mOutputFilePrintWriter.println(speciesName + ", " + mScientificNumberFormat.format(speciesFluctuations));
                    }
                }
                else
                {
                    mOutputFilePrintWriter.println("unable to compute steady-state fluctuations; perhaps the system did not reach steady-state?");
                }
            }

            mOutputFilePrintWriter.flush();
        }

        catch(Exception e)
        {
            System.err.println("an exception occurred; the error message is:");
            System.err.println(e.toString());
            Throwable cause = e.getCause();
            if(null != cause)
            {
                System.err.println("cause of exception: " + e.getCause().toString());
            }
            if(getDebug())
            {
                System.err.println("\nthe detailed error message of this exception is:\n");
                System.err.println(e.getMessage() + "\n");
                System.err.println("\nthe stack backtrace of this exception is:\n");
                e.printStackTrace(System.err);
                if(null != cause)
                {
                    System.err.println("\nthe stack backtrace of the cause exception is:\n");
                    cause.printStackTrace(System.err);
                }
            }
            else
            {
                System.err.println("\nto see a stack backtrace of the exception, simply re-run this program with the \"" + DEBUG_ARG + "\" argument before the required arguments");
            }
            System.exit(1);
        }
    }

    class SimulationProgressReportHandler implements Runnable
    {
        private static final long NULL_TIME_UPDATE_MILLIS = 0;

        private PrintWriter mOutputWriter;
        private final DateFormat mDateFormat = DateFormat.getDateTimeInstance();
        private long mLastUpdateTimeMillis;
        private double mLastUpdateFractionComplete;
        private long mLastUpdateIterations;

        public SimulationProgressReportHandler(PrintWriter pOutputWriter)
        {
            mOutputWriter = pOutputWriter;
            mLastUpdateTimeMillis = NULL_TIME_UPDATE_MILLIS;
            mLastUpdateIterations = 0;
        }

        public void run()
        {
            SimulationProgressReporter reporter = mSimulationProgressReporter;
            while(! reporter.getSimulationFinished())
            {
                synchronized(reporter)
                {
                    reporter.waitForUpdate();
                    if(! reporter.getSimulationFinished())
                    {
                        long updateTimeMillis = reporter.getTimeOfLastUpdateMillis();
                        double fractionComplete = reporter.getFractionComplete();

                        PrintWriter outputWriter = mOutputWriter;
                        Date dateTimeOfUpdate = new Date(updateTimeMillis);
                        outputWriter.println("at: " + mDateFormat.format(dateTimeOfUpdate));
                        outputWriter.println("fraction complete: " + fractionComplete);
                        long iterationsCompleted = reporter.getIterationCounter();
                        outputWriter.println("iterations completed: " + iterationsCompleted);
                        long changeTimeMillis = updateTimeMillis - mLastUpdateTimeMillis;
                        long newIterations = iterationsCompleted - mLastUpdateIterations;
                        mLastUpdateIterations = iterationsCompleted;
                        double changeTimeSeconds = ((double) changeTimeMillis) / MILLISECONDS_PER_SECOND;
                        double iterationsPerSecond = ((double) newIterations)/changeTimeSeconds;
                        outputWriter.println("iterations/second: " + iterationsPerSecond);

                        String estimatedTimeToCompletionStr = null;
                        if(NULL_TIME_UPDATE_MILLIS != mLastUpdateTimeMillis)
                        {
                            double changeFraction = fractionComplete - mLastUpdateFractionComplete;
                            if(changeFraction > 0.0)
                            {
                                double timeToCompletion = (1.0 - fractionComplete) * changeTimeSeconds / changeFraction;
                                estimatedTimeToCompletionStr = Double.toString(timeToCompletion);
                            }
                            else
                            {
                                estimatedTimeToCompletionStr = "STALLED";
                            }
                        }
                        else
                        {
                            estimatedTimeToCompletionStr = "UNKNOWN";
                        }
                        outputWriter.println("estimated time to completion: " + estimatedTimeToCompletionStr + " seconds");
                        outputWriter.println("\n");
                        outputWriter.flush();
                        mLastUpdateTimeMillis = updateTimeMillis;
                        mLastUpdateFractionComplete = fractionComplete;
                    }
                }
            }
        }
    }

    public static void main(String []pArgs)
    {
        try
        {
            SimulationLauncherCommandLine app = new SimulationLauncherCommandLine();
            app.run(pArgs);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
