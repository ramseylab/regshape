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
import org.systemsbiology.math.MutableDouble;
import org.systemsbiology.math.MutableInteger;
import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.InvalidInputException;
import org.systemsbiology.util.IAliasableClass;

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
public class GillespieSimulator extends StochasticSimulator implements IAliasableClass, ISimulator
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

    private static final double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                        SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                        double pEndTime,
                                        Reaction []pReactions,
                                        double []pReactionProbabilities,
                                        Random pRandomNumberGenerator,
                                        double []pDynamicSymbolValues,
                                        MutableInteger pLastReactionIndex,
                                        MultistepReactionSolver []pMultistepReactionSolvers) throws DataNotFoundException, IllegalStateException
    {
        double time = pSymbolEvaluator.getTime();

        int lastReactionIndex = pLastReactionIndex.getValue();
        if(lastReactionIndex >= 0)
        {

            Reaction lastReaction = pReactions[lastReactionIndex];

            updateSymbolValuesForReaction(pSymbolEvaluator,
                                          lastReaction,
                                          pDynamicSymbolValues,
                                          time,
                                          pMultistepReactionSolvers);
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

        if(pMultistepReactionSolvers.length == 0)
        {
            // do nothing
        }
        else
        {
            int nextMultistepReactionIndex = getNextMultistepReactionIndex(pMultistepReactionSolvers);
            if(nextMultistepReactionIndex >= 0)
            {
                MultistepReactionSolver solver = pMultistepReactionSolvers[nextMultistepReactionIndex];
                double nextMultistepReactionTime = solver.peekNextReactionTime();
                assert (nextMultistepReactionTime > time) : "invalid time for next multistep reaction";
//                System.out.println("next multistep reaction will occur at: " + nextMultistepReactionTime);
                if(nextMultistepReactionTime < time + deltaTimeToNextReaction)
                {
                    // execute multistep reaction
                    deltaTimeToNextReaction = nextMultistepReactionTime - time;
                    reactionIndex = solver.getReactionIndex();
//                    System.out.println("multistep reaction selected: " + pReactions[reactionIndex]);
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


    public void initialize(Model pModel, SimulationController pSimulationController) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel, pSimulationController);
        checkDynamicalSymbolsInitialValues();
        initializeRandomNumberGenerator();
    }

    public final void simulate(double pStartTime, 
                               double pEndTime,
                               SimulatorParameters pSimulatorParameters,
                               int pNumResultsTimePoints,
                               String []pRequestedSymbolNames,
                               double []pRetTimeValues,
                               Object []pRetSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException
    {
        if(! mInitialized)
        {
            throw new IllegalStateException("simulator not initialized yet");
        }

        Integer ensembleSizeObj = pSimulatorParameters.getEnsembleSize();
        if(null == ensembleSizeObj)
        {
            throw new IllegalArgumentException("ensemble size was not defined");
        }
            
        int ensembleSize = ensembleSizeObj.intValue();
        if(ensembleSize <= 0)
        {
            throw new IllegalArgumentException("illegal value for ensemble size");
        }

        if(pNumResultsTimePoints <= 0)
        {
            throw new IllegalArgumentException("number of time points must be nonnegative");
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
        Random randomNumberGenerator = mRandomNumberGenerator;
        Reaction []reactions = mReactions;
        double []dynamicSymbolValues = mDynamicSymbolValues;        
        int numDynamicSymbolValues = dynamicSymbolValues.length;
        HashMap symbolMap = mSymbolMap;
        MultistepReactionSolver []multistepReactionSolvers = mMultistepReactionSolvers;

        double []timesArray = new double[pNumResultsTimePoints];

        prepareTimesArray(pStartTime, 
                          pEndTime,
                          pNumResultsTimePoints,
                          timesArray);        

        Symbol []requestedSymbols = prepareRequestedSymbolArray(symbolMap,
                                                                pRequestedSymbolNames);

        int numRequestedSymbols = requestedSymbols.length;

        boolean isCancelled = false;

        int timeCtr = 0;

        MutableInteger lastReactionIndex = new MutableInteger(NULL_REACTION);

        for(int simCtr = ensembleSize; --simCtr >= 0; )
        {
            timeCtr = 0;

            double time = pStartTime;
            prepareForSimulation(time);
            lastReactionIndex.setValue(NULL_REACTION);

//            int numIterations = 0;

            while(pNumResultsTimePoints - timeCtr > 0)
            {
                time = iterate(speciesRateFactorEvaluator,
                               symbolEvaluator,
                               pEndTime,
                               reactions,
                               reactionProbabilities,
                               randomNumberGenerator,
                               dynamicSymbolValues,
                               lastReactionIndex,
                               multistepReactionSolvers);

//                ++numIterations;

                if(time > timesArray[timeCtr])
                {
                    timeCtr = addRequestedSymbolValues(time,
                                                       timeCtr,
                                                       requestedSymbols,
                                                       symbolEvaluator,
                                                       timesArray,
                                                       pRetSymbolValues);

                    isCancelled = checkSimulationControllerStatus();
                    if(isCancelled)
                    {
                        break;
                    }
                }
            }
            
            if(isCancelled)
            {
                break;
            }

//            System.out.println("number of iterations: " + numIterations);

        }

        double ensembleMult = 1.0 / ((double) ensembleSize);

        for(int timePointCtr = timeCtr; --timePointCtr >= 0; )
        {
            for(int symbolCtr = numRequestedSymbols; --symbolCtr >= 0; )
            {
                double []symbolValues = (double []) pRetSymbolValues[timePointCtr];
                symbolValues[symbolCtr] *= ensembleMult;
            }
        }

        // copy array of time points 
        System.arraycopy(timesArray, 0, pRetTimeValues, 0, timeCtr);
        
    }
}
