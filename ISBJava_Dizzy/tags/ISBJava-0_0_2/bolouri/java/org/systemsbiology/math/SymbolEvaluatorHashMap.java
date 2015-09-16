package org.systemsbiology.math;

import java.util.HashMap;
import org.systemsbiology.util.DataNotFoundException;

/**
 * An implementation of the {@link SymbolEvaluator} abstract
 * class based on a HashMap.  The string name in the
 * {@link Symbol} object is used as the key into the HashMap,
 * and the associated {@link Value} object is the value in the map.
 *
 * @author Stephen Ramsey
 */
public class SymbolEvaluatorHashMap extends SymbolEvaluator
{
    private HashMap mSymbolMap;

    public SymbolEvaluatorHashMap(HashMap pSymbolMap)
    {
        mSymbolMap = pSymbolMap;
    }

    void setSymbolMap(HashMap pSymbolMap)
    {
        mSymbolMap = pSymbolMap;
    }

    public double getValue(Symbol pSymbol) throws DataNotFoundException
    {
        String symbolName = pSymbol.getName();
        SymbolValue symbolValue = (SymbolValue) mSymbolMap.get(symbolName);
        double value = 0.0;
        if(null != symbolValue)
        {
            value = symbolValue.getValue().getValue(this);
        }
        else
        {
            throw new DataNotFoundException("unable to evaluate symbol: " + symbolName);
        }
        return(value);
    }

    public boolean hasValue(Symbol pSymbol)
    {
        String symbolName = pSymbol.getName();
        SymbolValue symbolValue = (SymbolValue) mSymbolMap.get(symbolName);
        return(null != symbolValue);
    }

    public Object clone()
    {
        SymbolEvaluatorHashMap newObj = new SymbolEvaluatorHashMap(mSymbolMap);
        return((Object) newObj);
    }
}
