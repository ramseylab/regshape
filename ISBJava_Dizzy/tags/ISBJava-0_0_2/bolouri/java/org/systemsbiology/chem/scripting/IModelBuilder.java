package org.systemsbiology.chem.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import org.systemsbiology.chem.Model;
import org.systemsbiology.util.InvalidInputException;

public interface IModelBuilder
{
    public Model buildModel( BufferedReader pInputReader, IncludeHandler pIncludeHandler ) throws InvalidInputException, IOException;    
    public String getFileRegex();
}
