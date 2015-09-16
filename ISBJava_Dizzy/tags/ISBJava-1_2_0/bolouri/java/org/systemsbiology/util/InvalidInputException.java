package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

public class InvalidInputException extends Exception
{
    public InvalidInputException(String pMessage)
    {
        super(pMessage);
    }

    public InvalidInputException(String pMessage, Throwable pCause)
    {
        super(pMessage, pCause);
    }
}
