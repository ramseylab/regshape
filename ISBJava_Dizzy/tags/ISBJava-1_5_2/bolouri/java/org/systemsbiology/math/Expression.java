package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import java.util.*;
import java.io.*;

/**
 * This class is a general-purpose facility for parsing simple mathematical 
 * expressions in floating-point arithmetic involving <b>symbols</b>, <b>functions</b>, 
 * and simple <b>operators</b>.  
 * <p />
 * <b>operators:</b> &nbsp;&nbsp;&nbsp; The allowed binary operator symbols are <code>*</code>,
 * <code>-</code>, <code>/</code>, <code>+</code>, and <code>^</code>, <code>%</code>
 * corresponding to multiplication, subtraction, division, addition,
 * exponentiation, and modulo division, respectively.  The only allowed unary operator
 * is <code>-</code>, which is negation.  Modular division is special in that
 * it is computed with floating point operands, just like the &quot;<code>%</code>&quot;
 * operator in the Java language.  Integer modulo division is simply a special case
 * of the more general floating-point modulo division.
 * <p />
 * <b>functions:</b> &nbsp;&nbsp;&nbsp; A small library of built-in functions
 * is supported; the function names are <em>case-sensitive</em>, and parentheses
 * required in order for the parser to detect a function call:  
 * <code>exp()</code>, <code>log()</code>, <code>sin()</code>, 
 * <code>cos()</code>, <code>tan()</code>, <code>asin()</code>, 
 * <code>acos()</code>, <code>atan()</code>, <code>abs()</code>, 
 * <code>floor()</code>, <code>ceil()</code>, <code>sqrt()</code>,
 * <code>theta()</code>.
 * The arguments of the trigonometric functions (<code>sin, cos, tan</code>)
 * <em>must</em> be in radians.  The return values of the inverse trigonometric
 * functions (<code>asin, acos, atan</code>) are in radians as well.  For more
 * details on the built-in functions, please refer to the
 * <a href="http://java.sun.com/j2se/1.4.1/docs/api/java/lang/Math.html"><code>java.lang.Math</code></a>
 * documentation.
 * <p />
 * <b>expressions:</b> &nbsp;&nbsp;&nbsp; Parentheses may be used
 * to group elements of a sub-expression.  The operator symbols,
 * parentheses, and whitespace are used to tokenize the expression
 * string; the resulting tokens are taken to be &quot;symbols&quot;
 * on which the operators are to be applied.  The following reserved characters
 * may not be used in an expression: <code>!@#$%[]|&amp;&gt;&lt;{},</code>.  
 * These characters are
 * reserved for future features of this expression class.  Function calls are not
 * allowed within expressions.  Note that bare numbers are permitted in
 * expressions, provided they parse as floating-point or integer numbers,
 * but they must not contain plus or minus characters.  For example, the
 * number &quot;1.5&quot; appearing in an expression is correctly parsed
 * as a floating-point value, but the number &quot;1.5e-7&quot; is 
 * <em>not</em> parsed as a floating-point value, because the tokenizer
 * interprets the hyphen character as denoting either negation or subtraction.
 * The fact that numbers are interpreted as values means that you <em>cannot</em>
 * specify a symbol in an expression that would otherwise be interpreted as
 * a floating-point number.  For example, if you input the expression
 * &quot;A + 10&quot;, the &quot;10&quot; will always be interpreted as a
 * value, and never as a symbol.  It is best if you make it a rule to always
 * begin symbol names with an alphabetic character. 
 * <p />
 * Precedence rules are taken from the
 * C programming language.  The order of precedence for the above-defined
 * operators and symbols is:
 * <ol>
 * <li>parentheses and function calls</li>
 * <li>unary operators (negation)</li>
 * <li>power exponentiation</li> 
 * <li>multiplication and division</li>
 * <li>addition and subtraction</li>
 * </ol>
 * where the increasing number represents <em>decreasing</em> precedence.
 * <p />
 * Example legal expressions
 * would be:
 * <blockquote>
 * <pre>
 * A + B + C
 * exp(A * (B + C))
 * A^(B - C)
 * sin(-1.7)
 * (A + B) * (C + D)
 * </pre>
 * </blockquote>
 * In the above, note that left-right associativity is used by the parser,
 * so that the first example would parse as: <code>(A + B) + C</code>.
 * The following examples of <em>illegal</em> expression statements, and
 * would generate a parser exception:
 * <blockquote>
 * <pre>
 * A (B + C)
 * A B
 * exp A
 * A * log(B)
 * </pre>
 * </blockquote>
 * In the above, note that <code>log(B)</code> is not allowed, because
 * the <code>MathExpression</code> class does not support function calls within 
 * expressions.
 * <p />
 * The following code fragment illustrates a sample use of this class:
 * <blockquote>
 * <pre>
 * MathExpression exp = new MathExpression("(A + B)/C");
 * System.out.println(exp.toString());
 * </pre>
 * </blockquote>
 * the above code fragment will result in <code>(A+B)/C</code>
 * being printed to standard output.  For a more non-trivial example code
 * using this class, please refer to the <code>main()</code> function below,
 * which serves as a test program for this class.
 * <p />
 * When an expression string has been parsed, the result is a
 * parse tree rooted at a single object called the &quot;root node&quot;.
 * As an example, the parse tree for the expression <code>log(A * (B + C)) + D</code>
 * might look (conceptually) like this:
 * <blockquote>
 * <pre>
 *        add
 *        / \
 *     log   D
 *      |
 *     mult
 *   /      \
 * symb(A)  add
 *          /  \
 *     symb(B) symb(C)
 * </pre>
 * </blockquote>
 * where &quot;mult&quot; represents an element whose <b>element code</b>
 * is <code>ElementCode.MULT</code>, and &quot;add&quot; represents an
 * element whose element code is <code>ElementCode.ADD</code>.  Furthermore,
 * the &quot;edges&quot; on the above graph represent links between an
 * element and its &quot;operands&quot; (i.e., its child nodes, in the tree).  
 * The &quot;symb(A)&quot; notation means an element whose element code
 * is <code>ElementCode.SYMBOL</code>, and whose <b>symbol name</b> field
 * is set to the string &quot;A&quot;.
 *
 * @author Stephen Ramsey
 */
public class Expression implements Cloneable
{
    /*========================================*
     * constants
     *========================================*/
    private static final String TOKEN_STRING_OPEN_PAREN = "(";
    private static final String TOKEN_STRING_CLOSE_PAREN = ")";
    private static final String TOKEN_STRING_MULT = "*";
    private static final String TOKEN_STRING_PLUS = "+";
    private static final String TOKEN_STRING_MINUS = "-";
    private static final String TOKEN_STRING_DIV = "/";
    private static final String TOKEN_STRING_POW = "^";
    private static final String TOKEN_STRING_MOD = "%";

    private static final String TOKEN_DELIMITERS = " *+-/^()";
    private static final String TOKEN_RESERVED = "!@#$[]|&><{},=";

    /*========================================*
     * inner class
     *========================================*/

    public interface SymbolPrinter
    {
        public String printSymbol(Symbol pSymbol) throws DataNotFoundException;
    }

    static class TokenCode
    {
        private final String mName;
        private TokenCode(String pName)
        {
            mName = pName;
        }
        public String toString()
        {
            return(mName);
        }
        public static final TokenCode NONE = new TokenCode("none");
        public static final TokenCode OPEN_PAREN = new TokenCode("open paren");
        public static final TokenCode CLOSE_PAREN = new TokenCode("close paren");
        public static final TokenCode NUMBER = new TokenCode("number");
        public static final TokenCode MULT = new TokenCode("mult");
        public static final TokenCode PLUS = new TokenCode("plus");
        public static final TokenCode MINUS = new TokenCode("minus");
        public static final TokenCode DIV = new TokenCode("div");
        public static final TokenCode POW = new TokenCode("pow");
        public static final TokenCode MOD = new TokenCode("mod");
        public static final TokenCode SYMBOL = new TokenCode("symbol");
        public static final TokenCode EXPRESSION = new TokenCode("expression");
    }

    static class ElementCode
    {
        private final String mName;
        private static HashMap mFunctionsMap;
        public final int mIntCode;
        public final int mNumFunctionArgs;

