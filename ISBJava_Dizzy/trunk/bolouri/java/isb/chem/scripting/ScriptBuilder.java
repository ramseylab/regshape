package isb.chem.scripting;

import java.io.*;
import isb.util.*;
import isb.chem.*;
import java.util.*;

/**
 * Builds a {@link Script} object based on textual input
 * parsed using an {@link IScriptBuildingParser}.  Contains
 * a {@link isb.util.ClassRegistry} of parser plug-ins, each
 * of which implements the {@link IScriptBuildingParser}
 * interface.  Allows for specifying an {@link IStatementFilter}
 * to filter the statements returned by the parser.
 *
 * @see IScriptBuildingParser
 * @see IStatementFilter
 * @see Script
 *
 * @author Stephen Ramsey
 */
public class ScriptBuilder
{
    /*========================================*
     * constants
     *========================================*/

    /*========================================*
     * inner classes
     *========================================*/
    
    /*========================================*
     * member data
     *========================================*/
    private FileIncludeHandler mFileIncludeHandler;
    private ClassRegistry mParserRegistry;

    /*========================================*
     * accessor/mutator methods
     *========================================*/

    private ClassRegistry getParserRegistry()
    {
        return(mParserRegistry);
    }

    private void setParserRegistry(ClassRegistry pParserRegistry)
    {
        mParserRegistry = pParserRegistry;
    }

    protected FileIncludeHandler getFileIncludeHandler()
    {
        return(mFileIncludeHandler);
    }

    protected void setFileIncludeHandler(FileIncludeHandler pFileIncludeHandler)
    {
        mFileIncludeHandler = pFileIncludeHandler;
    }


    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeParserRegistry() throws ClassNotFoundException, IOException
    {
        ClassRegistry parserRegistry = new ClassRegistry(isb.chem.scripting.IScriptBuildingParser.class);
        parserRegistry.buildRegistry();
        setParserRegistry(parserRegistry);
    }

    /*========================================*
     * constructors
     *========================================*/
    public ScriptBuilder() throws ClassNotFoundException, IOException
    {
        setFileIncludeHandler(null);
        initializeParserRegistry();
    }

    /*========================================*
     * private methods
     *========================================*/


    /*========================================*
     * protected methods
     *========================================*/    


    /*========================================*
     * public methods
     *========================================*/

    /**
     * Returns a copy of the internal set of parser
     * aliases for the parsers contained in the 
     * {@link ClassRegistry} inside the script builder.
     * The parser aliases are strings.
     *
     * @return a copy of the internal set of parser
     * aliases for the parsers contained in the 
     * {@link ClassRegistry} inside the script builder.
     */
    public Set getParserAliasesCopy()
    {
        return(getParserRegistry().getRegistryAliasesCopy());
    }

    /**
     * Based on the parser alias specified, returns
     * the corresponding parser instance.  The parser
     * instance returned is an object that implements
     * the {@link IScriptBuildingParser} interface. If
     * no instance corresponding to this parser alias
     * exists, an object is instantiated, a reference to
     * it is stored internally, and the instance reference
     * is returned
     *
     * @param pParserAlias the parser alias that identifies
     * the type of parser instance to return
     *
     * @return the parser instance corresponding to 
     * the parser alias specified
     */
    public IScriptBuildingParser getParser(String pParserAlias) throws DataNotFoundException
    {
        return((IScriptBuildingParser) getParserRegistry().getInstance(pParserAlias));
    }

    /**
     * Returns a {@link Script} based on the commands or 
     * textual model dsecription contained in the
     * text input stream <code>pBufferedReader</code>.
     * No statement filtering is performed.
     * 
     * @param pBufferedReader the text input stream
     *
     * @return a {@link Script} based on the commands in the
     * text input stream <code>pBufferedReader</code>.
     */
    public Script buildFromInputStream(String pParserAlias, BufferedReader pBufferedReader) throws InvalidInputException, IOException, DataNotFoundException
    {
        IStatementFilter statementFilter = null;
        return(buildFromInputStream(pParserAlias, pBufferedReader, statementFilter));
    }

    /**
     * Functions the same as {@link #buildFromInputStream(String,BufferedReader)} 
     * with an additional statement filter, which 
     * will allow only those statements that are permitted
     * by the specified statement filter to be added
     * to the script that is built.
     */
    public Script buildFromInputStream(String pParserAlias, BufferedReader pBufferedReader, IStatementFilter pStatementFilter) throws InvalidInputException, IOException, DataNotFoundException
    {
        ClassRegistry parserRegistry = getParserRegistry();
        IScriptBuildingParser parser = (IScriptBuildingParser) parserRegistry.getInstance(pParserAlias);
        Script script = new Script();
        FileIncludeHandler fileIncludeHandler = getFileIncludeHandler();
        parser.appendFromInputStream(pBufferedReader, fileIncludeHandler, script);
        if(null != pStatementFilter)
        {
            Script filteredScript = new Script();
            Iterator statementsIter = script.getStatementsIter();
            while(statementsIter.hasNext())
            {
                Statement statement = (Statement) statementsIter.next();
                if(pStatementFilter.allowStatement(statement))
                {
                    filteredScript.addStatement(statement);
                }
            }
            script = filteredScript;
        }
        return(script);
    }

    /**
     * Returns a {@link Script} based on the commands or
     * textual model description contained in the text file 
     * whose name is <code>pFileName</code>. 
     * 
     * @param pFileName the file to be processed
     *
     * @return a {@link Script} based on the commands in the
     * text file <code>pFileName</code>.
     */
    public Script buildFromFile(String pParserAlias, String pFileName) throws InvalidInputException, IOException, DataNotFoundException
    {
        IStatementFilter statementFilter = null;
        return(buildFromFile(pParserAlias, pFileName, statementFilter));
    }

    /**
     * Functions the same as {@link #buildFromFile(String,String)} 
     * with an additional statement filter, which 
     * will allow only those statements that are permitted
     * by the specified statement filter to be added
     * to the script that is built.
     */
    public Script buildFromFile(String pParserAlias, String pFileName, IStatementFilter pStatementFilter) throws InvalidInputException, IOException, DataNotFoundException
    {
        boolean withinIncludeFile = false;
        FileIncludeHandler fileIncludeHandler = new FileIncludeHandler();
        File inputFile = new File(pFileName);
        if(inputFile.isAbsolute())
        {
            File inputDir = inputFile.getParentFile();
            fileIncludeHandler.setDirectory(inputDir);
        }
        setFileIncludeHandler(fileIncludeHandler);
        BufferedReader bufReader = fileIncludeHandler.openReaderForIncludeFile(pFileName);
        try
        {
            return(buildFromInputStream(pParserAlias, bufReader, pStatementFilter));
        }
        catch(Exception e)
        {
            StringBuffer sb = new StringBuffer(e.toString() + " in file: \"" + pFileName + "\"");
            String message = sb.toString();
                
            if(e instanceof IOException)
            {
                throw new IOException(message);
            }
            else if(e instanceof InvalidInputException)
            {
                throw new InvalidInputException(message);
            }
            else
            {
                throw new IllegalStateException("unknown exception type: " + e.toString());
            }   
        }
    }
}
