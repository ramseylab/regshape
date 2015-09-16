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
 * described by {@link Reaction} objects using the Gillespie stochastic
 * algorithm, "direct method".
 *
 * @author Stephen Ramsey
 */
public class SimulatorStochasticGillespie extends SimulatorStochasticBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "gillespie-direct"; 

    private static final int chooseIndexOfNextReaction(Random pRandomNumberGenerator,
                                                       double pAggregateReactionProbabilityDensity, 
                                                       Reaction []pReactions,
                                                       double []pReactionProbabilities) throws IllegalArgumentException
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(pRandomNumberGenerator);

        double cumulativeReactionProbabilityDensity = 0.0;

        double fractionOfAggregateReactionProbabilityDensity = randomNumberUniformInterval * pAggregateReactionProbabilityDensity;

        if(pAggregateReactionProbabilityDensity <= 0.0)
        {
            throw new IllegalArgumentException("invalid aggregate reaction probability density: " + pAggregateReactionProbabilityDensity);
        }

        int numReactions = pReactions.length;
        int reactionIndex = -1;
        Reaction reaction = null;
        for(int reactionCtr = numReactions - 1; reactionCtr >= 0; --reactionCtr)
        {
            double reactionProbability = pReactionProbabilities[reactionCtr];
            reaction = pReactions[reactionCtr];
            cumulativeReactionProbabilityDensity += reactionProbability;
            if(cumulativeReactionProbabilityDensity >= fractionOfAggregateReactionProbabilityDensity)
            {
                reactionIndex = reactionCtr;
                break;
            }
        }
        assert (-1 != reactionIndex) : "null reaction found in chooseTypeOfNextReaction";
        return(reactionIndex);
    }

    protected final void prepareForStochasticSimulation(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                                        SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                        double pStartTime,
                                                        Random pRandomNumberGenerator,
                                                        Reaction []pReactions,
                                                        double []pReactionProbabilities)
    {
        // nothing to do
    }

    protected final double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                   SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                   double pEndTime,
                                   Reaction []pReactions,
                                   double []pReactionProbabilities,
                                   Random pRandomNumberGenerator,
                                   double []pDynamicSymbolValues,
                                   MutableInteger pLastReactionIndex,
                                   DelayedReactionSolver []pDelayedReactionSolvers) throws DataNotFoundException, IllegalStateException
    {
        double time = pSymbolEvaluator.getTime();

        int lastReactionIndex = pLastReactionIndex.getValue();
        if(NULL_REACTION != lastReactionIndex)
        {
            Reaction lastReaction = pReactions[lastReactionIndex];

            updateSymbolValuesForReaction(pSymbolEvaluator,
                                          lastReaction,
                                          pDynamicSymbolValues,
                                          time,
                                          pDelayedReactionSolvers);
            clearExpressionValueCaches();
        }

        computeReactionProbabilities(pSpeciesRateFactorEvaluator,
                                     pSymbolEvaluator,
                                     pReactionProbabilities,
                                     pReactions);
        
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
                assert (nextDelayedReactionTime > time) : "invalid time for next delayed reaction";
//                System.out.println("next delayed reaction will occur at: " + nextDelayedReactionTime);
                if(nextDelayedReactionTime < time + deltaTimeToNextReaction)
                {
                    // execute delayed reaction
                    deltaTimeToNextReaction = nextDelayedReactionTime - time;
                    reactionIndex = solver.getReactionIndex();
//                    System.out.println("delayed reaction selected: " + pReactions[reactionIndex]);
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
        // we have changed the time; must nuke the cache of expression values
        clearExpressionValueCaches();

        return(time);
    }


    public void initialize(Model pModel, SimulationController pSimulationController) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel, pSimulationController);
        initializeSimulatorStochastic(pModel, pSimulationController);
    }

}
