package isb.chem.scripting.tp;

import isb.chem.*;
import isb.chem.scripting.*;

public class ExecuteCommandLanguage
{
    public static void main(String []pArgs)
    {
        try
        {
            if(pArgs.length < 1)
            {
                System.err.println("required argument: filename");
                System.exit(1);
            }
            String inputFileName = pArgs[0];
            ScriptBuilder builder = new ScriptBuilder();
            Script script = builder.buildFromFile(CommandLanguageParser.CLASS_ALIAS, inputFileName);
            System.out.print(script.toString());
            ScriptRuntime runtime = new ScriptRuntime();
            runtime.execute(script);
        }
        catch(Exception e)
        {
            System.err.println("exception: " + e.toString());
            System.err.println("message: " + e.getMessage());
            System.err.println("stacktrace: ");
            e.printStackTrace(System.err);
        }
    }

}
