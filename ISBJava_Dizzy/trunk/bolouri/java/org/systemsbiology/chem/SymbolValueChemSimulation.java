package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.HashMap;
import org.systemsbiology.math.SymbolValue;

public class SymbolValueChemSimulation
{
    public static final void addSymbolValueToMap(HashMap pMap, String pSymbolName, SymbolValue pObject) throws IllegalArgumentException, IllegalStateException
    {
        if(SymbolEvaluatorChemCommandLanguage.isReservedSymbol(pObject.getSymbol().getName()))
        {
            throw new IllegalArgumentException("reserved symbol used: " + pObject.getSymbol().getName());
        }
        pObject.addSymbolToMap(pMap, pSymbolName);
    }
    
}
