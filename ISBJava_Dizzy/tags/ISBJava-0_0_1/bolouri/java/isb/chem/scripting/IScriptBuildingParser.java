package isb.chem.scripting;

import java.io.*;
import isb.util.*;

/**
 * Defines the methods that must be 
 * implemented by a script-building parser.
 *
 * @author Stephen Ramsey
 */
public interface IScriptBuildingParser
{
    void appendFromInputStream(BufferedReader pBufferedReader, 
                               IFileIncludeHandler pFileIncludeHandler, 
                               Script pScript) throws InvalidInputException, IOException;
    String getFileRegex();
}
