package isb.chem.scripting;

import java.io.*;
import isb.chem.*;
import isb.util.*;
import isb.chem.scripting.*;
import java.util.*;

/**
 * Command-line interface to the {@link IScriptBuildingParser}
 * and {@link ScriptRuntime} classes.  Allows for specifying
 * a script file to be parsed and executed, using the
 * script parser class specified (the class must implement
 * {@link IScriptBuildingParser}).   The user must specify a simulator
 * class to use, which must be a class implementing 
 * {@link ISimulator}.
 *
 * @see IScriptBuildingParser
 * @see ScriptRuntime
 *
 * @author Stephen Ramsey
 */
public class MainScriptRunner extends CommandLineApp
{
    private static final String DEBUG_ARG = "-debug";
    private static final String PARSER_ARG = "-parser";
    private static final int NUM_REQUIRED_ARGS = 1;
    private static final String DEFAULT_PARSER_ALIAS = isb.chem.scripting.CommandLanguageParser.CLASS_ALIAS;
    private boolean mDebug;
    private String mParserAlias;
    private String mFileName;
    private ScriptBuilder mScriptBuilder;

    private ScriptBuilder getScriptBuilder()
    {
        return(mScriptBuilder);
    }

    private void setScriptBuilder(ScriptBuilder pScriptBuilder)
    {
        mScriptBuilder = pScriptBuilder;
    }

    private boolean getDebug()
    {
        return(mDebug);
    }

    private void setDebug(boolean pDebug)
    {
        mDebug = pDebug;
    }

    private void setParserAlias(String pParserAlias)
    {
        mParserAlias = pParserAlias;
    }

    private String getParserAlias()
    {
        return(mParserAlias);
    }

    private String getFileName()
    {
        return(mFileName);
    }

    private void setFileName(String pFileName)
    {
        mFileName = pFileName;
    }
   

    public MainScriptRunner() throws ClassNotFoundException, IOException
    {
        setDebug(false);
        setParserAlias(null);
        setFileName(null);
    }

    protected void printUsage(OutputStream pOutputStream)
    {
        PrintWriter pw = new PrintWriter(pOutputStream);
        pw.println("usage:    java " + getClass().getName() + " [-debug] [-parser <parserAlias>] <scriptFile>");
        pw.println("  <parserAlias>:   the alias of the class implementing the interface ");
        pw.println("                   isb.chem.scripting.IScriptBuildingParser (default is determined");
        pw.println("                   by file extension");
        pw.println("  <scriptFile>:    the full filename of the script file to be parsed and executed");
        pw.println("Parser aliases:    ");
        pw.println("                   condensed-command-language (for Condensed Chemical Model Definition Language input),");
        pw.println("                   command-language (for Chemical Model Definition Language input)");
        pw.println("                   markup-language (for Systems Biology Markup Language input)");
        pw.println("If you do not specify a parser, the file suffix is used to select one");
        pw.flush();
    }

    

    private String selectParserAliasFromFileName(String pFileName) throws DataNotFoundException
    {
        ScriptBuilder scriptBuilder = getScriptBuilder();
        Set parserAliases = scriptBuilder.getParserAliasesCopy();
        Iterator parserAliasesIter = parserAliases.iterator();
        String retParserAlias = DEFAULT_PARSER_ALIAS;
        while(parserAliasesIter.hasNext())
        {
            String parserAlias = (String) parserAliasesIter.next();
            IScriptBuildingParser parser = scriptBuilder.getParser(parserAlias);
            String fileRegex = parser.getFileRegex();
            if(pFileName.matches(fileRegex))
            {
                retParserAlias = parserAlias;
                break;
            }
        }

        return(retParserAlias);
    }

    protected void handleCommandLine(String []pArgs) 
    {
        checkAndHandleHelpRequest(pArgs);

        int numArgs = pArgs.length;

        if(numArgs < NUM_REQUIRED_ARGS)
        {
            handleCommandLineError("the number of command-line arguments is insufficient; please refer to the usage description that follows");
        }

        for(int argCtr = 0; argCtr < numArgs; ++argCtr)
        {
            String arg = pArgs[argCtr];

            if(arg.substring(0, DEBUG_ARG.length()).equals(DEBUG_ARG))
            {
                setDebug(true);
                continue;
            }

            if(arg.substring(0, PARSER_ARG.length()).equals(PARSER_ARG))
            {
                if(argCtr == numArgs - 1)
                {
                    handleCommandLineError("argument \"" + PARSER_ARG + "\" has a required element after it; please refer to the usage description that follows");
                }

                ++argCtr;
                String parserAlias = pArgs[argCtr];
                setParserAlias(parserAlias);
                continue;
            }

            if(argCtr == numArgs - 1)
            {
                setFileName(arg);
            }
        }

        if(null == getFileName())
        {
            handleCommandLineError("required file name was not specified; please refer to the usage description that follows");
        }

        if(null == getParserAlias())
        {
            try
            {
                setParserAlias(selectParserAliasFromFileName(getFileName()));
            }
            catch(DataNotFoundException e)
            {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

        if(getDebug())
        {
            System.out.println("using parser alias: " + getParserAlias());
        }

    }

    private void run(String []pArgs)
    {
        try
        {
            ScriptBuilder scriptBuilder = new ScriptBuilder();
            setScriptBuilder(scriptBuilder);

            handleCommandLine(pArgs);

            String parserAlias = getParserAlias();

            if(getDebug())
            {
                System.out.println("building script...\n");
            }
            Script script = scriptBuilder.buildFromFile(parserAlias, getFileName());
            if(getDebug())
            {
                System.out.println("script build process complete; script is: \n");
                System.out.print(script.toString());
                System.out.println("\n\nexecuting script...\n");
            }

            ScriptRuntime scriptRuntime = new ScriptRuntime();

            scriptRuntime.execute(script);

            if(getDebug())
            {
                System.out.println("script execution complete");
            }
        }

        catch(Exception e)
        {
            System.err.println("an exception occurred; the error message is:");
            System.err.println(e.toString());
            Throwable cause = e.getCause();
            if(null != cause)
            {
                System.err.println("cause of exception: " + e.getCause().toString());
            }
            if(getDebug())
            {
                System.err.println("\nthe detailed error message of this exception is:\n");
                System.err.println(e.getMessage() + "\n");
                System.err.println("\nthe stack backtrace of this exception is:\n");
                e.printStackTrace(System.err);
                if(null != cause)
                {
                    System.err.println("\nthe stack backtrace of the cause exception is:\n");
                    cause.printStackTrace(System.err);
                }
            }
            else
            {
                System.err.println("\nto see a stack backtrace of the exception, simply re-run this program with the \"" + DEBUG_ARG + "\" argument before the required arguments");
            }
            System.exit(1);
        }
    }

    public static void main(String []pArgs)
    {
        try
        {
            MainScriptRunner runner = new MainScriptRunner();
            runner.run(pArgs);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
