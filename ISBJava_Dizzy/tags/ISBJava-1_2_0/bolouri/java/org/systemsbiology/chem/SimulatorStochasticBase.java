package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import java.util.*;

public abstract class SimulatorStochasticBase extends Simulator
{
    public static final int DEFAULT_ENSEMBLE_SIZE = 1;
    private static final int NUM_ITERATIONS_CHECK_CANCELLED = 1000;

    protected Random mRandomNumberGenerator;

    protected void setRandomNumberGenerator(Random pRandomNumberGenerator)
    {
        mRandomNumberGenerator = pRandomNumberGenerator;
    }

    protected Random getRandomNumberGenerator()
    {
        return(mRandomNumberGenerator);
    }

    protected static final double getRandomNumberUniformInterval(Random pRandomNumberGenerator)
    {
        return( 1.0 - pRandomNumberGenerator.nextDouble() );
    }

    protected void initializeRandomNumberGenerator()
    {
        setRandomNumberGenerator(new Random(System.currentTimeMillis()));
    }

    protected void checkDynamicalSymbolsInitialValues() throws InvalidInputException
    {
        int numDynamicalSymbols = mInitialDynamicSymbolValues.length;
        for(int ctr = 0; ctr < numDynamicalSymbols; ++ctr)
        {
            double initialValue = mInitialDynamicSymbolValues[ctr];
            if(initialValue > 1.0 && (initialValue - 1.0 == initialValue))
            {
                throw new InvalidInputException("initial species population value for species is too large for the stochastic Simulator");
            }
        }
    }

    protected static final int getNextDelayedReactionIndex(DelayedReactionSolver []pDelayedReactionSolvers)
    {
        int numDelayedReactions = pDelayedReactionSolvers.length;
        int nextReactionSolver = -1;
        double nextReactionTime = Double.POSITIVE_INFINITY;
        for(int ctr = numDelayedReactions; --ctr >= 0; )
        {
            DelayedReactionSolver solver = pDelayedReactionSolvers[ctr];
            if(solver.canHaveReaction())
            {
                double specReactionTime = solver.peekNextReactionTime();
                if(specReactionTime < nextReactionTime)
                {
                    nextReactionTime = specReactionTime;
                    nextReactionSolver = ctr;
                }
            }
        }
        return(nextReactionSolver);
    }

    protected static final void updateSymbolValuesForReaction(SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                              Reaction pReaction,
                                                              double []pSymbolValues,
                                                              double pCurrentTime,
                                                              DelayedReactionSolver []pDelayedReactionSolvers) throws DataNotFoundException
    {
        int numDelayedReactions = pDelayedReactionSolvers.length;

        Species []reactantsSpecies = pReaction.getReactantsSpeciesArray();
        boolean []reactantsDynamic = pReaction.getReactantsDynamicArray();

        int []reactantsStoichiometry = pReaction.getReactantsStoichiometryArray();
        int numReactants = reactantsSpecies.length;
        for(int ctr = numReactants; --ctr >= 0; )
        {
            if(reactantsDynamic[ctr])
            {
                Species reactant = reactantsSpecies[ctr];
                Symbol reactantSymbol = reactant.getSymbol();
                int reactantIndex = reactantSymbol.getArrayIndex();
                assert (reactantIndex != Symbol.NULL_ARRAY_INDEX) : "null array index";
                pSymbolValues[reactantIndex] -= ((double) reactantsStoichiometry[ctr]);
            }
        }

        Species []productsSpecies = pReaction.getProductsSpeciesArray();
        boolean []productsDynamic = pReaction.getProductsDynamicArray();
        int []productsStoichiometry = pReaction.getProductsStoichiometryArray();
        int numProducts = productsSpecies.length;
        for(int ctr = numProducts; --ctr >= 0; )
        {
            if(productsDynamic[ctr])
            {
                Species product = productsSpecies[ctr];
                Symbol productSymbol = product.getSymbol();
                int productIndex = productSymbol.getArrayIndex();
                assert (productIndex != Symbol.NULL_ARRAY_INDEX) : "null array index";
                pSymbolValues[productIndex] += ((double) productsStoichiometry[ctr]);
                if(numDelayedReactions > 0)
                {
                    for(int mCtr = numDelayedReactions; --mCtr >= 0; )
                    {
                        DelayedReactionSolver solver = pDelayedReactionSolvers[mCtr];
                        if(solver.hasIntermedSpecies(product))
                        {
                            assert (productsStoichiometry[ctr] == 1) : "invalid stoichiometry for delayed reaction";
                            solver.addReactant(pSymbolEvaluator);
                        }
                    }
                }
            }
        }
    }

