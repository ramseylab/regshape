package org.systemsbiology.chem.scripting;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.*;
import java.util.regex.*;
import java.util.*;

import org.systemsbiology.chem.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;

public class ModelBuilderCommandLanguage implements IModelBuilder, IAliasableClass
{
    private HashMap mDefaultModelSymbols;

    public static final String CLASS_ALIAS = "command-language";
    private static final String DEFAULT_MODEL_NAME = "model";
    private static final String TOKEN_MULTILINE_COMMENT_END = "*/";
    private static final String TOKEN_MULTILINE_COMMENT_BEGIN = "/*";
    private static final String TOKEN_SIMPLE_COMMENT = "//";
    private static final String REACTION_MODIFIER_STEPS = "steps:";
    private static final String REACTION_MODIFIER_DELAY = "delay:";

    private static final String STATEMENT_KEYWORD_INCLUDE = "include";
    private static final String STATEMENT_KEYWORD_MODEL = "model";
    private static final String STATEMENT_KEYWORD_REF = "ref";
    private static final String STATEMENT_KEYWORD_DEFINE = "define";
    private static final String NAMESPACE_IDENTIFIER = "::";
    private static final String KEYWORD_LOOP = "loop";

    private static final String VALID_SYMBOL_REGEX = "^[_a-zA-Z]([_a-zA-Z0-9])*$";
    private static final Pattern VALID_SYMBOL_PATTERN = Pattern.compile(VALID_SYMBOL_REGEX);

    private String mNamespace;

    static class Macro extends SymbolValue
    {
        public String mMacroName;
        public ArrayList mExternalSymbols;
        public LinkedList mTokenList;

        public Macro(String pMacroName)
        {
            super(pMacroName);
            mMacroName = pMacroName;
        }
    }

    static class DummySymbol extends SymbolValue
    {
        public Object mInstanceSymbolObject;

        public DummySymbol(String pDummySymbolName, Object pInstanceSymbolObject)
        {
            super(pDummySymbolName);
            mInstanceSymbolObject = pInstanceSymbolObject;
        }
    }

    static class Token
    {
        static class Code
        {
            private final String mName;
            private Code(String pName)
            {
                mName = pName;
            }

            public static final Code POUNDSIGN = new Code("#");
            public static final Code ATSIGN = new Code("@");
            public static final Code EQUALS = new Code("=");
            public static final Code SYMBOL = new Code("symbol");
            public static final Code HYPHEN = new Code("-");
            public static final Code COMMA = new Code(",");
            public static final Code GREATER_THAN = new Code(">");
            public static final Code PLUS = new Code("+");
            public static final Code BRACKET_BEGIN = new Code("[");
            public static final Code BRACKET_END = new Code("]");
            public static final Code PAREN_BEGIN = new Code("(");
            public static final Code PAREN_END = new Code(")");
            public static final Code BRACE_BEGIN = new Code("{");
            public static final Code BRACE_END = new Code("}");
            public static final Code QUOTE = new Code("\"");
            public static final Code SEMICOLON = new Code(";");
            public static final Code DOLLAR = new Code("$");
            public static final Code ASTERISK = new Code("*");
            public static final Code PERCENT = new Code("%");
            public static final Code RIGHT_SLASH = new Code("/");
            public static final Code CARET = new Code("^");
        }
        
        Code mCode;
        String mSymbol;
        int mLine;
        
        public Token(Code pCode)
        {
            mCode = pCode;
        }

        public String toString()
        {
            String string = null;
            if(mCode.equals(Code.SYMBOL))
            {
                string = mSymbol;
            }
            else
            {
                string = mCode.mName;
            }
            return(string);
        }
    }

    private Pattern mSearchPatternMath;

    private Pattern getSearchPatternMath()
    {
        return(mSearchPatternMath);
    }

    private void setSearchPatternMath(Pattern pSearchPatternMath)
    {
        mSearchPatternMath = pSearchPatternMath;
    }


    private void initializeSearchPatternMath()
    {
        String searchRegex = "\\[([^\\[\\]]+)\\]";
        Pattern searchPattern = Pattern.compile(searchRegex);
        setSearchPatternMath(searchPattern);
    }

    private static final String COMPARTMENT_NAME_DEFAULT = "univ";

    private void initializeDefaultModelSymbols()
    {
        mDefaultModelSymbols = new HashMap();
        Compartment compartment = new Compartment(COMPARTMENT_NAME_DEFAULT);
        mDefaultModelSymbols.put(compartment.getName(), compartment);
    }

    public ModelBuilderCommandLanguage()
    {
        initializeSearchPatternMath();
        initializeDefaultModelSymbols();
        mNamespace = null;
    }

    private Token getNextToken(ListIterator pTokenIter) throws InvalidInputException
    {
        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("expected a token, but no token was found");
        }

        Token token = (Token) pTokenIter.next();
        assert (null != token) : "unexpected null token";

