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

/**
 * Abstract Symbol Evaluator class used for chemical simulations.
 * 
 * @author Stephen Ramsey
 */
public abstract class SymbolEvaluatorChem extends SymbolEvaluator
{
    protected HashMap mSymbolsMap;
    protected double mTime;
    protected HashMap mLocalSymbolsMap;

    public SymbolEvaluatorChem()
    {
        // do nothing
    }

    public void setSymbolsMap(HashMap pSymbolsMap) throws DataNotFoundException
    {
        mSymbolsMap = pSymbolsMap;
    }

    public void setLocalSymbolsMap(HashMap pLocalSymbolsMap)
    {
        mLocalSymbolsMap = pLocalSymbolsMap;
    }

    public final void setTime(double pTime)
    {
        mTime = pTime;
    }

    public final double getTime()
    {
        return(mTime);
    }

    HashMap getSymbolsMap()
    {
        return(mSymbolsMap);
    }
    
    public Expression getExpressionValue(Symbol pSymbol) throws DataNotFoundException
    {
        Expression retVal = null;
        String symbolName = pSymbol.getName();
        Symbol symbol = (Symbol) mSymbolsMap.get(symbolName);
        if(null == symbol)
        {
            throw new DataNotFoundException("unable to find symbol in symbol map, symbol is \"" + symbolName + "\"");
        }
        Value []valueArray = symbol.getValueArray();
        int arrayIndex = symbol.getArrayIndex();
        if(NULL_ARRAY_INDEX == arrayIndex)
        {
            throw new IllegalStateException("null array index for symbol \"" + symbolName + "\"");
        }
        if(null != valueArray)
        {
            Value value = valueArray[arrayIndex];
            if(null == value)
            {
                throw new IllegalStateException("unexpected null value for symbol: " + symbolName);
            }
            if(value.isExpression())
            {
                retVal = value.getExpressionValue();
            }
        }
        return(retVal);
    }    
}
