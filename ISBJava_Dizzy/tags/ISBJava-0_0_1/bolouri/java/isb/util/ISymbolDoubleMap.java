package isb.util;

/**
 * Defines an interface for a class that can look up a value
 * (which is of type double) from a symbol name (which is of type string).
 */
public interface ISymbolDoubleMap
{
    /**
     * Returns the floating-point value associated with the specified
     * symbol name.
     * 
     * @param pSymbolName the string object containing the symbol name
     *
     * @return the floating-point value associated with the specified
     * symbol name.
     */
    public double getValue(String pSymbolName) throws DataNotFoundException;
}
