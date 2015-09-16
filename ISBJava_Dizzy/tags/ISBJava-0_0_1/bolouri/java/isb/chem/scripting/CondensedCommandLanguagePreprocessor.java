package isb.chem.scripting;

import java.util.*;
import java.io.*;
import isb.util.*;

/**
 * This class implements a preprocessor for the
 * command-language scripting interface to the
 * <code>isb.chem</code> package.  This preprocessor
 * accepts a condensed notation for defining chemical
 * species and reactions, with a Jarnac-like syntax,
 * and translates the condensed notation into the
 * corresponding command-language statements.  
 * A sample of the condensed notation for defining
 * a reaction is as follows,
 * <blockquote>
 * <pre>
 * r0, A + B -&gt; C, 1.0;
 * </pre>
 * </blockquote>
 * The above is automatically translated into:
 * <blockquote>
 * <pre>
 * species &quot;A&quot;: compartment &quot;univ&quot;;
 * species &quot;B&quot;: compartment &quot;univ&quot;;
 * species &quot;C&quot;: compartment &quot;univ&quot;;
 * reaction &quot;r0&quot;: reactants (&quot;A&quot; &quot;B&quot;), products (&quot;C&quot;), rate 1.0;
 * addReactionToModel &quot;myModel&quot;: reaction &quot;r0&quot;;
 * </pre>
 * </blockquote>
 * Initial species populations are defined using a simple
 * syntax as shown here:
 * <blockquote>
 * <pre>
 * A = 100;
 * </pre>
 * </blockquote>
 * The above statement is translated into:
 * <blockquote>
 * <pre>
 * addToSpeciesPopulations &quot;sp&quot;: species &quot;A&quot;, value 100;
 * </pre>
 * </blockquote>
 * All species are automatically placed in the default
 * {@link isb.chem.Compartment} &quot;univ&quot;, which has unit volume.
 * All reactions are automatically added to the default
 * {@link isb.chem.SpeciesPopulations} data structure
 * &quot;sp&quot;.  The output of this class is intended to
 * be passed into the {@link ScriptBuilder}
 * class, which (in turn) converts it into a {@link Script}.
 * This class implements the {@link ITranslator} interface.
 *
 * @see isb.chem.Compartment
 * @see isb.chem.SpeciesPopulations
 * @see ITranslator
 * @see Script
 * @see ScriptBuilder
 *
 * @author Stephen Ramsey
 */
public class CondensedCommandLanguagePreprocessor implements ITranslator
{
    /*========================================*
     * constants
     *========================================*/

    private static final String DELIMITERS = "=#->+ \t\",;{}[]:/*";

    // special string tokens that are meant to be interpreted by the front-end:
    private static final String KEYWORD_SIMULATE = "simulate";
    private static final String KEYWORD_PRINT = "print";
    private static final String KEYWORD_MODEL = "model";
    private static final String KEYWORD_MODEL_INSTANCE = "modelInstance";
    private static final String KEYWORD_SPECIES_POPULATIONS = "speciesPopulations";
    private static final String KEYWORD_EXPORT = "export";
    private static final char CHAR_BOUNDARY_SPECIES = '$';

    /*========================================*
     * inner classes
     *========================================*/
    static class Token
    {
        static class Code
        {
            private final String mName;
            private Code(String pName)
            {
                mName = pName;
            }

            public String toString()
            {
                return(mName);
            } 

            public static final Code EQUALS = new Code("=");
            public static final Code ENDSTATEMENT = new Code(";");
            public static final Code SYMBOL = new Code("symbol");
            public static final Code HYPHEN = new Code("-");
            public static final Code COMMA = new Code(",");
            public static final Code GREATERTHAN = new Code(">");
            public static final Code PLUS = new Code("+");
            public static final Code EXP_BEGIN = new Code("[");
            public static final Code EXP_END = new Code("]");
            public static final Code BLOCK_BEGIN = new Code("{");
            public static final Code BLOCK_END = new Code("}");
            public static final Code SPACE = new Code (" ");
            public static final Code COLON = new Code (":");
        }
        
        public Code mCode;
        public String mSymbolName;
        public int mLine;

        public Token(Code pCode, int pLine)
        {
            mCode = pCode;
            mLine = pLine;
            mSymbolName = null;
        }

        public String toString()
        {
            String retStr = null;
            if(mCode.equals(Code.SYMBOL))
            {
                retStr = mSymbolName;
            }
            else
            {
                retStr = mCode.toString();
            }
            return(retStr);
        }
        
    }

    static class StatementType
    {
        private final String mName;
        private StatementType(String pName)
        {
            mName = pName;
        }

        public String toString()
        {
            return(mName);
        }

