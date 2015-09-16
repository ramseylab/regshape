package org.systemsbiology.util;

import java.io.*;

/**
 * Provides utility methods related to handling exceptions.
 *
 * @author Stephen Ramsey
 */
public class ExceptionUtility
{
    /**
     * Returns the stack backtrace of an exception, as
     * a string.
     *
     * @param the exception whose stack backtrace is to
     * be returned
     *
     * @return the stack backtrace of an exception, as
     * a string. 
     */
    public static String getStackTrace(Exception pException)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        pException.printStackTrace(printWriter);
        return(stringWriter.toString());
    }
}
