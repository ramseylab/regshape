package org.systemsbiology.math.tp;

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
