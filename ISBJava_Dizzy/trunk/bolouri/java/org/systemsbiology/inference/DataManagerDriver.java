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

import cern.colt.list.IntArrayList;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ListSelectionModel;
import java.awt.event.KeyAdapter;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.systemsbiology.data.DataFileDelimiter;
import org.systemsbiology.gui.ComponentUtils;
import org.systemsbiology.gui.EmptyTableModel;
import org.systemsbiology.gui.FileChooser;
import org.systemsbiology.gui.FramePlacer;
import org.systemsbiology.gui.IconFactory;
import org.systemsbiology.gui.RegexFileFilter;
import org.systemsbiology.gui.SimpleTextArea;
import org.systemsbiology.gui.SortStatus;
import org.systemsbiology.math.ScientificNumberFormat;
import org.systemsbiology.math.SignificantDigitsCalculator;
import org.systemsbiology.util.InvalidInputException;
import java.util.LinkedList;
import java.text.NumberFormat;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.ObjectFactory2D;

/**
 * A graphical tool for merging {@link ObservationsData} data files.
 * Rows are specific elements, and columns are specific evidence types.
 * The entire data table may be saved to a file, or a set of
 * columns may be selected and saved to a file.
 * 
 * @author sramsey
 *
 */
public class DataManagerDriver
{
    private Container mContentPane;
    private Component mParent;
    private EmptyTableModel mEmptyTableModel;
    private File mWorkingDirectory;
    private NumberFormat mNumberFormat;
    private JButton mFileLoadButton;
    private JButton mFileClearButton;
    private JList mFileNameListBox;
    private JComboBox mDelimiterBox;
    private JLabel mDelimiterLabel;
    private LinkedList mFileNameList;
    private LinkedList mFileDataList;
    private JTable mDataTable;
    private JButton mResetFormButton;
    private ObservationsData mData;
    private JButton mClearSelectedFileButton;
    private JButton mMoveFileUpButton;
    private JButton mSaveEntireTableButton;
    private JButton mCycleElementSortButton;
    private JButton mCycleEvidenceSortButton;
    private JComboBox mSortStatusElement;
    private JComboBox mSortStatusEvidence;
    private SortStatus []mSortStatuses;
    private JButton mSaveSelectedColumnsButton;
    private JLabel mElementSortStatusLabel;
    private JLabel mEvidenceSortStatusLabel;
    private JCheckBox mAllowDuplicatesBox;
    
    private static final boolean DEFAULT_ALLOW_DUPLICATES = true;
    private static final String TOOL_TIP_SAVE_SELECTED_COLUMNS = "Save only the columns that you have selected in the data table, to a file.";
    private static final String TOOL_TIP_SORT_STATUS_EVIDENCE = "Specify the sorting status for the evidences (none, ascending, descending).";
    private static final String TOOL_TIP_SORT_STATUS_ELEMENT = "Specify the sorting status for the elements (none, ascending, descending).";
    private static final String TOOL_TIP_SAVE_ENTIRE_TABLE = "Save the entire contents of the data table to a file.";
    private static final String TOOL_TIP_MOVE_FILE_UP = "Move the selected file up one level in the list.";
    private static final String TOOL_TIP_FILE_LIST = "Lists the files you have loaded into the data table.  You can select an item and press \"delete\" to remove the file\'s data.";
    private static final String TOOL_TIP_CLEAR_SELECTED_FILE_BUTTON = "Clear the selected file from the data table.";
    private static final String TOOL_TIP_RESET_FORM_BUTTON = "Reset the form to the original state.";
    private static final String TOOL_TIP_DATA_TABLE = "The data you have loaded into the form.  You may select the columns you wish to save, using the column headers, or the cell range you wish to save.";
    private static final String TOOL_TIP_FILE_LOAD_BUTTON = "Load the file of normalized observations.  The first column should be element names.  The column headers (first row) should be evidence names.";
    private static final String TOOL_TIP_FILE_CLEAR_BUTTON = "Clear the file of normalized observations that was loaded into this form.";
    private static final String TOOL_TIP_DELIMITER = "Indicates the type of separator that is used in the data file you want to load.";
    private static final String TOOL_TIP_HELP = "Display the help screen that explains how to use this program";
    private static final String RESOURCE_HELP_ICON = "Help24.gif";
    private static final SortStatus DEFAULT_SORT_STATUS = SortStatus.NONE;
    private static final DataFileDelimiter DEFAULT_DATA_FILE_DELIMITER = DataFileDelimiter.COMMA;
    
