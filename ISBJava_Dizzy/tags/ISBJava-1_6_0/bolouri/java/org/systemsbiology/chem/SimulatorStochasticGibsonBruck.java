package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import edu.cornell.lassp.houle.RngPack.*;

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
public final class SimulatorStochasticGibsonBruck extends SimulatorStochasticBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "gibson-bruck"; 
    private static final long NUMBER_FIRINGS = 1;

    private Object []mReactionDependencies;
    private IndexedPriorityQueue mPutativeTimeToNextReactions;


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
        HashMap speciesReactions = new HashMap();  // create a map between species names and the set of reactions
                                                   // whose rates depend on this species
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

        SymbolEvaluationPostProcessor symbolEvaluationPostProcessor = mSymbolEvaluator.getSymbolEvaluationPostProcessor();
        if(null != symbolEvaluationPostProcessor &&
           (symbolEvaluationPostProcessor instanceof SymbolEvaluationPostProcessorChemMarkupLanguage))
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
            if(reaction.getRate().isExpression() || reaction.getReactantsMap().size() == 0)
            {
                customReactions.add(new Integer(ctr));
                continue;
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
                if(null != dependentReactions)
                {
                    reactionDependencies[ctr].addAll(dependentReactions);
                }
            }

            Collection reactantSpecies = reaction.getReactantsMap().keySet();
            Iterator reactantIter = reactantSpecies.iterator();
            while(reactantIter.hasNext())
            {
                String reactantSpeciesName = (String) reactantIter.next();
                HashSet dependentReactions = (HashSet) speciesReactions.get(reactantSpeciesName);
                if(null != dependentReactions)
                {
                    reactionDependencies[ctr].addAll(dependentReactions);
                }
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

    private static double computeTimeToNextReaction(double []pReactionProbabilities,
                                                    int pIndex,
                                                    double pTime,
                                                    RandomElement pRandomNumberGenerator)
    {
        double probRate = pReactionProbabilities[pIndex];
        double timeOfNextReaction = 0.0;
        if(0.0 < probRate)
        {
            timeOfNextReaction = pTime + chooseDeltaTimeToNextReaction(pRandomNumberGenerator,
                                                                       pReactionProbabilities[pIndex]);
        }
        else
        {
            timeOfNextReaction = Double.POSITIVE_INFINITY;
        }

        return(timeOfNextReaction);
    }
         

    private void computePutativeTimeToNextReactions(RandomElement pRandomNumberGenerator,
                                                    double pStartTime,
                                                    double []pReactionProbabilities,
                                                    Reaction []pReactions)
    {
        int numReactions = pReactions.length;
        IndexedPriorityQueue putativeTimeToNextReactions = mPutativeTimeToNextReactions;
        putativeTimeToNextReactions.clear();
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            double timeToNextReaction = computeTimeToNextReaction(pReactionProbabilities,
                                                                  ctr,
                                                                  pStartTime,
                                                                  pRandomNumberGenerator);
            MutableDouble storeTimeToNextReaction = new MutableDouble(timeToNextReaction);
            putativeTimeToNextReactions.offer(storeTimeToNextReaction);
        }
    }

    private static void updateReactionRateAndTime(SymbolEvaluatorChem pSymbolEvaluator,
                                                  IndexedPriorityQueue pPutativeTimeToNextReactions,
                                                  int pReactionIndex,
                                                  Reaction []pReactions,
                                                  double []pReactionProbabilities,
                                                  RandomElement pRandomNumberGenerator,
                                                  int pLastReactionIndex) throws DataNotFoundException
    {
        double time = pSymbolEvaluator.getTime();
        MutableDouble timeOfNextReactionEventObj = (MutableDouble) pPutativeTimeToNextReactions.get(pReactionIndex);
        double oldTimeOfNextReactionEvent = timeOfNextReactionEventObj.getValue();
        double oldRate = pReactionProbabilities[pReactionIndex];
        Reaction reaction = pReactions[pReactionIndex];
        double newRate = reaction.computeRate(pSymbolEvaluator);
        double newTimeOfNextReactionEvent = 0.0;
        if(pLastReactionIndex != pReactionIndex)
        {
            if(newRate > 0.0 && oldRate > 0.0)
            {
                newTimeOfNextReactionEvent = (oldTimeOfNextReactionEvent - time)*oldRate/newRate + time;
            }
            else
            {
                if(newRate > 0.0)
                {
                    newTimeOfNextReactionEvent = time + chooseDeltaTimeToNextReaction(pRandomNumberGenerator, newRate);
                }
                else
                {
                    newTimeOfNextReactionEvent = Double.POSITIVE_INFINITY;
                }
            }
        }
        else
        {
            newTimeOfNextReactionEvent = time + chooseDeltaTimeToNextReaction(pRandomNumberGenerator, newRate);
        }

        timeOfNextReactionEventObj.setValue(newTimeOfNextReactionEvent);
        pPutativeTimeToNextReactions.update(pReactionIndex, timeOfNextReactionEventObj);
        pReactionProbabilities[pReactionIndex] = newRate;
    }
                                                 

    protected void prepareForStochasticSimulation(SymbolEvaluatorChem pSymbolEvaluator,
                                                  double pStartTime,
                                                  RandomElement pRandomNumberGenerator,
                                                  Reaction []pReactions,
                                                  double []pReactionProbabilities,
                                                  SimulatorParameters pSimulatorParameters) throws DataNotFoundException
    {
        computeReactionProbabilities(pSymbolEvaluator,
                                     pReactionProbabilities,
                                     pReactions);

        computePutativeTimeToNextReactions(pRandomNumberGenerator,
                                           pStartTime,
                                           pReactionProbabilities,
                                           pReactions);
    }

    protected double iterate(SymbolEvaluatorChem pSymbolEvaluator,
                             double pEndTime,
                             Reaction []pReactions,
                             double []pReactionProbabilities,
                             RandomElement pRandomNumberGenerator,
                             double []pDynamicSymbolValues,
                             MutableInteger pLastReactionIndex,
                             DelayedReactionSolver []pDelayedReactionSolvers) throws DataNotFoundException, IllegalStateException
    {
        IndexedPriorityQueue putativeTimeToNextReactions = mPutativeTimeToNextReactions;
        Object []reactionDependencies = mReactionDependencies;

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
            clearExpressionValueCaches();
                                          

            Integer []dependentReactions = (Integer []) reactionDependencies[lastReactionIndex];
            int numDependentReactions = dependentReactions.length;
            for(int ctr = numDependentReactions; --ctr >= 0; )
            {
                Integer dependentReactionCtrObj = dependentReactions[ctr];
                int dependentReactionCtr = dependentReactionCtrObj.intValue();

                updateReactionRateAndTime(pSymbolEvaluator,
                                          putativeTimeToNextReactions,
                                          dependentReactionCtr,
                                          pReactions,
                                          pReactionProbabilities,
                                          pRandomNumberGenerator,
                                          lastReactionIndex);

//                System.out.println("dependent reaction: " + dependentReaction + "; old time: " + oldPutativeTimeToNextReaction + "; new time: " + newPutativeTimeToNextReaction);
            }
        }

        int reactionIndex = putativeTimeToNextReactions.peekIndex();
        MutableDouble timeOfNextReactionObj = (MutableDouble) putativeTimeToNextReactions.get(reactionIndex);
        double timeOfNextReaction = timeOfNextReactionObj.getValue();

        if(0 == pDelayedReactionSolvers.length)
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
                if(nextDelayedReactionTime < timeOfNextReaction)
                {
                    // execute delayed reaction
                    timeOfNextReaction = nextDelayedReactionTime;
                    reactionIndex = solver.getReactionIndex();
//                    System.out.println("delayed reaction selected: " + pReactions[reactionIndex]);
                    solver.pollNextReactionTime();
                }
            }
        }

        pLastReactionIndex.setValue(reactionIndex);
        Reaction nextReaction = pReactions[reactionIndex];

        double newTime = timeOfNextReaction;

//        System.out.println("reaction selected: " + nextReaction);
//        System.out.println("time after reaction: " + newTime);

        if(newTime < pEndTime)
        {
            // set time to new time value
            time = newTime;
        }
        else
        {
            time = pEndTime;
//            System.out.println("end of simulation");
        }
        pSymbolEvaluator.setTime(time);

        return(time);
    }

    public void initialize(Model pModel) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel);
        initializeSimulatorStochastic(pModel);
        createDependencyGraph(pModel);
        initializePutativeTimeToNextReactions();
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
    