        public static final StatementType NORMAL = new StatementType("normal");
        public static final StatementType REACTION = new StatementType("reaction");
        public static final StatementType DEFINE = new StatementType("define");
        public static final StatementType SPECIESPOP = new StatementType("speciespop");
        public static final StatementType SIMULATE = new StatementType("simulate");
        public static final StatementType PRINT = new StatementType("print");
        public static final StatementType EXPORT = new StatementType("export");
    }
    

    /*========================================*
     * member data
     *========================================*/
    private String mSpeciesPopulations;
    private String mCompartmentName;
    private String mModelName;
    private int mNumReactions;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    String getSpeciesPopulations()
    {
        return(mSpeciesPopulations);
    }

    void setSpeciesPopulations(String pSpeciesPopulations)
    {
        if(null == pSpeciesPopulations)
        {
            throw new IllegalArgumentException("invalid (null) species populations name");
        }
        mSpeciesPopulations = pSpeciesPopulations;
    }

    String getModelName()
    {
        return(mModelName);
    }

    void setModelName(String pModelName)
    {
        if(null == pModelName)
        {
            throw new IllegalArgumentException("invalid (null) model name");
        }
        mModelName = pModelName;
    } 

    String getCompartmentName()
    {
        return(mCompartmentName);
    }

    void setCompartmentName(String pCompartmentName)
    {
        if(null == pCompartmentName)
        {
            throw new IllegalArgumentException("invalid (null) compartment name");
        }
        mCompartmentName = pCompartmentName;
    }

    void setNumReactions(int pNumReactions)
    {
        mNumReactions = pNumReactions;
    }

    int getNumReactions()
    {
        return(mNumReactions);
    }
 
    /*========================================*
     * initialization methods
     *========================================*/

    /*========================================*
     * constructors
     *========================================*/
    public CondensedCommandLanguagePreprocessor(String pModelName,
                                             String pCompartmentName,
                                             String pSpeciesPopulationsName)
    {
        setCompartmentName(pCompartmentName);
        setSpeciesPopulations(pSpeciesPopulationsName);
        setModelName(pModelName);
        setNumReactions(0);
    }

    /*========================================*
     * private methods
     *========================================*/

    private void generatePreamble(StringBuffer pCommandLanguage)
    {
        pCommandLanguage.append(Statement.Type.MODEL.toString() + " \"" + getModelName() + 
                             "\": " + Element.Type.SPECIESMODE.toString() + " " +
                             Element.ModifierCode.MOLECULES.toString() + ";\n");
        pCommandLanguage.append(Statement.Type.COMPARTMENT.toString() + " \"" + getCompartmentName() + "\";\n");
        pCommandLanguage.append(Statement.Type.SPECIESPOPULATIONS.toString() + " \"" + getSpeciesPopulations() + "\";\n");
    }

