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
public class DeterministicSimulatorFixed extends DeterministicSimulator implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "ODE-RK5-fixed";


    // fixed step-size integrator
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

        rk4step(pSpeciesRateFactorEvaluator,
                pSymbolEvaluator,
                pReactions,
                pReactionProbabilities,
                pRKScratchPad,
                stepSize,  
                pDynamicSymbolValues,
                pNewDynamicSymbolValues);

        pSymbolEvaluator.setTime(pSymbolEvaluator.getTime() + stepSize);

        return(pSymbolEvaluator.getTime());
    }

}
