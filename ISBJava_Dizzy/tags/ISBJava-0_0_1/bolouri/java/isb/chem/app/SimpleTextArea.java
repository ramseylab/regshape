package isb.chem.app;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SimpleTextArea extends JTextArea
{
    private static final int NUM_ROWS = 8;
    private static final int NUM_COLS = 40;

    public SimpleTextArea(String pText)
    {
        super(pText, NUM_ROWS, NUM_COLS);
        setLineWrap(true);
        setEditable(false);
        setWrapStyleWord(true);
    }
}
