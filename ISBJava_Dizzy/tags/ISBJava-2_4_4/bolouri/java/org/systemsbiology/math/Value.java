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

/**
 * Class that can represent a floating point value,
 * or an expression representing a floating point value.
 * An object of this class always contains either an 
 * {@link Expression} object or a {@link MutableDouble} object.  
 * 
 * @author Stephen Ramsey
 */
public final class Value
{
    private Expression mExpressionValue;
    private boolean mExpressionValueCached;
    private MutableDouble mNumericValue;

    /**
     * Constructs a {@link Value} composed of the 
     * specified {@link Expression}.
     */
    public Value(Expression pExpressionValue) throws IllegalArgumentException
    {
        setValue(pExpressionValue);
    }

    /**
     * Constructs a {@link Value} composed of the
     * specified floating-point value.
     */
    public Value(double pNumericValue)
    {
        setValue(pNumericValue);
    }

    public Expression getExpressionValue()
    {
        return(mExpressionValue);
    }

    /**
     * Stores the specified floating-point value.
     */
    public void setValue(double pValue) throws IllegalStateException
    {
        if(null == mNumericValue)
        {
            mNumericValue = new MutableDouble(pValue);
        }
        else
        {
            mNumericValue.setValue(pValue);
        }
        mExpressionValue = null;
        mExpressionValueCached = false;
    }

    /**
     * Stores the specified {@link Expression}.
     * If this object was constructed using a
     * floating-point value, an IllegalStateException is thrown.
     */
    public void setValue(Expression pExpressionValue) throws IllegalArgumentException
    {
        if(null == pExpressionValue)
        {
            throw new IllegalArgumentException("null expression object");
        }
        mExpressionValue = pExpressionValue;
        mExpressionValueCached = false;
        mNumericValue = new MutableDouble(0.0);
    }

    /**
     * Returns the floating-point value defined for this object.
     * If the object instead has an {@link Expression} stored
     * within it, an IllegalStateException is thrown.
     */
    public double getValue() throws IllegalStateException
    {
        if(null == mExpressionValue)
        {
            return(mNumericValue.doubleValue());
        }
        else
        {
            throw new IllegalStateException("this symbol value is an expression; must provide a SymbolValueMap");
        }
    }

    /**
     * Returns true if the object has an {@link Expression} object
     * stored within it, or false otherwise.
     */
    public boolean isExpression()
    {
        return(null != mExpressionValue);
    }

    /**
     * If this object contains an {@link Expression}, computes
     * the value of the Expression using the supplied 
     * {@link SymbolEvaluator}; otherwise it returns the
     * floating-point value stored in the internal 
     * MutableDouble object within this object.
     */
    public double getValueWithCaching(SymbolEvaluator pSymbolValueMap) throws DataNotFoundException
    {
        Expression expression = mExpressionValue;
        if(null != expression)
        {
            if(mExpressionValueCached)
            {
                return(mNumericValue.mDouble);
            }
            else
            {
                double value = expression.computeValue(pSymbolValueMap);
                mExpressionValueCached = true;
                mNumericValue.setValue(value);
                return(value);
            }
        }
        else
        {
            return(mNumericValue.mDouble);
        }
    }

    public double getValue(SymbolEvaluator pSymbolValueMap) throws DataNotFoundException
    {
        Expression expression = mExpressionValue;
        if(null != expression)
        {
            double value = expression.computeValue(pSymbolValueMap);
            return(value);
        }
        else
        {
            return(mNumericValue.mDouble);
        }
    }

    /**
     * Return a string representation of the 
     * {@link Expression} object stored in this class.
     * If no expression object is stored within this class,
     * an IllegalStateException is thrown.
     */
    public String getExpressionString() throws IllegalStateException
    {
        if(! isExpression())
        {
            throw new IllegalStateException("Value object does not have an Expression defined");
        }
        return(mExpressionValue.toString());
    }

    public String getExpressionString(Expression.SymbolPrinter pSymbolPrinter) throws IllegalStateException, DataNotFoundException
    {
        if(! isExpression())
        {
            throw new IllegalStateException("Value object does not have an Expression defined");
        }
        return(mExpressionValue.toString(pSymbolPrinter));
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if(isExpression())
        {
            sb.append("\"");
            sb.append(mExpressionValue.toString());
            sb.append("\"");
        }
        else
        {
            sb.append(mNumericValue);
        }
        return(sb.toString());
    }

    public void clearExpressionValueCache()
    {
        mExpressionValueCached = false;
    }

    public Object clone()
    {
        Value value = null;
        if(null != mExpressionValue)
        {
            value = new Value((Expression) mExpressionValue.clone());
        }
        else
        {
            value = new Value(mNumericValue.doubleValue());
        }
        return(value);
    }
}
