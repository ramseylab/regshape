package org.systemsbiology.math;

import java.util.HashMap;

/**
 * Represents a {@link Symbol} and an associated
 * {@link Value}.  It is the base class for many
 * other classes such as {@link org.systemsbiology.chem.Species}.
 *
 * @author Stephen Ramsey
 */
public class SymbolValue implements Comparable
{
    protected Value mValue;
    protected Symbol mSymbol;

    /**
     * Constructs a SymbolValue using the
     * specified string symbol name.  The {@link Value}.
     * object is set to null.
     */
    public SymbolValue(String pSymbolName)
    {
        this(pSymbolName, null);
    }

    /**
     * The copy constructor.
     */
    public SymbolValue(SymbolValue pSymbolValue)
    {
        mValue = pSymbolValue.mValue;
        mSymbol = pSymbolValue.mSymbol;
    }

    /**
     * Constructs a SymbolValue using the specified
     * string symbol name, and the specified {@link Value}
     * object.
     */
    public SymbolValue(String pSymbolName, Value pValue)
    {
        setSymbol(new Symbol(pSymbolName));
        setValue(pValue);
    }

    /**
     * Adds itself to the specified HashMap, using the string symbol
     * name specified as <code>pSymbolName</code>.
     */
    public final void addSymbolToMap(HashMap pMap, String pSymbolName) throws IllegalStateException
    {
        SymbolValue foundObject = (SymbolValue) pMap.get(pSymbolName);
        if(null != foundObject)
        {
            if(! foundObject.equals(this))
            {
                throw new IllegalStateException("inconsistent object found with identical symbol names: " + pSymbolName + "; " + foundObject.getSymbol().getName());
            }
        }
        else
        {
            pMap.put(pSymbolName, this);
        }
    }

    public boolean equals(SymbolValue pSymbolValue)
    {
        return(mValue.equals(pSymbolValue.mValue) &&
               mSymbol.equals(pSymbolValue.mSymbol));
    }

    /**
     * Accessor for the {@link Value} object stored in
     * this object.
     */
    public final Value getValue()
    {
        return(mValue);
    }

    protected void setValue(Value pValue)
    {
        mValue = pValue;
    }

    /**
     * Accessor for the {@link Symbol} object stored in
     * this object.
     */
    public Symbol getSymbol()
    {
        return(mSymbol);
    }

    protected void setSymbol(Symbol pSymbol)
    {
        mSymbol = pSymbol;
    }

    public int compareTo(Object pObject)
    {
        return(mSymbol.getName().compareTo(((SymbolValue) pObject).mSymbol.getName()));
    }
}
