package isb.chem.scripting;

import java.util.*;
import java.io.*;
import java.text.*;
import java.util.regex.*;
import isb.util.*;

/**
 * This class parses a text input stream containing 
 * command language statements, into
 * a {@link Script} representation of those statements.
 * It is used by the {@link ScriptBuilder}
 * in order to generate a {@link Script} from a file or
 * other text input stream.  This class has support for
 * embedding a {@link ITranslator} as a preprocessor, which
 * (if specified) will be used to preprocess any input data
 * streams, including those of included files.   Here is
 * an example of the kind of command-language input this
 * class permits:
 * <blockquote>
 * <pre>
 * compartment &quot;univ&quot;: volume 1.0;
 * speciesPopulations &quot;sp&quot;;
 * species &quot;TFA&quot;: compartment &quot;univ&quot;;
 * species &quot;DNA_plus_TFA&quot;: compartment &quot;univ&quot;;
 * species &quot;DNA&quot;: compartment &quot;univ&quot;;
 * addToSpeciesPopulations &quot;sp&quot;: species &quot;TFA&quot;, population 5;
 * addToSpeciesPopulations &quot;sp&quot;: species &quot;DNA_plus_TFA&quot;, population 0;
 * addToSpeciesPopulations &quot;sp&quot;: species &quot;DNA&quot;, population 1;
 * reaction &quot;tfa&quot;: reactants (&quot;DNA&quot; &quot;TFA&quot;), 
                             products (&quot;DNA_plus_TFA&quot;), rate 1.0;
 * reaction &quot;tfa_rev&quot;: reactants (&quot;DNA_plus_TFA&quot;), 
            products (&quot;DNA&quot; &quot;TFA&quot;), rate 0.1;
 * model &quot;myModel&quot;: speciesMode molecules, 
 *                  reactions (&quot;tfa&quot; &quot;tfa_rev&quot;); 
 * printModel &quot;myModel&quot;;
 * printSpeciesPopulations &quot;sp&quot;;
 * validateModel &quot;myModel&quot;: speciesPopulations &quot;sp&quot;;
 * simulate &quot;myModel&quot;: speciesPopulations &quot;sp&quot;, stopTime 10000.0, 
                                 numTimePoints 500, viewSpecies (&quot;TFA&quot;);
 * </pre>
 * </blockquote>
 *
 * @see ITranslator
 * @see Script
 * @see ScriptBuilder
 *
 * @author Stephen Ramsey
 */
public class CommandLanguageParser implements IScriptBuildingParser, IAliasableClass
{
    /*========================================*
     * constants
     *========================================*/
    public static final String CLASS_ALIAS = "command-language";
    private static final String DELIMITERS = ":, \t\"();#{}[]";
    static final String KEYWORD_INCLUDE = "include";
    static final String KEYWORD_LOOP = "loop";
    static final String KEYWORD_DEFINE = "define";
    private static final int LOOP_DEFAULT_STEP = 1;
    private static final HashSet REQUIRED_ELEMENTS_LOOP;
    private static final HashSet ALLOWED_ELEMENTS_LOOP;
    private static final HashSet REQUIRED_ELEMENTS_DEFINE;
    private static final HashSet ALLOWED_ELEMENTS_DEFINE;

    static
    {
        REQUIRED_ELEMENTS_DEFINE = new HashSet();
        REQUIRED_ELEMENTS_DEFINE.add(Element.Type.VALUE);

        ALLOWED_ELEMENTS_DEFINE = new HashSet();
        ALLOWED_ELEMENTS_DEFINE.add(Element.Type.VALUE);

        REQUIRED_ELEMENTS_LOOP = new HashSet();
        REQUIRED_ELEMENTS_LOOP.add(Element.Type.START);
        REQUIRED_ELEMENTS_LOOP.add(Element.Type.STOP);

        ALLOWED_ELEMENTS_LOOP = new HashSet();
        ALLOWED_ELEMENTS_LOOP.add(Element.Type.START);
        ALLOWED_ELEMENTS_LOOP.add(Element.Type.STOP);
        ALLOWED_ELEMENTS_LOOP.add(Element.Type.STEP);
    }

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
        
