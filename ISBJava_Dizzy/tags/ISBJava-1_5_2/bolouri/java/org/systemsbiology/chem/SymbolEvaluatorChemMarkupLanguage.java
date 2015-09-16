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

public class SymbolEvaluatorChemMarkupLanguage extends SymbolEvaluatorChem
{
    private final HashMap mSpeciesCompartmentMap;        // maps species names to compartment names; provided by user
    private Symbol []mDynamicalSpeciesCompartmentSymbolsMap;
    private Symbol []mNonDynamicalSpeciesCompartmentSymbolsMap;

    public SymbolEvaluatorChemMarkupLanguage(HashMap pSpeciesCompartmentMap)
    {
        mSpeciesCompartmentMap = pSpeciesCompartmentMap;
        mDynamicalSpeciesCompartmentSymbolsMap = null;
        mNonDynamicalSpeciesCompartmentSymbolsMap = null;
    }

    public void setSymbolsMap(HashMap pSymbolsMap) throws DataNotFoundException
    {
        super.setSymbolsMap(pSymbolsMap);
        initializeSpeciesCompartmentSymbolsArrays();
    }

    private void initializeSpeciesCompartmentSymbolsArrays() throws DataNotFoundException
    {
        int numSymbols = mSymbolsMap.keySet().size();
        mDynamicalSpeciesCompartmentSymbolsMap = new Symbol[numSymbols];
        mNonDynamicalSpeciesCompartmentSymbolsMap = new Symbol[numSymbols];
        for(int i = 0; i < numSymbols; ++i)
        {
            mDynamicalSpeciesCompartmentSymbolsMap[i] = null;
            mNonDynamicalSpeciesCompartmentSymbolsMap[i] = null;
        }
    }

    public final double getUnindexedValue(Symbol pSymbol) throws DataNotFoundException, IllegalStateException
    {
        int arrayIndex = pSymbol.getArrayIndex();
        if(NULL_ARRAY_INDEX != arrayIndex)
        {
            throw new IllegalStateException("getUnindexedValue() was called on symbol with non-null array index: " + pSymbol.getName());
        }
        String symbolName = pSymbol.getName();

        Symbol indexedSymbol = null;
        if(null != mLocalSymbolsMap)
        {
            indexedSymbol = (Symbol) mLocalSymbolsMap.get(symbolName);
        }
        if(null == indexedSymbol)
        {
            indexedSymbol = (Symbol) mSymbolsMap.get(symbolName);
        }
        if(null == indexedSymbol)
        {
            throw new DataNotFoundException("unable to obtain value for symbol: " + symbolName);
        }
        pSymbol.copyIndexInfo(indexedSymbol);
        
        assert (Symbol.NULL_ARRAY_INDEX != pSymbol.getArrayIndex()) : "null array index found";

        String compartmentName = (String) mSpeciesCompartmentMap.get(symbolName);
        if(null != compartmentName)
        {
            // this symbol is a species; make sure we store the species-to-compartment association
            Symbol compartmentSymbol = (Symbol) mSymbolsMap.get(compartmentName);
            if(null == compartmentSymbol)
            {
                throw new DataNotFoundException("cound not find Symbol for compartment name \"" + compartmentName + "\"");
            }
            
            arrayIndex = pSymbol.getArrayIndex();
            double []doubleArray = pSymbol.getDoubleArray();
            if(null != doubleArray)
            {
                mDynamicalSpeciesCompartmentSymbolsMap[arrayIndex] = compartmentSymbol;
            }
            else
            {
                Value []valueArray = pSymbol.getValueArray();
                mNonDynamicalSpeciesCompartmentSymbolsMap[arrayIndex] = compartmentSymbol;
            }
        }

        return(getValue(pSymbol));
    }

    public static boolean isReservedSymbol(String pSymbolName)
    {
        return(false);
    }

    public final double getValue(Symbol pSymbol) throws DataNotFoundException
    {
        int arrayIndex = pSymbol.getArrayIndex();
        if(NULL_ARRAY_INDEX == arrayIndex)
        {
            return(getUnindexedValue(pSymbol));
        }
        else
        {
            double symbolValue = getIndexedValue(arrayIndex, pSymbol);
            Symbol compartmentSymbol = null;
            double []doubleArray = pSymbol.getDoubleArray();
            if(null != doubleArray)
            {
                compartmentSymbol = mDynamicalSpeciesCompartmentSymbolsMap[arrayIndex];
            }
            else
            {
                compartmentSymbol = mNonDynamicalSpeciesCompartmentSymbolsMap[arrayIndex];
            }
            if(null != compartmentSymbol)
            {
                symbolValue /= getValue(compartmentSymbol);
            }
            return(symbolValue);
        }
    }

    public final boolean hasValue(Symbol pSymbol) 
    {
        String symbolName = pSymbol.getName();
        return(null != mSymbolsMap.get(symbolName) ||
               isReservedSymbol(symbolName));
    }
}
