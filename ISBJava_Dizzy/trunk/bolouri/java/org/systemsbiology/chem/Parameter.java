package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

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

    public Parameter(String pName)
    {
        super(pName);
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

    public void setValue(Expression pValue)
    {
        setValue(new Value(pValue));
    }

    public void setValue(double pValue)
    {
        setValue(new Value(pValue));
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
