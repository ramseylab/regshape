package isb.chem.sbw;

import java.io.*;
import isb.chem.*;
import isb.util.*;
import isb.chem.scripting.*;

/**
 * Reads a model instance in SBML format, and converts it
 * to the {@link isb.chem.scripting.CommandLanguageParser} format.
 */
public class MainMarkupToCommandLanguageConverter extends CommandLineApp
{
    ReactionRateSpeciesMode mReactionRateSpeciesMode;
    String mInputFileName;
    private String getInputFileName()
    {
        return(mInputFileName);
    }

    private void setInputFileName(String pInputFileName)
    {
        mInputFileName = pInputFileName;
    }
    
    private ReactionRateSpeciesMode getReactionRateSpeciesMode()
    {
        return(mReactionRateSpeciesMode);
    }

    private void setReactionRateSpeciesMode(ReactionRateSpeciesMode pReactionRateSpeciesMode)
    {
        mReactionRateSpeciesMode = pReactionRateSpeciesMode;
    }

    protected void printUsage(OutputStream pOutputStream)
    {
        PrintWriter pw = new PrintWriter(pOutputStream);
        pw.println("usage:    java " + getClass().getName() + " filename [reaction rate species mode]");
        pw.flush();
    }

    protected void handleCommandLine(String []pArgs)
    {
        checkAndHandleHelpRequest(pArgs);

        if(pArgs.length < 1)
        {
            System.err.println("usage: filename [reaction rate species mode]");
            System.exit(1);
        }
        String inputFileName = pArgs[0];
        setInputFileName(inputFileName);

        ReactionRateSpeciesMode reactionRateSpeciesMode = ReactionRateSpeciesMode.CONCENTRATION;
        if(pArgs.length > 1)
        {
            String reactionRateSpeciesModeStr = pArgs[1];
                
            reactionRateSpeciesMode = ReactionRateSpeciesMode.get(reactionRateSpeciesModeStr);
            if(null == reactionRateSpeciesMode)
            {
                throw new IllegalArgumentException("invalid reaction rate species mode: " + reactionRateSpeciesModeStr);
            }
        }
    }

    private void run(String []pArgs) throws IOException, ModelInstanceImporterException, ScriptRuntimeException
    {
        handleCommandLine(pArgs);

        ReactionRateSpeciesMode reactionRateSpeciesMode = getReactionRateSpeciesMode();

        String inputFileName = getInputFileName();

        File inputFile = new File(inputFileName);
        FileReader inputFileReader = new FileReader(inputFile);
        BufferedReader inputBufferedReader = new BufferedReader(inputFileReader);

        Script modelDefinitionScript = new Script();
        MarkupLanguageParser parser = new MarkupLanguageParser();
        parser.setReactionRateSpeciesMode(reactionRateSpeciesMode);
        String modelName = parser.processMarkupLanguage(inputBufferedReader,
                                                        modelDefinitionScript);
        ScriptRuntime scriptRuntime = new ScriptRuntime();
        scriptRuntime.execute(modelDefinitionScript);
        String exporterAlias = CommandLanguageModelInstanceExporter.CLASS_ALIAS;
        Script exportScript = MarkupLanguageScriptBuildingUtility.buildExportScript(modelName,
                                                                                    exporterAlias);
        scriptRuntime.execute(exportScript);
    }

    public static void main(String []pArgs)
    {
        try
        {
            MainMarkupToCommandLanguageConverter app = new MainMarkupToCommandLanguageConverter();

            app.run(pArgs);

        }
        catch(Exception e)
        {
            System.err.println("exception: " + e.toString());
            System.err.println("message: " + e.getMessage());
            System.err.println("stack backtrace: ");
            e.printStackTrace(System.err);
        }
    }
}
