package org.systemsbiology.chem;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

/**
 * Simulates the dynamics of a set of coupled chemical reactions
 * described by {@link Reaction} objects using the Runge-Kutta
 * algorithm (fifth order with adaptive step-size control).
 *
 * @author Stephen Ramsey
 */
public abstract class SimulatorDeterministicBase extends Simulator
{
    private static final double DEFAULT_STEP_SIZE_FRACTION = 0.001;
    public static final double DEFAULT_MAX_ALLOWED_RELATIVE_ERROR = 0.0001;
    public static final double DEFAULT_MAX_ALLOWED_ABSOLUTE_ERROR = 0.01;
    public static final boolean DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS = false;
    protected static final int DEFAULT_NUM_HISTORY_BINS = 400;

    class RKScratchPad
    {
        double []k1;
        double []k2;
        double []k3;
        double []k4;
        double []ysav;
        double []yscratch;
        double []y1;
        double []y2;
        double []yscale;
        double []dydt;
        double stepSize;
        double maxStepSize;
        double maxRelativeError;
        double maxAbsoluteError;
        int numIterations;
        MutableDouble relativeError;
        MutableDouble absoluteError;

        public RKScratchPad(int pNumVariables)
        {
            k1 = new double[pNumVariables];
            k2 = new double[pNumVariables];
            k3 = new double[pNumVariables];
            k4 = new double[pNumVariables];
            ysav = new double[pNumVariables];
            yscratch = new double[pNumVariables];
            y1 = new double[pNumVariables];
            y2 = new double[pNumVariables];
            yscale = new double[pNumVariables];
            dydt = new double[pNumVariables];
            relativeError = new MutableDouble(0.0);
            absoluteError = new MutableDouble(0.0);
            clear();
        }

        public void clear()
        {
            DoubleVector.zeroElements(k1);
            DoubleVector.zeroElements(k2);
            DoubleVector.zeroElements(k3);
            DoubleVector.zeroElements(k4);
            DoubleVector.zeroElements(ysav);
            DoubleVector.zeroElements(yscratch);
            DoubleVector.zeroElements(y1);
            DoubleVector.zeroElements(y2);
            DoubleVector.zeroElements(yscale);
            DoubleVector.zeroElements(dydt);
            stepSize = 0.0;
            maxStepSize = 0.0;
            numIterations = 0;
            maxRelativeError = 0.0;
            maxAbsoluteError = 0.0;
            relativeError.setValue(0.0);
            absoluteError.setValue(0.0);
        }
    }

    protected RKScratchPad mRKScratchPad;



    protected abstract double iterate(double []pNewDynamicSymbolValues) throws DataNotFoundException, AccuracyException;


