package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * Represents an error condition in which the caller has requested
 * a particular data element, but the data element was not found.
 *
 * @author Stephen Ramsey 
 */
public class DataNotFoundException extends Exception
{
    public DataNotFoundException(String pMessage)
    {
        super(pMessage);
    }

    public DataNotFoundException(String pMessage, Throwable pCause)
    {
        super(pMessage, pCause);
    }
}
