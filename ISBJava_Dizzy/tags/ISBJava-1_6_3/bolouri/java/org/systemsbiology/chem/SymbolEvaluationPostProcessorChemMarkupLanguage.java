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

public final class SymbolEvaluationPostProcessorChemMarkupLanguage extends SymbolEvaluationPostProcessor
{
    private static final int NULL_ARRAY_INDEX = Symbol.NULL_ARRAY_INDEX;

    private final HashMap mSpeciesCompartmentMap;        // maps species names to compartment names; provided by user
    private Symbol []mDynamicalSpeciesCompartmentSymbolsMap;
    private Symbol []mNonDynamicalSpeciesCompartmentSymbolsMap;
    private boolean []mDynamicalSpeciesCompartmentSymbolsInitialized;
    private boolean []mNonDynamicalSpeciesCompartmentSymbolsInitialized;
    private double []mGlobalSymbolsDoubleArray;
    private Value []mGlobalSymbolsValuesArray;

    public SymbolEvaluationPostProcessorChemMarkupLanguage(HashMap pSpeciesCompartmentMap)
    {
        mSpeciesCompartmentMap = pSpeciesCompartmentMap;
        mDynamicalSpeciesCompartmentSymbolsMap = null;
        mNonDynamicalSpeciesCompartmentSymbolsMap = null;
        mDynamicalSpeciesCompartmentSymbolsInitialized = null;
        mNonDynamicalSpeciesCompartmentSymbolsInitialized = null;
        mGlobalSymbolsDoubleArray = null;
        mGlobalSymbolsValuesArray = null;
    }

    private void initializeDynamicSymbolsArrays(int pNumDynamicSymbols)
    {
        mDynamicalSpeciesCompartmentSymbolsMap = new Symbol[pNumDynamicSymbols];
        mDynamicalSpeciesCompartmentSymbolsInitialized = new boolean[pNumDynamicSymbols];
        for(int i = 0; i < pNumDynamicSymbols; ++i)
        {
            mDynamicalSpeciesCompartmentSymbolsMap[i] = null;
            mDynamicalSpeciesCompartmentSymbolsInitialized[i] = false;
        }
    }

    private void initializeNonDynamicSymbolsArrays(int pNumNonDynamicSymbols)
    {

        mNonDynamicalSpeciesCompartmentSymbolsMap = new Symbol[pNumNonDynamicSymbols];
        mNonDynamicalSpeciesCompartmentSymbolsInitialized = new boolean[pNumNonDynamicSymbols];
        for(int i = 0; i < pNumNonDynamicSymbols; ++i)
        {
            mNonDynamicalSpeciesCompartmentSymbolsMap[i] = null;
            mNonDynamicalSpeciesCompartmentSymbolsInitialized[i] = false;
        }        
    }

    private Symbol getCompartmentSymbolForSpeciesSymbol(Symbol pSymbol, SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        String symbolName = pSymbol.getName();
        // get compartment name, if any
        String compartmentName = (String) mSpeciesCompartmentMap.get(symbolName);
        Symbol compartmentSymbol = null;
        if(null != compartmentName)
        {
            compartmentSymbol = pSymbolEvaluator.getSymbol(compartmentName);
            if(null == compartmentSymbol)
            {
                throw new DataNotFoundException("compartment symbol not found: \"" + compartmentName + "\"");
            }
        }
        return(compartmentSymbol);
    }

    public double modifyResult(Symbol pSymbol,
                               SymbolEvaluator pSymbolEvaluator,
                               double pSymbolValue) throws DataNotFoundException
    {
        int arrayIndex = pSymbol.getArrayIndex();
        Symbol compartmentSymbol = null;
        if(NULL_ARRAY_INDEX != arrayIndex)
        {
            double []doubleArray = pSymbol.getDoubleArray();
            if(null != doubleArray)
            {
                if(null == mDynamicalSpeciesCompartmentSymbolsInitialized)
                {
                    if(null != ((SymbolEvaluatorChem) pSymbolEvaluator).getSymbolsMap().get(pSymbol.getName()))
                    {
                        // it is a global symbol
                        initializeDynamicSymbolsArrays(doubleArray.length);
                        mGlobalSymbolsDoubleArray = doubleArray;
                    }
                    else
                    {
                        // it is a local symbol, which cannot be a species
                        return(pSymbolValue);
                    }
                }
                if(! doubleArray.equals(mGlobalSymbolsDoubleArray))
                {
                    return(pSymbolValue);
                }
                if(mDynamicalSpeciesCompartmentSymbolsInitialized[arrayIndex])
                {
                    compartmentSymbol = mDynamicalSpeciesCompartmentSymbolsMap[arrayIndex];
                }
                else
                {
                    compartmentSymbol = getCompartmentSymbolForSpeciesSymbol(pSymbol, (SymbolEvaluatorChem) pSymbolEvaluator);
                    mDynamicalSpeciesCompartmentSymbolsMap[arrayIndex] = compartmentSymbol;
                    mDynamicalSpeciesCompartmentSymbolsInitialized[arrayIndex] = true;
                }
            }
            else
            {
                Value []valueArray = pSymbol.getValueArray();
                if(null != valueArray)
                {
                    
                    if(null == mNonDynamicalSpeciesCompartmentSymbolsInitialized)
                    {
                        if(null != ((SymbolEvaluatorChem) pSymbolEvaluator).getSymbolsMap().get(pSymbol.getName()))
                        {
                            // it is a global symbol
                            initializeNonDynamicSymbolsArrays(valueArray.length);
                            mGlobalSymbolsValuesArray = valueArray;
                        }
                        else
                        {
                            // it is a local symbol, which cannot be a species
                            return(pSymbolValue);
                        }
                    }
                    if(! valueArray.equals(mGlobalSymbolsValuesArray))
                    {
                        return(pSymbolValue);
                    }
                    if(mNonDynamicalSpeciesCompartmentSymbolsInitialized[arrayIndex])
                    {
                        compartmentSymbol = mNonDynamicalSpeciesCompartmentSymbolsMap[arrayIndex];
                    }
                    else
                    {
                        compartmentSymbol = getCompartmentSymbolForSpeciesSymbol(pSymbol, (SymbolEvaluatorChem) pSymbolEvaluator);
                        mNonDynamicalSpeciesCompartmentSymbolsMap[arrayIndex] = compartmentSymbol;
                        mNonDynamicalSpeciesCompartmentSymbolsInitialized[arrayIndex] = true;
                    }
                }
                else
                {
                    throw new IllegalStateException("both the double array and value array are null, for the indexed symbol \"" + pSymbol.getName() + "\"");
                }
            }
        }
        if(null != compartmentSymbol)
        {
            pSymbolValue /= pSymbolEvaluator.getValue(compartmentSymbol);
        }
        return(pSymbolValue);
    }
}