    protected final void rk4step(double pTimeStepSize,
                                 double []pNewDynamicSymbolValues) throws DataNotFoundException
    {
        double time = mSymbolEvaluator.getTime();

        double []k1 = mRKScratchPad.k1;  // note:  our "k1" is equivalent to 0.5 times the "k1" in numerical recipes
        int numVars = k1.length;

        double []y = mDynamicSymbolValues;
        double []ysav = mRKScratchPad.ysav;
        double []yscratch = mRKScratchPad.yscratch;

        double halfStep = pTimeStepSize / 2.0;
        double timePlusHalfStep = time + halfStep;

        // save a copy of the initial y values
        System.arraycopy(y, 0, ysav, 0, numVars);

        computeDerivative(yscratch, k1);
;
        DoubleVector.scalarMultiply(k1, halfStep, k1);
        // now, k1 contains  h * f'(t, y)/2.0

        // set the y values to "y + k1"
        DoubleVector.add(ysav, k1, y);

        // set the time to "t + h/2"
        mSymbolEvaluator.setTime(timePlusHalfStep);

        double []k2 = mRKScratchPad.k2;
        computeDerivative(yscratch, k2);

        DoubleVector.scalarMultiply(k2, halfStep, k2);

        DoubleVector.add(ysav, k2, y);
        // y now contains "y + k2"

        double []k3 = mRKScratchPad.k3;
        computeDerivative(yscratch, k3);

        DoubleVector.scalarMultiply(k3, pTimeStepSize, k3);
        // k3 now contains h * f'(t + h/2, y + k2)

        DoubleVector.add(ysav, k3, y);
        // y now contains  "y + k3"

        double []k4 = mRKScratchPad.k4;


        // set time to "t + h"
        double pNextTime = time + pTimeStepSize;
        mSymbolEvaluator.setTime(pNextTime);

        computeDerivative(yscratch, k4);

        DoubleVector.scalarMultiply(k4, pTimeStepSize, k4);
        // k4 now contains h * f'(t + h, y + k3)

        double newDynamicSymbolValue;
        for(int ctr = numVars; --ctr >= 0; )
        {
            newDynamicSymbolValue = ysav[ctr] + 
                                           (k1[ctr]/3.0) +
                                           (2.0 * k2[ctr] / 3.0) +
                                           (k3[ctr] / 3.0) +
                                           (k4[ctr] / 6.0);
            pNewDynamicSymbolValues[ctr] = newDynamicSymbolValue;
        }

        DoubleVector.zeroNegativeElements(pNewDynamicSymbolValues);

        // restore to previous values; allow the driver to update
        System.arraycopy(ysav, 0, y, 0, numVars);
        mSymbolEvaluator.setTime(time);
    }

    private void resetScratchpad()
    {
        int numDynamicSymbols = mDynamicSymbolValues.length;
        mRKScratchPad = new RKScratchPad(numDynamicSymbols);
    }

    public void initialize(Model pModel) throws DataNotFoundException
    {
        initializeSimulator(pModel);
        initializeDynamicSymbolAdjustmentVectors();
        resetScratchpad();
        setInitialized(true);
    }

    protected final void computeScale(double pTimeStepSize,
                                      double []yscale) throws DataNotFoundException, AccuracyException
    {
        double []yscratch = mRKScratchPad.yscratch;
        double []dydt = mRKScratchPad.dydt;
        int numDynamicSymbols = mDynamicSymbolValues.length;
        double dydtn = 0.0;
        double yn = 0.0;

        computeDerivative(yscratch, dydt);

        double scale;
        boolean gotNonzero = false;
        for(int symCtr = numDynamicSymbols; --symCtr >= 0; )
        {
            dydtn = dydt[symCtr];
            yn = mDynamicSymbolValues[symCtr];
            scale = Math.abs(yn) + Math.abs(dydtn * pTimeStepSize);
            yscale[symCtr] = scale;
            if(scale > 0.0)
            {
                gotNonzero = true;
            }
        }        

        if(! gotNonzero)
        {
            // all of the scale factors are zero!
            throw new AccuracyException("unable to determine any scale at time: " + mSymbolEvaluator.getTime());
        }
    }


