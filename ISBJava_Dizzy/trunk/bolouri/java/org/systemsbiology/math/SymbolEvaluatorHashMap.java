package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

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
    protected final HashMap mSymbolMap;

    public SymbolEvaluatorHashMap(HashMap pSymbolMap)
    {
        super();
        mSymbolMap = pSymbolMap;
    }

//    public void setSymbolsMap(HashMap pSymbolMap)
//    {
//        mSymbolMap = pSymbolMap;
//    }

    protected double getValue(String pSymbolName) throws DataNotFoundException
    {
        SymbolValue symbolValue = (SymbolValue) mSymbolMap.get(pSymbolName);
        double value = 0.0;

        if(null != symbolValue)
        {
            Value valueObj = symbolValue.getValue();
            if(null == valueObj)
            {
                throw new IllegalStateException("no value was assigned for symbol: " + symbolValue.getSymbol().getName());
            }
            value = symbolValue.getValue().getValue(this);
        }
        else
        {
            throw new DataNotFoundException("unable to evaluate symbol: " + pSymbolName);
        }
        return(value);
    }

    public double getUnindexedValue(Symbol pSymbol) throws DataNotFoundException
    {
        return(getValue(pSymbol.getName()));
    }

    public boolean hasValue(Symbol pSymbol)
    {
        String symbolName = pSymbol.getName();
        SymbolValue symbolValue = (SymbolValue) mSymbolMap.get(symbolName);
        return(null != symbolValue);
    }

    public Expression getExpressionValue(Symbol pSymbol) throws DataNotFoundException
    {
        String symbolName = pSymbol.getName();
        SymbolValue symbolValue = (SymbolValue) mSymbolMap.get(symbolName);
        if(null == symbolValue)
        {
            throw new DataNotFoundException("unable to find symbol in symbol map, symbol is \"" + symbolName + "\"");
        }
        Value value = symbolValue.getValue();
        Expression retVal = null;
        if(null != value)
        {
            if(value.isExpression())
            {
                retVal = value.getExpressionValue();
            }
        }
        return(retVal);
    }
}
