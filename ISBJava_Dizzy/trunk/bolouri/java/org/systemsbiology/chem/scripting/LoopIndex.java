package org.systemsbiology.chem.scripting;
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
