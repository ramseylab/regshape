package isb.chem.app;

import isb.chem.scripting.*;
import isb.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import javax.swing.filechooser.*;

public class EditorPane
{
    private static final int EDITOR_TEXT_AREA_NUM_ROWS = 24;
    private static final int EDITOR_TEXT_AREA_NUM_COLS = 80;
    private static final String LABEL_FILE =   "file: ";
    private static final String LABEL_PARSER = "parser: ";

    private Component mMainFrame;
    private JTextArea mEditorPaneTextArea;
    private JButton mProcessFileButton;
    private JLabel mFileNameLabel;
    private JLabel mParserAliasLabel;
    private String mFileName;
    private String mParserAlias;
    private boolean mBufferDirty;

    private String getParserAlias()
    {
        return(mParserAlias);
    }

    private void setParserAlias(String pParserAlias)
    {
        mParserAlias = pParserAlias;
    }

    JTextArea getEditorPaneTextArea()
    {
        return(mEditorPaneTextArea);
    }

    private String getFileName()
    {
        return(mFileName);
    }

    private void setFileName(String pFileName)
    {
        mFileName = pFileName;
    }

    private boolean getBufferDirty()
    {
        return(mBufferDirty);
    }

    private void setBufferDirty(boolean pBufferDirty)
    {
        mBufferDirty = pBufferDirty;
    }

    public EditorPane(Container pPane)
    {
        initialize(pPane);
        mMainFrame = MainApp.getApp().getMainFrame();
        setFileNameLabel(null);
        setParserAlias(null);
        setBufferDirty(false);
    }

    private void handleCancel(String pOperation)
    {
        SimpleDialog messageDialog = new SimpleDialog(mMainFrame, pOperation + " cancelled", 
                                                      "Your " + pOperation + " operation has been cancelled");
        messageDialog.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        messageDialog.show();
    }

    void setProcessMenuEnabled()
    {
        boolean enabled = false;
        if(! editorBufferIsEmpty())
        {
            enabled = true;
        }
        MainApp.getApp().enableProcessMenuItem(enabled);
    }

    public void close()
    {
        boolean doClose = false;
        String fileName = getFileName();
        
        if(getBufferDirty())
        {
            SimpleTextArea textArea = new SimpleTextArea("Changes have been made to this edit buffer that have not yet been saved to the file:\n" + fileName + "\nThe close operation will discard these change.\nAre you sure you want to proceed?");
            SimpleDialog messageDialog = new SimpleDialog(mMainFrame,
                                                          "Discard edits to this file?",
                                                          textArea);
            messageDialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
            messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
            messageDialog.show();
            Integer response = (Integer) messageDialog.getValue();
            if(null != response && response.intValue() == JOptionPane.YES_OPTION)
            {
                doClose = true;
            }
            else
            {
                if(null == response)
                {
                    handleCancel("close");
                }
            }
        }
        else
        {
            doClose = true;
        }

        if(doClose)
        {
            setFileNameLabel(null);
            setBufferDirty(false);
            clearEditorText();
            setProcessMenuEnabled();
            setParserAliasLabel(null);
        }
    }

    public void open()
    {
        FileChooser fileChooser = new FileChooser(mMainFrame);
        FileFilter fileFilter = new ChemFileFilter();
        JFileChooser intFileChooser = fileChooser.getFileChooser();
        intFileChooser.setFileFilter(fileFilter);
        fileChooser.setDialogTitle("Please select a file to open");
        fileChooser.show();
        String fileName = fileChooser.getFileName();
        if(null != fileName)
        {
            File file = new File(fileName);
            if(file.exists())
            {
                if(file.isDirectory())
                {
                    SimpleTextArea textArea = new SimpleTextArea("The file you selected is a directory:\n" + fileName + "\n");
                    SimpleDialog messageDialog = new SimpleDialog(mMainFrame,
                                                                  "Open cancelled",
                                                                  textArea);
                    messageDialog.show();
                }
                else
                {
                    loadFileToEditBuffer(fileName);
                }
            }
            else
            {
                    SimpleTextArea textArea = new SimpleTextArea("The file you selected does not exist:\n" + fileName + "\n");
                    SimpleDialog messageDialog = new SimpleDialog(mMainFrame,
                                                                  "Open cancelled",
                                                                  textArea);
                    messageDialog.show();
                
            }
        }
    }

