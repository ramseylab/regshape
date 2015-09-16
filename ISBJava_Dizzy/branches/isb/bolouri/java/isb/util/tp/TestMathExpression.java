package isb.util.tp;

import isb.util.*;
import java.util.*;

public class TestMathExpression
{

   /** 
     * A simple test interface to this class.  It takes the first command-line
     * argument and attempts to parse the argument as an expression; it then
     * prints out the string representation of the expression's parse tree,
     * to standard output.  It then prints out the value of the expression, for
     * three pre-defined symbols:  A = 1.0, B = 2.0, and C = 3.0.  If you invoke
     * the test program with an expression involving any symbols other than A, B, 
     * or C, you will get a runtime exception.
     *
     * @param pArgs the command-line arguments, of which only the first argument is
     * used (see above description).
     */
    public static final void main(String []pArgs)
    {
        String expressionStr = pArgs[0];

        MathExpression expression = new MathExpression(expressionStr);

        class SymbolDoubleMap implements ISymbolDoubleMap
        {
            HashMap mMap;
            
            public void setValue(String pKey, double pValue)
            {
                mMap.put(pKey, new Double(pValue));
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

        try
        {
            System.out.println("is valid symbol: " + MathExpression.isValidSymbol(expressionStr));
            SymbolDoubleMap map = new SymbolDoubleMap();
            map.setValue("A", 1.0);
            map.setValue("B", 2.0);
            map.setValue("C", 3.0);
            double val = expression.computeValue(map);
            System.out.println("value of expression: " + expression.toString() + " is: " + val);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.out);
        }
    }
}
