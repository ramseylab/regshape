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
import javax.swing.filechooser.*;
import java.io.File;

public class FileChooser extends JFileChooser
{
    private Component mMainFrame;
    private File mSelectedFile;

    protected Component getMainFrame()
    {
        return(mMainFrame);
    }

    private void initialize()
    {
        mSelectedFile = null;
        ComponentUtils.disableDoubleMouseClick(this);
    }
        
    public FileChooser(Component pMainFrame)
    {
        mMainFrame = pMainFrame;

        initialize();
    }

    public static boolean handleOutputFileAlreadyExists(Component pFrame, String pOutputFileName)
    {
        boolean proceed = true;
        // need to ask user to confirm whether the file should be overwritten
        SimpleTextArea textArea = new SimpleTextArea("The output file you selected already exists:\n" + pOutputFileName + "\nThis operation will overwrite this file.\nAre you sure you want to proceed?");
        
        JOptionPane messageOptionPane = new JOptionPane();
        messageOptionPane.setMessage(textArea);
        messageOptionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
        messageOptionPane.setOptionType(JOptionPane.YES_NO_OPTION);
        messageOptionPane.createDialog(pFrame, "Overwrite existing file?").show();
        Integer response = (Integer) messageOptionPane.getValue();
        if(null != response &&
           response.intValue() == JOptionPane.YES_OPTION)
        {
            // do nothing
        }
        else
        {
            proceed = false;
        }
        return(proceed);
    }

    public void show()
    {
        int returnVal = showOpenDialog(mMainFrame);
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            mSelectedFile = getSelectedFile();
        }
    }

}
