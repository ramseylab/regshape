package org.systemsbiology.chem;

import org.systemsbiology.math.SymbolValue;
import org.systemsbiology.math.Value;
import org.systemsbiology.math.Expression;

public class Parameter extends SymbolValue
{
    private String mName;
    
    public String getSymbolName()
    {
        return(mName);
    }

    public String getName()
    {
        return(mName);
    }

    public Parameter(String pName, Expression pValue)
    {
        super(pName);
        setValue(new Value(pValue));
        mName = pName;
    }

    public Parameter(String pName, double pValue)
    {
        super(pName);
        setValue(new Value(pValue));
        mName = pName;
    }
    
    public Parameter(SymbolValue pSymbolValue)
    {
        super(pSymbolValue);
        mName = pSymbolValue.getSymbol().getName();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Parameter: ");
        sb.append(getName());
        sb.append(" [Value: ");
        sb.append(getValue().toString());
        sb.append("]");
        return(sb.toString());
    }
}
