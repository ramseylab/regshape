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
import odeToJava.modules.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import org.systemsbiology.chem.*;

public abstract class SimulatorOdeToJavaBase extends Simulator implements ODE, ODERecorder
{
    public static final double DEFAULT_MAX_ALLOWED_RELATIVE_ERROR = 0.0001;
    public static final double DEFAULT_MAX_ALLOWED_ABSOLUTE_ERROR = 0.0001;
    public static final boolean DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS = false;
    protected static final int DEFAULT_NUM_HISTORY_BINS = 400;
    private static final double DEFAULT_STEP_SIZE_FRACTION = 0.001;

    private long mIterationCounter;
    private double []mDerivative;
    private double []mScratch;
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

    public SimulationResults simulate(double pStartTime, 
                                      double pEndTime,
                                      SimulatorParameters pSimulatorParameters,
                                      int pNumResultsTimePoints,
                                      String []pRequestedSymbolNames) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, AccuracyException, SimulationFailedException
    {
        checkSimulationParameters(pStartTime,
                                  pEndTime,
                                  pSimulatorParameters,
                                  pNumResultsTimePoints);

        // set the number of history bins for the delayed reaction solvers
        int numHistoryBins = pSimulatorParameters.getNumHistoryBins().intValue();
        if(null != mDelayedReactionSolvers)
        {
            resizeDelayedReactionSolvers(numHistoryBins);
        }

        double []retTimeValues = new double[pNumResultsTimePoints];
        Object []retSymbolValues = new Object[pNumResultsTimePoints];

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
        
        Symbol []requestedSymbols = createRequestedSymbolArray(mSymbolMap,
                                                               pRequestedSymbolNames);

        double deltaTime = pEndTime - pStartTime;
        double initialStepSize = pSimulatorParameters.getStepSizeFraction().doubleValue() * deltaTime;

        double maxAllowedRelativeError = pSimulatorParameters.getMaxAllowedRelativeError().doubleValue();
        double maxAllowedAbsoluteError = pSimulatorParameters.getMaxAllowedAbsoluteError().doubleValue();

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

        mTimeRangeMult = 1.0 / deltaTime;

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

        SimulationResults simulationResults = null;
        if(! mSimulationCancelled)
        {
            try
            {
                readSimulationOutput(tempOutputFile,
                                     mSymbolEvaluator,
                                     mDynamicSymbolValues,
                                     pStartTime,
                                     pEndTime,
                                     pNumResultsTimePoints,
                                     requestedSymbols,
                                     timesArray,
                                     retSymbolValues);
            }
            catch(InvalidInputException e)
            {
                tempOutputFile.delete();
                throw new SimulationFailedException("unable to parse the output from the simulator; error message is: " + e.toString());
            }
            catch(FileNotFoundException e)
            {
                tempOutputFile.delete();
                throw new SimulationFailedException("unable to parse the output from the simulator; error message is: " + e.toString());
            }
            catch(IOException e)
            {
                throw new SimulationFailedException("unable to read the temporary simulation output file; error message is: " + e.toString());
            }

            tempOutputFile.delete();

            boolean estimateFinalSpeciesFluctuations = DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS;
            Boolean flagGetFinalSymbolFluctuations = pSimulatorParameters.getComputeFluctuations();
            double []finalSpeciesFluctuations = null;
            if(null != flagGetFinalSymbolFluctuations)
            {
                estimateFinalSpeciesFluctuations = flagGetFinalSymbolFluctuations.booleanValue();
            }
            if(estimateFinalSpeciesFluctuations)
            {
                if(mUseExpressionValueCaching)
                {
                    clearExpressionValueCaches();
                }
                computeReactionProbabilities();

                double []allFinalSpeciesFluctuations = SteadyStateAnalyzer.estimateSpeciesFluctuations(mReactions,
                                                                                                       mDynamicSymbols,
                                                                                                       mDynamicSymbolAdjustmentVectors,
                                                                                                       mReactionProbabilities,
                                                                                                       mSymbolEvaluator);
                if(null != allFinalSpeciesFluctuations)
                {
                    int numRequestedSymbols = pRequestedSymbolNames.length;
                    finalSpeciesFluctuations = new double[numRequestedSymbols];
                    for(int i = 0; i < numRequestedSymbols; ++i)
                    {
                        Symbol requestedSymbol = requestedSymbols[i];
                        int arrayIndex = requestedSymbol.getArrayIndex();
                        finalSpeciesFluctuations[i] = 0.0;
                        if(Symbol.NULL_ARRAY_INDEX != arrayIndex)
                        {
                            if(null != requestedSymbol.getDoubleArray())
                            {
                                finalSpeciesFluctuations[i] = allFinalSpeciesFluctuations[arrayIndex];
                            }
                        }
                    }
                }
            }

            simulationResults = createSimulationResults(pStartTime,
                                                        pEndTime,
                                                        pSimulatorParameters,
                                                        pRequestedSymbolNames,
                                                        timesArray,
                                                        retSymbolValues,
                                                        finalSpeciesFluctuations);
        }
        return(simulationResults);
    }