    protected final void rkqc(double pTimeStepSize,
                              double []pDynamicSymbolValueScales,
                              double []pNewDynamicSymbolValues,
                              MutableDouble pRetAggregateRelativeError,
                              MutableDouble pRetAggregateAbsoluteError) throws DataNotFoundException
    {
        double time = mSymbolEvaluator.getTime();
        int numDynamicSymbols = mDynamicSymbolValues.length;

        rk4step(pTimeStepSize, pNewDynamicSymbolValues);

        double halfStepSize = pTimeStepSize / 2.0;
        double timePlusHalfStep = time + halfStepSize;

        double []y1 = mRKScratchPad.y1;

        rk4step(halfStepSize, y1);
        
        double []ysav = mRKScratchPad.ysav;
        System.arraycopy(mDynamicSymbolValues, 0, ysav, 0, numDynamicSymbols);
        System.arraycopy(y1, 0, mDynamicSymbolValues, 0, numDynamicSymbols);
        
        mSymbolEvaluator.setTime(timePlusHalfStep);

        double []y2 = mRKScratchPad.y2;

        rk4step(halfStepSize, y2);

        System.arraycopy(ysav, 0, mDynamicSymbolValues, 0, numDynamicSymbols);
        mSymbolEvaluator.setTime(time);

        double aggregateRelativeError = 0.0;
        double aggregateAbsoluteError = 0.0;
        double singleError = 0.0;
        double scale = 0.0;
        double deltaY = 0.0;

        // compute error
        for(int symCtr = numDynamicSymbols; --symCtr >= 0; )
        {
            // FOR DEBUGGING ONLY:
//            System.out.println("y1[" + symCtr + "]: " + pNewDynamicSymbolValues[symCtr] + "; y2[" + symCtr + "]: " + y2[symCtr] + "; yscal[" + symCtr + "]: " + pDynamicSymbolValueScales[symCtr]);

            
            scale = pDynamicSymbolValueScales[symCtr];
            if(scale > 0.0)
            {
                deltaY = Math.abs(pNewDynamicSymbolValues[symCtr] - y2[symCtr]);
                singleError = deltaY/scale;
                aggregateRelativeError += singleError;
                aggregateAbsoluteError += deltaY;
            }

            // problem:  what if there is only one y(t), and the initial y(0) and y'(0) are 0.0?
        }

        pRetAggregateRelativeError.setValue(aggregateRelativeError);
        pRetAggregateAbsoluteError.setValue(aggregateAbsoluteError);
    }

    protected abstract void setupErrorTolerances(SimulatorParameters pSimulatorParams,
                                                 RKScratchPad pRKScratchPad);
    

    public SimulationResults simulate(double pStartTime, 
                                      double pEndTime,
                                      SimulatorParameters pSimulatorParameters,
                                      int pNumResultsTimePoints,
                                      String []pRequestedSymbolNames) throws DataNotFoundException, IllegalStateException, AccuracyException
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
        SimulationController simulationController = mSimulationController;
        
        boolean doUpdates = (null != simulationController || null != simulationProgressReporter);
            
        long minNumMillisecondsForUpdate = 0;

        long timeOfLastUpdateMilliseconds = 0;
        if(doUpdates)
        {
            minNumMillisecondsForUpdate = mMinNumMillisecondsForUpdate;
            timeOfLastUpdateMilliseconds = System.currentTimeMillis();
        }

        long iterationCounter = 0;

        if(null != simulationProgressReporter)
        {
            simulationProgressReporter.updateProgressStatistics(false, 0.0, iterationCounter);
        }

        double []timesArray = createTimesArray(pStartTime, 
                                               pEndTime,
                                               pNumResultsTimePoints);        

        Symbol []requestedSymbols = createRequestedSymbolArray(mSymbolMap,
                                                               pRequestedSymbolNames);
        int numRequestedSymbols = requestedSymbols.length;

        SymbolEvaluatorChem symbolEvaluator = mSymbolEvaluator;
        double []reactionProbabilities = mReactionProbabilities;
        Reaction []reactions = mReactions;
        double []dynamicSymbolValues = mDynamicSymbolValues;        
        int numDynamicSymbolValues = dynamicSymbolValues.length;
        Object []dynamicSymbolAdjustmentVectors = mDynamicSymbolAdjustmentVectors;

        Value []nonDynamicSymbolValues = mNonDynamicSymbolValues;

        double time = pStartTime;        

        double timeRangeMult = 1.0 / (pEndTime - pStartTime);

        prepareForSimulation(time);

        // set "last" values for dynamic symbols to be same as initial values
        double []newSimulationSymbolValues = new double[numDynamicSymbolValues];
        System.arraycopy(dynamicSymbolValues, 0, newSimulationSymbolValues, 0, numDynamicSymbolValues);

        int timeCtr = 0;
        
        RKScratchPad scratchPad = mRKScratchPad;
        scratchPad.clear();

