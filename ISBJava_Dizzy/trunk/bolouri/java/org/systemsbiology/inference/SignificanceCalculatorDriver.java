/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.inference;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.table.AbstractTableModel;
import org.systemsbiology.data.DataFileDelimiter;
import org.systemsbiology.gui.*;
import org.systemsbiology.util.InvalidInputException;
import org.systemsbiology.math.ScientificNumberFormat;
import org.systemsbiology.math.SignificantDigitsCalculator;
import org.systemsbiology.data.MatrixString;

/**
 * @author sramsey
 *
 */
public class SignificanceCalculatorDriver
{
    private Component mParent;
    private Container mContentPane;
    private ScientificNumberFormat mNumberFormat;
    
    private static final String TOOL_TIP_FILE_LOAD_BUTTON = "Load the file of normalized observations.  The first column should be element names.  The column headers (first row) should be evidence names.";
    private static final String TOOL_TIP_FILE_CLEAR_BUTTON = "Clear the file of normalized observations that was loaded into this form.";
    private static final String TOOL_TIP_DELIMITER = "Indicates the type of separator that is used in the data file you want to load.";
    private static final String TOOL_TIP_HELP = "Display the help screen that explains how to use this program";
    private static final String RESOURCE_HELP_ICON = "Help24.gif";
    private static final String TOOL_TIP_EVIDENCE_CHOICES_TABLE = "Select a row and click on the \"set file\" button to modify the negative control file";
    private static final String TOOL_TIP_OBSERVATIONS_TABLE = "This is the table of observations that you loaded from the data file.";
    private static final String TOOL_TIP_SET_NEGATIVE_CONTROL_DATA = "Select the file containing the negative control data for this evidence type.";
    private static final String TOOL_TIP_CLEAR_NEGATIVE_CONTROL_DATA = "Clear the negative control data for this evidence type.";
    
    private static final int NUM_COLUMNS_TEXT_FIELD_NUMERIC = 15;
    private static final int NUM_COLUMNS_TEXT_FIELD_FILE_NAME = 30;
    
    private JTable mEvidenceChoicesTable;
    private JButton mSetNegativeControlFileButton;
    private JButton mClearNegativeControlFileButton;
    
    class NegativeControlData
    {
        public Double []mValues;
        public int mColumn;
        public File mFile;
        
        public NegativeControlData(File pFile, int pColumn, Double []pValues)
        {
            mFile = pFile;
            mColumn = pColumn;
            mValues = pValues;
        }
    }
    
    class EvidenceChoicesTableModel extends AbstractTableModel
    {
        private ObservationsData mObservations;
        private NegativeControlData []mNegativeControlData;
        private Boolean []mSingleTailed;
        
        public EvidenceChoicesTableModel(ObservationsData pObservations)
        {
            mObservations = pObservations;
            int numEvidences = pObservations.getNumEvidences();
            mNegativeControlData = new NegativeControlData[numEvidences];
            mSingleTailed = new Boolean[numEvidences];
            for(int j = 0; j < numEvidences; ++j)
            {
                mSingleTailed[j] = new Boolean(false);
            }
        }
        
        public Class getColumnClass(int pColumn)
        {
            return getValueAt(0, pColumn).getClass();
        }
        
        public NegativeControlData getNegativeControlData(int pRow)
        {
            return mNegativeControlData[pRow];
        }
        
        public void setNegativeControlData(int pRow, NegativeControlData pData)
        {
            mNegativeControlData[pRow] = pData;
        }
        
        public int getColumnCount()
        {
            return 3;
        }
        
        public int getRowCount()
        {
            return mObservations.getNumEvidences();
        }
        
        public void setValueAt(Object pValue, int pRow, int pColumn)
        {
            if(pColumn == 2)
            {
                mSingleTailed[pRow] = (Boolean) pValue;
            }
        }
        
