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
import javax.swing.filechooser.*;
import java.io.File;

public class FileChooser
{
    private Component mMainFrame;
    private JFileChooser mFileChooser;
    private File mSelectedFile;

    protected Component getMainFrame()
    {
        return(mMainFrame);
    }

    void setCurrentDirectory(File pCurrentDirectory)
    {
        mFileChooser.setCurrentDirectory(pCurrentDirectory);
    }

    protected JFileChooser getFileChooser()
    {

        return(mFileChooser);
    }
    
    File getSelectedFile()
    {
        return(mSelectedFile);
    }
    
    void setDialogTitle(String pTitle)
    {
        mFileChooser.setDialogTitle(pTitle);
    }

    public FileChooser(Component pMainFrame)
    {
        JFileChooser fileChooser = new JFileChooser();
        mFileChooser = fileChooser;
        mSelectedFile = null;

        disableDoubleMouseClick(fileChooser);

        mMainFrame = pMainFrame;
    }

    public static boolean handleOutputFileAlreadyExists(Component pFrame, String pOutputFileName)
    {
        boolean proceed = true;
        // need to ask user to confirm whether the file should be overwritten
        SimpleTextArea textArea = new SimpleTextArea("The output file you selected already exists:\n" + pOutputFileName + "\nThis operation will overwrite this file.\nAre you sure you want to proceed?");
        
        SimpleDialog messageDialog = new SimpleDialog(pFrame, 
                                                      "Overwrite existing file?",
                                                      textArea);
        messageDialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
        messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
        messageDialog.show();
        Integer response = (Integer) messageDialog.getValue();
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
        int returnVal = mFileChooser.showOpenDialog(mMainFrame);
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            mSelectedFile = mFileChooser.getSelectedFile();
        }
    }

    void setApproveButtonText(String pApproveButtonText)
    {
        mFileChooser.setApproveButtonText(pApproveButtonText);
    }

    void setSelectedFile(File pSelectedFile)
    {
        mFileChooser.setSelectedFile(pSelectedFile);
    }

    void setFileFilter(FileFilter pFileFilter)
    {
        mFileChooser.setFileFilter(pFileFilter);
    }

    private void disableDoubleMouseClick(Component c) 
    {
      if (c instanceof JList)
      {             
           java.util.EventListener[] listeners=c.getListeners(java.awt.event.MouseListener.class);
           for(int i=0; i<listeners.length; i++) 
           {
               if (listeners[i].toString().indexOf("SingleClickListener") != -1) 
               {
                   c.removeMouseListener((java.awt.event.MouseListener)listeners[i]);
               }
           }
           return; 
       }
       Component[] children = null;
       if (c instanceof Container) 
       {
           children = ((Container)c).getComponents();
       }
       if (children != null) 
       {
           for(int i = 0; i < children.length; i++) 
           {
               disableDoubleMouseClick(children[i]);
           }
       }
   } 
}
