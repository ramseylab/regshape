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
public class SimulatorDeterministicRungeKuttaFixed extends SimulatorDeterministicBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "ODE-RK5-fixed";
    private static final int NUM_ITERATIONS_BEFORE_ERROR_CHECK = 10;

    // fixed step-size integrator
    protected final double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                   SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                   Reaction []pReactions,
                                   Object []pDynamicSymbolAdjustmentVectors,
                                   double []pReactionProbabilities,
                                   RKScratchPad pRKScratchPad,
                                   double []pDynamicSymbolValues,
                                   double []pNewDynamicSymbolValues) throws DataNotFoundException, SimulationAccuracyException
    {
        double stepSize = pRKScratchPad.stepSize;


        int numIterations = pRKScratchPad.numIterations;
        if(0 != numIterations % NUM_ITERATIONS_BEFORE_ERROR_CHECK)
        {
            rk4step(pSpeciesRateFactorEvaluator,
                    pSymbolEvaluator,
                    pReactions,
                    pDynamicSymbolAdjustmentVectors,
                    pReactionProbabilities,
                    pRKScratchPad,
                    stepSize,  
                    pDynamicSymbolValues,
                    pNewDynamicSymbolValues);
        }
        else
        {
            double []yscale = pRKScratchPad.yscale;

            computeScale(pSpeciesRateFactorEvaluator,
                         pSymbolEvaluator,
                         pReactions,
                         pDynamicSymbolAdjustmentVectors,
                         pReactionProbabilities,
                         pRKScratchPad,
                         stepSize,
                         pDynamicSymbolValues,
                         yscale);

            MutableDouble relativeErrorObj = pRKScratchPad.relativeError;
            MutableDouble absoluteErrorObj = pRKScratchPad.absoluteError;

            rkqc(pSpeciesRateFactorEvaluator,
                 pSymbolEvaluator,
                 pReactions,
                 pDynamicSymbolAdjustmentVectors,
                 pReactionProbabilities,
                 pRKScratchPad,
                 stepSize,  
                 yscale,
                 pDynamicSymbolValues,
                 pNewDynamicSymbolValues,
                 relativeErrorObj,
                 absoluteErrorObj);

            double maxRelativeError = pRKScratchPad.maxRelativeError;
            double relativeError = relativeErrorObj.getValue();

            if(maxRelativeError - relativeError < 0.0)
            {
                throw new SimulationAccuracyException("numeric approximation error exceeded threshold; try a larger value for \"min number of timesteps\"");
            }

            double maxAbsoluteError = pRKScratchPad.maxAbsoluteError;
            double absoluteError = absoluteErrorObj.getValue();

            if(maxAbsoluteError - absoluteError < 0.0)
            {
                throw new SimulationAccuracyException("numeric approximation error exceeded threshold; try a larger value for \"min number of timesteps\"");
            }
        }

        pSymbolEvaluator.setTime(pSymbolEvaluator.getTime() + stepSize);

        return(pSymbolEvaluator.getTime());
    }

    protected final void setupScratchPad(double pStartTime,
                                         double pEndTime,
                                         SimulatorParameters pSimulatorParams, 
                                         RKScratchPad pRKScratchPad)
    {
        Double maxAbsoluteErrorObj = pSimulatorParams.getMaxAllowedAbsoluteError();
        if(null != maxAbsoluteErrorObj)
        {
            double maxAbsoluteError = maxAbsoluteErrorObj.doubleValue();
            pRKScratchPad.maxAbsoluteError = maxAbsoluteError;
        }
        else
        {
            throw new IllegalArgumentException("max absolute error must be specified");
        }

        Double maxRelativeErrorObj = pSimulatorParams.getMaxAllowedRelativeError();
        if(null != maxRelativeErrorObj)
        {
            double maxRelativeError = maxRelativeErrorObj.doubleValue();
            pRKScratchPad.maxRelativeError = maxRelativeError;
        }
        else
        {
            throw new IllegalArgumentException("max relative error must be specified");
        }

        int minNumSteps;
        Integer minNumStepsObj = pSimulatorParams.getMinNumSteps();
        if(null != minNumStepsObj)
        {
            minNumSteps = minNumStepsObj.intValue();
            if(minNumSteps <= 0)
            {
                throw new IllegalArgumentException("illegal value for number of steps");
            }
        }
        else
        {
            throw new IllegalArgumentException("required minimum number of steps was not provided");
        }

        double stepSize = (pEndTime - pStartTime) / ((double) minNumSteps);

        pRKScratchPad.stepSize = stepSize;
    }

}
