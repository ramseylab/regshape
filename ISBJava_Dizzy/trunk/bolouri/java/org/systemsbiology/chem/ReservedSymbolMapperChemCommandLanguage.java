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

public final class ReservedSymbolMapperChemCommandLanguage extends ReservedSymbolMapper
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

    public static boolean isReservedSymbol(String pSymbolName)
    {
        return(sReservedSymbolNames.contains(pSymbolName));
    }

    public boolean isReservedSymbol(Symbol pSymbol)
    {
        return(sReservedSymbolNames.contains(pSymbol.getName()));
    }

    public double getReservedSymbolValue(Symbol pSymbol, SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        String symbolName = pSymbol.getName();
        if(symbolName.equals(SYMBOL_AVOGADRO))
        {
            return(Constants.AVOGADRO_CONSTANT);
        }
        else if(symbolName.equals(SYMBOL_TIME))
        {
            return(((SymbolEvaluatorChem) pSymbolEvaluator).getTime());
        }
        else
        {
            throw new DataNotFoundException("symbol is not a reserved symbol: \"" + symbolName + "\"");
        }
    }
}
