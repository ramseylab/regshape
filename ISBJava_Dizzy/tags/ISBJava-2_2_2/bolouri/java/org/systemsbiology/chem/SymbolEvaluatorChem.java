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

/**
 * Symbol Evaluator class used for chemical simulations.
 * 
 * @author Stephen Ramsey
 */
public final class SymbolEvaluatorChem extends SymbolEvaluator
{
    private final HashMap mSymbolsMap;
    private double mTime;
    private HashMap mLocalSymbolsMap;
    private final ReservedSymbolMapper mReservedSymbolMapper;

    public SymbolEvaluatorChem(boolean pUseExpressionValueCaching,
                               SymbolEvaluationPostProcessor pSymbolEvaluationPostProcessor,
                               ReservedSymbolMapper pReservedSymbolMapper,
                               HashMap pSymbolsMap)
    {
        super(pUseExpressionValueCaching, pSymbolEvaluationPostProcessor);
        mReservedSymbolMapper = pReservedSymbolMapper;
        mSymbolsMap = pSymbolsMap;
    }

    public ReservedSymbolMapper getReservedSymbolMapper()
    {
        return(mReservedSymbolMapper);
    }

    public void setLocalSymbolsMap(HashMap pLocalSymbolsMap)
    {
        mLocalSymbolsMap = pLocalSymbolsMap;
    }

    public void setTime(double pTime)
    {
        mTime = pTime;
    }

    public double getTime()
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
            symbol = (Symbol) mLocalSymbolsMap.get(symbolName);
            if(null == symbol)
            {
                throw new DataNotFoundException("unable to find symbol in symbol map, symbol is \"" + symbolName + "\"");
            }
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

    public double getUnindexedValue(Symbol pSymbol) throws DataNotFoundException, IllegalStateException
    {
        
        if(NULL_ARRAY_INDEX != pSymbol.getArrayIndex())
        {
            throw new IllegalStateException("getUnindexedValue() was called on symbol with non-null array index: " + pSymbol.getName());
        }

        if(null != mReservedSymbolMapper)
        {
            if(mReservedSymbolMapper.isReservedSymbol(pSymbol))
            {
                return(mReservedSymbolMapper.getReservedSymbolValue(pSymbol, this));
            }
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
        return(getValue(pSymbol));
    }

    public boolean hasValue(Symbol pSymbol) 
    {
        boolean hasValue = false;
        if(null != mReservedSymbolMapper )
        {
            if(mReservedSymbolMapper.isReservedSymbol(pSymbol))
            {
                hasValue = true;
            }
        }
        if(! hasValue)
        {
            if(null != mSymbolsMap.get(pSymbol.getName()))
            {
                hasValue = true;
            }
        }
        return(hasValue);
    }

    public Symbol getSymbol(String pSymbolName)
    {
        return((Symbol) mSymbolsMap.get(pSymbolName));
    }
}
