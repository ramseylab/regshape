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
 * described by {@link Reaction} objects using the Gillespie stochastic
 * algorithm, "direct method".
 *
 * @author Stephen Ramsey
 */
public final class SimulatorStochasticGillespie extends SimulatorStochasticBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "gillespie-direct"; 
    private static final long NUMBER_FIRINGS = 1;

    protected void prepareForStochasticSimulation(double pStartTime,
                                                  SimulatorParameters pSimulatorParameters)
    {
        // nothing to do
    }

    protected double iterate(MutableInteger pLastReactionIndex) throws DataNotFoundException, IllegalStateException
    {
        double time = mSymbolEvaluator.getTime();

        int lastReactionIndex = pLastReactionIndex.getValue();
        if(NULL_REACTION != lastReactionIndex)
        {
            updateSymbolValuesForReaction(lastReactionIndex,
                                          mDynamicSymbolValues,
                                          mDynamicSymbolDelayedReactionAssociations,
                                          NUMBER_FIRINGS);
        }

        computeReactionProbabilities();
        
        double aggregateReactionProbability = DoubleVector.sumElements(mReactionProbabilities);
        double deltaTimeToNextReaction = Double.POSITIVE_INFINITY;

        if(aggregateReactionProbability > 0.0)
        {
            deltaTimeToNextReaction = chooseDeltaTimeToNextReaction(aggregateReactionProbability);
        }
        
        int reactionIndex = -1;

        if(null != mDelayedReactionSolvers)
        {
            int nextDelayedReactionIndex = getNextDelayedReactionIndex(mDelayedReactionSolvers);
            if(nextDelayedReactionIndex >= 0)
            {
                DelayedReactionSolver solver = mDelayedReactionSolvers[nextDelayedReactionIndex];
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
            reactionIndex = chooseIndexOfNextReaction(aggregateReactionProbability);
        }

        if(-1 != reactionIndex)
        {
            pLastReactionIndex.setValue(reactionIndex);

            time += deltaTimeToNextReaction;
        }
        else
        {
            time = Double.POSITIVE_INFINITY;
        }

        mSymbolEvaluator.setTime(time);

        return(time);
    }


    public void initialize(Model pModel) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel);
        initializeSimulatorStochastic(pModel);
        setInitialized(true);
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
