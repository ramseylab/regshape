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
    private Integer []mReactionsRecomputeAfterEachIteration;
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

    class ExpressionDependencyGetter implements Expression.IVisitor
    {
        private Collection mDependentSymbols;
        public ExpressionDependencyGetter(Collection pDependentSymbols)
        {
            setDependentSymbols(pDependentSymbols);
        }
        public void setDependentSymbols(Collection pDependentSymbols)
        {
            mDependentSymbols = pDependentSymbols;
        }
        public Collection getDependentSymbols()
        {
            return(mDependentSymbols);
        }
        public void visit(Symbol pSymbol)
        {
            Symbol indexedSymbol = (Symbol) mSymbolMap.get(pSymbol.getName());
            if(null != indexedSymbol)
            {
                if(null != indexedSymbol.getValueArray())
                {
                    Value symbolValue = mNonDynamicSymbolValues[indexedSymbol.getArrayIndex()];
                    if(symbolValue.isExpression())
                    {
                        symbolValue.getExpressionValue().visit(this);
                    }
                    else
                    {
                        // do nothing, it is a non-important dependence
                    }
                }
                else
                {
                    mDependentSymbols.add(pSymbol.getName());
                }
            }
            else
            {
                mDependentSymbols.add(pSymbol.getName());
            }
        }
        
    }

    private void processExpressionDependencies(Collection pDependentSymbolNames,
                                               HashMap pSpeciesReactions,
                                               boolean pTimeSymbolAllowed,
                                               HashSet pReactionsRecomputeAfterEachIteration,
                                               Integer pReactionIndex)
    {
        Iterator dependentSymbolNames = pDependentSymbolNames.iterator();
        HashSet speciesReactionDependencies = null;
        while(dependentSymbolNames.hasNext())
        {
            String dependentSymbolName = (String) dependentSymbolNames.next();
            speciesReactionDependencies = (HashSet) pSpeciesReactions.get(dependentSymbolName);
            if(null != speciesReactionDependencies)
            {
                if(! pReactionsRecomputeAfterEachIteration.contains(pReactionIndex))
                {
                    speciesReactionDependencies.add(pReactionIndex);
                }
            }
            else
            {
                if(pTimeSymbolAllowed && dependentSymbolName.equals(ReservedSymbolMapperChemCommandLanguage.SYMBOL_TIME))
                {
                    pReactionsRecomputeAfterEachIteration.add(pReactionIndex);
                }
                else
                {
                    // do nothing, since the symbol is not dynamical and not an expression, and not "time"
                }
            }
        }        
    }

    private void createDependencyGraph(Model pModel) throws DataNotFoundException
    {
        Reaction []reactions = mReactions;
        int numReactions = reactions.length;

        HashSet dependencySet = new HashSet();
        ExpressionDependencyGetter dependencyGetter = new ExpressionDependencyGetter(dependencySet);
        HashSet reactionsRecomputeAfterEachIteration = new HashSet();

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
        }

        ReservedSymbolMapper reservedSymbolMapper = mSymbolEvaluator.getReservedSymbolMapper();
        boolean timeSymbolAllowed = (reservedSymbolMapper != null &&
                                     (reservedSymbolMapper instanceof ReservedSymbolMapperChemCommandLanguage));

        for(int j = 0; j < numReactions; ++j)
        {
            Reaction reaction = reactions[j];
            Value reactionRate = reaction.getRate();
            Integer reactionIndex = new Integer(j);
            if(! reactionRate.isExpression())
            {
                Symbol []reactants = (Symbol []) mReactionsReactantsSpecies[j];
                
                int numReactants = reactants.length;
                for(int k = 0; k < numReactants; ++k)
                {
                    Symbol reactant = reactants[k];
                    String reactantName = reactant.getName();
                    HashSet speciesReactionDependencies = (HashSet) speciesReactions.get(reactantName);
                    if(null != speciesReactionDependencies)
                    {
                        if(! reactionsRecomputeAfterEachIteration.contains(reactionIndex))
                        {
                            speciesReactionDependencies.add(reactionIndex);
                        }
                    }
                    else
                    {
                        Symbol indexedReactantSymbol = (Symbol) mSymbolMap.get(reactantName);
                        if(null != indexedReactantSymbol)
                        {
                            if(null != indexedReactantSymbol.getValueArray())
                            {
                                Value indexedReactantValue = mNonDynamicSymbolValues[indexedReactantSymbol.getArrayIndex()];
                                if(indexedReactantValue.isExpression())
                                {
                                    // need to find out all the other symbols it depends on
                                    dependencySet.clear();
                                    indexedReactantValue.getExpressionValue().visit(dependencyGetter);
                                    processExpressionDependencies(dependencySet,
                                                                  speciesReactions,
                                                                  timeSymbolAllowed,
                                                                  reactionsRecomputeAfterEachIteration,
                                                                  reactionIndex);
                                }
                                else
                                {
                                    // do nothing, since the dependent symbol is not dynamical and not an expression
                                }
                            }
                            else
                            {
                                throw new IllegalStateException("symbol has not been indexed: " + reactantName + " in reaction: " + reaction);
                            }
                        }
                        else
                        {
                            throw new IllegalStateException("unrecognized symbol: " + reactantName + " in reaction: " + reaction.getName());
                        }
                    }
                }
            }
            else
            {
                // need to find out all the other symbols it depends on
                dependencySet.clear();
                reactionRate.getExpressionValue().visit(dependencyGetter);
                processExpressionDependencies(dependencySet,
                                              speciesReactions,
                                              timeSymbolAllowed,
                                              reactionsRecomputeAfterEachIteration,
                                              reactionIndex);
            }
        }

        Integer []dummyArray = new Integer[0];
        mReactionsRecomputeAfterEachIteration = (Integer []) reactionsRecomputeAfterEachIteration.toArray(dummyArray);

        HashSet []reactionDependencies = new HashSet[numReactions];
        for(int j = 0; j < numReactions; ++j)
        {
            Reaction reaction = reactions[j];
            HashSet dependentReactions = new HashSet();

            // go through the list of products for this reaction

            Symbol []productsSpecies = (Symbol []) mReactionsProductsSpecies[j];
            boolean []productsDynamic = (boolean []) mReactionsProductsDynamic[j];
            int numProducts = productsSpecies.length;
            for(int i = 0; i < numProducts; ++i)
            {
                if(productsDynamic[i])
                {
                    Symbol productSpecies = productsSpecies[i];
                    String productSpeciesName = productSpecies.getName();
                    HashSet speciesDependentReactions = (HashSet) speciesReactions.get(productSpeciesName);
                    if(null != speciesDependentReactions)
                    {
                        dependentReactions.addAll(speciesDependentReactions);
                    }

                }
            }

            Symbol []reactantsSpecies = (Symbol []) mReactionsReactantsSpecies[j];
            boolean []reactantsDynamic = (boolean []) mReactionsReactantsDynamic[j];

            int numReactants = reactantsSpecies.length;
            for(int i = 0; i < numReactants; ++i)
            {
                if(reactantsDynamic[i])
                {
                    Symbol reactantSpecies = reactantsSpecies[i];
                    String reactantSpeciesName = reactantSpecies.getName();
                    HashSet speciesDependentReactions = (HashSet) speciesReactions.get(reactantSpeciesName);
                    if(null != speciesDependentReactions)
                    {
                        dependentReactions.addAll(speciesDependentReactions);
                    }
                }
            }

            reactionDependencies[j] = dependentReactions;
        }

        mReactionDependencies = new Object[numReactions];
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            HashSet dependentReactions = reactionDependencies[ctr];
            Integer reactionCtr = new Integer(ctr);
            if(dependentReactions.contains(reactionCtr))
            {
                dependentReactions.remove(reactionCtr);
            }
            Integer []fakeArray = new Integer[0];
            mReactionDependencies[ctr] = (Integer []) dependentReactions.toArray(fakeArray);
        }
    }

    private double computeTimeToNextReaction(double []pReactionProbabilities,
                                             int pIndex,
                                             double pTime,
                                             RandomElement pRandomNumberGenerator)
    {
        double probRate = pReactionProbabilities[pIndex];
        double timeOfNextReaction = 0.0;
        if(0.0 < probRate)
        {
            timeOfNextReaction = pTime + chooseDeltaTimeToNextReaction(pReactionProbabilities[pIndex]);
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

    private void updateReactionRateAndTime(double pCurrentTime,
                                           int pReactionIndex,
                                           int pLastReactionIndex) throws DataNotFoundException
    {
        MutableDouble timeOfNextReactionEventObj = (MutableDouble) mPutativeTimeToNextReactions.get(pReactionIndex);
        double newRate = computeReactionRate(pReactionIndex);

        if(pLastReactionIndex != pReactionIndex)
        {
            if(newRate > 0.0 && mReactionProbabilities[pReactionIndex] > 0.0)
            {
                timeOfNextReactionEventObj.setValue(((timeOfNextReactionEventObj.getValue() - pCurrentTime)*
                                                     mReactionProbabilities[pReactionIndex]/newRate) + pCurrentTime);
            }
            else
            {
                if(newRate > 0.0)
                {
                    timeOfNextReactionEventObj.setValue(pCurrentTime + chooseDeltaTimeToNextReaction(newRate));
                }
                else
                {
                    timeOfNextReactionEventObj.setValue(Double.POSITIVE_INFINITY);
                }
            }
        }
        else
        {
            timeOfNextReactionEventObj.setValue(pCurrentTime + chooseDeltaTimeToNextReaction(newRate));
        }

        mPutativeTimeToNextReactions.update(pReactionIndex, timeOfNextReactionEventObj);
        mReactionProbabilities[pReactionIndex] = newRate;
    }
                                                 

    protected void prepareForStochasticSimulation(double pStartTime,
                                                  SimulatorParameters pSimulatorParameters) throws DataNotFoundException
    {
        computeReactionProbabilities();

        computePutativeTimeToNextReactions(mRandomNumberGenerator,
                                           pStartTime,
                                           mReactionProbabilities,
                                           mReactions);
    }

    protected double iterate(MutableInteger pLastReactionIndex) throws DataNotFoundException, IllegalStateException
    {
        IndexedPriorityQueue putativeTimeToNextReactions = mPutativeTimeToNextReactions;
        Object []reactionDependencies = mReactionDependencies;

        double time = mSymbolEvaluator.getTime();

        int lastReactionIndex = pLastReactionIndex.getValue();
        if(NULL_REACTION != lastReactionIndex)
        {
            updateSymbolValuesForReaction(lastReactionIndex,
                                          mDynamicSymbolValues,
                                          mDynamicSymbolDelayedReactionAssociations,
                                          NUMBER_FIRINGS);
            if(mHasExpressionValues)
            {
                clearExpressionValueCaches(mNonDynamicSymbolValues);
            }
                           
            updateReactionRateAndTime(time, lastReactionIndex, lastReactionIndex);

            int numReactionsRecomputeAfterEachIteration = mReactionsRecomputeAfterEachIteration.length;
            for(int j = numReactionsRecomputeAfterEachIteration; --j >= 0; )
            {
                int reactionCtr = mReactionsRecomputeAfterEachIteration[j].intValue();
                updateReactionRateAndTime(time, j, lastReactionIndex);
            }
            
            Integer []dependentReactions = (Integer []) reactionDependencies[lastReactionIndex];
            int numDependentReactions = dependentReactions.length;
            for(int ctr = numDependentReactions; --ctr >= 0; )
            {
                Integer dependentReactionCtrObj = dependentReactions[ctr];
                int dependentReactionCtr = dependentReactionCtrObj.intValue();
                updateReactionRateAndTime(time, dependentReactionCtr, lastReactionIndex);

//                System.out.println("dependent reaction: " + dependentReaction + "; old time: " + oldPutativeTimeToNextReaction + "; new time: " + newPutativeTimeToNextReaction);
            }
        }

        int reactionIndex = putativeTimeToNextReactions.peekIndex();
        MutableDouble timeOfNextReactionObj = (MutableDouble) putativeTimeToNextReactions.get(reactionIndex);
        double timeOfNextReaction = timeOfNextReactionObj.getValue();

        if(null == mDelayedReactionSolvers)
        {
            // do nothing
        }
        else
        {
            int nextDelayedReactionIndex = getNextDelayedReactionIndex(mDelayedReactionSolvers);
            if(nextDelayedReactionIndex >= 0)
            {
                DelayedReactionSolver solver = mDelayedReactionSolvers[nextDelayedReactionIndex];
                double nextDelayedReactionTime = solver.peekNextReactionTime();
//                System.out.println("next delayed reaction will occur at: " + nextDelayedReactionTime);
                if(nextDelayedReactionTime < timeOfNextReaction)
                {
                    // execute delayed reaction
                    timeOfNextReaction = nextDelayedReactionTime;
                    reactionIndex = solver.getReactionIndex();
//                    System.out.println("delayed reaction selected: " + mReactions[reactionIndex]);
                    solver.pollNextReactionTime();
                }
            }
        }

        pLastReactionIndex.setValue(reactionIndex);
//        System.out.println("reaction selected: " + mReactions[reactionIndex].toString() + " at time: " + time);

        time = timeOfNextReaction;

        mSymbolEvaluator.setTime(time);

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
    