        public static final int NULL_FUNCTION_CODE = 0;
        public static final int ELEMENT_CODE_NONE = 0;
        public static final int ELEMENT_CODE_SYMBOL = 1;
        public static final int ELEMENT_CODE_NUMBER = 2;
        public static final int ELEMENT_CODE_MULT = 3;
        public static final int ELEMENT_CODE_ADD = 4;
        public static final int ELEMENT_CODE_SUBT = 5;
        public static final int ELEMENT_CODE_DIV = 6;
        public static final int ELEMENT_CODE_POW = 7;
        public static final int ELEMENT_CODE_MOD = 8;
        public static final int ELEMENT_CODE_NEG = 9;
        public static final int ELEMENT_CODE_EXP = 10;
        public static final int ELEMENT_CODE_LOG = 11;
        public static final int ELEMENT_CODE_SIN = 12;
        public static final int ELEMENT_CODE_COS = 13;
        public static final int ELEMENT_CODE_TAN = 14;
        public static final int ELEMENT_CODE_ASIN = 15;
        public static final int ELEMENT_CODE_ACOS = 16;
        public static final int ELEMENT_CODE_ATAN = 17;
        public static final int ELEMENT_CODE_ABS = 18;
        public static final int ELEMENT_CODE_FLOOR = 19;
        public static final int ELEMENT_CODE_CEIL = 20;
        public static final int ELEMENT_CODE_SQRT = 21;
        public static final int ELEMENT_CODE_THETA = 22;

        static
        {
            mFunctionsMap = new HashMap();
        }

        private ElementCode(String pName, int pIntCode)
        {
            this(pName, pIntCode, false, 0);
        }

        private ElementCode(String pName, int pIntCode, boolean pIsFunction, int pNumFunctionArgs)
        {
            mName = pName;
            mIntCode = pIntCode;
            mNumFunctionArgs = pNumFunctionArgs;
            if(pIsFunction)
            {
                putFunction(this);
            }
        }
        
        private void putFunction(ElementCode pElementCode)
        {
            mFunctionsMap.put(pElementCode.mName, pElementCode);
        }

        public static ElementCode getFunction(String pName)
        {
            return((ElementCode) mFunctionsMap.get(pName));
        }

        public boolean isFunction()
        {
            return(null != getFunction(mName));
        }
        public String toString()
        {
            return(mName);
        }
        /**
         * element code indicating an empty element (this should never
         * occur in a valid expression parse tree)
         */
        public static final ElementCode NONE = new ElementCode("none", ELEMENT_CODE_NONE);

        /**
         * element code indicating that the element is a &quot;symbol&quot;
         * (like a variable)
         */
        public static final ElementCode SYMBOL = new ElementCode("symbol", ELEMENT_CODE_SYMBOL);

        /**
         * element code indicating that the element is a &quot;value&quot;
         * (like a number)
         */
        public static final ElementCode NUMBER = new ElementCode("number", ELEMENT_CODE_NUMBER);

        /**
         * element code indicating that the element is an operation
         * element specifying the multiplication of its two child operands
         */
        public static final ElementCode MULT = new ElementCode("mult", ELEMENT_CODE_MULT);

        /**
         * element code indicating that the element is an operation
         * element specifying the addition of its two child operands
         */
        public static final ElementCode ADD = new ElementCode("add", ELEMENT_CODE_ADD);

        /**
         * element code indicating that the element is an operation
         * element specifying the subtraction of its two child operands
         * (the second operand is to be subtracted from the first)
         */
        public static final ElementCode SUBT = new ElementCode("subt", ELEMENT_CODE_SUBT);

        /**
         * element code indicating that the element is an operation
         * element specifying the division of its two child operands
         */
        public static final ElementCode DIV = new ElementCode("div", ELEMENT_CODE_DIV);

        /**
         * element code indicating that the element is an operation
         * element specifying the exponentiation of its first operand
         * by the value of the second operand
         */    
        public static final ElementCode POW = new ElementCode("pow", ELEMENT_CODE_POW);

        /**
         * element code indicating that the element is an operation
         * element specifying the modulus (remainder) of the quotient
         * of the first and second operands.
         */    
        public static final ElementCode MOD = new ElementCode("mod", ELEMENT_CODE_MOD);

        /**
         * element code indicating that the element is an operation
         * element specifying the negation of its first (and only)
         * operand
         */    
        public static final ElementCode NEG = new ElementCode("neg", ELEMENT_CODE_NEG);

        /**
         * element code specifying the exponential function of the 
         * first (and only) argument
         */
        public static final ElementCode EXP = new ElementCode("exp", ELEMENT_CODE_EXP, true, 1);

        /**
         * element code specifying the logarithm function (base e) of the 
         * first (and only) argument
         */
        public static final ElementCode LOG = new ElementCode("log", ELEMENT_CODE_LOG, true, 1);

        /**
         * element code specifying the sine function of the 
         * first (and only) argument; argument is in radians
         */
        public static final ElementCode SIN = new ElementCode("sin", ELEMENT_CODE_SIN, true, 1);

        /**
         * element code specifying the cosine function of the 
         * first (and only) argument; argument is in radians
         */
        public static final ElementCode COS = new ElementCode("cos", ELEMENT_CODE_COS, true, 1);

        /**
         * element code specifying the tangent function of the 
         * first (and only) argument; argument is in radians
         */
        public static final ElementCode TAN = new ElementCode("tan", ELEMENT_CODE_TAN, true, 1);

        /**
         * element code specifying the inverse sine function of the 
         * first (and only) argument; argument is in the range [-1,1];
         * return value is in radians
         */
        public static final ElementCode ASIN = new ElementCode("asin", ELEMENT_CODE_ASIN, true, 1);

        /**
         * element code specifying the inverse cosine function of the 
         * first (and only) argument; argument is in the range [-1,1];
         * return value is in radians
         */
        public static final ElementCode ACOS = new ElementCode("acos", ELEMENT_CODE_ACOS, true, 1);

        /**
         * element code specifying the inverse tangent function of the 
         * first (and only) argument; return value is in radians
         */
        public static final ElementCode ATAN = new ElementCode("atan", ELEMENT_CODE_ATAN, true, 1);

        /**
         * element code specifying the absolute value of the 
         * first (and only) argument
         */
        public static final ElementCode ABS = new ElementCode("abs", ELEMENT_CODE_ABS, true, 1);

        /**
         * element code specifying the greatest integer value that is
         * less than or equal to the argument
         */
        public static final ElementCode FLOOR = new ElementCode("floor", ELEMENT_CODE_FLOOR, true, 1);

        /**
         * element code specifying the smallest integer value that is
         * greater than or equal to the argument
         */
        public static final ElementCode CEIL = new ElementCode("ceil", ELEMENT_CODE_CEIL, true, 1);

        /**
         * element code specifying the square root of the argument, which 
         * must be nonnegative.
         */
        public static final ElementCode SQRT = new ElementCode("sqrt", ELEMENT_CODE_SQRT, true, 1);

        /**
         * element code specifying the Heaviside step function ("theta" function)
         */
        public static final ElementCode THETA = new ElementCode("theta", ELEMENT_CODE_THETA, true, 1);
    }

    /**
     * Data structure defining a single element of a parse tree for a 
     * mathematical expression.  An element can be either a <b>symbol element</b>, or an
     * <b>operation element</b> (unary or binary operators are allowed).
     * In the case of a symbol element, the <code>mFirstOperand</code>
     * and <code>mSecondOperand</code> fields are <code>null</code>, 
     * and the symbol name is stored in the string <code>mSymbolName</code>.
     * In the case of an operation element with a unary operator (the
     * negation operator is the only such operator allowed), the operand
     * is an {@link Expression.Element} pointed to by the <code>mFirstOperand</code>
     * field.  In the case of an operation element  with a binary
     * operator, the first operand is pointed to by the <code>mFirstOperand</code>
     * field, and the second operand is pointed to by the <code>mSecondOperand</code>
     * field.  In both the unary and binary operator cases, the <code>mCode</code>
     * field stores an integer <b>element code</b> indicating the type of operator.
     *
     * For a list of element codes, refer to the {@link Expression} class.
     */
    static class Element implements Cloneable
    {
        protected static final Element ONE = new Element(1.0);
        protected static final Element TWO = new Element(2.0);
        
        public Element()
        {
            // do nothing
        }

        public Element(ElementCode pCode)
        {
            mCode = pCode;
        }

        public Element(double pNumericValue)
        {
            mCode = ElementCode.NUMBER;
            mNumericValue = pNumericValue;
        }

        /**
         * The element code indicating whether the element is a
         * <b>symbol element</b> or an <b>operation element</b>, and
         * (in the case of an operation element) the type of operator. 
         * For a valid parse tree, the <code>mCode</code> field should
         * never take the value of <code>ElementCode.NONE = 0</code>, which
         * is reseved for an unused element field.
         */
        public ElementCode mCode;

