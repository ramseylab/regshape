package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.File;

/**
 * Utility class for manipulating file names 
 *
 * @author Stephen Ramsey
 */
public class FileUtils
{
    public static String addSuffixToFilename(String pFileName, String pSuffix)
    {
        int lastIndex = pFileName.lastIndexOf('.');
        String retVal = null;
        if(-1 != lastIndex)
        {
            retVal = pFileName.substring(0, lastIndex) + pSuffix;
        }
        else
        {
            retVal = pFileName + pSuffix;
        }
        return retVal;
    }
    
    public static String getExtension(String pFileName)
    {
        int lastIndex = pFileName.lastIndexOf('.');
        String retExtension = null;
        if(-1 != lastIndex)
        {
            retExtension = pFileName.substring(lastIndex, pFileName.length());
        }
        return(retExtension);
    }

    public static String removeExtension(String pFileName)
    {
        int lastIndex = pFileName.lastIndexOf('.');
        String retFileName = pFileName;
        if(-1 != lastIndex)
        {
            retFileName = pFileName.substring(0, lastIndex - 1);
        }
        return(retFileName);
    }

    public static String createFileURL(File pFile)
    {
        String fileName = pFile.getAbsolutePath();

        // for non-Unix operating systems, convert path separator first
        fileName = fileName.replace(File.separatorChar, '/');

        if(fileName.charAt(0) != '/')
        {
            fileName = "/" + fileName;
        }

        fileName = fileName.replaceAll("%", "%25");
        fileName = fileName.replaceAll(" ", "%20");
        fileName = fileName.replaceAll("\\:", "%3A");
        fileName = fileName.replaceAll("#", "%23");
        fileName = fileName.replaceAll("\\$", "%24");
        fileName = fileName.replaceAll("&", "%26");
        fileName = fileName.replaceAll("\\?", "%3F");
        fileName = fileName.replaceAll("@", "%40");
        fileName = fileName.replaceAll(";", "%3B");
        fileName = fileName.replaceAll(",", "%2C");
        fileName = fileName.replaceAll("\\+", "%2C");
        fileName = fileName.replaceAll("<", "%3C");
        fileName = fileName.replaceAll(">", "%3E");
        fileName = fileName.replaceAll("\"", "%34");

        return("file:" + fileName);
    }
}
