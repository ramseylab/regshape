package org.systemsbiology.chem;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.Value;
import org.systemsbiology.math.Symbol;
import org.systemsbiology.math.SymbolEvaluator;
import org.systemsbiology.math.MathFunctions;
import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.IAliasableClass;
import org.systemsbiology.util.DebugUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Simulates the dynamics of a set of coupled chemical reactions
 * described by {@link Reaction} objects using the Runge-Kutta
 * algorithm (fifth order with adaptive step-size control).
 *
 * @author Stephen Ramsey
 */
public abstract class DeterministicSimulator extends Simulator
{
    public static final int DEFAULT_MIN_NUM_STEPS = 1000;
    public static final double DEFAULT_MAX_ALLOWED_ERROR = 0.01;
    private static final int NUM_ITERATIONS_CHECK_CANCELLED = 100;

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
        double maxFractionalError;
        int numIterations;

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
            clear();
        }

        public void clear()
        {
            MathFunctions.vectorZeroElements(k1);
            MathFunctions.vectorZeroElements(k2);
            MathFunctions.vectorZeroElements(k3);
            MathFunctions.vectorZeroElements(k4);
            MathFunctions.vectorZeroElements(ysav);
            MathFunctions.vectorZeroElements(yscratch);
            MathFunctions.vectorZeroElements(y1);
            MathFunctions.vectorZeroElements(y2);
            MathFunctions.vectorZeroElements(yscale);
            MathFunctions.vectorZeroElements(dydt);
            stepSize = 0.0;
            maxStepSize = 0.0;
            maxFractionalError = 0.0;
            numIterations = 0;
        }
    }

    protected RKScratchPad mRKScratchPad;
    protected Object []mDynamicSymbolAdjustmentVectors;

    protected abstract void setupScratchPad(double pStartTime,
                                            double pEndTime,
                                            SimulatorParameters pSimulatorParams, 
                                            RKScratchPad pRKScratchPad);

    protected static final void computeDerivative(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                                  SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                  Reaction []pReactions,
                                                  Object []pDynamicSymbolAdjustmentVectors,
                                                  double []pReactionProbabilities,
                                                  double []pTempDynamicSymbolValues,
                                                  double []pDynamicSymbolDerivatives) throws DataNotFoundException
    {
        computeReactionProbabilities(pSpeciesRateFactorEvaluator,
                                     pSymbolEvaluator,
                                     pReactionProbabilities,
                                     pReactions);

        int numReactions = pReactions.length;
        Reaction reaction = null;
        double reactionRate = 0.0;

        MathFunctions.vectorZeroElements(pDynamicSymbolDerivatives);

        Object []dynamicSymbolAdjustmentVectors = pDynamicSymbolAdjustmentVectors;

        for(int reactionCtr = numReactions; --reactionCtr >= 0; )
        {
            reaction = pReactions[reactionCtr];
            reactionRate = pReactionProbabilities[reactionCtr];

            double []symbolAdjustmentVector = (double []) dynamicSymbolAdjustmentVectors[reactionCtr];
            
            // we want to multiply this vector by the reaction rate and add it to the derivative
            MathFunctions.vectorScalarMultiply(symbolAdjustmentVector, reactionRate, pTempDynamicSymbolValues);
            
            MathFunctions.vectorAdd(pTempDynamicSymbolValues, pDynamicSymbolDerivatives, pDynamicSymbolDerivatives);
        }
    }

    protected abstract double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                      SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                      Reaction []pReactions,
                                      Object []pDynamicSymbolAdjustmentVectors,
                                      double []pReactionProbabilities,
                                      RKScratchPad pRKScratchPad,
                                      double []pDynamicSymbolValues,
                                      double []pNewDynamicSymbolValues) throws DataNotFoundException, SimulationAccuracyException;


    protected static final void rk4step(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                        SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                        Reaction []pReactions,
                                        Object []pDynamicSymbolAdjustmentVectors,
                                        double []pReactionProbabilities,
                                        RKScratchPad pRKScratchPad,
                                        double pTimeStepSize,
                                        double []pDynamicSymbolValues,
                                        double []pNewDynamicSymbolValues) throws DataNotFoundException
    {
        double time = pSymbolEvaluator.getTime();

        double []k1 = pRKScratchPad.k1;  // note:  our "k1" is equivalent to 0.5 times the "k1" in numerical recipes
        int numVars = k1.length;

        double []y = pDynamicSymbolValues;
        double []ysav = pRKScratchPad.ysav;
        double []yscratch = pRKScratchPad.yscratch;

        double halfStep = pTimeStepSize / 2.0;
        double timePlusHalfStep = time + halfStep;

        // save a copy of the initial y values
        System.arraycopy(y, 0, ysav, 0, numVars);

        computeDerivative(pSpeciesRateFactorEvaluator,
                          pSymbolEvaluator,
                          pReactions,
                          pDynamicSymbolAdjustmentVectors,
                          pReactionProbabilities,
                          yscratch,
                          k1);
        MathFunctions.vectorScalarMultiply(k1, halfStep, k1);
        // now, k1 contains  h * f'(t, y)/2.0

        // set the y values to "y + k1"
        MathFunctions.vectorAdd(ysav, k1, y);

        // set the time to "t + h/2"
        pSymbolEvaluator.setTime(timePlusHalfStep);

        double []k2 = pRKScratchPad.k2;
        computeDerivative(pSpeciesRateFactorEvaluator,
                          pSymbolEvaluator,
                          pReactions,
                          pDynamicSymbolAdjustmentVectors,
                          pReactionProbabilities,
                          yscratch,
                          k2);
        MathFunctions.vectorScalarMultiply(k2, halfStep, k2);

        MathFunctions.vectorAdd(ysav, k2, y);
        // y now contains "y + k2"

        double []k3 = pRKScratchPad.k3;
        computeDerivative(pSpeciesRateFactorEvaluator,
                          pSymbolEvaluator,
                          pReactions,
                          pDynamicSymbolAdjustmentVectors,
                          pReactionProbabilities,
                          yscratch,
                          k3);
        MathFunctions.vectorScalarMultiply(k3, pTimeStepSize, k3);
        // k3 now contains h * f'(t + h/2, y + k2)

        MathFunctions.vectorAdd(ysav, k3, y);
        // y now contains  "y + k3"

        double []k4 = pRKScratchPad.k4;


        // set time to "t + h"
        double pNextTime = time + pTimeStepSize;
        pSymbolEvaluator.setTime(pNextTime);

        computeDerivative(pSpeciesRateFactorEvaluator,
                          pSymbolEvaluator,
                          pReactions,
                          pDynamicSymbolAdjustmentVectors,
                          pReactionProbabilities,
                          yscratch,
                          k4);
        MathFunctions.vectorScalarMultiply(k4, pTimeStepSize, k4);
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

        MathFunctions.vectorZeroNegativeElements(pNewDynamicSymbolValues);

        // restore to previous values; allow the driver to update
        System.arraycopy(ysav, 0, y, 0, numVars);
        pSymbolEvaluator.setTime(time);
    }

    private void resetScratchpad()
    {
        int numDynamicSymbols = mDynamicSymbolValues.length;
        mRKScratchPad = new RKScratchPad(numDynamicSymbols);
    }

    private void initializeDynamicSymbolAdjustmentVectors(Species []pDynamicSymbols)
    {
        Reaction []reactions = mReactions;

        int numReactions = reactions.length;
        Object []dynamicSymbolAdjustmentVectors = new Object[numReactions];

        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            Reaction reaction = reactions[ctr];
            dynamicSymbolAdjustmentVectors[ctr ] = reaction.constructDynamicSymbolAdjustmentVector(pDynamicSymbols);
        }

        mDynamicSymbolAdjustmentVectors = dynamicSymbolAdjustmentVectors;
    }

    protected void initializeMultistepReactionSolvers()
    {
        MultistepReactionSolver []multistepSolvers = mMultistepReactionSolvers;
        int numMultistepSolvers = multistepSolvers.length;
        for(int ctr = 0; ctr < numMultistepSolvers; ++ctr)
        {
            MultistepReactionSolver solver = multistepSolvers[ctr];
            boolean isStochasticSimulator = false;
            solver.initialize(isStochasticSimulator);
        }
    }

    public void initialize(Model pModel, SimulationController pSimulationController) throws DataNotFoundException
    {
        initializeSimulator(pModel, pSimulationController);
        initializeDynamicSymbolAdjustmentVectors(mDynamicSymbols);
        resetScratchpad();
    }

    protected static final void computeScale(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                             SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                             Reaction []pReactions,
                                             Object []pDynamicSymbolAdjustmentVectors,
                                             double []pReactionProbabilities,
                                             RKScratchPad pRKScratchPad,
                                             double pTimeStepSize,
                                             double []pDynamicSymbolValues,
                                             double []yscale) throws DataNotFoundException, SimulationAccuracyException
    {
        double []yscratch = pRKScratchPad.yscratch;
        double []dydt = pRKScratchPad.dydt;
        int numDynamicSymbols = pDynamicSymbolValues.length;
        double dydtn = 0.0;
        double yn = 0.0;

        computeDerivative(pSpeciesRateFactorEvaluator,
                          pSymbolEvaluator,
                          pReactions,
                          pDynamicSymbolAdjustmentVectors,
                          pReactionProbabilities,
                          yscratch,
                          dydt);

        double scale;
        boolean gotNonzero = false;
        for(int symCtr = numDynamicSymbols; --symCtr >= 0; )
        {
            dydtn = dydt[symCtr];
            yn = pDynamicSymbolValues[symCtr];
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
            throw new SimulationAccuracyException("unable to determine any scale at time: " + pSymbolEvaluator.getTime());
        }
    }


    protected static final double rkqc(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                       SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                       Reaction []pReactions,
                                       Object []pDynamicSymbolAdjustmentVectors,
                                       double []pReactionProbabilities,
                                       RKScratchPad pRKScratchPad,
                                       double pTimeStepSize,
                                       double []pDynamicSymbolValueScales,
                                       double []pDynamicSymbolValues,
                                       double []pNewDynamicSymbolValues) throws DataNotFoundException
    {
        double time = pSymbolEvaluator.getTime();
        int numDynamicSymbols = pDynamicSymbolValues.length;

        rk4step(pSpeciesRateFactorEvaluator,
                pSymbolEvaluator,
                pReactions,
                pDynamicSymbolAdjustmentVectors,
                pReactionProbabilities,
                pRKScratchPad,
                pTimeStepSize,
                pDynamicSymbolValues,
                pNewDynamicSymbolValues);

        double halfStepSize = pTimeStepSize / 2.0;
        double timePlusHalfStep = time + halfStepSize;

        double []y1 = pRKScratchPad.y1;

        rk4step(pSpeciesRateFactorEvaluator,
                pSymbolEvaluator,
                pReactions,
                pDynamicSymbolAdjustmentVectors,
                pReactionProbabilities,
                pRKScratchPad,
                halfStepSize,
                pDynamicSymbolValues,
                y1);
        
        double []ysav = pRKScratchPad.ysav;
        System.arraycopy(pDynamicSymbolValues, 0, ysav, 0, numDynamicSymbols);
        System.arraycopy(y1, 0, pDynamicSymbolValues, 0, numDynamicSymbols);
        
        pSymbolEvaluator.setTime(timePlusHalfStep);

        double []y2 = pRKScratchPad.y2;

        rk4step(pSpeciesRateFactorEvaluator,
                pSymbolEvaluator,
                pReactions,
                pDynamicSymbolAdjustmentVectors,
                pReactionProbabilities,
                pRKScratchPad,
                halfStepSize,
                pDynamicSymbolValues,
                y2);

        System.arraycopy(ysav, 0, pDynamicSymbolValues, 0, numDynamicSymbols);
        pSymbolEvaluator.setTime(time);

        double aggregateError = 0.0;
        double singleError = 0.0;
        // compute error
        for(int symCtr = numDynamicSymbols; --symCtr >= 0; )
        {
            // FOR DEBUGGING ONLY:
//            System.out.println("y1[" + symCtr + "]: " + pNewDynamicSymbolValues[symCtr] + "; y2[" + symCtr + "]: " + y2[symCtr] + "; yscal[" + symCtr + "]: " + pDynamicSymbolValueScales[symCtr]);

            double scale = pDynamicSymbolValueScales[symCtr];
            if(scale > 0.0)
            {
                singleError = Math.abs(pNewDynamicSymbolValues[symCtr] - y2[symCtr])/scale;
                aggregateError += singleError;
            }

            // problem:  what if there is only one y(t), and the initial y(0) and y'(0) are 0.0?
        }

        return(aggregateError);
    }



    public void simulate(double pStartTime, 
                         double pEndTime,
                         SimulatorParameters pSimulatorParameters,
                         int pNumResultsTimePoints,
                         String []pRequestedSymbolNames,
                         double []pRetTimeValues,
                         Object []pRetSymbolValues) throws DataNotFoundException, IllegalStateException, SimulationAccuracyException
    {
        if(! mInitialized)
        {
            throw new IllegalStateException("simulator has not been initialized yet");
        }

        if(pNumResultsTimePoints <= 0)
        {
            throw new IllegalArgumentException("number of time points must be positive");
        }

        if(pStartTime > pEndTime)
        {
            throw new IllegalArgumentException("end time must come after start time");
        }
        
        if(pRetTimeValues.length != pNumResultsTimePoints)
        {
            throw new IllegalArgumentException("illegal length of pRetTimeValues array");
        }

        if(pRetSymbolValues.length != pNumResultsTimePoints)
        {
            throw new IllegalArgumentException("illegal length of pRetSymbolValues array");
        }

        SpeciesRateFactorEvaluator speciesRateFactorEvaluator = mSpeciesRateFactorEvaluator;
        SymbolEvaluatorChemSimulation symbolEvaluator = mSymbolEvaluator;
        double []reactionProbabilities = mReactionProbabilities;
        Reaction []reactions = mReactions;
        double []dynamicSymbolValues = mDynamicSymbolValues;        
        int numDynamicSymbolValues = dynamicSymbolValues.length;
        HashMap symbolMap = mSymbolMap;
        Object []dynamicSymbolAdjustmentVectors = mDynamicSymbolAdjustmentVectors;
        double []timesArray = new double[pNumResultsTimePoints];

        prepareTimesArray(pStartTime, 
                          pEndTime,
                          pNumResultsTimePoints,
                          timesArray);        

        Symbol []requestedSymbols = prepareRequestedSymbolArray(symbolMap,
                                                                pRequestedSymbolNames);

        int numRequestedSymbols = requestedSymbols.length;

        double []newSimulationSymbolValues = new double[numDynamicSymbolValues];

        double time = pStartTime;
        
        prepareForSimulation(time);

        // set "last" values for dynamic symbols to be same as initial values
        System.arraycopy(dynamicSymbolValues, 0, newSimulationSymbolValues, 0, numDynamicSymbolValues);

        int timeCtr = 0;
            
        int minNumSteps = pNumResultsTimePoints;

        int numMultistepReactionSteps = 0;
        int numReactions = reactions.length;
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            Reaction reaction = reactions[ctr];
            int numReactionSteps = reaction.getNumSteps();
            if(numReactionSteps > 1)
            {
                numMultistepReactionSteps += numReactionSteps;
            }
        }
        numMultistepReactionSteps *= 5;

        if(minNumSteps < numMultistepReactionSteps)
        {
            minNumSteps = numMultistepReactionSteps;
        }

        Integer minNumStepsObj = pSimulatorParameters.getMinNumSteps();
        if(null != minNumStepsObj)
        {
            if(minNumStepsObj.intValue() < minNumSteps)
            {
                pSimulatorParameters.setMinNumSteps(minNumSteps);
            }
        }
        else
        {
            pSimulatorParameters.setMinNumSteps(minNumSteps);
        }
        

        RKScratchPad scratchPad = mRKScratchPad;
        scratchPad.clear();

        setupScratchPad(pStartTime,
                        pEndTime,
                        pSimulatorParameters,
                        scratchPad);

        boolean isCancelled = false;

        int iterationCtr = 0;
        boolean checkCancelled = false;

        while(pNumResultsTimePoints - timeCtr > 0)
        {
            time = iterate(speciesRateFactorEvaluator,
                           symbolEvaluator,
                           reactions,
                           dynamicSymbolAdjustmentVectors,
                           reactionProbabilities,
                           scratchPad,
                           dynamicSymbolValues,
                           newSimulationSymbolValues);

            ++(scratchPad.numIterations);

            if(time > timesArray[timeCtr])
            {
                timeCtr = addRequestedSymbolValues(time,
                                                   timeCtr,
                                                   requestedSymbols,
                                                   symbolEvaluator,
                                                   timesArray,
                                                   pRetSymbolValues);

                checkCancelled = true;
            }

            System.arraycopy(newSimulationSymbolValues, 0, dynamicSymbolValues, 0, numDynamicSymbolValues);

            ++iterationCtr;
            if(iterationCtr > NUM_ITERATIONS_CHECK_CANCELLED)
            {
                iterationCtr = 0;
                checkCancelled = true;
            }

            if(checkCancelled)
            {
                isCancelled = checkSimulationControllerStatus();
                if(isCancelled)
                {
                    break;
                }
                checkCancelled = false;
            }
        }

        // copy array of time points 
        System.arraycopy(timesArray, 0, pRetTimeValues, 0, timeCtr);        
    }


    public SimulatorParameters getDefaultSimulatorParameters()
    {
        SimulatorParameters sp = new SimulatorParameters();
        sp.setMinNumSteps(DEFAULT_MIN_NUM_STEPS);
        sp.setMaxAllowedError(DEFAULT_MAX_ALLOWED_ERROR);
        return(sp);
    }
}