        /**
         * For a binary or unary operation element, this field is a
         * reference pointing to the first operand, which is itself
         * an {@link Expression.Element}.  This field should be <code>null</code>
         * for the case of a symbol element.
         */
        public Element mFirstOperand;

        /**
         * For a binary element, this field is a
         * reference pointing to the second operand, which is itself
         * an {@link Expression.Element}.  For a unary operation element or
         * a symbol element, this field should be null.
         */
        public Element mSecondOperand;

        /**
         * For a symbol element, this field contains the symbol name. 
         */
        public Symbol mSymbol;
        
        /**
         * For a number element, this field contains the element value;
         */
        public double mNumericValue;

        /**
         * Returns a human-comprehensible textual description of this
         * mathematical expression.  This method assumes that the expression
         * has been &quot;set&quot; by calling {@link #setExpression(String)} 
         * or by using the parameterized constructor.
         *
         * @return a human-comprehensible textual description of this
         * mathematical expression.
         */
        public String toString(SymbolPrinter pSymbolPrinter) throws IllegalStateException, DataNotFoundException
        {
            ElementCode code = mCode;
            StringBuffer sb = new StringBuffer();
            if(code == ElementCode.NEG)
            {
                sb.append(TOKEN_STRING_MINUS);
                if(mFirstOperand.mCode != ElementCode.SYMBOL)
                {
                    sb.append("(");
                }
                sb.append(mFirstOperand.toString(pSymbolPrinter));
                if(mFirstOperand.mCode != ElementCode.SYMBOL)
                {
                    sb.append(")");
                }
            }
            else
            {
                if(mCode.isFunction())
                {
                    sb.append(mCode + "(");
                    if(null != mFirstOperand)
                    {
                        sb.append(mFirstOperand.toString(pSymbolPrinter));
                    }
                    sb.append(")");
                }
                else
                {
                    String operatorSymbol = getBinaryOperatorSymbol(code);
                    if(null != operatorSymbol)
                    {
                        if(mFirstOperand.mCode != ElementCode.SYMBOL)
                        {
                            sb.append("(");
                        }
                        sb.append(mFirstOperand.toString(pSymbolPrinter));
                        if(mFirstOperand.mCode != ElementCode.SYMBOL)
                        {
                            sb.append(")");
                        }
                        sb.append(operatorSymbol);
                        if(mSecondOperand.mCode != ElementCode.SYMBOL)
                        {
                            sb.append("(");
                        }
                        sb.append(mSecondOperand.toString(pSymbolPrinter));
                        if(mSecondOperand.mCode != ElementCode.SYMBOL)
                        {
                            sb.append(")");
                        }                        
                    }
                    else if(code == ElementCode.SYMBOL)
                    {
                        if(null != pSymbolPrinter)
                        {
                            sb.append(pSymbolPrinter.printSymbol(mSymbol));
                        }
                        else
                        {
                            sb.append(mSymbol.getName());
                        }
                    }
                    else if(code == ElementCode.NUMBER)
                    {
                        sb.append(mNumericValue);
                    }
                    else
                    {
                        throw new IllegalStateException("invalid element code encountered; code is: " + code);
                    }                
                }
            }

            return(sb.toString());
        }

        public Object clone()
        {
            Element newElement = new Element();
            newElement.mCode = mCode;

            if(null != mFirstOperand)
            {
                newElement.mFirstOperand = (Element) mFirstOperand.clone();
            }
            else
            {
                newElement.mFirstOperand = null;
            }

            if(null != mSecondOperand)
            {
                newElement.mSecondOperand = (Element) mSecondOperand.clone();
            }
            else
            {
                newElement.mSecondOperand = null;
            }
            
            newElement.mNumericValue = mNumericValue;
            if(null != mSymbol)
            {
                newElement.mSymbol = (Symbol) mSymbol.clone();
            }
            else
            {
                newElement.mSymbol = null;
            }

            return(newElement);
        }
    }

    // represents an element of the token list for a tokenized 
    // mathematical epxression
    private class Token
    {
        TokenCode mCode;
        String mSymbolName;
        Element mParsedExpression;
        double mNumericValue;

        public Token()
        {
            mCode = TokenCode.NONE;
            mSymbolName = null;
            mParsedExpression = null;
        }
    }

    /*========================================*
     * member data
     *========================================*/
    private Element mRootElement;
    private SymbolEvaluatorHashMap mSymbolEvaluator;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setRootElement(Element pRootElement)
    {
        mRootElement = pRootElement;
    }

    private Element getRootElement()
    {
        return(mRootElement);
    }

    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeRootElement()
    {
        setRootElement(null);
    }

    private void initialize()
    {
        initializeRootElement();
        mSymbolEvaluator = null;
    }

    /*========================================*
     * constructors
     *========================================*/
    public Expression()
    {
        initialize();
    }
    
    public Expression(String pExpression) throws IllegalArgumentException
    {
        initialize();
        setRootElement(parseExpression(pExpression));
    }

    /*========================================*
     * private methods
     *========================================*/
    private SymbolEvaluator getSymbolEvaluator(HashMap pSymbolMap)
    {
        if(null == mSymbolEvaluator)
        {
            mSymbolEvaluator = new SymbolEvaluatorHashMap(pSymbolMap);
        }
        else
        {
            mSymbolEvaluator.setSymbolMap(pSymbolMap);
        }
        return(mSymbolEvaluator);
    }

    private void checkForReservedCharacters(String pFormula) throws IllegalArgumentException
    {
        String tokenReserved = TOKEN_RESERVED;
        int numReservedChars = tokenReserved.length();
        for(int charCtr = 0; charCtr < numReservedChars; ++charCtr)
        {
            String reservedChar = tokenReserved.substring(charCtr, charCtr + 1);
            int index = pFormula.indexOf(reservedChar);
            if(index != -1)
            {
                throw new IllegalArgumentException("expression contained reserved character: " + reservedChar + " at position " + index);
            }
        }            
    }

    private List tokenizeExpression(String pFormula) throws IllegalArgumentException
    {
        checkForReservedCharacters(pFormula);                              

        List tokenizedFormula = new LinkedList();
        boolean returnDelims = true;
        StringTokenizer stringTokenizer = new StringTokenizer(pFormula, TOKEN_DELIMITERS, returnDelims);
        while(stringTokenizer.hasMoreTokens())
        {
            String tokenStr = stringTokenizer.nextToken();
            if(0 == tokenStr.trim().length())
            {
                // ignore empty (whitespace) tokens
                continue;
            }

            Token token = new Token();

            if(tokenStr.equals(TOKEN_STRING_OPEN_PAREN))
            {
                token.mCode = TokenCode.OPEN_PAREN;
            }
            else if(tokenStr.equals(TOKEN_STRING_CLOSE_PAREN))
            {
                token.mCode = TokenCode.CLOSE_PAREN;
            }
            else if(tokenStr.equals(TOKEN_STRING_MULT))
            {
                token.mCode = TokenCode.MULT;
            }
            else if(tokenStr.equals(TOKEN_STRING_PLUS))
            {
                token.mCode = TokenCode.PLUS;
            }
            else if(tokenStr.equals(TOKEN_STRING_MINUS))
            {
                token.mCode = TokenCode.MINUS;
            }
            else if(tokenStr.equals(TOKEN_STRING_DIV))
            {
                token.mCode = TokenCode.DIV;
            }
            else if(tokenStr.equals(TOKEN_STRING_POW))
            {
                token.mCode = TokenCode.POW;
            }
            else if(tokenStr.equals(TOKEN_STRING_MOD))
            {
                token.mCode = TokenCode.MOD;
            }
            else
            {
                try
                {
                    double value = Double.parseDouble(tokenStr);
                    token.mCode = TokenCode.NUMBER;
                    token.mNumericValue = value;
                }
                catch(NumberFormatException e)
                {
                    token.mCode = TokenCode.SYMBOL;
                    token.mSymbolName = tokenStr;
                }
            }
            tokenizedFormula.add(token);
        }
        return(tokenizedFormula);
    }


