package org.systemsbiology.chem.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;

import org.systemsbiology.chem.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;

public class ModelBuilderCommandLanguage implements IModelBuilder, IAliasableClass
{
    public static final String CLASS_ALIAS = "command-language";
    private static final String DEFAULT_MODEL_NAME = "model";
    private static final String TOKEN_MULTILINE_COMMENT_END = "*/";
    private static final String TOKEN_MULTILINE_COMMENT_BEGIN = "/*";
    private static final String TOKEN_SIMPLE_COMMENT = "//";

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
        }
        
        Code mCode;
        String mSymbol;
        
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

    public ModelBuilderCommandLanguage()
    {
        initializeSearchPatternMath();
    }


    private void tokenizeStatement(String pStatement, List pTokens) throws InvalidInputException
    {
        StringTokenizer st = new StringTokenizer(pStatement, "=\", \t[]{}()->+;@#", true);
        String tokenString = null;
        boolean inQuote = false;
        StringBuffer symbolTokenBuffer = new StringBuffer();

        while(st.hasMoreElements())
        {
            tokenString = st.nextToken();

            Token token = null;

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
                        // do nothing
                    }
                    else if(tokenString.equals("\t"))
                    {
                        // do nothing
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

        if(! Symbol.isValidSymbol(pSymbolName))
        {
            throw new InvalidInputException("invalid symbol definition: " + pSymbolName);
        }
    }

    private static final String COMPARTMENT_NAME_DEFAULT = "univ";
    private void initializeModelElements(HashMap pSymbolMap) 
    {
        Compartment compartment = new Compartment(COMPARTMENT_NAME_DEFAULT);
        pSymbolMap.put(COMPARTMENT_NAME_DEFAULT, compartment);
    }

    private String translateMathExpressionsInString(String pInputString, 
                                                    HashMap pSymbolMap) throws DataNotFoundException, IllegalArgumentException
    {
        Pattern searchPatternMath = getSearchPatternMath();
        Matcher matcher = searchPatternMath.matcher(pInputString);
        while(matcher.find())
        {
            String matchedSubsequence = matcher.group(1);
            Expression exp = new Expression(matchedSubsequence);
            double value = exp.computeValue(pSymbolMap);
            String formattedExp = Integer.toString((int) value);
            pInputString = matcher.replaceFirst(formattedExp);
            matcher = searchPatternMath.matcher(pInputString);
        }
        return(pInputString);
    }

    private String obtainSymbol(ListIterator pTokenIter, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        assert (pTokenIter.hasNext()) : "expected token";
        Token token = (Token) pTokenIter.next();
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
                throw new InvalidInputException("expected symbol or quoted string");
            }
        }

        String adjustedSymbol = null;
        if(symbolName.charAt(0) == '$')
        {
            adjustedSymbol = symbolName.substring(1);
        }
        else
        {
            adjustedSymbol = symbolName;
        }
        checkSymbolValidity(adjustedSymbol);

        return(symbolName);
    }

    private Value obtainValue(ListIterator pTokenIter, HashMap pSymbolMap) throws InvalidInputException
    {
        boolean firstToken = true;
        StringBuffer expressionBuffer = new StringBuffer();
        boolean deferredExpression = false;
        while(pTokenIter.hasNext())
        {
            Token token = (Token) pTokenIter.next();
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
                    expressionBuffer.append(token.toString());
                }
                firstToken = false;
            }
            else
            {
                if(! deferredExpression || ! token.mCode.equals(Token.Code.BRACKET_END))
                {
                    expressionBuffer.append(token);
                }
                else
                {
                    if(deferredExpression)
                    {
                        break;
                    }
                }
            }
        }

        String expressionString = expressionBuffer.toString();
        Expression expression = new Expression(expressionString);
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
                throw new InvalidInputException("unable to determine value for expression: " + expressionString);
            }
        }

        return(value);
    }

    private void handleStatementAssociate(ListIterator pTokenIter, Model pModelHashMap, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        assert (pTokenIter.hasPrevious()) : "previous list element not found";

        while(pTokenIter.hasPrevious())
        {
            Token token = (Token) pTokenIter.previous();
            if(token.mCode.equals(Token.Code.SEMICOLON) ||
               token.mCode.equals(Token.Code.BRACE_END))
            {
                break;
            }
        }

        String symbolName = obtainSymbol(pTokenIter, pSymbolMap);
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

        String associatedSymbolName = obtainSymbol(pTokenIter, pSymbolMap);
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

    private void handleStatementDefine(ListIterator pTokenIter, Model pModelHashMap, HashMap pSymbolMap) throws InvalidInputException, DataNotFoundException
    {
        assert (pTokenIter.hasPrevious()) : "previous list element not found";

        while(pTokenIter.hasPrevious())
        {
            Token token = (Token) pTokenIter.previous();
            if(token.mCode.equals(Token.Code.SEMICOLON) ||
               token.mCode.equals(Token.Code.BRACE_END))
            {
                break;
            }
        }

        String symbolName = obtainSymbol(pTokenIter, pSymbolMap);

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
            throw new InvalidInputException("symbol multiply defined: " + symbolName);
        }
        pSymbolMap.put(symbolName, symbolValue);

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
            // get symbol
            String speciesName = obtainSymbol(pTokenIter, pSymbolMap);
            boolean dynamic = true;
            if(speciesName.charAt(0) == '$')
            {
                dynamic = false;
                speciesName = speciesName.substring(1);
            }
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

            Token token = getNextToken(pTokenIter);
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
        // back up to beginning of statement
        
        Token token = null;

        boolean hasName = false;
        boolean hasReactants = false;

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
            else if(token.mCode.equals(Token.Code.SEMICOLON))
            {
                pTokenIter.next();
                break;
            }
        }

        String reactionName = null;

        if(hasName)
        {
            reactionName = obtainSymbol(pTokenIter, pSymbolMap);
            token = (Token) pTokenIter.next();
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
                reaction.setNumSteps(numSteps);
            }
            else
            {
                // number of steps is exactly one; so there is nothing to do
            }
        }
        else
        {
            pTokenIter.previous();
        }
        
        getEndOfStatement(pTokenIter);
    }

    private Token getNextToken(ListIterator pTokenIter) throws InvalidInputException
    {
        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("expected a token, but no token was found");
        }

        Token token = (Token) pTokenIter.next();
        return(token);
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

    private void handleStatementInclude(ListIterator pTokenIter, 
                                        Model pModel, 
                                        HashMap pSymbolMap, 
                                        MutableInteger pNumReactions,
                                        IncludeHandler pIncludeHandler) throws InvalidInputException
    {

        String fileName = getQuotedString(pTokenIter);

        getEndOfStatement(pTokenIter);

        BufferedReader bufferedReader = null;

        try
        {
            bufferedReader = pIncludeHandler.openReaderForIncludeFile(fileName);
            if(null != bufferedReader)
            {
                parseModelDefinition(bufferedReader, pModel, pIncludeHandler, pSymbolMap, pNumReactions);
            }
        }
        catch(IOException e)
        {
            throw new InvalidInputException("error reading include file \"" + fileName + "\"", e);
        }
        catch(InvalidInputException e)
        {
            StringBuffer sb = new StringBuffer(e.toString());
            sb.append(" \"" + fileName + "\"; included");
            throw new InvalidInputException(sb.toString(), e);
        }

    }

    private void executeStatementBlock(List pTokens, 
                                       Model pModel, 
                                       IncludeHandler pIncludeHandler, 
                                       HashMap pSymbolMap, 
                                       MutableInteger pNumReactions) throws InvalidInputException, DataNotFoundException
    {
        ListIterator tokenIter = pTokens.listIterator();
        Token prevToken = null;
        Token token = null;
        while(tokenIter.hasNext())
        {
            token = (Token) tokenIter.next();
            if(token.mCode.equals(Token.Code.EQUALS))
            {
                handleStatementDefine(tokenIter, pModel, pSymbolMap);
            }
            else if(token.mCode.equals(Token.Code.ATSIGN))
            {
                handleStatementAssociate(tokenIter, pModel, pSymbolMap);
            }
            else if(token.mCode.equals(Token.Code.GREATER_THAN))
            {
                if(null != prevToken)
                {
                    if(prevToken.mCode.equals(Token.Code.HYPHEN))
                    {
                        handleStatementReaction(tokenIter, pModel, pSymbolMap, pNumReactions);
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
            else if(token.mCode.equals(Token.Code.SYMBOL) && token.mSymbol.equals("include"))
            {
                if(null != prevToken)
                {
                    if(prevToken.mCode.equals(Token.Code.POUNDSIGN))
                    {
                        handleStatementInclude(tokenIter, pModel, pSymbolMap, pNumReactions, pIncludeHandler);
                    }
                    else
                    {
                        // do nothing
                    }
                }
                else
                {
                    // do nothing
                }
            }
            else if(token.mCode.equals(Token.Code.PAREN_BEGIN))
            {
                if(null != prevToken)
                {
                    if(prevToken.mCode.equals(Token.Code.SYMBOL))
                    {
                        if(prevToken.mSymbol.equals("loop"))
                        {
                            handleStatementLoop(tokenIter, pModel, pIncludeHandler, pSymbolMap, pNumReactions);
                            // handle loop
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


    private void handleStatementLoop(ListIterator pTokenIter, 
                                     Model pModel,
                                     IncludeHandler pIncludeHandler,
                                     HashMap pSymbolMap,
                                     MutableInteger pNumReactions) throws InvalidInputException, DataNotFoundException
    {
        Token token = getNextToken(pTokenIter);
        if(! token.mCode.equals(Token.Code.SYMBOL))
        {
            throw new InvalidInputException("invalid token found when expected loop index symbol");
        }

        String loopIndexSymbolName = token.mSymbol;

        if(SymbolEvaluatorChemSimulation.isReservedSymbol(loopIndexSymbolName))
        {
            throw new InvalidInputException("cannot use a reserved symbol as a loop index: " + loopIndexSymbolName);
        }

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
            token = (Token) pTokenIter.next();
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
            token = (Token) pTokenIter.next();
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
            token = (Token) pTokenIter.next();
            if(token.mCode.equals(Token.Code.BRACE_BEGIN))
            {
                braceCtr++;
            }
            else if(token.mCode.equals(Token.Code.BRACE_END))
            {
                braceCtr--;
            }
            if(braceCtr < 0)
            {
                throw new InvalidInputException("brace encountered without matching begin brace");
            }
            if(braceCtr > 0 ||
               !(token.mCode.equals(Token.Code.BRACE_END)))
            {
                subTokenList.add(token); 
            }
        }

        for(int loopIndex = startValue; loopIndex <= endValue; ++loopIndex)
        {
            loopIndexObj.setValue(loopIndex);
            
            executeStatementBlock(subTokenList, pModel, pIncludeHandler, pSymbolMap, pNumReactions);
        }

        // nuke the loop index object
        pSymbolMap.remove(loopIndexSymbolName);
    }

    private void tokenizeAndExecuteStatementBuffer(StringBuffer pStatementBuffer, 
                                                   List pTokenList,
                                                   Model pModel,
                                                   IncludeHandler pIncludeHandler,
                                                   HashMap pSymbolMap,
                                                   MutableInteger pNumReactions) throws InvalidInputException, DataNotFoundException
    {
        String statement = pStatementBuffer.toString();
                                
        //===========================================
        // TOKENIZE THE STATEMENT:
        //===========================================
        tokenizeStatement(statement, pTokenList);
                                
        pStatementBuffer.delete(0, statement.length());

        executeStatementBlock(pTokenList, pModel, pIncludeHandler, pSymbolMap, pNumReactions);
        pTokenList.clear();
    }


    private void parseModelDefinition(BufferedReader pInputReader, 
                                      Model pModel, 
                                      IncludeHandler pIncludeHandler,
                                      HashMap pSymbolMap, 
                                      MutableInteger pNumReactions) throws IOException, InvalidInputException
    {
        String line = null;
        int lineCtr = 0;
        StringBuffer statementBuffer = new StringBuffer();
        boolean finishedStatement = false;
        boolean inMultilineComment = false;
        List tokenList = new LinkedList();
        initializeModelElements(pSymbolMap);

        int braceLevel = 0;

        while((line = pInputReader.readLine()) != null)
        {
            ++lineCtr;

            try
            {
                StringTokenizer st = new StringTokenizer(line, "\"/*;{}", true);
                
                boolean inQuote = false;
                
                String token = null;
                String prevToken = null;

                while(st.hasMoreElements())
                {
                    token = st.nextToken();
                    
                    if(token.equals("\""))
                    {
                        if(! inMultilineComment)
                        {
                            if(! inQuote)
                            {
                                inQuote = true;
                            }
                            else
                            {
                                inQuote = false;
                            }
                            statementBuffer.append(token);
                        }
                    }
                    else if(token.equals("/"))
                    {
                        if(! inMultilineComment)
                        {
                            if(! inQuote)
                            {
                                if(null != prevToken)
                                {
                                    if(prevToken.equals(token))
                                    {
                                        // this is a simple comment; kill previous character in buffer and
                                        // break to end of line
                                        statementBuffer.deleteCharAt(statementBuffer.toString().length() - 1);
                                        break;  // quit parsing this line of input; move onto next line
                                    }
                                    else
                                    {
                                        statementBuffer.append(token);
                                    }
                                }
                                else
                                {
                                    statementBuffer.append(token);
                                }
                            }
                            else
                            {
                                statementBuffer.append(token);
                            }
                        }
                        else
                        {
                            if(null != prevToken)
                            {
                                if(prevToken.equals("*"))
                                {
                                    // end of multiline comment
                                    inMultilineComment = false;
                                }
                                else
                                {
                                    // do nothing
                                }
                            }
                            else
                            {
                                // do nothing
                            }
                        }
                    }
                    else if(token.equals("*"))
                    {
                        if(! inMultilineComment)
                        {
                            if(! inQuote)
                            {
                                if(null != prevToken)
                                {
                                    if(prevToken.equals("/"))
                                    {
                                        inMultilineComment = true;
                                        statementBuffer.deleteCharAt(statementBuffer.toString().length() - 1);
                                        // start of multiline comment
                                    }
                                    else
                                    {
                                        statementBuffer.append(token);
                                    }
                                }
                                else
                                {
                                    statementBuffer.append(token);
                                }
                            }
                            else
                            {
                                statementBuffer.append(token);
                            }
                        }
                        else
                        {
                            // we are in a multiline comment, in which case a "*" character is ignored
                        }
                    }
                    else if(token.equals(";"))
                    {
                        if(! inMultilineComment)
                        {
                            if(! inQuote)
                            {
                                statementBuffer.append(token);
                                
                                if(0 == braceLevel)
                                {
                                    tokenizeAndExecuteStatementBuffer(statementBuffer,
                                                                      tokenList,
                                                                      pModel,
                                                                      pIncludeHandler,
                                                                      pSymbolMap,
                                                                      pNumReactions);
                                }
                            }
                            else
                            {
                                statementBuffer.append(token);
                            }
                        }
                        else
                        {
                            // do nothing; ignore semicolons in multiline comments
                        }
                    }
                    else if(token.equals("{"))
                    {
                        if(! inMultilineComment)
                        {
                            if(! inQuote)
                            {
                                braceLevel++;
                            }
                            statementBuffer.append(token);
                        }
                        else
                        {
                            // do nothing; ignore semicolons in multiline comments
                        }
                    }
                    else if(token.equals("}"))
                    {
                        if(! inMultilineComment)
                        {
                            if(! inQuote)
                            {
                                if(0 == braceLevel)
                                {
                                    throw new InvalidInputException("encountered close brace \"}\" with no corresponding open brace \"{\"");
                                }
                                --braceLevel;
                            }
                            statementBuffer.append(token);
                            if(0 == braceLevel)
                            {
                                tokenizeAndExecuteStatementBuffer(statementBuffer,
                                                                  tokenList,
                                                                  pModel,
                                                                  pIncludeHandler,
                                                                  pSymbolMap,
                                                                  pNumReactions);                                
                            }
                        }
                        else
                        {
                            // do nothing; ignore semicolons in multiline comments
                        }
                    }
                    else
                    {
                        if(! inMultilineComment)
                        {
                            statementBuffer.append(token);
                        }
                    }

                    prevToken = token;
                       
                }

                if(inQuote)
                {
                    throw new InvalidInputException("end of line encountered in a quotation");
                }

                if(0 != statementBuffer.toString().length())
                {
                    statementBuffer.append(" ");
                }
            }

            catch(InvalidInputException e)
            {
                StringBuffer message = new StringBuffer(e.toString());
                message.append(" at line " + lineCtr + " of model definition file");
                throw new InvalidInputException(message.toString(), e);
            }

            catch(DataNotFoundException e)
            {
                StringBuffer message = new StringBuffer(e.toString());
                message.append(" at line " + lineCtr + " of model definition file");
                throw new InvalidInputException(message.toString(), e);
            }

        }

        if(statementBuffer.toString().length() != 0)
        {
            throw new InvalidInputException("model definition file ended without a statement-ending token (semicolon); at line " + lineCtr + " of model definition file");
        }

        defineParameters(pSymbolMap, pModel);
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

    public Model buildModel( BufferedReader pInputReader,
                             IncludeHandler pIncludeHandler ) throws InvalidInputException, IOException
    {
        assert (null != pIncludeHandler) : "null include handler";
        Model model = new Model(DEFAULT_MODEL_NAME);
        HashMap symbolMap = new HashMap();
        MutableInteger numReactions = new MutableInteger(0);
        parseModelDefinition(pInputReader, model, pIncludeHandler, symbolMap, numReactions);
        return(model);
    }

    public String getFileRegex()
    {
        return(".*\\.(dizzy|cmdl)$");
    }
}
