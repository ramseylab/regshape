package org.systemsbiology.gui;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import javax.swing.*;

public class SimpleTextArea extends JTextArea
{
    private static final int NUM_ROWS = 8;
    private static final int NUM_COLS = 40;

    public SimpleTextArea(String pText)
    {
        super(pText, NUM_ROWS, NUM_COLS);
        setLineWrap(true);
        setEditable(false);
        setWrapStyleWord(true);
    }
}
