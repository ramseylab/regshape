package isb.chem.sbw.tp;

import isb.chem.*;
import isb.chem.scripting.*;
import isb.chem.sbw.*;
import isb.util.*;
import java.io.*;
import java.util.*;

public class CommandLanguageToMarkupLanguage
{
    public static void main(String []pArgs)
    {
        if(pArgs.length < 1)
        {
            System.err.println("required argument: filename");
            System.exit(1);
        }
        try
        {
            String inputFileName = pArgs[0];
            ScriptBuilder builder = new ScriptBuilder();
            Script script = builder.buildFromFile(CommandLanguageParser.CLASS_ALIAS, inputFileName);
            ScriptRuntime scriptRuntime = new ScriptRuntime();
            scriptRuntime.execute(script);
            Set modelNames = scriptRuntime.getModelNamesCopy();
            Iterator modelNamesIter = modelNames.iterator();
            assert (modelNamesIter.hasNext()) : "missing model name in SBML model";
            String modelName = (String) modelNamesIter.next();
            Set speciesPopulationsNames = scriptRuntime.getSpeciesPopulationsNamesCopy();
            Iterator speciesPopulationsNamesIter = speciesPopulationsNames.iterator();
            assert (speciesPopulationsNamesIter.hasNext()) : "missing species populations name";
            String speciesPopulationsName = (String) speciesPopulationsNamesIter.next();
            SpeciesPopulations speciesPopulations = scriptRuntime.getSpeciesPopulations(speciesPopulationsName);
            Model model = scriptRuntime.getModel(modelName);

            PrintWriter pw = new PrintWriter(System.out, true);
            IModelInstanceExporter modelInstanceExporter = new MarkupLanguageModelInstanceExporter();
            modelInstanceExporter.exportModelInstance(model, speciesPopulations, pw);
            System.out.println("");  // print an extra line so that it shows up in Bash shell
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