    private void parseStatement(ListIterator pTokenIter, HashSet pSpeciesSet, StringBuffer pCommandLanguage) throws InvalidInputException
    {
        // determine whether this is a definition or a reaction, or something else
        StatementType statementType = StatementType.NORMAL;
        String lastSymbol = null;
        List statementList = new LinkedList();
        StringBuffer exp = null;
        StringBuffer sym = null;
        Token token = null;
        Token prevToken = null;
        Token prevNonSpaceToken = null;
        
        // grab all tokens in the statement, and determine the statement type
        while(pTokenIter.hasNext())
        {
            prevToken = token;
            if(null != token && ! token.mCode.equals(Token.Code.SPACE))
            {
                prevNonSpaceToken = token;
            }
            token = (Token) pTokenIter.next();
            Token.Code tokenCode = token.mCode;

            if(null == exp)
            {
                // we are not in an expression context
                if(tokenCode.equals(Token.Code.EXP_BEGIN))
                {
                    exp = new StringBuffer("[");
                }
                else if(tokenCode.equals(Token.Code.EXP_END))
                {
                    throw new InvalidInputException("encountered \"]\" token while not within an expression");
                }
                else if(tokenCode.equals(Token.Code.SYMBOL))
                {
                    String symbol = token.mSymbolName;
                    if(null != sym)
                    {
                        sym.append(symbol);
                    }
                    else
                    {
                        sym = new StringBuffer(symbol);
                    }
                }
                else if(tokenCode.equals(Token.Code.COLON))
                {
                    if(null != prevNonSpaceToken)
                    {
                        if(Token.Code.SYMBOL == prevNonSpaceToken.mCode)
                        {
                            String prevSymbol = prevNonSpaceToken.mSymbolName;
                            if(prevSymbol.equals(KEYWORD_PRINT))
                            {
                                statementType = StatementType.PRINT;
                                if(null != sym)
                                {
                                    Token symbolToken = new Token(Token.Code.SYMBOL, token.mLine);
                                    symbolToken.mSymbolName = KEYWORD_PRINT;
                                    statementList.add(symbolToken);
                                    sym = null;
                                }
                            }
                            else if(prevSymbol.equals(KEYWORD_SIMULATE))
                            {
                                statementType = StatementType.SIMULATE;
                                if(null != sym)
                                {
                                    Token symbolToken = new Token(Token.Code.SYMBOL, token.mLine);
                                    symbolToken.mSymbolName = KEYWORD_SIMULATE;
                                    statementList.add(symbolToken);
                                    sym = null;
                                }
                            }
                            else if(prevSymbol.equals(KEYWORD_EXPORT))
                            {
                                statementType = StatementType.EXPORT;
                                if(null != sym)
                                {
                                    Token symbolToken = new Token(Token.Code.SYMBOL, token.mLine);
                                    symbolToken.mSymbolName = KEYWORD_EXPORT;
                                    statementList.add(symbolToken);
                                    sym = null;
                                }                                
                            }
                        }
                    }
                    if(null != sym)
                    {
                        sym.append(":");
                    }
                }
                else
                {
                    // the token is not a colon, a symbol, and the start of a math expression;
                    // so it must be one of the operators "+", "=", "->", "{", "}", ";", ",", or a space
                    if(null != sym)
                    {
                        String symbol = sym.toString();
                        lastSymbol = symbol;
                        Token symbolToken = new Token(Token.Code.SYMBOL, token.mLine);
                        symbolToken.mSymbolName = symbol;
                        statementList.add(symbolToken);
                        sym = null;
                    }

                    statementList.add(token);
                    if(tokenCode.equals(Token.Code.EQUALS))
                    {
                        if(pSpeciesSet.contains(lastSymbol))
                        {
                            statementType = StatementType.SPECIESPOP;
                        }
                        else
                        {
                            statementType = StatementType.DEFINE;
                        }
                    }
                    else if(tokenCode.equals(Token.Code.GREATERTHAN))
                    {
                        if(null != prevToken && prevToken.mCode.equals(Token.Code.HYPHEN))
                        {
                            statementType = StatementType.REACTION;
                        }
                        else
                        {
                            throw new InvalidInputException("encountered \">\" token without preceding hyphen");
                        }
                    }
                    else if(tokenCode.equals(Token.Code.ENDSTATEMENT))
                    {
                        break;
                    }
                    else if(tokenCode.equals(Token.Code.BLOCK_BEGIN))
                    {
                        break;
                    }
                    else if(tokenCode.equals(Token.Code.BLOCK_END))
                    {
                        break;
                    }
                }
            }
            else
            {
                // we are in an expression context
                if(tokenCode.equals(Token.Code.EXP_END))
                {
                    exp.append("]");
                    String expStr = exp.toString();
                    if(null != sym)
                    {
                        sym.append(expStr);
                    }
                    else
                    {
                        sym = new StringBuffer(expStr);
                    }
                    exp = null;
                }
                else if(tokenCode.equals(Token.Code.EXP_BEGIN))
                {
                    throw new InvalidInputException("encountered \"[\" token within an expression context");
                }
                else
                {
                    exp.append(token);
                }
            }
        }

        ListIterator tokenIter = statementList.listIterator();

        if(statementType.equals(StatementType.NORMAL))
        {
            parseStatementNormal(tokenIter, pCommandLanguage);
        }
        else if(statementType.equals(StatementType.REACTION))
        {
            parseStatementReaction(tokenIter, pSpeciesSet, pCommandLanguage);
        }
        else if(statementType.equals(StatementType.DEFINE))
        {
            parseStatementDefine(tokenIter, pCommandLanguage);
        }
        else if(statementType.equals(StatementType.SPECIESPOP))
        {
            parseStatementSpeciesPop(tokenIter, pCommandLanguage);
        }
        else if(statementType.equals(StatementType.SIMULATE))
        {
            parseStatementSimulate(tokenIter, pCommandLanguage);
        }
        else if(statementType.equals(StatementType.PRINT))
        {
            parseStatementPrint(tokenIter, pCommandLanguage);
        }
        else if(statementType.equals(StatementType.EXPORT))
        {
            parseStatementExport(tokenIter, pCommandLanguage);
        }
    }

    private void parseStatementNormal(ListIterator pTokenIter, StringBuffer pCommandLanguage) throws InvalidInputException
    {
        Token token = null;
        while(pTokenIter.hasNext())
        {
            token = (Token) pTokenIter.next();
            pCommandLanguage.append(token.toString());
        }
        if(null == token)
        {
            if(! token.mCode.equals(Token.Code.ENDSTATEMENT) &&
               ! token.mCode.equals(Token.Code.BLOCK_BEGIN) &&
               ! token.mCode.equals(Token.Code.BLOCK_END))
            {
                throw new InvalidInputException("statment did not end with a semicolon");
            }
        }
        pCommandLanguage.append("\n");
    }

