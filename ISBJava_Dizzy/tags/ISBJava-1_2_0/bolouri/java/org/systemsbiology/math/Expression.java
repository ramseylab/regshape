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
 * <code>rand()</code>, <code>theta()</code>.
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

        static
        {
            mFunctionsMap = new HashMap();
        }

        private ElementCode(String pName)
        {
            this(pName, false);
        }
        private ElementCode(String pName, boolean pIsFunction)
        {
            mName = pName;
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
        public static final ElementCode NONE = new ElementCode("none");

        /**
         * element code indicating that the element is a &quot;symbol&quot;
         * (like a variable)
         */
        public static final ElementCode SYMBOL = new ElementCode("symbol");

        /**
         * element code indicating that the element is a &quot;value&quot;
         * (like a number)
         */
        public static final ElementCode NUMBER = new ElementCode("number");

        /**
         * element code indicating that the element is an operation
         * element specifying the multiplication of its two child operands
         */
        public static final ElementCode MULT = new ElementCode("mult");

        /**
         * element code indicating that the element is an operation
         * element specifying the addition of its two child operands
         */
        public static final ElementCode ADD = new ElementCode("add");

        /**
         * element code indicating that the element is an operation
         * element specifying the subtraction of its two child operands
         * (the second operand is to be subtracted from the first)
         */
        public static final ElementCode SUBT = new ElementCode("subt");

        /**
         * element code indicating that the element is an operation
         * element specifying the division of its two child operands
         */
        public static final ElementCode DIV = new ElementCode("div");

        /**
         * element code indicating that the element is an operation
         * element specifying the exponentiation of its first operand
         * by the value of the second operand
         */    
        public static final ElementCode POW = new ElementCode("pow");

        /**
         * element code indicating that the element is an operation
         * element specifying the modulus (remainder) of the quotient
         * of the first and second operands.
         */    
        public static final ElementCode MOD = new ElementCode("mod");

        /**
         * element code indicating that the element is an operation
         * element specifying the negation of its first (and only)
         * operand
         */    
        public static final ElementCode NEG = new ElementCode("neg");

        /**
         * element code specifying the exponential function of the 
         * first (and only) argument
         */
        public static final ElementCode EXP = new ElementCode("exp", true);

        /**
         * element code specifying the logarithm function (base e) of the 
         * first (and only) argument
         */
        public static final ElementCode LOG = new ElementCode("log", true);

        /**
         * element code specifying the sine function of the 
         * first (and only) argument; argument is in radians
         */
        public static final ElementCode SIN = new ElementCode("sin", true);

        /**
         * element code specifying the cosine function of the 
         * first (and only) argument; argument is in radians
         */
        public static final ElementCode COS = new ElementCode("cos", true);

        /**
         * element code specifying the tangent function of the 
         * first (and only) argument; argument is in radians
         */
        public static final ElementCode TAN = new ElementCode("tan", true);

        /**
         * element code specifying the inverse sine function of the 
         * first (and only) argument; argument is in the range [-1,1];
         * return value is in radians
         */
        public static final ElementCode ASIN = new ElementCode("asin", true);

        /**
         * element code specifying the inverse cosine function of the 
         * first (and only) argument; argument is in the range [-1,1];
         * return value is in radians
         */
        public static final ElementCode ACOS = new ElementCode("acos", true);

        /**
         * element code specifying the inverse tangent function of the 
         * first (and only) argument; return value is in radians
         */
        public static final ElementCode ATAN = new ElementCode("atan", true);

        /**
         * element code specifying the absolute value of the 
         * first (and only) argument
         */
        public static final ElementCode ABS = new ElementCode("abs", true);

        /**
         * element code specifying the greatest integer value that is
         * less than or equal to the argument
         */
        public static final ElementCode FLOOR = new ElementCode("floor", true);

        /**
         * element code specifying the smallest integer value that is
         * greater than or equal to the argument
         */
        public static final ElementCode CEIL = new ElementCode("ceil", true);

        /**
         * element code specifying the square root of the argument, which 
         * must be nonnegative.
         */
        public static final ElementCode SQRT = new ElementCode("sqrt", true);

        /**
         * element code specifying the natural logarithm of the gamma function 
         * of the argument
         */
        public static final ElementCode GAMMALN = new ElementCode("gammaln", true);

        /**
         * element code specifying the natural logarithm of the gamma function 
         * of the argument
         */
        public static final ElementCode THETA = new ElementCode("theta", true);

        /**
         * element code specifying a random number chosen from the unit interval
         * with uniform distribution (inclusive of 0.0, exclusive of 1.0).
         */
        public static final ElementCode RAND = new ElementCode("rand", true);
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
    class Element implements Cloneable
    {


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
        public String toString() throws IllegalStateException
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
                sb.append(mFirstOperand.toString());
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
                        sb.append(mFirstOperand.toString());
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
                        sb.append(mFirstOperand.toString());
                        if(mFirstOperand.mCode != ElementCode.SYMBOL)
                        {
                            sb.append(")");
                        }
                        sb.append(operatorSymbol);
                        if(mSecondOperand.mCode != ElementCode.SYMBOL)
                        {
                            sb.append("(");
                        }
                        sb.append(mSecondOperand.toString());
                        if(mSecondOperand.mCode != ElementCode.SYMBOL)
                        {
                            sb.append(")");
                        }                        
                    }
                    else if(code == ElementCode.SYMBOL)
                    {
                        sb.append(mSymbol.getName());
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
                ElementCode functionCode = parseFunctionName(symbolName);

                // check to see if this is a function call
                if(iter.hasNext())
                {
                    // check to see if next token is an expression token
                    Token nextTok = (Token) iter.next();
                    if(nextTok.mCode == TokenCode.EXPRESSION)
                    {
                        Element functionCallElement = new Element();
                        functionCallElement.mFirstOperand = nextTok.mParsedExpression;
                        if(null != functionCode)
                        {
                            functionCallElement.mCode = functionCode;
                        }
                        else
                        {
                            throw new IllegalArgumentException("unknown symbol used as function name: " + symbolName);
                        }
                        nextTok.mParsedExpression = functionCallElement;
                        iter.previous();
                        iter.previous();
                        iter.remove();   // remove the previous symbol token, since it is not needed anymore
                        iter.next();
                    }
                    else
                    {
                        if(null != functionCode)
                        {
                            throw new IllegalArgumentException("reserved function name used as symbol: " + symbolName);
                        }
                    }
                }
                else
                {
                    if(null != functionCode)
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

    private double valueOfSubtree(Element pElement, SymbolEvaluator pSymbolValueMap) throws DataNotFoundException
    {
        double valueOfSubtree = 0.0;
        ElementCode elementCode = pElement.mCode;

        if(elementCode == ElementCode.NUMBER)
        {
            valueOfSubtree = pElement.mNumericValue;
        }
        else if(elementCode == ElementCode.SYMBOL)
        {
            Symbol symbol = pElement.mSymbol;
            if(null == symbol)
            {
                throw new IllegalArgumentException("found an element in the tree with element code ElementCode.SYMBOL, but with a null symbol object");
            }
            // the value of the element is the value of the symbol
            valueOfSubtree = pSymbolValueMap.getValue(symbol);
        }
        else
        {
            // the element must be an operation element, either unary or binary-- 
            // so get the value of the first operand

            Element firstOperand = pElement.mFirstOperand;
            if(null == firstOperand)
            {
                // must be a function call with no arguments
                if(elementCode == ElementCode.RAND)
                {
                    valueOfSubtree = Math.random();
                }
                else
                {
                    throw new IllegalArgumentException("an argument is required for expression element: \"" + elementCode + "\"");
                }
            }
            else
            {
                double valueOfFirstOperand = valueOfSubtree(firstOperand, pSymbolValueMap);

                if(elementCode == ElementCode.NEG)
                {
                    valueOfSubtree = -1.0 * valueOfFirstOperand;
                }
                else if(elementCode == ElementCode.EXP)
                {
                    valueOfSubtree = Math.exp(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.LOG)
                {
                    valueOfSubtree = Math.log(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.SIN)
                {
                    valueOfSubtree = Math.sin(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.COS)
                {
                    valueOfSubtree = Math.cos(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.TAN)
                {
                    valueOfSubtree = Math.tan(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.ASIN)
                {
                    valueOfSubtree = Math.asin(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.ACOS)
                {
                    valueOfSubtree = Math.acos(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.ATAN)
                {
                    valueOfSubtree = Math.atan(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.ABS)
                {
                    valueOfSubtree = Math.abs(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.FLOOR)
                {
                    valueOfSubtree = Math.floor(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.CEIL)
                {
                    valueOfSubtree = Math.ceil(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.SQRT)
                {
                    valueOfSubtree = Math.sqrt(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.GAMMALN)
                {
                    valueOfSubtree = MathFunctions.gammaln(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.THETA)
                {
                    valueOfSubtree = MathFunctions.thetaFunction(valueOfFirstOperand);
                }
                else if(elementCode == ElementCode.RAND)
                {
                    throw new IllegalArgumentException("function rand() does not permit specification of a function argument");
                }
                else
                {
                    // must be a binary operator
                    Element secondOperand = pElement.mSecondOperand;
                    if(null == secondOperand)
                    {
                        throw new IllegalArgumentException("found an element in the tree with an element code corresponding to a binary operator, but with a null second operand field");
                    }
                    double valueOfSecondOperand = valueOfSubtree(secondOperand, pSymbolValueMap);
                    if(elementCode == ElementCode.MULT)
                    {
                        valueOfSubtree = valueOfFirstOperand * valueOfSecondOperand;
                    }
                    else if(elementCode == ElementCode.ADD)
                    {
                        valueOfSubtree = valueOfFirstOperand + valueOfSecondOperand;
                    }
                    else if(elementCode == ElementCode.DIV)
                    {
                        valueOfSubtree = valueOfFirstOperand / valueOfSecondOperand;
                    }
                    else if(elementCode == ElementCode.SUBT)
                    {
                        valueOfSubtree = valueOfFirstOperand - valueOfSecondOperand;
                    }
                    else if(elementCode == ElementCode.POW)
                    {
                        valueOfSubtree = Math.pow(valueOfFirstOperand, valueOfSecondOperand);
                    }
                    else if(elementCode == ElementCode.MOD)
                    {
                        valueOfSubtree = valueOfFirstOperand % valueOfSecondOperand;
                    }
                    else if(elementCode.isFunction())
                    {
                        throw new IllegalArgumentException("function \"" + elementCode + "\" is not permitted to have two arguments");
                    }
                    else
                    {
                        throw new IllegalArgumentException("found an element with an unknown element code: " + elementCode);
                    }
                }
            }
        }
        return(valueOfSubtree);
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
        String retStr;
        Element rootElement = getRootElement();
        if(null != rootElement)
        {
            retStr = rootElement.toString();
        }
        else
        {
            retStr = "null";
        }
        return(retStr);
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
    public double computeValue(SymbolEvaluator pSymbolValueMap) throws DataNotFoundException, IllegalStateException, IllegalArgumentException
    {
        Element rootElement = getRootElement();
        if(null == rootElement)
        {
            throw new IllegalStateException("attempted to compute value of a math expression object that has no expression defined");
        }
        double retVal = 0.0;
        try
        {
            retVal = valueOfSubtree(rootElement, pSymbolValueMap);
        }
        catch(java.lang.StackOverflowError e)
        {
            throw new IllegalArgumentException("circular expression encountered while attempting to parse expression: " + toString());
        }


        return(valueOfSubtree(rootElement, pSymbolValueMap));
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
}