        setupErrorTolerances(pSimulatorParameters,
                             scratchPad);

        double deltaTime = pEndTime - pStartTime;

        double initialStepSize = pSimulatorParameters.getStepSizeFraction().doubleValue() * deltaTime;
        scratchPad.stepSize = initialStepSize;

        setupImpl(deltaTime,
                  pNumResultsTimePoints,
                  pSimulatorParameters,
                  scratchPad);

        boolean isCancelled = false;

        DelayedReactionSolver []delayedReactionSolvers = mDelayedReactionSolvers;
        int numDelayedReactions = 0;
        if(null != delayedReactionSolvers)
        {
            numDelayedReactions = delayedReactionSolvers.length;
        }

        long currentTimeMilliseconds = 0;
        double fractionComplete = 0.0;

        while(pNumResultsTimePoints - timeCtr > 0)
        {
            time = iterate(newSimulationSymbolValues);

            ++(scratchPad.numIterations);

            if(time > timesArray[timeCtr])
            {
                timeCtr = addRequestedSymbolValues(time,
                                                   timeCtr,
                                                   requestedSymbols,
                                                   timesArray,
                                                   retSymbolValues);
            }

            System.arraycopy(newSimulationSymbolValues, 0, dynamicSymbolValues, 0, numDynamicSymbolValues);

            if(null != delayedReactionSolvers)
            {
                // update delayed reaction solvers
                for(int ctr = numDelayedReactions; --ctr >= 0; )
                {
                    DelayedReactionSolver solver = delayedReactionSolvers[ctr];
                    solver.update(symbolEvaluator, time);
                }
            }

            ++iterationCounter;

            if(doUpdates)
            {
                currentTimeMilliseconds = System.currentTimeMillis();
                if(currentTimeMilliseconds - timeOfLastUpdateMilliseconds >= minNumMillisecondsForUpdate)
                {
                    if(null != simulationController)
                    {
                        isCancelled = simulationController.handlePauseOrCancel();
                        if(isCancelled)
                        {
                            break;
                        }
                    }

                    if(null != simulationProgressReporter)
                    {
                        fractionComplete = time*timeRangeMult;
                        simulationProgressReporter.updateProgressStatistics(false, fractionComplete, iterationCounter);
                    }

                    timeOfLastUpdateMilliseconds = System.currentTimeMillis();
                }
            }
        }

        if(null != simulationProgressReporter)
        {
            fractionComplete = time*timeRangeMult;
            simulationProgressReporter.updateProgressStatistics(true, fractionComplete, iterationCounter);
        }

        SimulationResults simulationResults = null;

        if(! isCancelled)
        {
            boolean estimateFinalSpeciesFluctuations = DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS;
            Boolean flagGetFinalSymbolFluctuations = pSimulatorParameters.getComputeFluctuations();
            if(null != flagGetFinalSymbolFluctuations)
            {
                estimateFinalSpeciesFluctuations = flagGetFinalSymbolFluctuations.booleanValue();
            }        
            double []finalSpeciesFluctuations = null;
            if(estimateFinalSpeciesFluctuations)
            {
                if(mUseExpressionValueCaching)
                {
                    clearExpressionValueCaches();
                }
                computeReactionProbabilities();
                double []allFinalSpeciesFluctuations = SteadyStateAnalyzer.estimateSpeciesFluctuations(reactions,
                                                                                                       mDynamicSymbols,
                                                                                                       mDynamicSymbolAdjustmentVectors,
                                                                                                       mReactionProbabilities,
                                                                                                       symbolEvaluator);
                if(null != allFinalSpeciesFluctuations)
                {
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

    public boolean allowsInterrupt()
    {
        return(true);
    }

    protected abstract void setupImpl(double pDeltaTime,
                                      int pNumResultsTimePoints,
                                      SimulatorParameters pSimulatorParams,
                                      RKScratchPad pRKScratchPad);

}
