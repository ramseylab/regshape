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
public class DeterministicSimulatorAdaptive extends DeterministicSimulator implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "ODE-RK5-adaptive";

    private static final double SAFETY = 0.9;
    private static final double PGROW = -0.20;
    private static final double PSHRINK = -0.25;
    private static final double ERRCON = 6.0e-4;
    private static final int MAXSTEPS = 100;


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
        double maxFractionalError = pRKScratchPad.maxFractionalError;

        double nextStepSize = adaptiveStep(pSpeciesRateFactorEvaluator,
                                           pSymbolEvaluator,
                                           pReactions,
                                           pDynamicSymbolAdjustmentVectors,
                                           pReactionProbabilities,
                                           pRKScratchPad,
                                           maxFractionalError,
                                           stepSize,
                                           pDynamicSymbolValues,
                                           pNewDynamicSymbolValues);

        pRKScratchPad.stepSize = nextStepSize;

        return(pSymbolEvaluator.getTime());
    }

    private static final double adaptiveStep(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                             SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                             Reaction []pReactions,
                                             Object []pDynamicSymbolAdjustmentVectors,
                                             double []pReactionProbabilities,
                                             RKScratchPad pRKScratchPad,
                                             double pMaxFractionalError,
                                             double pTimeStepSize,
                                             double []pDynamicSymbolValues,
                                             double []pNewDynamicSymbolValues) throws DataNotFoundException, SimulationAccuracyException
    {
        double stepSize = pTimeStepSize;
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

        double aggregateError = 0.0;
        double errRatio = 0.0;

        double time = pSymbolEvaluator.getTime();

        int numSteps = 0;

        do
        {
            aggregateError = rkqc(pSpeciesRateFactorEvaluator,
                                  pSymbolEvaluator,
                                  pReactions,
                                  pDynamicSymbolAdjustmentVectors,
                                  pReactionProbabilities,
                                  pRKScratchPad,
                                  stepSize,
                                  yscale,
                                  pDynamicSymbolValues,
                                  pNewDynamicSymbolValues);

            errRatio = aggregateError / pMaxFractionalError ;
// FOR DEBUGGING ONLY:
//            System.out.println("time: " + time + "; stepsize: " + stepSize + "; aggregateError: " + aggregateError + "; errRatio: " + errRatio);
            
            if(errRatio > 1.0)
            {
                // error is too big; need to decrease the step size
                stepSize *= SAFETY * Math.exp(PSHRINK * Math.log(errRatio));
            }
            else
            {
                break;
            }

            numSteps++;
            if(numSteps > MAXSTEPS)
            {
                throw new SimulationAccuracyException("maximum number of time step subdivisions exceeded; this model probably is too stuff for this simple Runge-Kutta adaptive integrator");
            }
        }
        while(true);
        
        pSymbolEvaluator.setTime(time + stepSize);

        double nextStepSize = 0.0;

        if(errRatio > ERRCON)
        {
            nextStepSize =  SAFETY * stepSize * Math.exp(PGROW * Math.log(errRatio));
        }
        else
        {
            nextStepSize = 4.0 * stepSize;
        }

        double maxStepSize = pRKScratchPad.maxStepSize;
        if(nextStepSize > maxStepSize)
        {
            nextStepSize = maxStepSize;
        }

        return(nextStepSize);
    }

    protected final void setupScratchPad(double pStartTime,
                                         double pEndTime,
                                         SimulatorParameters pSimulatorParams, 
                                         RKScratchPad pRKScratchPad)
    {
        Double maxFractionalErrorObj = pSimulatorParams.getMaxAllowedError();
        if(null != maxFractionalErrorObj)
        {
            double maxFractionalError = maxFractionalErrorObj.doubleValue();
            pRKScratchPad.maxFractionalError = maxFractionalError;
        }
        else
        {
            throw new IllegalArgumentException("max fractional error must be specified");
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

        double maxStepSize = (pEndTime - pStartTime) / ((double) minNumSteps);
        double stepSize = maxStepSize / 5.0;

        pRKScratchPad.stepSize = stepSize;
        pRKScratchPad.maxStepSize = maxStepSize;
    }
}
