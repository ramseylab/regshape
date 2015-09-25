package org.systemsbiology.chem.odetojava;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.*;
import java.util.*;
import odeToJava.*;
import odeToJava.modules.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import org.systemsbiology.chem.*;

public abstract class SimulatorOdeToJavaBase extends Simulator implements ODE, ODERecorder
{
    public static final int DEFAULT_MIN_NUM_STEPS = 10000;
    public static final double DEFAULT_MAX_ALLOWED_RELATIVE_ERROR = 0.0001;
    public static final double DEFAULT_MAX_ALLOWED_ABSOLUTE_ERROR = 0.0001;

    private long mIterationCounter;
    private double []mDerivative;
    private double []mScratch;
    private boolean mHasDelayedReactionSolvers;
    private boolean mSimulationCancelled;
    private double mTimeRangeMult;
    private long mTimeOfLastUpdateMilliseconds;
    boolean mSimulationCancelledEventNegReturnFlag;
    private boolean mDoUpdates;
    private double mFractionComplete;

    protected abstract void runExternalSimulation(Span pSimulationTimeSpan,
                                                  double []pInitialDynamicSymbolValues,
                                                  double pInitialStepSize,
                                                  double pMaxAllowedRelativeError,
                                                  double pMaxAllowedAbsoluteError,
                                                  String pTempOutputFileName);

