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

public class SymbolEvaluatorChemCommandLanguage extends SymbolEvaluatorChem
{
    public static final String SYMBOL_TIME = "time";
    public static final String SYMBOL_AVOGADRO = "Navo";

    private static final HashSet sReservedSymbolNames;

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

    public SymbolEvaluatorChemCommandLanguage()
    {
        // do nothing
    }

    public final double getUnindexedValue(Symbol pSymbol) throws DataNotFoundException, IllegalStateException
    {
        if(NULL_ARRAY_INDEX != pSymbol.getArrayIndex())
        {
            throw new IllegalStateException("getUnindexedValue() was called on symbol with non-null array index: " + pSymbol.getName());
        }
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
        assert (Symbol.NULL_ARRAY_INDEX != pSymbol.getArrayIndex()) : "null array index found";
        return(getValue(pSymbol));
    }

    public static boolean isReservedSymbol(String pSymbolName)
    {
        return(sReservedSymbolNames.contains(pSymbolName));
    }

    public final boolean hasValue(Symbol pSymbol) 
    {
        String symbolName = pSymbol.getName();
        return(null != mSymbolsMap.get(symbolName) ||
               isReservedSymbol(symbolName));
    }

}