    protected static final double chooseDeltaTimeToNextReaction(Random pRandomNumberGenerator,
                                                                double pReactionProbability)
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(pRandomNumberGenerator);
        double inverseRandomNumberUniformInterval = 1.0 / randomNumberUniformInterval;
        assert (randomNumberUniformInterval >= 0.0) : ("randomNumberUniformInterval: " + randomNumberUniformInterval);
        double logInverseRandomNumberUniformInterval = Math.log(inverseRandomNumberUniformInterval);
        double timeConstant = 1.0 / pReactionProbability;

        double deltaTime = timeConstant * logInverseRandomNumberUniformInterval;
        return(deltaTime);
    }

    public SimulatorParameters getDefaultSimulatorParameters()
    {
        SimulatorParameters sp = new SimulatorParameters();
        sp.setEnsembleSize(DEFAULT_ENSEMBLE_SIZE);
        return(sp);
    }

    public boolean isStochasticSimulator()
    {
        return(true);
    }

    protected abstract double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                      SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                      double pEndTime,
                                      Reaction []pReactions,
                                      double []pReactionProbabilities,
                                      Random pRandomNumberGenerator,
                                      double []pDynamicSymbolValues,
                                      MutableInteger pLastReactionIndex,
                                      DelayedReactionSolver []pDelayedReactionSolvers) throws DataNotFoundException, IllegalStateException;

    protected abstract void prepareForStochasticSimulation(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                                           SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                           double pStartTime,
                                                           Random pRandomNumberGenerator,
                                                           Reaction []pReactions,
                                                           double []pReactionProbabilities) throws DataNotFoundException;
   
    public final void simulate(double pStartTime, 
                               double pEndTime,
                               SimulatorParameters pSimulatorParameters,
                               int pNumResultsTimePoints,
                               String []pRequestedSymbolNames,
                               double []pRetTimeValues,
                               Object []pRetSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException
    {
        conductPreSimulationCheck(pStartTime,
                                  pEndTime,
                                  pSimulatorParameters,
                                  pNumResultsTimePoints,
                                  pRequestedSymbolNames,
                                  pRetTimeValues,
                                  pRetSymbolValues);

        double []timesArray = createTimesArray(pStartTime, 
                                               pEndTime,
                                               pNumResultsTimePoints);

        Symbol []requestedSymbols = createRequestedSymbolArray(mSymbolMap,
                                                               pRequestedSymbolNames);
        int numRequestedSymbols = requestedSymbols.length;

        SpeciesRateFactorEvaluator speciesRateFactorEvaluator = mSpeciesRateFactorEvaluator;
        SymbolEvaluatorChemSimulation symbolEvaluator = mSymbolEvaluator;
        double []reactionProbabilities = mReactionProbabilities;
        Reaction []reactions = mReactions;
        double []dynamicSymbolValues = mDynamicSymbolValues;        
        int numDynamicSymbolValues = dynamicSymbolValues.length;
        DelayedReactionSolver []delayedReactionSolvers = mDelayedReactionSolvers;

        Random randomNumberGenerator = mRandomNumberGenerator;

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


        boolean isCancelled = false;

        int timeCtr = 0;

        MutableInteger lastReactionIndex = new MutableInteger(NULL_REACTION);

        for(int simCtr = ensembleSize; --simCtr >= 0; )
        {
            timeCtr = 0;

            double time = pStartTime;
            prepareForSimulation(time);

            lastReactionIndex.setValue(NULL_REACTION);

            int iterationCtr = 0;

            prepareForStochasticSimulation(speciesRateFactorEvaluator,
                                           symbolEvaluator,
                                           pStartTime,
                                           randomNumberGenerator,
                                           reactions,
                                           reactionProbabilities);

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
                               delayedReactionSolvers);

                ++iterationCtr;
                if(0 == (iterationCtr % NUM_ITERATIONS_CHECK_CANCELLED))
                {
                    isCancelled = checkSimulationControllerStatus();
                    if(isCancelled)
                    {
                        break;
                    }
                }

                if(time >= timesArray[timeCtr])
                {
                    timeCtr = addRequestedSymbolValues(time,
                                                       timeCtr,
                                                       requestedSymbols,
                                                       symbolEvaluator,
                                                       timesArray,
                                                       pRetSymbolValues);
                }
            }
            
            if(isCancelled)
            {
                break;
            }

//            System.out.println("number of iterations: " + iterationCtr);

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
