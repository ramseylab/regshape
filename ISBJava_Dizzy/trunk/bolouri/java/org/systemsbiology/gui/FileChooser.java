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
import java.io.*;

public class FileChooser extends JFileChooser
{
    public FileChooser()
    {
        ComponentUtils.disableDoubleMouseClick(this);
    }
    
    public FileChooser(File pCurrentDirectory)
    {
        super(pCurrentDirectory);
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


}
