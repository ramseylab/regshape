package org.systemsbiology.math;

import org.systemsbiology.util.DataNotFoundException;

/**
 * Defines an interface for a class that can look up a value
 * (which is of type double) from a symbol name (which is of type string).
 *
 * @author Stephen Ramsey
 */
public interface ISymbolValueMap
{
    /**
     * Returns the {@link Value} associated with the specified
     * symbol name.
     * 
     * @param pSymbol the {@link Symbol} object for which the value is to be 
     * returned
     *
     * @return the {@link Value} associated with the specified
     * symbol.
     */
    public double getValue(Symbol pSymbol) throws DataNotFoundException;
}
