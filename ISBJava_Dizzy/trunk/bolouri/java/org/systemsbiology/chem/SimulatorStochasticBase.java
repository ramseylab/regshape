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

/**
 * Class for running a simulation from the command-line.
 *
 * @author Stephen Ramsey
 */
public abstract class SimulatorStochasticBase extends Simulator
{
    public static final int DEFAULT_ENSEMBLE_SIZE = 1;

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
                pSymbolValues[productIndex] += ((double) productsStoichiometry[ctr]);
                if(numDelayedReactions > 0)
                {
                    for(int i = numDelayedReactions; --i >= 0; )
                    {
                        DelayedReactionSolver solver = pDelayedReactionSolvers[i];
                        if(solver.hasIntermedSpecies(product))
                        {
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

        SimulationProgressReporter simulationProgressReporter = mSimulationProgressReporter;
        SimulationController simulationController = mSimulationController;

        boolean doUpdates = (null != simulationController || null != simulationProgressReporter);
            
        long minNumMillisecondsForUpdate = 0;

        long timeOfLastUpdateMilliseconds = 0;
        if(doUpdates)
        {
            minNumMillisecondsForUpdate = mMinNumMillisecondsForUpdate;
            timeOfLastUpdateMilliseconds = System.currentTimeMillis();
        }

        long iterationCounter = 0;

        if(null != simulationProgressReporter)
        {
            simulationProgressReporter.updateProgressStatistics(0.0, iterationCounter);
        }


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

        Long ensembleSizeObj = pSimulatorParameters.getEnsembleSize();
        if(null == ensembleSizeObj)
        {
            throw new IllegalArgumentException("ensemble size was not defined");
        }            
        long ensembleSize = ensembleSizeObj.longValue();
        if(ensembleSize <= 0)
        {
            throw new IllegalArgumentException("illegal value for ensemble size");
        }

        boolean isCancelled = false;

        int timePointIndex = 0;

        MutableInteger lastReactionIndex = new MutableInteger(NULL_REACTION);

        double ensembleMult = 1.0 / ((double) ensembleSize);
        double timeRangeMult = 1.0 / (pEndTime - pStartTime);
        double fractionComplete = 0.0;

        double time = 0.0;
        long currentTimeMilliseconds = 0;

        for(long simCtr = ensembleSize; --simCtr >= 0; )
        {
            // time point index must be re-set to zero
            timePointIndex = 0;

            time = pStartTime;

            prepareForSimulation(time);

            lastReactionIndex.setValue(NULL_REACTION);

            prepareForStochasticSimulation(speciesRateFactorEvaluator,
                                           symbolEvaluator,
                                           pStartTime,
                                           randomNumberGenerator,
                                           reactions,
                                           reactionProbabilities);

            while(pNumResultsTimePoints - timePointIndex > 0)
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

                ++iterationCounter;
                
                if(doUpdates)
                {
                    currentTimeMilliseconds = System.currentTimeMillis();
                    if(currentTimeMilliseconds - timeOfLastUpdateMilliseconds >= minNumMillisecondsForUpdate)
                    {
                        timeOfLastUpdateMilliseconds = currentTimeMilliseconds;
                        if(null != simulationProgressReporter)
                        {
                            fractionComplete = (((double) (ensembleSize - simCtr - 1)) + time*timeRangeMult)*ensembleMult;
                            simulationProgressReporter.updateProgressStatistics(fractionComplete, iterationCounter);
                        }

                        if(null != simulationController)
                        {
                            isCancelled = simulationController.handlePauseOrCancel();
                            if(isCancelled)
                            {
                                break;
                            }
                        }
                    }
                }

                if(time >= timesArray[timePointIndex])
                {
                    // at this point, the expression value caches may be stale; clear them
                    clearExpressionValueCaches();

                    timePointIndex = addRequestedSymbolValues(time,
                                                              timePointIndex,
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
        }


        for(int timePointCtr = timePointIndex; --timePointCtr >= 0; )
        {
            for(int symbolCtr = numRequestedSymbols; --symbolCtr >= 0; )
            {
                double []symbolValues = (double []) pRetSymbolValues[timePointCtr];
                symbolValues[symbolCtr] *= ensembleMult;
            }
        }

        // copy array of time points 
        System.arraycopy(timesArray, 0, pRetTimeValues, 0, timePointIndex);

        if(null != simulationProgressReporter)
        {
            simulationProgressReporter.updateProgressStatistics(1.0, iterationCounter);
        }
    }

    public boolean allowsInterrupt()
    {
        return(true);
    }

    public boolean usesExpressionValueCaching()
    {
        return(true);
    }

    protected void initializeSimulatorStochastic(Model pModel) throws InvalidInputException
    {
        checkDynamicalSymbolsInitialValues();
        initializeRandomNumberGenerator();
    }
}