    private void parseStatementPrint(ListIterator pTokenIter, StringBuffer pCommandLanguage) throws InvalidInputException
    {
        String itemToBePrinted = null;
        while(pTokenIter.hasNext())
        {
            Token token = (Token) pTokenIter.next();
            if(token.mCode.equals(Token.Code.SPACE))
            {
                continue;
            }
            else if(token.mCode.equals(Token.Code.SYMBOL))
            {
                if(token.mSymbolName.equals(KEYWORD_PRINT))
                {
                    continue;
                }
                if(null == itemToBePrinted)
                {
                    itemToBePrinted = token.mSymbolName;
                    break;
                }
                else
                {
                    throw new InvalidInputException("can only print one item at a time; you requested to print: " + itemToBePrinted + " and " + token.mSymbolName);
                }
            }
            else if(token.mCode.equals(Token.Code.ENDSTATEMENT))
            {
                break;
            }
            else
            {
                throw new InvalidInputException("unexpected token encountered in print statement: " + token);
            }
        }
        if(null == itemToBePrinted)
        {
            throw new InvalidInputException("you must specify the item you wish to print");
        }
        if(itemToBePrinted.equals(KEYWORD_MODEL))
        {
            pCommandLanguage.append(Statement.Type.PRINTMODEL.toString() + " \"" + getModelName() + "\"");
        }
        else if(itemToBePrinted.equals(KEYWORD_SPECIES_POPULATIONS))
        {
            pCommandLanguage.append(Statement.Type.PRINTSPECIESPOPULATIONS.toString() + " \"" + getSpeciesPopulations() + "\"");
        }
        else
        {
            throw new InvalidInputException("unknown keyword appearing after print: " + itemToBePrinted);
        }

        parseStatementNormal(pTokenIter, pCommandLanguage);
    }


    private void parseStatementExport(ListIterator pTokenIter, StringBuffer pCommandLanguage) throws InvalidInputException
    {
        String itemToBeExported = null;
        while(pTokenIter.hasNext())
        {
            Token token = (Token) pTokenIter.next();
            if(token.mCode.equals(Token.Code.SPACE))
            {
                continue;
            }
            else if(token.mCode.equals(Token.Code.SYMBOL))
            {
                if(token.mSymbolName.equals(KEYWORD_EXPORT))
                {
                    continue;
                }
                if(null == itemToBeExported)
                {
                    itemToBeExported = token.mSymbolName;
                    break;
                }
                else
                {
                    throw new InvalidInputException("can only export one item at a time; you requested to export: " + itemToBeExported + " and " + token.mSymbolName);
                }
            }
            else if(token.mCode.equals(Token.Code.ENDSTATEMENT))
            {
                break;
            }
            else
            {
                throw new InvalidInputException("unexpected token encountered in export statement: " + token);
            }
        }
        if(null == itemToBeExported)
        {
            throw new InvalidInputException("you must specify the item you wish to export");
        }
        if(itemToBeExported.equals(KEYWORD_MODEL_INSTANCE))
        {
            String newStatement = new String(Statement.Type.EXPORTMODELINSTANCE.toString() + " \"" + getModelName() + "\": " + Element.Type.SPECIESPOPULATIONS.toString() + " \"" + getSpeciesPopulations() + "\"");
            pCommandLanguage.append(newStatement);
        }
        else
        {
            throw new InvalidInputException("unknown export modifier: " + itemToBeExported);
        }

        parseStatementNormal(pTokenIter, pCommandLanguage);
    }

    private void parseStatementSimulate(ListIterator pTokenIter, StringBuffer pCommandLanguage) throws InvalidInputException
    {
        while(pTokenIter.hasNext())
        {
            Token token = (Token) pTokenIter.next();
            if(token.mCode.equals(Token.Code.SPACE))
            {
                continue;
            }
            else if(token.mCode.equals(Token.Code.SYMBOL))
            {
                if(token.mSymbolName.equals(KEYWORD_SIMULATE))
                {
                    break;
                }
            }
        }
        pCommandLanguage.append(Statement.Type.SIMULATE.toString() + " \"" + getModelName() + "\": " + 
                             Element.Type.SPECIESPOPULATIONS.toString() + " \"" + getSpeciesPopulations() + 
                             "\", ");
        parseStatementNormal(pTokenIter, pCommandLanguage);
    }

