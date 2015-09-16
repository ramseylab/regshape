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
    private static final int MIN_NUM_REACTION_STEPS_FOR_USING_DELAY_FUNCTION = 15;
    protected static final long DEFAULT_MIN_NUM_MILLISECONDS_FOR_UPDATE = 1000;
    protected static final int NULL_REACTION = -1;
    private static final long MIN_POPULATION_FOR_COMBINATORIC_EFFECTS = 100000;

    protected Value []mNonDynamicSymbolValues;
    protected boolean mHasExpressionValues;
    protected SymbolEvaluatorChem mSymbolEvaluator;
    protected HashMap mSymbolMap; // mapping between symbol names and the corresponding "Symbol" object
    protected SimulationController mSimulationController;
    protected DelayedReactionSolver []mDelayedReactionSolvers;
    protected boolean mInitialized;
    protected long mMinNumMillisecondsForUpdate;
    protected SimulationProgressReporter mSimulationProgressReporter;
    protected boolean mIsStochasticSimulator;

    // all arrays defined in the following block, are of length "mDynamicSymbols.length"
    protected Species []mDynamicSymbols;
    protected double []mInitialDynamicSymbolValues;  // saves a copy of the initial data
    protected String []mDynamicSymbolNames;
    protected double []mDynamicSymbolValues;

    // all arrays defined in the following block, are of length "mReactions.length"
    protected Reaction []mReactions;
    protected Object []mDynamicSymbolAdjustmentVectors;
    protected double []mReactionProbabilities;
    protected Object []mReactionsReactantsSpecies;
    protected Object []mReactionsReactantsStoichiometries;
    protected Object []mReactionsReactantsDynamic;
    protected Object []mReactionsProductsSpecies;
    protected Object []mReactionsProductsStoichiometries;
    protected Object []mReactionsProductsDynamic;
    protected HashMap []mReactionsLocalParamSymbolsMaps;
    protected Value []mReactionRates;
    protected DelayedReactionSolver []mReactionsDelayedReactionAssociations;

    static final void indexSymbolArray(SymbolValue []pSymbolArray, 
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

            assert (null == symbol.getDoubleArray()) : "array not null for new symbol " + symbolName;
            assert (null == symbol.getValueArray()) : "array not null for new symbol " + symbolName;
            assert (Symbol.NULL_ARRAY_INDEX == symbol.getArrayIndex()) : "array index not null for new symbol " + symbolName;
            assert (null != value) : "null value object for symbol: " + symbolName;

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

    public final boolean isInitialized()
    {
        return(mInitialized);
    }

    private final void clearDelayedReactionSolvers()
    {
        int numDelayedReactionSolvers = mDelayedReactionSolvers.length;
        for(int ctr = 0; ctr < numDelayedReactionSolvers; ++ctr)
        {
            DelayedReactionSolver solver = mDelayedReactionSolvers[ctr];
            solver.clear();
        }
    }

    protected final void prepareForSimulation(double pStartTime) 
    {
        // set initial values for dynamic symbols 
        System.arraycopy(mInitialDynamicSymbolValues, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);
        MathFunctions.vectorZeroElements(mReactionProbabilities);
        if(null != mDelayedReactionSolvers)
        {
            clearDelayedReactionSolvers();
        }
        if(mHasExpressionValues)
        {
            clearExpressionValueCaches(mNonDynamicSymbolValues);
        }
        mSymbolEvaluator.setTime(pStartTime);
        mSymbolEvaluator.setLocalSymbolsMap(null);
    }

    private final static void handleDelayedReaction(Reaction pReaction,
                                                    ArrayList pReactions,
                                                    int pReactionIndex,
                                                    ArrayList pDynamicSpecies,
                                                    ArrayList pDelayedReactionSolvers,
                                                    MutableInteger pRecursionDepth,
                                                    boolean pIsStochasticSimulator)
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
        
        Species reactant = (Species) ((ReactionParticipant) reactantsMap.values().iterator().next()).getSpecies();
        Species product = (Species) ((ReactionParticipant) productsMap.values().iterator().next()).getSpecies();

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
        String intermedSpeciesName = new String(reactionName + "___intermed_species_0");
        Compartment reactantCompartment = reactant.getCompartment();
        Species intermedSpecies = new Species(intermedSpeciesName, reactantCompartment);
        intermedSpecies.setSpeciesPopulation(0.0);
        firstReaction.addProduct(intermedSpecies, 1);
        pReactions.set(pReactionIndex, firstReaction);
        pDynamicSpecies.add(intermedSpecies);

        numSteps--;

        if(numSteps > 0 && numSteps < MIN_NUM_REACTION_STEPS_FOR_USING_DELAY_FUNCTION)
        {
            Species lastIntermedSpecies = intermedSpecies;

            for(int ctr = 0; ctr < numSteps; ++ctr)
            {
                Reaction reaction = new Reaction(reactionName + "___multistep_reaction_" + ctr);
                reaction.setRate(rate);

                reaction.addReactant(lastIntermedSpecies, 1);

                if(ctr < numSteps - 1)
                {
                    intermedSpeciesName = new String(reactionName + "___intermed_species_" + (ctr + 1));
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
            Reaction delayedReaction = new Reaction(reactionName + "___delayed_reaction");
            delayedReaction.addReactant(intermedSpecies, 1);
            delayedReaction.addProduct(product, 1);
            pReactions.add(delayedReaction);
            delayedReaction.setRate(rate);
            double delay = 0.0;
            if(numSteps > 0)
            {
                delay = (numSteps - 1) / rate;
            }
            else
            {
                delay = pReaction.getDelay();
            }

            DelayedReactionSolver solver = new DelayedReactionSolver(reactant,
                                                                     intermedSpecies,
                                                                     delay,
                                                                     rate,
                                                                     pReactions.size() - 1,
                                                                     pIsStochasticSimulator);
            pDelayedReactionSolvers.add(solver);
        }
    }

    public abstract boolean isStochasticSimulator();

    protected final void initializeSimulator(Model pModel) throws DataNotFoundException
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

        ArrayList delayedReactionSolvers = new ArrayList();

        boolean isStochasticSimulator = isStochasticSimulator();
        mIsStochasticSimulator = isStochasticSimulator;

        // for each multi-step reaction, split the reaction into two separate reactions
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            Reaction reaction = (Reaction) reactionsList.get(reactionCtr);
            int numSteps = reaction.getNumSteps();
            double delay = reaction.getDelay();
            if(numSteps == 1 && delay == 0.0)
            {
                continue;
            }
            MutableInteger recursionDepth = new MutableInteger(1);

            handleDelayedReaction(reaction, 
                                  reactionsList, 
                                  reactionCtr, 
                                  dynamicSymbolsList,
                                  delayedReactionSolvers,
                                  recursionDepth,
                                  isStochasticSimulator);
        }

        DelayedReactionSolver []delayedReactionSolversArray = (DelayedReactionSolver []) delayedReactionSolvers.toArray(new DelayedReactionSolver[0]);
        int numDelayedReactions = 0;
        mDelayedReactionSolvers = null;
        if(delayedReactionSolversArray.length > 0)
        {
            mDelayedReactionSolvers = delayedReactionSolversArray;
            numDelayedReactions = mDelayedReactionSolvers.length;
        }

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

            Symbol symbol = symbolValue.getSymbol();
            String symbolName = symbol.getName();

            Value value = symbolValue.getValue();
            assert (null != value) : "null value for symbol: " + symbolName;

            double symbolValueDouble = symbolValue.getValue().getValue();
            dynamicSymbolValues[symbolCtr] = symbolValueDouble;
            dynamicSymbolNames[symbolCtr] = symbolName;
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

        boolean hasExpressionValues = false;
        for(int ctr = 0; ctr < numNonDynamicSymbols; ++ctr)
        {
            if(mNonDynamicSymbolValues[ctr].isExpression())
            {
                hasExpressionValues = true;
            }
        }
        mHasExpressionValues = hasExpressionValues;

        if(null != mDelayedReactionSolvers)
        {
            mReactionsDelayedReactionAssociations = new DelayedReactionSolver[numReactions];
            for(int j = 0; j < numReactions; ++j)
            {
                mReactionsDelayedReactionAssociations[j] = null;
            }
            for(int ctr = 0; ctr < numDelayedReactions; ++ctr)
            {
                DelayedReactionSolver solver = mDelayedReactionSolvers[ctr];

                solver.initializeSpeciesSymbols(symbolMap,
                                                dynamicSymbols,
                                                nonDynamicSymbols);

                int reactionIndex = solver.getReactionIndex();
                mReactionsDelayedReactionAssociations[reactionIndex] = solver;
            }
        }
        else
        {
            mReactionsDelayedReactionAssociations = null;
        }
        
        ReservedSymbolMapper reservedSymbolMapper = pModel.getReservedSymbolMapper();
        SymbolEvaluationPostProcessor symbolEvaluationPostProcessor = pModel.getSymbolEvaluationPostProcessor();
        boolean useExpressionValueCaching = true;
        SymbolEvaluatorChem evaluator = new SymbolEvaluatorChem(useExpressionValueCaching,
                                                                symbolEvaluationPostProcessor,
                                                                reservedSymbolMapper,
                                                                symbolMap);
        evaluator.setTime(0.0);
        mSymbolEvaluator = evaluator;
        
        checkSymbolsValues();
        
        mReactionsReactantsSpecies = new Object[numReactions];
        mReactionsReactantsStoichiometries = new Object[numReactions];
        mReactionsReactantsDynamic = new Object[numReactions];

        mReactionsProductsSpecies = new Object[numReactions];
        mReactionsProductsStoichiometries = new Object[numReactions];
        mReactionsProductsDynamic = new Object[numReactions];

        mReactionsLocalParamSymbolsMaps = new HashMap[numReactions];

        mReactionRates = new Value[numReactions];

        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            Reaction reaction = reactions[reactionCtr];
            mReactionRates[reactionCtr] = reaction.getValue();

            int numReactants = reaction.getNumReactants();

            Species []reactantsSpeciesArray = new Species[numReactants];
            int []reactantsStoichiometryArray = new int[numReactants];
            boolean []reactantsDynamicArray = new boolean[numReactants];

            reaction.constructSpeciesArrays(reactantsSpeciesArray, 
                                            reactantsStoichiometryArray, 
                                            reactantsDynamicArray,
                                            dynamicSymbols, 
                                            nonDynamicSymbols,
                                            symbolMap, 
                                            ReactionParticipant.Type.REACTANT);

            Symbol []reactantsSymbolsArray = new Symbol[numReactants];
            for(int j = 0; j < numReactants; ++j)
            {
                reactantsSymbolsArray[j] = reactantsSpeciesArray[j].getSymbol();
            }

            mReactionsReactantsSpecies[reactionCtr] = reactantsSymbolsArray;
            mReactionsReactantsStoichiometries[reactionCtr] = reactantsStoichiometryArray;
            mReactionsReactantsDynamic[reactionCtr] = reactantsDynamicArray;

            int numProducts = reaction.getNumProducts();
            Species []productsSpeciesArray = new Species[numProducts];
            int []productsStoichiometryArray = new int[numProducts];
            boolean []productsDynamicArray = new boolean[numProducts];
        
            reaction.constructSpeciesArrays(productsSpeciesArray, 
                                            productsStoichiometryArray, 
                                            productsDynamicArray,
                                            dynamicSymbols, 
                                            nonDynamicSymbols,
                                            symbolMap, 
                                            ReactionParticipant.Type.PRODUCT);
        
            Symbol []productsSymbolsArray = new Symbol[numProducts];
            for(int j = 0; j < numProducts; ++j)
            {
                productsSymbolsArray[j] = productsSpeciesArray[j].getSymbol();
            }

            mReactionsProductsSpecies[reactionCtr] = productsSymbolsArray;
            mReactionsProductsStoichiometries[reactionCtr] = productsStoichiometryArray;
            mReactionsProductsDynamic[reactionCtr] = productsDynamicArray;

            SymbolValue []localSymbolsValues = reaction.getLocalSymbolValues();
            int numLocalSymbols = localSymbolsValues.length;
            Value []localValues = new Value[numLocalSymbols];
            HashMap localSymbolsMap = new HashMap();
            
            indexSymbolArray(localSymbolsValues,
                             localSymbolsMap,
                             null,
                             localValues);            

            mReactionsLocalParamSymbolsMaps[reactionCtr] = localSymbolsMap;
        }

        // create an array of doubles to hold the reaction probabilities
        mReactionProbabilities = new double[numReactions];

        checkReactionRates();

        mInitialized = true;
    }

    private final void checkReactionRates() throws DataNotFoundException
    {
        int numReactions = mReactions.length;
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            double reactionRate = computeReactionRate(reactionCtr);
            Symbol []reactants = (Symbol []) mReactionsReactantsSpecies[reactionCtr];
            int numReactants = reactants.length;
            for(int reactantCtr = numReactants; --reactantCtr >= 0; )
            {
                Symbol symbol = reactants[reactantCtr];
                mSymbolEvaluator.getValue(symbol);
            }
        }
    }

    private final void checkSymbolsValues() throws DataNotFoundException
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
        mSymbolMap = null;
        mSimulationController = null;
        mDelayedReactionSolvers = null;
        mDynamicSymbols = null;
        mDynamicSymbolAdjustmentVectors = null;
        mReactionsDelayedReactionAssociations = null;
    }

    public Simulator()
    {
        clearSimulatorState();
        setMinNumMillisecondsForUpdate(DEFAULT_MIN_NUM_MILLISECONDS_FOR_UPDATE);
    }

    protected final void initializeDynamicSymbolAdjustmentVectors()
    {
        Reaction []reactions = mReactions;

        int numReactions = reactions.length;
        Object []dynamicSymbolAdjustmentVectors = new Object[numReactions];

        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            Reaction reaction = reactions[ctr];
            
            dynamicSymbolAdjustmentVectors[ctr] = constructDynamicSymbolAdjustmentVector(ctr);
        }

        mDynamicSymbolAdjustmentVectors = dynamicSymbolAdjustmentVectors;
    }

    public abstract String getAlias();

    protected final SimulationResults createSimulationResults(double pStartTime,
                                                              double pEndTime,
                                                              SimulatorParameters pSimulatorParameters,
                                                              String []pResultsSymbolNames,
                                                              double []pResultsTimeValues,
                                                              Object []pResultsSymbolValues,
                                                              double []pResultsFinalSymbolFluctuations)
    {
        SimulationResults simulationResults = new SimulationResults();
        simulationResults.setSimulatorAlias(getAlias());
        simulationResults.setStartTime(pStartTime);
        simulationResults.setEndTime(pEndTime);
        simulationResults.setSimulatorParameters(pSimulatorParameters);
        simulationResults.setResultsSymbolNames(pResultsSymbolNames);
        simulationResults.setResultsTimeValues(pResultsTimeValues);
        simulationResults.setResultsSymbolValues(pResultsSymbolValues);
        simulationResults.setResultsFinalSymbolFluctuations(pResultsFinalSymbolFluctuations);
        return(simulationResults);

    }

    protected final int addRequestedSymbolValues(double pCurTime,
                                                 int pLastTimeIndex,
                                                 Symbol []pRequestedSymbols,
                                                 double []pTimeValues,
                                                 Object []pRetSymbolValues) throws DataNotFoundException
    {
        int numTimePoints = pTimeValues.length;
        int numRequestedSymbolValues = pRequestedSymbols.length;

        if(mHasExpressionValues)
        {
            clearExpressionValueCaches(mNonDynamicSymbolValues);
        }

        double saveTime = mSymbolEvaluator.getTime();

        int timeCtr = pLastTimeIndex;

        for(; timeCtr < numTimePoints; ++timeCtr)
        {
            double timeValue = pTimeValues[timeCtr];
            mSymbolEvaluator.setTime(timeValue);
            if(timeValue <= pCurTime)
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
                    symbolValues[symCtr] += mSymbolEvaluator.getValue(symbol);
                }
            }
            else
            {
                break;
            }
        }

        mSymbolEvaluator.setTime(saveTime);

        return(timeCtr);
    }

    protected final static double []createTimesArray(double pStartTime,
                                                     double pEndTime,
                                                     int pNumTimePoints)
    {
        assert (pNumTimePoints > 1) : " invalid number of time points";

        double []retTimesArray = new double[pNumTimePoints];

        double deltaTime = (pEndTime - pStartTime) / ((double) (pNumTimePoints - 1));

        for(int timeCtr = 0; timeCtr < pNumTimePoints; ++timeCtr)
        {
            double time = ((double) timeCtr) * deltaTime;
            if(time > pEndTime)
            {
                time = pEndTime;
            }
            retTimesArray[timeCtr] = time;
        }

        return(retTimesArray);
    }

    protected final Symbol []createRequestedSymbolArray(HashMap pSymbolMap,
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

    protected final void conductPreSimulationCheck(double pStartTime,
                                                   double pEndTime,
                                                   SimulatorParameters pSimulatorParameters,
                                                   int pNumResultsTimePoints) throws IllegalArgumentException, IllegalStateException
    {
       if(! mInitialized)
        {
            throw new IllegalStateException("simulator has not been initialized yet");
        }

        if(pNumResultsTimePoints <= 1)
        {
            throw new IllegalArgumentException("number of time points must be greater than or equal to 1");
        }

        if(pStartTime >= pEndTime)
        {
            throw new IllegalArgumentException("end time must be greater than the start time");
        }
    }

    protected final void computeReactionProbabilities() throws DataNotFoundException
    {
        // loop through all reactions, and for each reaction, compute the reaction probability
        int numReactions = mReactions.length;
        
        if(mHasExpressionValues)
        {
            clearExpressionValueCaches(mNonDynamicSymbolValues);
        }

        for(int reactionCtr = numReactions; --reactionCtr >= 0; )
        {
            // store reaction probability
            mReactionProbabilities[reactionCtr] = computeReactionRate(reactionCtr);
        }
    }

    protected static final double getDelayedReactionEstimatedAverageFutureRate(SymbolEvaluatorChem pSymbolEvaluator,
                                                                               DelayedReactionSolver []pDelayedReactionSolvers) throws DataNotFoundException
    {
        double compositeRate = 0.0;
        int numDelayedReactions = pDelayedReactionSolvers.length;
        for(int ctr = numDelayedReactions; --ctr >= 0; )
        {
            DelayedReactionSolver solver = pDelayedReactionSolvers[ctr];
            double estimatedFutureRate = solver.getEstimatedAverageFutureRate(pSymbolEvaluator);
            compositeRate += estimatedFutureRate;
        }
        return(compositeRate);
    }

    protected static final void clearExpressionValueCaches(Value []pNonDynamicSymbolValues)
    {
        int numNonDynamicSymbols = pNonDynamicSymbolValues.length;
        for(int ctr = numNonDynamicSymbols; --ctr >= 0; )
        {
            pNonDynamicSymbolValues[ctr].clearExpressionValueCache();
        }
    }

    protected final void computeDerivative(double []pTempDynamicSymbolValues,
                                           double []pDynamicSymbolDerivatives) throws DataNotFoundException
    {
        computeReactionProbabilities();

        int numReactions = mReactions.length;
        Reaction reaction = null;
        double reactionRate = 0.0;

        MathFunctions.vectorZeroElements(pDynamicSymbolDerivatives);

        Object []dynamicSymbolAdjustmentVectors = mDynamicSymbolAdjustmentVectors;

        for(int reactionCtr = numReactions; --reactionCtr >= 0; )
        {
            reaction = mReactions[reactionCtr];
            reactionRate = mReactionProbabilities[reactionCtr];

            double []symbolAdjustmentVector = (double []) dynamicSymbolAdjustmentVectors[reactionCtr];
            
            // we want to multiply this vector by the reaction rate and add it to the derivative
            MathFunctions.vectorScalarMultiply(symbolAdjustmentVector, reactionRate, pTempDynamicSymbolValues);
            
            MathFunctions.vectorAdd(pTempDynamicSymbolValues, pDynamicSymbolDerivatives, pDynamicSymbolDerivatives);
        }
    }

    public final void setProgressReporter(SimulationProgressReporter pSimulationProgressReporter)
    {
        mSimulationProgressReporter = pSimulationProgressReporter;
    }

    public final void setController(SimulationController pSimulationController)
    {
        mSimulationController = pSimulationController;
    }

    public final void setStatusUpdateIntervalSeconds(double pUpdateIntervalSeconds) throws IllegalArgumentException
    {
        if(pUpdateIntervalSeconds <= 0.0)
        {
            throw new IllegalArgumentException("invalid minimum number of seconds for update: " + pUpdateIntervalSeconds);
        }
        long updateIntervalMilliseconds = (long) (1000.0 * pUpdateIntervalSeconds);
        setMinNumMillisecondsForUpdate(updateIntervalMilliseconds);
    }

    protected final void setMinNumMillisecondsForUpdate(long pMinNumMillisecondsForUpdate)
    {
        mMinNumMillisecondsForUpdate = pMinNumMillisecondsForUpdate;
    }

    protected final double getMinDelayedReactionDelay()
    {
        double retDelay = Double.MAX_VALUE;
        DelayedReactionSolver []delayedReactionSolvers = mDelayedReactionSolvers;
        int numDelayedReactions = delayedReactionSolvers.length;
        for(int ctr = 0; ctr < numDelayedReactions; ++ctr)
        {
            DelayedReactionSolver delayedReactionSolver = delayedReactionSolvers[ctr];
            double delay = delayedReactionSolver.getDelay();
            if(delay < retDelay)
            {
                retDelay = delay;
            }
        }
        return(retDelay);
    }

    private static double computeRateFactorForSpecies(double pSpeciesValue,
                                                      int pStoichiometry,
                                                      boolean pIsDynamic,
                                                      boolean pIsStochastic)
    {
        if(pIsStochastic && pIsDynamic)
        {
            if(pSpeciesValue >= pStoichiometry)
            {
                if(pSpeciesValue < MIN_POPULATION_FOR_COMBINATORIC_EFFECTS)
                {
                    if(1 == pStoichiometry)
                    {
                        return(pSpeciesValue);
                    }
                    else
                    {
                        double retVal = 1.0;
                        for(int ctr = pStoichiometry; --ctr >= 0; )
                        {
                            retVal *= (pSpeciesValue - (double) ctr);
                        }
                        return(retVal);
                    }
                }
                else
                {
                    return(Math.pow(pSpeciesValue, pStoichiometry));
                }
            }
            else
            {
                return(0.0);
            }
        }
        else
        {
            if(1 == pStoichiometry)
            {
                return(pSpeciesValue);
            }
            else
            {
                return(Math.pow(pSpeciesValue, pStoichiometry));
            }
        }
    }

    final double computeReactionRate(int pReactionCtr) throws DataNotFoundException
    {
        if(null == mReactionsDelayedReactionAssociations || null == mReactionsDelayedReactionAssociations[pReactionCtr])
        {
            if(! mReactionRates[pReactionCtr].isExpression())
            {
                Symbol []reactantsSpecies = (Symbol []) mReactionsReactantsSpecies[pReactionCtr];
                int []reactantsStoichiometries = (int []) mReactionsReactantsStoichiometries[pReactionCtr];
                boolean []reactantsDynamic = (boolean []) mReactionsReactantsDynamic[pReactionCtr];
                double rate = mReactionRates[pReactionCtr].getValue();
                int numReactants = reactantsSpecies.length;
                for(int reactantCtr = numReactants; --reactantCtr >= 0; )
                {
                    rate *= computeRateFactorForSpecies(mSymbolEvaluator.getValue(reactantsSpecies[reactantCtr]), 
                                                        reactantsStoichiometries[reactantCtr],
                                                        reactantsDynamic[reactantCtr],
                                                        mIsStochasticSimulator);
                }
                return(rate);
            }
            else
            {
                mSymbolEvaluator.setLocalSymbolsMap(mReactionsLocalParamSymbolsMaps[pReactionCtr]);
                double rate = mReactionRates[pReactionCtr].getValue(mSymbolEvaluator);
                mSymbolEvaluator.setLocalSymbolsMap(null);
                return(rate);
            }
        }
        else
        {
            // it is a delayed reaction; call computeRate() on the DelayedReactionSolver
            double rate = mReactionsDelayedReactionAssociations[pReactionCtr].computeRate(mSymbolEvaluator);
//            System.out.println("time: " + mSymbolEvaluator.getTime() + "; rate for delayed reaction is: " + rate);
            return(rate);
        }
    }

    private double []constructDynamicSymbolAdjustmentVector(int pReactionCtr)
    {
        int numSymbols = mDynamicSymbols.length;
        double []dynamicSymbolVector = new double[numSymbols];

        Reaction reaction = mReactions[pReactionCtr];
        HashMap reactantsMap = reaction.getReactantsMap();
        HashMap productsMap = reaction.getProductsMap();

        for(int symbolCtr = 0; symbolCtr < numSymbols; ++symbolCtr)
        {
            SymbolValue symbolValue = mDynamicSymbols[symbolCtr];
            Symbol symbol = symbolValue.getSymbol();
            String symbolName = symbol.getName();
            double vecElement = 0.0;
            ReactionParticipant reactantParticipant = (ReactionParticipant) reactantsMap.get(symbolName);
            if(null != reactantParticipant && reactantParticipant.mDynamic)
            {
                vecElement -= reactantParticipant.mStoichiometry;
            }
            ReactionParticipant productParticipant = (ReactionParticipant) productsMap.get(symbolName);
            if(null != productParticipant && productParticipant.mDynamic)
            {
                vecElement += productParticipant.mStoichiometry;
            }

            dynamicSymbolVector[symbolCtr] = vecElement;
        }

        return(dynamicSymbolVector);
    }


    public static Expression []getReactionRateExpressions(Reaction []pReactions) throws DataNotFoundException
    {
        int numReactions = pReactions.length;
        Expression []a = new Expression[numReactions];
        for(int j = 0; j < numReactions; ++j)
        {
            Reaction reaction = pReactions[j];
            a[j] = reaction.getRateExpression();
        }
        return(a);
    }
}
