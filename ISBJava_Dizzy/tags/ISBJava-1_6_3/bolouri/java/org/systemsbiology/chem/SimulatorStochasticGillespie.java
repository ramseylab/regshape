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

import java.util.*;
import edu.cornell.lassp.houle.RngPack.*;

/**
 * Simulates the dynamics of a set of coupled chemical reactions
 * described by {@link Reaction} objects using the Gillespie stochastic
 * algorithm, "direct method".
 *
 * @author Stephen Ramsey
 */
public final class SimulatorStochasticGillespie extends SimulatorStochasticBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "gillespie-direct"; 
    private static final long NUMBER_FIRINGS = 1;

    protected void prepareForStochasticSimulation(SymbolEvaluatorChem pSymbolEvaluator,
                                                  double pStartTime,
                                                  RandomElement pRandomNumberGenerator,
                                                  Reaction []pReactions,
                                                  double []pReactionProbabilities,
                                                  SimulatorParameters pSimulatorParameters)
    {
        // nothing to do
    }

    protected double iterate(SymbolEvaluatorChem pSymbolEvaluator,
                             double pEndTime,
                             Reaction []pReactions,
                             double []pReactionProbabilities,
                             RandomElement pRandomNumberGenerator,
                             double []pDynamicSymbolValues,
                             MutableInteger pLastReactionIndex,
                             DelayedReactionSolver []pDelayedReactionSolvers,
                             boolean pHasExpressionValues,
                             Value []pNonDynamicSymbolValues) throws DataNotFoundException, IllegalStateException
    {
        double time = pSymbolEvaluator.getTime();

        int lastReactionIndex = pLastReactionIndex.getValue();
        if(NULL_REACTION != lastReactionIndex)
        {
            Reaction lastReaction = pReactions[lastReactionIndex];

            updateSymbolValuesForReaction(pSymbolEvaluator,
                                          lastReaction,
                                          pDynamicSymbolValues,
                                          pDelayedReactionSolvers,
                                          NUMBER_FIRINGS);
        }

        computeReactionProbabilities(pSymbolEvaluator,
                                     pReactionProbabilities,
                                     pReactions,
                                     pHasExpressionValues,
                                     pNonDynamicSymbolValues,
                                     true);
        
        double aggregateReactionProbability = MathFunctions.vectorSumElements(pReactionProbabilities);
        double deltaTimeToNextReaction = Double.POSITIVE_INFINITY;

        if(aggregateReactionProbability > 0.0)
        {
            deltaTimeToNextReaction = chooseDeltaTimeToNextReaction(pRandomNumberGenerator, 
                                                                    aggregateReactionProbability);
        }
        
        int reactionIndex = -1;

        if(pDelayedReactionSolvers.length == 0)
        {
            // do nothing
        }
        else
        {
            int nextDelayedReactionIndex = getNextDelayedReactionIndex(pDelayedReactionSolvers);
            if(nextDelayedReactionIndex >= 0)
            {
                DelayedReactionSolver solver = pDelayedReactionSolvers[nextDelayedReactionIndex];
                double nextDelayedReactionTime = solver.peekNextReactionTime();
//                System.out.println("next delayed reaction will occur at: " + nextDelayedReactionTime);
                if(nextDelayedReactionTime < time + deltaTimeToNextReaction)
                {
                    // execute delayed reaction
                    deltaTimeToNextReaction = nextDelayedReactionTime - time;
                    reactionIndex = solver.getReactionIndex();
//                    System.out.println("delayed reaction \"" + pReactions[reactionIndex] + "\" occurring at time: " + nextDelayedReactionTime );
                    solver.pollNextReactionTime();
                }
            }
        }

        if(-1 == reactionIndex && aggregateReactionProbability > 0.0)
        {
            reactionIndex = chooseIndexOfNextReaction(pRandomNumberGenerator,
                                                      aggregateReactionProbability,
                                                      pReactions,
                                                      pReactionProbabilities);
        }

        if(-1 != reactionIndex)
        {
            // choose type of next reaction
            Reaction reaction = pReactions[reactionIndex];

            pLastReactionIndex.setValue(reactionIndex);

            time += deltaTimeToNextReaction;
            
//            System.out.println("time: " + time + "; reaction occurred: " + reaction);
        }
        else
        {
            time = pEndTime;
        }

        pSymbolEvaluator.setTime(time);

        return(time);
    }


    public void initialize(Model pModel) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel);
        initializeSimulatorStochastic(pModel);
    }

    protected void modifyDefaultSimulatorParameters(SimulatorParameters pSimulatorParameters)
    {
        // do nothing
    }

    public String getAlias()
    {
        return(CLASS_ALIAS);
    }
}
