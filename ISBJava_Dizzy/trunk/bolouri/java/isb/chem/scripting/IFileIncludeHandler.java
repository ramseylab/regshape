package isb.chem.scripting;

import java.io.*;

/**
 * Interface representing a class that can process
 * an &quot;include&quot; file, which is typically an
 * unqualified file name (with no path information).
 * An implementation of this class would find the file
 * within some include path, and return a 
 * <code>BufferedReader</code> for the file.
 * There is also a method for looking up the fully
 * qualified file name for the &quot;found&quot; 
 * include file.  An object implementing this interface 
 * is used by the {@link CommandLanguageParser} class.
 *
 * @see FileIncludeHandler
 * @see CommandLanguageParser
 *
 * @author Stephen Ramsey
 */
public interface IFileIncludeHandler
{
    BufferedReader openReaderForIncludeFile(String pIncludedFileName) throws IOException;
    String getIncludeFileAbsolutePath(String pIncludeFileName) throws IOException;
    boolean isWithinIncludeFile();
}