    private Element parseTokenizedExpression(List pFormula) throws IllegalArgumentException
    {
        // parse for parentheses (sub-expressions)
        parseParentheses(pFormula);

        // parse for built-in function calls (exp, log, sin, cos, tan, etc.)
        parseFunctionCalls(pFormula);

        // parse for unary operators
        HashMap unaryMap = new HashMap();
        unaryMap.put(TokenCode.MINUS, ElementCode.NEG);
        parseUnaryOperator(unaryMap, pFormula);
        
        // parse for pow        
        HashMap binaryMap = new HashMap();
        binaryMap.put(TokenCode.POW, ElementCode.POW);
        parseBinaryOperator(binaryMap, pFormula);

        // parse for mult and div
        binaryMap.clear();
        binaryMap.put(TokenCode.MULT, ElementCode.MULT);
        binaryMap.put(TokenCode.DIV, ElementCode.DIV);        
        binaryMap.put(TokenCode.MOD, ElementCode.MOD);        
        parseBinaryOperator(binaryMap, pFormula);
       
        // parse for add and subt
        binaryMap.clear();
        binaryMap.put(TokenCode.PLUS, ElementCode.ADD);
        binaryMap.put(TokenCode.MINUS, ElementCode.SUBT);        
        parseBinaryOperator(binaryMap, pFormula);

        Iterator iter = pFormula.listIterator();
        if(! iter.hasNext())
        {
            throw new IllegalArgumentException("no elements found in the parse tree for this expression");
        }
        Token finalToken = (Token) iter.next();
        if(iter.hasNext())
        {
            throw new IllegalArgumentException("found more than one element at the root of the parsed formula tree");
        }
        return(convertTokenToElement(finalToken));
    }


    private Element convertTokenToElement(Token pToken) throws IllegalArgumentException
    {
        TokenCode tokCode = pToken.mCode;
        if(tokCode != TokenCode.EXPRESSION &&
           tokCode != TokenCode.SYMBOL && 
           tokCode != TokenCode.NUMBER)
        {
            throw new IllegalArgumentException("called tokToElem with a token that is neither a symbol nor an expression; code is: " + tokCode);
        }
        Element retVal;
        if(tokCode == TokenCode.EXPRESSION)
        {
            retVal = pToken.mParsedExpression;
        }
        else
        {
            retVal = new Element();
            if(tokCode == TokenCode.NUMBER)
            {
                retVal.mCode = ElementCode.NUMBER;
                retVal.mNumericValue = pToken.mNumericValue;
            }
            else
            {
                retVal.mCode = ElementCode.SYMBOL;
                retVal.mSymbol = new Symbol(pToken.mSymbolName);
            }
        }
        return(retVal);
    }

    private void parseFunctionCalls(List pTokenizedExpression) throws IllegalArgumentException
    {
        ListIterator iter = pTokenizedExpression.listIterator();
        Token tok = null;
        Token prevTok = null;

        // parse parentheses first, since they have the highest level of precedence
        while(iter.hasNext())
        {
            prevTok = tok;
            tok = (Token) iter.next();
            TokenCode tokenCode = tok.mCode;

            if(tok.mCode == TokenCode.SYMBOL)
            {
                String symbolName = tok.mSymbolName;
                ElementCode elementCodeFunction = parseFunctionName(symbolName);

                // check to see if this is a function call
                if(iter.hasNext())
                {
                    // check to see if next token is an expression token
                    Token nextTok = (Token) iter.next();
                    if(nextTok.mCode == TokenCode.EXPRESSION)
                    {
                        Element functionCallElement = new Element();
                        functionCallElement.mFirstOperand = nextTok.mParsedExpression;
                        if(null != elementCodeFunction)
                        {
                            functionCallElement.mCode = elementCodeFunction;
                        }
                        else
                        {
                            throw new IllegalArgumentException("unknown symbol used as function name: " + symbolName);
                        }
                        int numArgs = elementCodeFunction.mNumFunctionArgs;
                        if(numArgs == 0)
                        {
                            if(functionCallElement.mFirstOperand != null)
                            {
                                throw new IllegalArgumentException("function does not allow any arguments: " + elementCodeFunction);
                            }
                        }
                        else if(numArgs == 1)
                        {
                            if(functionCallElement.mFirstOperand == null)
                            {
                                throw new IllegalArgumentException("function requires an argument: " + elementCodeFunction);
                            }
                        }
                        nextTok.mParsedExpression = functionCallElement;
                        iter.previous();
                        iter.previous();
                        iter.remove();   // remove the previous symbol token, since it is not needed anymore
                        iter.next();
                    }
                    else
                    {
                        if(null != elementCodeFunction)
                        {
                            throw new IllegalArgumentException("reserved function name used as symbol: " + symbolName);
                        }
                    }
                }
                else
                {
                    if(null != elementCodeFunction)
                    {
                        throw new IllegalArgumentException("reserved function name used as symbol: " + symbolName);
                    }
                }
            }
            else
            {
                // do nothing
            }
        }
    }

    static private String getBinaryOperatorSymbol(ElementCode pElementOperatorCode)
    {
        String retVal = null;

        if(pElementOperatorCode == ElementCode.MULT)
        {
            retVal = TOKEN_STRING_MULT;
        }
        else if(pElementOperatorCode == ElementCode.ADD)
        {
            retVal = TOKEN_STRING_PLUS;
        }
        else if(pElementOperatorCode == ElementCode.SUBT)
        {
            retVal = TOKEN_STRING_MINUS;
        }
        else if(pElementOperatorCode == ElementCode.DIV)
        {
            retVal = TOKEN_STRING_DIV;
        }
        else if(pElementOperatorCode == ElementCode.POW)
        {
            retVal = TOKEN_STRING_POW;
        }
        else if(pElementOperatorCode == ElementCode.MOD)
        {
            retVal = TOKEN_STRING_MOD;
        }
        return(retVal);
    }

    static private ElementCode parseFunctionName(String pFunctionName)
    {
        return(ElementCode.getFunction(pFunctionName));
    }

    private void parseParentheses(List pTokenizedExpression) throws IllegalArgumentException
    {
        int parenDepth = 0;
        int tokenCtr = 0;
        ListIterator iter = pTokenizedExpression.listIterator();
        List subFormula = null;

        Token tok = null;
        Token prevTok = null;

        // parse parentheses first, since they have the highest level of precedence
        while(iter.hasNext())
        {
            prevTok = tok;
            tok = (Token) iter.next();
            TokenCode tokenCode = tok.mCode;

            if(parenDepth > 1 ||
               (parenDepth == 1 && tokenCode != TokenCode.CLOSE_PAREN))
            {
                iter.remove();
                subFormula.add(tok);
            }
                
            if(tokenCode == TokenCode.OPEN_PAREN)
            {
                if(parenDepth == 0)
                {
                    subFormula = new LinkedList();
                    iter.remove();
                }
                ++parenDepth;
                
            }
            else if(tokenCode == TokenCode.CLOSE_PAREN)
            {
                --parenDepth;
                if(parenDepth < 0)
                {
                    throw new IllegalArgumentException("invalid parenthesis encountered for token number: " + tokenCtr);
                }
                if(parenDepth == 0)
                {
                    // we just completed a parenthetical expression
                    tok.mCode = TokenCode.EXPRESSION;

                    Element parsedSubFormula = null;
                    if(prevTok == null ||
                       ! prevTok.mCode.equals(TokenCode.OPEN_PAREN))
                    {
                        parsedSubFormula = parseTokenizedExpression(subFormula);
                    }
                    
                    // this expression is to be added directly to the token stream
                    tok.mParsedExpression = parsedSubFormula;
                }
            }

            ++tokenCtr;
        }

        if(parenDepth > 0)
        {
            throw new IllegalArgumentException("mismatched parentheses found in formula");
        }
    }

    // From a list of tokens, parse all unary operator expressions (of the specified type) that occur.
    // The pMask variable is a logical OR of token code values of unary operators to look for.
    private void parseUnaryOperator(HashMap pTokenCodeMap, List pTokenizedExpression) throws IllegalArgumentException
    {
        // parse for unary operators
        ListIterator iter = pTokenizedExpression.listIterator();
        Token lastTok = null;
        Token token = null;
        while(iter.hasNext())
        {
            lastTok = token;
            token = (Token) iter.next();
            TokenCode tokenCode = token.mCode;

            if(tokenCode == TokenCode.EXPRESSION)
            {
                continue;
            }

            Set tokenCodeSet = pTokenCodeMap.keySet();

            if(tokenCodeSet.contains(tokenCode))
            {
                // check previous token to make sure this operator is not 
                // being used as a binary operator (e.g., negation)
                if(lastTok == null ||
                   (lastTok.mCode != TokenCode.SYMBOL &&
                    lastTok.mCode != TokenCode.EXPRESSION &&
                    lastTok.mCode != TokenCode.NUMBER))
                {
                    // must be using minus as a unary operator
                    if(! iter.hasNext())
                    {
                        throw new IllegalArgumentException("last token in the list is a minus, this is not allowed");
                    }
                    Token nextTok = (Token) iter.next();

                    Element operand = convertTokenToElement(nextTok);

                    Element opElement = new Element();
                    ElementCode elementCode = (ElementCode) pTokenCodeMap.get(tokenCode);
                    opElement.mCode = elementCode;
                    opElement.mFirstOperand = operand;

                    iter.previous();
                    iter.remove();
                    token.mCode = TokenCode.EXPRESSION;
                    token.mParsedExpression = opElement;
                }
            }
        }
    }

