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

import java.util.*;

public abstract class Simulator 
{
    private static final int NUM_REACTION_STEPS_USE_GAMMA_APPROXIMATION = 15;

    protected String []mDynamicSymbolNames;
    protected double []mDynamicSymbolValues;
    protected double []mInitialDynamicSymbolValues;  // saves a copy of the initial data
    protected Value []mNonDynamicSymbolValues;
    protected SymbolEvaluatorChemSimulation mSymbolEvaluator;
    protected Reaction []mReactions;
    protected double []mReactionProbabilities;
    protected SpeciesRateFactorEvaluator mSpeciesRateFactorEvaluator;
    protected HashMap mSymbolMap;
    protected SimulationController mSimulationController;
    protected boolean mInitialized;
    protected Species []mDynamicSymbols;

    protected MultistepReactionSolver []mMultistepReactionSolvers;

    public static final int NULL_REACTION = -1;

    static void indexSymbolArray(SymbolValue []pSymbolArray, 
                                 HashMap pSymbolMap, 
                                 double []pDoubleArray, 
                                 Value []pValueArray)
    {
        assert (null == pDoubleArray ||
                null == pValueArray) : "either pDoubleArray or pValueArray must be null";

        int numSymbols = pSymbolArray.length;
        for(int symbolCtr = 0; symbolCtr < numSymbols; ++symbolCtr)
        {
            SymbolValue symbolValue = pSymbolArray[symbolCtr];
            Symbol symbol = (Symbol) symbolValue.getSymbol();
            String symbolName = symbol.getName();
            Value value = (Value) symbolValue.getValue();

            pSymbolMap.put(symbolName, symbol);

            if(null != pDoubleArray)
            {
                symbol.setArray(pDoubleArray);
                pDoubleArray[symbolCtr] = value.getValue();
            }
            else
            {
                symbol.setArray(pValueArray);
                pValueArray[symbolCtr] = (Value) value;
            }

            symbol.setArrayIndex(symbolCtr);
        }
    }

    public boolean isInitialized()
    {
        return(mInitialized);
    }

    private void clearMultistepReactionSolvers()
    {
        int numMultistepReactionSolvers = mMultistepReactionSolvers.length;
        for(int ctr = 0; ctr < numMultistepReactionSolvers; ++ctr)
        {
            MultistepReactionSolver solver = mMultistepReactionSolvers[ctr];
            solver.clear();
        }
    }

    protected void prepareForSimulation(double pStartTime) 
    {
        // set initial values for dynamic symbols 
        System.arraycopy(mInitialDynamicSymbolValues, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);
        MathFunctions.vectorZeroElements(mReactionProbabilities);
        clearMultistepReactionSolvers();
        mSymbolEvaluator.setTime(pStartTime);
    }



