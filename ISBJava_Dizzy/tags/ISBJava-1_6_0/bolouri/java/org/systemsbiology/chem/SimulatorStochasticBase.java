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
import edu.cornell.lassp.houle.RngPack.*;

/**
 * Base class for all stochastic simulators.
 *
 * @author Stephen Ramsey
 */
public abstract class SimulatorStochasticBase extends Simulator
{
    public static final int DEFAULT_ENSEMBLE_SIZE = 1;
    public static final boolean DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS = false;

    protected RandomElement mRandomNumberGenerator;

    protected abstract void modifyDefaultSimulatorParameters(SimulatorParameters pSimulatorParameters);

    public SimulatorParameters getDefaultSimulatorParameters()
    {
        SimulatorParameters sp = new SimulatorParameters();
        sp.setEnsembleSize(DEFAULT_ENSEMBLE_SIZE);
        sp.setFlagGetFinalSymbolFluctuations(DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS);
        modifyDefaultSimulatorParameters(sp);
        return(sp);
    }

    protected void setRandomNumberGenerator(RandomElement pRandomNumberGenerator)
    {
        mRandomNumberGenerator = pRandomNumberGenerator;
    }

    protected RandomElement getRandomNumberGenerator()
    {
        return(mRandomNumberGenerator);
    }

    protected static final double getRandomNumberUniformInterval(RandomElement pRandomNumberGenerator)
    {
        return( 1.0 - pRandomNumberGenerator.raw() );
    }

    protected void initializeRandomNumberGenerator()
    {
        setRandomNumberGenerator(new Ranmar(System.currentTimeMillis()));
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

    protected static final void updateSymbolValuesForReaction(SymbolEvaluatorChem pSymbolEvaluator,
                                                              Reaction pReaction,
                                                              double []pSymbolValues,
                                                              DelayedReactionSolver []pDelayedReactionSolvers,
                                                              long pNumberFirings) throws DataNotFoundException
    {
        int numDelayedReactions = 0;
        if(null != pDelayedReactionSolvers)
        {
            numDelayedReactions = pDelayedReactionSolvers.length;
        }

        Species []speciesArray = pReaction.getReactantsSpeciesArray();
        boolean []speciesDynamicFlagArray = pReaction.getReactantsDynamicArray();
        int []speciesStoichiometryArray = pReaction.getReactantsStoichiometryArray();
        int numSpecies = speciesArray.length;

        Species species = null;
        int speciesIndex = -1;

        for(int ctr = numSpecies; --ctr >= 0; )
        {
            if(speciesDynamicFlagArray[ctr])
            {
                species = speciesArray[ctr];
                speciesIndex = species.getSymbol().getArrayIndex();
                pSymbolValues[speciesIndex] -= ((double) (pNumberFirings*speciesStoichiometryArray[ctr]));
            }
        }

        speciesArray = pReaction.getProductsSpeciesArray();
        speciesDynamicFlagArray = pReaction.getProductsDynamicArray();
        speciesStoichiometryArray = pReaction.getProductsStoichiometryArray();
        numSpecies = speciesArray.length;

        for(int ctr = numSpecies; --ctr >= 0; )
        {
            if(speciesDynamicFlagArray[ctr])
            {
                species = speciesArray[ctr];
                speciesIndex = species.getSymbol().getArrayIndex();
                pSymbolValues[speciesIndex] += ((double) (pNumberFirings*speciesStoichiometryArray[ctr]));
                for(int i = numDelayedReactions; --i >= 0; )
                {
                    DelayedReactionSolver solver = pDelayedReactionSolvers[i];
                    if(solver.hasIntermedSpecies(species))
                    {
                        for(long j = pNumberFirings; --j >= 0; )
                        {
                            solver.addReactant(pSymbolEvaluator);
                        }
                    }
                }
            }
        }
    }

    protected static final double chooseDeltaTimeToNextReaction(RandomElement pRandomNumberGenerator,
                                                                double pReactionProbability)
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(pRandomNumberGenerator);
        double inverseRandomNumberUniformInterval = 1.0 / randomNumberUniformInterval;
        double logInverseRandomNumberUniformInterval = Math.log(inverseRandomNumberUniformInterval);
        double timeConstant = 1.0 / pReactionProbability;

        double deltaTime = timeConstant * logInverseRandomNumberUniformInterval;
        return(deltaTime);
    }

