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
import org.systemsbiology.chem.scripting.*;
import java.util.*;
import java.io.*;

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
    private static final String DEBUG_ARG = "-debug";
    private static final String PARSER_ARG = "-parser";
    private static final String SIMULATOR_ARG = "-simulator";
    private static final int NUM_REQUIRED_ARGS = 1;
    private static final String DEFAULT_PARSER_ALIAS = org.systemsbiology.chem.scripting.ModelBuilderCommandLanguage.CLASS_ALIAS;
    private static final String START_TIME_ARG = "-startTime";
    private static final double DEFAULT_START_TIME = 0.0;

    private static final String STOP_TIME_ARG = "-stopTime";
    private static final String NUM_SAMPLES_ARG = "-numSamples";
    private static final int DEFAULT_NUM_SAMPLES = 100;

    private static final String ENSEMBLE_SIZE_ARG = "-ensembleSize";
    private static final String REL_TOLERANCE_ARG = "-relativeTolerance";
    private static final String ABS_TOLERANCE_ARG = "-absoluteTolerance";
    private static final String MIN_TIME_STEPS_ARG = "-minTimeSteps";
    private static final String MODEL_FILE_ARG = "-modelFile";
    private static final String OUTPUT_FILE_ARG = "-outputFile";
    private static final String OUTPUT_FILE_FORMAT_ARG = "-outputFormat";
    private static final TimeSeriesSymbolValuesReporter.OutputFormat DEFAULT_OUTPUT_FORMAT = TimeSeriesSymbolValuesReporter.OutputFormat.CSV_EXCEL;

    private boolean mDebug;
    private String mParserAlias;
    private File mModelFile;
    private ClassRegistry mModelBuilderRegistry;
    private ClassRegistry mSimulatorRegistry;

    private ISimulator mSimulator;
    private SimulatorParameters mSimulatorParameters;
    private Double mStartTime;
    private Double mStopTime;
    private Integer mNumSamples;
    private Long mEnsembleSize;
    private Double mRelativeTolerance;
    private Double mAbsoluteTolerance;
    private Long mMinTimeSteps;
    private PrintWriter mOutputFilePrintWriter;
    private TimeSeriesSymbolValuesReporter.OutputFormat mOutputFileFormat;

    private long mMinNumPoints;

    
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
        setDebug(false);
        setParserAlias(null);
        ClassRegistry modelBuilderRegistry = new ClassRegistry(org.systemsbiology.chem.scripting.IModelBuilder.class);
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

    protected String getOutputFormatAliasesList()
    {
        StringBuffer sb = new StringBuffer();
        String []formatAliasesArray = TimeSeriesSymbolValuesReporter.OutputFormat.getSortedFileFormatNames();
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
        pw.println("usage:    java " + getClass().getName() + " [-debug] [-parser <parserAlias>] [-startTime <startTime_float>] -stopTime <stopTime_float> [-numSamples <numSamples_int>] [-ensembleSize <ensembleSize_long>] [-relativeTolerance <tolerance_float>] [-absoluteTolerance <tolerance_float>] [-minTimeSteps <steps_long>] -simulator <simulatorAlias> -modelFile <modelFile> [-outputFile <outputFile>] [-outputFormat <formatAlias>]");
        pw.println("  <parserAlias>:   the alias of the class implementing the interface ");
        pw.println("                   org.systemsbiology.chem.scripting.IModelBuilder (default is determined");
        pw.println("                   by file extension");
        pw.println("  <modelFile>:     the full filename of the model definition file to be loaded");
        pw.println("\nThe list of allowed values for the \"-parser\" argument is:    ");
        pw.print(getParserAliasesList());
        pw.println("(If you do not specify a parser, the file suffix is used to select one)\n");
        pw.println("The list of allowed values for the \"-simulator\" argument is:");
        pw.print(getSimulatorAliasesList());
        pw.println("\nThe list of allowed values for the \"-outputFormat\" argument is: ");
        pw.print(getOutputFormatAliasesList());
        pw.println("(the default is: " + DEFAULT_OUTPUT_FORMAT.toString() + ")");
        pw.println("\nArguments can be in any order.");
        pw.println("If the argument \"-outputFile\" is not specified, the");
        pw.println("simulation results are printed to standard output.");
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
        mOutputFileFormat = DEFAULT_OUTPUT_FORMAT;

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
                mEnsembleSize = getRequiredLongArgumentModifier(ENSEMBLE_SIZE_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(REL_TOLERANCE_ARG))
            {
                mRelativeTolerance = getRequiredDoubleArgumentModifier(REL_TOLERANCE_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(ABS_TOLERANCE_ARG))
            {
                mAbsoluteTolerance = getRequiredDoubleArgumentModifier(ABS_TOLERANCE_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(MIN_TIME_STEPS_ARG))
            {
                mMinTimeSteps = getRequiredLongArgumentModifier(MIN_TIME_STEPS_ARG, pArgs, ++argCtr);
            }
            else if(arg.equals(MODEL_FILE_ARG))
            {
                mModelFile = new File(getRequiredArgumentModifier(MODEL_FILE_ARG, pArgs, ++argCtr));
                if(! mModelFile.exists())
                {
                    handleCommandLineError("model definition file does not exist: " + mModelFile.getAbsolutePath());
                }
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
                TimeSeriesSymbolValuesReporter.OutputFormat outputFormat = TimeSeriesSymbolValuesReporter.OutputFormat.get(outputFormatAlias);
                if(null == outputFormat)
                {
                    handleCommandLineError("invalid output format alias: " + outputFormatAlias);
                }
            }
        }

        if(null == mStopTime)
        {
            handleCommandLineError("required argument not supplied: " + STOP_TIME_ARG);
        }

        if(null == mSimulator)
        {
            handleCommandLineError("required argument not supplied: " + SIMULATOR_ARG);
        }

        if(null == mModelFile)
        {
            handleCommandLineError("required model name was not specified");
        }

        if(null != mEnsembleSize)
        {
            mSimulatorParameters.setEnsembleSize(mEnsembleSize.longValue());
        }

        if(null != mRelativeTolerance)
        {
            mSimulatorParameters.setMaxAllowedRelativeError(mRelativeTolerance.doubleValue());
        }

        if(null != mAbsoluteTolerance)
        {
            mSimulatorParameters.setMaxAllowedAbsoluteError(mAbsoluteTolerance.doubleValue());
        }

        if(null != mMinTimeSteps)
        {
            mSimulatorParameters.setMinNumSteps(mMinTimeSteps.longValue());
        }

        if(null == mOutputFilePrintWriter)
        {
            mOutputFilePrintWriter = new PrintWriter(System.out);
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
            System.out.println("using parser alias: " + getParserAlias());
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
                System.out.println("building script...\n");
            }
            IModelBuilder modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(parserAlias);
            FileReader fileReader = new FileReader(mModelFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            IncludeHandler includeHandler = new IncludeHandler();
            File fileDir = mModelFile.getParentFile();
            includeHandler.setDirectory(fileDir);
            Model model = modelBuilder.buildModel(bufferedReader, includeHandler);
            if(getDebug())
            {
                System.out.println("script build process complete; model is: \n");
                System.out.print(model.toString());
                System.out.println("\n\nrunning simulation...\n");
            }

            String []symbolArray = model.getOrderedNonConstantSymbolNamesArray();
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
            double []resultsTimeValues = new double[numSamples];
            Object []resultsSymbolValues = new Object[numSamples];

            SimulationController simControl = null;
            mSimulator.initialize(model, simControl);

            long currentTimeStart = System.currentTimeMillis();

            mSimulator.simulate(mStartTime.doubleValue(),
                                mStopTime.doubleValue(),
                                mSimulatorParameters,
                                numSamples,
                                globalSymbolsArray,
                                resultsTimeValues,
                                resultsSymbolValues);

            long currentTimeEnd = System.currentTimeMillis();

            double elapsedTimeSeconds = ((double) (currentTimeEnd - currentTimeStart))/1000.0;

            TimeSeriesSymbolValuesReporter.reportTimeSeriesSymbolValues(mOutputFilePrintWriter,
                                                                        globalSymbolsArray,
                                                                        resultsTimeValues,
                                                                        resultsSymbolValues,
                                                                        mOutputFileFormat);

            if(getDebug())
            {
                System.out.println("elapsed time to carry out the simulation: " + elapsedTimeSeconds + " seconds");
                System.out.println("number of iterations: " + mSimulator.getIterationCounter() + "\n");
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