            public static final Code END_STATEMENT = new Code("endstatement");
            public static final Code SYMBOL = new Code("symbol");
            public static final Code NUMBER = new Code("value");
            public static final Code KEYWORD = new Code("keyword");
            public static final Code LIST_BEGIN = new Code("listbegin");
            public static final Code LIST_END = new Code("listend");
            public static final Code ELEMENT_DELIMITER = new Code("elementdelimiter");
            public static final Code FIRST_ELEMENT_DELIMITER = new Code("firstelementdelimiter");
            public static final Code BLOCK_BEGIN = new Code("blockbegin");
            public static final Code BLOCK_END = new Code("blockend");
            public static final Code EXP_BEGIN = new Code("expbegin");
            public static final Code EXP_END = new Code("expend");
        }

        public final Code mCode;
        public String mKeywordName;
        public String mSymbolName;
        public Double mNumberValue;
        public List mSymbolList;
        public int mLineNumber;

        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("token code: " + mCode);
            sb.append("; keyword: " + mKeywordName + "; symbol name: " + mSymbolName + "; number value: " + mNumberValue);
            return(sb.toString());
        }

        public Token(Code pCode, int pLineNumber)
        {
            mCode = pCode;
            mKeywordName = null;
            mSymbolName = null;
            mNumberValue = null;
            mSymbolList = null;
            mLineNumber = pLineNumber;
        }
    }

    /*========================================*
     * member data
     *========================================*/
    private HashMap mAllowedElements;
    private HashMap mRequiredElements;
    private HashMap mAllowedElementDataTypes;
    private HashMap mAllowedModifiers;
    private Pattern mSearchPatternMath;
    private NumberFormat mNumberFormatMath;
    private ITranslator mPreprocessor;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    public ITranslator getPreprocessor()
    {
        return(mPreprocessor);
    }

    public void setPreprocessor(ITranslator pPreprocessor)
    {
        mPreprocessor = pPreprocessor;
    }

    private HashMap getAllowedElements()
    {
        return(mAllowedElements);
    }

    private void setAllowedElements(HashMap pAllowedElements)
    {
        mAllowedElements = pAllowedElements;
    }

    private HashMap getRequiredElements()
    {
        return(mRequiredElements);
    }

    private void setRequiredElements(HashMap pRequiredElements)
    {
        mRequiredElements = pRequiredElements;
    }

    private HashMap getAllowedElementDataTypes()
    {
        return(mAllowedElementDataTypes);
    }

    private void setAllowedElementDataTypes(HashMap pAllowedElementDataTypes)
    {
        mAllowedElementDataTypes = pAllowedElementDataTypes;
    }

    private HashMap getAllowedModifiers()
    {
        return(mAllowedModifiers);
    }

    private void setAllowedModifiers(HashMap pAllowedModifiers)
    {
        mAllowedModifiers = pAllowedModifiers;
    }

    private Pattern getSearchPatternMath()
    {
        return(mSearchPatternMath);
    }

    private void setSearchPatternMath(Pattern pSearchPatternMath)
    {
        mSearchPatternMath = pSearchPatternMath;
    }

    private NumberFormat getNumberFormatMath()
    {
        return(mNumberFormatMath);
    }

    private void setNumberFormatMath(NumberFormat pNumberFormatMath)
    {
        mNumberFormatMath = pNumberFormatMath;
    }

    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeSearchPatternMath()
    {
        String searchRegex = "\\$\\[([^\\[\\]]+)\\]";
        Pattern searchPattern = Pattern.compile(searchRegex);
        setSearchPatternMath(searchPattern);
    }

    private void initializeNumberFormatMath()
    {
        NumberFormat numberFormatMath = NumberFormat.getInstance();
        numberFormatMath.setMinimumFractionDigits(0);
        numberFormatMath.setMaximumFractionDigits(12);
        numberFormatMath.setGroupingUsed(false);
        setNumberFormatMath(numberFormatMath);
    }

    /*========================================*
     * constructors
     *========================================*/
    public CommandLanguageParser()
    {
        setAllowedElements(new HashMap());
        setRequiredElements(new HashMap());
        setAllowedElementDataTypes(new HashMap());
        setAllowedModifiers(new HashMap());
        setPreprocessor(null);
        initializeSearchPatternMath();
        initializeNumberFormatMath();
        buildCommandLanguageParser();
    }

    /*========================================*
     * private methods
     *========================================*/
    private void buildCommandLanguageParser() throws IllegalStateException
    {
        HashSet elements = new HashSet();

        // defining allowed elements
        HashMap allowed = getAllowedElements();

        elements.add(Element.Type.VALUE);
        elements.add(Element.Type.REACTIONS);
        allowed.put(Statement.Type.PARAMETER, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIESTYPE);
        elements.add(Element.Type.COMPARTMENT);
        allowed.put(Statement.Type.SPECIES, elements.clone());
        elements.clear();

        elements.add(Element.Type.VOLUME);
        allowed.put(Statement.Type.COMPARTMENT, elements.clone());
        elements.clear();

        elements.add(Element.Type.RATE);
        elements.add(Element.Type.REACTANTS);
        elements.add(Element.Type.PRODUCTS);
        allowed.put(Statement.Type.REACTION, elements.clone());
        elements.clear();

        elements.add(Element.Type.PARAMETERS);
        elements.add(Element.Type.SUBMODELS);
        elements.add(Element.Type.SPECIESMODE);
        elements.add(Element.Type.REACTIONS);
        allowed.put(Statement.Type.MODEL, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIESLIST);
        elements.add(Element.Type.POPULATIONS);
        allowed.put(Statement.Type.SPECIESPOPULATIONS, elements.clone());
        elements.clear();

        elements.add(Element.Type.STARTTIME);
        elements.add(Element.Type.STOPTIME);
        elements.add(Element.Type.SPECIESPOPULATIONS);
        elements.add(Element.Type.NUMTIMEPOINTS);
        elements.add(Element.Type.VIEWSPECIES);
        elements.add(Element.Type.OUTPUT);
        elements.add(Element.Type.DEBUG);
        elements.add(Element.Type.ENSEMBLESIZE);
        elements.add(Element.Type.OUTPUTFILE);
        elements.add(Element.Type.STORENAME);
        elements.add(Element.Type.SIMULATOR);
        allowed.put(Statement.Type.SIMULATE, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIES);
        elements.add(Element.Type.POPULATION);
        allowed.put(Statement.Type.ADDTOSPECIESPOPULATIONS, elements.clone());
        elements.clear();

        allowed.put(Statement.Type.PRINTMODEL, elements.clone());
        elements.clear();

        allowed.put(Statement.Type.PRINTSPECIESPOPULATIONS, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIESPOPULATIONS);
        allowed.put(Statement.Type.VALIDATEMODEL, elements.clone());
        elements.clear();

        elements.add(Element.Type.REACTION);
        allowed.put(Statement.Type.ADDREACTIONTOMODEL, elements.clone());
        elements.clear();

        elements.add(Element.Type.PARAMETER);
        allowed.put(Statement.Type.ADDPARAMETERTOMODEL, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIESPOPULATIONS);
        elements.add(Element.Type.OUTPUTFILE);
        elements.add(Element.Type.EXPORTER);
        allowed.put(Statement.Type.EXPORTMODELINSTANCE, elements.clone());
        elements.clear();

        // defining required elements
        HashMap required = getRequiredElements();

        elements.add(Element.Type.VALUE);
        required.put(Statement.Type.PARAMETER, elements.clone());
        elements.clear();

        elements.add(Element.Type.COMPARTMENT);
        required.put(Statement.Type.SPECIES, elements.clone());
        elements.clear();
        
        required.put(Statement.Type.COMPARTMENT, new HashSet());

        elements.add(Element.Type.RATE);
        elements.add(Element.Type.REACTANTS);
        elements.add(Element.Type.PRODUCTS);
        required.put(Statement.Type.REACTION, elements.clone());
        elements.clear();

        required.put(Statement.Type.MODEL, new HashSet());
        elements.clear();

        required.put(Statement.Type.SPECIESPOPULATIONS, elements.clone());
        elements.clear();

        elements.add(Element.Type.STOPTIME);
        elements.add(Element.Type.SPECIESPOPULATIONS);
        elements.add(Element.Type.VIEWSPECIES);
        required.put(Statement.Type.SIMULATE, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIES);
        elements.add(Element.Type.POPULATION);
        required.put(Statement.Type.ADDTOSPECIESPOPULATIONS, elements.clone());
        elements.clear();

        required.put(Statement.Type.PRINTMODEL, elements.clone());
        elements.clear();

        required.put(Statement.Type.PRINTSPECIESPOPULATIONS, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIESPOPULATIONS);
        required.put(Statement.Type.VALIDATEMODEL, elements.clone());
        elements.clear();

        elements.add(Element.Type.REACTION);
        required.put(Statement.Type.ADDREACTIONTOMODEL, elements.clone());
        elements.clear();

        elements.add(Element.Type.PARAMETER);
        required.put(Statement.Type.ADDPARAMETERTOMODEL, elements.clone());
        elements.clear();

        elements.add(Element.Type.SPECIESPOPULATIONS);
        required.put(Statement.Type.EXPORTMODELINSTANCE, elements.clone());
        elements.clear();

        // defining allowed data types
        HashSet dataTypes = new HashSet();
        HashMap allowedDataTypes = getAllowedElementDataTypes();

        dataTypes.add(Element.DataType.MODIFIER);
        allowedDataTypes.put(Element.Type.SPECIESTYPE, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.COMPARTMENT, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        dataTypes.add(Element.DataType.DOUBLE);
        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.RATE, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.MODIFIER);
        allowedDataTypes.put(Element.Type.SPECIESMODE, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.DOUBLE);
        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.VOLUME, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOLLIST);
        allowedDataTypes.put(Element.Type.PRODUCTS, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOLLIST);
        allowedDataTypes.put(Element.Type.REACTANTS, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOLLIST);
        allowedDataTypes.put(Element.Type.PARAMETERS, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOLLIST);
        allowedDataTypes.put(Element.Type.SUBMODELS, dataTypes.clone());
        dataTypes.clear();
        
        dataTypes.add(Element.DataType.DOUBLE);
        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.VALUE, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOLLIST);
        allowedDataTypes.put(Element.Type.REACTIONS, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOLLIST);
        allowedDataTypes.put(Element.Type.SPECIESLIST, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.DATALIST);
        allowedDataTypes.put(Element.Type.POPULATIONS, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.DOUBLE);
        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.STARTTIME, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.DOUBLE);
        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.STOPTIME, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.SPECIESPOPULATIONS, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOLLIST);
        allowedDataTypes.put(Element.Type.VIEWSPECIES, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.MODIFIER);
        allowedDataTypes.put(Element.Type.OUTPUT, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.SPECIES, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.INTEGER);
        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.POPULATION, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.NUMTIMEPOINTS, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.START, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.STOP, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.STEP, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.REACTION, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.DEBUG, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.INTEGER);
        allowedDataTypes.put(Element.Type.ENSEMBLESIZE, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.PARAMETER, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.OUTPUTFILE, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.STORENAME, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.SIMULATOR, dataTypes.clone());
        dataTypes.clear();

        dataTypes.add(Element.DataType.SYMBOL);
        allowedDataTypes.put(Element.Type.EXPORTER, dataTypes.clone());
        dataTypes.clear();

        // allowed modifiers 
        HashSet modifiers = new HashSet();
        HashMap allowedModifiers = getAllowedModifiers();

        modifiers.add(Element.ModifierCode.BOUNDARY);
        modifiers.add(Element.ModifierCode.FLOATING);
        allowedModifiers.put(Element.Type.SPECIESTYPE, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.COMPARTMENT, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.RATE, modifiers.clone());
        modifiers.clear();

        modifiers.add(Element.ModifierCode.CONCENTRATION);
        modifiers.add(Element.ModifierCode.MOLECULES);
        allowedModifiers.put(Element.Type.SPECIESMODE, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.VOLUME, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.PRODUCTS, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.REACTANTS, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.PARAMETERS, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.SUBMODELS, modifiers.clone());
        modifiers.clear();
        
        allowedModifiers.put(Element.Type.VALUE, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.REACTIONS, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.SPECIESLIST, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.POPULATIONS, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.STARTTIME, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.STOPTIME, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.SPECIESPOPULATIONS, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.VIEWSPECIES, modifiers.clone());
        modifiers.clear();

        modifiers.add(Element.ModifierCode.PRINT);
        modifiers.add(Element.ModifierCode.STORE);
        allowedModifiers.put(Element.Type.OUTPUT, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.POPULATION, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.SPECIES, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.SPECIES, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.REACTION, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.DEBUG, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.ENSEMBLESIZE, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.PARAMETER, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.OUTPUTFILE, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.STORENAME, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.SIMULATOR, modifiers.clone());
        modifiers.clear();

        allowedModifiers.put(Element.Type.EXPORTER, modifiers.clone());
        modifiers.clear();

        // IMPORTANT:  check the parser for integrity and completeness, before
        //             leaving this function (this is our earliest chance to check)
        parserSanityCheck();
    }

    private void parserSanityCheck() throws IllegalStateException
    {
        Iterator statementTypesIter = Statement.Type.getIter();
        while(statementTypesIter.hasNext())
        {
            Statement.Type statementType = (Statement.Type) statementTypesIter.next();
            Set allowedElements = (Set) getAllowedElements().get(statementType);
            if(null == allowedElements)
            {
                throw new IllegalStateException("statement type has no allowed elements list: " + statementType);
            }
            Set requiredElements = (Set) getRequiredElements().get(statementType);
            if(null == requiredElements)
            {
                throw new IllegalStateException("statement type has no required elements list: " + statementType);
            }
        }

        Iterator elementTypesIter = Element.Type.getIter();
        while(elementTypesIter.hasNext())
        {
            Element.Type elementType = (Element.Type) elementTypesIter.next();
            Set allowedElementDataTypes = (Set) getAllowedElementDataTypes().get(elementType);
            if(null == allowedElementDataTypes)
            {
                throw new IllegalStateException("element type has no allowed data types list: " + elementType);
            }
        }
    }

    private void parseElements(ListIterator pTokenIterator, 
                               Set pAllowedElements,
                               Set pRequiredElements,
                               SymbolDoubleMap pSymbolDoubleMap,
                               HashMap pReturnElements) throws InvalidInputException
    {
        Element element = null;

        Set requiredElementsFound = new HashSet();

        boolean inDataList = false;
        StringBuffer exp = null;

        while(pTokenIterator.hasNext())
        {
            Token token = (Token) pTokenIterator.next();
  
// FOR DEBUGGING PURPOSES ONLY-------------------------------------           
//            System.out.println("parsing token: " + token);
// FOR DEBUGGING PURPOSES ONLY-------------------------------------           

            Token.Code tokenCode = token.mCode;

            if(Token.Code.SYMBOL == tokenCode)
            {
                if(null != element)
                {
                    if(null == exp)
                    {
                        if(! inDataList)
                        {
                            element.setSymbolName(token.mSymbolName);
                        }
                        else
                        {
                            element.addDataToList(token.mSymbolName);
                        }
                    }
                    else
                    {
                        exp.append(token.mSymbolName);
                    }
                }
                else
                {
                    throw new InvalidInputException("symbol token encountered before keyword: " + token.mSymbolName + "; perhaps a comma delimiter has been mistakenly added;");
                }
            }
            else if(Token.Code.NUMBER == tokenCode)
            {
                if(null != element)
                {
                    if(null == exp)
                    {
                        if(! inDataList)
                        {
                            if(null == element.getNumberValue())
                            {
                                element.setNumberValue(token.mNumberValue);
                            }
                            else
                            {
                                throw new InvalidInputException("numeric value for token has already been defined");
                            }
                        }
                        else
                        {
                            if(null == element.getNumberValue())
                            {
                                element.addDataToList(token.mNumberValue);
                            }
                            else
                            {
                                throw new InvalidInputException("numeric value for token has already been defined");
                            }
                        }
                    }
                    else
                    {
                        exp.append(token.mNumberValue.toString());
                    }
                }
                else
                {
                    throw new InvalidInputException("numeric token encountered before keyword: " + token.mNumberValue);
                }
            }
            else if(Token.Code.KEYWORD == tokenCode)
            {
                if(null == element)
                {
                    String keywordName = token.mKeywordName;
                    Element.Type elementType = Element.Type.get(keywordName);
                    if(null == elementType)
                    {
                        throw new InvalidInputException("unknown keyword encountered: " + keywordName);
                    }
                    // is this element allowed?
                    if(pAllowedElements.contains(elementType))
                    {
                        element = new Element(elementType);
                    }
                    else
                    {
                        throw new InvalidInputException("keyword not allowed for this statement: " + keywordName);
                    }
                    requiredElementsFound.add(elementType);
                }
                else
                {
                    if(null == exp)
                    {
                        // this must be a modifier (such as "boundary" or "floating")
                        if(null == element.getModifier())
                        {
                            String modifier = token.mKeywordName;
                            Element.ModifierCode modifierCode = Element.ModifierCode.get(modifier);
                            if(null != modifierCode)
                            {
                                Element.Type elementType = element.getType();
                                HashSet allowedModifiers = (HashSet) mAllowedModifiers.get(elementType);
                                if(null != allowedModifiers)
                                {
                                    if(allowedModifiers.contains(modifierCode))
                                    {
                                        element.setModifier(modifierCode);
                                    }
                                    else
                                    {
                                        throw new InvalidInputException("modifier not allowed for element type: " +  element.getType() + "; modifiercode: " + modifierCode);
                                    }
                                }
                                else
                                {
                                    throw new InvalidInputException("no modifiers are allowed for element type: " +  element.getType());
                                }
                            }
                            else
                            {
                                throw new InvalidInputException("unknown modifier: " + modifier + "; possibly a \",\" is missing after the previous element; or, possibly this modifier should have double-quotes around it;");
                            }
                        }
                        else
                        {
                            throw new InvalidInputException("encountered keyword token when already defined: " + token.mKeywordName);
                        }
                    }
                    else
                    {
                        exp.append(token.mKeywordName);
                    }
                }
            }
            else if(Token.Code.LIST_BEGIN == tokenCode)
            {
                if(null != element)
                {
                    if(null == exp)
                    {
                        if(null == element.getSymbolName())
                        {
                            inDataList = true;
                            element.createDataList();
                        }
                        else
                        {
                            throw new InvalidInputException("list begin element encountered after symbol element");
                        }
                    }
                    else
                    {
                        exp.append("(");
                    }
                }
                else
                {
                    throw new InvalidInputException("list begin element encountered before keyword");
                }
            }
            else if(Token.Code.LIST_END == tokenCode)
            {
                if(null != element)
                {
                    if(null == exp)
                    {
                        if(null == element.getSymbolName())
                        {
                            inDataList = false;
                        }
                        else
                        {
                            throw new InvalidInputException("list end element encountered after symbol element");
                        }
                    }
                    else
                    {
                        exp.append(")");
                    }
                }
                else
                {
                    throw new InvalidInputException("list end element encountered before keyword");
                }
            }
            else if(Token.Code.BLOCK_END == tokenCode)
            {
                throw new InvalidInputException("end block token not permitted while parsing statement elements");
            }
            else if(Token.Code.EXP_BEGIN == tokenCode)
            {
                if(null == element)
                {
                    throw new InvalidInputException("begin-expression token found before element-defining keyword");
                }
                if(null != exp)
                {
                    throw new InvalidInputException("begin-expression token found when already within an expression");
                }
                exp = new StringBuffer();
            }
            else if(Token.Code.EXP_END == tokenCode)
            {
                if(null == exp)
                {
                    throw new InvalidInputException("end-expression token found when not currently within an expression");
                }
                // evalue the element
                String expStr = exp.toString();
                MathExpression mathExpression = new MathExpression(expStr);
                double expVal = 0.0;
                try
                {
                    expVal = mathExpression.computeValue(pSymbolDoubleMap);
                }
                catch(DataNotFoundException e)
                {
                    throw new InvalidInputException("unable to parse mathematical expression; error message is: " + e.toString());
                }
                Double expValObj = new Double(expVal);
                if(! inDataList)
                {
                    if(null != element.getNumberValue())
                    {
                        throw new InvalidInputException("expression found after number value already assigned for element, for element: " + element.getType());
                    }
                    element.setNumberValue(expValObj);
                }
                else
                {
                    element.addDataToList(expValObj);
                }
                exp = null;
            }
            else if(Token.Code.ELEMENT_DELIMITER == tokenCode ||
                    Token.Code.END_STATEMENT == tokenCode ||
                    Token.Code.BLOCK_BEGIN == tokenCode)
            {
                if(null != element)
                {
                    try
                    {
                        element.validate(mAllowedElementDataTypes);
                    }
                    catch(IllegalStateException e)
                    {
                        throw new InvalidInputException("invalid token stream for element; error message is: " + e.toString());
                    }
                    Element.Type elementType = element.getType();
                    if(null != pReturnElements.get(elementType))
                    {
                        throw new InvalidInputException("element defined twice for a given statement: " + elementType);
                    }
                    pReturnElements.put(elementType, element);
                    element = null;
                }
                else
                {
                    throw new InvalidInputException("element delimiter encountered unexpectedly");
                }
                if(Token.Code.ELEMENT_DELIMITER == tokenCode)
                {
                    if(! pTokenIterator.hasNext())
                    {
                        throw new InvalidInputException("element delimiter followed by no more tokens");
                    }
                    Token nextToken = (Token) pTokenIterator.next();
                    if(Token.Code.END_STATEMENT == nextToken.mCode)
                    {
                        throw new InvalidInputException("element delimiter followed end of statement");
                    }
                    else if(Token.Code.BLOCK_BEGIN == nextToken.mCode)
                    {
                        throw new InvalidInputException("element delimiter followed begin-block token");
                    }
                    pTokenIterator.previous();
                }
                else
                {
                    // end of statement or beginning of block means that 
                    // we are done parsing the elements for this statement
                    break;
                }
            }
            else
            {
                throw new InvalidInputException("unknown token code encountered: " + tokenCode);
            }
        }

        if(! requiredElementsFound.containsAll(pRequiredElements))
        {
            throw new InvalidInputException("required elements not supplied for this type of statement; required elements are: " + pRequiredElements);
        }
    }

    private static void tokenizeLine(String pLine, int pLineNumber, List pLineTokens) throws InvalidInputException
    {
        // a model description is a sequence of *statements* terminated by ";" characters. Statements
        // are made up of a sequence of *elements*; the first element is always followed by either a ":"
        // (meaning more elements follow), or a ";" (meaning no more elements follow); subsequent elements
        // in the statement are delimited with "," characters

        boolean includeDelimiters = true;
        StringTokenizer tokenizer = new StringTokenizer(pLine, DELIMITERS, includeDelimiters);

        StringBuffer quotedString = null;

        while(tokenizer.hasMoreTokens())
        {
            String tokenStr = tokenizer.nextToken();
            String trimTokenStr = tokenStr.trim();
            if(0 == trimTokenStr.length())
            {
                // ignore any zero-length tokens after trimming, as these are whitespace
                continue;
            }

            if(quotedString != null)
            {
                // we are in a quotation, which requires special processing of the token

                if(tokenStr.equals("\""))
                {
                    // quotation just ended; create a symbol token to contain it
                    Token token = new Token(Token.Code.SYMBOL, pLineNumber);
                    token.mSymbolName = quotedString.toString();
                    pLineTokens.add(token);

                    quotedString = null;
                }
                else
                {
                    // append the string to the running quotation
                    quotedString.append(tokenStr);
                }
            }
            else
            {
                tokenStr = trimTokenStr;

                // we are not in a quotation; process the token normally

                if(tokenStr.equals("\""))
                {
                    // start of a new quotation
                    quotedString = new StringBuffer("");
                }
                else
                {
                    if(tokenStr.equals("#"))
                    {
                        // discard the rest of the line of input
                        break;
                    }
                    else if(tokenStr.equals("("))
                    {
                        pLineTokens.add(new Token(Token.Code.LIST_BEGIN, pLineNumber));
                    }
                    else if(tokenStr.equals(")"))
                    {
                        pLineTokens.add(new Token(Token.Code.LIST_END, pLineNumber));
                    }
                    else if(tokenStr.equals(","))
                    {
                        pLineTokens.add(new Token(Token.Code.ELEMENT_DELIMITER, pLineNumber));
                    }
                    else if(tokenStr.equals(":"))
                    {
                        pLineTokens.add(new Token(Token.Code.FIRST_ELEMENT_DELIMITER, pLineNumber));
                    }
                    else if(tokenStr.equals(";"))
                    {
                        pLineTokens.add(new Token(Token.Code.END_STATEMENT, pLineNumber));
                    }
                    else if(tokenStr.equals("{"))
                    {
                        pLineTokens.add(new Token(Token.Code.BLOCK_BEGIN, pLineNumber));
                    }
                    else if(tokenStr.equals("}"))
                    {
                        pLineTokens.add(new Token(Token.Code.BLOCK_END, pLineNumber));
                    }
                    else if(tokenStr.equals("["))
                    {
                        pLineTokens.add(new Token(Token.Code.EXP_BEGIN, pLineNumber));
                    }
                    else if(tokenStr.equals("]"))
                    {
                        pLineTokens.add(new Token(Token.Code.EXP_END, pLineNumber));
                    }
                    else
                    {
                        Token token = null;
                        try
                        {
                            double val = Double.parseDouble(tokenStr);
                            token = new Token(Token.Code.NUMBER, pLineNumber);
                            token.mNumberValue = new Double(val);
                        }
                        catch(NumberFormatException e)
                        {
                            token = new Token(Token.Code.KEYWORD, pLineNumber);
                            token.mKeywordName = tokenStr;
                        }
                        pLineTokens.add(token);
                    }
                }
            }            
        }
        if(null != quotedString)
        {
            throw new InvalidInputException("end of line reached in the middle of a quotation");
        }
    }

    private String translateMathExpressionsInString(String pInputString, 
                                                    ISymbolDoubleMap pSymbolDoubleMap,
                                                    NumberFormat pNumberFormat) throws DataNotFoundException, IllegalArgumentException
    {
        Pattern searchPatternMath = getSearchPatternMath();
        Matcher matcher = searchPatternMath.matcher(pInputString);
        while(matcher.find())
        {
            String matchedSubsequence = matcher.group(1);
            MathExpression exp = new MathExpression(matchedSubsequence);
            double value = exp.computeValue(pSymbolDoubleMap);
            String formattedExp = null;
            if(null != pNumberFormat)
            {
                formattedExp = pNumberFormat.format(value);
            }
            else
            {
                formattedExp = (new Double(value)).toString();
            }
            pInputString = matcher.replaceFirst(formattedExp);
            matcher = searchPatternMath.matcher(pInputString);
        }
        return(pInputString);
    }

    private void parseMathExpressionWithinElement(Element pElement, SymbolDoubleMap pSymbolDoubleMap) throws DataNotFoundException, IllegalArgumentException
    {
        NumberFormat nf = getNumberFormatMath();

        String symbolName = pElement.getSymbolName();
        if(null != symbolName)
        {
            String translatedSymbolName = translateMathExpressionsInString(symbolName, pSymbolDoubleMap, nf);
            // within an element symbol name, math expressions are formatted as integers:
            pElement.setSymbolName(translatedSymbolName);
        }
        
        if(pElement.hasDataList())
        {
            ListIterator dataListIter = pElement.getDataListIter();
            while(dataListIter.hasNext())
            {
                Object datum = dataListIter.next();
                if(datum instanceof String)
                {
                    String datumString = (String) datum;
                    datumString = translateMathExpressionsInString(datumString, pSymbolDoubleMap, nf);
                    dataListIter.remove();
                    dataListIter.add(datumString);
                }
            }
        }
    }

    private void parseMathExpressionsWithinStatementSymbols(Statement pStatement, SymbolDoubleMap pSymbolDoubleMap) throws DataNotFoundException, IllegalArgumentException
    {
        Pattern searchPattern = getSearchPatternMath();

        NumberFormat nf = getNumberFormatMath();

        boolean roundOffInt = true;
        String statementName = translateMathExpressionsInString(pStatement.getName(), pSymbolDoubleMap, nf);
        pStatement.setName(statementName);

        Iterator elementsIter = pStatement.getElementsIter();
        while(elementsIter.hasNext())
        {
            Element element = (Element) elementsIter.next();
            parseMathExpressionWithinElement(element, pSymbolDoubleMap);
        }
    }

    private void handleDefine(String pSymbolName,
                              ListIterator pTokenIter, 
                              HashMap pElementsMap,
                              SymbolDoubleMap pSymbolDoubleMap) throws InvalidInputException, IOException
    {
            
        parseElements(pTokenIter, 
                      ALLOWED_ELEMENTS_DEFINE, 
                      REQUIRED_ELEMENTS_DEFINE,
                      pSymbolDoubleMap,
                      pElementsMap);  

        Element valueElement = (Element) pElementsMap.get(Element.Type.VALUE);
        assert (null != valueElement) : "parser improperly defined:  define without VALUE element";
        Double valueObj = valueElement.getNumberValue();
        assert (null != valueObj) : "parser improperly defined:  VALUE element without numeric value";
        pSymbolDoubleMap.setValue(pSymbolName, valueObj);
    }

    private void handleLoop(String pLoopIndex,
                            ListIterator pTokenIter, 
                            IFileIncludeHandler pFileIncludeHandler, 
                            HashMap pElementsMap,
                            SymbolDoubleMap pSymbolDoubleMap,
                            Script pScript) throws InvalidInputException, IOException
    {
        parseElements(pTokenIter, 
                      ALLOWED_ELEMENTS_LOOP, 
                      REQUIRED_ELEMENTS_LOOP,
                      pSymbolDoubleMap,
                      pElementsMap);

        Token prevToken = (Token) pTokenIter.previous();
        if(Token.Code.BLOCK_BEGIN != prevToken.mCode)
        {
            throw new InvalidInputException("loop statement did not terminate with begin-block token");
        }
        pTokenIter.next();

        Element startElement = (Element) pElementsMap.get(Element.Type.START);
        assert (null != startElement) : "parser improperly defined:  loop without START element";
        Double startElementValue = startElement.getNumberValue();
        assert (null != startElementValue) : "parser improperly defined:  loop has START element with no numeric value";
        int startIndex = startElementValue.intValue();

        Element stopElement = (Element) pElementsMap.get(Element.Type.STOP);
        assert (null != stopElement) : "parser improperly defined:  loop without STOP element";
        Double stopElementValue = stopElement.getNumberValue();
        assert (null != stopElementValue) : "parser improperly defined:  loop has STOP element with no numeric value";
        int stopIndex = stopElementValue.intValue();

        int stepValue = LOOP_DEFAULT_STEP;
        Element stepElement = (Element) pElementsMap.get(Element.Type.STEP);
        if(null != stepElement)
        {
            Double stepElementValue = stepElement.getNumberValue();
            assert (null != stopElementValue) : "parser improperly defined:  loop has STEP element with no numeric value";
            stepValue = stepElementValue.intValue();
        }

        if(stepValue <= 0)
        {
            throw new InvalidInputException("invalid loop step value: " + stepValue);
        }

        List subTokens = new LinkedList();

        int braceDepth = 1;

        while(braceDepth > 0)
        {
            if(! pTokenIter.hasNext())
            {
                throw new InvalidInputException("begin block has no corresponding end block");
            }
            Token myToken = (Token) pTokenIter.next();
            pTokenIter.remove();

            Token.Code myTokenCode = myToken.mCode;
            if(Token.Code.BLOCK_BEGIN == myTokenCode)
            {
                braceDepth++;
            }
            else if(Token.Code.BLOCK_END == myTokenCode)
            {
                braceDepth--;
            }
            if(braceDepth > 0)
            {
                subTokens.add(myToken);
            }
        }

        for(int index = startIndex; index <= stopIndex; index += stepValue)
        {
            Script subScript = new Script();
            ListIterator subTokensIter = subTokens.listIterator();

            while(subTokensIter.hasNext())
            {
                pSymbolDoubleMap.setValue(pLoopIndex, new Double((double) index));
                try
                {
                    parseCommandLanguageStatement(subTokensIter, 
                                                  pFileIncludeHandler, 
                                                  pSymbolDoubleMap,
                                                  subScript);
                }
                catch(InvalidInputException e)
                {
                    StringBuffer msgBuf = new StringBuffer(e.toString());
                    if(subTokensIter.hasPrevious())
                    {
                        Token token = (Token) subTokensIter.previous();
                        if(null != token)
                        {
                            msgBuf.append(" at line " + token.mLineNumber + ";");
                        }
                    }
                    msgBuf.append(" within loop statement starting");
                    throw new InvalidInputException(msgBuf.toString(), e);
                }
            }
            Iterator subStatementsIter = subScript.getStatementsIter();
            while(subStatementsIter.hasNext())
            {
                Statement subStatement = (Statement) ((Statement) subStatementsIter.next()).clone();
                pScript.addStatement(subStatement);
            }
        }

        pSymbolDoubleMap.setValue(pLoopIndex, null);

    }
                
    private void parseCommandLanguageStatement(ListIterator pTokenIter, 
                                               IFileIncludeHandler pFileIncludeHandler, 
                                               SymbolDoubleMap pSymbolDoubleMap,
                                               Script pScript) throws InvalidInputException, IOException
    {
        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("statement did not contain a first keyword");
        }

        Token token = (Token) pTokenIter.next();
        if(token.mCode != Token.Code.KEYWORD)
        {
            throw new InvalidInputException("statement did not begin with a valid keyword");
        }

        String keywordName = token.mKeywordName;
        
        if(! pTokenIter.hasNext())
        {
            throw new InvalidInputException("statement keyword not followed with a name; statement keyword was: " + keywordName);
        }
        
        token = (Token) pTokenIter.next();

        if(token.mCode != Token.Code.SYMBOL)
        {
            throw new InvalidInputException("statement keyword not followed with a valid name; statement keyword was: " + keywordName + "; succeeding token type was: " + token.mCode);
        }

        String symbolName = token.mSymbolName;

        token = (Token) pTokenIter.next();
        Token.Code nextTokenCode = token.mCode;

        boolean hasElements = false;
        HashMap elementsMap = null;

        if(nextTokenCode == Token.Code.END_STATEMENT)
        {
            // there are no elements for this statement
        }
        else if(nextTokenCode == Token.Code.FIRST_ELEMENT_DELIMITER)
        {
            hasElements = true;
            elementsMap = new HashMap();
        }
        else
        {
            throw new InvalidInputException("invalid token type encountered: " + nextTokenCode + "; this probably means that a \":\" character is missing;");
        }

        if(keywordName.equals(KEYWORD_INCLUDE))
        {
            if(hasElements)
            {
                throw new InvalidInputException("elements not allowed in an INCLUDE statement");
            }

            // handle INCLUDE statement
            String includeFileName = symbolName;
            assert (null != includeFileName) : "parser error:  include statement encountered with no include file name";
            if(null != pFileIncludeHandler)
            {
                boolean withinIncludeFile = true;
                readFromFileRecursive(includeFileName, pFileIncludeHandler, pScript);
            }
            else
            {
                throw new InvalidInputException("unable to process include directive because no file include handler was provided, for include file: \"" + includeFileName + "\";");
            }
        }
        else if(keywordName.equals(KEYWORD_DEFINE))
        {
            if(! hasElements)
            {
                throw new InvalidInputException("elements required for a DEFINE statement");
            }

            handleDefine(symbolName,
                         pTokenIter,
                         elementsMap,
                         pSymbolDoubleMap);
        }
        else if(keywordName.equals(KEYWORD_LOOP))
        {
            if(! hasElements)
            {
                throw new InvalidInputException("elements required for a LOOP statement");
            }
            // symbolName contains the loop index name, such as "i" in the expression:  loop "i": start 1, stop 10 { ...

            handleLoop(symbolName,
                       pTokenIter,
                       pFileIncludeHandler,
                       elementsMap,
                       pSymbolDoubleMap,
                       pScript);
        }
        else
        {
            // handle normal scripting statement
            Statement.Type statementType = (Statement.Type) Statement.Type.get(keywordName);

            if(null == statementType)
            {
                throw new InvalidInputException("statement keyword name is invalid: \"" + keywordName + "\"");
            }

            Statement statement = new Statement(statementType);

            statement.setName(symbolName);

            if(hasElements)
            {
                Set allowedElements = (Set) getAllowedElements().get(statementType);
                assert (null != allowedElements) : new String("statement type has no allowed elements list: " + statementType);
                Set requiredElements = (Set) getRequiredElements().get(statementType);
                assert (null != requiredElements) : new String("statement type has no required elements list: " + statementType);
                // there are elements for this statement, need to parse them
                parseElements(pTokenIter, 
                              allowedElements, 
                              requiredElements, 
                              pSymbolDoubleMap,
                              elementsMap);

                Collection elementSet = elementsMap.values();
                Iterator elementsIter = elementSet.iterator();
                while(elementsIter.hasNext())
                {
                    Element element = (Element) elementsIter.next();
                    statement.putElement(element);
                }
            }

            try
            {
                parseMathExpressionsWithinStatementSymbols(statement, pSymbolDoubleMap);
            }
            catch(Exception e)
            {
                throw new InvalidInputException("unable to parse math expression within statement; error message is: " + e.toString(), e);
            }
            
            pScript.addStatement(statement);
        }
    }

    /*========================================*
     * protected methods
     *========================================*/

    void readFromFileRecursive(String pFileName, IFileIncludeHandler pFileIncludeHandler, Script pScript) throws InvalidInputException, IOException
    {
        BufferedReader bufferedReader = pFileIncludeHandler.openReaderForIncludeFile(pFileName);
        if(null != bufferedReader)
        {
            try
            {
                appendFromInputStream(bufferedReader, 
                                      pFileIncludeHandler, 
                                      pScript);
            }
            catch(InvalidInputException e)
            {
                StringBuffer sb = new StringBuffer(e.toString() + " in file: \"" + pFileIncludeHandler.getIncludeFileAbsolutePath(pFileName) + "\"");
                if(pFileIncludeHandler.isWithinIncludeFile())
                {
                    sb.append("; included");
                }
                
                String message = sb.toString();
                throw new InvalidInputException(message, e);
            }
        }
    }



    /*========================================*
     * public methods
     *========================================*/

    public void appendFromInputStream(BufferedReader pBufferedReader, 
                                      IFileIncludeHandler pFileIncludeHandler, 
                                      Script pScript) throws InvalidInputException, IOException
    {
        int lineCounter = 0;
        List tokenList = new LinkedList();

        boolean withinIncludeFile = false;
        if(null != pFileIncludeHandler)
        {
            withinIncludeFile = pFileIncludeHandler.isWithinIncludeFile();
        }

        ITranslator translator = getPreprocessor();
        if(null != translator)
        {
            StringWriter translatedInputStreamStringWriter = new StringWriter();
            PrintWriter translatedInputStreamPrintWriter = new PrintWriter(translatedInputStreamStringWriter);
            if(! withinIncludeFile)
            {
                translator.generatePreamble(translatedInputStreamPrintWriter);
            }
            // need to translate the input stream
            translator.translate(pBufferedReader, translatedInputStreamPrintWriter);
            String translatedInputStream = translatedInputStreamStringWriter.toString();
            StringReader translatedInputStreamReader = new StringReader(translatedInputStream);
            pBufferedReader = new BufferedReader(translatedInputStreamReader);
        }

        try
        {
            String line = null;
            while((line = pBufferedReader.readLine()) != null)
            {
                ++lineCounter;
                tokenizeLine(line, lineCounter, tokenList);
            }
        }

        catch(Exception e)
        {
            throw new InvalidInputException("error tokenizing input stream; error message is: " + e.toString() + "; on line " + lineCounter, e);        
        }

        ListIterator tokenIter = tokenList.listIterator();

        SymbolDoubleMap loopIndexMap = new SymbolDoubleMap();

        try
        {
            while(tokenIter.hasNext())
            {
                parseCommandLanguageStatement(tokenIter, pFileIncludeHandler, loopIndexMap, pScript);
            }
        }

        catch(InvalidInputException e)
        {
            StringBuffer message = new StringBuffer(e.toString());
            if(tokenIter.hasPrevious())
            {
                CommandLanguageParser.Token token = (CommandLanguageParser.Token) tokenIter.previous();
                message.append(" on line " + token.mLineNumber);
            }
            throw new InvalidInputException(message.toString(), e);
        }
    }

    public String getFileRegex()
    {
        return(".*\\.isbchem$");
    }
}
