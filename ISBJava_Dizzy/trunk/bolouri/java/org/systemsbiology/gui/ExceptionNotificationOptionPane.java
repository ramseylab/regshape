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
import java.awt.*;
import java.awt.event.*;

import org.systemsbiology.util.*;

public class ExceptionNotificationOptionPane extends JOptionPane
{
    private static final int NUM_ROWS = 16;
    private static final int NUM_COLS = 60;

    private JButton mShowDetailsButton;
    private Throwable mException;
    private JTextArea mTextArea;

    public void handleDetailedButton()
    {
        mShowDetailsButton.setEnabled(false);
        String stackTrace = ExceptionUtility.getStackTrace(mException);
        mTextArea.append("\n\n" + stackTrace);
    }

    public ExceptionNotificationOptionPane(Throwable pException)
    {
        JTextArea textArea = new JTextArea(pException.toString() + "\n\nThis operation is cancelled", 
                                           NUM_ROWS, NUM_COLS);
        mTextArea = textArea;
        mException = pException;
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        setMessage(scrollPane);
        setMessageType(JOptionPane.WARNING_MESSAGE);
        JButton detailedButton = new JButton("show details");
        mShowDetailsButton = detailedButton;
        ActionListener detailedButtonListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleDetailedButton();
            }
        };
        detailedButton.addActionListener(detailedButtonListener);
        add(detailedButton);
    }
}
