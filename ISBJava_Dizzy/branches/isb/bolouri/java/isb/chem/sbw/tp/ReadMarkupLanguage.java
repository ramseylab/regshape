package isb.chem.sbw.tp;

import isb.chem.*;
import isb.chem.sbw.*;
import isb.chem.scripting.*;

public class ReadMarkupLanguage
{

    public static void main(String []pArgs)
    {
        try
        {
            if(pArgs.length < 1)
            {
                System.err.println("usage: filename [reaction rate species mode]");
                System.exit(1);
            }
            String inputFileName = pArgs[0];
            String parserAlias = null;
            if(pArgs.length > 1)
            {
                String reactionRateSpeciesModeStr = pArgs[1];
                
                ReactionRateSpeciesMode reactionRateSpeciesMode = ReactionRateSpeciesMode.get(reactionRateSpeciesModeStr);
                if(null == reactionRateSpeciesMode)
                {
                    throw new IllegalArgumentException("invalid reaction rate species mode: " + reactionRateSpeciesModeStr);
                }

                if(reactionRateSpeciesMode.equals(ReactionRateSpeciesMode.MOLECULES))
                {
                    parserAlias = MarkupLanguageParserMolecules.CLASS_ALIAS;
                }
                else if(reactionRateSpeciesMode.equals(ReactionRateSpeciesMode.CONCENTRATION))
                {
                    parserAlias = MarkupLanguageParser.CLASS_ALIAS;
                }
                else
                {
                    throw new IllegalStateException("unknown reaction rate species mode: " + reactionRateSpeciesMode);
                }
            }
            ScriptBuilder builder = new ScriptBuilder();
            Script script = builder.buildFromFile(parserAlias, inputFileName);
            System.out.print(script.toString());
            
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
