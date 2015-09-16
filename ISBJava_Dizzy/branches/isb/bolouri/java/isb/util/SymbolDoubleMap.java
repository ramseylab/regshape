package isb.util;

import java.util.*;


/**
 * Sample implementation of {@link ISymbolDoubleMap} that uses a 
 * <code>HashMap</code> to store key-value pairs, where the keys are
 * strings, and the values are objects of class <code>Double</code>.
 */
public class SymbolDoubleMap implements ISymbolDoubleMap
{
    HashMap mMap;

    public void setValue(String pKey, Double pValue)
    {
        mMap.put(pKey, pValue);
    }

    public double getValue(String pKey) throws DataNotFoundException
    {
        Double val = (Double) mMap.get(pKey);
        if(null == val)
        {
            throw new DataNotFoundException("could not find value associated with key: " + pKey);
        }
        return(val.doubleValue());
    }
        
    public SymbolDoubleMap()
    {
        mMap = new HashMap();
    }
}
