package org.systemsbiology.math;

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
    public abstract double getValue(Symbol pSymbol) throws DataNotFoundException;

    /**
     * Returns true if the object has a {@link Value} defined,
     * or false otherwise.
     *
     * @return true if the object has a {@link Value} defined,
     * or false otherwise.
     */
    public abstract boolean hasValue(Symbol pSymbol);

    public abstract Object clone();
}