        public boolean isEditable(int pRow, int pColumn)
        {
            if(pColumn == 2)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        
        public String getColumnName(int pColumn)
        {
            switch(pColumn)
            {
            case 0:
                return "evidence name";
                
            case 1:
                return "negative control data source";
                
            case 2:
                return "single-tailed";
                
            default:
                throw new IllegalStateException("unexpected column number: " + pColumn);
            }
        }
        
        public Object getValueAt(int pRow, int pColumn)
        {
            switch(pColumn)
            {
            case 0:
                return mObservations.getEvidenceName(pRow);
                
            case 1:
                NegativeControlData negativeControlData = mNegativeControlData[pRow];
                String retVal = null;
                if(null != negativeControlData)
                {
                    String fileName = negativeControlData.mFile.getAbsolutePath();
                    int column = negativeControlData.mColumn;
                    retVal = fileName + "; column: " + column;
                }
                else
                {
                    retVal = "<none>";
                }
                return retVal;
                
            case 2:
                return mSingleTailed[pRow];
                
            default:
                throw new IllegalStateException("unexpected column number: " + pColumn);
            }
        }
    }
    
    class ObservationsTableModel extends AbstractTableModel
    {
        private ObservationsData mObservationsData;
        
        public ObservationsTableModel(ObservationsData pObservationsData)
        {
            mObservationsData = pObservationsData;
        }
        
        public int getRowCount()
        {
            return mObservationsData.getNumElements();
        }
        
        public int getColumnCount()
        {
            return mObservationsData.getNumEvidences() + 1;
        }
        
        public String getColumnName(int pColumn)
        {
            if(pColumn == 0)
            {
                return "element";
            }
            else
            {
                return mObservationsData.getEvidenceName(pColumn - 1);
            }
        }
        
        public Object getValueAt(int pRow, int pColumn)
        {
            Object retObj = null;
            
            if(pColumn == 0)
            {
                retObj = mObservationsData.getElementName(pRow);
            }
            else
            {
                Double val = mObservationsData.getValueAt(pRow, pColumn - 1);
                if(null != val)
                {
                    retObj = mNumberFormat.format(val.doubleValue());
                }
                else
                {
                    retObj = null;
                }
            }
            return retObj;
        }
    }
    
    
    public void initialize(Container pContentPane, Component pParent)
    {
        mContentPane = pContentPane;
        mParent = pParent;
        mEmptyTableModel = new EmptyTableModel();
        mWorkingDirectory = null;
        mObservationsData = null;
        mNumberFormat = new ScientificNumberFormat(new SignificantDigitsCalculator());
        initializeContentPane();
    }
    
    private ObservationsData mObservationsData;
    private JTable mObservationsTable;
    private JButton mFileLoadButton;
    private JButton mFileClearButton;
    private JLabel mDelimiterLabel;
    private JComboBox mDelimiterBox;
    private JLabel mFileNameLabel;
    private EmptyTableModel mEmptyTableModel;
    private File mWorkingDirectory;
    
    private void setEnableStateForFields(boolean pFileLoaded, boolean pResultsObtained)
    {
        setEnableStateForNegativeControlButtons();
        mFileLoadButton.setEnabled(! pFileLoaded);
        mFileClearButton.setEnabled(pFileLoaded);
        if(pFileLoaded)
        {
            mEvidenceChoicesTable.setToolTipText(TOOL_TIP_EVIDENCE_CHOICES_TABLE);
            mObservationsTable.setToolTipText(TOOL_TIP_OBSERVATIONS_TABLE);
        }
        else
        {
            mEvidenceChoicesTable.setToolTipText(null);
            mObservationsTable.setToolTipText(null);
        }
    }
    
    private void handleMessage(String pMessage, String pTitle, int pMessageType)
    {
        SimpleTextArea simpleArea = new SimpleTextArea(pMessage);
        JOptionPane.showMessageDialog(mParent, simpleArea, pTitle, pMessageType);
    }
    
    private void setDefaults()
    {
        
        handleClearResults();
    }
    