    // From a list of tokens, parse all binary operator expressions (of the specified type) that occur.
    // The pMask variable is a logical OR of token code values of binary operators to look for.
    private void parseBinaryOperator(HashMap pTokenCodeMap, List pTokenizedExpression) throws IllegalArgumentException
    {
        ListIterator iter = pTokenizedExpression.listIterator();
        Token lastTok = null;
        Token token = null;
        while(iter.hasNext())
        {
            lastTok = token;
            token = (Token) iter.next();
            TokenCode tokenCode = token.mCode;

            if(tokenCode == TokenCode.EXPRESSION)
            {
                continue;
            }

            Set tokenCodeSet = pTokenCodeMap.keySet();

            if(tokenCodeSet.contains(tokenCode))
            {
                if(lastTok == null)
                {
                    throw new IllegalArgumentException("encountered binary operator with no first operand found");
                }
                if(! iter.hasNext())
                {
                    throw new IllegalArgumentException("encountered binary operator with no second operand found");
                }
                Token nextTok = (Token) iter.next();
                Element op1 = convertTokenToElement(lastTok);
                Element op2 = convertTokenToElement(nextTok);
                Element product = new Element();
                ElementCode elementCode = (ElementCode) pTokenCodeMap.get(tokenCode);
                product.mCode = elementCode;
                product.mFirstOperand = op1;
                product.mSecondOperand = op2;
                iter.remove();
                iter.previous();
                iter.previous();
                iter.remove();
                iter.next();
                token.mCode = TokenCode.EXPRESSION;
                token.mParsedExpression = product;
            }
        }        
    }


