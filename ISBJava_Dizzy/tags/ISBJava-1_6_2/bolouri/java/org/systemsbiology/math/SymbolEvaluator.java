package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.DataNotFoundException;

/*
 * Abstract class defining the interface for a class that
 * can convert a {@link Symbol} into a floating-point value.
 * An abstract class is used instead of an interface, for 
 * reasons of speed.
 *
 * @author Stephen Ramsey
 */
public abstract class SymbolEvaluator implements Cloneable
{
    protected static final int NULL_ARRAY_INDEX = Symbol.NULL_ARRAY_INDEX;

    private final boolean mUseExpressionValueCaching;
    private final SymbolEvaluationPostProcessor mSymbolEvaluationPostProcessor;

    public SymbolEvaluator()
    {
        this(false, null);
    }

    public SymbolEvaluationPostProcessor getSymbolEvaluationPostProcessor()
    {
        return(mSymbolEvaluationPostProcessor);
    }

    public SymbolEvaluator(boolean pUseExpressionValueCaching,
                           SymbolEvaluationPostProcessor pSymbolEvaluationPostProcessor)
    {
        mUseExpressionValueCaching = pUseExpressionValueCaching;
        mSymbolEvaluationPostProcessor = pSymbolEvaluationPostProcessor;
    }

    protected final double getIndexedValue(int pArrayIndex, Symbol pSymbol) throws DataNotFoundException
    {
        double []doubleArray = pSymbol.mDoubleArray;
        if(null != doubleArray)
        {
            if(null == mSymbolEvaluationPostProcessor)
            {
                return(doubleArray[pArrayIndex]);
            }
            else
            {
                return(mSymbolEvaluationPostProcessor.modifyResult(pSymbol, this, doubleArray[pArrayIndex]));
            }
        }
        else
        {
            if(! mUseExpressionValueCaching)
            {
                if(null == mSymbolEvaluationPostProcessor)
                {
                    return(pSymbol.mValueArray[pArrayIndex].getValue(this));
                }
                else
                {
                    return(mSymbolEvaluationPostProcessor.modifyResult(pSymbol, this, pSymbol.mValueArray[pArrayIndex].getValue(this)));
                }
            }
            else
            {
                if(null == mSymbolEvaluationPostProcessor)
                {
                    return(pSymbol.mValueArray[pArrayIndex].getValueWithCaching(this));
                }
                else
                {
                    return(mSymbolEvaluationPostProcessor.modifyResult(pSymbol, this, pSymbol.mValueArray[pArrayIndex].getValueWithCaching(this)));
                }
            }
        }
    }

    /**
     * Returns null if the symbol corresponds to a numeric value; or
     * returns the Expression if the symbol corresponds to an Expression;
     * or throws an exception if the symbol is not defined.
     */
    public abstract Expression getExpressionValue(Symbol pSymbol) throws DataNotFoundException;


    /**
     * Returns the floating-point value associated with the specified
     * {@link Symbol}.
     * 
     * @param pSymbol the {@link Symbol} object for which the value is to be 
     * returned
     *
     * @return the floating-point value associated with the specified
     * symbol.
     */
    /*
     * NOTE:  the code in this function is nasty-looking and repetitive because 
     * it has been optimized for speed.
     */
    public final double getValue(Symbol pSymbol) throws DataNotFoundException
    {
        if(NULL_ARRAY_INDEX == pSymbol.mArrayIndex)
        {
            if(null == mSymbolEvaluationPostProcessor)
            {
                return(getUnindexedValue(pSymbol));
            }
            else
            {
                return(mSymbolEvaluationPostProcessor.modifyResult(pSymbol, this, getUnindexedValue(pSymbol)));
            }
        }
        else
        {
            if(null != pSymbol.mDoubleArray)
            {
                if(null == mSymbolEvaluationPostProcessor)
                {
                    return(pSymbol.mDoubleArray[pSymbol.mArrayIndex]);
                }
                else
                {
                    return(mSymbolEvaluationPostProcessor.modifyResult(pSymbol, this, pSymbol.mDoubleArray[pSymbol.mArrayIndex]));
                }
            }
            else
            {
                if(! mUseExpressionValueCaching)
                {
                    if(null == mSymbolEvaluationPostProcessor)
                    {
                        return(pSymbol.mValueArray[pSymbol.mArrayIndex].getValue(this));
                    }
                    else
                    {
                        return(mSymbolEvaluationPostProcessor.modifyResult(pSymbol, this, pSymbol.mValueArray[pSymbol.mArrayIndex].getValue(this)));
                    }
                }
                else
                {
                    if(null == mSymbolEvaluationPostProcessor)
                    {
                        return(pSymbol.mValueArray[pSymbol.mArrayIndex].getValueWithCaching(this));
                    }
                    else
                    {
                        return(mSymbolEvaluationPostProcessor.modifyResult(pSymbol, this, pSymbol.mValueArray[pSymbol.mArrayIndex].getValueWithCaching(this)));
                    }
                }
            }        
        }
    }

    /**
     * Returns true if the object has a {@link Value} defined,
     * or false otherwise.
     *
     * @return true if the object has a {@link Value} defined,
     * or false otherwise.
     */
    public abstract boolean hasValue(Symbol pSymbol);

    public abstract double getUnindexedValue(Symbol pSymbol) throws DataNotFoundException;
}
