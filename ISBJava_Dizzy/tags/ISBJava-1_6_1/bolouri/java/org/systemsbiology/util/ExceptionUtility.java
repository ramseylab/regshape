package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

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
