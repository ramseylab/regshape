package org.systemsbiology.chem.scripting;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.*;
import java.util.*;

/**
 *
 * @author Stephen Ramsey
 */
public class FileIncludeHandler
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