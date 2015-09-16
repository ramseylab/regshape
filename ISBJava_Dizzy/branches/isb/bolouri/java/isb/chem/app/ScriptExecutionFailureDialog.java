package isb.chem.app;

import java.io.File;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ScriptExecutionFailureDialog 
{
    private JOptionPane mPane;
    private Component mParent;
    static final String OPTION_RESTART = "restart runtime";
    static final String OPTION_EXIT = "exit program";
    private static final String []OPTIONS = {OPTION_RESTART, OPTION_EXIT};
    private static final int NUM_ROWS = 16;
    private static final int NUM_COLS = 60;

    public ScriptExecutionFailureDialog(Component pParent, Exception pException)
    {

        mParent = pParent;
        JTextArea textArea = new JTextArea("The script runtime was unable to process the input you provided.\n\n" +
                                           "The specific error message is:\n" + pException.toString() + 
                                           "\n\nThis operation is cancelled.  At this point you have two options:\n" +
                                           "(1) you can exit the program\n" + 
                                           "(2) you can restart the script runtime, which clears all saved variables",
                                           NUM_ROWS, NUM_COLS);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);

        mPane = new JOptionPane(textArea,
                                JOptionPane.WARNING_MESSAGE,
                                JOptionPane.DEFAULT_OPTION,
                                null,
                                OPTIONS);
    }

    String getValue()
    {
        return((String) mPane.getValue());
    }

    public void show()
    {
        JDialog dialog = mPane.createDialog(mParent, "An error occurred in the script runtime");
        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.show();
    }
}
