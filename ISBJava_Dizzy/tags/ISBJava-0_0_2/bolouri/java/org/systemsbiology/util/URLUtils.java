package org.systemsbiology.util;

import java.io.File;

/**
 * Utility class for encoding a URL element.  This is a
 * string element that you wish to embed in a URL.  
 *
 * @author Stephen Ramsey
 */
public class URLUtils
{
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