    public static void handleMultistepReaction(Reaction pReaction,
                                               ArrayList pReactions,
                                               int pReactionIndex,
                                               ArrayList pDynamicSpecies,
                                               ArrayList pMultistepReactionSolvers,
                                               MutableInteger pRecursionDepth)
    {
        String reactionName = pReaction.getName();

        HashMap reactantsMap = pReaction.getReactantsMap();
        if(reactantsMap.size() != 1)
        {
            throw new IllegalStateException("a multi-step reaction must have excactly one reactant species; reaction is: " + reactionName);
        }

        HashMap productsMap = pReaction.getProductsMap();
        if(productsMap.size() != 1)
        {
            throw new IllegalStateException("a multi-step reaction must have exactly one product species; reaction is: " + reactionName);
        }

        int numSteps = pReaction.getNumSteps();
        if(numSteps < 2)
        {
            throw new IllegalStateException("a multi-step reaction must have at least two steps");
        }
        
        Species reactant = (Species) ((Reaction.ReactionElement) reactantsMap.values().iterator().next()).getSpecies();
        Species product = (Species) ((Reaction.ReactionElement) productsMap.values().iterator().next()).getSpecies();

        if(! reactant.getCompartment().equals(product.getCompartment()))
        {
            throw new IllegalStateException("the reactant and product for a multi-step reaction must be the same compartment");
        }

        Value rateValue = pReaction.getRate();
        if(rateValue.isExpression())
        {
            throw new IllegalStateException("a multi-step reaction must have a numeric reaction rate, not a custom rate expression");
        }
        double rate = rateValue.getValue();

        Reaction firstReaction = new Reaction(reactionName);
        firstReaction.setRate(rate);
        firstReaction.addReactant(reactant, 1);
        String intermedSpeciesName = new String("multistep_species_" + reactionName + "_" + product.getName() + "_0");
        Compartment reactantCompartment = reactant.getCompartment();
        Species intermedSpecies = new Species(intermedSpeciesName, reactantCompartment);
        intermedSpecies.setSpeciesPopulation(0.0);
        firstReaction.addProduct(intermedSpecies, 1);
        pReactions.set(pReactionIndex, firstReaction);
        pDynamicSpecies.add(intermedSpecies);

        numSteps--;

        if(numSteps < NUM_REACTION_STEPS_USE_GAMMA_APPROXIMATION)
        {
            Species lastIntermedSpecies = intermedSpecies;

            for(int ctr = 0; ctr < numSteps; ++ctr)
            {
                Reaction reaction = new Reaction("multistep_reaction_" + reactionName + "_" + product.getName() + "_" + ctr);
                reaction.setRate(rate);

                reaction.addReactant(lastIntermedSpecies, 1);

                if(ctr < numSteps - 1)
                {
                    intermedSpeciesName = new String("multistep_species_" + reactionName + "_" + product.getName() + "_" + (ctr + 1));
                    intermedSpecies = new Species(intermedSpeciesName, reactantCompartment);                    
                    intermedSpecies.setSpeciesPopulation(0.0);
                    reaction.addProduct(intermedSpecies, 1);
                    pDynamicSpecies.add(intermedSpecies);
                    lastIntermedSpecies = intermedSpecies;
                }
                else
                {
                    reaction.addProduct(product, 1);
                }

                pReactions.add(reaction);
            }
        }
        else
        {
            Reaction multistepReaction = new Reaction("multistep_reaction_" + reactionName + "_" + product.getName());
            multistepReaction.setNumSteps(numSteps);
            multistepReaction.addReactant(intermedSpecies, 1);
            multistepReaction.addProduct(product, 1);
            pReactions.add(multistepReaction);

            // need to use gamma distribution approximation; leave as multi-step reaction;
            // create a "multistep reaction solver" to store the time-series data for the reactant species
            MultistepReactionSolver solver = new MultistepReactionSolver(reactant,
                                                                         intermedSpecies,
                                                                         numSteps,
                                                                         rate,
                                                                         pReactions.size() - 1);
            pMultistepReactionSolvers.add(solver);
            multistepReaction.setRate(solver);
        }
    }

    protected abstract void initializeMultistepReactionSolvers();

    protected void initializeSimulator(Model pModel, SimulationController pSimulationController) throws DataNotFoundException
    {
        clearSimulatorState();

        // obtain and save an array of all reactions in the model
        ArrayList reactionsList = pModel.constructReactionsList();
        int numReactions = reactionsList.size();

        // get an array of all dynamical SymbolValues in the model
        ArrayList dynamicSymbolsList = pModel.constructDynamicSymbolsList();

        // get an array of all symbols in the model
        SymbolValue []nonDynamicSymbols = pModel.constructGlobalNonDynamicSymbolsArray();
        int numNonDynamicSymbols = nonDynamicSymbols.length;

        ArrayList multistepReactionSolvers = new ArrayList();

        // for each multi-step reaction, split the reaction into two separate reactions
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            Reaction reaction = (Reaction) reactionsList.get(reactionCtr);
            int numSteps = reaction.getNumSteps();
            if(numSteps == 1)
            {
                continue;
            }

            MutableInteger recursionDepth = new MutableInteger(1);

            handleMultistepReaction(reaction, 
                                    reactionsList, 
                                    reactionCtr, 
                                    dynamicSymbolsList,
                                    multistepReactionSolvers,
                                    recursionDepth);
        }

        mMultistepReactionSolvers = (MultistepReactionSolver []) multistepReactionSolvers.toArray(new MultistepReactionSolver[0]);
        initializeMultistepReactionSolvers();

        int numMultistepReactions = mMultistepReactionSolvers.length;

        Species []dynamicSymbols = (Species []) dynamicSymbolsList.toArray(new Species[0]);;
        mDynamicSymbols = dynamicSymbols;
        int numDynamicSymbols = dynamicSymbols.length;

        Reaction []reactions = (Reaction []) reactionsList.toArray(new Reaction[0]);
        mReactions = reactions;
        numReactions = reactions.length;

        // create an array of doubles to hold the dynamical symbols' values
        double []dynamicSymbolValues = new double[numDynamicSymbols];
        String []dynamicSymbolNames = new String[numDynamicSymbols];