    private void parseStatementDefine(ListIterator pTokenIter, StringBuffer pCommandLanguage) throws InvalidInputException
    {
        pCommandLanguage.append(CommandLanguageParser.KEYWORD_DEFINE + " \"");

        String symbolName = null;

        while(pTokenIter.hasNext())
        {
            Token token = (Token) pTokenIter.next();
            Token.Code tokenCode = token.mCode;
            if(tokenCode.equals(Token.Code.SYMBOL))
            {
                if(null == symbolName)
                {
                    symbolName = token.mSymbolName;
                }
                else
                {
                    throw new InvalidInputException("found second symbol token in definition statement: " + token.mSymbolName);
                }
            }
            else if(tokenCode.equals(Token.Code.EQUALS))
            {
                break;
            }
            else if(tokenCode.equals(Token.Code.SPACE))
            {
                // do nothing
            }
            else 
            {
                throw new InvalidInputException("invalid token found: " + tokenCode);
            }
        }

        pCommandLanguage.append(symbolName + "\": " + Element.Type.VALUE.toString() + " ");

        parseStatementNormal(pTokenIter, pCommandLanguage);
    }
    
 
    private void parseStatementSpeciesPop(ListIterator pTokenIter, StringBuffer pCommandLanguage) throws InvalidInputException
    {
        pCommandLanguage.append(Statement.Type.ADDTOSPECIESPOPULATIONS.toString() + " \"" + 
                             getSpeciesPopulations() + "\": " + Element.Type.SPECIES.toString() + " \"");

        String symbolName = null;
        while(pTokenIter.hasNext())
        {
            Token token = (Token) pTokenIter.next();
            Token.Code tokenCode = token.mCode;
            if(tokenCode.equals(Token.Code.SYMBOL))
            {
                if(null == symbolName)
                {
                    symbolName = token.mSymbolName;
                }
                else
                {
                    throw new InvalidInputException("found second symbol token in definition statement: " + token.mSymbolName);
                }
            }
            else if(tokenCode.equals(Token.Code.EQUALS))
            {
                break;
            }
            else if(tokenCode.equals(Token.Code.SPACE))
            {
                // do nothing
            }
            else 
            {
                throw new InvalidInputException("invalid token found: " + tokenCode);
            }
        }

        pCommandLanguage.append(symbolName + "\", " + Element.Type.POPULATION.toString() + " ");

        parseStatementNormal(pTokenIter, pCommandLanguage);
    }

    private String createReactionName(Iterator pReactantsIter, Iterator pProductsIter)
    {
        int numReactions = getNumReactions();
        setNumReactions(numReactions + 1);
        Integer numReactionsObj = new Integer(numReactions);
        StringBuffer reactionName = new StringBuffer("r" + numReactionsObj.toString() + "___");
        
        while(pReactantsIter.hasNext())
        {
            String speciesName = (String) pReactantsIter.next();
            speciesName = speciesName.replace(':','_');
            reactionName.append(speciesName);
            if(pReactantsIter.hasNext())
            {
                reactionName.append(":");
            }
        }
        reactionName.append("___");
        while(pProductsIter.hasNext())
        {
            String speciesName = (String) pProductsIter.next();
            speciesName = speciesName.replace(':','_');
            reactionName.append(speciesName);
            if(pProductsIter.hasNext())
            {
                reactionName.append(":");
            }
        }        
        reactionName.append("___");
        reactionName.append(System.currentTimeMillis());
        return(reactionName.toString());
    }

