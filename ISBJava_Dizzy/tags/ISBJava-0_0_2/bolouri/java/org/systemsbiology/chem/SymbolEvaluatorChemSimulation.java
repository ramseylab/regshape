
package org.systemsbiology.chem;

import org.systemsbiology.math.SymbolEvaluator;
import org.systemsbiology.math.Symbol;
import org.systemsbiology.math.Value;
import org.systemsbiology.util.DataNotFoundException;

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

    static
    {
        sReservedSymbolNames = new HashSet();
        sReservedSymbolNames.add(SYMBOL_TIME);
        sReservedSymbolNames.add(SYMBOL_AVOGADRO);
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

    public final double getValue(Symbol pSymbol) throws DataNotFoundException
    {
        boolean hasValue = false;
        double value = 0.0;

        int arrayIndex = pSymbol.getArrayIndex();
        if(Symbol.NULL_ARRAY_INDEX == arrayIndex)
        {
            String symbolName = pSymbol.getName();

            if(symbolName.equals(SYMBOL_TIME))
            {
                value = mTime;
                hasValue = true;
            }
            else if(symbolName.equals(SYMBOL_AVOGADRO))
            {
                value = Constants.AVOGADRO_CONSTANT;
                hasValue = true;
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
                arrayIndex = pSymbol.getArrayIndex();
            }
        }

        if(! hasValue)
        {
            double []doubleArray = pSymbol.getDoubleArray();

            assert (arrayIndex >= 0) : "invalid (negative) array index";

            if(null != doubleArray)
            {
                value = doubleArray[arrayIndex];
            }
            else
            {
                Value []valueArray = pSymbol.getValueArray();
                assert (null != valueArray) : "no value or double array defined for indexed symbol";
                Value valueObj = valueArray[arrayIndex];
                value = valueObj.getValue(this);
            }
        }

        return(value);
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
