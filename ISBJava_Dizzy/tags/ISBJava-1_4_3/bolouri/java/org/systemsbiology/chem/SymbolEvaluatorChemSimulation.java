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

import java.util.HashMap;
import java.util.HashSet;

public class SymbolEvaluatorChemSimulation extends SymbolEvaluator
{
    public static final String SYMBOL_TIME = "time";
    public static final String SYMBOL_AVOGADRO = "Navo";

    private final HashMap mSymbolsMap;
    private static final HashSet sReservedSymbolNames;
    private double mTime;
    private HashMap mLocalSymbolsMap;
    private boolean mUseExpressionValueCaching;

    static
    {
        sReservedSymbolNames = new HashSet();
        getReservedSymbolNames(sReservedSymbolNames);
    }

    public static void getReservedSymbolNames(HashSet pReservedSymbolNames)
    {
        pReservedSymbolNames.add(SYMBOL_TIME);
        pReservedSymbolNames.add(SYMBOL_AVOGADRO);
    }

    public SymbolEvaluatorChemSimulation(HashMap pSymbolsMap, double pTime)
    {
        mSymbolsMap = pSymbolsMap;
        setTime(pTime);
    }

    final void setLocalSymbolsMap(HashMap pLocalSymbolsMap)
    {
        mLocalSymbolsMap = pLocalSymbolsMap;
    }

    final void setTime(double pTime)
    {
        mTime = pTime;
    }

    final double getTime()
    {
        return(mTime);
    }

    public final boolean hasValue(Symbol pSymbol) 
    {
        String symbolName = pSymbol.getName();
        return(null != mSymbolsMap.get(symbolName) ||
               isReservedSymbol(symbolName));
    }

    public void setUseExpressionValueCaching(boolean pUseExpressionValueCaching)
    {
        mUseExpressionValueCaching = pUseExpressionValueCaching;
    }

    private static final int NULL_ARRAY_INDEX = Symbol.NULL_ARRAY_INDEX;

    public final double getValue(Symbol pSymbol) throws DataNotFoundException
    {
        if(NULL_ARRAY_INDEX == pSymbol.getArrayIndex())
        {
            String symbolName = pSymbol.getName();
            if(sReservedSymbolNames.contains(symbolName))
            {
                if(symbolName.equals(SYMBOL_TIME))
                {
                    return(mTime);
                }
                else if(symbolName.equals(SYMBOL_AVOGADRO))
                {
                    return(Constants.AVOGADRO_CONSTANT);
                }
                else
                {
                    throw new IllegalStateException("unknown reserved symbol name: " + symbolName);
                }
            }
            else
            {
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
            }
        }

        if(mUseExpressionValueCaching)
        {
            return(pSymbol.getIndexedValueWithCaching(this));
        }
        else
        {
            return(pSymbol.getIndexedValue(this));
        }
    }


    public static boolean isReservedSymbol(String pSymbolName)
    {
        return(sReservedSymbolNames.contains(pSymbolName));
    }

    public Object clone()
    {
        SymbolEvaluatorChemSimulation newObj = new SymbolEvaluatorChemSimulation(mSymbolsMap, mTime);
        return((Object) newObj);
    }
}