    private void getReactionSpecies(ListIterator pTokenIter, 
                                    List pReactants, 
                                    List pProducts,
                                    StringBuffer pReactionName) throws InvalidInputException
    {
        boolean foundComma = true;
        Token token = null;
        Token prevToken = null;
        Token prevNonSpaceToken = null;
        boolean inProducts = false;
        Token saveToken = null;

        StringBuffer speciesName = null;
        String reactionName = null;

        assert (pReactionName.length() == 0) : "invalid string buffer length; should be empty";

        while(pTokenIter.hasNext())
        {
            prevToken = token;
            if(null != token && 
               ! token.mCode.equals(Token.Code.SPACE))
            {
                prevNonSpaceToken = token;
            }
            token = (Token) pTokenIter.next();
            Token.Code tokenCode = token.mCode;

            if(tokenCode.equals(Token.Code.SPACE))
            {
                continue;
            }

            if(null != prevNonSpaceToken && prevNonSpaceToken.mCode.equals(Token.Code.SPACE))
            {
                assert false;
            }

            if(tokenCode.equals(Token.Code.SYMBOL))
            {

                String symbolName = token.mSymbolName;
                if(null != prevToken &&
                   prevToken.mCode.equals(Token.Code.SPACE) &&
                   null != prevNonSpaceToken &&
                   prevNonSpaceToken.mCode.equals(Token.Code.SYMBOL))
                {
                    throw new InvalidInputException("cannot have two symbols separated by a space, in a reaction definition; this error is between the tokens: \"" + prevNonSpaceToken.mSymbolName + "\" and \"" + token.mSymbolName + "\"");
                }
                if(null != speciesName)
                {
                    speciesName.append(symbolName);
                }
                else
                {
                    speciesName = new StringBuffer(symbolName);
                }
            }
            else if(tokenCode.equals(Token.Code.PLUS))
            {
                // previous must be a symbol
                if(null != prevNonSpaceToken)
                {
                    if(prevNonSpaceToken.mCode.equals(Token.Code.SYMBOL))
                    {
                        assert (null != speciesName) : "null species name";
                        if(! inProducts)
                        {
                            pReactants.add(speciesName.toString());
                        }
                        else
                        {
                            pProducts.add(speciesName.toString());
                        }
                        speciesName = null;
                        // do nothing, everything is cool
                    }
                    else
                    {
                        throw new InvalidInputException("encountered \"+\" without previous species symbol");
                    }
                }
                else
                {
                    throw new InvalidInputException("encountered \"+\" without previous token");
                }
            }
            else if(tokenCode.equals(Token.Code.HYPHEN))
            {
                if(! inProducts)
                {
                    if(null != prevNonSpaceToken)
                    {
                        if(prevNonSpaceToken.mCode.equals(Token.Code.SYMBOL) ||
                           prevNonSpaceToken.mCode.equals(Token.Code.COMMA))
                        {
                            // do nothing, everything is cool
                        }
                        else
                        {
                            throw new InvalidInputException("encountered \"-\" with invalid previous symbol");
                        }
                    }
                    else
                    {
                        // do nothing, there are no reactants
                    }        
                }
                else
                {
                    throw new InvalidInputException("illegal token \"-\" found when parsing reaction products");
                }
            }
            else if(tokenCode.equals(Token.Code.GREATERTHAN))
            {
                if(! inProducts)
                {
                    if(prevNonSpaceToken.mCode.equals(Token.Code.HYPHEN))
                    {
                        if(null != speciesName)
                        {
                            pReactants.add(speciesName.toString());
                            speciesName = null;
                        }
                        inProducts = true;
                    }
                    else
                    {
                        throw new InvalidInputException("encountered token \">\" with no preceding hyphen");
                    }
                }
                else
                {
                    throw new InvalidInputException("illegal token \">\" found when parsing reaction products");
                }
            }
            else if(tokenCode.equals(Token.Code.COMMA))
            {
                if(inProducts)
                {
                    if(null != speciesName)
                    {
                        pProducts.add(speciesName.toString());
                    }
                    foundComma = true;
                    break;
                }
                else
                {
                    if(null != speciesName)
                    {
                        if(null == reactionName)
                        {
                            reactionName = speciesName.toString();
                        }
                        else
                        {
                            throw new InvalidInputException("reaction name defined twice: " + reactionName + " and " + speciesName);
                        }
                        speciesName = null;
                    }
                    else
                    {
                        throw new InvalidInputException("encountered separator \",\" with no preceding symbol; this should be preceded by the reaction name");
                    }
                }
            }
            else if(tokenCode.equals(Token.Code.ENDSTATEMENT))
            {
                throw new InvalidInputException("reaction statement missing comma separator \",\", which must appear (followed by the reaction rate)");
            }
        }

        if(! foundComma)
        {
            throw new InvalidInputException("reaction statement missing comma separator and trailing semicolon");
        }

        if(null == reactionName)
        {
            reactionName = createReactionName(pReactants.iterator(),
                                              pProducts.iterator());
        }

        pReactionName.append(reactionName);
    }



    private String getReactionRate(ListIterator pTokenIter) throws InvalidInputException
    {
        StringBuffer sb = new StringBuffer("");
        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("no reaction rate specified, and missing end statement");
        }