    public boolean isStochasticSimulator()
    {
        return(true);
    }

    protected abstract double iterate(SymbolEvaluatorChem pSymbolEvaluator,
                                      double pEndTime,
                                      Reaction []pReactions,
                                      double []pReactionProbabilities,
                                      RandomElement pRandomNumberGenerator,
                                      double []pDynamicSymbolValues,
                                      MutableInteger pLastReactionIndex,
                                      DelayedReactionSolver []pDelayedReactionSolvers) throws DataNotFoundException, IllegalStateException, SimulationAccuracyException;

    protected abstract void prepareForStochasticSimulation(SymbolEvaluatorChem pSymbolEvaluator,
                                                           double pStartTime,
                                                           RandomElement pRandomNumberGenerator,
                                                           Reaction []pReactions,
                                                           double []pReactionProbabilities,
                                                           SimulatorParameters pSimulatorParameters) throws DataNotFoundException, IllegalArgumentException;

    protected void initializeSimulatorStochastic(Model pModel) throws InvalidInputException
    {
        checkDynamicalSymbolsInitialValues();
        initializeRandomNumberGenerator();
    }

    protected static final int chooseIndexOfNextReaction(RandomElement pRandomNumberGenerator,
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
        return(reactionIndex);
    }

   
    public final SimulationResults simulate(double pStartTime, 
                                            double pEndTime,
                                            SimulatorParameters pSimulatorParameters,
                                            int pNumResultsTimePoints,
                                            String []pRequestedSymbolNames) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, SimulationAccuracyException
    {
        System.err.println("starting stochastic simulation");
        conductPreSimulationCheck(pStartTime,
                                  pEndTime,
                                  pSimulatorParameters,
                                  pNumResultsTimePoints);

        double []retTimeValues = new double[pNumResultsTimePoints];
        Object []retSymbolValues = new Object[pNumResultsTimePoints];
        
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
            simulationProgressReporter.updateProgressStatistics(false, 0.0, iterationCounter);
        }

        double []timesArray = createTimesArray(pStartTime, 
                                               pEndTime,
                                               pNumResultsTimePoints);

        Symbol []requestedSymbols = createRequestedSymbolArray(mSymbolMap,
                                                               pRequestedSymbolNames);
        int numRequestedSymbols = requestedSymbols.length;

        SymbolEvaluatorChem symbolEvaluator = mSymbolEvaluator;
        double []reactionProbabilities = mReactionProbabilities;
        Reaction []reactions = mReactions;
        double []dynamicSymbolValues = mDynamicSymbolValues;        
        int numDynamicSymbolValues = dynamicSymbolValues.length;
        DelayedReactionSolver []delayedReactionSolvers = mDelayedReactionSolvers;

        RandomElement randomNumberGenerator = mRandomNumberGenerator;

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

        long simCtr = ensembleSize;

        Boolean getFinalSymbolFluctuationsObj = pSimulatorParameters.getFlagGetFinalSymbolFluctuations();

        Object []finalSymbolValues = null;
        double []finalSymbolValuesElem = null;
        if(null != getFinalSymbolFluctuationsObj && getFinalSymbolFluctuationsObj.booleanValue())
        {
            if(ensembleSize > Integer.MAX_VALUE)
            {
                throw new IllegalArgumentException("it is not possible to obtain final symbol fluctuations when the ensemble size is greater than: " + Integer.MAX_VALUE);
            }
            if(ensembleSize < 2)
            {
                throw new IllegalArgumentException("an ensemble size of greater than one is required, in order to compute the final species fluctuations");
            }
            
            finalSymbolValues = new Object[(int) ensembleSize];
        }