    public void save()
    {
        saveEditBufferToFile(getFileName());
    }

    public void saveAs()
    {
        FileChooser fileChooser = new FileChooser(mMainFrame);
        FileFilter fileFilter = new ChemFileFilter();
        JFileChooser intFileChooser = fileChooser.getFileChooser();
        intFileChooser.setFileFilter(fileFilter);
        intFileChooser.setApproveButtonText("Save");
        fileChooser.setDialogTitle("Please select a file name to save as");
        fileChooser.show();
        String fileName = fileChooser.getFileName();
        boolean doSave = false;
        if(null != fileName)
        {
            String curFileName = getFileName();

            File file = new File(fileName);
            if(file.exists())
            {
                if(file.isDirectory())
                {
                    SimpleTextArea textArea = new SimpleTextArea("The file you selected is a directory:\n" + fileName + "\n");
                    SimpleDialog messageDialog = new SimpleDialog(mMainFrame,
                                                                  "Save cancelled",
                                                                  textArea);
                    messageDialog.show();
                }
                else
                {
                    if((null != curFileName &&
                       fileName.equals(curFileName)) ||
                        null == curFileName)
                    {
                            SimpleTextArea textArea = new SimpleTextArea("The file you selected already exists:\n" + fileName + "\nThe save operation will overwrite this file.\nAre you sure you want to proceed?");
                            SimpleDialog messageDialog = new SimpleDialog(mMainFrame,
                                                                                "Overwrite existing file?",
                                                                                textArea);
                            messageDialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
                            messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
                            messageDialog.show();
                            Integer response = (Integer) messageDialog.getValue();
                            if(null != response && response.intValue() == JOptionPane.YES_OPTION)
                            {
                                doSave = true;
                            }
                            else
                            {
                                if(null == response)
                                {
                                    handleCancel("save");
                                }
                            }
                    }
                    else
                    {
                        doSave = true;
                    }
                }
            }
            else
            {
                doSave = true;
            }

            if(doSave && null != curFileName && !(curFileName.equals(fileName)))
            {
                setParserAliasLabel(null);
            }
        }

        if(doSave)
        {
            saveEditBufferToFile(fileName);
        }
    }

    private void initialize(Container pMainPane)
    {
        JPanel editorPanel = new JPanel();
        editorPanel.setBorder(BorderFactory.createEtchedBorder());
        editorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        LayoutManager editorLayoutManager = new BoxLayout(editorPanel, BoxLayout.Y_AXIS);
        editorPanel.setLayout(editorLayoutManager);

        JPanel labelPanel = new JPanel();
        labelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        LayoutManager labelLayoutManager = new BoxLayout(labelPanel, BoxLayout.Y_AXIS);
        labelPanel.setLayout(labelLayoutManager);
        JLabel fileLabel = new JLabel(LABEL_FILE + "(none)");
        labelPanel.add(fileLabel);
        mFileNameLabel = fileLabel;

        JLabel parserLabel = new JLabel(LABEL_PARSER + "(none)");
        mParserAliasLabel = parserLabel;
        labelPanel.add(parserLabel);

        editorPanel.add(labelPanel);


        mParserAliasLabel = parserLabel;
        
        JPanel labelEditorPane = new JPanel();
        LayoutManager layoutManager = new FlowLayout();
        labelEditorPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelEditorPane.setLayout(layoutManager);
        editorPanel.add(labelEditorPane);

        initializeLabel(labelEditorPane);
        initializeEditorTextArea(labelEditorPane);

        pMainPane.add(editorPanel);
    }