        boolean hasFirstToken = false;
        while(pTokenIter.hasNext())
        {
            Token token = (Token) pTokenIter.next();
            Token.Code tokenCode = token.mCode;
            if(tokenCode.equals(Token.Code.ENDSTATEMENT))
            {
                break;
            }
            else
            {
                sb.append(token.toString());
                hasFirstToken = true;
            }
        }
        if(! hasFirstToken)
        {
            throw new InvalidInputException("no reaction rate specified");
        }
        return(sb.toString());
    }

    private void handleSpeciesDefinitions(ListIterator pSpeciesIter, 
                                          HashSet pDefinedSpeciesSet,
                                          StringBuffer pCommandLanguage)
    {
        while(pSpeciesIter.hasNext())
        {
            String speciesName = (String) pSpeciesIter.next();
            boolean floating = true;
            if(speciesName.charAt(0) == CHAR_BOUNDARY_SPECIES)
            {
                floating = false;
                speciesName = speciesName.substring(1);
                pSpeciesIter.remove();
                pSpeciesIter.add(speciesName);
            }
            defineSpecies(speciesName, pDefinedSpeciesSet, pCommandLanguage, floating);
        }
    }

    private void parseStatementReaction(ListIterator pTokenIter, 
                                        HashSet pDefinedSpeciesSet,
                                        StringBuffer pCommandLanguage) throws InvalidInputException
    {
        List reactants = new LinkedList();
        List products = new LinkedList();
        StringBuffer reactionNameBuf = new StringBuffer();
        getReactionSpecies(pTokenIter, reactants, products, reactionNameBuf);
        String reactionName = reactionNameBuf.toString();
        ListIterator reactantsIter = reactants.listIterator();
        handleSpeciesDefinitions(reactantsIter, pDefinedSpeciesSet, pCommandLanguage);
        ListIterator productsIter = products.listIterator();
        handleSpeciesDefinitions(productsIter, pDefinedSpeciesSet, pCommandLanguage);
        pCommandLanguage.append(Statement.Type.REACTION.toString() + " \"");
        String reactionRate = getReactionRate(pTokenIter);
        pCommandLanguage.append(reactionName + "\": " + Element.Type.REACTANTS.toString() + " (");
        reactantsIter = reactants.listIterator();
        while(reactantsIter.hasNext())
        {
            String reactant = (String) reactantsIter.next();

            pCommandLanguage.append("\"" + reactant + "\"");
            if(reactantsIter.hasNext())
            {
                pCommandLanguage.append(" ");
            }
        }
        pCommandLanguage.append("), " + Element.Type.PRODUCTS.toString() + " (");
        productsIter = products.listIterator();
        while(productsIter.hasNext())
        {
            String product = (String) productsIter.next();
            pCommandLanguage.append("\"" + product + "\"");
            if(productsIter.hasNext())
            {
                pCommandLanguage.append(" ");
            }
        }
        pCommandLanguage.append("), " + Element.Type.RATE.toString() + " " + reactionRate);
        pCommandLanguage.append(";\n");
        pCommandLanguage.append(Statement.Type.ADDREACTIONTOMODEL.toString() + " \"" + getModelName() + 
                             "\": " + Element.Type.REACTION.toString() + " \"" + reactionName + "\";\n");
    }

    private void defineSpecies(String pSpeciesName, HashSet pDefinedSpeciesSet, StringBuffer pStatement, boolean pFloating)
    {
        pDefinedSpeciesSet.add(pSpeciesName);
        pStatement.append(Statement.Type.SPECIES.toString() + " \"" + pSpeciesName + 
                          "\": " + Element.Type.COMPARTMENT.toString() + " \"" + getCompartmentName() + "\"");
        if(! pFloating)
        {
            pStatement.append(", speciesType boundary");
        }
        pStatement.append(";\n");
    }

    private void tokenizeLine(String pLine, 
                              int pLineNumber, 
                              List pLineTokens, 
                              MutableBoolean pWithinMultilineComment) throws InvalidInputException
    {
        boolean includeDelimiters = true;
        StringTokenizer tokenizer = new StringTokenizer(pLine, DELIMITERS, includeDelimiters);
        StringBuffer quotedString = null;

        String lastToken = null;
        String tokenStr = null;

        while(tokenizer.hasMoreTokens())
        {
            lastToken = tokenStr;
            tokenStr = tokenizer.nextToken();

            String trimTokenStr = tokenStr.trim();

            if(pWithinMultilineComment.getValue())
            {
                if(tokenStr.equals("/") && null != lastToken && lastToken.equals("*"))
                {
                    pWithinMultilineComment.setValue(false);
                }
                continue;
            }

            if(quotedString != null)
            {
                if(tokenStr.equals("\""))
                {
                    // just ended a quotation
                    Token token = new Token(Token.Code.SYMBOL, pLineNumber);
                    token.mSymbolName = new String("\"" + quotedString.toString() + "\"");
                    pLineTokens.add(token);
                    quotedString = null;
                }
                else
                {
                    quotedString.append(tokenStr);
                }
            }
            else
            {
                if(tokenStr.equals("\""))
                {
                    quotedString = new StringBuffer("");
                }
                else if(tokenStr.equals("#"))
                {
                    break;
                }
                else if(tokenStr.equals("*") && null != lastToken && lastToken.equals("/"))
                {
                    pLineTokens.remove(pLineTokens.size() - 1);
                    pWithinMultilineComment.setValue(true);
                    continue;
                }
                else if(tokenStr.equals("="))
                {
                    pLineTokens.add(new Token(Token.Code.EQUALS, pLineNumber));
                }
                else if(tokenStr.equals(","))
                {
                    pLineTokens.add(new Token(Token.Code.COMMA, pLineNumber));
                }
                else if(tokenStr.equals("+"))
                {
                    pLineTokens.add(new Token(Token.Code.PLUS, pLineNumber));
                }
                else if(tokenStr.equals("-"))
                {
                    pLineTokens.add(new Token(Token.Code.HYPHEN, pLineNumber));
                }
                else if(tokenStr.equals(">"))
                {
                    pLineTokens.add(new Token(Token.Code.GREATERTHAN, pLineNumber));
                }
                else if(tokenStr.equals(";"))
                {
                    pLineTokens.add(new Token(Token.Code.ENDSTATEMENT, pLineNumber));
                }
                else if(tokenStr.equals("["))
                {
                    pLineTokens.add(new Token(Token.Code.EXP_BEGIN, pLineNumber));
                }
                else if(tokenStr.equals(":"))
                {
                    pLineTokens.add(new Token(Token.Code.COLON, pLineNumber));
                }
                else if(tokenStr.equals("]"))
                {
                    pLineTokens.add(new Token(Token.Code.EXP_END, pLineNumber));
                }
                else if(tokenStr.equals("{"))
                {
                    pLineTokens.add(new Token(Token.Code.BLOCK_BEGIN, pLineNumber));
                }
                else if(tokenStr.equals("}"))
                {
                    pLineTokens.add(new Token(Token.Code.BLOCK_END, pLineNumber));
                }
                else if(trimTokenStr.length() == 0)
                {
                    pLineTokens.add(new Token(Token.Code.SPACE, pLineNumber));
                }
                else
                {
                    Token token = new Token(Token.Code.SYMBOL, pLineNumber);
                    token.mSymbolName = tokenStr;
                    pLineTokens.add(token);
                }
            }

        }
    }


    /*========================================*
     * protected methods
     *========================================*/

    void convertToCommandLanguage(BufferedReader pBufferedReader, 
                                  PrintWriter pOutputCommandLanguage) throws InvalidInputException, IOException
    {
        int lineCounter = 0;
        List tokenList = new LinkedList();

        try
        {
            String line = null;
            MutableBoolean withinMultilineComment = new MutableBoolean(false);
            while((line = pBufferedReader.readLine()) != null)
            {
                ++lineCounter;
                tokenizeLine(line, lineCounter, tokenList, withinMultilineComment);
            }
            if(withinMultilineComment.getValue())
            {
                throw new InvalidInputException("command language input ended within a comment context; the close-comment symbol is probably missing");
            }
        }

        catch(InvalidInputException e)
        {
            throw new InvalidInputException("error tokenizing input stream; error message is: " + e.toString() + "; on line " + lineCounter, e);        
        }

        ListIterator tokenIter = tokenList.listIterator();

        HashSet speciesSet = new HashSet();
        StringBuffer commandLanguage = new StringBuffer();

        try
        {
            while(tokenIter.hasNext())
            {
                parseStatement(tokenIter, speciesSet, commandLanguage);
            }
        }

        catch(InvalidInputException e)
        {
            StringBuffer msgBuf = new StringBuffer("error parsing input");
            if(tokenIter.hasPrevious())
            {
                Token token = (Token) tokenIter.previous();
                msgBuf.append(" at line " + token.mLine);
            }
            msgBuf.append("; error message is: " + e.toString());
            throw new InvalidInputException(msgBuf.toString(), e);
        }

        StringReader commandLanguageStringReader = new StringReader(commandLanguage.toString());
        BufferedReader commandLanguageReader = new BufferedReader(commandLanguageStringReader);
        
        String line = null;
        while((line = commandLanguageReader.readLine()) != null)
        {
            pOutputCommandLanguage.println(line);
        }
        pOutputCommandLanguage.flush();
    }
    
    void readFromFile(String pFileName) throws IOException, InvalidInputException
    {
        FileReader fileReader = new FileReader(pFileName);
        BufferedReader bufRdr = new BufferedReader(fileReader);
        PrintWriter outputWriter = new PrintWriter(System.out);
        convertToCommandLanguage(bufRdr, outputWriter);
    }

    /*========================================*
     * public methods
     *========================================*/
    public void translate(BufferedReader pInput,
                          PrintWriter pOutput) throws InvalidInputException, IOException
    {
        convertToCommandLanguage(pInput, pOutput);
    }

    public void generatePreamble(PrintWriter pOutput) throws IOException
    {
        StringBuffer preamble = new StringBuffer();
        generatePreamble(preamble);
        StringReader preambleStringReader = new StringReader(preamble.toString());
        BufferedReader preambleBufferedReader = new BufferedReader(preambleStringReader);
        String line = null;
        while((line = preambleBufferedReader.readLine()) != null)
        {
            pOutput.println(line);
        }
    }

}
