package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import isb.util.*;

public class ExceptionDialogOperationCancelled extends SimpleDialog
{
    private static final int NUM_ROWS = 16;
    private static final int NUM_COLS = 60;

    private JButton mShowDetailsButton;
    private Exception mException;
    private JTextArea mTextArea;

    public void handleDetailedButton()
    {
        mShowDetailsButton.setEnabled(false);
        String stackTrace = ExceptionUtility.getStackTrace(mException);
        mTextArea.append("\n\n" + stackTrace);
    }

    public ExceptionDialogOperationCancelled(Component pMainFrame, String pErrorTitle, Exception pException)
    {
        super(pMainFrame, pErrorTitle, null);
        mException = pException;
        JTextArea textArea = new JTextArea(pException.toString() + "\n\nThis operation is cancelled", 
                                           NUM_ROWS, NUM_COLS);
        mTextArea = textArea;
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        mPane.setMessage(scrollPane);
        mPane.setMessageType(JOptionPane.WARNING_MESSAGE);
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
        mPane.add(detailedButton);
    }
}