    private void openFile(File pFile, DataFileDelimiter pDelimiter)
    {
        try
        {
            ObservationsData observationsData = new ObservationsData();
            observationsData.loadFromFile(pFile, pDelimiter);
            mObservationsData = observationsData;
            ObservationsTableModel observationsTableModel = new ObservationsTableModel(observationsData);
            mObservationsTable.setModel(observationsTableModel);
            mFileNameLabel.setText(pFile.getAbsolutePath());
            EvidenceChoicesTableModel negativeControlsTableModel = new EvidenceChoicesTableModel(mObservationsData);
            mEvidenceChoicesTable.setModel(negativeControlsTableModel);
            setDefaults();
        }
        catch(FileNotFoundException e)
        {
            handleMessage("The file you specified, \"" + pFile.getAbsolutePath() + "\", could not be found",
                          "File not found",
                          JOptionPane.ERROR_MESSAGE);            
            
        }
        catch(IOException e)
        {
            String errorMessage = e.getMessage();
            handleMessage("The file you specified, \"" + pFile.getAbsolutePath() + "\", could not be read; the specific error message is: " + errorMessage,
                          "File unreadable",
                          JOptionPane.ERROR_MESSAGE);              
        }
        catch(InvalidInputException e)
        {
            String errorMessage = e.getMessage();
            handleMessage("The file you specified, \"" + pFile.getAbsolutePath() + "\", does not conform to the " + pDelimiter.getName() + "-separated value format; the specific error message is: " + errorMessage,
                    "Invalid file format",
                    JOptionPane.ERROR_MESSAGE);            
            
        }
    }
    
    private DataFileDelimiter getDelimiter()
    {
        if(null == mDelimiterBox)
        {
            throw new IllegalStateException("delimiter combo box has not been initialized yet");
        }
        String delimiterName = (String) mDelimiterBox.getSelectedItem();
        DataFileDelimiter delimiter = DataFileDelimiter.forName(delimiterName);
        if(null == delimiter)
        {
            throw new IllegalStateException("unknown data file delimiter name: " + delimiterName); 
        }
        return delimiter;
    }    
    
    private void loadFile()
    {
        FileChooser fileChooser = new FileChooser(mWorkingDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        DataFileDelimiter delimiter = getDelimiter();
        RegexFileFilter fileFilter = new RegexFileFilter(delimiter.getFilterRegex(), "data file in " + delimiter.getName() + "-delimited format");
        fileChooser.setFileFilter(fileFilter);
        int result = fileChooser.showOpenDialog(mParent);
        if(JFileChooser.APPROVE_OPTION == result)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if(selectedFile.exists())
            {
                File parentDir = selectedFile.getParentFile();
                mWorkingDirectory = parentDir;
                openFile(selectedFile, delimiter);
            }
        }           
    }
        
    private void handleClearResults()
    {
        setEnableStateForFields(null != mObservationsData, false);
    }
    
