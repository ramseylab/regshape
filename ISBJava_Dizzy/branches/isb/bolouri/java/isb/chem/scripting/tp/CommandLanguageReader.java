package isb.chem.scripting.tp;

import isb.chem.scripting.*;

public class CommandLanguageReader
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
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
