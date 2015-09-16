package isb.chem.app;

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
