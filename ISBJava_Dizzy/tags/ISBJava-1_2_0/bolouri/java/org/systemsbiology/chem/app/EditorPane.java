package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.chem.*;
import org.systemsbiology.chem.scripting.*;
import org.systemsbiology.util.*;
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
    private static final int EDITOR_TEXT_AREA_NUM_ROWS = 40;
    private static final int EDITOR_TEXT_AREA_NUM_COLS = 80;
    private static final int SCROLL_PANE_OFFSET_PIXELS = 20;

    private static final int EDITOR_TEXT_AREA_MIN_WIDTH_CHARS = 4;
    private static final int EDITOR_TEXT_AREA_MIN_HEIGHT_CHARS = 2;
    static final long TIMESTAMP_BUFFER_LAST_CHANGE_NULL = -1;

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
    private File mCurrentDirectory;
    private int mOriginalWidthPixels;
    private int mOriginalHeightPixels;
    private long mTimestampLastChange;
    private MainApp mMainApp;
    private JScrollPane mEditorScrollPane;

    interface EditorStateUpdater
    {
        public void updateEditorState();
    }

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

    String getFileName()
    {
        return(mFileName);
    }

    private void setFileName(String pFileName)
    {
        mFileName = pFileName;
    }

    boolean getBufferDirty()
    {
        return(mBufferDirty);
    }

    private void setBufferDirty(boolean pBufferDirty)
    {
        mBufferDirty = pBufferDirty;
    }

    private void setCurrentDirectory(File pCurrentDirectory)
    {
        mCurrentDirectory = pCurrentDirectory;
    }

    private File getCurrentDirectory()
    {
        return(mCurrentDirectory);
    }

    private void setTimestampLastChange(long pTimestampLastChange)
    {
        mTimestampLastChange = pTimestampLastChange;
    }

    long getTimestampLastChange()
    {
        return(mTimestampLastChange);
    }

    public EditorPane(Container pPane)
    {
        initialize(pPane);
        mMainApp = MainApp.getApp();
        mMainFrame = mMainApp.getMainFrame();
        setFileNameLabel(null);
        setParserAlias(null);
        setBufferDirty(false);
        setTimestampLastChange(TIMESTAMP_BUFFER_LAST_CHANGE_NULL);
        setCurrentDirectory(null);
    }

    private void handleCancel(String pOperation)
    {
        SimpleDialog messageDialog = new SimpleDialog(mMainFrame, pOperation + " cancelled", 
                                                      "Your " + pOperation + " operation has been cancelled");
        messageDialog.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        messageDialog.show();
    }

    public void handleCut()
    {
        JTextArea textArea = getEditorPaneTextArea();
        textArea.cut();
    }

    public void handlePaste()
    {
        JTextArea textArea = getEditorPaneTextArea();
        textArea.paste();
    }
    
    public void handleCopy()
    {
        JTextArea textArea = getEditorPaneTextArea();
        textArea.copy();
    }
    
    public void handleResize(int changeWidthPixels, int changeHeightPixels)
    {
        if(0 == mOriginalWidthPixels)
        {
            mOriginalWidthPixels = mEditorPaneTextArea.getWidth();
            mOriginalHeightPixels = mEditorPaneTextArea.getHeight();
        }
        int originalWidthPixels = mOriginalWidthPixels;
        int originalHeightPixels = mOriginalHeightPixels;
        int widthPixels = originalWidthPixels + changeWidthPixels;
        int heightPixels = originalHeightPixels + changeHeightPixels;
        if(widthPixels > 0 && heightPixels > 0)
        {
            int widthChars = (int) Math.floor( ((double) widthPixels)/((double) originalWidthPixels) * 
                                     ((double) EDITOR_TEXT_AREA_NUM_COLS));

            int heightChars = (int) Math.floor( ((double) heightPixels)/((double) originalHeightPixels) * 
                                     ((double) EDITOR_TEXT_AREA_NUM_ROWS));
            if(widthChars < EDITOR_TEXT_AREA_MIN_WIDTH_CHARS)
            {
                widthChars = EDITOR_TEXT_AREA_MIN_WIDTH_CHARS;
            }
            if(heightChars < EDITOR_TEXT_AREA_MIN_HEIGHT_CHARS)
            {
                heightChars = EDITOR_TEXT_AREA_MIN_HEIGHT_CHARS;
            }

            mEditorPaneTextArea.setRows(heightChars);
            mEditorPaneTextArea.setColumns(widthChars);
            mEditorScrollPane.setPreferredSize(new Dimension(widthPixels + SCROLL_PANE_OFFSET_PIXELS, 
                                                             heightPixels + SCROLL_PANE_OFFSET_PIXELS));
            mEditorPaneTextArea.revalidate();
            mEditorScrollPane.revalidate();
            mEditorScrollPane.repaint();
        }
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
            setTimestampLastChange(TIMESTAMP_BUFFER_LAST_CHANGE_NULL);
            clearEditorText();
            mMainApp.updateMenus();
            setParserAliasLabel(null);
        }
    }

    public void open()
    {
        FileChooser fileChooser = new FileChooser(mMainFrame);
        FileFilter fileFilter = new ChemFileFilter();
        JFileChooser intFileChooser = fileChooser.getFileChooser();
        File currentDirectory = getCurrentDirectory();
        if(null != currentDirectory)
        {
            fileChooser.setCurrentDirectory(currentDirectory);
        }
        intFileChooser.setFileFilter(fileFilter);
        fileChooser.setDialogTitle("Please select a file to open");
        fileChooser.show();
        File inputFile = fileChooser.getSelectedFile();
        if(null != inputFile)
        {
            String fileName = inputFile.getAbsolutePath();
            if(inputFile.exists())
            {
                if(inputFile.isDirectory())
                {
                    SimpleTextArea textArea = new SimpleTextArea("The file you selected is a directory:\n" + fileName + "\n");
                    SimpleDialog messageDialog = new SimpleDialog(mMainFrame,
                                                                  "Open cancelled",
                                                                  textArea);
                    messageDialog.show();
                }
                else
                {
                    File parentFile = inputFile.getParentFile();
                    if(parentFile.isDirectory())
                    {
                        setCurrentDirectory(parentFile);
                    }
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
        mMainApp.updateMenus();
    }

    public void saveAs()
    {
        FileChooser fileChooser = new FileChooser(mMainFrame);
        FileFilter fileFilter = new ChemFileFilter();
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setApproveButtonText("Save");
        fileChooser.setDialogTitle("Please select a file name to save as");
        fileChooser.show();
        String curFileName = getFileName();
        if(null != curFileName)
        {
            File curFile = new File(curFileName);
            if(curFile.exists())
            {
                fileChooser.setSelectedFile(curFile);
            }
        }
        File outputFile = fileChooser.getSelectedFile();
        boolean doSave = false;
        String fileName = null;
        if(null != outputFile)
        {
            fileName = outputFile.getAbsolutePath();
            if(outputFile.exists())
            {
                if(outputFile.isDirectory())
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
            mMainApp.updateMenus();
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
                                             // do nothing
                                         }

                                         public void keyTyped(KeyEvent e)
                                         {
                                             if(! getBufferDirty())
                                             {
                                                 setBufferDirty(true);
                                                 
                                             }
                                             setTimestampLastChange(System.currentTimeMillis());
                                             mMainApp.updateMenus();
                                         }
                                     };
        JTextArea editorTextArea = new JTextArea(EDITOR_TEXT_AREA_NUM_ROWS,
                                                 EDITOR_TEXT_AREA_NUM_COLS);
        editorTextArea.setEditable(true);
        editorTextArea.addKeyListener(listener);
        mEditorPaneTextArea = editorTextArea;
        
        JScrollPane scrollPane = new JScrollPane(editorTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        mEditorScrollPane = scrollPane;

        mOriginalWidthPixels = 0;
        mOriginalHeightPixels = 0;

        pPane.add(scrollPane);
    }

    void clearEditorText()
    {
        JTextArea fileEditorTextArea = getEditorPaneTextArea();
        int textLen = fileEditorTextArea.getText().length();
        fileEditorTextArea.replaceRange(null, 0, textLen);
    }

    public Model processModel()
    {
        String parserAlias = getParserAlias();
        boolean doProcess = false;
        
        assert (! editorBufferIsEmpty()) : "editor buffer was empty";

        if(null == parserAlias)
        {
            String fileName = getFileName();
            if(null == fileName)
            {
                // user has not set file name yet; ask for parser alias
                fileName = "";
            }
            ParserPicker parserPicker = new ParserPicker(mMainFrame);
            parserAlias = parserPicker.selectParserAliasFromFileName(fileName);
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
        
        Model model = null;

        if(doProcess)
        {
            ModelProcessor processor = new ModelProcessor(mMainFrame);
            String modelText = getEditorPaneTextArea().getText();
            StringReader stringReader = new StringReader(modelText);
            BufferedReader bufferedReader = new BufferedReader(stringReader);
            model = processor.processModel(fileName, bufferedReader, parserAlias);
        }

        return(model);
    }

    boolean editorBufferIsEmpty()
    {
        return(0 == getEditorPaneTextArea().getText().length());
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
        close();

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
            setBufferDirty(false);
            setTimestampLastChange(System.currentTimeMillis());
            mMainApp.updateMenus();
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
        if(null != pParserAlias)
        {
            mParserAliasLabel.setText(LABEL_PARSER + pParserAlias);
        }
        else
        {
            mParserAliasLabel.setText(LABEL_PARSER + "(none)");
        }
    }

    private void setFileNameLabel(String pFileName)
    {
        setFileName(pFileName);
        if(null != pFileName)
        {
            mFileNameLabel.setText(LABEL_FILE + pFileName);
        }
        else
        {
            mFileNameLabel.setText(LABEL_FILE + "(none)");
        }
    }
}