    private void initializeEditorTextArea(Container pPane)
    {
        KeyListener listener = new KeyListener()
                                     {
                                         public void keyPressed(KeyEvent e)
                                         {
                                             // do nothing
                                         }

                                         public void keyReleased(KeyEvent e)
                                         {
                                             setProcessMenuEnabled();
                                             setBufferDirty(true);
                                         }

                                         public void keyTyped(KeyEvent e)
                                         {
                                             // do nothing
                                         }
                                     };
        JTextArea editorTextArea = new JTextArea(EDITOR_TEXT_AREA_NUM_ROWS,
                                                 EDITOR_TEXT_AREA_NUM_COLS);
        editorTextArea.setEditable(true);
        editorTextArea.addKeyListener(listener);
        mEditorPaneTextArea = editorTextArea;
        JScrollPane scrollPane = new JScrollPane(editorTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pPane.add(scrollPane);
    }

    void clearEditorText()
    {
        JTextArea fileEditorTextArea = getEditorPaneTextArea();
        int textLen = fileEditorTextArea.getText().length();
        fileEditorTextArea.replaceRange(null, 0, textLen);
    }

    public void process()
    {
        String parserAlias = getParserAlias();
        boolean doProcess = false;
        
        assert (! editorBufferIsEmpty()) : "editor buffer was empty";

        if(null == parserAlias)
        {
            ParserPicker parserPicker = new ParserPicker(mMainFrame);
            parserAlias = parserPicker.selectParserAliasFromFileName(getFileName());
            if(null != parserAlias)
            {
                setParserAliasLabel(parserAlias);
                doProcess = true;
            }
            else
            {
                // do nothing, as this case means that the user has cancelled the open operation
            }
        }
        else
        {
            doProcess = true;
        }

        String fileName = getFileName();

        if(doProcess)
        {
            ModelProcessor processor = new ModelProcessor(mMainFrame);
            String modelText = getEditorPaneTextArea().getText();
            StringReader stringReader = new StringReader(modelText);
            BufferedReader bufferedReader = new BufferedReader(stringReader);
            processor.processModel(fileName, bufferedReader, parserAlias);
        }
    }

    private boolean editorBufferIsEmpty()
    {
        return(0 == getEditorPaneTextArea().getText().trim().length());
    }

    private void initializeLabel(Container pPane)
    {
        JPanel labelPanel = new JPanel();
        JLabel label = new JLabel("model definition:");
        labelPanel.add(label);
        pPane.add(labelPanel);
    }

    public void saveEditBufferToFile(String pFileName)
    {
        File file = new File(pFileName);
        String shortFileName = file.getName();
        try
        {
            MainApp theApp = MainApp.getApp();
            JTextArea fileEditorTextArea = getEditorPaneTextArea();
            String fileContents = fileEditorTextArea.getText();
            FileWriter fileWriter = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(fileContents);
            printWriter.flush();
            setBufferDirty(false);
        }

        catch(Exception e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "error saving file: " + shortFileName, e);
            dialog.show();
            return;
        }

        setFileNameLabel(pFileName);
    }

    public void loadFileToEditBuffer(String pFileName)
    {
        File file = new File(pFileName);
        String shortFileName = file.getName();

        try
        {
            StringBuffer fileContents = new StringBuffer();

            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;
            while((line = bufferedReader.readLine()) != null)
            {
                fileContents.append(line);
                fileContents.append("\n");
            }

            MainApp theApp = MainApp.getApp();
            JTextArea fileEditorTextArea = getEditorPaneTextArea();
            StringReader stringReader = new StringReader(fileContents.toString());
            
            clearEditorText();

            bufferedReader = new BufferedReader(stringReader);
            while((line = bufferedReader.readLine()) != null)
            {
                fileEditorTextArea.append(line);
                fileEditorTextArea.append("\n");
            }

            setFileNameLabel(pFileName);
            setProcessMenuEnabled();
            setBufferDirty(false);
        }

        catch(Exception e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "error loading file: " + shortFileName, e);
            dialog.show();
            return;
        }
    }

    private void setParserAliasLabel(String pParserAlias)
    {
        setParserAlias(pParserAlias);
        boolean enableSave = false;
        if(null != pParserAlias)
        {
            mParserAliasLabel.setText(LABEL_PARSER + pParserAlias);
            enableSave = true;
        }
        else
        {
            mParserAliasLabel.setText(LABEL_PARSER + "(none)");
        }
    }

    private void setFileNameLabel(String pFileName)
    {
        setFileName(pFileName);
        boolean enableSave = false;
        if(null != pFileName)
        {
            mFileNameLabel.setText(LABEL_FILE + pFileName);
            enableSave = true;
        }
        else
        {
            mFileNameLabel.setText(LABEL_FILE + "(none)");
        }
        MainApp.getApp().enableSaveMenuItem(enableSave);
        MainApp.getApp().enableCloseMenuItem(enableSave);
    }
}
