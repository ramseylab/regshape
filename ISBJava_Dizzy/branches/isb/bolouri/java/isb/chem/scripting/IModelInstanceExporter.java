package isb.chem.scripting;

import isb.chem.*;
import isb.util.*;
import java.io.*;

/**
 * Defines a class that can write a markup language description of a
 * {@link Model} of chemical reactions, and a {@link SpeciesPopulations} 
 * defining the initial populations of the chemical {@link Species}.
 *
 * @see Model
 * @see Species
 * @see SpeciesPopulations
 *
 * @author Stephen Ramsey
 */
public interface IModelInstanceExporter
{
    public void exportModelInstance(Model pModel, SpeciesPopulations pInitialSpeciesPopulations, PrintWriter pOutputWriter) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, ModelInstanceExporterException;

    public String getFileRegex();
}
