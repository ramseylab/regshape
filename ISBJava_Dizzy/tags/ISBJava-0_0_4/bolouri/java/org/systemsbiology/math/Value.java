package org.systemsbiology.math;

import org.systemsbiology.util.DataNotFoundException;

/**
 * Class that can represent a floating point value,
 * or an expression representing a floating point value.
 * An object of this class always contains either an 
 * {@link Expression} object or a {@link MutableDouble} object.  
 * 
 * @author Stephen Ramsey
 */
public class Value
{
    private Expression mExpressionValue;
    private MutableDouble mNumericValue;

    /**
     * Constructs a {@link Value} composed of the 
     * specified {@link Expression}.
     */
    public Value(Expression pExpressionValue) throws IllegalArgumentException
    {
        setValue(pExpressionValue);
        mNumericValue = null;
    }

    /**
     * Constructs a {@link Value} composed of the
     * specified floating-point value.
     */
    public Value(double pNumericValue)
    {
        mExpressionValue = null;
        mNumericValue = new MutableDouble(pNumericValue);
    }

    /**
     * Stores the specified floating-point value.
     * If this object was constructed using an
     * {@link Expression}, an IllegalStateException is thrown.
     */
    public void setValue(double pValue) throws IllegalStateException
    {
        if(null != mExpressionValue)
        {
            throw new IllegalStateException("this symbol value is an expression; value cannot be set from a double type");
        }
        else
        {
            mNumericValue.setValue(pValue);
        }
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
    }

    /**
     * Returns the floating-point value defined for this object.
     * If the object instead has an {@link Expression} stored
     * within it, an IllegalStateException is thrown.
     */
    public final double getValue() throws IllegalStateException
    {
        double value = 0.0;
        if(null != mExpressionValue)
        {
            throw new IllegalStateException("this symbol value is an expression; must provide a SymbolValueMap");
        }
        else
        {
            value = mNumericValue.doubleValue();
        }
        return(value);        
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
    public double getValue(SymbolEvaluator pSymbolValueMap) throws DataNotFoundException
    {
        double value = 0.0;
        if(null != mExpressionValue)
        {
            value = mExpressionValue.computeValue(pSymbolValueMap);
        }
        else
        {
            value = mNumericValue.doubleValue();
        }
        return(value);
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
