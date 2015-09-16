/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.math;

/**
 * The requested operation or calculation could not be performed
 * at the requested accuracy.
 * 
 * @author sramsey
 *
 */
public class AccuracyException extends Exception
{
    public AccuracyException(String pMessage)
    {
        super(pMessage);
    }

    public AccuracyException(String pMessage, Throwable pCause)
    {
        super(pMessage, pCause);
    }
}
