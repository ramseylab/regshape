package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.regex.Pattern;

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

    private static final String VALID_SYMBOL_REGEX = "^[_a-zA-Z]([_a-zA-Z0-9])*$";
    private static final Pattern VALID_SYMBOL_PATTERN = Pattern.compile(VALID_SYMBOL_REGEX);

    public static final int NULL_ARRAY_INDEX = -1;
    
    public static boolean isValidSymbol(String pSymbolName)
    {
        return(VALID_SYMBOL_PATTERN.matcher(pSymbolName).matches());
    }

    public Symbol(String pSymbolName) throws IllegalArgumentException
    {
        if(! isValidSymbol(pSymbolName))
        {
            throw new IllegalArgumentException("invalid symbol name: " + pSymbolName);
        }
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
