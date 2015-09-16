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
    private String mFileName;

    protected Component getMainFrame()
    {
        return(mMainFrame);
    }

    protected JFileChooser getFileChooser()
    {

        return(mFileChooser);
    }

    String getFileName()
    {
        return(mFileName);
    }

    protected void setFileName(String pFileName)
    {
        mFileName = pFileName;
    }

    void setDialogTitle(String pTitle)
    {
        mFileChooser.setDialogTitle(pTitle);
    }

    public FileChooser(Component pMainFrame)
    {
        JFileChooser fileChooser = new JFileChooser();
        mFileChooser = fileChooser;

        disableDoubleMouseClick(fileChooser);

        mMainFrame = pMainFrame;
        setFileName(null);
    }

    public void show()
    {
        int returnVal = mFileChooser.showOpenDialog(mMainFrame);
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            File inputFile = mFileChooser.getSelectedFile();
            setFileName(inputFile.getAbsolutePath());
        }
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
