package org.systemsbiology.chem;

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

    protected final double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                          SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                          Reaction []pReactions,
                                          double []pReactionProbabilities,
                                          RKScratchPad pRKScratchPad,
                                          double pMaxFractionalError,
                                          double []pDynamicSymbolValues,
                                          double []pNewDynamicSymbolValues) throws DataNotFoundException
    {
        double stepSize = pRKScratchPad.stepSize;

        double nextStepSize = adaptiveStep(pSpeciesRateFactorEvaluator,
                                           pSymbolEvaluator,
                                           pReactions,
                                           pReactionProbabilities,
                                           pRKScratchPad,
                                           pMaxFractionalError,
                                           stepSize,
                                           pDynamicSymbolValues,
                                           pNewDynamicSymbolValues);

        pRKScratchPad.stepSize = nextStepSize;

        return(pSymbolEvaluator.getTime());
    }

    private static final double rkqc(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                     SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                     Reaction []pReactions,
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
            singleError = Math.abs(pNewDynamicSymbolValues[symCtr] - y2[symCtr])/pDynamicSymbolValueScales[symCtr];
            aggregateError += singleError;
        }

        return(aggregateError);
    }

    private static final double adaptiveStep(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                             SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                             Reaction []pReactions,
                                             double []pReactionProbabilities,
                                             RKScratchPad pRKScratchPad,
                                             double pMaxFractionalError,
                                             double pTimeStepSize,
                                             double []pDynamicSymbolValues,
                                             double []pNewDynamicSymbolValues) throws DataNotFoundException
    {
        double []dydt = pRKScratchPad.dydt;
        double []yscratch = pRKScratchPad.yscratch;
        double []yscale = pRKScratchPad.yscale;

        computeDerivative(pSpeciesRateFactorEvaluator,
                          pSymbolEvaluator,
                          pReactions,
                          pReactionProbabilities,
                          yscratch,
                          dydt);

        int numDynamicSymbols = pDynamicSymbolValues.length;
        double dydtn = 0.0;
        double yn = 0.0;

        for(int symCtr = numDynamicSymbols; --symCtr >= 0; )
        {
            dydtn = dydt[symCtr];
            yn = pDynamicSymbolValues[symCtr];
            yscale[symCtr] = Math.abs(yn) + Math.abs(dydtn * pTimeStepSize) + TINY;
        }
        
        double aggregateError = 0.0;
        double errRatio = 0.0;
        double stepSize = pTimeStepSize;

        double time = pSymbolEvaluator.getTime();

        do
        {
            aggregateError = rkqc(pSpeciesRateFactorEvaluator,
                                  pSymbolEvaluator,
                                  pReactions,
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

            // decrease step size
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


}
