package org.systemsbiology.chem;

import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.systemsbiology.math.SymbolValue;
import org.systemsbiology.util.DebugUtils;

/**
 * A named collection of {@link Reaction} and
 * {@link Parameter} objects, which represents
 * a system of interacting chemical {@link Species}.
 * The chemical Species are contained in the Reaction
 * objects.
 *
 * @author Stephen Ramsey
 */
public class Model
{
    private HashMap mReactionsMap;
    private String mName;
    private HashMap mDynamicSymbolsMap;
    private HashMap mSymbolsMap;
    private HashMap mParametersMap;
    private SpeciesRateFactorEvaluator mSpeciesRateFactorEvaluator;

    public Model(String pName)
    {
        mName = pName;
        mReactionsMap = new HashMap();
        mDynamicSymbolsMap = new HashMap();
        mSymbolsMap = new HashMap();
        mParametersMap = new HashMap();
        setSpeciesRateFactorEvaluator(new SpeciesRateFactorEvaluatorCombinatoric());
    }

    public String getName()
    {
        return(mName);
    }

    public void setSpeciesRateFactorEvaluator(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator)
    {
        mSpeciesRateFactorEvaluator = pSpeciesRateFactorEvaluator;
    }

    public SpeciesRateFactorEvaluator getSpeciesRateFactorEvaluator()
    {
        return(mSpeciesRateFactorEvaluator);
    }

    HashMap getSymbolsMap()
    {
        return(mSymbolsMap);
    }

    HashMap getDynamicSymbolsMap()
    {
        return(mDynamicSymbolsMap);
    }

    Reaction []constructReactionsArray()
    {
        Reaction []sampleArray = new Reaction[0];
        return( (Reaction[]) getReactionsMap().values().toArray(sampleArray) );
    }

    Species []constructDynamicSymbolsArray()
    {
        Species []sampleArray = new Species[0];
        return( (Species []) getDynamicSymbolsMap().values().toArray(sampleArray) );
    }

    SymbolValue []constructGlobalNonDynamicSymbolsArray()
    {
        HashMap symbolsMap = getSymbolsMap();
        HashMap dynamicSpeciesMap = getDynamicSymbolsMap();
        Iterator symbolsIter = symbolsMap.keySet().iterator();
        ArrayList retList = new ArrayList();
        while(symbolsIter.hasNext())
        {
            String symbolName = (String) symbolsIter.next();
            if(null == dynamicSpeciesMap.get(symbolName))
            {
                retList.add((SymbolValue) symbolsMap.get(symbolName));
            }
        }
        SymbolValue []sampleArray = new SymbolValue[0];
        return( (SymbolValue []) retList.toArray(sampleArray) );
    }

    SymbolValue getSymbolByName(String pSymbolName)
    {
        return((SymbolValue) getSymbolsMap().get(pSymbolName));
    }

    HashMap getReactionsMap()
    {
        return(mReactionsMap);
    }

    public void addParameter(Parameter pParameter)
    {
        SymbolValueChemSimulation.addSymbolValueToMap(mSymbolsMap, pParameter.getSymbolName(), pParameter);
        SymbolValueChemSimulation.addSymbolValueToMap(mParametersMap, pParameter.getSymbolName(), pParameter);
    }

    public void addSpecies(Species pSpecies)
    {
        SymbolValueChemSimulation.addSymbolValueToMap(mSymbolsMap, pSpecies.getName(), pSpecies);
    }

    public Collection getDynamicSymbols()
    {
        return(mDynamicSymbolsMap.values());
    }

    public Collection getReactions()
    {
        return(mReactionsMap.values());
    }

    public Collection getSymbols()
    {
        return(mSymbolsMap.values());
    }

    /**
     * It is illegal to add the reaction with a given name, twice
     */
    public void addReaction(Reaction pReaction) throws IllegalStateException
    {
        String reactionName = pReaction.getName();

        Reaction storedReaction = (Reaction) mReactionsMap.get(reactionName);
        if(null != storedReaction)
        {
            throw new IllegalStateException("reaction is already added to this model: " + reactionName);
        }
        else
        {
            mReactionsMap.put(reactionName, pReaction);
        }

        pReaction.addDynamicSpeciesToGlobalSpeciesMap(getDynamicSymbolsMap());
        pReaction.addSymbolsToGlobalSymbolMap(getSymbolsMap());
    }

    public String []getOrderedSpeciesNamesArray()
    {
        List speciesNamesList = new LinkedList();
        Iterator symbolValuesIter = mSymbolsMap.values().iterator();
        while(symbolValuesIter.hasNext())
        {
            SymbolValue symbolValue = (SymbolValue) symbolValuesIter.next();
            if(symbolValue instanceof Species)
            {
                speciesNamesList.add(symbolValue.getSymbol().getName());
            }
        }
        Collections.sort(speciesNamesList);
        return((String []) speciesNamesList.toArray(new String[0]));
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Model: ");
        sb.append(getName());
        sb.append("\n\n");
        sb.append("Parameters: \n");
        DebugUtils.describeSortedObjectList(sb, mParametersMap);
        sb.append("\n\n");
        sb.append("Compartments: \n");
        DebugUtils.describeSortedObjectList(sb, mSymbolsMap, Compartment.class);
        sb.append("\n\n");
        sb.append("Reactions: \n");
        String separatorString = ",\n\n";
        DebugUtils.describeSortedObjectList(sb, mReactionsMap, separatorString);
        return(sb.toString());
    }
}
