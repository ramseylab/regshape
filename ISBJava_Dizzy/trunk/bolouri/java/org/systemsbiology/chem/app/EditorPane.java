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
import org.systemsbiology.gui.*;
import org.systemsbiology.util.*;

import java.awt.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.*;
import java.io.*;

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
    private static final String LABEL_LINE = "line: ";
    
    private Component mMainFrame;
    private JTextArea mEditorPaneTextArea;
    private JButton mProcessFileButton;
    private JLabel mFileNameLabel;
    private JLabel mParserAliasLabel;
    private String mFileName;
    private String mParserAlias;
    private boolean mBufferDirty;
    private int mOriginalWidthPixels;
    private int mOriginalHeightPixels;
    private long mTimestampLastChange;
    private MainApp mMainApp;
    private JScrollPane mEditorScrollPane;
    private File mCurrentDirectory;
    private IModelBuilder mModelBuilder;
    private JLabel mLineNumberLabel;
    
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
        setCurrentDirectory(mMainApp.getCurrentDirectory());
        setTimestampLastChange(TIMESTAMP_BUFFER_LAST_CHANGE_NULL);
    }

    private void setCurrentDirectory(File pCurrentDirectory)
    {
        mCurrentDirectory = pCurrentDirectory;
    }

    private File getCurrentDirectory()
    {
        return(mCurrentDirectory);
    }

    private void setModelBuilder(IModelBuilder pModelBuilder)
    {
        mModelBuilder = pModelBuilder;
    }
    
    private IModelBuilder getModelBuilder()
    {
        return(mModelBuilder);
    }
    
    private void handleCancel(String pOperation)
    {
        JOptionPane.showMessageDialog(mMainFrame,
                                      "Your " + pOperation + " operation has been cancelled",
                                      pOperation + " cancelled",
                                      JOptionPane.INFORMATION_MESSAGE);
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

    public boolean close()
    {
        boolean doClose = false;
        String fileName = getFileName();
        
        if(getBufferDirty())
        {
            SimpleTextArea textArea = new SimpleTextArea("Changes have been made to this edit buffer that have not yet been saved to the file:\n" + fileName + "\nThe close operation will discard these change.\nAre you sure you want to proceed?");
            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage(textArea);
            optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
            optionPane.setOptionType(JOptionPane.YES_NO_OPTION);

            optionPane.createDialog(mMainFrame, "Discard edits to this file?").show();
            Integer response = (Integer) optionPane.getValue();
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
            setTimestampLastChange(TIMESTAMP_BUFFER_LAST_CHANGE_NULL);
            clearEditorText();
            mMainApp.updateMenus();
            setParserAliasLabel(null);
            setBufferDirty(false);
        }
        return(doClose);
    }

    public void open()
    {
        FileChooser fileChooser = new FileChooser();
        javax.swing.filechooser.FileFilter fileFilter = new ChemFileFilter();
        File currentDirectory = getCurrentDirectory();
        if(null != currentDirectory)
        {
            fileChooser.setCurrentDirectory(currentDirectory);
        }
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Please select a file to open");
        int result = fileChooser.showOpenDialog(mMainFrame);
        if(JFileChooser.APPROVE_OPTION == result)
        {
            File inputFile = fileChooser.getSelectedFile();
            String fileName = inputFile.getAbsolutePath();
            if(inputFile.exists())
            {
                File parentFile = inputFile.getParentFile();
                if(parentFile.isDirectory())
                {
                    setCurrentDirectory(parentFile);
                    mMainApp.setCurrentDirectory(parentFile);
                }
                loadFileToEditBuffer(fileName);
            }   
            else
            {
                SimpleTextArea textArea = new SimpleTextArea("The file you selected does not exist:\n" + fileName + "\n");
                JOptionPane.showMessageDialog(mMainFrame,
                                              textArea,
                                              "Open cancelled",
                                              JOptionPane.INFORMATION_MESSAGE);                
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
        FileChooser fileChooser = new FileChooser();
        javax.swing.filechooser.FileFilter fileFilter = new ChemFileFilter();
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setApproveButtonText("Save");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Please select a file name to save as");
        String curFileName = getFileName();
        File curFile = null;
        if(null != curFileName)
        {
            curFile = new File(curFileName);
            if(curFile.exists())
            {
                fileChooser.setSelectedFile(curFile);
            }
        }
        int result = fileChooser.showSaveDialog(mMainFrame);
                if(JFileChooser.APPROVE_OPTION == result)
        {
            boolean doSave = false;
            File outputFile = fileChooser.getSelectedFile();
            String fileName = outputFile.getAbsolutePath();
            if(outputFile.exists())
            {
                if(null == curFile ||
                   curFileName.equals(fileName))
                {
                    doSave = true;
                }
                else
                {
                    SimpleTextArea textArea = new SimpleTextArea("The file you selected already exists:\n" + fileName + "\nThe save operation will overwrite this file.\nAre you sure you want to proceed?");
                    JOptionPane optionPane = new JOptionPane();
                    optionPane.setMessage(textArea);
                    optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
                    optionPane.setOptionType(JOptionPane.YES_NO_OPTION);
                    optionPane.createDialog(mMainFrame, "Overwrite existing file?").show();
                    Integer response = (Integer) optionPane.getValue();
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
            }
            else
            {
                doSave = true;
            }
            
            if(doSave && null != curFileName && !(curFileName.equals(fileName)))
            {
                setParserAliasLabel(null);
            }

            if(doSave)
            {
                saveEditBufferToFile(fileName);
                mMainApp.updateMenus();
            }        
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
        Font plainFont = fileLabel.getFont().deriveFont(Font.PLAIN);
        labelPanel.add(fileLabel);
        fileLabel.setFont(plainFont);
        mFileNameLabel = fileLabel;

        JLabel parserLabel = new JLabel(LABEL_PARSER + "(none)");
        parserLabel.setFont(plainFont);
        mParserAliasLabel = parserLabel;
        labelPanel.add(parserLabel);

        editorPanel.add(labelPanel);

        mParserAliasLabel = parserLabel;
        
        JPanel labelEditorPane = new JPanel();
        LayoutManager layoutManager = new FlowLayout();
        labelEditorPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelEditorPane.setLayout(layoutManager);
        editorPanel.add(labelEditorPane);

        JLabel lineNumberLabel = new JLabel(LABEL_LINE + "???");
        mLineNumberLabel = lineNumberLabel;
        editorPanel.add(lineNumberLabel);
        lineNumberLabel.setFont(plainFont);
        
        initializeEditorTextArea(labelEditorPane);

        handleCaretPositionChange();
        pMainPane.add(editorPanel);
    }

    private void handleEditorDocumentChange(DocumentEvent e)
    {
        if(! getBufferDirty())
        {
            setBufferDirty(true);
        }
        setTimestampLastChange(System.currentTimeMillis());
        mMainApp.updateMenus();
    }

    private void handleCaretPositionChange()
    {
        try
        {
            int linePosition = mEditorPaneTextArea.getLineOfOffset(mEditorPaneTextArea.getCaretPosition()) + 1;
            mLineNumberLabel.setText(LABEL_LINE + linePosition);
        }
        catch(BadLocationException e)
        {
            mLineNumberLabel.setText(LABEL_LINE + "???");
        }
    }
    
    private void initializeEditorTextArea(Container pPane)
    {
        DocumentListener listener = new DocumentListener()
        {
            public void changedUpdate(DocumentEvent e)
            {
                handleEditorDocumentChange(e);
            }

            public void insertUpdate(DocumentEvent e)
            {
                handleEditorDocumentChange(e);
            }

            public void removeUpdate(DocumentEvent e)
            {
                handleEditorDocumentChange(e);
            }
        };

        JTextArea editorTextArea = new JTextArea(EDITOR_TEXT_AREA_NUM_ROWS,
                                                 EDITOR_TEXT_AREA_NUM_COLS);
        editorTextArea.setEditable(true);
        editorTextArea.getDocument().addDocumentListener(listener);
        editorTextArea.addCaretListener(new CaretListener()
                {
            public void caretUpdate(CaretEvent e)
            {
                handleCaretPositionChange();
            }
                });
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
        Model model = null;
        try
        {
            IModelBuilder modelBuilder = getModelBuilder();
            assert (null != modelBuilder) : "null model builder";
            
            String modelText = getEditorPaneTextArea().getText();
            byte []bytes = modelText.getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            
            String fileName = getFileName();
            File file = new File(fileName);
            File dir = file.getParentFile();
            IncludeHandler includeHandler = new IncludeHandler();
            includeHandler.setDirectory(mCurrentDirectory);

            model = modelBuilder.buildModel(inputStream, includeHandler);
        }
        
        catch(InvalidInputException e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mMainFrame, "error in model definition").show();
            return(model);
        }

        catch(IOException e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mMainFrame,
                                    "I/O error in processing model definition").show();
            return(model);
        }

        return model;
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
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mMainFrame, "error saving file: " + shortFileName).show();
            return;
        }

        setFileNameLabel(pFileName);
    }

    public void loadFileToEditBuffer(String pFileName)
    {
        boolean didClose = close();
        if(!didClose)
        {
            return;
        }

        File file = new File(pFileName);
        String shortFileName = file.getName();
            
        try
        {
            ParserPicker parserPicker = new ParserPicker(mMainFrame);
            String parserAlias = parserPicker.selectParserAliasFromFileName(pFileName);

            if(null != parserAlias)
            {
                ClassRegistry modelBuilderRegistry = mMainApp.getModelBuilderRegistry();
                IModelBuilder modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(parserAlias);
                setModelBuilder(modelBuilder);
                InputStream inputStream = new FileInputStream(file);
                BufferedReader bufferedReader = modelBuilder.getBufferedReader(inputStream);
                
                StringBuffer fileContents = new StringBuffer();

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
                setParserAliasLabel(parserAlias);
                setBufferDirty(false);
                setTimestampLastChange(System.currentTimeMillis());
                mMainApp.updateMenus();
            }
        }

        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mMainFrame, "error loading file: " + shortFileName).show();
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