        return(token);
    }


    private void defineParameters(HashMap pSymbolMap, Model pModel)
    {
        Iterator symbolValueIter = pSymbolMap.values().iterator();
        while(symbolValueIter.hasNext())
        {
            SymbolValue symbolValue = (SymbolValue) symbolValueIter.next();
            if(symbolValue.getClass().getSuperclass().equals(Object.class))
            {
                Parameter parameter = new Parameter(symbolValue);
                pModel.addParameter(parameter);
            }
        }
    }

    private void tokenizeStatement(String pStatement, List pTokens, int pStartingLineNumber) 
    {
        StringTokenizer st = new StringTokenizer(pStatement, "=\", \t[]{}()->+;@#$*/%^\n", true);

        String tokenString = null;
        boolean inQuote = false;
        StringBuffer symbolTokenBuffer = new StringBuffer();
        int lineCtr = pStartingLineNumber;

        while(st.hasMoreElements())
        {
            tokenString = st.nextToken();

            Token token = null;


            if(tokenString.equals("\n"))
            {
                ++lineCtr;
            }
            else
            {
                if(tokenString.equals("\""))
                {
                    if(inQuote)
                    {
                        inQuote = false;
                    }
                    else
                    {
                        inQuote = true;
                    }

                    token = new Token(Token.Code.QUOTE);
                }
                else
                {
                    if(! inQuote)
                    {
                        if(tokenString.equals("="))
                        {
                            token = new Token(Token.Code.EQUALS);
                        }
                        else if(tokenString.equals(","))
                        {
                            token = new Token(Token.Code.COMMA);
                        }
                        else if(tokenString.equals("="))
                        {
                            token = new Token(Token.Code.EQUALS);
                        }
                        else if(tokenString.equals(" "))
                        {
                            // this is whitespace, just ignore
                        }
                        else if(tokenString.equals("\t"))
                        {
                            // this is a tab character, just ignore
                        }
                        else if(tokenString.equals("("))
                        {
                            token = new Token(Token.Code.PAREN_BEGIN);
                        }
                        else if(tokenString.equals(")"))
                        {
                            token = new Token(Token.Code.PAREN_END);
                        }
                        else if(tokenString.equals("["))
                        {
                            token = new Token(Token.Code.BRACKET_BEGIN);
                        }
                        else if(tokenString.equals("]"))
                        {
                            token = new Token(Token.Code.BRACKET_END);
                        }
                        else if(tokenString.equals("{"))
                        {
                            token = new Token(Token.Code.BRACE_BEGIN);
                        }
                        else if(tokenString.equals("}"))
                        {
                            token = new Token(Token.Code.BRACE_END);
                        }
                        else if(tokenString.equals("-"))
                        {
                            token = new Token(Token.Code.HYPHEN);
                        }
                        else if(tokenString.equals(">"))
                        {
                            token = new Token(Token.Code.GREATER_THAN);
                        }
                        else if(tokenString.equals("+"))
                        {
                            token = new Token(Token.Code.PLUS);
                        }
                        else if(tokenString.equals(";"))
                        {
                            token = new Token(Token.Code.SEMICOLON);
                        }
                        else if(tokenString.equals("@"))
                        {
                            token = new Token(Token.Code.ATSIGN);
                        }
                        else if(tokenString.equals("#"))
                        {
                            token = new Token(Token.Code.POUNDSIGN);
                        }
                        else if(tokenString.equals("$"))
                        {
                            token = new Token(Token.Code.DOLLAR);
                        }
                        else if(tokenString.equals("/"))
                        {
                            token = new Token(Token.Code.RIGHT_SLASH);
                        }
                        else if(tokenString.equals("*"))
                        {
                            token = new Token(Token.Code.ASTERISK);
                        }
                        else if(tokenString.equals("%"))
                        {
                            token = new Token(Token.Code.PERCENT);
                        }
                        else if(tokenString.equals("^"))
                        {
                            token = new Token(Token.Code.CARET);
                        }
                        else
                        {
                            token = new Token(Token.Code.SYMBOL);
                            token.mSymbol = tokenString;
                        }
                    }
                    else
                    {
                        // we are in a quoted environment; just save the token string
                        symbolTokenBuffer.append(tokenString);
                    }
                }
            }

            if(tokenString.equals("\"") && (! inQuote))
            {
                String symbolTokenString = symbolTokenBuffer.toString();
                if(symbolTokenString.length() > 0)
                {
                    Token symbolToken = new Token(Token.Code.SYMBOL);
                    symbolToken.mSymbol = symbolTokenString;
                    symbolTokenBuffer.delete(0, symbolTokenString.length());
                    pTokens.add(symbolToken);
                }
            }

            if(null != token)
            {
                token.mLine = lineCtr;
                pTokens.add(token);
                token = null;
            }
            else
            {
                // do nothing
            }
        }
    }

    private void checkSymbolValidity(String pSymbolName) throws InvalidInputException
    {
        if(SymbolEvaluatorChemSimulation.isReservedSymbol(pSymbolName))
        {
            throw new InvalidInputException("attempt to define a reserved symbol: " + pSymbolName);
        }
        if(null != mDefaultModelSymbols.get(pSymbolName))
        {
            throw new InvalidInputException("symbol name is reserved as a default model element: " + pSymbolName);
        }

        if(! isValidSymbol(pSymbolName))
        {
            throw new InvalidInputException("invalid symbol definition: " + pSymbolName);
        }
    }

    private void processDefaultModelElements(HashMap pSymbolMap) 
    {
        Iterator defaultModelSymbolsIter = mDefaultModelSymbols.values().iterator();
        while(defaultModelSymbolsIter.hasNext())
        {
            SymbolValue defaultModelSymbolValue = (SymbolValue) defaultModelSymbolsIter.next();
            pSymbolMap.put(defaultModelSymbolValue.getSymbol().getName(), defaultModelSymbolValue);
        }
    }

    static class SymbolEvaluatorNamespaced extends SymbolEvaluatorHashMap
    {
        private String mNamespace;

        public SymbolEvaluatorNamespaced(HashMap pSymbolMap, String pNamespace)
        {
            super(pSymbolMap);
            mNamespace = pNamespace;
        }

        public double getValue(Symbol pSymbol) throws DataNotFoundException
        {
            Object convertedSymbol = performSymbolTokenMacroTranslationLookup(pSymbol.getName(), mSymbolMap, mNamespace);
            double retVal = 0.0;
            if(convertedSymbol instanceof String)
            {
                String symbolName = (String) convertedSymbol;
                retVal = super.getValue(symbolName);
            }
            else if(convertedSymbol instanceof Value)
            {
                Value symbolValue = (Value) convertedSymbol;
                retVal = symbolValue.getValue(this);
            }
            else
            {
                assert false : "unknown converted symbol type, for symbol: " + pSymbol.getName();
            }
            
            return(retVal);
        }
    }

    private static String addNamespaceToSymbol(String pSymbolName, String pNamespace)
    {
        String retName = pSymbolName;
        if(null != pNamespace)
        {
            retName = pNamespace + NAMESPACE_IDENTIFIER + pSymbolName;
        }
        return(retName);
    }

    // When the parser encounters a symbol token, it must check to see if this
    // symbol is one of the special "dummy symbols" in a macro reference.  If so,
    // the "dummy symbol" must be translated in accordance with the macro's translation
    // table.  It can get translated into another symbol (most common), or into a
    // value.  The return type of the function is either String or Value, depending 
    // on whether the translation returns a symbol or a value.
    private static Object performSymbolTokenMacroTranslationLookup(String pSymbol, HashMap pSymbolMap, String pNamespace)
    {
        Object retObj = pSymbol;
        if(null != pNamespace)
        {
            if(! SymbolEvaluatorChemSimulation.isReservedSymbol(pSymbol))
            {
                pSymbol = addNamespaceToSymbol(pSymbol, pNamespace);
                retObj = pSymbol;

                // check to see if this is a dummy symbol
                SymbolValue symbolValue = (SymbolValue) pSymbolMap.get(pSymbol);
                if(null != symbolValue && symbolValue instanceof DummySymbol)
                {
                    DummySymbol dummySymbol = (DummySymbol) symbolValue;
                    Object dummySymbolInstance = dummySymbol.mInstanceSymbolObject;
                    if(dummySymbolInstance instanceof String)
                    {
                        retObj = dummySymbolInstance;
                    }
                    else if(dummySymbolInstance instanceof Value)
                    {
                        retObj = dummySymbolInstance;
                    }
                    else
                    {
                        assert false : "unknown dummy symbol instance class, for symbol: " + pSymbol;
                    }
                }
            }
            else
            {
                // do nothing; reserved symbols do not get namespace scoping
            }
        }
        return(retObj);
    }

    // Handle the occurrence of embedded mathematical expression(s)
    // within a quoted string; if an expression occurs, translate it
    // and truncate the resulting value to an integer, before embedding
    // the result into the string at the point where the expression occurs.
    private String translateMathExpressionsInString(String pInputString, 
                                                    HashMap pSymbolMap) throws DataNotFoundException, IllegalArgumentException
    {
        Pattern searchPatternMath = getSearchPatternMath();
        Matcher matcher = searchPatternMath.matcher(pInputString);
        while(matcher.find())
        {
            String matchedSubsequence = matcher.group(1);
            Expression exp = new Expression(matchedSubsequence);
            SymbolEvaluatorNamespaced evaluator = new SymbolEvaluatorNamespaced(pSymbolMap, mNamespace);
            double value = exp.computeValue(evaluator);
            String formattedExp = Integer.toString((int) value);
            pInputString = matcher.replaceFirst(formattedExp);
            matcher = searchPatternMath.matcher(pInputString);
        }
        return(pInputString);
    }

    // This function is used by the handleStatementMacroReference
    // function, to grab all of the tokens in the token stream up to
    // the next comma "," token or end-parenthesis ")" token.  The
    // reason why we cannot just use "obtainSymbol" is that in a
    // macro reference, either a symbol *or* a value can be provided
    // in the comma-separated list.  It is the only place in the CMDL
    // where either a symbol, or a value/expression is allowed.  So
    // special code is needed to handle this case, since we do not
    // know definitively whether to expect a symbol or a value/expression.
    private void grabTokensToNextCommaOrParen(ListIterator pTokenIter, LinkedList pTokens, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        Token token = null;
        boolean inQuote = false;
        StringBuffer quotedString = new StringBuffer();
        boolean gotFirstToken = false;
        while(pTokenIter.hasNext())
        {
            token = getNextToken(pTokenIter);
            if(! inQuote)
            {
                if(token.mCode.equals(Token.Code.PAREN_END))
                {
                    pTokenIter.previous();
                    break;
                }
                else if(token.mCode.equals(Token.Code.SEMICOLON))
                {
                    throw new InvalidInputException("end of statement reached when parenthesis expected");
                }
                else if(token.mCode.equals(Token.Code.COMMA))
                {
                    pTokenIter.previous();
                    break;
                }
                else if(token.mCode.equals(Token.Code.QUOTE))
                {
                    inQuote = true;
                    if(! gotFirstToken)
                    {
                        gotFirstToken = true;
                    }
                }
                else
                {
                    pTokens.add(token);
                    if(! gotFirstToken)
                    {
                        gotFirstToken = true;
                    }
                }
            }
            else
            {
                if(token.mCode.equals(Token.Code.QUOTE))
                {
                    inQuote = false;
                    Token newToken = new Token(Token.Code.SYMBOL);
                    String symbolName = quotedString.toString();
                    quotedString.delete(0, symbolName.length());
                    symbolName = translateMathExpressionsInString(symbolName, pSymbolMap);
                    newToken.mSymbol = symbolName;
                    pTokens.add(newToken);
                }
                else
                {
                    quotedString.append(token.toString());
                }
            }

        }
        if(! gotFirstToken)
        {
            throw new InvalidInputException("a symbol token was expected in list environment");
        }
    }

    private String obtainSymbol(ListIterator pTokenIter, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        Token token = getNextToken(pTokenIter);
        String symbolName = null;
        if(token.mCode.equals(Token.Code.SYMBOL))
        {
            symbolName = token.mSymbol;
        }
        else
        {
            if(token.mCode.equals(Token.Code.QUOTE))
            {
                token = getNextToken(pTokenIter);
                if(! token.mCode.equals(Token.Code.SYMBOL))
                {
                    throw new InvalidInputException("expected symbol token after quote");
                }

                symbolName = translateMathExpressionsInString(token.mSymbol, pSymbolMap);

                token = getNextToken(pTokenIter);
                if(! token.mCode.equals(Token.Code.QUOTE))
                {
                    throw new InvalidInputException("expected end quote token");
                }
            }
            else
            {
                throw new InvalidInputException("expected symbol or quoted string; instead found token: " + token);
            }
        }

        checkSymbolValidity(symbolName);

        return(symbolName);
    }


    private String obtainSymbolWithNamespace(ListIterator pTokenIter, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        String symbol = obtainSymbol(pTokenIter, pSymbolMap);
        Object convertedSymbolObj = performSymbolTokenMacroTranslationLookup(symbol, pSymbolMap, mNamespace);
        String retSymbol = null;
        if(convertedSymbolObj instanceof String)
        {
            retSymbol = (String) convertedSymbolObj;
        }
        else
        {
            throw new InvalidInputException("where expected a symbol, macro reference translated into a value: " + symbol);
        }
        return(retSymbol);
    }

    // For places where the parser expects to be able to obtain a value, this
    // function walks the token list and attempts to parse a value.  The value
    // may be either a "deferred evaluation" expression, or a simple numeric value.
    private Value obtainValue(ListIterator pTokenIter, HashMap pSymbolMap) throws InvalidInputException
    {
        boolean firstToken = true;
        StringBuffer expressionBuffer = new StringBuffer();
        boolean deferredExpression = false;
        while(pTokenIter.hasNext())
        {
            Token token = getNextToken(pTokenIter);
            String symbolName = null;
            Value symbolValueObj = null;
            String addToExpression = null;

            if(token.mCode.equals(Token.Code.SYMBOL))
            {
                symbolName = token.mSymbol;
                assert (null != symbolName) : "unexpected null symbol name";
                if(! Expression.isFunctionName(symbolName))
                {
                    // is it a numeric literal?
                    boolean isNumericLiteral = true;
                    try
                    {
                        Double.parseDouble(symbolName);
                    }
                    catch(NumberFormatException e)
                    {
                        isNumericLiteral = false;
                    }

                    if(! isNumericLiteral)
                    {
                        Object convertedSymbol = performSymbolTokenMacroTranslationLookup(symbolName, pSymbolMap, mNamespace);
                        if(convertedSymbol instanceof String)
                        {
                            addToExpression = (String) convertedSymbol;
                        }
                        else if(convertedSymbol instanceof Value)
                        {
                            symbolValueObj = (Value) convertedSymbol;
                            if(symbolValueObj.isExpression())
                            {
                                addToExpression = symbolValueObj.getExpressionString();
                            }
                            else
                            {
                                double value = symbolValueObj.getValue();
                                addToExpression = Double.toString(value);
                            }
                        }
                        else
                        {
                            assert false : "unknown symbol type for converted symbol: " + symbolName;
                        }
                    }
                    else
                    {
                        addToExpression = symbolName;
                    }
                }
            }

            if(token.mCode.equals(Token.Code.SEMICOLON))
            {
                if(firstToken)
                {
                    throw new InvalidInputException("semicolon encountered where expression expected");
                }

                pTokenIter.previous();
                break;
            }
            else if(token.mCode.equals(Token.Code.COMMA))
            {
                if(firstToken)
                {
                    throw new InvalidInputException("comma encountered where expression expected");
                }

                pTokenIter.previous();
                break;
            }

            boolean appendToken = false;

            if(firstToken)
            {
                if(token.mCode.equals(Token.Code.BRACKET_BEGIN))
                {
                    deferredExpression = true;
                    // deferred expression
                }
                else
                {
                    // non-deferred expression
                    appendToken = true;
                }
                firstToken = false;
            }
            else
            {
                if(! deferredExpression || ! token.mCode.equals(Token.Code.BRACKET_END))
                {
                    appendToken = true;
                }
                else
                {
                    if(deferredExpression)
                    {
                        break;
                    }
                }
            }

            if(appendToken)
            {
                if(null != addToExpression)
                {
                    expressionBuffer.append(addToExpression);
                }
                else
                {
                    expressionBuffer.append(token.toString());
                }
            }
        }

        String expressionString = expressionBuffer.toString();
        Expression expression = null;
        try
        {
            expression = new Expression(expressionString);
        }
        catch(IllegalArgumentException e)
        {
            throw new InvalidInputException("invalid mathematical formula; cause is: " + e.getMessage() + ";", e);
        }
        Value value = null;
        if(deferredExpression)
        {
            value = new Value(expression);
        }
        else
        {
            try
            {
                value = new Value(expression.computeValue(pSymbolMap));
            }
            catch(DataNotFoundException e)
            {
                throw new InvalidInputException("unable to determine value for expression: " + expressionString, e);
            }
        }

        return(value);
    }

    private void handleStatementAssociate(ListIterator pTokenIter, Model pModelHashMap, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        String symbolName = obtainSymbolWithNamespace(pTokenIter, pSymbolMap);
        assert (null != symbolName) : "null symbol string for symbol token";

        Token token = getNextToken(pTokenIter);
        if(! token.mCode.equals(Token.Code.ATSIGN))
        {
            if(token.mCode.equals(Token.Code.BRACKET_BEGIN))
            {
                throw new InvalidInputException("encountered begin bracket token when expected at-sign token; perhaps you forgot to put double quotes around your symbol definition?");
            }
            else
            {
                throw new InvalidInputException("encountered unknown token when expected at-sign token");
            }
        }

        String associatedSymbolName = obtainSymbolWithNamespace(pTokenIter, pSymbolMap);
        SymbolValue associatedSymbolValue = (SymbolValue) pSymbolMap.get(associatedSymbolName);
        Compartment compartment = null;
        if(! (associatedSymbolValue instanceof Compartment))
        {
            if(! associatedSymbolValue.getClass().getSuperclass().equals(Object.class))
            {
                throw new InvalidInputException("symbol \"" + associatedSymbolName + "\" is already defined as an incompatible symbol type");
            }
            compartment = new Compartment(associatedSymbolValue);
            pSymbolMap.put(associatedSymbolName, compartment);
        }
        else
        {
            compartment = (Compartment) associatedSymbolValue;
        }

        SymbolValue symbolValue = (SymbolValue) pSymbolMap.get(symbolName);
        Species species = null;
        if(! (symbolValue instanceof Species))
        {
            if(! symbolValue.getClass().getSuperclass().equals(Object.class))
            {
                throw new InvalidInputException("symbol \"" + symbolName + "\" is already defined as an incompatible symbol type");
            }
            species = new Species(symbolValue, compartment);
            pSymbolMap.put(symbolName, species);
        }
        else
        {
            species = (Species) symbolValue;
            if(! species.getCompartment().equals(compartment))
            {
                throw new InvalidInputException("species \"" + symbolName + "\" is already assigned to a different compartment: " + compartment.getName());
            }
        }

        getEndOfStatement(pTokenIter);
    }

    private void handleStatementSymbolDefinition(ListIterator pTokenIter, Model pModelHashMap, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        String symbolName = obtainSymbolWithNamespace(pTokenIter, pSymbolMap);

        assert (null != symbolName) : "null symbol string for symbol token";

        Token token = getNextToken(pTokenIter);

        if(! token.mCode.equals(Token.Code.EQUALS))
        {
            if(token.mCode.equals(Token.Code.BRACKET_BEGIN))
            {
                throw new InvalidInputException("encountered begin bracket token when expected equals token; perhaps you forgot to put double quotes around your symbol definition?");
            }
            else
            {
                throw new InvalidInputException("encountered unknown token when expected equals token");
            }
        }
        
        Value value = obtainValue(pTokenIter, pSymbolMap);
        SymbolValue symbolValue = new SymbolValue(symbolName, value);
        SymbolValue foundSymbolValue = (SymbolValue) pSymbolMap.get(symbolName);
        if(null != foundSymbolValue)
        {
            // :BUGBUG: this is a hack until I can get an "if/then/else" construct working
            // in the CMDL.  When defining a vector of species inside a loop, it is
            // often useful to be able to have a nonzero value for just one element of
            // the vector.  With multiple nested loops, this is difficult to accomplish.
            // So for now, we make it legal to redefine the Value after the loop, so
            // long as the symbol value has not yet been turned into a derived class.
            if(null == foundSymbolValue.getValue() ||
                foundSymbolValue.getClass().getSuperclass().equals(Object.class))
            {
                // this SymbolValue either has no Value assigned yet, or it 
                // is just a plain symbol value; it is ok to redefine its value
                foundSymbolValue.setValue(symbolValue.getValue());
            }
            else
            {
                throw new InvalidInputException("a symbol of type " + foundSymbolValue.getClass().getName() + " cannot have its value redefined; symbol name is: " + symbolName);
            }
        }
        else
        {
            pSymbolMap.put(symbolName, symbolValue);
        }

        getEndOfStatement(pTokenIter);
    }

    private Compartment getDefaultCompartment(HashMap pSymbolMap) 
    {
        Compartment compartment = (Compartment) pSymbolMap.get(COMPARTMENT_NAME_DEFAULT);
        assert (null != compartment) : "default compartment not found";
        return(compartment);
    }

    private void getReactionParticipants(ListIterator pTokenIter,
                                         HashMap pSymbolMap,
                                         HashMap pSpeciesStoicMap,
                                         HashMap pSpeciesDynamicMap,
                                         Reaction.ParticipantType pParticipantType) throws InvalidInputException, DataNotFoundException
    {
        while(pTokenIter.hasNext())
        {
            boolean dynamic = true;
            // get symbol
            Token token = getNextToken(pTokenIter);
            if(token.mCode.equals(Token.Code.DOLLAR))
            {
                dynamic = false;
            }
            else
            {
                pTokenIter.previous();
            }
            String speciesName = obtainSymbolWithNamespace(pTokenIter, pSymbolMap);
            MutableInteger speciesStoic = (MutableInteger) pSpeciesStoicMap.get(speciesName);
            if(null == speciesStoic)
            {
                speciesStoic = new MutableInteger(1);
                pSpeciesStoicMap.put(speciesName, speciesStoic);
            }
            else
            {
                int stoic = speciesStoic.getValue() + 1;
                speciesStoic.setValue(stoic);
            }
            MutableBoolean speciesDynamic = (MutableBoolean) pSpeciesDynamicMap.get(speciesName);
            if(null != speciesDynamic)
            {
                if(speciesDynamic.booleanValue() == dynamic)
                {
                    // everything is cool
                }
                else
                {
                    throw new InvalidInputException("species " + speciesName + " is defined both as dynamic and boundary, for the same reaction");
                }
            }
            else
            {
                speciesDynamic = new MutableBoolean(dynamic);
                pSpeciesDynamicMap.put(speciesName, speciesDynamic);
            }

            token = getNextToken(pTokenIter);
            if(pParticipantType.equals(Reaction.ParticipantType.REACTANT) &&
               token.mCode.equals(Token.Code.HYPHEN))
            {
                token = getNextToken(pTokenIter);
                assert (token.mCode.equals(Token.Code.GREATER_THAN)) : "expected greater-than symbol";
                break;
            }
            else if(token.mCode.equals(Token.Code.PLUS))
            {
                continue;
            }
            else if(pParticipantType.equals(Reaction.ParticipantType.PRODUCT) &&
                    token.mCode.equals(Token.Code.COMMA))
            {
                break;
            }
            else
            {
                throw new InvalidInputException("invalid token type encountered in reaction definition: \"" + token.toString() + "\"");
            }
        }
    }
                                         
    private void handleSpeciesDefinitions(Reaction pReaction, 
                                          Reaction.ParticipantType pReactionParticipantType,
                                          HashMap pSymbolMap,
                                          HashMap pSpeciesStoicMap,
                                          HashMap pSpeciesDynamicMap) throws InvalidInputException
    {
        Iterator speciesIter = pSpeciesStoicMap.keySet().iterator();
        while(speciesIter.hasNext())
        {
            String speciesName = (String) speciesIter.next();
            MutableBoolean speciesDynamic = (MutableBoolean) pSpeciesDynamicMap.get(speciesName);
            assert (null != speciesDynamic) : "expected to find non-null object for species: " + speciesName;
            
            boolean dynamic = speciesDynamic.booleanValue();

            MutableInteger speciesStoic = (MutableInteger) pSpeciesStoicMap.get(speciesName);
            assert (null != speciesStoic) : "expected to find non-null object for species: " + speciesName;
            int stoichiometry = speciesStoic.getValue();
            SymbolValue speciesSymbolValue = (SymbolValue) pSymbolMap.get(speciesName);
            if(null == speciesSymbolValue)
            {
                throw new InvalidInputException("species \"" + speciesName + "\" was referenced in a reaction defintion, but was not previously defined");
            }
            Species species = null;
            if(! (speciesSymbolValue instanceof Species))
            {
                if(! speciesSymbolValue.getClass().getSuperclass().equals(Object.class))
                {
                    throw new InvalidInputException("symbol: \"" + speciesName + "\" is already defined as s different (non-species) symbol");
                }
                Compartment compartment = getDefaultCompartment(pSymbolMap);
                species = new Species(speciesSymbolValue, compartment);
                pSymbolMap.put(speciesName, species);
            }
            else
            {
                species = (Species) speciesSymbolValue;
            }    

            pReaction.addSpecies(species, stoichiometry, dynamic, pReactionParticipantType);
        }
    }

    private void handleStatementReaction(ListIterator pTokenIter, Model pModel, HashMap pSymbolMap, MutableInteger pNumReactions) throws InvalidInputException, DataNotFoundException
    {
        
        Token token = null;

        boolean hasName = false;
        boolean hasReactants = false;

        // advance token iterator to reaction symbol "->"
        boolean gotHyphen = false;
        while(pTokenIter.hasNext())
        {
            token = getNextToken(pTokenIter);
            if(token.mCode.equals(Token.Code.HYPHEN))
            {
                gotHyphen = true;
                break;
            }
        }
        assert (gotHyphen) : "unable to locate the \"->\" reaction symbol within reaction statement";

        // back up to the beginning of the reaction statement
        while(pTokenIter.hasPrevious())
        {
            token = (Token) pTokenIter.previous();
            if(token.mCode.equals(Token.Code.COMMA))
            {
                hasName = true;
            }
            else if(token.mCode.equals(Token.Code.PLUS))
            {
                hasReactants = true;
            }
            else if(token.mCode.equals(Token.Code.SYMBOL))
            {
                if(! hasName)
                {
                    hasReactants = true;
                }
            }
            else if(token.mCode.equals(Token.Code.DOLLAR))
            {
                hasReactants = true;
            }
            else if(token.mCode.equals(Token.Code.SEMICOLON))
            {
                pTokenIter.next();
                break;
            }
            else if(token.mCode.equals(Token.Code.BRACE_END))
            {
                pTokenIter.next();
                break;
            }
        }

        String reactionName = null;

        if(hasName)
        {
            reactionName = obtainSymbolWithNamespace(pTokenIter, pSymbolMap);
            token = getNextToken(pTokenIter);
            if(! token.mCode.equals(Token.Code.COMMA))
            {
                throw new InvalidInputException("expected comma after reaction name token");
            }
        }
        else
        {
            int numReactions = pNumReactions.getValue() + 1;
            pNumReactions.setValue(numReactions);
            reactionName = "___r" + numReactions;
        }

        Reaction reaction = new Reaction(reactionName);

        HashMap speciesStoicMap = new HashMap();
        HashMap speciesDynamicMap = new HashMap();

        if(hasReactants)
        {
            getReactionParticipants(pTokenIter,
                                    pSymbolMap,
                                    speciesStoicMap,
                                    speciesDynamicMap,
                                    Reaction.ParticipantType.REACTANT);

            handleSpeciesDefinitions(reaction, 
                                     Reaction.ParticipantType.REACTANT,
                                     pSymbolMap,
                                     speciesStoicMap,
                                     speciesDynamicMap);
        }
        else
        {
            pTokenIter.next();
            pTokenIter.next();
        }

        token = getNextToken(pTokenIter);
        boolean hasProducts = false;
        if(token.mCode.equals(Token.Code.SYMBOL) ||
           token.mCode.equals(Token.Code.QUOTE))
        {
            hasProducts = true;
            pTokenIter.previous();
        }
        else
        {
            if(! token.mCode.equals(Token.Code.COMMA))
            {
                throw new InvalidInputException("expected comma separator between reaction and rate; token is: " + token);
            }
        }

        speciesStoicMap.clear();

        if(hasProducts)
        {
            getReactionParticipants(pTokenIter,
                                    pSymbolMap,
                                    speciesStoicMap,
                                    speciesDynamicMap, 
                                    Reaction.ParticipantType.PRODUCT);

            handleSpeciesDefinitions(reaction, 
                                     Reaction.ParticipantType.PRODUCT,
                                     pSymbolMap,
                                     speciesStoicMap,
                                     speciesDynamicMap);            
        }

        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("incomplete reaction definition; expected to find reaction rate specifier");
        }

        Value rateValue = obtainValue(pTokenIter, pSymbolMap);
        reaction.setRate(rateValue);

        if(null != pSymbolMap.get(reactionName))
        {
            throw new InvalidInputException("already found a symbol defined with name: " + reactionName + "; cannot process reaction definition of same name");
        }

        pSymbolMap.put(reactionName, reaction);
        pModel.addReaction(reaction);

        Token nextToken = getNextToken(pTokenIter);
        if(nextToken.mCode.equals(Token.Code.COMMA))
        {
            // check if next token is a symbol
            nextToken = getNextToken(pTokenIter);
            if(nextToken.mCode.equals(Token.Code.SYMBOL))
            {
                if(nextToken.mSymbol.equals(REACTION_MODIFIER_STEPS))
                {
                    handleMultistepReaction(reaction, pSymbolMap, pTokenIter);
                }
                else if(nextToken.mSymbol.equals(REACTION_MODIFIER_DELAY))
                {
                    handleDelayedReaction(reaction, pSymbolMap, pTokenIter);
                }
                else
                {
                    throw new InvalidInputException("unknown reaction modifier symbol: " + nextToken.mSymbol + "; did you forget to include the reaction modifier \"" + REACTION_MODIFIER_STEPS + "\" or \"" + REACTION_MODIFIER_DELAY + "\"?");
                }
            }
            else
            {
                // For compatibility with early versions of Dizzy, allow for a number
                // to be specified without a modifier; this is taken to represent
                // the number of steps in the reaction
                pTokenIter.previous();
                handleMultistepReaction(reaction, pSymbolMap, pTokenIter);
            }
        }
        else
        {
            pTokenIter.previous();
        }
        
        getEndOfStatement(pTokenIter);
    }

    private void handleDelayedReaction(Reaction pReaction, HashMap pSymbolMap, ListIterator pTokenIter) throws InvalidInputException
    {
        Value delayValue = obtainValue(pTokenIter, pSymbolMap);
        if(delayValue.isExpression())
        {
            throw new InvalidInputException("reaction delay must be specified as a number, not a deferred-evaluation expression");
        }
        double delay = delayValue.getValue();
        if(delay < 0.0)
        {
            throw new InvalidInputException("reaction delay must be a nonnegative number");
        }
        pReaction.setDelay(delay);
    }

    private void handleMultistepReaction(Reaction pReaction, HashMap pSymbolMap, ListIterator pTokenIter) throws InvalidInputException
    {
        Value stepsValue = obtainValue(pTokenIter, pSymbolMap);
        if(stepsValue.isExpression())
        {
            throw new InvalidInputException("number of reaction steps must be specified as a number, not a deferred-evaluation expression");
        }
        int numSteps = (int) stepsValue.getValue();
        if(numSteps <= 0)
        {
            throw new InvalidInputException("invalid number of steps specified");
        }
        else if(numSteps > 1)
        {
            pReaction.setNumSteps(numSteps);
        }
        else
        {
            // number of steps is exactly one; so there is nothing to do
        }        
    }

    private void getEndOfStatement(ListIterator pTokenIter) throws InvalidInputException
    {
        Token token = getNextToken(pTokenIter);
        if(! token.mCode.equals(Token.Code.SEMICOLON))
        {
            throw new InvalidInputException("expected statement-ending semicolon; instead encountered token \"" + token + "\"");
        }
    }

    private String getQuotedString(ListIterator pTokenIter) throws InvalidInputException
    {
        Token token = getNextToken(pTokenIter);
        if(! token.mCode.equals(Token.Code.QUOTE))
        {
            throw new InvalidInputException("expected quote symbol");
        }

        token = getNextToken(pTokenIter);
        if(! token.mCode.equals(Token.Code.SYMBOL))
        {
            throw new InvalidInputException("expected quoted string");
        }

        String string = token.mSymbol;
        
        token = getNextToken(pTokenIter);

        assert (token.mCode.equals(Token.Code.QUOTE)) : "missing terminating quote";
        
        return(string);
    }

    private void handleStatementModel(ListIterator pTokenIter, 
                                      Model pModel,
                                      HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        Token token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.POUNDSIGN)) : "where expected a pound sign, got an unexpected token: " + token;

        token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.SYMBOL)) : "where expected a symbol token, got an unexpected token: " + token;
        assert (token.mSymbol.equals(STATEMENT_KEYWORD_MODEL)) : "where expected the model keyword, got an unexpected symbol token: " + token.mSymbol;

        if(null != mNamespace)
        {
            throw new InvalidInputException("it is illegal to define a model name inside a macro reference");
        }
        String modelName = obtainSymbol(pTokenIter, pSymbolMap);
        pModel.setName(modelName);
        getEndOfStatement(pTokenIter);
    }


   
    private void handleStatementMacroDefinition(ListIterator pTokenIter, 
                                                Model pModel, 
                                                HashMap pSymbolMap, 
                                                MutableInteger pNumReactions) throws InvalidInputException, DataNotFoundException
    {
        Token token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.POUNDSIGN)) : "where expected a pound sign, got an unexpected token: " + token;

        token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.SYMBOL)) : "where expected a symbol token, got an unexpected token: " + token;
        assert (token.mSymbol.equals(STATEMENT_KEYWORD_DEFINE)) : "where expected the define keyword, got an unexpected symbol token: " + token.mSymbol;

        String macroName = obtainSymbol(pTokenIter, pSymbolMap);
        if(null != pSymbolMap.get(macroName))
        {
            throw new InvalidInputException("symbol " + macroName + " was defined more than once in the same model");
        }

        if(-1 != macroName.indexOf(NAMESPACE_IDENTIFIER))
        {
            throw new InvalidInputException("macro name may not contain the namespace identifier \"" + NAMESPACE_IDENTIFIER + "\"; macro name is: " + macroName);
        }
        
        ArrayList externalSymbolsList = new ArrayList();

        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("expected parenthesis or curly brace after macro definition statement");
        }

        token = getNextToken(pTokenIter);
        
        if(token.mCode.equals(Token.Code.PAREN_BEGIN))
        {
            boolean expectSymbol = true;
            boolean gotEndParen = false;
            while(pTokenIter.hasNext())
            {
                if(expectSymbol)
                {
                    String symbol = obtainSymbol(pTokenIter, pSymbolMap);
                    externalSymbolsList.add(symbol);
                    expectSymbol = false;
                }
                else
                {
                    token = getNextToken(pTokenIter);
                    if(token.mCode.equals(Token.Code.PAREN_END))
                    {
                        gotEndParen = true;
                        break;
                    }
                    else if(token.mCode.equals(Token.Code.SEMICOLON))
                    {
                        throw new InvalidInputException("end of statement token encountered inside macro definition symbol list");
                    }
                    else if(token.mCode.equals(Token.Code.COMMA))
                    {
                        if(expectSymbol)
                        {
                            throw new InvalidInputException("comma encountered unexpectedly in macro definition symbol list");
                        }
                        expectSymbol = true;
                    }
                    else 
                    {
                        throw new InvalidInputException("unknown token encountered in macro definition symbol list");
                    }
                }
            }
            if(! gotEndParen)
            {
                throw new InvalidInputException("failed to find end parenthesis");
            }
        }
        else if(token.mCode.equals(Token.Code.BRACE_BEGIN))
        {
            pTokenIter.previous();
        }
        else
        {
            throw new InvalidInputException("unknown token found in macro definition statement");
        }

        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("expected curly brace token, instead found nothing");
        }

        token = getNextToken(pTokenIter);

        if(! token.mCode.equals(Token.Code.BRACE_BEGIN))
        {
            throw new InvalidInputException("expected curly brace token in macro definition statement");
        }

        Macro macro = new Macro(macroName);
        macro.mExternalSymbols = externalSymbolsList;
        
        LinkedList tokenList = new LinkedList();

        boolean gotEndBrace = false;
        Token prevToken = null;
        int braceCtr = 1;
        while(pTokenIter.hasNext())
        {
            prevToken = token;
            token = getNextToken(pTokenIter);
            if(token.mCode.equals(Token.Code.BRACE_END))
            {
                --braceCtr;
                if(0 == braceCtr)
                {
                    gotEndBrace = true;
                    break;
                }
            }
            else if(token.mCode.equals(Token.Code.BRACE_BEGIN))
            {
                ++braceCtr;
            }
            else if(null != prevToken && prevToken.mCode.equals(Token.Code.POUNDSIGN) && token.mCode.equals(Token.Code.SYMBOL) && token.mSymbol.equals(STATEMENT_KEYWORD_DEFINE))
            {
                    throw new InvalidInputException("it is illegal to embed a macro definition inside a macro definition");
            }
            tokenList.add(token);
        }
        if(! gotEndBrace)
        {
            throw new InvalidInputException("failed to find end brace");
        }
        macro.mTokenList = tokenList;
        pSymbolMap.put(macroName, macro);
    }

    private void handleStatementInclude(ListIterator pTokenIter, 
                                        Model pModel, 
                                        HashMap pSymbolMap, 
                                        MutableInteger pNumReactions,
                                        IncludeHandler pIncludeHandler) throws InvalidInputException
    {
        Token token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.POUNDSIGN)) : "where expected a pound sign, got an unexpected token: " + token;

        token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.SYMBOL)) : "where expected a symbol token, got an unexpected token: " + token;
        assert (token.mSymbol.equals(STATEMENT_KEYWORD_INCLUDE)) : "where expected the include keyword, got an unexpected symbol token: " + token.mSymbol;

        String fileName = getQuotedString(pTokenIter);

        getEndOfStatement(pTokenIter);

        BufferedReader bufferedReader = null;

        try
        {
            bufferedReader = pIncludeHandler.openReaderForIncludeFile(fileName);
            if(null != bufferedReader)
            {
                boolean insideInclude = true;
                parseModelDefinition(bufferedReader, pModel, pIncludeHandler, pSymbolMap, pNumReactions, insideInclude);
            }
        }
        catch(IOException e)
        {
            throw new InvalidInputException("error reading include file \"" + fileName + "\"", e);
        }
        catch(InvalidInputException e)
        {
            StringBuffer sb = new StringBuffer(e.getMessage());
            sb.append(" in file \"" + fileName + "\"; included");
            throw new InvalidInputException(sb.toString(), e);
        }

    }

    private String getErrorMessage(Exception e, int pLineCtr)
    {
        StringBuffer message = new StringBuffer(e.getMessage());
        message.append(" at line " + pLineCtr);    
        return(message.toString());
    }

    private void synchIterators(ListIterator master, ListIterator slave)
    {
        while(slave.nextIndex() < master.nextIndex())
        {
            slave.next();
        }
    }

    private void executeStatementBlock(List pTokens, 
                                       Model pModel, 
                                       IncludeHandler pIncludeHandler, 
                                       HashMap pSymbolMap, 
                                       MutableInteger pNumReactions) throws InvalidInputException
    {
        ListIterator tokenIter = pTokens.listIterator();
        ListIterator tokenIterExec = pTokens.listIterator();

        Token prevToken = null;


        Token token = null;
        int lineCtr = 0;

        try
        {

            while(tokenIter.hasNext())
            {
                token = (Token) tokenIter.next();
                assert (null != token) : "unexpected null token";
                lineCtr = token.mLine;

                if(token.mCode.equals(Token.Code.EQUALS))
                {
                    // if an "=" token is detected, this is definitely a symbol definition statement
                    handleStatementSymbolDefinition(tokenIterExec, pModel, pSymbolMap);
                    synchIterators(tokenIterExec, tokenIter);
                }
                else if(token.mCode.equals(Token.Code.ATSIGN))
                {
                    // if an "@" token is detected, this is definitely a compartment association statement
                    handleStatementAssociate(tokenIterExec, pModel, pSymbolMap);
                    synchIterators(tokenIterExec, tokenIter);
                }
                else if(token.mCode.equals(Token.Code.GREATER_THAN))
                {
                    // if a ">" token immediately follows a "-" token, this is definitely a reaction statement
                    if(null != prevToken)
                    {
                        if(prevToken.mCode.equals(Token.Code.HYPHEN))
                        {
                            handleStatementReaction(tokenIterExec, pModel, pSymbolMap, pNumReactions);
                            synchIterators(tokenIterExec, tokenIter);
                        }
                        else
                        {
                            throw new InvalidInputException("encountered \">\" unexpectedly");
                        }
                    }
                    else
                    {
                        throw new InvalidInputException("encountered \">\" with no preceding hyphen and outside of an expression context"); 
                    }
                }
                else if(token.mCode.equals(Token.Code.SYMBOL) && null != prevToken && prevToken.mCode.equals(Token.Code.POUNDSIGN))
                {
                    assert (null != token.mSymbol) : "null symbol string found in symbol token";
                    if(token.mSymbol.equals(STATEMENT_KEYWORD_INCLUDE))
                    {
                        handleStatementInclude(tokenIterExec, pModel, pSymbolMap, pNumReactions, pIncludeHandler);
                        synchIterators(tokenIterExec, tokenIter);
                    }
                    else if(token.mSymbol.equals(STATEMENT_KEYWORD_MODEL))
                    {
                        handleStatementModel(tokenIterExec, pModel, pSymbolMap);
                        synchIterators(tokenIterExec, tokenIter);
                    }
                    else if(token.mSymbol.equals(STATEMENT_KEYWORD_DEFINE))
                    {
                        handleStatementMacroDefinition(tokenIterExec, pModel, pSymbolMap, pNumReactions);
                        synchIterators(tokenIterExec, tokenIter);
                    }
                    else if(token.mSymbol.equals(STATEMENT_KEYWORD_REF))
                    {
                        handleStatementMacroReference(tokenIterExec, pModel, pIncludeHandler, pSymbolMap, pNumReactions);
                        synchIterators(tokenIterExec, tokenIter);
                    }
                    else
                    {
                        throw new InvalidInputException("unknown command keyword: " + token.mSymbol);
                    }
                }
                else if(token.mCode.equals(Token.Code.PAREN_BEGIN))
                {
                    if(null != prevToken)
                    {
                        if(prevToken.mCode.equals(Token.Code.SYMBOL))
                        {
                            if(prevToken.mSymbol.equals(KEYWORD_LOOP))
                            {
                                handleStatementLoop(tokenIterExec, pModel, pIncludeHandler, pSymbolMap, pNumReactions);
                                synchIterators(tokenIterExec, tokenIter);
                            }
                            else
                            {
                                throw new InvalidInputException("parenthesis following unknown keyword: " + prevToken.mSymbol);
                            }
                        }
                        else
                        {
                            throw new InvalidInputException("parenthesis following unknown token: " + prevToken);
                        }
                    }
                    else
                    {
                        throw new InvalidInputException("statement began with a parenthesis");
                    }
                }
                else if(token.mCode.equals(Token.Code.SEMICOLON))
                {
                    throw new InvalidInputException("unknown statement type");
                }
                prevToken = token;
            }
        }
        catch(DataNotFoundException e)
        {
            throw new InvalidInputException(getErrorMessage(e, lineCtr), e);
        }
        catch(InvalidInputException e)
        {
            throw new InvalidInputException(getErrorMessage(e, lineCtr), e);
        }
    }

   private void handleStatementLoop(ListIterator pTokenIter, 
                                     Model pModel,
                                     IncludeHandler pIncludeHandler,
                                     HashMap pSymbolMap,
                                     MutableInteger pNumReactions) throws InvalidInputException, DataNotFoundException
    {
        Token token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.SYMBOL)) : "expected a symbol token, unexpectedly got token: " + token;
        assert (token.mSymbol.equals(KEYWORD_LOOP)) : "expected loop keyword; unexpectedly got symbol: " + token.mSymbol;

        token = getNextToken(pTokenIter);
        if(! token.mCode.equals(Token.Code.PAREN_BEGIN))
        {
            throw new InvalidInputException("invalid token found when expected beginning parenthesis; token is: " + token);
        }

        token = getNextToken(pTokenIter);
        if(! token.mCode.equals(Token.Code.SYMBOL))
        {
            throw new InvalidInputException("invalid token found when expected loop index symbol; token is: " + token);
        }

        String loopIndexSymbolName = token.mSymbol;

        if(SymbolEvaluatorChemSimulation.isReservedSymbol(loopIndexSymbolName))
        {
            throw new InvalidInputException("cannot use a reserved symbol as a loop index: " + loopIndexSymbolName);
        }

        loopIndexSymbolName = addNamespaceToSymbol(loopIndexSymbolName, mNamespace);

        token =  getNextToken(pTokenIter);

        if(! token.mCode.equals(Token.Code.COMMA))
        {
            throw new InvalidInputException("invalid token found when expected comma separator");
        }

        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("missing loop starting value");
        }

        StringBuffer sb = new StringBuffer();
        while(pTokenIter.hasNext())
        {
            token = getNextToken(pTokenIter);
            if(token.mCode.equals(Token.Code.COMMA))
            {
                break; 
            }

            sb.append(token.toString());
        }
        
        String startExpressionString = sb.toString();
        Expression startExpression = new Expression(startExpressionString);

        int startValue = (int) (startExpression.computeValue(pSymbolMap));
        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("missing loop ending value");
        }

        sb.delete(0, sb.toString().length());
        Token prevToken = null;
        while(pTokenIter.hasNext())
        {
            token = getNextToken(pTokenIter);
            if(token.mCode.equals(Token.Code.BRACE_BEGIN))
            {
                if(null != prevToken)
                {
                    if(prevToken.mCode.equals(Token.Code.PAREN_END))
                    {
                        sb.deleteCharAt(sb.toString().length() - 1);
                        break;
                    }
                    else
                    {
                        throw new InvalidInputException("found begin-brace token without preceding end-paren token");
                    }
                }
                else
                {
                    throw new InvalidInputException("did not find end-paren token in loop statement");
                }
            }

            prevToken = token;
            sb.append(token.toString());
        }      

        String endExpressionString = sb.toString();
        Expression endExpression = new Expression(endExpressionString);
        int endValue = (int) (endExpression.computeValue(pSymbolMap));

        SymbolValue loopIndexSymbolValue = (SymbolValue) pSymbolMap.get(loopIndexSymbolName);
        LoopIndex loopIndexObj = null;
        if(null != loopIndexSymbolValue)
        {
            if(loopIndexSymbolValue instanceof LoopIndex)
            {
                loopIndexObj = (LoopIndex) loopIndexSymbolValue;
            }
            else
            {
                throw new InvalidInputException("loop index \"" + loopIndexSymbolName + "\" has already been used as a symbol elsewhere");
            }
        }
        else
        {
            loopIndexSymbolValue = new LoopIndex(loopIndexSymbolName, startValue);
            pSymbolMap.put(loopIndexSymbolName, loopIndexSymbolValue);
            loopIndexObj = (LoopIndex) loopIndexSymbolValue;
        }

        List subTokenList = new LinkedList();
        int braceCtr = 1;
        while(pTokenIter.hasNext())
        {
            token = getNextToken(pTokenIter);
            if(token.mCode.equals(Token.Code.BRACE_BEGIN))
            {
                braceCtr++;
            }
            else if(token.mCode.equals(Token.Code.BRACE_END))
            {
                braceCtr--;
            }

            if(braceCtr > 0)
            {
                subTokenList.add(token); 
            }
            if(braceCtr == 0)
            {
                break;
            }
        }

        if(braceCtr > 0)
        {
            throw new InvalidInputException("end-of-file encountered without matching end brace");
        }

        for(int loopIndex = startValue; loopIndex <= endValue; ++loopIndex)
        {
            loopIndexObj.setValue(loopIndex);
            try
            {
                executeStatementBlock(subTokenList, pModel, pIncludeHandler, pSymbolMap, pNumReactions);
            }
            catch(InvalidInputException e)
            {
                StringBuffer messageBuf = new StringBuffer(e.getMessage());
                messageBuf.append(" in loop block beginning");
                throw new InvalidInputException(messageBuf.toString(), e);
            }
        }

        // nuke the loop index object
        pSymbolMap.remove(loopIndexSymbolName);
    }

    private void handleStatementMacroReference(ListIterator pTokenIter, 
                                               Model pModel, 
                                               IncludeHandler pIncludeHandler,
                                               HashMap pSymbolMap, 
                                               MutableInteger pNumReactions) throws InvalidInputException, DataNotFoundException
    { 
        Token token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.POUNDSIGN)) : "where expected a pound sign, got an unexpected token: " + token;

        token = getNextToken(pTokenIter);
        assert (token.mCode.equals(Token.Code.SYMBOL)) : "where expected a symbol token, got an unexpected token: " + token;
        assert (token.mSymbol.equals(STATEMENT_KEYWORD_REF)) : "where expected the ref keyword, got an unexpected symbol token: " + token.mSymbol;

        String macroName = obtainSymbol(pTokenIter, pSymbolMap);

        SymbolValue symbolValue = (SymbolValue) pSymbolMap.get(macroName);
        if(null == symbolValue)
        {
            throw new InvalidInputException("unknown macro referenced: " + macroName);
        }

        if(! (symbolValue instanceof Macro))
        {
            throw new InvalidInputException("symbol referenced is not a macro: " + macroName);
        }

        Macro macro = (Macro) symbolValue;

        String macroInstanceName = obtainSymbol(pTokenIter, pSymbolMap);
        
        ArrayList externalSymbolsList = new ArrayList();

        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("expected parenthesis or curly brace after macro definition statement");
        }

        token = getNextToken(pTokenIter);

        if(token.mCode.equals(Token.Code.PAREN_BEGIN))
        {

            boolean expectSymbol = true;
            boolean gotEndParen = false;

            LinkedList tokenList = new LinkedList();
            while(pTokenIter.hasNext())
            {
                if(expectSymbol)
                {
                    
                    // grab all symbols until the comma or paren end or semicolon
                    tokenList.clear();
                    grabTokensToNextCommaOrParen(pTokenIter, tokenList, pSymbolMap);
                    if(tokenList.size() > 1 )
                    {
                        // cannot be a single symbol; must be an expression
                        Value valueObj = obtainValue(tokenList.listIterator(), pSymbolMap);
                        externalSymbolsList.add(valueObj);
                    }
                    else
                    {
                        Token firstToken = (Token) tokenList.get(0);
                        if(firstToken.mCode.equals(Token.Code.SYMBOL))
                        {
                            Object symbolObj = null;
                            String symbolName = firstToken.mSymbol;
                            // try to parse as a number
                            double value = 0.0;
                            try
                            {
                                value = Double.parseDouble(symbolName);
                                Value symbolValueObj = new Value(value);
                                symbolObj = symbolValueObj;
                            }
                            catch(NumberFormatException nfe)
                            {
                                // must be a symbol name, which is the normal case
                                symbolObj = symbolName;
                            }
                            externalSymbolsList.add(symbolObj);
                        }
                        else
                        {
                            throw new InvalidInputException("unknown token in list of symbols: " + firstToken);
                        }
                    }

//                    String symbol = obtainSymbol(pTokenIter, pSymbolMap);
//                    externalSymbolsList.add(symbol);
                    expectSymbol = false;
                }
                else
                {
                    token = getNextToken(pTokenIter);
                    if(token.mCode.equals(Token.Code.PAREN_END))
                    {
                        gotEndParen = true;
                        break;
                    }
                    else if(token.mCode.equals(Token.Code.SEMICOLON))
                    {
                        throw new InvalidInputException("end of statement token encountered inside parentheses");
                    }
                    else if(token.mCode.equals(Token.Code.COMMA))
                    {
                        if(expectSymbol)
                        {
                            throw new InvalidInputException("comma encountered unexpectedly");
                        }
                        expectSymbol = true;
                    }
                    else 
                    {
                        throw new InvalidInputException("unknown symbol encountered " + token);
                    }
                }
            }
            if(! gotEndParen)
            {
                throw new InvalidInputException("failed to find end parenthesis");
            }
        }
        else
        {
            pTokenIter.previous();
        }

        getEndOfStatement(pTokenIter);

        if(externalSymbolsList.size() != macro.mExternalSymbols.size())
        {
            throw new InvalidInputException("number of symbols is mismatched, for macro reference: " + macroName);
        }

        String oldNamespace = mNamespace;
        mNamespace = addNamespaceToSymbol(macroInstanceName, mNamespace);

        // add the dummy symbols to the global symbols map
        int numExtSym = externalSymbolsList.size();
        for(int i = 0; i < numExtSym; ++i)
        {
            String dummySymbolName = (String) macro.mExternalSymbols.get(i);
            assert (null != dummySymbolName) : "unexpected null array element";

            // add namespace to the dummy symbol
            dummySymbolName = addNamespaceToSymbol(dummySymbolName, mNamespace);
            assert (null == pSymbolMap.get(dummySymbolName)) : "unexpectedly found dummy symbol in global symbol table: " + dummySymbolName;
            
            Object extSymValueObj = externalSymbolsList.get(i);
            assert (null != extSymValueObj) : "unexpected null array element";

//            String extSymValue = (String) externalSymbolsList.get(i);
//            assert (null != extSymValue) : "unexpected null array element";

            
//            if(null == pSymbolMap.get(extSymValue))
//            {
//                throw new InvalidInputException("unknown symbol referenced: " + extSymValue);
//            }

            DummySymbol dummySymbol = new DummySymbol(dummySymbolName, extSymValueObj);
            pSymbolMap.put(dummySymbolName, dummySymbol);
        }


        LinkedList tokenList = macro.mTokenList;

        LinkedList updatedTokenList = new LinkedList();

        Iterator iter = tokenList.iterator();

        try
        {
            executeStatementBlock(macro.mTokenList,
                                  pModel,
                                  pIncludeHandler,
                                  pSymbolMap,
                                  pNumReactions);
        }
        catch(InvalidInputException e)
        {
            StringBuffer messageBuffer = new StringBuffer(e.getMessage());
            messageBuffer.append(" in macro referenced");
            throw new InvalidInputException(messageBuffer.toString(), e);
        }

        // remove the dummy symbols from the global symbols map
        for(int i = 0; i < numExtSym; ++i)
        {
            String extSymDummy = (String) macro.mExternalSymbols.get(i);
            extSymDummy = addNamespaceToSymbol(extSymDummy, mNamespace);
            pSymbolMap.remove(extSymDummy);
        }

        mNamespace = oldNamespace;

    }

    private void tokenizeAndExecuteStatementBuffer(StringBuffer pStatementBuffer, 
                                                   List pTokenList,
                                                   Model pModel,
                                                   IncludeHandler pIncludeHandler,
                                                   HashMap pSymbolMap,
                                                   MutableInteger pNumReactions,
                                                   int pLineNumber) throws InvalidInputException
    {
        String statement = pStatementBuffer.toString();

        tokenizeStatement(statement, pTokenList, pLineNumber);
        
        pStatementBuffer.delete(0, statement.length());

        executeStatementBlock(pTokenList, pModel, pIncludeHandler, pSymbolMap, pNumReactions);

        pTokenList.clear();
    }

    private void parseModelDefinition(BufferedReader pInputReader, 
                                      Model pModel, 
                                      IncludeHandler pIncludeHandler,
                                      HashMap pSymbolMap, 
                                      MutableInteger pNumReactions,
                                      boolean pInsideInclude) throws IOException, InvalidInputException
    {
        StreamTokenizer streamTokenizer = new StreamTokenizer(pInputReader);

        streamTokenizer.slashSlashComments(true);
        streamTokenizer.slashStarComments(true);
        streamTokenizer.lowerCaseMode(false);

        // our quote character is the "double-quote" mark, 
        streamTokenizer.quoteChar('\"');

        // we want to preserve newlines
        streamTokenizer.eolIsSignificant(true);

        // we want to preserve whitespace
        streamTokenizer.ordinaryChars(' ', ' ');
        streamTokenizer.ordinaryChars('\t', '\t');
        
        // disable parsing of numbers
        streamTokenizer.ordinaryChars('0', '9');
        streamTokenizer.ordinaryChars('.', '.');
        streamTokenizer.ordinaryChars('-', '-');

        // disable interpretation of a single slash character as a comment
        streamTokenizer.ordinaryChars('/', '/');

        // if we are inside an include file, the model elements have already been initialized
        if(! pInsideInclude)
        {
            processDefaultModelElements(pSymbolMap);
        }

        int lineCtr = 1;
        StringBuffer statementBuffer = new StringBuffer();
        List tokenList = new LinkedList();

        int braceLevel = 0;
        while(true)
        {
            boolean executeStatement = false;
            int tokenType = streamTokenizer.nextToken();
            if(StreamTokenizer.TT_EOF == tokenType)
            {
                break;
            }
            if(tokenType == '\"')
            {
                String quotedString = streamTokenizer.sval;
                statementBuffer.append("\"" + quotedString + "\"");
            }
            else if(tokenType == StreamTokenizer.TT_EOL)
            {
                statementBuffer.append("\n");
            }
            else if(tokenType == '{')
            {
                braceLevel++;
                statementBuffer.append("{");
            }
            else if(tokenType == '}')
            {
                if(0 == braceLevel )
                {
                    throw new InvalidInputException("mismatched braces, encountered \"}\" brace without matching \"{\" brace previously");
                }
                braceLevel--;
                statementBuffer.append("}");
                if(0 == braceLevel)
                {
                    executeStatement = true;
                }
            }
            else if(tokenType == ';')
            {
                statementBuffer.append(";");
                if(0 == braceLevel)
                {
                    executeStatement = true;
                }
            }
            else if(tokenType == StreamTokenizer.TT_WORD)
            {
                statementBuffer.append(streamTokenizer.sval);
            }
            else if(tokenType == '\t')
            {
                statementBuffer.append(" ");
            }
            else if(tokenType == StreamTokenizer.TT_NUMBER)
            {
                double value = streamTokenizer.nval;
                statementBuffer.append(value);
            }
            else
            {
                statementBuffer.append(Character.toString((char) tokenType));
            }

            if(executeStatement)
            {
                tokenizeAndExecuteStatementBuffer(statementBuffer,
                                                  tokenList,
                                                  pModel,
                                                  pIncludeHandler,
                                                  pSymbolMap,
                                                  pNumReactions,
                                                  lineCtr);                    
                lineCtr = streamTokenizer.lineno();
            }
        }

        if(statementBuffer.toString().trim().length() != 0)
        {
            throw new InvalidInputException("model definition file ended without a statement-ending token (semicolon); at line " + lineCtr + " of model definition file");
        }

        defineParameters(pSymbolMap, pModel);
    }



    public Model buildModel( BufferedReader pInputReader,
                             IncludeHandler pIncludeHandler ) throws InvalidInputException, IOException
    {
        assert (null != pIncludeHandler) : "null include handler";
        Model model = new Model();
        model.setName(DEFAULT_MODEL_NAME);
        HashMap symbolMap = new HashMap();
        MutableInteger numReactions = new MutableInteger(0);
        mNamespace = null;
        boolean insideInclude = false;
        parseModelDefinition(pInputReader, model, pIncludeHandler, symbolMap, numReactions, insideInclude);
        return(model);
    }

    public static boolean isValidSymbol(String pSymbolName)
    {
        return(VALID_SYMBOL_PATTERN.matcher(pSymbolName).matches());
    }

    public String getFileRegex()
    {
        return(".*\\.(dizzy|cmdl)$");
    }
}
