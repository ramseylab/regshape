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

public class SimpleDialog
{
    protected JOptionPane mPane;
    private Component mMainFrame;
    private String mErrorTitle;
    private boolean mAllowClose;

    public SimpleDialog(Component pMainFrame, String pErrorTitle, Object pMessage)
    {
        mMainFrame = pMainFrame;
        mErrorTitle = pErrorTitle;
        mPane = new JOptionPane(pMessage);
        mAllowClose = true;
    }

    public void setAllowClose(boolean pAllowClose)
    {
        mAllowClose = pAllowClose;
    }

    public void setOptionType(int pOptionType)
    {
        mPane.setOptionType(pOptionType);
    }

    public void setMessageType(int pMessageType)
    {
        mPane.setMessageType(pMessageType);
    }

    public void setIcon(Icon pIcon)
    {
        mPane.setIcon(pIcon);
    }

    public Object getValue()
    {
        return(mPane.getValue());
    }

    public void show()
    {
        JDialog dialog = mPane.createDialog(mMainFrame, mErrorTitle);
        if(! mAllowClose)
        {
            dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        }
        dialog.show();
    }
}
