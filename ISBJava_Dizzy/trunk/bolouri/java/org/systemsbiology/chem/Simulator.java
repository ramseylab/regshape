package org.systemsbiology.chem;

import org.systemsbiology.math.SymbolValue;
import org.systemsbiology.math.Value;
import org.systemsbiology.math.Symbol;
import org.systemsbiology.math.SymbolEvaluator;
import org.systemsbiology.math.MathFunctions;
import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.DebugUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public abstract class Simulator 
{
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

    protected void prepareForSimulation(double pStartTime) 
    {
        // set initial values for dynamic symbols 
        System.arraycopy(mInitialDynamicSymbolValues, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);
        MathFunctions.vectorZeroElements(mReactionProbabilities);
        mSymbolEvaluator.setTime(pStartTime);
    }

    protected void initializeSimulator(Model pModel, SimulationController pSimulationController) throws DataNotFoundException
    {
        clearSimulatorState();

        // obtain and save an array of all reactions in the model
        Reaction []reactions = pModel.constructReactionsArray();
        mReactions = reactions;

        // create an array of doubles to hold the reaction probabilities
        mReactionProbabilities = new double[reactions.length];

        // get an array of all dynamical SymbolValues in the model
        Species []dynamicSymbols = pModel.constructDynamicSymbolsArray();
        int numDynamicSymbols = dynamicSymbols.length;

        // create an array of doubles to hold the dynamical symbols' values
        double []dynamicSymbolValues = new double[numDynamicSymbols];

        mDynamicSymbolValues = dynamicSymbolValues;

        // store the initial dynamic symbol values as a "vector" (native double array)
        for(int symbolCtr = 0; symbolCtr < numDynamicSymbols; ++symbolCtr)
        {
            SymbolValue symbolValue = dynamicSymbols[symbolCtr];
            assert (null != symbolValue.getValue()) : "null value for symbol: " + symbolValue.getSymbol().getName();
            double symbolValueDouble = symbolValue.getValue().getValue();
            dynamicSymbolValues[symbolCtr] = symbolValueDouble;
        }

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


        // get an array of all symbols in the model
        SymbolValue []nonDynamicSymbols = pModel.constructGlobalNonDynamicSymbolsArray();

        // build a map between symbol names and array indices

        Value []nonDynamicSymbolValues = new Value[nonDynamicSymbols.length];
        mNonDynamicSymbolValues = nonDynamicSymbolValues;

        indexSymbolArray(nonDynamicSymbols,
                         symbolMap,
                         null,
                         nonDynamicSymbolValues);

        SymbolEvaluatorChemSimulation evaluator = new SymbolEvaluatorChemSimulation(symbolMap, 0.0);
        mSymbolEvaluator = evaluator;

        checkSymbolsValues();
        
        int numReactions = reactions.length;
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            Reaction reaction = reactions[reactionCtr];
            reaction.prepareSymbolVectorsForSimulation(dynamicSymbols, nonDynamicSymbols, symbolMap);
        }

        mSpeciesRateFactorEvaluator = pModel.getSpeciesRateFactorEvaluator();

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

    protected void prepareTimesArray(double pStartTime,
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


    protected static final double computeReactionProbabilities(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                                                               SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                               double []pReactionProbabilities,
                                                               Reaction []pReactions) throws DataNotFoundException
    {
        // loop through all reactions, and for each reaction, compute the reaction probability
        int numReactions = pReactions.length;
        
        double aggregateReactionProbability = 0.0;

        Reaction reaction = null;
        double reactionProbability = 0.0;

        for(int reactionCtr = numReactions; --reactionCtr >= 0; )
        {
            reaction = pReactions[reactionCtr];

            reactionProbability = reaction.computeRate(pSpeciesRateFactorEvaluator, pSymbolEvaluator);

            // store reaction probability
            pReactionProbabilities[reactionCtr] = reactionProbability;

            // increment aggregate reaction probability
            aggregateReactionProbability += reactionProbability;
        }

        return(aggregateReactionProbability);
    }


}
