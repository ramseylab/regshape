package org.systemsbiology.chem;

import org.systemsbiology.math.Value;
import org.systemsbiology.math.Symbol;
import org.systemsbiology.math.SymbolEvaluator;
import org.systemsbiology.math.MathFunctions;
import org.systemsbiology.math.MutableDouble;
import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.InvalidInputException;
import org.systemsbiology.util.IAliasableClass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Simulates the dynamics of a set of coupled chemical reactions
 * described by {@link Reaction} objects using the Gillespie stochastic
 * algorithm.
 *
 * @author Stephen Ramsey
 */
public class GillespieSimulator extends Simulator implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "gillespie-direct"; 

    private Random mRandomNumberGenerator;


    private void setRandomNumberGenerator(Random pRandomNumberGenerator)
    {
        mRandomNumberGenerator = pRandomNumberGenerator;
    }

    private Random getRandomNumberGenerator()
    {
        return(mRandomNumberGenerator);
    }

    private static final double getRandomNumberUniformInterval(Random pRandomNumberGenerator)
    {
        return( 1.0 - pRandomNumberGenerator.nextDouble() );
    }

    private static final double chooseDeltaTimeToNextReaction(Random pRandomNumberGenerator,
                                                              double pAggregateReactionProbability)
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(pRandomNumberGenerator);
        double inverseRandomNumberUniformInterval = 1.0 / randomNumberUniformInterval;
        assert (randomNumberUniformInterval >= 0.0) : ("randomNumberUniformInterval: " + randomNumberUniformInterval);
        double logInverseRandomNumberUniformInterval = Math.log(inverseRandomNumberUniformInterval);
        double timeConstant = 1.0 / pAggregateReactionProbability;

        double deltaTime = timeConstant * logInverseRandomNumberUniformInterval;
        return(deltaTime);
    }

    private static final Reaction chooseTypeOfNextReaction(Random pRandomNumberGenerator,
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
        Reaction reaction = null;
        for(int reactionCtr = numReactions - 1; reactionCtr >= 0; --reactionCtr)
        {
            double reactionProbability = pReactionProbabilities[reactionCtr];
            reaction = pReactions[reactionCtr];
            cumulativeReactionProbabilityDensity += reactionProbability;
            if(cumulativeReactionProbabilityDensity >= fractionOfAggregateReactionProbabilityDensity)
            {
                break;
            }
        }
        assert (null != reaction) : "null reaction found in chooseTypeOfNextReaction";
        return(reaction);
    }

    private void checkDynamicalSymbolsInitialValues() throws InvalidInputException
    {
        int numDynamicalSymbols = mInitialDynamicSymbolValues.length;
        for(int ctr = 0; ctr < numDynamicalSymbols; ++ctr)
        {
            double initialValue = mInitialDynamicSymbolValues[ctr];
            if(initialValue > 1.0 && (initialValue - 1.0 == initialValue))
            {
                throw new InvalidInputException("initial species population value for species is too large for the Gillespie Simulator");
            }
        }
    }


                                                    

    private static final double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                        SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                        double pEndTime,
                                        Reaction []pReactions,
                                        double []pReactionProbabilities,
                                        Random pRandomNumberGenerator,
                                        double []pDynamicSymbolValues,
                                        double []pNewDynamicSymbolValues) throws DataNotFoundException, IllegalStateException
    {
        double time = pSymbolEvaluator.getTime();

        // loop through all reactions, and for each reaction, compute the reaction probability
        int numReactions = pReactions.length;
        
        double aggregateReactionProbability = computeReactionProbabilities(pSpeciesRateFactorEvaluator,
                                                                           pSymbolEvaluator,
                                                                           pReactionProbabilities,
                                                                           pReactions);
        // if the aggregate reaction probability is 0.0, no more reactions can happen
        if(aggregateReactionProbability > 0.0)
        {

            // choose time of next reaction
            double deltaTimeToNextReaction = chooseDeltaTimeToNextReaction(pRandomNumberGenerator, 
                                                                           aggregateReactionProbability);

            
            // choose type of next reaction
            Reaction reaction = chooseTypeOfNextReaction(pRandomNumberGenerator,
                                                         aggregateReactionProbability,
                                                         pReactions,
                                                         pReactionProbabilities);
            
            
            double []dynamicSymbolAdjustmentVector = reaction.getDynamicSymbolAdjustmentVector();
            
            MathFunctions.vectorAdd(pDynamicSymbolValues, dynamicSymbolAdjustmentVector, pNewDynamicSymbolValues);
            
            MathFunctions.vectorZeroNegativeElements(pDynamicSymbolValues);
            
            time += deltaTimeToNextReaction;
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
        setRandomNumberGenerator(new Random(System.currentTimeMillis()));
    }


    public final void simulate(double pStartTime, 
                               double pEndTime,
                               int pNumTimePoints,
                               int pNumSteps,
                               String []pRequestedSymbolNames,
                               double []pRetTimeValues,
                               Object []pRetSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException
    {
        if(! mInitialized)
        {
            throw new IllegalStateException("simulator not initialized yet");
        }

        if(pNumSteps <= 0)
        {
            throw new IllegalArgumentException("illegal value for number of steps");
        }

        if(pNumTimePoints <= 0)
        {
            throw new IllegalArgumentException("number of time points must be nonnegative");
        }

        if(pStartTime > pEndTime)
        {
            throw new IllegalArgumentException("end time must come after start time");
        }
        
        if(pRetTimeValues.length != pNumTimePoints)
        {
            throw new IllegalArgumentException("illegal length of pRetTimeValues array");
        }

        if(pRetSymbolValues.length != pNumTimePoints)
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

        double []timesArray = new double[pNumTimePoints];

        prepareTimesArray(pStartTime, 
                          pEndTime,
                          pNumTimePoints,
                          timesArray);        

        Symbol []requestedSymbols = prepareRequestedSymbolArray(symbolMap,
                                                                pRequestedSymbolNames);

        int numRequestedSymbols = requestedSymbols.length;

        double []newSimulationSymbolValues = new double[numDynamicSymbolValues];

        boolean isCancelled = false;

        int timeCtr = 0;

        for(int simCtr = pNumSteps; --simCtr >= 0; )
        {
            timeCtr = 0;

            double time = pStartTime;
            prepareForSimulation(time);

            // set "last" values for dynamic symbols to be same as initial values
            System.arraycopy(dynamicSymbolValues, 0, newSimulationSymbolValues, 0, numDynamicSymbolValues);

//            int numIterations = 0;

            while(pNumTimePoints - timeCtr > 0)
            {
                time = iterate(speciesRateFactorEvaluator,
                               symbolEvaluator,
                               pEndTime,
                               reactions,
                               reactionProbabilities,
                               randomNumberGenerator,
                               dynamicSymbolValues,
                               newSimulationSymbolValues);
                
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

                System.arraycopy(newSimulationSymbolValues, 0, dynamicSymbolValues, 0, numDynamicSymbolValues);
            }
            
            if(isCancelled)
            {
                break;
            }

//            System.out.println("number of iterations: " + numIterations);

        }

        double ensembleMult = 1.0 / ((double) pNumSteps);

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