    private void handleHelp()
    {
        JOptionPane.showMessageDialog(mParent, "Sorry, no help is currently available", "No help available", 
                                      JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void initializeContentPane()
    {
        JPanel topPanel = new JPanel();
        
        GridBagLayout gridLayout = new GridBagLayout();
        topPanel.setLayout(gridLayout);
        GridBagConstraints constraints = new GridBagConstraints();
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 0;  
        
        JPanel fileButtonPanel = new JPanel();
        JButton fileLoadButton = new JButton("load observations");
        fileLoadButton.setToolTipText(TOOL_TIP_FILE_LOAD_BUTTON);
        mFileLoadButton = fileLoadButton;
        fileButtonPanel.add(fileLoadButton);
        fileLoadButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                loadFile();
            }
                });
        JButton fileClearButton = new JButton("clear observations");
        fileClearButton.setToolTipText(TOOL_TIP_FILE_CLEAR_BUTTON);
        mFileClearButton = fileClearButton;
        fileButtonPanel.add(fileClearButton);
        topPanel.add(fileButtonPanel);
        fileClearButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                clearFile();
            }
                });
        

        
        topPanel.add(fileButtonPanel);
        gridLayout.setConstraints(fileButtonPanel, constraints);

        JPanel delimitersPanel = new JPanel();
        JLabel delimitersLabel = new JLabel("delimiter: ");
        mDelimiterLabel = delimitersLabel;
        delimitersPanel.add(delimitersLabel);
        DataFileDelimiter []delimiters = DataFileDelimiter.getAll();
        int numDelimiters = delimiters.length;
        String []delimiterNames = new String[numDelimiters];
        for(int i = 0; i < numDelimiters; ++i)
        {
            delimiterNames[i] = delimiters[i].getName();
        }
        JComboBox delimitersBox = new JComboBox(delimiterNames);
        mDelimiterBox = delimitersBox;
        delimitersBox.setToolTipText(TOOL_TIP_DELIMITER);
        delimitersPanel.add(delimitersBox);        
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 1;
        constraints.gridy = 0;          
        
        topPanel.add(delimitersPanel);
        gridLayout.setConstraints(delimitersPanel, constraints);
        
        JLabel fileNameLabel = new JLabel("");
        mFileNameLabel = fileNameLabel;
        fileNameLabel.setPreferredSize(new Dimension(500, 10));
        fileNameLabel.setMinimumSize(new Dimension(500, 10));
        JPanel fileNamePanel = new JPanel();
        fileNamePanel.setBorder(BorderFactory.createEtchedBorder());
        fileNamePanel.add(fileNameLabel);
                
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 2;
        constraints.gridy = 0;          
        
        topPanel.add(fileNamePanel);
        gridLayout.setConstraints(fileNamePanel, constraints);
        
        
        JButton appHelpButton = new JButton();
        ImageIcon helpIcon = IconFactory.getIconByName(RESOURCE_HELP_ICON);

        if(null != helpIcon)
        {
            appHelpButton.setIcon(helpIcon);
        }
        else
        {
            appHelpButton.setText("help");
        }
        appHelpButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                handleHelp();
            }
                });
              
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 3;
        //constraints.gridy = 0;  
        
        appHelpButton.setToolTipText(TOOL_TIP_HELP);
        topPanel.add(appHelpButton);
        gridLayout.setConstraints(appHelpButton, constraints);        
        
        JTable observationsTable = new JTable();
        mObservationsTable = observationsTable;        
        JScrollPane observationsScrollPane = new JScrollPane(observationsTable);
        observationsScrollPane.setBorder(BorderFactory.createEtchedBorder());
        observationsScrollPane.setPreferredSize(new Dimension(500, 250));
        observationsScrollPane.setMaximumSize(new Dimension(500, 250));
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(observationsScrollPane);
        gridLayout.setConstraints(observationsScrollPane, constraints);
        
        JPanel negativeControlsButtonsPanel = new JPanel();
        negativeControlsButtonsPanel.setLayout(new BoxLayout(negativeControlsButtonsPanel, BoxLayout.Y_AXIS));
        
        JButton setNegativeControlFileButton = new JButton("load negative control data");
        setNegativeControlFileButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        setNegativeControlFile();
                    }
                });
        mSetNegativeControlFileButton = setNegativeControlFileButton;
        negativeControlsButtonsPanel.add(setNegativeControlFileButton);
        JButton clearNegativeControlFileButton = new JButton("clear negative control data");
        mClearNegativeControlFileButton = clearNegativeControlFileButton;
        clearNegativeControlFileButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        clearNegativeControlFile();
                    }
                });
        negativeControlsButtonsPanel.add(clearNegativeControlFileButton);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.2;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(negativeControlsButtonsPanel);
        gridLayout.setConstraints(negativeControlsButtonsPanel, constraints);        
        
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.2;
        constraints.gridx = 1;
        //constraints.gridy++;
        
        JTable evidenceChoicesTable = new JTable();
        mEvidenceChoicesTable = evidenceChoicesTable;
        JScrollPane evidenceChoicesScrollPane = new JScrollPane(evidenceChoicesTable);
        evidenceChoicesScrollPane.setPreferredSize(new Dimension(500, 100));
        evidenceChoicesScrollPane.setMinimumSize(new Dimension(500, 100));
        evidenceChoicesTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener()
                {
                    public void valueChanged(ListSelectionEvent e)
                    {
                        setEnableStateForNegativeControlButtons();
                    }
                });
        topPanel.add(evidenceChoicesScrollPane);
        gridLayout.setConstraints(evidenceChoicesScrollPane, constraints);
        
        
