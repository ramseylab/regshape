package org.systemsbiology.chem;

import java.util.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;

/**
 * Implementation of the Gibson-Bruck "Next Reaction" algorithm.
 *
 * Note:  If the Model contains M reactions with custom reaction
 * rate expressions, the Gibson algorithm will require M^2 memory
 * because each of the M reactions will need to be recomputed when
 * any of the other reactions occurs.  
 *
 * @author Stephen Ramsey
 */
public class GibsonSimulator extends StochasticSimulator implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "gibson-bruck"; 

    private Object []mReactionDependencies;
    private IndexedPriorityQueue mPutativeTimeToNextReactions;

    public void initialize(Model pModel, SimulationController pSimulationController) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel, pSimulationController);
        checkDynamicalSymbolsInitialValues();
        initializeRandomNumberGenerator();
        createDependencyGraph(pModel);
        initializePutativeTimeToNextReactions();
    }

    private void initializePutativeTimeToNextReactions()
    {
        IndexedPriorityQueue putativeTimeToNextReactions = new IndexedPriorityQueue(new AbstractComparator() 
                         {
                             public int compare(Object p1, Object p2)
                             {
                                 return(MutableDouble.compare((MutableDouble) p1, (MutableDouble) p2));
                             }
                         } );
        mPutativeTimeToNextReactions = putativeTimeToNextReactions;
    }

    private void createDependencyGraph(Model pModel) throws DataNotFoundException
    {
        Reaction []reactions = mReactions;
        int numReactions = reactions.length;

        // for each species, obtain the set of all reactions that contain the given species
        // as a reactant
        String []speciesArray = mDynamicSymbolNames;
        int numSpecies = speciesArray.length;
        HashMap speciesReactions = new HashMap();
        for(int ctr = 0; ctr < numSpecies; ++ctr)
        {
            String speciesName = speciesArray[ctr];
            speciesReactions.put(speciesName, new HashSet());
            // find all reactions that contain this species
            for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
            {
                Reaction reaction = reactions[reactionCtr];
                if(reaction.containsReactant(speciesName))
                {
                    ((HashSet) speciesReactions.get(speciesName)).add(new Integer(reactionCtr));
                }
            }
        }

        boolean checkCompartmentValues = false;
        if(pModel.getSpeciesRateFactorEvaluator() instanceof SpeciesRateFactorEvaluatorConcentration)
        {
            checkCompartmentValues = true;
        }

        HashSet customSpecies = new HashSet();
        for(int ctr = 0; ctr < numSpecies; ++ctr)
        {
            String speciesName = speciesArray[ctr];
            Symbol speciesSymbol = (Symbol) mSymbolMap.get(speciesName);
            if(null != speciesSymbol)
            {
                boolean addSpeciesToCustomList = false;
                if(null != speciesSymbol.getValueArray())
                {
                    // this is a boundary species with a custom population expression
                    addSpeciesToCustomList = true;
                }
                else
                {
                    if(checkCompartmentValues)
                    {
                        Compartment compartment = pModel.getSpeciesByName(speciesName).getCompartment();
                        if(compartment.getValue().isExpression())
                        {
                            addSpeciesToCustomList = true;
                        }
                    }
                }
                if(addSpeciesToCustomList)
                {
                    customSpecies.add(speciesName);
                }
            }
        }

        HashSet customReactions = new HashSet();
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            Reaction reaction = reactions[ctr];
            if(reaction.getRate().isExpression() || reaction.getReactantsMap().size() == 0
                || reaction.getNumSteps() > 1)
            {
                customReactions.add(new Integer(ctr));
            }

            HashMap reactantsMap = reaction.getReactantsMap();
            Collection reactantSpecies = reactantsMap.keySet();
            Iterator reactantIter = reactantSpecies.iterator();
            while(reactantIter.hasNext())
            {
                String reactantSpeciesName = (String) reactantIter.next();
                if(customSpecies.contains(reactantSpeciesName))
                {
                    customReactions.add(new Integer(ctr));
                }
            }
        }

        HashSet []reactionDependencies = new HashSet[numReactions];
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            reactionDependencies[ctr] = new HashSet();
            // go through the list of products for this reaction
            
            Reaction reaction = reactions[ctr];
            Collection productSpecies = reaction.getProductsMap().keySet();
            Iterator productIter = productSpecies.iterator();
            while(productIter.hasNext())
            {
                String productSpeciesName = (String) productIter.next();
                HashSet dependentReactions = (HashSet) speciesReactions.get(productSpeciesName);
                reactionDependencies[ctr].addAll(dependentReactions);
            }

            Collection reactantSpecies = reaction.getReactantsMap().keySet();
            Iterator reactantIter = reactantSpecies.iterator();
            while(reactantIter.hasNext())
            {
                String reactantSpeciesName = (String) reactantIter.next();
                HashSet dependentReactions = (HashSet) speciesReactions.get(reactantSpeciesName);
                reactionDependencies[ctr].addAll(dependentReactions);
            }

            reactionDependencies[ctr].addAll(customReactions);
        }

        mReactionDependencies = new Object[numReactions];
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            HashSet dependentReactions = reactionDependencies[ctr];
            Integer []fakeArray = new Integer[0];
            mReactionDependencies[ctr] = (Integer []) dependentReactions.toArray(fakeArray);
        }
    }

    private final void computePutativeTimeToNextReactions(Random pRandomNumberGenerator,
                                                          double pStartTime,
                                                          double []pReactionProbabilities,
                                                          Reaction []pReactions)
    {
        int numReactions = pReactions.length;
        IndexedPriorityQueue putativeTimeToNextReactions = mPutativeTimeToNextReactions;
        putativeTimeToNextReactions.clear();
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            double reactionProbability = pReactionProbabilities[ctr];
            double timeToNextReaction = 0.0;
            if(0.0 < reactionProbability)
            {
                timeToNextReaction = pStartTime + chooseDeltaTimeToNextReaction(pRandomNumberGenerator,
                                                                                reactionProbability);
            }
            else
            {
                timeToNextReaction = Double.POSITIVE_INFINITY;
            }
            MutableDouble storeTimeToNextReaction = new MutableDouble(timeToNextReaction);
            putativeTimeToNextReactions.offer(storeTimeToNextReaction);
        }
    }

    private static final double iterate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                        SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                        double pEndTime,
                                        Reaction []pReactions,
                                        double []pReactionProbabilities,
                                        Random pRandomNumberGenerator,
                                        double []pDynamicSymbolValues,
                                        MutableInteger pLastReactionIndex,
                                        IndexedPriorityQueue pPutativeTimeToNextReactions,
                                        Object []pReactionDependencies,
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
                                          

            Integer []dependentReactions = (Integer []) pReactionDependencies[lastReactionIndex];
            int numDependentReactions = dependentReactions.length;
            for(int ctr = numDependentReactions; --ctr >= 0; )
            {
                Integer dependentReactionCtrObj = dependentReactions[ctr];
                int dependentReactionCtr = dependentReactionCtrObj.intValue();
                Reaction dependentReaction = pReactions[dependentReactionCtr];

                double oldProbability = pReactionProbabilities[dependentReactionCtr];
                // compute reaction probability
                double newProbability = dependentReaction.computeRate(pSpeciesRateFactorEvaluator,
                                                                      pSymbolEvaluator);

                pReactionProbabilities[dependentReactionCtr] = newProbability;
                MutableDouble dependentReactionPutativeTime = (MutableDouble) pPutativeTimeToNextReactions.get(dependentReactionCtr);
                // compute or adjust putative time for reaction
                double oldPutativeTimeToNextReaction = dependentReactionPutativeTime.doubleValue();
                double newPutativeTimeToNextReaction = 0.0;
                if(dependentReactionCtr != lastReactionIndex)
                {
                    if(newProbability > 0.0 && oldProbability > 0.0)
                    {
//                        System.out.println("rescaling time to next reaction");
                        newPutativeTimeToNextReaction = ((oldPutativeTimeToNextReaction - time)*oldProbability/newProbability) + time;
                    }
                    else
                    {
                        if(newProbability == 0.0)
                        {
                            newPutativeTimeToNextReaction = Double.POSITIVE_INFINITY;
                        }
                        else
                        {
//                            System.out.println("generating new time to next reaction");
                            newPutativeTimeToNextReaction = time + chooseDeltaTimeToNextReaction(pRandomNumberGenerator,
                                                                                                 newProbability);
                        }
                    }
                }
                else
                {
                    newPutativeTimeToNextReaction = time + chooseDeltaTimeToNextReaction(pRandomNumberGenerator,
                                                                                         newProbability);
                }
//                System.out.println("dependent reaction: " + dependentReaction + "; old time: " + oldPutativeTimeToNextReaction + "; new time: " + newPutativeTimeToNextReaction);
                dependentReactionPutativeTime.setValue(newPutativeTimeToNextReaction);
                pPutativeTimeToNextReactions.update(dependentReactionCtr, dependentReactionPutativeTime);
                assert ( ((MutableDouble) pPutativeTimeToNextReactions.get(dependentReactionCtr)).getValue() == newPutativeTimeToNextReaction) : "inconsistent putative reaction time";
            }
        }

        int indexNextReaction = pPutativeTimeToNextReactions.peekIndex();
        pLastReactionIndex.setValue(indexNextReaction);
        Reaction nextReaction = pReactions[indexNextReaction];

        MutableDouble timeOfNextReaction = (MutableDouble) pPutativeTimeToNextReactions.get(indexNextReaction);

        double newTime = timeOfNextReaction.doubleValue();
        if(newTime < pEndTime)
        {
            // set time to new time value
            pSymbolEvaluator.setTime(newTime);
            time = newTime;
//            System.out.println("reaction selected: " + nextReaction);
//            System.out.println("time after reaction: " + newTime);
        }
        else
        {
            time = pEndTime;
//            System.out.println("end of simulation");
        }

        return(time);
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
        Object []reactionDependencies = mReactionDependencies;

        double []timesArray = new double[pNumTimePoints];

        prepareTimesArray(pStartTime, 
                          pEndTime,
                          pNumTimePoints,
                          timesArray);        

        Symbol []requestedSymbols = prepareRequestedSymbolArray(symbolMap,
                                                                pRequestedSymbolNames);

        int numRequestedSymbols = requestedSymbols.length;

        boolean isCancelled = false;

        int timeCtr = 0;

        IndexedPriorityQueue putativeTimeToNextReactions = mPutativeTimeToNextReactions;

        MutableInteger lastReactionIndex = new MutableInteger(NULL_REACTION);

        MultistepReactionSolver []multistepReactionSolvers = mMultistepReactionSolvers;

        for(int simCtr = pNumSteps; --simCtr >= 0; )
        {
            timeCtr = 0;

            double time = pStartTime;
            prepareForSimulation(time);
            lastReactionIndex.setValue(NULL_REACTION);

            computeReactionProbabilities(speciesRateFactorEvaluator,
                                         symbolEvaluator,
                                         reactionProbabilities,
                                         reactions);

            computePutativeTimeToNextReactions(randomNumberGenerator,
                                               pStartTime,
                                               reactionProbabilities,
                                               reactions);

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
                               lastReactionIndex,
                               putativeTimeToNextReactions,
                               reactionDependencies,
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
    

