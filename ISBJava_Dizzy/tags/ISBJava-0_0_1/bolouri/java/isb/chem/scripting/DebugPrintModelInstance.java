package isb.chem.scripting;

import isb.util.*;
import isb.chem.*;
import java.io.*;

/**
 * Default model description writer.  Just writes
 * a simple textual description of the model.
 *
 * @author Stephen Ramsey
 */
public class DebugPrintModelInstance implements IModelInstanceExporter, IAliasableClass
{
    public static final String CLASS_ALIAS = "debug-printing";

    public void exportModelInstance(Model pModel, SpeciesPopulations pInitialSpeciesPopulations, PrintWriter pOutputWriter) 
    {
        StringBuffer outputString = new StringBuffer();
        outputString.append(pModel.toString());
        outputString.append("\n\n");
        outputString.append(pInitialSpeciesPopulations.toString());
        pOutputWriter.write(outputString.toString());
        pOutputWriter.flush();

    }

    public String getFileRegex()
    {
        return(".*");
    }
}
