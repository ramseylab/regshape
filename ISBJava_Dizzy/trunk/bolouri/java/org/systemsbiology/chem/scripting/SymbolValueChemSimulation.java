package org.systemsbiology.chem.scripting;

import org.systemsbiology.chem.SymbolEvaluatorChemSimulation;
import java.util.HashMap;
import org.systemsbiology.math.SymbolValue;

public class SymbolValueChemSimulation
{
    public static final void addSymbolValueToMap(HashMap pMap, String pSymbolName, SymbolValue pObject) throws IllegalArgumentException, IllegalStateException
    {
        if(SymbolEvaluatorChemSimulation.isReservedSymbol(pObject.getSymbol().getName()))
        {
            throw new IllegalArgumentException("reserved symbol used: " + pObject.getSymbol().getName());
        }
        pObject.addSymbolToMap(pMap, pSymbolName);
    }
    
}
