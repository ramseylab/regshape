package org.systemsbiology.math.tp;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.Symbol;

public class TestSymbol
{
    private static void printSymbolValidity(String pSymbolName)
    {
        boolean valid = Symbol.isValidSymbol(pSymbolName);
        System.out.println("symbol: " + pSymbolName + "; valid: " + valid);
    }

    public static final void main(String []pArgs)
    {
        printSymbolValidity(""); 
        printSymbolValidity("abc");
   }
}