    // this method is PERFORMANCE CRITICAL code; that is why it is so ugly
    private static final double valueOfSubtreeNonSimple(Element pElement, SymbolEvaluator pSymbolEvaluator, int pElementCodeInt) throws DataNotFoundException
    {
        double valueOfFirstOperand;
        Element firstOperand = pElement.mFirstOperand;
        int firstOperandIntCode = firstOperand.mCode.mIntCode;
        switch(firstOperandIntCode)
        {
            case ElementCode.ELEMENT_CODE_SYMBOL:
                valueOfFirstOperand = pSymbolEvaluator.getValue(firstOperand.mSymbol);
                break;
            case ElementCode.ELEMENT_CODE_NUMBER:
                valueOfFirstOperand = firstOperand.mNumericValue;
                break;
            default:
                valueOfFirstOperand = valueOfSubtreeNonSimple(firstOperand, pSymbolEvaluator, firstOperandIntCode);
                break;
        }

        Element secondOperand = pElement.mSecondOperand;
        if(null != secondOperand)
        {
            double valueOfSecondOperand;
            int secondOperandIntCode = secondOperand.mCode.mIntCode;
            switch(secondOperandIntCode)
            {
                case ElementCode.ELEMENT_CODE_SYMBOL:
                    valueOfSecondOperand = pSymbolEvaluator.getValue(secondOperand.mSymbol);
                    break;
                case ElementCode.ELEMENT_CODE_NUMBER:
                    valueOfSecondOperand = secondOperand.mNumericValue;
                    break;
                default:
                    valueOfSecondOperand = valueOfSubtreeNonSimple(secondOperand, pSymbolEvaluator, secondOperandIntCode);
                    break;
            }

            switch(pElementCodeInt)
            {
                case ElementCode.ELEMENT_CODE_MULT:
                    return(valueOfFirstOperand * valueOfSecondOperand);
                    
                case ElementCode.ELEMENT_CODE_ADD:
                    return(valueOfFirstOperand + valueOfSecondOperand);
                    
                case ElementCode.ELEMENT_CODE_DIV:
                    return(valueOfFirstOperand / valueOfSecondOperand);
                    
                case ElementCode.ELEMENT_CODE_SUBT:
                    return(valueOfFirstOperand - valueOfSecondOperand);
                    
                case ElementCode.ELEMENT_CODE_POW:
                    if(valueOfSecondOperand == 2.0)
                    {
                        return(valueOfFirstOperand * valueOfFirstOperand);
                    }
                    else if(valueOfSecondOperand == 3.0)
                    {
                        return(valueOfFirstOperand * valueOfFirstOperand * valueOfFirstOperand);
                    }
                    else if(valueOfSecondOperand == 4.0)
                    {
                        return(valueOfFirstOperand * valueOfFirstOperand * valueOfFirstOperand * valueOfFirstOperand);
                    }
                    return(Math.pow(valueOfFirstOperand, valueOfSecondOperand));
                    
                case ElementCode.ELEMENT_CODE_MOD:
                    return(valueOfFirstOperand % valueOfSecondOperand);
                    
                default:
                    throw new IllegalStateException("unknown function code: " + pElement.mCode);
            }
        }
        else
        {
            switch(pElementCodeInt)
            {
                case ElementCode.ELEMENT_CODE_NEG:
                    return(-1.0 * valueOfFirstOperand);
                    
                case ElementCode.ELEMENT_CODE_EXP:
                    return(Math.exp(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_LOG:
                    return(Math.log(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_SIN:
                    return(Math.sin(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_COS:
                    return(Math.cos(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_TAN:
                    return(Math.tan(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_ASIN:
                    return(Math.asin(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_ACOS:
                    return(Math.acos(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_ATAN:
                    return(Math.atan(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_ABS:
                    return(Math.abs(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_FLOOR:
                    return(Math.floor(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_CEIL:
                    return(Math.ceil(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_SQRT:
                    return(Math.sqrt(valueOfFirstOperand));
                    
                case ElementCode.ELEMENT_CODE_THETA:
                    return(MathFunctions.thetaFunction(valueOfFirstOperand));
                    
                default:
                    throw new IllegalStateException("unknown function code: " + pElement.mCode);
            }
        }
    }

    private static final double valueOfSubtree(Element pElement, SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        int elementCodeInt = pElement.mCode.mIntCode;

        switch(elementCodeInt)
        {
            // first handle all the cases that have no operands:

            case ElementCode.ELEMENT_CODE_SYMBOL:
                Symbol symbol = pElement.mSymbol;
                return(pSymbolEvaluator.getValue(symbol));

            case ElementCode.ELEMENT_CODE_NUMBER:
                return(pElement.mNumericValue);

            default:
                return(valueOfSubtreeNonSimple(pElement, pSymbolEvaluator, elementCodeInt));
        }
    }

    /*========================================*
     * protected methods
     *========================================*/
   /**
     * Returns an {@link Expression.Element} which is the root of a tree of 
     * <code>Element</code> objects representing the parse tree for
     * a mathematical expression defined by the string <code>pExpressionString</code>.
     * The parse tree is not stored in the <code>Expression</code> object.
     *
     * @param pExpressionString the string definition of the mathematical
     * expression to be parsed
     *
     * @return an {@link Expression.Element} which is the root of a tree of 
     * <code>Element</code> objects representing the parse tree for
     * a mathematical expression defined by the string <code>pExpressionString</code>.
     */
    Element parseExpression(String pExpressionString) throws IllegalArgumentException
    {
        List tokenizedExpression = tokenizeExpression(pExpressionString);
        return(parseTokenizedExpression(tokenizedExpression));
    }

    /*========================================*
     * public methods
     *========================================*/
    
    /**
     * Returns true of the string token passed as <code>pToken</code>
     * is a valid &quot;symbol&quot;, meaning that it does not contain 
     * any of the operators <code>*</code>, <code>-</code>, <code>/</code>,
     * <code>+</code>, <code>^</code>, or any parentheses.  In addition, the
     * following characters may not appear in symbols because they are reserved for
     * future use in mathematical expressions:  <code>!@#$%[]|&amp;&gt;&lt;{}</code>.
     *
     * <p />
     * Examples of valid symbols would be: 
     * <blockquote>
     * <pre>
     * X1
     * SY2C
     * </pre>
     * </blockquote>
     * Examples of <em>invalid</em> symbols would be:
     * <blockquote>
     * <pre>
     * X1-2
     * X(1)
     * -X
     * (X)
     * X*B
     * X+C
     * X B
     * exp
     * 1.7
     * </pre>
     * </blockquote>
     * 
     * @param pToken the string token to be checked for validity as a symbol
     *
     * @return true of the string token passed as <code>pToken</code>
     * is a valid &quot;symbol&quot;.
     */
    static public boolean isValidSymbol(String pToken)
    {
        boolean isValidSymbol = false;
        try
        {
            Expression testExp = new Expression(pToken);
            Element elem = testExp.getRootElement();
            if(ElementCode.SYMBOL == elem.mCode)
            {
                // check for parentheses and whitespace, which are not allowed in symbols
                boolean returnDelims = true;
                StringTokenizer tok = new StringTokenizer(pToken, " ()", returnDelims);
                int numTokens = tok.countTokens();
                if(numTokens == 1)
                {
                    isValidSymbol = true;
                }
            }
        }
        catch(Exception e)
        {
            isValidSymbol = false;
        }
        return(isValidSymbol);
    }

    /**
     * Returns a human-comprehensible representation of this
     * mathematical expression.
     *
     * @return a human-comprehensible representation of this
     * mathematical expression.
     */
    public String toString() throws IllegalStateException
    {
        try
        {
            return(toString(null));
        }
        catch(DataNotFoundException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public String toString(SymbolPrinter pSymbolPrinter) throws IllegalStateException, DataNotFoundException
    {
        String retStr;
        Element rootElement = getRootElement();
        if(null != rootElement)
        {
            retStr = rootElement.toString(pSymbolPrinter);
        }
        else
        {
            retStr = "null";
        }
        return(retStr);
    }

    /**
     * Returns true if the argument is a valid function name, or 
     * false otherwise.
     */
    public static boolean isFunctionName(String pName)
    {
        ElementCode function = ElementCode.getFunction(pName);
        return(null != function && function.isFunction());
    }

    /**
     * Parses the mathematical expression defined by the string 
     * <code>pExpressionString</code> and stores the parse tree within
     * this <code>Expression</code> object.  This method can be
     * called even if the <code>Expression</code> object was 
     * initialized with a different expression string, in which
     * case the string specified by <code>pExpressionString</code>
     * is parsed and the parse tree replaces the parse tree created
     * when the object was constructed.
     *
     * @param pExpressionString the string definition of the mathematical
     * expression to be parsed
     *
     */
    public void setExpression(String pExpressionString) throws IllegalArgumentException
    {
        setRootElement(parseExpression(pExpressionString));
    }

    public double computeValue(HashMap pSymbolMap) throws DataNotFoundException, IllegalStateException
    {
        SymbolEvaluator symbolEvaluator = getSymbolEvaluator(pSymbolMap);
        return(computeValue(symbolEvaluator));
    }

    /**
     * Return the computed value of the expression (must have been defined
     * already in the constructor, or in a call to {@link #setExpression(String)}),
     * using the symbol values defined in the map <code>ISymbolValueMap</code>.
     */
    public double computeValue(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException, IllegalStateException, IllegalArgumentException
    {
        Element rootElement = getRootElement();
        if(null == rootElement)
        {
            throw new IllegalStateException("attempted to compute value of a math expression object that has no expression defined");
        }
        double retVal = 0.0;
        try
        {
            retVal = valueOfSubtree(rootElement, pSymbolEvaluator);
        }
        catch(java.lang.StackOverflowError e)
        {
            throw new IllegalArgumentException("circular expression encountered while attempting to parse expression: " + toString());
        }


        return(valueOfSubtree(rootElement, pSymbolEvaluator));
    }

    public Object clone()
    {
        Expression newExpression = new Expression();
        if(mRootElement != null)
        {
            newExpression.mRootElement = (Element) mRootElement.clone();
        }
        else
        {
            newExpression.mRootElement = null;
        }
        return(newExpression);
    }

    private Element computePartialDerivative(Element pElement, Symbol pSymbol)
    {
        Element retElement = new Element();
        ElementCode code = pElement.mCode;
        int intCode = code.mIntCode;

        Element firstOperand = pElement.mFirstOperand;
        Element firstOperandDerivExpression = null;
        boolean firstOperandDerivZero = false;
        boolean firstOperandDerivUnity = false;
        if(null != firstOperand)
        {
            firstOperandDerivExpression = computePartialDerivative(firstOperand, pSymbol);
            ElementCode firstOperandElementCode = firstOperand.mCode;
            if(firstOperandDerivExpression.mCode.equals(ElementCode.NUMBER))
            {
                double derivValue = firstOperandDerivExpression.mNumericValue;
                if(derivValue == 0.0)
                {
                    firstOperandDerivZero = true;
                }
                else if(derivValue == 1.0)
                {
                    firstOperandDerivUnity = true;
                }
            }
        }

        Element secondOperand = pElement.mSecondOperand;
        Element secondOperandDerivExpression = null;
        boolean secondOperandDerivZero = false;
        boolean secondOperandDerivUnity = false;
        if(null != secondOperand)
        {
            secondOperandDerivExpression = computePartialDerivative(secondOperand, pSymbol);
            ElementCode secondOperandElementCode = secondOperand.mCode;
            if(secondOperandDerivExpression.mCode.equals(ElementCode.NUMBER))
            {
                double derivValue = secondOperandDerivExpression.mNumericValue;
                if(derivValue == 0.0)
                {
                    secondOperandDerivZero = true;
                }
                else if(derivValue == 1.0)
                {
                    secondOperandDerivUnity = true;
                }
            }
        }

        switch(intCode)
        {
            case ElementCode.ELEMENT_CODE_NUMBER:
                retElement.mCode = ElementCode.NUMBER;
                retElement.mNumericValue = 0.0;
                break;
                
            case ElementCode.ELEMENT_CODE_SYMBOL:
                retElement.mCode = ElementCode.NUMBER;
                Symbol symbol = pElement.mSymbol;
                double derivValue = 0.0;
                if(symbol.equals(pSymbol))
                {
                    derivValue = 1.0;
                }
                retElement.mNumericValue = derivValue;
                break;

            case ElementCode.ELEMENT_CODE_NONE:
                throw new IllegalArgumentException("element code NONE should never occur in a valid expression tree");

            case ElementCode.ELEMENT_CODE_MOD:
                throw new IllegalArgumentException("unable to compute the derivative of the modulo division operator");

            case ElementCode.ELEMENT_CODE_ABS:
                throw new IllegalArgumentException("unable to compute the derivative of the abs() function");

            case ElementCode.ELEMENT_CODE_FLOOR:
                throw new IllegalArgumentException("unable to compute the derivative of the floor() function");

            case ElementCode.ELEMENT_CODE_CEIL:
                throw new IllegalArgumentException("unable to compute the derivative of the ceil() function");

            case ElementCode.ELEMENT_CODE_THETA:
                throw new IllegalArgumentException("unable to compute the derivative of the theta() function");

            case ElementCode.ELEMENT_CODE_ACOS:
                if(! firstOperandDerivZero)
                {
                    retElement.mCode = ElementCode.NEG;

                    Element ratio = new Element(ElementCode.DIV);
                    retElement.mFirstOperand = ratio;

                    ratio.mFirstOperand = firstOperandDerivExpression;

                    Element sqrtOneMinusXSquared = new Element(ElementCode.SQRT);
                    ratio.mSecondOperand = sqrtOneMinusXSquared;

                    Element oneMinusXSquared = new Element(ElementCode.SUBT);
                    sqrtOneMinusXSquared.mFirstOperand = oneMinusXSquared;

                    oneMinusXSquared.mFirstOperand = Element.ONE;
                    
                    Element xSquared = new Element(ElementCode.POW);
                    oneMinusXSquared.mSecondOperand = xSquared;

                    xSquared.mFirstOperand = firstOperand;
                    xSquared.mSecondOperand = Element.TWO;
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }
                break;

            case ElementCode.ELEMENT_CODE_ASIN:
                if(! firstOperandDerivZero)
                {
                    retElement.mCode = ElementCode.DIV;

                    retElement.mFirstOperand = firstOperandDerivExpression;

                    Element sqrtOneMinusXSquared = new Element(ElementCode.SQRT);
                    retElement.mSecondOperand = sqrtOneMinusXSquared;

                    Element oneMinusXSquared = new Element(ElementCode.SUBT);
                    sqrtOneMinusXSquared.mFirstOperand = oneMinusXSquared;

                    oneMinusXSquared.mFirstOperand = Element.ONE;
                    
                    Element xSquared = new Element(ElementCode.POW);
                    oneMinusXSquared.mSecondOperand = xSquared;

                    xSquared.mFirstOperand = firstOperand;
                    xSquared.mSecondOperand = Element.TWO;
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }
                break;

            case ElementCode.ELEMENT_CODE_ATAN:
                if(! firstOperandDerivZero)
                {
                    retElement.mCode = ElementCode.DIV;

                    retElement.mFirstOperand = firstOperandDerivExpression;
                    
                    Element onePlusXSquared = new Element();
                    retElement.mSecondOperand = onePlusXSquared;
                    onePlusXSquared.mCode = ElementCode.ADD;
                                       
                    onePlusXSquared.mFirstOperand = Element.ONE;
                    
                    Element xSquared = new Element();
                    xSquared.mCode = ElementCode.POW;
                    xSquared.mFirstOperand = firstOperand;
                    xSquared.mSecondOperand = Element.TWO;
                    
                    onePlusXSquared.mSecondOperand = xSquared;

                    retElement.mSecondOperand = onePlusXSquared;
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }
                break;

            case ElementCode.ELEMENT_CODE_SQRT:
                if(! firstOperandDerivZero)
                {
                    retElement.mCode = ElementCode.DIV;
                    
                    retElement.mFirstOperand = firstOperandDerivExpression;
                    Element twoSqrt = new Element();
                    twoSqrt.mCode = ElementCode.MULT;
                    twoSqrt.mFirstOperand = Element.TWO;
                    twoSqrt.mSecondOperand = pElement;
                    retElement.mSecondOperand = twoSqrt;
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }

                break;

            case ElementCode.ELEMENT_CODE_LOG:
                if(! firstOperandDerivZero)
                {
                    retElement.mCode = ElementCode.DIV;
                    retElement.mFirstOperand = firstOperandDerivExpression;
                    retElement.mSecondOperand = firstOperand;
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }
                break;

            case ElementCode.ELEMENT_CODE_POW:
                if(! firstOperandDerivZero)
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement.mCode = ElementCode.MULT;
                        Element sum = new Element();
                        retElement.mFirstOperand = sum;
                        sum.mCode = ElementCode.ADD;


                        Element xlogx = new Element();
                        xlogx.mCode = ElementCode.MULT;
                        xlogx.mFirstOperand = firstOperand;
                        Element logx = new Element();
                        logx.mCode = ElementCode.LOG;
                        logx.mFirstOperand = firstOperand;
                        xlogx.mSecondOperand = logx;
                        
                        if(! secondOperandDerivUnity)
                        {
                            Element sumFirstTerm = new Element();
                            sumFirstTerm.mCode = ElementCode.MULT;
                            sumFirstTerm.mFirstOperand = secondOperandDerivExpression;
                            sumFirstTerm.mSecondOperand = xlogx;
                            sum.mFirstOperand = sumFirstTerm;
                        }
                        else
                        {
                            sum.mFirstOperand = xlogx;
                        }

                        if(! firstOperandDerivUnity)
                        {
                            Element sumSecondTerm = new Element();
                            sum.mSecondOperand = sumSecondTerm;
                            sumSecondTerm.mCode = ElementCode.MULT;
                            sumSecondTerm.mFirstOperand = firstOperandDerivExpression;
                            sumSecondTerm.mSecondOperand = secondOperand;
                        }
                        else
                        {
                            sum.mSecondOperand = secondOperand;
                        }

                        Element yminus1 = new Element();

                        if(! secondOperand.mCode.equals(ElementCode.NUMBER))
                        {
                            yminus1.mCode = ElementCode.SUBT;
                            yminus1.mFirstOperand = secondOperand;
                            yminus1.mSecondOperand = Element.ONE;
                        }
                        else
                        {
                            double newVal = secondOperand.mNumericValue - 1.0;
                            if(newVal != 1.0)
                            {
                                yminus1.mCode = ElementCode.NUMBER;
                                yminus1.mNumericValue = newVal;
                            }
                            else
                            {
                                yminus1 = null;
                            }
                        }

                        Element xtoyminus1 = new Element();
                        
                        if(null != yminus1)
                        {
                            xtoyminus1.mCode = ElementCode.POW;
                            xtoyminus1.mFirstOperand = firstOperand;
                            xtoyminus1.mSecondOperand = yminus1;
                        }
                        else
                        {
                            xtoyminus1 = firstOperand;
                        }

                        retElement.mSecondOperand = xtoyminus1;
                    }
                    else
                    {
                        retElement.mCode = ElementCode.MULT;

                        if(! firstOperandDerivUnity)
                        {
                            Element xprimey = new Element();
                            xprimey.mCode = ElementCode.MULT;
                            xprimey.mFirstOperand = firstOperandDerivExpression;
                            xprimey.mSecondOperand = secondOperand;
                            retElement.mFirstOperand = xprimey;
                        }
                        else
                        {
                            retElement.mFirstOperand = secondOperand;
                        }

                        Element yminus1 = new Element();

                        if(! secondOperand.mCode.equals(ElementCode.NUMBER))
                        {
                            yminus1.mCode = ElementCode.SUBT;
                            yminus1.mFirstOperand = secondOperand;
                            yminus1.mSecondOperand = Element.ONE;                        
                        }
                        else
                        {
                            double newExp = secondOperand.mNumericValue - 1.0;
                            if(newExp != 1.0)
                            {
                                yminus1.mCode = ElementCode.NUMBER;
                                yminus1.mNumericValue = newExp;
                            }
                            else
                            {
                                yminus1 = null;
                            }
                        }

                        Element xtoyminus1 = new Element();
                        if(null != yminus1)
                        {
                            xtoyminus1.mCode = ElementCode.POW;
                            xtoyminus1.mFirstOperand = firstOperand;
                            xtoyminus1.mSecondOperand = yminus1;
                        }
                        else
                        {
                            xtoyminus1 = firstOperand;
                        }

                        retElement.mSecondOperand = xtoyminus1;
                    }
                }
                else
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement.mCode = ElementCode.MULT;

                        Element logx = new Element();
                        logx.mCode = ElementCode.LOG;
                        logx.mFirstOperand = firstOperand;

                        if(! secondOperandDerivUnity)
                        {
                            Element yprimelogx = new Element();
                            yprimelogx.mCode = ElementCode.MULT;
                            yprimelogx.mFirstOperand = secondOperandDerivExpression;
                            yprimelogx.mSecondOperand = logx;
                            retElement.mFirstOperand = yprimelogx;
                        }
                        else
                        {
                            retElement.mFirstOperand = logx;
                        }

                        retElement.mSecondOperand = pElement;
                    }
                    else
                    {
                        retElement = firstOperandDerivExpression;
                    }
                }
                break;

            case ElementCode.ELEMENT_CODE_EXP:
                if(! firstOperandDerivZero)
                {
                    if(! firstOperandDerivUnity)
                    {
                        retElement.mCode = ElementCode.MULT;
                        retElement.mFirstOperand = firstOperandDerivExpression;
                        retElement.mSecondOperand = pElement;
                    }
                    else
                    {
                        retElement = pElement;
                    }
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }
                break;

            case ElementCode.ELEMENT_CODE_NEG:
                if(! firstOperandDerivZero)
                {
                    if(! firstOperandDerivUnity)
                    {
                        retElement.mCode = ElementCode.NEG;
                        retElement.mFirstOperand = firstOperandDerivExpression;
                    }
                    else
                    {
                        retElement.mCode = ElementCode.NUMBER;
                        retElement.mNumericValue = -1.0;
                    }
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }
                break;

            case ElementCode.ELEMENT_CODE_TAN:
                if(! firstOperandDerivZero)
                {
                    retElement.mCode = ElementCode.DIV;
                    retElement.mFirstOperand = firstOperandDerivExpression;
                    Element cosSq = new Element();
                    cosSq.mCode = ElementCode.POW;
                    Element cos = new Element();
                    cos.mCode = ElementCode.COS;
                    cos.mFirstOperand = firstOperand;
                    cosSq.mFirstOperand = cos;
                    cosSq.mSecondOperand = Element.TWO;
                    retElement.mSecondOperand = cosSq;
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }
                break;

            case ElementCode.ELEMENT_CODE_COS:
                if(! firstOperandDerivZero)
                {
                    if(! firstOperandDerivUnity)
                    {
                        retElement.mCode = ElementCode.NEG;
                        Element prod = new Element();
                        prod.mCode = ElementCode.MULT;
                        retElement.mFirstOperand = prod;
                        Element sinFunc = new Element();
                        sinFunc.mCode = ElementCode.SIN;
                        sinFunc.mFirstOperand = firstOperand;
                        prod.mFirstOperand = sinFunc;
                        prod.mSecondOperand = firstOperandDerivExpression;
                    }
                    else
                    {
                        retElement.mCode = ElementCode.NEG;
                        Element sinFunc = new Element();
                        sinFunc.mCode = ElementCode.SIN;
                        sinFunc.mFirstOperand = firstOperand;
                        retElement.mFirstOperand = sinFunc;
                    }
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }

                break;

            case ElementCode.ELEMENT_CODE_SIN:
                if(! firstOperandDerivZero)
                {
                    if(! firstOperandDerivUnity)
                    {
                        retElement.mCode = ElementCode.MULT;
                        Element cosFunc = new Element();
                        cosFunc.mCode = ElementCode.COS;
                        cosFunc.mFirstOperand = firstOperand;
                        retElement.mFirstOperand = cosFunc;
                        retElement.mSecondOperand = firstOperandDerivExpression;
                    }
                    else
                    {
                        retElement.mCode = ElementCode.COS;
                        retElement.mFirstOperand = firstOperand;
                    }
                }
                else
                {
                    retElement = firstOperandDerivExpression;
                }

                break;

            case ElementCode.ELEMENT_CODE_ADD:
                if(! firstOperandDerivZero)
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement.mCode = ElementCode.ADD;
                        retElement.mFirstOperand = firstOperandDerivExpression;
                        retElement.mSecondOperand = secondOperandDerivExpression;
                    }
                    else
                    {
                        retElement = firstOperandDerivExpression;
                    }
                }
                else
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement = secondOperandDerivExpression;
                    }
                    else
                    {
                        retElement = firstOperandDerivExpression;
                    }
                }

                break;
                
            case ElementCode.ELEMENT_CODE_SUBT:
                if(! firstOperandDerivZero)
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement.mCode = ElementCode.SUBT;
                        retElement.mFirstOperand = firstOperandDerivExpression;
                        retElement.mSecondOperand = secondOperandDerivExpression;
                    }
                    else
                    {
                        retElement = firstOperandDerivExpression;
                    }
                }
                else
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement.mCode = ElementCode.NEG;
                        retElement.mFirstOperand = secondOperandDerivExpression;
                    }
                    else
                    {
                        retElement = firstOperandDerivExpression;
                    }
                }

                break;
                
            case ElementCode.ELEMENT_CODE_DIV:
                if(! firstOperandDerivZero)
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement.mCode = ElementCode.SUBT;
                        Element firstTerm = new Element();
                        firstTerm.mCode = ElementCode.DIV;
                        firstTerm.mFirstOperand = firstOperandDerivExpression;
                        firstTerm.mSecondOperand = secondOperand;

                        retElement.mFirstOperand = firstTerm;

                        Element secondTerm = new Element();
                        secondTerm.mCode = ElementCode.DIV;

                        Element secondTermNum = null;

                        if(! secondOperandDerivUnity)
                        {
                            secondTermNum = new Element();
                            secondTermNum.mCode = ElementCode.MULT;
                            secondTermNum.mFirstOperand = firstOperand;
                            secondTermNum.mSecondOperand = secondOperandDerivExpression;
                        }
                        else
                        {
                            secondTermNum = firstOperand;
                        }
                        
                        secondTerm.mFirstOperand = secondTermNum;

                        Element secondTermDenom = new Element();
                        secondTermDenom.mCode = ElementCode.POW;
                        secondTermDenom.mFirstOperand = secondOperand;
                        secondTermDenom.mSecondOperand = Element.TWO;
                        secondTerm.mSecondOperand = secondTermDenom;

                        retElement.mSecondOperand = secondTerm;
                    }
                    else
                    {
                        retElement.mCode = ElementCode.DIV;
                        retElement.mFirstOperand = firstOperandDerivExpression;
                        retElement.mSecondOperand = secondOperand;
                    }
                }
                else
                {
                    if(! secondOperandDerivZero)
                    {
                        if(! secondOperandDerivUnity)
                        {
                            retElement.mCode = ElementCode.NEG;
                            
                            Element secondTermArg = new Element();
                            secondTermArg.mCode = ElementCode.DIV;
                            retElement.mFirstOperand = secondTermArg;
                            
                            Element secondTermNum = new Element();
                            secondTermNum.mCode = ElementCode.MULT;
                            secondTermNum.mFirstOperand = firstOperand;
                            secondTermNum.mSecondOperand = secondOperandDerivExpression;
                            secondTermArg.mFirstOperand = secondTermNum;
                            
                            Element secondTermDenom = new Element();
                            secondTermDenom.mCode = ElementCode.POW;
                            secondTermDenom.mFirstOperand = secondOperand;
                            secondTermDenom.mSecondOperand = Element.TWO;
                            secondTermArg.mSecondOperand = secondTermDenom;
                        }
                        else
                        {
                            retElement.mCode = ElementCode.NEG;
                            
                            Element secondTermArg = new Element();
                            secondTermArg.mCode = ElementCode.DIV;
                            retElement.mFirstOperand = secondTermArg;
                            
                            Element secondTermNum = firstOperand;
                            secondTermArg.mFirstOperand = secondTermNum;

                            Element secondTermDenom = new Element();
                            secondTermDenom.mCode = ElementCode.POW;
                            secondTermDenom.mFirstOperand = secondOperand;
                            secondTermDenom.mSecondOperand = Element.TWO;
                            secondTermArg.mSecondOperand = secondTermDenom;                            
                        }
                    }
                    else
                    {
                        retElement = secondOperandDerivExpression;
                    }
                }
                break;

            case ElementCode.ELEMENT_CODE_MULT:

                if(! firstOperandDerivZero)
                {
                    if(! secondOperandDerivZero)
                    {
                        retElement.mCode = ElementCode.ADD;

                        if(! firstOperandDerivUnity)
                        {
                            if(! secondOperandDerivUnity)
                            { 
                                Element firstTerm = new Element();
                                firstTerm.mCode = ElementCode.MULT;
                                firstTerm.mFirstOperand = firstOperandDerivExpression;
                                firstTerm.mSecondOperand = secondOperand;
                                Element secondTerm = new Element();
                                secondTerm.mCode = ElementCode.MULT;
                                secondTerm.mFirstOperand = firstOperand;
                                secondTerm.mSecondOperand = secondOperandDerivExpression;
                                retElement.mFirstOperand = firstTerm;
                                retElement.mSecondOperand = secondTerm;
                            }
                            else
                            {
                                Element firstTerm = new Element();
                                firstTerm.mCode = ElementCode.MULT;
                                firstTerm.mFirstOperand = firstOperandDerivExpression;
                                firstTerm.mSecondOperand = secondOperand;
                                Element secondTerm = firstOperand;
                                retElement.mFirstOperand = firstTerm;
                                retElement.mSecondOperand = secondTerm;                                
                            }
                        }
                        else
                        {
                            if(! secondOperandDerivUnity)
                            {
                                Element firstTerm = secondOperand;
                                Element secondTerm = new Element();
                                secondTerm.mCode = ElementCode.MULT;
                                secondTerm.mFirstOperand = firstOperand;
                                secondTerm.mSecondOperand = secondOperandDerivExpression;
                                retElement.mFirstOperand = firstTerm;
                                retElement.mSecondOperand = secondTerm;
                            }
                            else
                            {
                                retElement.mFirstOperand = firstOperand;
                                retElement.mSecondOperand = secondOperand;
                            }
                        }
                    }
                    else
                    {
                        if(! firstOperandDerivUnity)
                        {
                            retElement.mCode = ElementCode.MULT;
                            retElement.mFirstOperand = firstOperandDerivExpression;
                            retElement.mSecondOperand = secondOperand;
                        }
                        else
                        {
                            retElement = secondOperand;
                        }
                    }
                }
                else
                {
                    if(! secondOperandDerivZero)
                    {
                        if(! secondOperandDerivUnity)
                        {
                            retElement.mCode = ElementCode.MULT;
                            retElement.mFirstOperand = firstOperand;
                            retElement.mSecondOperand = secondOperandDerivExpression;
                        }
                        else
                        {
                            retElement = firstOperand;
                        }
                    }
                    else
                    {
                        retElement = secondOperandDerivExpression;
                    }
                }

                break;

            default:
                throw new IllegalStateException("unable to differentiate element code: " + pElement.mCode);
        }


        return(retElement);
    }

    public Expression computePartialDerivative(Symbol pSymbol)
    {
        Expression expression = new Expression();
        expression.mRootElement = computePartialDerivative(mRootElement, pSymbol);
        return(expression);
    }

                                               
    public static final void main(String []pArgs)
    {
        try
        {
            InputStream in = System.in;
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader bufReader = new BufferedReader(reader);
            String line = null;
            while(null != (line = bufReader.readLine()))
            {
                Expression expression = new Expression(line);
                Symbol X = new Symbol("X");
                System.out.println(expression.computePartialDerivative(X).toString());
                System.out.println("");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