    public void simulate(double pStartTime, 
                         double pEndTime,
                         SimulatorParameters pSimulatorParameters,
                         int pNumResultsTimePoints,
                         String []pResultsSymbolNames,
                         double []pRetResultsTimeValues,
                         Object []pRetResultsSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, SimulationAccuracyException, SimulationFailedException
    {
        conductPreSimulationCheck(pStartTime,
                                  pEndTime,
                                  pSimulatorParameters,
                                  pNumResultsTimePoints,
                                  pResultsSymbolNames,
                                  pRetResultsTimeValues,
                                  pRetResultsSymbolValues);

        SimulationProgressReporter simulationProgressReporter = mSimulationProgressReporter;
        mDoUpdates = (null != mSimulationController || null != simulationProgressReporter);

        mFractionComplete = 0.0;

        mTimeOfLastUpdateMilliseconds = 0;
        if(mDoUpdates)
        {
            mTimeOfLastUpdateMilliseconds = System.currentTimeMillis();
        }

        mIterationCounter = 0;

        if(null != simulationProgressReporter)
        {
            simulationProgressReporter.updateProgressStatistics(false, mFractionComplete, mIterationCounter);
        }

        double []timesArray = createTimesArray(pStartTime, 
                                               pEndTime,
                                               pNumResultsTimePoints);
        int numTimePoints = timesArray.length;
        System.arraycopy(timesArray, 0, pRetResultsTimeValues, 0, numTimePoints);
        
        Symbol []requestedSymbols = createRequestedSymbolArray(mSymbolMap,
                                                               pResultsSymbolNames);

        double initialStepSize = (pEndTime - pStartTime)/((double) DEFAULT_MIN_NUM_STEPS);


        Double maxAllowedRelativeErrorObj = pSimulatorParameters.getMaxAllowedRelativeError();
        if(null == maxAllowedRelativeErrorObj)
        {
            throw new IllegalArgumentException("no maximum allowed relative error was supplied");
        }

        double maxAllowedRelativeError = maxAllowedRelativeErrorObj.doubleValue();
        if(maxAllowedRelativeError <= 0.0)
        {
            throw new IllegalArgumentException("invalid maximum allowed relative error was specified");
        }

        Double maxAllowedAbsoluteErrorObj = pSimulatorParameters.getMaxAllowedAbsoluteError();
        if(null == maxAllowedAbsoluteErrorObj)
        {
            throw new IllegalArgumentException("no maximum allowed absolute error was supplied");
        }

        double maxAllowedAbsoluteError = maxAllowedAbsoluteErrorObj.doubleValue();
        if(maxAllowedAbsoluteError <= 0.0)
        {
            throw new IllegalArgumentException("invalid maximum allowed absolute error was specified");
        }

        if(pNumResultsTimePoints <= 0)
        {
            throw new IllegalArgumentException("invalid number of time points requested");
        }

        Span simulationTimeSpan = new Span(timesArray);

        File tempOutputFile = null;
        try
        {
            tempOutputFile = File.createTempFile("simulationOutput", ".txt");
        }
        catch(IOException e)
        {
            throw new SimulationFailedException("unable to open temporary output file, error message is: " + e.toString());
        }
        String tempOutputFileName = tempOutputFile.getAbsolutePath();

        prepareForSimulation(pStartTime);

        mSimulationCancelled = false;
        mSimulationCancelledEventNegReturnFlag = false;

        mTimeRangeMult = 1.0/(pEndTime - pStartTime);

        runExternalSimulation(simulationTimeSpan,
                              mDynamicSymbolValues,
                              initialStepSize,
                              maxAllowedRelativeError,
                              maxAllowedAbsoluteError,
                              tempOutputFileName);

        if(! mSimulationCancelled)
        {
            mFractionComplete = 1.0;
        }

        if(null != simulationProgressReporter)
        {
            simulationProgressReporter.updateProgressStatistics(true, mFractionComplete, mIterationCounter);
        }

        if(mSimulationCancelled)
        {
            return;
        }

        try
        {
            readSimulationOutput(tempOutputFile,
                                 mSymbolEvaluator,
                                 mDynamicSymbolValues,
                                 pStartTime,
                                 pEndTime,
                                 pNumResultsTimePoints,
                                 requestedSymbols,
                                 pRetResultsTimeValues,
                                 pRetResultsSymbolValues);
        }
        catch(InvalidInputException e)
        {
            throw new SimulationFailedException("unable to parse the output from the simulator; error message is: " + e.toString());
        }
        catch(FileNotFoundException e)
        {
            throw new SimulationFailedException("unable to parse the output from the simulator; error message is: " + e.toString());
        }
        catch(IOException e)
        {
            throw new SimulationFailedException("unable to read the temporary simulation output file; error message is: " + e.toString());
        }

        tempOutputFile.delete();

    }

    protected void readSimulationOutput(File pSimulationResultsFile, 
                                        SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                        double []pDynamicSymbolValues,
                                        double pStartTime,
                                        double pEndTime,
                                        int pNumResultsTimePoints,
                                        Symbol []pRequestedSymbols,
                                        double []pRetResultsTimeValues,
                                        Object []pRetResultsSymbolValues) throws InvalidInputException, DataNotFoundException, FileNotFoundException, IOException, SimulationFailedException
    {
        FileReader fileReader = new FileReader(pSimulationResultsFile);
        BufferedReader inputReader = new BufferedReader(fileReader);
        String line = null;
        int lineNumber = 0;
        SymbolEvaluatorChemSimulation symbolEvaluator = pSymbolEvaluator;
        int numDynamicSymbols = pDynamicSymbolValues.length;
        int numRequestedSymbols = pRequestedSymbols.length;
        int []requestedSymbolIndices = new int[numRequestedSymbols];

        int timeIndex = 0;
        while(null != (line = inputReader.readLine()))
        {
            ++lineNumber;
            if(0 == line.trim().length())
            {
                continue;
            }

            double curTime = pStartTime;
            StringTokenizer st = new StringTokenizer(line, " ");
            int index = -1;
            while(st.hasMoreTokens())
            {
                String token = st.nextToken();
                double value;
                Double valueObj = null;
                try
                {
                    valueObj = new Double(token);
                }
                catch(NumberFormatException e)
                {
                    throw new InvalidInputException("failed to parse simulation output file at line: " + lineNumber);
                }
                value = valueObj.doubleValue();
                
                if(index == -1)
                {
                    symbolEvaluator.setTime(value);
                    curTime = value;
                }
                else
                {
                    pDynamicSymbolValues[index] = value;
                }
                ++index;
            }

            if(index != numDynamicSymbols)
            {
                throw new InvalidInputException("failed to parse expected number of values from simulation output file at line: " + line);
            }
            
            timeIndex = addRequestedSymbolValues(curTime, 
                                                 timeIndex, 
                                                 pRequestedSymbols,
                                                 symbolEvaluator,
                                                 pRetResultsTimeValues,
                                                 pRetResultsSymbolValues);
        }

        if(timeIndex != pNumResultsTimePoints)
        {
            throw new SimulationFailedException("failed to obtain results from simulation; try making the \"max allowed relative error\" smaller");
        }
    }

    public double[] f(double t, double[] x)
    {
        // copy the values in x to the array that is read by the "symbol evaluator"
        System.arraycopy(x, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);

        SymbolEvaluatorChemSimulation symbolEvaluator = mSymbolEvaluator;
        // set the time to t
        symbolEvaluator.setTime(t);

        try
        {
            computeDerivative(mSpeciesRateFactorEvaluator,
                              mSymbolEvaluator,
                              mReactions,
                              mDynamicSymbolAdjustmentVectors,
                              mReactionProbabilities,
                              mScratch,
                              mDerivative);
        }
        catch(DataNotFoundException e)
        {
            throw new RuntimeException("data not found: " + e.getMessage(), e);
        }
        
        return(mDerivative);
    }


    public double[] g(double t, double[] x)
    {   // empty implementation of g because there are no events associated
        double[] event = new double[1];   // with this function

        if(mSimulationCancelled)
        {
            // This nastiness is needed in order to tell the odeToJava simulator
            // to quit, without having to throw an exception; it looks for a change
            // of sign in subsequent invocations of this function ["g(t,x)"], as the
            // signal to halt the integration routine.  The logic below ensures that
            // subsequent calls will have a sign change, and thus trigger termination.
            double retVal = 1.0;
            if(mSimulationCancelledEventNegReturnFlag)
            {
                retVal = -1.0;
                mSimulationCancelledEventNegReturnFlag = false;
            }
            else
            {
                mSimulationCancelledEventNegReturnFlag = true;
            }
            event[0] = retVal;
        }
        return(event);
    }

    public void record(double t, double []x)
    {
        ++mIterationCounter;

        if(mDoUpdates)
        {
            long currentTimeMillis = System.currentTimeMillis();
            if(currentTimeMillis - mTimeOfLastUpdateMilliseconds > mMinNumMillisecondsForUpdate)
            {
                if(null != mSimulationController)
                {
                    mSimulationCancelled = mSimulationController.handlePauseOrCancel();
                }

                if(null != mSimulationProgressReporter)
                {
                    mFractionComplete = t * mTimeRangeMult;
                    mSimulationProgressReporter.updateProgressStatistics(false, mFractionComplete, mIterationCounter);
                }

                mTimeOfLastUpdateMilliseconds = System.currentTimeMillis();
            }
        }

        if(mHasDelayedReactionSolvers)
        {
            // copy the values in x to the array that is read by the "symbol evaluator"
            System.arraycopy(x, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);
            
            SymbolEvaluatorChemSimulation symbolEvaluator = mSymbolEvaluator;
            // set the time to t
            symbolEvaluator.setTime(t);
            
            try
            {
                DelayedReactionSolver []solvers = mDelayedReactionSolvers;
                int numDelayedReactionSolvers = solvers.length;
                for(int ctr = numDelayedReactionSolvers; --ctr >= 0; )
                {
                    DelayedReactionSolver solver = solvers[ctr];
                    solver.update(symbolEvaluator, t);
                }
            }
            catch(DataNotFoundException e)
            {
                throw new RuntimeException("data not found: " + e.getMessage());
            }
        }
    }

    public boolean usesExpressionValueCaching()
    {
        return(false);
    }

    public void initialize(Model pModel) throws DataNotFoundException
    {
        initializeSimulator(pModel);
        initializeDynamicSymbolAdjustmentVectors(mDynamicSymbols);
        int numSpecies = mDynamicSymbolValues.length;
        mDerivative = new double[numSpecies];
        mScratch = new double[numSpecies];
        mHasDelayedReactionSolvers = (mDelayedReactionSolvers.length > 0);
        mSimulationCancelled = false;
        mSimulationCancelledEventNegReturnFlag = false;
    }

    public SimulatorParameters getDefaultSimulatorParameters()
    {
        SimulatorParameters sp = new SimulatorParameters();
        sp.setMaxAllowedRelativeError(DEFAULT_MAX_ALLOWED_RELATIVE_ERROR);
        sp.setMaxAllowedAbsoluteError(DEFAULT_MAX_ALLOWED_ABSOLUTE_ERROR);
        return(sp);
    }

    public boolean isStochasticSimulator()
    {
        return(false);
    }

}