    protected void readSimulationOutput(File pSimulationResultsFile, 
                                        SymbolEvaluatorChem pSymbolEvaluator,
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
        SymbolEvaluatorChem symbolEvaluator = pSymbolEvaluator;
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
                                                 pRetResultsTimeValues,
                                                 pRetResultsSymbolValues);
        }

        if(timeIndex != pNumResultsTimePoints)
        {
            throw new SimulationFailedException("failed to obtain results from simulation; try making the \"max allowed relative error\" smaller");
        }
    }

    public final double[] f(double t, double[] x)
    {
        // copy the values in x to the array that is read by the "symbol evaluator"
        System.arraycopy(x, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);

        SymbolEvaluatorChem symbolEvaluator = mSymbolEvaluator;
        // set the time to t
        symbolEvaluator.setTime(t);

        try
        {
            if(mUseExpressionValueCaching)
            {
                clearExpressionValueCaches();
            }
            computeDerivative(mScratch,
                              mDerivative);
        }
        catch(DataNotFoundException e)
        {
            throw new RuntimeException("data not found: " + e.getMessage(), e);
        }
        
        return(mDerivative);
    }

    public final double[] g(double t, double[] x)
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

    public final void record(double t, double []x)
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

        if(null != mDelayedReactionSolvers)
        {
            // copy the values in x to the array that is read by the "symbol evaluator"
            System.arraycopy(x, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);
            
            SymbolEvaluatorChem symbolEvaluator = mSymbolEvaluator;
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

    public void initialize(Model pModel) throws DataNotFoundException
    {
        initializeSimulator(pModel);
        initializeDynamicSymbolAdjustmentVectors();
        int numSpecies = mDynamicSymbolValues.length;
        mDerivative = new double[numSpecies];
        mScratch = new double[numSpecies];
        mSimulationCancelled = false;
        mSimulationCancelledEventNegReturnFlag = false;
        setInitialized(true);
    }

    public SimulatorParameters getDefaultSimulatorParameters()
    {
        SimulatorParameters sp = new SimulatorParameters();
        sp.setMaxAllowedRelativeError(new Double(DEFAULT_MAX_ALLOWED_RELATIVE_ERROR));
        sp.setMaxAllowedAbsoluteError(new Double(DEFAULT_MAX_ALLOWED_ABSOLUTE_ERROR));
        sp.setComputeFluctuations(DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS);
        sp.setStepSizeFraction(new Double(DEFAULT_STEP_SIZE_FRACTION));
        sp.setNumHistoryBins(DEFAULT_NUM_HISTORY_BINS);
        return(sp);
    }

    public boolean isStochasticSimulator()
    {
        return(false);
    }

    public void checkSimulationParametersImpl(SimulatorParameters pSimulatorParameters,
                                              int pNumResultsTimePoints)
    {
        if(null == pSimulatorParameters.getMaxAllowedAbsoluteError())
        {
            throw new IllegalArgumentException("missing max allowed absolute error");
        }
        if(null == pSimulatorParameters.getMaxAllowedRelativeError())
        {
            throw new IllegalArgumentException("missing max allowed relative error");
        }
        checkSimulationParametersForDeterministicSimulator(pSimulatorParameters,
                                                           pNumResultsTimePoints);
    }

}
