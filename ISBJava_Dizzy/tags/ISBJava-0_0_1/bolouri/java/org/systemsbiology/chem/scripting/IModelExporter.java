package org.systemsbiology.chem.scripting;

import org.systemsbiology.chem.*;
import org.systemsbiology.util.*;
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
public interface IModelExporter
{
    public void export(Model pModel, PrintWriter pOutputWriter) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException;

    public String getFileRegex();
}