package org.systemsbiology.chem.scripting;

import org.systemsbiology.math.SymbolValue;
import org.systemsbiology.math.Value;

public class LoopIndex extends SymbolValue
{
    public LoopIndex(String pIndexName, int pValue)
    {
        super(pIndexName);
        setValue(new Value((double) pValue));
    }

    public void setValue(int pValue)
    {
        getValue().setValue((double) pValue);
    }

    public String toString()
    {
        int value = (int) getValue().getValue();
        return(Integer.toString(value));
    }
}
