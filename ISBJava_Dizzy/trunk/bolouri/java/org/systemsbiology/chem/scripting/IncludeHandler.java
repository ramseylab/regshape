package org.systemsbiology.chem.scripting;

import java.io.*;
import java.util.*;

/**
 *
 * @author Stephen Ramsey
 */
public class IncludeHandler
{
    private HashSet mIncludedFiles;
    private File mDirectory;

    public File getDirectory()
    {
        return(mDirectory);
    }

    public void setDirectory(File pDirectory) throws IllegalArgumentException
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

    public IncludeHandler()
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