        mDynamicSymbolValues = dynamicSymbolValues;

        // store the initial dynamic symbol values as a "vector" (native double array)
        for(int symbolCtr = 0; symbolCtr < numDynamicSymbols; ++symbolCtr)
        {
            SymbolValue symbolValue = dynamicSymbols[symbolCtr];
            assert (null != symbolValue.getValue()) : "null value for symbol: " + symbolValue.getSymbol().getName();
            double symbolValueDouble = symbolValue.getValue().getValue();
            dynamicSymbolValues[symbolCtr] = symbolValueDouble;
            dynamicSymbolNames[symbolCtr] = symbolValue.getSymbol().getName();
        }

        mDynamicSymbolNames = dynamicSymbolNames;

        double []initialDynamicSymbolValues = new double[numDynamicSymbols];
        System.arraycopy(dynamicSymbolValues, 0, initialDynamicSymbolValues, 0, numDynamicSymbols);
        mInitialDynamicSymbolValues = initialDynamicSymbolValues;

        // create a map between symbol names, and their corresponding indexed
        // "Symbol" object
        HashMap symbolMap = new HashMap();
        mSymbolMap = symbolMap;

        indexSymbolArray(dynamicSymbols,
                         symbolMap,
                         dynamicSymbolValues,
                         null);


        // build a map between symbol names and array indices

        Value []nonDynamicSymbolValues = new Value[numNonDynamicSymbols];
        mNonDynamicSymbolValues = nonDynamicSymbolValues;

        indexSymbolArray(nonDynamicSymbols,
                         symbolMap,
                         null,
                         nonDynamicSymbolValues);

        for(int ctr = 0; ctr < numMultistepReactions; ++ctr)
        {
            MultistepReactionSolver solver = mMultistepReactionSolvers[ctr];
            solver.initializeSpeciesSymbols(symbolMap,
                                            dynamicSymbols,
                                            nonDynamicSymbols);
                                            
        }

        SymbolEvaluatorChemSimulation evaluator = new SymbolEvaluatorChemSimulation(symbolMap, 0.0);
        mSymbolEvaluator = evaluator;