        while( --simCtr >= 0 )
        {
            // time point index must be re-set to zero
            timePointIndex = 0;

            time = pStartTime;

            prepareForSimulation(time);

            lastReactionIndex.setValue(NULL_REACTION);

            prepareForStochasticSimulation(symbolEvaluator,
                                           pStartTime,
                                           randomNumberGenerator,
                                           reactions,
                                           reactionProbabilities,
                                           pSimulatorParameters);

            while(pNumResultsTimePoints - timePointIndex > 0)
            {
//                System.out.println("calling iterate");
                time = iterate(symbolEvaluator,
                               pEndTime,
                               reactions,
                               reactionProbabilities,
                               randomNumberGenerator,
                               dynamicSymbolValues,
                               lastReactionIndex,
                               delayedReactionSolvers);
//                System.out.println("returne from iterate, time is: " + time);

                ++iterationCounter;
                
                if(doUpdates)
                {
                    currentTimeMilliseconds = System.currentTimeMillis();
                    if(currentTimeMilliseconds - timeOfLastUpdateMilliseconds >= minNumMillisecondsForUpdate)
                    {
                        if(null != simulationController)
                        {
                            isCancelled = simulationController.handlePauseOrCancel();
                            if(isCancelled)
                            {
                                break;
                            }
                        }

                        if(null != simulationProgressReporter)
                        {
                            fractionComplete = (((double) (ensembleSize - simCtr - 1)) + time*timeRangeMult)*ensembleMult;
                            simulationProgressReporter.updateProgressStatistics(false, fractionComplete, iterationCounter);
                        }

                        timeOfLastUpdateMilliseconds = System.currentTimeMillis();
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
                                                              retSymbolValues);
//                    System.out.println("time point index after iteration is: " + timePointIndex + "; time is: " + time);
                }

            }   // end of this particular simulation
            
            if(isCancelled)
            {
                break;
            }
            else
            {
                if(null != finalSymbolValues)
                {
                    finalSymbolValuesElem = new double[numRequestedSymbols];
                    finalSymbolValues[(int) simCtr] = finalSymbolValuesElem;
                    for(int i = numRequestedSymbols; --i >= 0; )
                    {
                        finalSymbolValuesElem[i] = symbolEvaluator.getValue(requestedSymbols[i]);
                    }
                }
            }

        } // end of the entire ensemble of simulations

        if(null != simulationProgressReporter)
        {
            fractionComplete = (((double) (ensembleSize - simCtr - 1)) + time*timeRangeMult)*ensembleMult;
            simulationProgressReporter.updateProgressStatistics(true, fractionComplete, iterationCounter);
        }

        SimulationResults simulationResults = null;

        if(! isCancelled)
        {
            // divide symbol values by ensemble size, to obtain ensemble average
            for(int timePointCtr = timePointIndex; --timePointCtr >= 0; )
            {
                double []symbolValues = (double []) retSymbolValues[timePointCtr];
                for(int symbolCtr = numRequestedSymbols; --symbolCtr >= 0; )
                {
                    symbolValues[symbolCtr] *= ensembleMult;
                }
            }
            
            double []retFinalSymbolFluctuations = null;
            if(null != finalSymbolValues)
            {
                retFinalSymbolFluctuations = new double[numRequestedSymbols];
                double []averageFinalSymbolValues = (double []) retSymbolValues[timePointIndex-1];
                double avg = 0.0;
                double stddev = 0.0;
                finalSymbolValuesElem = null;
                for(int i = 0; i < numRequestedSymbols; ++i)
                {
                    avg = averageFinalSymbolValues[i];
                    stddev = 0.0;
                    for(int j = 0; j < ensembleSize; ++j)
                    {
                        finalSymbolValuesElem = (double []) finalSymbolValues[j];
                        stddev += Math.pow(avg - finalSymbolValuesElem[i], 2.0);
                    }
                    retFinalSymbolFluctuations[i] = Math.sqrt(stddev/((double) (ensembleSize-1)));
                }
            }
            
            simulationResults = createSimulationResults(pStartTime,
                                                        pEndTime,
                                                        pSimulatorParameters,
                                                        pRequestedSymbolNames,
                                                        timesArray,
                                                        retSymbolValues,
                                                        retFinalSymbolFluctuations);
        }



        return(simulationResults);
    }

    public boolean allowsInterrupt()
    {
        return(true);
    }

    public boolean usesExpressionValueCaching()
    {
        return(true);
    }


}
