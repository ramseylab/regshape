package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * Contains a string identifier and an (optional)
 * array index.  Used by the {@link ISymbolDoubleMap} class.
 * The array index can be used instead of the string identifier,
 * in order to find the symbol's value.
 *
 * @author Stephen Ramsey
 */
public class Symbol
{
    private final String mSymbolName;
    private int mArrayIndex;
    private double []mDoubleArray;
    private Value []mValueArray;

    public static final int NULL_ARRAY_INDEX = -1;
    
    public Symbol(String pSymbolName) throws IllegalArgumentException
    {
        mSymbolName = pSymbolName;
        clearIndexInfo();
    }

    public boolean hasArrayIndex()
    {
        return(NULL_ARRAY_INDEX != mArrayIndex);
    }

    public String getName()
    {
        return(mSymbolName);
    }

    public void setArrayIndex(int pArrayIndex)
    {
        mArrayIndex = pArrayIndex;
    }

    public int getArrayIndex()
    {
        return(mArrayIndex);
    }

    public void setArray(double []pArray)
    {
        mDoubleArray = pArray;
        mValueArray = null;
    }

    public void setArray(Value []pArray)
    {
        mDoubleArray = null;
        mValueArray = pArray;
    }

    public double []getDoubleArray()
    {
        return(mDoubleArray);
    }

    public Value []getValueArray()
    {
        return(mValueArray);
    }

    public boolean equals(Symbol pSymbol)
    {
        return( mSymbolName.equals(pSymbol.mSymbolName) &&
                mArrayIndex == pSymbol.mArrayIndex );
    }

    public Object clone()
    {
        Symbol newSymbol = new Symbol(mSymbolName);
        return(newSymbol);
    }

    public void copyIndexInfo(Symbol pSymbol)
    {
        assert pSymbol.getName().equals(mSymbolName) : "inconsistent symbol names";
        mArrayIndex = pSymbol.mArrayIndex;
        mDoubleArray = pSymbol.mDoubleArray;
        mValueArray = pSymbol.mValueArray;
    }

    public void clearIndexInfo()
    {
        mArrayIndex = NULL_ARRAY_INDEX;
        mDoubleArray = null;
        mValueArray = null;        
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getName() + "; index: " + mArrayIndex + "; array: " + mDoubleArray);
        return(sb.toString());
    }
}