        checkSymbolsValues();
        
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            Reaction reaction = reactions[reactionCtr];
            reaction.prepareSymbolVectorsForSimulation(dynamicSymbols, nonDynamicSymbols, symbolMap);
        }

        mSpeciesRateFactorEvaluator = pModel.getSpeciesRateFactorEvaluator();

        // create an array of doubles to hold the reaction probabilities
        mReactionProbabilities = new double[numReactions];

        checkReactionRates();

        mSimulationController = pSimulationController;
        mInitialized = true;
    }

    private void checkReactionRates() throws DataNotFoundException
    {
        int numReactions = mReactions.length;
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            Reaction reaction = mReactions[reactionCtr];
            double reactionRate = reaction.computeRate(mSpeciesRateFactorEvaluator, mSymbolEvaluator);
            reaction.checkReactantValues(mSymbolEvaluator);
        }
    }

    private void checkSymbolsValues() throws DataNotFoundException
    {
        // test getting value for each symbol in the symbol table
        Iterator symbolNameIter = mSymbolMap.keySet().iterator();
        while(symbolNameIter.hasNext())
        {
            String symbolName = (String) symbolNameIter.next();
            Symbol symbol = (Symbol) mSymbolMap.get(symbolName);
            assert (null != symbol) : "found null Symbol where a valid Symbol object was expected";
        }        
    }

    protected void clearSimulatorState()
    {
        mInitialized = false;
        mDynamicSymbolValues = null;
        mInitialDynamicSymbolValues = null;
        mNonDynamicSymbolValues = null;
        mSymbolEvaluator = null;
        mReactions = null;
        mReactionProbabilities = null;
        mSpeciesRateFactorEvaluator = null;
        mSymbolMap = null;
        mSimulationController = null;
        mMultistepReactionSolvers = null;
    }

    public Simulator()
    {
        clearSimulatorState();
    }

    protected boolean checkSimulationControllerStatus()
    {
        boolean isCancelled = false;
        if(null != mSimulationController)
        {
            if(mSimulationController.checkIfCancelled())
            {
                isCancelled = true;
            }
        }
        return(isCancelled);
    }

    protected final int addRequestedSymbolValues(double pCurTime,
                                                 int pLastTimeIndex,
                                                 Symbol []pRequestedSymbols,
                                                 SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                 double []pTimeValues,
                                                 Object []pRetSymbolValues) throws DataNotFoundException
    {
        int numTimePoints = pTimeValues.length;
        int numRequestedSymbolValues = pRequestedSymbols.length;

        double saveTime = pSymbolEvaluator.getTime();

        int timeCtr = pLastTimeIndex;

        for(; timeCtr < numTimePoints; ++timeCtr)
        {
            double timeValue = pTimeValues[timeCtr];
            pSymbolEvaluator.setTime(timeValue);
            if(timeValue < pCurTime)
            {
                double []symbolValues = (double []) pRetSymbolValues[timeCtr];
                if(null == symbolValues)
                {
                    symbolValues = new double[numRequestedSymbolValues];
                    for(int symCtr = numRequestedSymbolValues - 1; symCtr >= 0; --symCtr)
                    {
                        symbolValues[symCtr] = 0.0;
                    }
                    pRetSymbolValues[timeCtr] = symbolValues;
                }
                for(int symCtr = numRequestedSymbolValues - 1; symCtr >= 0; --symCtr)
                {
                    Symbol symbol = pRequestedSymbols[symCtr];
                    symbolValues[symCtr] += pSymbolEvaluator.getValue(symbol);
                }
            }
            else
            {
                break;
            }
        }

        pSymbolEvaluator.setTime(saveTime);

        return(timeCtr);
    }

    protected static void prepareTimesArray(double pStartTime,
                                            double pEndTime,
                                            int pNumTimePoints,
                                            double []pRetTimesArray)
    {
        assert (pNumTimePoints > 0) : " invalid number of time points";

        double deltaTime = (pEndTime - pStartTime) / ((double) pNumTimePoints);
        double halfDeltaTime = deltaTime / 2.0;

        double time = halfDeltaTime;

        for(int timeCtr = 0; timeCtr < pNumTimePoints; ++timeCtr)
        {
            pRetTimesArray[timeCtr] = (((double) timeCtr) * deltaTime) + halfDeltaTime;
        }
    }

    protected Symbol []prepareRequestedSymbolArray(HashMap pSymbolMap,
                                                   String []pRequestedSymbols) throws DataNotFoundException
    {
        int numSymbols = pRequestedSymbols.length;
        Symbol []symbols = new Symbol[numSymbols];
        for(int symbolCtr = numSymbols - 1; symbolCtr >= 0; --symbolCtr)
        {
            String symbolName = pRequestedSymbols[symbolCtr];
            Symbol symbol = (Symbol) pSymbolMap.get(symbolName);
            if(null == symbol)
            {
                throw new DataNotFoundException("requested symbol not found in model: " + symbolName);
            }
            symbols[symbolCtr] = symbol;
        }
        return(symbols);
    }

    public abstract void simulate(double pStartTime, 
                                  double pEndTime,
                                  int pNumTimePoints,
                                  int pNumSteps,
                                  String []pRequestedSymbolNames,
                                  double []pRetTimeValues,
                                  Object []pRetSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException;




    protected static final void computeReactionProbabilities(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                                             SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                             double []pReactionProbabilities,
                                                             Reaction []pReactions) throws DataNotFoundException
    {
        // loop through all reactions, and for each reaction, compute the reaction probability
        int numReactions = pReactions.length;
        
        Reaction reaction = null;
        double reactionProbability = 0.0;

        for(int reactionCtr = numReactions; --reactionCtr >= 0; )
        {
            reaction = pReactions[reactionCtr];

            reactionProbability = reaction.computeRate(pSpeciesRateFactorEvaluator, pSymbolEvaluator);

//            System.out.println("reaction: " + reaction + "; rate: " + reactionProbability);
            // store reaction probability
            pReactionProbabilities[reactionCtr] = reactionProbability;
        }
    }

    protected static final double getMultistepReactionEstimatedAverageFutureRate(SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                                                 MultistepReactionSolver []pMultistepReactionSolvers) throws DataNotFoundException
    {
        double compositeRate = 0.0;
        int numMultistepReactions = pMultistepReactionSolvers.length;
        for(int ctr = numMultistepReactions; --ctr >= 0; )
        {
            MultistepReactionSolver solver = pMultistepReactionSolvers[ctr];
            double estimatedFutureRate = solver.getEstimatedAverageFutureRate(pSymbolEvaluator);
            compositeRate += estimatedFutureRate;
        }
        return(compositeRate);
    }
}
