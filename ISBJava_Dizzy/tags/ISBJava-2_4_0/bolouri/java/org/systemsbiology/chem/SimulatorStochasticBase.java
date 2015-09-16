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
import edu.cornell.lassp.houle.RngPack.*;
import cern.jet.random.*;

/**
 * Base class for all stochastic simulators.
 *
 * @author Stephen Ramsey
 */
public abstract class SimulatorStochasticBase extends Simulator
{
    public static final int DEFAULT_ENSEMBLE_SIZE = 1;
    public static final boolean DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS = false;
    public static final int DEFAULT_NUM_HISTORY_BINS = 400;

    protected RandomElement mRandomNumberGenerator;
    protected Poisson mPoissonEventGenerator;
    protected DelayedReactionSolver []mDynamicSymbolDelayedReactionAssociations;

    protected abstract void modifyDefaultSimulatorParameters(SimulatorParameters pSimulatorParameters);

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

    protected void initializePoissonEventGenerator()
    {
        mPoissonEventGenerator = new Poisson(1.0, mRandomNumberGenerator);
    }

    protected void checkDynamicalSymbolsValues(boolean pSimulationIsRunning, SymbolEvaluatorChem pSymbolEvaluator) throws AccuracyException
    {
        int numDynamicalSymbols = mDynamicSymbolValues.length;
        for(int ctr = 0; ctr < numDynamicalSymbols; ++ctr)
        {
            double speciesValue = mDynamicSymbolValues[ctr];
            if(speciesValue > 1.0 && (speciesValue - 1.0 == speciesValue))
            {
                String timeStr = null;
                if(! pSimulationIsRunning)
                {
                    timeStr = "at the initial time";
                }
                else
                {
                    timeStr = "at time " + Double.toString(pSymbolEvaluator.getTime());
                }
                throw new AccuracyException(timeStr + ", the species value for species \"" + mDynamicSymbolNames[ctr] + "\" is too large for the stochastic Simulator: " + speciesValue);
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

    protected final void updateSymbolValuesForReaction(int pReactionCtr,
                                                       double []pDynamicSymbolValues,
                                                       DelayedReactionSolver []pDynamicSymbolDelayedReactionAssociations,
                                                       long pNumberFirings) throws DataNotFoundException
    {
        Symbol []speciesArray = (Symbol []) mReactionsReactantsSpecies[pReactionCtr];
        boolean []speciesDynamicFlagArray = (boolean []) mReactionsReactantsDynamic[pReactionCtr];
        int []speciesStoichiometryArray = (int []) mReactionsReactantsStoichiometries[pReactionCtr];
        int numSpecies = speciesArray.length;

        for(int ctr = numSpecies; --ctr >= 0; )
        {
            if(speciesDynamicFlagArray[ctr])
            {
                pDynamicSymbolValues[speciesArray[ctr].getArrayIndex()] -= ((double) (pNumberFirings*speciesStoichiometryArray[ctr]));
            }
        }

        speciesArray = (Symbol []) mReactionsProductsSpecies[pReactionCtr];
        speciesDynamicFlagArray = (boolean []) mReactionsProductsDynamic[pReactionCtr];
        speciesStoichiometryArray = (int []) mReactionsProductsStoichiometries[pReactionCtr];
        numSpecies = speciesArray.length;

        int speciesIndex;
        for(int ctr = numSpecies; --ctr >= 0; )
        {
            if(speciesDynamicFlagArray[ctr])
            {
                speciesIndex = speciesArray[ctr].getArrayIndex();

                pDynamicSymbolValues[speciesIndex] += ((double) (pNumberFirings*speciesStoichiometryArray[ctr]));

                if(null != pDynamicSymbolDelayedReactionAssociations)
                {
                    if(null != pDynamicSymbolDelayedReactionAssociations[speciesIndex])
                    {
                        DelayedReactionSolver solver = pDynamicSymbolDelayedReactionAssociations[speciesIndex];

                        for(long j = pNumberFirings; --j >= 0; )
                        {
//                            System.out.println("adding reactant \"" + mDynamicSymbols[speciesIndex].getName() + "\" for delayed reaction: " + mReactions[solver.getReactionIndex()].getName() + "\"");
                            solver.addReactant(mSymbolEvaluator);
                        }
                    }
                }
            }
        }
    }

    protected final double chooseDeltaTimeToNextReaction(double pReactionProbability)
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(mRandomNumberGenerator);
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

    protected abstract double iterate(MutableInteger pLastReactionIndex) throws DataNotFoundException, IllegalStateException, AccuracyException;

    protected abstract void prepareForStochasticSimulation(double pStartTime,
                                                           SimulatorParameters pSimulatorParameters) throws DataNotFoundException, IllegalArgumentException;
    

    protected static final int getPoissonEvent(Poisson pPoissonEventGenerator,
                                               double pMean)
    {
        boolean gotSuccessfulEvent = false;
        int retVal = 0;
        do
        {
            try
            {
                retVal = pPoissonEventGenerator.nextInt(pMean);
                gotSuccessfulEvent = true;
            }
            catch(ArrayIndexOutOfBoundsException e)
            {
                System.err.println("internal bug in cern.jet.random.Poisson tripped; this is being handled");
            }
        }
        while(! gotSuccessfulEvent);
        return(retVal);
    }

    private static final void integerizeInitialData(double []pDynamicSymbolValues,
                                                    Species []pDynamicSymbols,
                                                    RandomElement pRandomNumberGenerator)
    {
        int numSpecies = pDynamicSymbolValues.length;
        double speciesValue = 0.0;
        for(int i = 0; i < numSpecies; ++i)
        {
            speciesValue = pDynamicSymbolValues[i];
            double floorSpeciesValue = Math.floor(speciesValue);
            if(speciesValue > floorSpeciesValue)
            {
                double frac = speciesValue - floorSpeciesValue;
                double randVal = pRandomNumberGenerator.raw();
                double initialVal = floorSpeciesValue;
                if(randVal < frac)
                {
                    initialVal += 1.0;
                }
                pDynamicSymbolValues[i] = initialVal;
//                pDynamicSymbolValues[i] = getPoissonEvent(pPoissonEventGenerator, speciesValue);
//                System.out.println("setting initial value for species \"" + pDynamicSymbols[i].getName() + "\" to " + pDynamicSymbolValues[i]);
            }
        }
    }

    protected final void initializeSimulatorStochastic(Model pModel) throws InvalidInputException
    {
        try
        {
            boolean simulationIsRunning = false;
            SymbolEvaluatorChem symbolEvaluator = null;
            checkDynamicalSymbolsValues(simulationIsRunning, symbolEvaluator);
        }
        catch(AccuracyException e)
        {
            throw new InvalidInputException(e.getMessage(), e);
        }
        initializeRandomNumberGenerator();
        initializePoissonEventGenerator();

        if(null != mDelayedReactionSolvers)
        {
            int numDynamicSymbols = mDynamicSymbols.length;

            mDynamicSymbolDelayedReactionAssociations = new DelayedReactionSolver[numDynamicSymbols];
            for(int ctr = 0; ctr < numDynamicSymbols; ++ctr)
            {
                mDynamicSymbolDelayedReactionAssociations[ctr] = null;
            }
            
            int numDelayedReactions = mDelayedReactionSolvers.length;
            for(int ctr = 0; ctr < numDelayedReactions; ++ctr)
            {
                DelayedReactionSolver solver = mDelayedReactionSolvers[ctr];
                Species intermedSpecies = solver.getIntermedSpecies();
                String intermedSpeciesName = intermedSpecies.getName();
                Symbol intermedSpeciesSymbol = (Symbol) mSymbolMap.get(intermedSpeciesName);
                int intermedSpeciesIndex = intermedSpeciesSymbol.getArrayIndex();
                mDynamicSymbolDelayedReactionAssociations[intermedSpeciesIndex] = solver;
            }
        }
        else
        {
            mDynamicSymbolDelayedReactionAssociations = null;
        }
    }

    protected final int chooseIndexOfNextReaction(double pAggregateReactionProbabilityDensity) throws IllegalArgumentException
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(mRandomNumberGenerator);

        double cumulativeReactionProbabilityDensity = 0.0;

        double fractionOfAggregateReactionProbabilityDensity = randomNumberUniformInterval * pAggregateReactionProbabilityDensity;

        if(pAggregateReactionProbabilityDensity <= 0.0)
        {
            throw new IllegalArgumentException("invalid aggregate reaction probability density: " + pAggregateReactionProbabilityDensity);
        }

        int numReactions = mReactions.length;
        int reactionIndex = -1;
        for(int reactionCtr = numReactions - 1; reactionCtr >= 0; --reactionCtr)
        {
            double reactionProbability = mReactionProbabilities[reactionCtr];
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
                                            String []pRequestedSymbolNames) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, AccuracyException
    {
        checkSimulationParameters(pStartTime,
                                  pEndTime,
                                  pSimulatorParameters,
                                  pNumResultsTimePoints);

        // set the number of history bins for the delayed reaction solvers
        int numHistoryBins = pSimulatorParameters.getNumHistoryBins().intValue();
        if(null != mDelayedReactionSolvers)
        {
            resizeDelayedReactionSolvers(numHistoryBins);
        }

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
        Species []dynamicSymbols = mDynamicSymbols;
        int numDynamicSymbolValues = dynamicSymbolValues.length;
        DelayedReactionSolver []delayedReactionSolvers = mDelayedReactionSolvers;

        Value []nonDynamicSymbolValues = mNonDynamicSymbolValues;

        RandomElement randomNumberGenerator = mRandomNumberGenerator;
        Poisson poissonEventGenerator = mPoissonEventGenerator;
        
        int ensembleSize = pSimulatorParameters.getEnsembleSize().intValue();

        boolean isCancelled = false;

        int timePointIndex = 0;

        MutableInteger lastReactionIndex = new MutableInteger(NULL_REACTION);

        double ensembleMult = 1.0 / ((double) ensembleSize);
        double timeRangeMult = 1.0 / (pEndTime - pStartTime);
        double fractionComplete = 0.0;

        double time = 0.0;
        long currentTimeMilliseconds = 0;

        int simCtr = ensembleSize;

        Boolean getFinalSymbolFluctuationsObj = pSimulatorParameters.getComputeFluctuations();

        Object []finalSymbolValues = null;
        double []finalSymbolValuesElem = null;
        if(true == getFinalSymbolFluctuationsObj.booleanValue())
        {
            if(ensembleSize < 2)
            {
                throw new IllegalArgumentException("an ensemble size of greater than one is required, in order to compute the final species fluctuations");
            }
            
            finalSymbolValues = new Object[ensembleSize];
        }

        boolean simulationIsRunning = true;
        
        while( --simCtr >= 0 )
        {
            // time point index must be re-set to zero
            timePointIndex = 0;

            time = pStartTime;

            prepareForSimulation(pStartTime);

            lastReactionIndex.setValue(NULL_REACTION);

            integerizeInitialData(dynamicSymbolValues,
                                  dynamicSymbols,
                                  randomNumberGenerator);

            prepareForStochasticSimulation(pStartTime,
                                           pSimulatorParameters);

            while(pNumResultsTimePoints - timePointIndex > 0)
            {
                time = iterate(lastReactionIndex);
                if(time > pEndTime)
                {
                    time = pEndTime;
                    symbolEvaluator.setTime(pEndTime);
                }

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
                    checkDynamicalSymbolsValues(simulationIsRunning, symbolEvaluator);
                    timePointIndex = addRequestedSymbolValues(time,
                                                              timePointIndex,
                                                              requestedSymbols,
                                                              timesArray,
                                                              retSymbolValues);
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
                    finalSymbolValues[simCtr] = finalSymbolValuesElem;
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

    protected void checkSimulationParametersImpl(SimulatorParameters pSimulatorParameters,
                                                 int pNumResultsTimePoints)
    {
        Boolean flagGetFinalSymbolFluctuations = pSimulatorParameters.getComputeFluctuations();
        if(null == flagGetFinalSymbolFluctuations)
        {
            throw new IllegalArgumentException("missing flag for whether to obtain the final symbol fluctuations");
        }

        Integer ensembleSizeObj = pSimulatorParameters.getEnsembleSize();
        if(null == ensembleSizeObj)
        {
            throw new IllegalArgumentException("missing ensemble size");
        }
        int ensembleSize = ensembleSizeObj.intValue();
        if(ensembleSize <= 0)
        {
            throw new IllegalStateException("illegal ensemble size: " + ensembleSize);
        }

        if(hasDelayedReactionSolvers())
        {
            // validate the number of requested history bins
            Integer numHistoryBinsObj = pSimulatorParameters.getNumHistoryBins();
            if(null == numHistoryBinsObj)
            {
                throw new IllegalArgumentException("no number of history bins defined");
            }       
            int numHistoryBins = numHistoryBinsObj.intValue();
            if(numHistoryBins <= 0)
            {
                throw new IllegalArgumentException("invalid number of history bins: " + numHistoryBins);
            }
        }
    }

    public SimulatorParameters getDefaultSimulatorParameters()
    {
        SimulatorParameters sp = new SimulatorParameters();
        sp.setEnsembleSize(new Integer(DEFAULT_ENSEMBLE_SIZE));
        sp.setComputeFluctuations(DEFAULT_FLAG_GET_FINAL_SYMBOL_FLUCTUATIONS);
        sp.setNumHistoryBins(DEFAULT_NUM_HISTORY_BINS);
        modifyDefaultSimulatorParameters(sp);
        return(sp);
    }
}
