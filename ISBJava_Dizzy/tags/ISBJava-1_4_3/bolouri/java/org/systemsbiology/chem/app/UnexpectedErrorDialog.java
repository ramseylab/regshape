package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class UnexpectedErrorDialog extends SimpleDialog
{
    private static final String DIALOG_TITLE = "an unexpected error has occurred";

    public UnexpectedErrorDialog(Component pFrame, Object pMessage)
    {
        super(pFrame, DIALOG_TITLE, pMessage);
        setMessageType(JOptionPane.ERROR_MESSAGE);
    }
}
