package isb.chem.scripting;

import java.io.*;
import java.util.*;

/**
 * Simple implementation of the {@link IFileIncludeHandler}
 * class that has a <code>HashSet</code> of files that
 * have already been included.  This prevents double-inclusion
 * of a given file.  It stores a <code>File</code> representing
 * the &quot;include directory&quot;, which is the directory 
 * that is searched for include files.  Objects of this class are
 * instantiated by the the {@link ScriptBuilder} class, 
 * and are then used by the {@link CommandLanguageParser} class.
 * 
 * @see CommandLanguageParser
 * @see IFileIncludeHandler
 * @see ScriptBuilder
 *
 * @author Stephen Ramsey
 */
public class FileIncludeHandler implements IFileIncludeHandler
{
    private HashSet mIncludedFiles;
    private File mDirectory;

    File getDirectory()
    {
        return(mDirectory);
    }

    void setDirectory(File pDirectory) throws IllegalArgumentException
    {
        if(null != pDirectory)
        {
            if(! pDirectory.isDirectory())
            {
                throw new IllegalArgumentException("specified pathname is not a directory: " + pDirectory.getAbsolutePath());
            }
        }
        mDirectory = pDirectory;
    }
        
    private HashSet getIncludedFiles()
    {
        return(mIncludedFiles);
    }

    private void setIncludedFiles(HashSet pIncludedFiles)
    {
        mIncludedFiles = pIncludedFiles;
    }

    private boolean alreadyParsedFile(String pFileName)
    {
        return(getIncludedFiles().contains(pFileName));
    }

    private void storeParsedFile(String pFileName)
    {
        getIncludedFiles().add(pFileName);
    }

    public FileIncludeHandler()
    {
        setIncludedFiles(new HashSet());
        setDirectory(null);
    }

    public String getIncludeFileAbsolutePath(String pIncludeFileName) throws IOException
    {
        File includeFile = new File(pIncludeFileName);
        String includeFileAbsolutePath = null;
        if(! includeFile.isAbsolute())
        {
            File dirFile = getDirectory();
            if(null != dirFile)
            {
                includeFileAbsolutePath = dirFile.getAbsolutePath() + File.separator + pIncludeFileName;
            }
            else
            {
                includeFileAbsolutePath = (new File(pIncludeFileName)).getAbsolutePath();
            }
        }
        else
        {
            includeFileAbsolutePath = includeFile.getAbsolutePath();
        }
        return(includeFileAbsolutePath);
    }

    public BufferedReader openReaderForIncludeFile(String pIncludedFileName) throws IOException
    {
        String includeFileAbsolutePath = getIncludeFileAbsolutePath(pIncludedFileName);
        BufferedReader bufferedReader = null;
        if(! alreadyParsedFile(includeFileAbsolutePath))
        {
            storeParsedFile(includeFileAbsolutePath);
            FileReader fileReader = new FileReader(includeFileAbsolutePath);
            bufferedReader = new BufferedReader(fileReader);
        }
        else
        {
            // do nothing
        }
        return(bufferedReader);
    }


    public boolean isWithinIncludeFile()
    {
        return( getIncludedFiles().size() > 1 );
    }

}