    private void initializeContentPane()
    {
        JPanel topPanel = new JPanel();
        
        GridBagLayout gridLayout = new GridBagLayout();
        topPanel.setLayout(gridLayout);
        GridBagConstraints constraints = new GridBagConstraints();


        
        JPanel firstRowPanel = new JPanel();
        
        JPanel fileButtonPanel = new JPanel();
        JButton fileLoadButton = new JButton("load data");
        mFileLoadButton = fileLoadButton;
        mFileLoadButton.setToolTipText(TOOL_TIP_FILE_LOAD_BUTTON);
        fileButtonPanel.add(fileLoadButton);
        fileLoadButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                loadFile();
            }
                });
        JButton fileClearButton = new JButton("clear all data");
        mFileClearButton = fileClearButton;
        fileButtonPanel.add(fileClearButton);
        topPanel.add(fileButtonPanel);
        fileClearButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                clearAllData();
            }
                });
        mClearSelectedFileButton = new JButton("clear selected file");
        fileButtonPanel.add(mClearSelectedFileButton);
        mClearSelectedFileButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        clearSelectedFile();
                    }
                });
        
        mMoveFileUpButton = new JButton("move file up");
        fileButtonPanel.add(mMoveFileUpButton);
        mMoveFileUpButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        moveFileUpInList();
                    }
                });
        firstRowPanel.add(fileButtonPanel);

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
        
        firstRowPanel.add(delimitersPanel);
        
        JPanel allowDuplicatesPanel = new JPanel();
        JLabel allowDuplicatesLabel = new JLabel("allow duplicates: ");
        allowDuplicatesPanel.add(allowDuplicatesLabel);
        mAllowDuplicatesBox = new JCheckBox();
        mAllowDuplicatesBox.setSelected(DEFAULT_ALLOW_DUPLICATES);
        allowDuplicatesPanel.add(mAllowDuplicatesBox);
        
        firstRowPanel.add(allowDuplicatesPanel);
        
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

        
        appHelpButton.setToolTipText(TOOL_TIP_HELP);

        firstRowPanel.add(appHelpButton);

        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 0;  
        
        topPanel.add(firstRowPanel);
        gridLayout.setConstraints(firstRowPanel, constraints);
        

        mFileNameListBox = new JList();
        mFileNameListBox.addKeyListener(
                new KeyAdapter()
                {
                    public void keyPressed(KeyEvent e)
                    {
                        if(e.getKeyCode() == KeyEvent.VK_DELETE ||
                           e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
                        {
                            if(-1 != mFileNameListBox.getSelectedIndex())
                            {
                                clearSelectedFile();
                            }
                        }
                    }
                });
        JScrollPane fileNameListScrollPane = new JScrollPane(mFileNameListBox);
        mFileNameListBox.addListSelectionListener(
                new ListSelectionListener()
                {
                    public void valueChanged(ListSelectionEvent e)
                    {
                        setEnableStateForFields(null != mData);
                        setToolTipsForFields(null != mData);
                    }
                });
        fileNameListScrollPane.setPreferredSize(new Dimension(800, 75));
        fileNameListScrollPane.setMinimumSize(new Dimension(800, 75));
        fileNameListScrollPane.setBorder(BorderFactory.createEtchedBorder());
        JPanel fileNamePanel = new JPanel();
                
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.25;
        constraints.gridx = 0;
        constraints.gridy++;          
        
        topPanel.add(fileNameListScrollPane);
        gridLayout.setConstraints(fileNameListScrollPane, constraints);
                
        mDataTable = new JTable();
        mDataTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener()
                {
                    public void valueChanged(ListSelectionEvent e)
                    {
                        setEnableStateForFields(null != mData);
                        setToolTipsForFields(null != mData);
                    }
                });
        mDataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        mDataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mDataTable.setRowSelectionAllowed(false);
        mDataTable.setColumnSelectionAllowed(true);
        JScrollPane dataTablePane = new JScrollPane(mDataTable);
        dataTablePane.setPreferredSize(new Dimension(800, 400));
        dataTablePane.setMinimumSize(new Dimension(800, 400));
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy++;   
        
        topPanel.add(dataTablePane);
        gridLayout.setConstraints(dataTablePane, constraints);
        
        
        JPanel sortPanel = new JPanel();
        mElementSortStatusLabel = new JLabel("element sort status: ");
        sortPanel.add(mElementSortStatusLabel);
        JPanel elementStatusPanel = new JPanel();
        SortStatus []sortStatuses = SortStatus.getAll();
        mSortStatuses = sortStatuses;
        int numSortStatuses = sortStatuses.length;
        String []sortStatusNames = new String[numSortStatuses]; 
        for(int i = 0; i < numSortStatuses; ++i)
        {
            sortStatusNames[i] = sortStatuses[i].getName();
        }
        mSortStatusElement = new JComboBox(sortStatusNames);
        sortPanel.add(mSortStatusElement);
        mSortStatusElement.addItemListener(
                new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        handleElementSortStatusChange();
                    }
                });
        mEvidenceSortStatusLabel = new JLabel("evidence sort status: ");
        sortPanel.add(mEvidenceSortStatusLabel);
        mSortStatusEvidence = new JComboBox(sortStatusNames);
        sortPanel.add(mSortStatusEvidence);
        mSortStatusEvidence.addItemListener(
                new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        handleEvidenceSortStatusChange();
                    }
                });

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;   
        
        sortPanel.setBorder(BorderFactory.createEtchedBorder());
        
        topPanel.add(sortPanel);
        gridLayout.setConstraints(sortPanel, constraints);        
        
        JPanel buttonPanel = new JPanel();
        mResetFormButton = new JButton("reset form");
        mResetFormButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        resetForm();
                    }
                });
        buttonPanel.add(mResetFormButton);
        mSaveEntireTableButton = new JButton("save entire table");
        mSaveEntireTableButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        saveEntireTable();
                    }
                });
        buttonPanel.add(mSaveEntireTableButton);
        
        mSaveSelectedColumnsButton = new JButton("save selected columns");
        mSaveSelectedColumnsButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        saveSelectedColumns();
                    }
                });
        buttonPanel.add(mSaveSelectedColumnsButton);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;          
        
        topPanel.add(buttonPanel);
        gridLayout.setConstraints(buttonPanel, constraints);
        
        mContentPane.add(topPanel);
        
        setDefaultSortStatusOnControls();
        clearAllData();
    }
    
    private void setDefaultSortStatusOnControls()
    {
        mSortStatusElement.setSelectedItem(DEFAULT_SORT_STATUS.getName());
        mSortStatusEvidence.setSelectedItem(DEFAULT_SORT_STATUS.getName());
    }
    
    private void clearSelectedFile()
    {
        int selectedFileIndex = mFileNameListBox.getSelectedIndex();
        if(-1 != selectedFileIndex)
        {
            ObservationsData removeData = (ObservationsData) mFileDataList.get(selectedFileIndex);
            String nameOfFileToRemove = (String) mFileNameList.get(selectedFileIndex);
            mFileDataList.remove(selectedFileIndex);
            try
            {
                updateDataTable();                
            }
            catch(Exception e)
            {
                handleMessage("Unable to remove the file \"" + nameOfFileToRemove + "\" from the data table; the specific error message is: " + e.getMessage(),
                              "Unable to remove the file from the data table",
                              JOptionPane.ERROR_MESSAGE);
                mFileDataList.add(selectedFileIndex, removeData);
                return;
            }
            mFileNameList.remove(selectedFileIndex);
            mFileNameListBox.setListData(mFileNameList.toArray());
            if(mFileNameList.size() == 0)
            {
                setToolTipsForFields(false);
                setEnableStateForFields(false);
                mDelimiterBox.setSelectedItem(DEFAULT_DATA_FILE_DELIMITER.getName());
                setDefaultSortStatusOnControls();
            }            
        }
    }
    
    
    private void handleEvidenceSortStatusChange()
    {
        int selectedItem = mSortStatusEvidence.getSelectedIndex();
        if(-1 != selectedItem && null != mData)
        {
            ObservationsTableModel tableModel = (ObservationsTableModel) mDataTable.getModel();
            SortStatus oldSortStatus = tableModel.getEvidenceSortStatus();
            SortStatus newSortStatus = mSortStatuses[selectedItem];
            if(! newSortStatus.equals(oldSortStatus))
            {
                tableModel.setEvidenceSortStatus(newSortStatus);
            }
        }
    }
    
    private void handleElementSortStatusChange()
    {
        int selectedItem = mSortStatusElement.getSelectedIndex();
        if(-1 != selectedItem && null != mData)
        {
            ObservationsTableModel tableModel = (ObservationsTableModel) mDataTable.getModel();
            SortStatus oldSortStatus = tableModel.getElementSortStatus();
            SortStatus newSortStatus = mSortStatuses[selectedItem];
            if(! newSortStatus.equals(oldSortStatus))
            {
                tableModel.setElementSortStatus(newSortStatus);
            }
        }
    }

    // WARNING:  make sure pSelectedColumns doesn't contain column "0" (the element name column), and is left-shifted by 1
    private void saveSortedDataTable(int []pSelectedEvidences)
    {
        ObservationsTableModel tableModel = (ObservationsTableModel) mDataTable.getModel();
        int numEvidences = tableModel.getColumnCount() - 1;

        if(null == pSelectedEvidences)
        {
            pSelectedEvidences = new int[numEvidences];
            for(int j = 0; j < numEvidences; ++j)
            {
                pSelectedEvidences[j] = j;
            }
        }
        
        int numElements = tableModel.getRowCount() - 1;

        int numSelectedEvidences = pSelectedEvidences.length;
        
        String []elementNames = new String[numElements];
        String []evidenceNames = new String[numSelectedEvidences];
        ObjectMatrix2D obsMatrix = ObjectFactory2D.dense.make(numElements, numSelectedEvidences);
        
        for(int i = 0; i < numElements; ++i)
        {
            elementNames[i] = (String) tableModel.getValueAt(i + 1, 0);
        }
        
        int evidenceIndex = 0;
        for(int j = 0; j < numSelectedEvidences; ++j)
        {
            evidenceIndex = pSelectedEvidences[j];
            evidenceNames[j] = (String) tableModel.getValueAt(0, evidenceIndex + 1);
        }
        
        int i= 0;
        Double obsVal = null;
        for(int j = 0; j < numSelectedEvidences; ++j)
        {
            evidenceIndex = pSelectedEvidences[j];

            for(i = 0; i < numElements; ++i)
            {
                obsVal = (Double) tableModel.getValueAtNoFormatting(i + 1, evidenceIndex + 1);
                obsMatrix.set(i, j, obsVal);
            }
        }
            
        ObservationsData outputData = new ObservationsData(obsMatrix, elementNames, evidenceNames);
        
        saveData(outputData);
    }
    
    private void saveSelectedColumns()
    {
        int []selectedColumns = mDataTable.getSelectedColumns();
        IntArrayList arrayList = new IntArrayList(selectedColumns);
        arrayList.sort();
        int zeroIndex = arrayList.binarySearch(0);
        if(zeroIndex >= 0)
        {
            arrayList.remove(zeroIndex);
        }
        int arraySize = arrayList.size();
        int []correctedSelectedColumns = arrayList.elements();
        for(int j = 0; j < arraySize; ++j)
        {
            correctedSelectedColumns[j]--;
        }
        saveSortedDataTable(correctedSelectedColumns);
    }
    
    private void saveEntireTable()
    {
        saveSortedDataTable(null);
    }
    
    private void saveData(ObservationsData pData)
    {
        DataFileDelimiter delimiter = getDelimiter();
        FileChooser fileChooser = new FileChooser(mWorkingDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showSaveDialog(mParent);
        if(result == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            String selectedFileName = selectedFile.getAbsolutePath();
            if(-1 == selectedFileName.indexOf('.'))
            {
                selectedFileName = selectedFileName + "." + delimiter.getDefaultExtension();
                selectedFile = new File(selectedFileName);
            }
            boolean confirmProceed = true;
            if(selectedFile.exists())
            {
                confirmProceed = FileChooser.handleOutputFileAlreadyExists(mParent, selectedFile.getAbsolutePath());
            }
            if(confirmProceed)
            {
                writeData(pData, selectedFile, delimiter);
            }
        }
    }
    
    private void writeData(ObservationsData pData, File pSelectedFile, DataFileDelimiter pDelimiter)
    {
        String []evidences = pData.getEvidenceNames();
        String []elements = pData.getElementNames();
        
        StringBuffer sb = new StringBuffer();
        String delimiter = pDelimiter.getDelimiter();
        int numEvidences = evidences.length;
        int numElements = elements.length;
        sb.append(ObservationsTableModel.COLUMN_NAME_ELEMENT + delimiter);
        for(int j = 0; j < numEvidences; ++j)
        {
            sb.append(evidences[j]);
            if(j < numEvidences - 1)
            {
                sb.append(delimiter);
            }
        }
        sb.append("\n");
        Double obsObj = null;
        double obs = 0.0;
        String elementName = null;
        for(int i = 0; i < numElements; ++i)
        {
            elementName = pDelimiter.scrubIdentifier(elements[i]);
            sb.append(elementName + delimiter);
            for(int j = 0; j < numEvidences; ++j)
            {
                obsObj = pData.getValueAt(i, j);
                if(null != obsObj)
                {
                    sb.append(mNumberFormat.format(obsObj.doubleValue()));
                }
                if(j < numEvidences - 1)
                {
                    sb.append(delimiter);
                }
            }
            sb.append("\n");
        }
        
        try
        {
            FileWriter fileWriter = new FileWriter(pSelectedFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(sb.toString());
            printWriter.flush();
            JOptionPane.showMessageDialog(mParent, 
                                          new SimpleTextArea("data was written to the file \"" + pSelectedFile.getAbsolutePath() + "\" successfully."), 
                                          "data written successfully", JOptionPane.INFORMATION_MESSAGE);
        }
        catch(IOException e)
        {
            SimpleTextArea simpleTextArea = new SimpleTextArea("unable to write the data to the file you requested, \"" + pSelectedFile.getAbsolutePath() + "\"; specific error message is: " + e.getMessage());
            JOptionPane.showMessageDialog(mParent, simpleTextArea, "unable to write data to file", JOptionPane.ERROR_MESSAGE);
        }        
    }        
    
    
    private void updateDataTable()
    {
        ObservationsData []dataArray = (ObservationsData []) mFileDataList.toArray(new ObservationsData[0]);
        if(dataArray.length == 0)
        {
            mData = null;
            mDataTable.setModel(mEmptyTableModel);
            return;
        }
        boolean newObservationsTableModel = (null == mData);
        mData = new ObservationsData();
        mData.mergeDataArray(dataArray, mAllowDuplicatesBox.isSelected());
        ObservationsTableModel tableModel = null;
        if(! newObservationsTableModel)
        {
            tableModel = (ObservationsTableModel) mDataTable.getModel();
            tableModel.setObservationsData(mData);
        }
        else
        {
            tableModel = new ObservationsTableModel(mData);
            tableModel.setCellValuesEditable(true);
            tableModel.setElementNamesEditable(true);
            tableModel.setEvidenceNamesEditable(true);
            mDataTable.setModel(tableModel);
        }
    }
    
    private void clearAllData()
    {
        mData = null;
        mFileNameList.clear();
        mFileDataList.clear();
        
        mFileNameListBox.setListData(mFileNameList.toArray());
        mDataTable.setModel(mEmptyTableModel);
        
        setEnableStateForFields(false);
        setToolTipsForFields(false);
        mDelimiterBox.setSelectedItem(DEFAULT_DATA_FILE_DELIMITER.getName());
        setDefaultSortStatusOnControls();
    }
          
    private void setEnableStateForFields(boolean pFileLoaded)
    {
        mFileClearButton.setEnabled(pFileLoaded);
        mSaveEntireTableButton.setEnabled(pFileLoaded);
        if(pFileLoaded)
        {
            int index = mFileNameListBox.getSelectedIndex();
            mClearSelectedFileButton.setEnabled(index != -1);
            mMoveFileUpButton.setEnabled(index > 0);
        }
        else
        {
            mClearSelectedFileButton.setEnabled(false);
            mMoveFileUpButton.setEnabled(false);
        }
        mResetFormButton.setEnabled(pFileLoaded);        
        mFileNameListBox.setEnabled(pFileLoaded);
        mSortStatusElement.setEnabled(pFileLoaded);
        mSortStatusEvidence.setEnabled(pFileLoaded);
        boolean showSaveSelectedColumns = pFileLoaded && (mDataTable.getSelectedColumnCount() > 0);
        mSaveSelectedColumnsButton.setEnabled(showSaveSelectedColumns);
        mElementSortStatusLabel.setEnabled(pFileLoaded);
        mEvidenceSortStatusLabel.setEnabled(pFileLoaded);
    }
    
    private void setToolTipsForFields(boolean pFileLoaded)
    {
        if(pFileLoaded)
        {
            mSaveEntireTableButton.setToolTipText(TOOL_TIP_SAVE_ENTIRE_TABLE);
            mFileClearButton.setToolTipText(TOOL_TIP_FILE_CLEAR_BUTTON);
            mDataTable.setToolTipText(TOOL_TIP_DATA_TABLE);
            mResetFormButton.setToolTipText(TOOL_TIP_RESET_FORM_BUTTON);
            int index = mFileNameListBox.getSelectedIndex();
            if(index != -1)
            {
                mClearSelectedFileButton.setToolTipText(TOOL_TIP_CLEAR_SELECTED_FILE_BUTTON);
                if(index > 0)
                {
                    mMoveFileUpButton.setToolTipText(TOOL_TIP_MOVE_FILE_UP);
                }
                else
                {
                    mMoveFileUpButton.setToolTipText(null);
                }
            }
            else
            {
                mClearSelectedFileButton.setToolTipText(null);
                mMoveFileUpButton.setToolTipText(null);
            }
            if(mDataTable.getSelectedColumnCount() > 0)
            {
                mSaveSelectedColumnsButton.setToolTipText(TOOL_TIP_SAVE_SELECTED_COLUMNS);
            }
            else
            {
                mSaveSelectedColumnsButton.setToolTipText(null);
            }
            mFileNameListBox.setToolTipText(TOOL_TIP_FILE_LIST);
            mSortStatusElement.setToolTipText(TOOL_TIP_SORT_STATUS_ELEMENT);
            mSortStatusEvidence.setToolTipText(TOOL_TIP_SORT_STATUS_EVIDENCE);
        }
        else
        {
            mSaveSelectedColumnsButton.setToolTipText(null);
            mSaveEntireTableButton.setToolTipText(null);
            mFileClearButton.setToolTipText(null);
            mDataTable.setToolTipText(null);
            mResetFormButton.setToolTipText(null);
            mClearSelectedFileButton.setToolTipText(null);
            mFileNameListBox.setToolTipText(null);
            mSortStatusElement.setToolTipText(null);
            mSortStatusEvidence.setToolTipText(null);
        }
    }    
    
    private void resetForm()
    {
        ObservationsTableModel tableModel = (ObservationsTableModel) mDataTable.getModel();
        tableModel.setElementSortStatus(DEFAULT_SORT_STATUS);
        tableModel.setEvidenceSortStatus(DEFAULT_SORT_STATUS);
        setDefaultSortStatusOnControls();
        handleEvidenceSortStatusChange();
        handleElementSortStatusChange();
        mAllowDuplicatesBox.setSelected(DEFAULT_ALLOW_DUPLICATES);
    }
    
    private void handleMessage(String pMessage, String pTitle, int pMessageType)
    {
        SimpleTextArea simpleArea = new SimpleTextArea(pMessage);
        JOptionPane.showMessageDialog(mParent, simpleArea, pTitle, pMessageType);
    }
    
    private void openFile(File pFile, DataFileDelimiter pDelimiter)
    {
        try
        {
            ObservationsData observationsData = new ObservationsData();
            FileReader fileReader = new FileReader(pFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            observationsData.loadFromFile(bufferedReader, pDelimiter);

            String fileName = pFile.getAbsolutePath();
            
            int selectedFileIndex = mFileNameListBox.getSelectedIndex();
            if(-1 == selectedFileIndex)
            {
                mFileDataList.add(observationsData);
            }
            else
            {
                mFileDataList.add(selectedFileIndex, observationsData);
            }
            try
            {
                updateDataTable();
            }
            catch(Exception e)
            {
                handleMessage("Unable to load the data file you requested, \"" + fileName + "\"; the specific error message is: " + e.getMessage(),
                              "Unable to load the data file",
                              JOptionPane.ERROR_MESSAGE);
                mFileDataList.remove(mFileDataList.size() - 1);
                return;
            }
            if(-1 == selectedFileIndex)
            {
                mFileNameList.add(fileName);
            }
            else
            {
                mFileNameList.add(selectedFileIndex, fileName);
            }
            mFileNameListBox.setListData(mFileNameList.toArray());
            setEnableStateForFields(true);
            setToolTipsForFields(true);
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
            handleMessage("The file you specified, \"" + pFile.getAbsolutePath() + "\", does not conform to the comma-separated value format; the specific error message is: " + errorMessage,
                    "Invalid file format",
                    JOptionPane.ERROR_MESSAGE);            
            
        }
    }    
    
    private void moveFileUpInList()
    {
        int selectedFileIndex = mFileNameListBox.getSelectedIndex();
        if(selectedFileIndex > 0)
        {
            String fileName = (String) mFileNameList.remove(selectedFileIndex);
            mFileNameList.add(selectedFileIndex - 1, fileName);
            ObservationsData data = (ObservationsData) mFileDataList.remove(selectedFileIndex);
            mFileDataList.add(selectedFileIndex - 1, data);
            mFileNameListBox.setListData(mFileNameList.toArray());
            try
            {
                updateDataTable();
            }
            catch(Exception e)
            {
                mFileNameList.remove(selectedFileIndex - 1);
                mFileDataList.remove(selectedFileIndex - 1);
                mFileNameList.add(selectedFileIndex, fileName);
                mFileDataList.add(selectedFileIndex, data);
                handleMessage("Unable move the file you requested, \"" + fileName + "\"; the specific error message is: " + e.getMessage(),
                        "Unable to move file up in the list",
                        JOptionPane.ERROR_MESSAGE);                
                return;
            }            
            
        }
    }
       
    private void loadFile()
    {
        JFileChooser fileChooser = new JFileChooser(mWorkingDirectory);
        ComponentUtils.disableDoubleMouseClick(fileChooser);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        DataFileDelimiter delimiter = getDelimiter();
        RegexFileFilter fileFilter = new RegexFileFilter(delimiter.getFilterRegex(), 
                                                        "data file in " + delimiter.getName() + "-delimited format");
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
       
    
    private void run(String []pArgs)
    {
        String programName = "Data Manager"; 
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
    
    public void initialize(Container pContentPane, Component pParent)
    {
        mContentPane = pContentPane;
        mParent = pParent;
        mWorkingDirectory = null;
        mEmptyTableModel = new EmptyTableModel();
        mNumberFormat = new ScientificNumberFormat(new SignificantDigitsCalculator());
        mFileNameList = new LinkedList();
        mFileDataList = new LinkedList();
        mData = null;
        initializeContentPane();
    }

    private DataFileDelimiter getDelimiter()
    {
        if(null == mDelimiterBox)
        {
            throw new IllegalStateException("delimiter combo box has not been initialized yet");
        }
        String delimiterName = (String) mDelimiterBox.getSelectedItem();
        DataFileDelimiter delimiter = DataFileDelimiter.get(delimiterName);
        if(null == delimiter)
        {
            throw new IllegalStateException("unknown data file delimiter name: " + delimiterName); 
        }
        return delimiter;
    }    
        
    private void handleHelp()
    {
        JOptionPane.showMessageDialog(mParent, "Sorry, no help is currently available", "No help available", 
                                      JOptionPane.INFORMATION_MESSAGE);
    }    
    
    public static final void main(String []pArgs)
    {
        // try to create an instance of this class
        try
        {
            DataManagerDriver app = new DataManagerDriver();
            app.run(pArgs);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
