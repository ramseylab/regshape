package isb.chem.app;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RuntimeOutputPane
{
    private static final int OUTPUT_TEXT_AREA_NUM_ROWS = 12;
    private static final int OUTPUT_TEXT_AREA_NUM_COLS = 80;

    private StringWriter mRuntimeOutputLog;
    private JTextArea mOutputTextArea;
    private JButton mClearOutputButton;
    private Container mMainPane;

    public RuntimeOutputPane(Container pMainPane)
    {
        mMainPane = pMainPane;
        setRuntimeOutputLog(new StringWriter());
        initialize(pMainPane);
    }

    private void setRuntimeOutputLog(StringWriter pRuntimeOutputLog)
    {
        mRuntimeOutputLog = pRuntimeOutputLog;
    }

    void appendToOutputLog(String pText)
    {
        getOutputTextArea().append(pText);
    }

    StringWriter getRuntimeOutputLog()
    {
        return(mRuntimeOutputLog);
    }


    private void setOutputTextArea(JTextArea pOutputTextArea)
    {
        mOutputTextArea = pOutputTextArea;
    }

    JTextArea getOutputTextArea()
    {
        return(mOutputTextArea);
    }

    void clearRuntimeOutputLog()
    {
        StringWriter runtimeOutputLog = getRuntimeOutputLog();
        StringBuffer buffer = runtimeOutputLog.getBuffer();
        buffer.delete(0, buffer.toString().length());
    }

    void updateOutputText()
    {
        StringWriter runtimeOutputLog = getRuntimeOutputLog();
        String newOutputText = runtimeOutputLog.toString();
        getOutputTextArea().append(newOutputText);
        clearRuntimeOutputLog();
    }

    void clearOutputText()
    {
        int textLen = getOutputTextArea().getText().length();
        getOutputTextArea().replaceRange(null, 0, textLen);
        clearRuntimeOutputLog();
    }


    void setEnableClearOutputLog(boolean pEnable)
    {
        mClearOutputButton.setEnabled(pEnable);
    }

    private void initialize(Container pMainPane)
    {
        JPanel outputTextPane = new JPanel();
        outputTextPane.setBorder(BorderFactory.createEtchedBorder());
        outputTextPane.setLayout(new BoxLayout(outputTextPane, BoxLayout.Y_AXIS));
        
        pMainPane.add(outputTextPane);


        JPanel outputTextAreaPane = new JPanel();
        outputTextPane.add(outputTextAreaPane);
        JLabel outputTextLabel = new JLabel("runtime output log:");
        outputTextAreaPane.add(outputTextLabel);

        JTextArea outputTextArea = new JTextArea(OUTPUT_TEXT_AREA_NUM_ROWS,
                                                 OUTPUT_TEXT_AREA_NUM_COLS);
        outputTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outputTextAreaPane.add(scrollPane);
        setOutputTextArea(outputTextArea);

        JPanel buttonPane = new JPanel();
        JButton clearButton = new JButton("clear runtime output log");
        buttonPane.add(clearButton);
        mClearOutputButton = clearButton;
        clearButton.addActionListener( new ActionListener()
                                      {
                                          public void actionPerformed(ActionEvent e)
                                          {
                                              clearOutputText();
                                          }
                                      });
        outputTextPane.add(buttonPane);
    }
}