//        JLabel numBinsLabel = new JLabel("number of bins for calculating distribution: ");
//        mNumBinsLabel = numBinsLabel;
//        JTextField numBinsField = new JTextField(NUM_COLUMNS_TEXT_FIELD_NUMERIC);
//        mNumBinsField = numBinsField;
//        
        
        // add components to the topPanel
        
        mContentPane.add(topPanel);

        clearFile();
        setEnableStateForFields(false, false);
    }
    
    class NegativeControlSelectionHandler
    {
        private MatrixString mMatrixString;
        private int mEvidenceNum;
        private File mFile;
        
        public NegativeControlSelectionHandler(MatrixString pMatrixString,
                                               int pEvidenceNum,
                                               File pFile)
        {
            mFile = pFile;
            mEvidenceNum = pEvidenceNum;
            mMatrixString = pMatrixString;
        }
        
        public void handleSelection(int pSelectedColumn)
        {
            String []column = mMatrixString.getColumn(pSelectedColumn);
            int numObservations = column.length;
            int startInt = 0;
            try
            {
                Double.parseDouble(column[0]);
            }
            catch(NumberFormatException e)
            {
                numObservations--;
                startInt++;
            }
            Double []observations = new Double[numObservations];
            for(int i = startInt; i < numObservations; ++i)
            {
                try
                {
                    observations[i] = new Double(column[i]);
                }
                catch(NumberFormatException e)
                {
                    handleMessage("The file you specified, \"" + mFile.getAbsolutePath() + "\", contains an element that does not parse as a floating-point number, on line " + i + ": \"" + column[i] + "\"",
                            "Invalid file format",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            NegativeControlData negativeControlData = new NegativeControlData(mFile, pSelectedColumn, observations);
            EvidenceChoicesTableModel tableModel = (EvidenceChoicesTableModel) mEvidenceChoicesTable.getModel();
            tableModel.setNegativeControlData(mEvidenceNum, negativeControlData);
            tableModel.fireTableDataChanged();
        }
    }
    
    private void setNegativeControlFile()
    {
        int selectedRow = mEvidenceChoicesTable.getSelectedRow();
        if(-1 == selectedRow)
        {
            throw new IllegalStateException("could not find selected row for negative control data table");
        }
        FileChooser fileChooser = new FileChooser(mWorkingDirectory);        
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        DataFileDelimiter delimiter = getDelimiter();
        RegexFileFilter fileFilter = new RegexFileFilter(delimiter.getFilterRegex(), "data file in " + delimiter.getName() + "-delimited format");
        fileChooser.setFileFilter(fileFilter);
        int response = fileChooser.showOpenDialog(mParent);
        if(response == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            MatrixString matrixString = new MatrixString();
            final NegativeControlSelectionHandler selectionHandler = new NegativeControlSelectionHandler(matrixString, 
                                                                                                         selectedRow,
                                                                                                         selectedFile);
            try
            {
                FileReader fileReader = new FileReader(selectedFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                matrixString.buildFromLineBasedStringDelimitedInput(bufferedReader, delimiter);
                String []columnStrings = null;
                int numColumns = matrixString.getColumnCount();
                if(numColumns > 1)
                {
                    boolean modal = true;
                    String topElem = null;
                    boolean firstRowIsTitles = false;
                    for(int j = 0; j < numColumns; ++j)
                    {
                        topElem = matrixString.getValueAt(0, j);
                        if(topElem != null)
                        {
                            topElem = topElem.trim();
                            try
                            {
                                Double.parseDouble(topElem);
                            }
                            catch(NumberFormatException e)
                            {
                                firstRowIsTitles = true;
                            }
                        }
                    }
                    final MatrixStringSelectorDialog dialog = new MatrixStringSelectorDialog(null, "Please select the data column for the negative control",
                            modal, matrixString, firstRowIsTitles); 
                    dialog.setMultipleSelectionsAllowed(false);
                    Point location = FramePlacer.placeInCenterOfScreen(dialog.getWidth(), dialog.getHeight());
                    dialog.setLocation(location);
                    
                    dialog.addApproveListener(
                            new ActionListener()
                            {
                                public void actionPerformed(ActionEvent e)
                                {
                                    Boolean []selectedColumns = dialog.getSelectedColumns();
                                    int numColumns = selectedColumns.length;
                                    int j = 0;
                                    for(j = 0; j < numColumns; ++j)
                                    {
                                        if(selectedColumns[j].booleanValue())
                                        {
                                            break;
                                        }
                                    }
                                    if(j < numColumns)
                                    {
                                        selectionHandler.handleSelection(0);
                                    }
                                }
                            });
                    dialog.setVisible(true);
                }
                else
                {
                    selectionHandler.handleSelection(0);
                }
            }
            catch(IOException e)
            {
                handleMessage("The file you specified, \"" + selectedFile.getAbsolutePath() + "\", could not be read.  The specific message is: " + e.getMessage(),
                        "Unable to read file",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch(InvalidInputException e)
            {
                handleMessage("The file you specified, \"" + selectedFile.getAbsolutePath() + "\", does not conform to the " + delimiter.getName() + "-separated value format; the specific error message is: " + e.getMessage(),
                        "Invalid file format",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }
    
    
    private void clearNegativeControlFile()
    {
        int selectedRow = mEvidenceChoicesTable.getSelectedRow();
        if(-1 != selectedRow)
        {
            EvidenceChoicesTableModel tableModel = (EvidenceChoicesTableModel) mEvidenceChoicesTable.getModel();
            tableModel.setNegativeControlData(selectedRow, null);
            tableModel.fireTableDataChanged();
        }
        else
        {
            throw new IllegalStateException("expected non-negative selected row number for the negative control files table");
        }
        
    }
    
    private void clearFile()
    {
        mFileNameLabel.setText("");
        mObservationsData = null;
        mObservationsTable.setModel(mEmptyTableModel);
        mEvidenceChoicesTable.setModel(mEmptyTableModel);
        
        setEnableStateForFields(false, false);
    }
    
    private void setEnableStateForNegativeControlButtons()
    {
        int selectedRow = mEvidenceChoicesTable.getSelectedRow();
        boolean rowSelected = (-1 != selectedRow);
        if(rowSelected)
        {
            mSetNegativeControlFileButton.setToolTipText(TOOL_TIP_SET_NEGATIVE_CONTROL_DATA);
        }
        else
        {
            mSetNegativeControlFileButton.setToolTipText(null);
        }        
        mSetNegativeControlFileButton.setEnabled(rowSelected);
        boolean clearButtonEnableState = false;
        if(rowSelected)
        {
            NegativeControlData data = ((EvidenceChoicesTableModel) mEvidenceChoicesTable.getModel()).getNegativeControlData(selectedRow);
            if(null != data)
            {
                clearButtonEnableState = true;
            }
        }
        if(clearButtonEnableState)
        {
            mClearNegativeControlFileButton.setToolTipText(TOOL_TIP_CLEAR_NEGATIVE_CONTROL_DATA);
        }
        else
        {
            mClearNegativeControlFileButton.setToolTipText(null);
        }
        mClearNegativeControlFileButton.setEnabled(clearButtonEnableState);
    }
    
    private void run(String []pArgs)
    {
        String programName = "Significance Calculator";
        if(pArgs.length > 0)
        {
            programName = pArgs[0] + ": " + programName;
        }
        JFrame frame = new JFrame(programName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = frame.getContentPane();
        initialize(contentPane, frame);
        frame.pack();
        FramePlacer.placeInCenterOfScreen(frame);
        frame.setVisible(true);
    }
    
    public static final void main(String []pArgs)
    {
        // try to create an instance of this class
        try
        {
            SignificanceCalculatorDriver app = new SignificanceCalculatorDriver();
            app.run(pArgs);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
