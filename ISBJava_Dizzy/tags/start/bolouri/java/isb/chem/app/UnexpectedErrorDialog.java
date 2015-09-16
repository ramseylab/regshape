package isb.chem.app;

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
