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

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

import javax.swing.table.*;
import org.systemsbiology.gui.*;
import org.systemsbiology.data.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import java.io.*;
import cern.colt.matrix.*;
import java.util.*;

/**
 * Graphical user interface for the Pointillist algorithm for
 * inferring the set of elements affected by a perturbation of a biological
 * system.  The algorithm computes the combined significance, which is 
 * analogous (but
 * not identical) to the joint probability that an element would
 * have a given set of significance values, by chance, if it were
 * not in the set of true affected elements.  This algorithm was
 * designed by Daehee Hwang at Institute for Systems Biology.
 * 
 * @author sramsey
 *
 */
public class EvidenceWeightedInfererDriver 
{
    public static final double ALPHA_VALUE_DEVIATION_WARNING_THRESHOLD = 0.1;    
    private static final String COLUMN_NAME_ELEMENT = "element";
    private static final String COLUMN_NAME_AFFECTED = "affected";
    private static final String COLUMN_NAME_OVERALL_SIGNIFICANCE = "overall significance";
    private static final String TOOL_TIP_LOAD_FILE = "Load the significances file.  The file must contain comma-separated values, with element names in the first column, and evidence type names in the first row";
    private static final String TOOL_TIP_CLEAR_FILE = "Clear the file of significances that was loaded into this form.";
    private static final String TOOL_TIP_SEPARATION = "The degree of separation of the distributions of signfificances for putative affected & unaffected elements; it should be very close to 1.0";
    private static final String TOOL_TIP_ITERATIONS = "The number of iterations that the algorithm required in order to complete";
    private static final String TOOL_TIP_ALPHA = "A parameter indicating how independent the different evidences are; should be very close to 1.0";
    private static final String TOOL_TIP_HELP = "Display the help screen that explains how to use this program";
    private static final String TOOL_TIP_SIGNIFICANCES = "The first column of this table should be the element names; the column headers should be the evidence names.";
    private static final String TOOL_TIP_DELIMITER = "Indicates the type of separator that is used in the data file you want to load.";
    private static final String TOOL_TIP_RESET_FORM = "Resets all fields in this form to their original default values";
    private static final String TOOL_TIP_COMBINED_EFFECTIVE_SIGNIFICANCE_FIELD = "Set this to something less than 1e-N, where N is the number of evidences.";
    private static final String TOOL_TIP_INFER_BUTTON = "load the file of significances and infer the list of affected elements for this system";
    private static final String TOOL_TIP_WEIGHT_BOX = "set this to the weight type correponding to the method you used for computing the significances";
    private static final String TOOL_TIP_SEPARATION_FIELD = "set this to between 1.0e-5 and 1.0e-8; the smaller the value, the closer the final separation will be to 1.0";
    private static final String TOOL_TIP_QUANTILE_FIELD = "set this to between 0.05 and 0.1; the larger the value, the quicker (and rougher) the results";
    private static final String TOOL_TIP_INITIAL_CUTOFF_FIELD = "set this to what you believe to be the approximate fraction of elements that are in the affected set";
    private static final String TOOL_TIP_BINS_FIELD = "set this to something between 10 (if you have just a few dozen elements) and 1000 (if you have over 10000 elements)";
    private static final String TOOL_TIP_SMOOTHING_LENGTH_FIELD = "Set this to the smoothing length for obtaining the nonparametric distribution of the significances.";
    private static final String TOOL_TIP_NUM_AFFECTED = "The number of elements that are in the final putative set of affected elements.";
    private static final String RESOURCE_HELP_ICON = "Help24.gif";
    private static final String HELP_SET_MAP_ID = "evidenceweightedinferer";
    private static final String TOOL_TIP_SAVE_WEIGHTS = "Save the evidence weights to a file.";
    private static final String COLUMN_NAME_EVIDENCE = "evidence name";
    private static final String COLUMN_NAME_WEIGHT = "weight";
    
    private Container mContentPane;
    
    private static final double DEFAULT_SMOOTHING_LENGTH = 0.05;
    
    private static final int MIN_NUM_BINS = 10;
    private static final double MAX_COMBINED_SIGNIFICANCE_CUTOFF = 1.0e-8;
    private static final int NUM_COLUMNS_NUMERIC_FIELD = 10;
    public static final int DEFAULT_NUM_BINS = 100;
    public static final double DEFAULT_INITIAL_SIGNIFICANCE_CUTOFF = 0.05;
    public static final double DEFAULT_QUANTILE_THRESHOLD = 0.05;
    public static final double DEFAULT_SEPARATION_THRESHOLD = 1.0e-7;
    public static final EvidenceWeightType DEFAULT_EVIDENCE_WEIGHT_TYPE = EvidenceWeightType.POWER;
    public static final DataFileDelimiter DEFAULT_DATA_FILE_DELIMITER = DataFileDelimiter.COMMA;
    private static final String FRAME_NAME = "Evidence-Weighted Inferer";
    
    private final ScientificNumberFormat mNumberFormat;
    private SignificancesData mSignificancesData;
    private EvidenceWeightedInferer mEvidenceWeightedInferer;
    private EvidenceWeightedInfererResults mEvidenceWeightedInfererResults;
    private EmptyTableModel mEmptyTableModel;
    private String mProgramName;
    private HelpBrowser mHelpBrowser;
        
    private JButton mClearFileButton;
    private JTable mSignificancesTable;
    private JLabel mFileNameLabel;
    private JLabel mBinsLabel;
    private JTextField mBinsField;
    private JLabel mInitialCutoffLabel;
    private JTextField mInitialCutoffField;
    private JLabel mCombinedSignificanceLabel;
    private JTextField mCombinedSignificanceField;
    private JLabel mQuantileLabel;
    private JTextField mQuantileField;
    private JLabel mSeparationLabel;
    private JTextField mSeparationField;
    private JLabel mWeightLabel;
    private JComboBox mWeightBox;
    private Component mParent;
    private File mWorkingDirectory;
    private JButton mInferButton;
    private JLabel mDelimiterLabel;
    private JComboBox mDelimiterBox;
    private JTable mResultsTable;
    private JLabel mIterationsLabel;
    private JLabel mIterationsLabelData;
    private JLabel mFinalSeparationLabel;
    private JLabel mFinalSeparationLabelData;
    private JLabel mAlphaLabel;
    private JLabel mAlphaLabelData;
    private JButton mLoadFileButton;
    private JButton mResetFormButton;
    private JButton mSaveResultsButton;
    private JButton mClearResultsButton;
    private JTable mWeightsTable;
    private JLabel mSmoothingLengthLabel;
    private JTextField mSmoothingLengthField;
    private JLabel mResultsStatisticsLabel;
    private JButton mSaveWeightsButton;
    private JLabel mNumAffectedLabel;
    private JLabel mNumAffectedField;
    private AppConfig mAppConfig;
    
    class WeightsTableModel extends AbstractTableModel
    {
        private String []mEvidenceNames;
        private double []mWeights;
        
        public WeightsTableModel(String []pEvidenceNames, double []pWeights)
        {
            mWeights = pWeights;
            mEvidenceNames = pEvidenceNames;
        }
        public int getColumnCount()
        {
            return 2;
        }
        public int getRowCount()
        {
            return mWeights.length;
        }
        public Object getValueAt(int pRow, int pColumn)
        {
            switch(pColumn)
            {
            case 0:
                return mEvidenceNames[pRow];
                
            case 1:
                return mNumberFormat.format(mWeights[pRow]);
                
            default:
                throw new IllegalStateException("unknown column requested from weights table model: " + pColumn);
                
            }
        }
        public String getColumnName(int pColumn)
        {
            switch(pColumn)
            {
            case 0:
                return COLUMN_NAME_EVIDENCE;
                
            case 1:
                return COLUMN_NAME_WEIGHT;
                
            default:
                throw new IllegalStateException("unknown column requested from weights table model: " + pColumn);
                
            }
        }
    }
    
    class InferenceResultsMouseHandler extends MouseAdapter
    {
        private InferenceResultsTableModel mInferenceResultsTableModel;
        
        public InferenceResultsMouseHandler(InferenceResultsTableModel pInferenceResultsTableModel)
        {
            mInferenceResultsTableModel = pInferenceResultsTableModel;
        }
        
        public void mouseClicked(MouseEvent e) {
            JTableHeader h = (JTableHeader) e.getSource();
            TableColumnModel columnModel = h.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int column = columnModel.getColumn(viewColumn).getModelIndex();
            if (column != -1) {
                int status = mInferenceResultsTableModel.getSortingStatus(column);
                if (! e.isControlDown())
                {
                    mInferenceResultsTableModel.cancelSorting();
                }
                // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or 
                // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed. 
                status = status + (e.isShiftDown() ? -1 : 1);
                status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
                mInferenceResultsTableModel.setSortingStatus(column, status);
            }
        }
    }
    

    class InferenceResultsTableModel extends AbstractTableModel
    {
        private EvidenceWeightedInfererResults mResults;
        private SignificancesData mSignificancesData;
        private Boolean []mAffectedElements;
        private Double []mCombinedSignificances;
        private MasterSorter mMasterSorter;
        private LinkedList mIndex;
        private int []mSortingStatus;
        public static final int SORT_STATUS_NOT_SORTED = 0;
        public static final int SORT_STATUS_ASCENDING = 1;
        public static final int SORT_STATUS_DESCENDING = -1;
        public static final int NUM_COLUMNS = 3;
        private final String []COLUMN_NAMES = {COLUMN_NAME_ELEMENT, 
                                               COLUMN_NAME_AFFECTED, 
                                               COLUMN_NAME_OVERALL_SIGNIFICANCE};
        private Comparator []mSortingComparators;
        
        public void cancelSorting()
        {
            for(int i = 0; i < NUM_COLUMNS; ++i)
            {
                mSortingStatus[i] = SORT_STATUS_NOT_SORTED;
            }
            updateSorting();
        }
        
        public void setSortingStatus(int pColumn, int pStatus)
        {
            mSortingStatus[pColumn] = pStatus;
            updateSorting();
        }
        
        public int getSortingStatus(int pColumn)
        {
            return mSortingStatus[pColumn];
        }
        
        private void updateSorting()
        {
            Collections.sort(mIndex, mMasterSorter);
            fireTableDataChanged();
        }
        
        class MasterSorter implements Comparator
        {
            public int compare(Object p1, Object p2)
            {
                //System.out.println("comparing " + p1 + " to " + p2);
                int index1 = ((Integer)p1).intValue();
                int index2 = ((Integer)p2).intValue();
                for(int i = 0; i < NUM_COLUMNS; ++i)
                {
                    int status = mSortingStatus[i];
                    if(SORT_STATUS_NOT_SORTED != status)
                    {
//                        System.out.println("using comparator");
                        Comparator subComp = mSortingComparators[i];
                        int compVal = subComp.compare(p1, p2);
                        if(compVal != 0)
                        {
                            if(SORT_STATUS_ASCENDING == status)
                            {
                                return compVal;
                            }
                            else
                            {
                                return -compVal;
                            }
                        }
                    }
                }
                return index1 - index2;
            }
        }
        
        class SortBySig implements Comparator
        {
            public int compare(Object p1, Object p2)
            {
                int index1 = ((Integer)p1).intValue();
                int index2 = ((Integer)p2).intValue();
                return Double.compare(mCombinedSignificances[index1].doubleValue(), 
                                      mCombinedSignificances[index2].doubleValue());
            }
        }
        
        class SortByAff implements Comparator
        {
            public int compare(Object p1, Object p2)
            {
                int index1 = ((Integer)p1).intValue();
                int index2 = ((Integer)p2).intValue();
                boolean aff1 = mAffectedElements[index1].booleanValue();
                boolean aff2 = mAffectedElements[index2].booleanValue();
                int aff1int = (aff1) ? 1 : 0;
                int aff2int = (aff2) ? 1 : 0;
                return (aff1int - aff2int);
            }
        }
        
        class SortByName implements Comparator
        {
            public int compare(Object p1, Object p2)
            {
                int index1 = ((Integer)p1).intValue();
                int index2 = ((Integer)p2).intValue();
                return mSignificancesData.getElementName(index1).compareTo(mSignificancesData.getElementName(index2));
            }
        }
        
        public InferenceResultsTableModel(EvidenceWeightedInfererResults pResults,
                                          SignificancesData pSignificancesData)
        {
            mResults = pResults;
            mSignificancesData = pSignificancesData;
            int numElements = mResults.mAffectedElements.length;
            mAffectedElements = new Boolean[numElements];
            mCombinedSignificances = new Double[numElements];
            double sig = 0.0;
            mIndex = new LinkedList();
            mSortingStatus = new int[NUM_COLUMNS];
            mSortingComparators = new Comparator[NUM_COLUMNS];
            mSortingComparators[0] = new SortByName();
            mSortingComparators[1] = new SortByAff();
            mSortingComparators[2] = new SortBySig();
            mMasterSorter = new MasterSorter();
            
            for(int i = 0; i < numElements; ++i)
            {
                mAffectedElements[i] = new Boolean(mResults.mAffectedElements[i]);
                sig = mResults.mCombinedEffectiveSignificances[i];
                mCombinedSignificances[i] = new Double(sig);
                mIndex.add(new Integer(i));
            }

            cancelSorting();
        }
        
        public int getRowCount()
        {
            return mResults.mAffectedElements.length;
        }
        
        public int getColumnCount()
        {
            return NUM_COLUMNS;
        }
        
        public String getColumnName(int pColumn)
        {
            if(pColumn >= NUM_COLUMNS)
            {
                throw new IllegalArgumentException("invalid column number: " + pColumn);
            }
            return COLUMN_NAMES[pColumn];
        }

        public Integer []getSortedIndices()
        {
            return (Integer []) mIndex.toArray(new Integer[0]);
        }
        
        public Object getValueAt(int pRow, int pColumn)
        {
            if(pColumn >= NUM_COLUMNS)
            {
                throw new IllegalArgumentException("invalid column number: " + pColumn);
            }
            
            int index = ((Integer) mIndex.get(pRow)).intValue();
            
            switch(pColumn)
            {
            case 0:
                return mSignificancesData.getElementName(index);
                
            case 1:
                return mAffectedElements[index];
                
            case 2:
                double val = mCombinedSignificances[index].doubleValue();
                return mNumberFormat.format(val);
                
            default:
                throw new IllegalStateException("unknown column number: " + pColumn);

            }
        }
    }
    
    class SignificancesTableModel extends AbstractTableModel
    {
        private SignificancesData mSignificancesData;
        
        public SignificancesTableModel(SignificancesData pSignificancesData)
        {
            mSignificancesData = pSignificancesData;
        }
        public int getRowCount()
        {
            return mSignificancesData.getNumElements();
        }
        public int getColumnCount()
        {
            return mSignificancesData.getNumEvidences() + 1;
        }
        public String getColumnName(int pColumn)
        {
            if(pColumn == 0)
            {
                return COLUMN_NAME_ELEMENT;
            }
            else
            {
                return mSignificancesData.getEvidenceName(pColumn - 1);
            }
        }
        public Object getValueAt(int pRow, int pColumn)
        {
            if(pColumn == 0)
            {
                return mSignificancesData.getElementName(pRow);
            }
            else
            {
                double val = mSignificancesData.getValueAt(pRow, pColumn - 1);
                if(val >= 0.0)
                {
                    return mNumberFormat.format(val);
                }
                else
                {
                    return "";
                }
            }
        }
    }
    
    public EvidenceWeightedInfererDriver()
    {
        mNumberFormat = new ScientificNumberFormat(new SignificantDigitsCalculator());
        mEvidenceWeightedInferer = new EvidenceWeightedInferer();
    }

    public void initialize(Container pContentPane, Component pParent, String pProgramName)
    {
        mContentPane = pContentPane;
        mParent = pParent;
        mProgramName = pProgramName;
        mWorkingDirectory = null;
        mHelpBrowser = null;
        mSignificancesData = null;
        mEvidenceWeightedInfererResults = null;
        mWeightsTable = null;
        mEmptyTableModel = new EmptyTableModel();

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
    
    private void clearFile()
    {
        mFileNameLabel.setText("");
        mDelimiterBox.setSelectedItem(DEFAULT_DATA_FILE_DELIMITER.getName());

        mSignificancesData = null;
        mSignificancesTable.setModel(mEmptyTableModel);
        setDefaults();
    }
    

    private void setDefaults()
    {
        mInitialCutoffField.setText(Double.toString(DEFAULT_INITIAL_SIGNIFICANCE_CUTOFF));

        if(null == mSignificancesData)
        {
            mBinsField.setText(Integer.toString(DEFAULT_NUM_BINS));
            mCombinedSignificanceField.setText(mNumberFormat.format(MAX_COMBINED_SIGNIFICANCE_CUTOFF));
        }
        else
        {
            int numElements = mSignificancesData.getNumElements();
            int defaultNumBins = (int) Math.max(((double) numElements)/10.0, MIN_NUM_BINS);
            
            mBinsField.setText(Integer.toString(defaultNumBins));
            
            int numEvidences = mSignificancesData.getNumEvidences();
            double missingDataRate = mSignificancesData.getMissingDataRate();
            int numEvidencesEffec = (int) Math.max(Math.rint((1.0 - missingDataRate) * ((double) numEvidences)), 1.0);
            double defaultCombinedSignificance = Math.pow(DEFAULT_INITIAL_SIGNIFICANCE_CUTOFF, numEvidencesEffec);
            defaultCombinedSignificance = Math.min(defaultCombinedSignificance, MAX_COMBINED_SIGNIFICANCE_CUTOFF);
            mCombinedSignificanceField.setText(mNumberFormat.format(defaultCombinedSignificance));
        }
        
        mSmoothingLengthField.setText(Double.toString(DEFAULT_SMOOTHING_LENGTH));
        mQuantileField.setText(Double.toString(DEFAULT_QUANTILE_THRESHOLD));
        mSeparationField.setText(Double.toString(DEFAULT_SEPARATION_THRESHOLD));
        mWeightBox.setSelectedItem(DEFAULT_EVIDENCE_WEIGHT_TYPE.toString());

        handleClearResults();
    }

    private void handleMessage(String pMessage, String pTitle, int pMessageType)
    {
        SimpleTextArea simpleArea = new SimpleTextArea(pMessage);
        JOptionPane.showMessageDialog(mParent, simpleArea, pTitle, pMessageType);
    }
    
    private boolean validateFields(EvidenceWeightedInfererParams pParams)
    {   
        String numBinsString = mBinsField.getText().trim();
        int numBins = 0;
        try
        {
            numBins = Integer.parseInt(numBinsString);
        }
        catch(NumberFormatException e)
        {
            handleMessage("The number of bins you specified, \"" + numBinsString + "\", is not an integer",
                          "Invalid number of bins",
                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(numBins < EvidenceWeightedInferer.MIN_NUM_BINS)
        {
            handleMessage("The number of bins you specified is not valid because it needs to be greater than " + 
                          EvidenceWeightedInferer.MIN_NUM_BINS + ".",
                          "Invalid number of bins",
                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        pParams.setNumBins(new Integer(numBins));
        
        String initialCutoffString = mInitialCutoffField.getText().trim();
        double initialCutoff = 0.0;
        try
        {
            initialCutoff = Double.parseDouble(initialCutoffString);
        }
        catch(NumberFormatException e)
        {
            handleMessage("The initial quantile threshold you specified, \"" + initialCutoffString + "\", is not a floating-point number.",
                          "Invalid initial quantile threshold",
                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(initialCutoff <= 0.0 || initialCutoff >= 1.0)
        {
            handleMessage("The initial quantile threshold must be greater than 0.0 and less than 1.0.",
                          "Invalid initial quantile threshold",
                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        pParams.setInitialSignificanceCutoff(new Double(initialCutoff));
        
        String combinedSignificanceString = mCombinedSignificanceField.getText().trim();
        double combinedSignificance = 0.0;
        try
        {
            combinedSignificance = Double.parseDouble(combinedSignificanceString);
        }
        catch(NumberFormatException e)
        {
            handleMessage("The combined significance cutoff you specified, \"" + combinedSignificanceString + "\", is not a valid floating-point number.",
                    "Invalid combined cutoff",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(combinedSignificance < 0.0)
        {
            handleMessage("The combined significance cutoff must be greater than or equal to 0.0.",
                          "Invalid combined cutoff",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        pParams.setCombinedSignificanceCutoff(new Double(combinedSignificance));
        
        String quantileString = mQuantileField.getText();
        double quantile = 0.0;
        try
        {
            quantile = Double.parseDouble(quantileString);
        }
        catch(NumberFormatException e)
        {
            handleMessage("The fraction of elements to remove (in each iteration) that you specified, " + quantileString + "\", is not a valid floating-point number.",
                    "Invalid fraction of elements to remove",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(quantile <= 0.0 || quantile >= 1.0)
        {
            handleMessage("The fraction of elements to remove (in each iteration) must be greater than 0.0, and less than 1.0.",
                    "Invalid fraction of elements to remove",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        pParams.setFractionToRemove(new Double(quantile));
        
        String separationString = mSeparationField.getText();
        double separation = 0.0;
        try
        {
            separation = Double.parseDouble(separationString);
        }
        catch(NumberFormatException e)
        {
            handleMessage("The area separation threshold that you specified, \"" + separationString + "\", is not a valid floating-point number.",
                    "Invalid area separation threshold",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(separation <= 0.0 || separation >= 1.0)
        {
            handleMessage("The area separation threshold must be greater than 0.0, and less than 1.0.",
                    "Invalid area separation threshold",
                    JOptionPane.ERROR_MESSAGE);
            return false;            
        }
        pParams.setMinFractionalCostChange(new Double(separation));
        
        String smoothingLengthString = mSmoothingLengthField.getText();
        double smoothingLength = 0.0;
        try
        {
            smoothingLength = Double.parseDouble(smoothingLengthString);
        }
        catch(NumberFormatException e)
        {
            handleMessage("The smoothing length that you specified, \"" + smoothingLengthString + "\", is not a valid floating-point number.",
                    "Invalid smoothing length",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(smoothingLength <= 0.0)
        {
            handleMessage("The smoothing length that you specified, \"" + smoothingLength + "\", was not greater than 0.0.",
                          "Invalid smoothing length",
                          JOptionPane.ERROR_MESSAGE);
            return false;
        
        }
        pParams.setSmoothingLength(new Double(smoothingLength));
        
        String weightString = (String) mWeightBox.getSelectedItem();
        EvidenceWeightType weightType = EvidenceWeightType.get(weightString);
        if(null == weightType)
        {
            throw new IllegalStateException("unknown weight type: " + weightType);
        }
        pParams.setEvidenceWeightType(weightType);
        
        return true;
    }
    
    
    private void openFile(File pFile, DataFileDelimiter pDelimiter)
    {
        try
        {
            SignificancesData significancesData = new SignificancesData();
            FileReader fileReader = new FileReader(pFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            significancesData.loadFromFile(bufferedReader, pDelimiter);
            mSignificancesData = significancesData;
            SignificancesTableModel significancesTableModel = new SignificancesTableModel(significancesData);
            mSignificancesTable.setModel(significancesTableModel);
            mFileNameLabel.setText(pFile.getAbsolutePath());
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
            handleMessage("The file you specified, \"" + pFile.getAbsolutePath() + "\", does not conform to the comma-separated value format; the specific error message is: " + errorMessage,
                    "Invalid file format",
                    JOptionPane.ERROR_MESSAGE);            
            
        }
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
    

   
    private void setEnableStateForFields(boolean pFileLoaded, boolean pResultsObtained)
    {
        mLoadFileButton.setEnabled(! pFileLoaded);
        mDelimiterLabel.setEnabled(! pFileLoaded);
        mDelimiterBox.setEnabled(! pFileLoaded);
        mResetFormButton.setEnabled(pFileLoaded);
        mBinsLabel.setEnabled(pFileLoaded);
        mBinsField.setEnabled(pFileLoaded);
        mInitialCutoffLabel.setEnabled(pFileLoaded);
        mInitialCutoffField.setEnabled(pFileLoaded);
        mSmoothingLengthField.setEnabled(pFileLoaded);
        mSmoothingLengthLabel.setEnabled(pFileLoaded);
        mCombinedSignificanceLabel.setEnabled(pFileLoaded);
        mCombinedSignificanceField.setEnabled(pFileLoaded);
        mQuantileLabel.setEnabled(pFileLoaded);
        mQuantileField.setEnabled(pFileLoaded);
        mSeparationLabel.setEnabled(pFileLoaded);
        mSeparationField.setEnabled(pFileLoaded);
        mWeightLabel.setEnabled(pFileLoaded);
        mWeightBox.setEnabled(pFileLoaded);
        mInferButton.setEnabled(pFileLoaded);

        if(pFileLoaded)
        {
            mLoadFileButton.setToolTipText(null);
            mLoadFileButton.setToolTipText(TOOL_TIP_CLEAR_FILE);
            mInferButton.setToolTipText(TOOL_TIP_INFER_BUTTON);
            mWeightBox.setToolTipText(TOOL_TIP_WEIGHT_BOX);
            mBinsField.setToolTipText(TOOL_TIP_BINS_FIELD);
            mInitialCutoffField.setToolTipText(TOOL_TIP_INITIAL_CUTOFF_FIELD);
            mCombinedSignificanceField.setToolTipText(TOOL_TIP_COMBINED_EFFECTIVE_SIGNIFICANCE_FIELD);
            mQuantileField.setToolTipText(TOOL_TIP_QUANTILE_FIELD);
            mSeparationField.setToolTipText(TOOL_TIP_SEPARATION_FIELD);
            mSignificancesTable.setToolTipText(TOOL_TIP_SIGNIFICANCES);
            mSmoothingLengthField.setToolTipText(TOOL_TIP_SMOOTHING_LENGTH_FIELD);
            mResetFormButton.setToolTipText(TOOL_TIP_RESET_FORM);
        }
        else
        {
            mLoadFileButton.setToolTipText(TOOL_TIP_LOAD_FILE);
            mClearFileButton.setToolTipText(null);
            mInferButton.setToolTipText(null);
            mWeightBox.setToolTipText(null);
            mBinsField.setToolTipText(null);
            mInitialCutoffField.setToolTipText(null);
            mCombinedSignificanceField.setToolTipText(null);
            mQuantileField.setToolTipText(null);
            mSeparationField.setToolTipText(null);
            mSignificancesTable.setToolTipText(null);
            mSmoothingLengthField.setToolTipText(null);
            mResetFormButton.setToolTipText(null);
        }
        
        if(pResultsObtained)
        {
            mSaveResultsButton.setToolTipText("save the table of results to a text file using the same delimiter type used for the file of significance data");
            mClearResultsButton.setToolTipText("clear the results table");
            mAlphaLabel.setToolTipText(TOOL_TIP_ALPHA);
            mAlphaLabelData.setToolTipText(TOOL_TIP_ALPHA);
            mFinalSeparationLabel.setToolTipText(TOOL_TIP_SEPARATION);
            mFinalSeparationLabelData.setToolTipText(TOOL_TIP_SEPARATION);
            mIterationsLabel.setToolTipText(TOOL_TIP_ITERATIONS);
            mIterationsLabelData.setToolTipText(TOOL_TIP_ITERATIONS);
            mSaveWeightsButton.setToolTipText(TOOL_TIP_SAVE_WEIGHTS);
            mNumAffectedLabel.setToolTipText(TOOL_TIP_NUM_AFFECTED);
            mNumAffectedField.setToolTipText(TOOL_TIP_NUM_AFFECTED);            
        }
        else
        {
            mSaveResultsButton.setToolTipText(null);
            mClearResultsButton.setToolTipText(null);
            mAlphaLabel.setToolTipText(null);
            mAlphaLabelData.setToolTipText(null);
            mFinalSeparationLabel.setToolTipText(null);
            mFinalSeparationLabelData.setToolTipText(null);
            mIterationsLabel.setToolTipText(null);
            mIterationsLabelData.setToolTipText(null);
            mSaveWeightsButton.setToolTipText(null);
            mNumAffectedLabel.setToolTipText(null);
            mNumAffectedField.setToolTipText(null);            
        }
       
        mNumAffectedLabel.setEnabled(pResultsObtained);
        mNumAffectedField.setEnabled(pResultsObtained);
        mIterationsLabel.setEnabled(pResultsObtained);
        mIterationsLabelData.setEnabled(pResultsObtained);
        mFinalSeparationLabel.setEnabled(pResultsObtained);
        mFinalSeparationLabelData.setEnabled(pResultsObtained);
        mAlphaLabel.setEnabled(pResultsObtained);
        mAlphaLabelData.setEnabled(pResultsObtained);
        mSaveResultsButton.setEnabled(pResultsObtained);
        mClearResultsButton.setEnabled(pResultsObtained);
        mResultsStatisticsLabel.setEnabled(pResultsObtained);
        mSaveWeightsButton.setEnabled(pResultsObtained);
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
        JButton loadFileButton = new JButton("load sigs");
        mLoadFileButton = loadFileButton;
        JButton fileClear = new JButton("clear sigs");
        mClearFileButton = fileClear;
        fileClear.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                clearFile();
            }
        });
        loadFileButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                loadFile();
            }
                });
        fileButtonPanel.add(loadFileButton);
        fileButtonPanel.add(fileClear);
        JLabel delimitersLabel = new JLabel("delimiter: ");
        mDelimiterLabel = delimitersLabel;
        fileButtonPanel.add(delimitersLabel);
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
        fileButtonPanel.add(delimitersBox);
        
        topPanel.add(fileButtonPanel);
        gridLayout.setConstraints(fileButtonPanel, constraints);
        
        JLabel fileNameLabel = new JLabel("");
        JPanel fileNameLabelPanel = new JPanel();
        fileNameLabel.setPreferredSize(new Dimension(400, 10));
        fileNameLabel.setMinimumSize(new Dimension(400, 10));
        mFileNameLabel = fileNameLabel;
        fileNameLabelPanel.add(fileNameLabel);
        fileNameLabelPanel.setBorder(BorderFactory.createEtchedBorder());
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        //constraints.gridy = 0;  

        topPanel.add(fileNameLabelPanel);
        gridLayout.setConstraints(fileNameLabelPanel, constraints);
        
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
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 3;
        //constraints.gridy = 0;  
        
        appHelpButton.setToolTipText(TOOL_TIP_HELP);
        topPanel.add(appHelpButton);
        gridLayout.setConstraints(appHelpButton, constraints);
        
        JTable significancesTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(significancesTable);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.setPreferredSize(new Dimension(500, 250));
        scrollPane.setMaximumSize(new Dimension(500, 250));
        significancesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(scrollPane);
        gridLayout.setConstraints(scrollPane, constraints);
        
        mSignificancesTable = significancesTable;        
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;  
        
        JLabel binsLabel = new JLabel("Number of bins for significance distributions: ");
        mBinsLabel = binsLabel;
        topPanel.add(binsLabel);
        gridLayout.setConstraints(binsLabel, constraints);
        
        JTextField binsField = new JTextField(NUM_COLUMNS_NUMERIC_FIELD);
        mBinsField = binsField;
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        //constraints.gridy = 2;  
        
        topPanel.add(binsField);
        gridLayout.setConstraints(binsField, constraints);
        

        JLabel initialCutoffLabel = new JLabel("Initial quantile threshold for affected: ");
        mInitialCutoffLabel = initialCutoffLabel;
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 2;
        //constraints.gridy++;  
        
        topPanel.add(initialCutoffLabel);
        gridLayout.setConstraints(initialCutoffLabel, constraints);
        
        JTextField initialCutoffField = new JTextField(NUM_COLUMNS_NUMERIC_FIELD);
        mInitialCutoffField = initialCutoffField;

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 3;
        //constraints.gridy = 3;
        
        topPanel.add(initialCutoffField);
        gridLayout.setConstraints(initialCutoffField, constraints);
        
        JLabel combinedSignificanceLabel = new JLabel("Cutoff for combined significances: ");
        mCombinedSignificanceLabel = combinedSignificanceLabel;
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;  
        
        topPanel.add(combinedSignificanceLabel);
        gridLayout.setConstraints(combinedSignificanceLabel, constraints);
        
        JTextField combinedSignificanceField = new JTextField(NUM_COLUMNS_NUMERIC_FIELD);
        mCombinedSignificanceField = combinedSignificanceField;
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        //constraints.gridy = 4;
        
        topPanel.add(combinedSignificanceField);
        gridLayout.setConstraints(combinedSignificanceField, constraints);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 2;
        //constraints.gridy++;  
        
        JLabel quantileLabel = new JLabel("Fraction of elements to move in each iteration: ");
        mQuantileLabel = quantileLabel;
        topPanel.add(quantileLabel);
        gridLayout.setConstraints(quantileLabel, constraints);
        
        JTextField quantileField = new JTextField(NUM_COLUMNS_NUMERIC_FIELD);
        mQuantileField = quantileField;
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 3;
        //constraints.gridy = 5;
        
        topPanel.add(quantileField);
        gridLayout.setConstraints(quantileField, constraints);
        
        JLabel separationLabel = new JLabel("Area separation threshold (from unity): ");
        mSeparationLabel = separationLabel;
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;  
        
        topPanel.add(separationLabel);
        gridLayout.setConstraints(separationLabel, constraints);
        
        JTextField separationField = new JTextField(NUM_COLUMNS_NUMERIC_FIELD);
        mSeparationField = separationField;
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        //constraints.gridy = 6;
        
        topPanel.add(separationField);
        gridLayout.setConstraints(separationField, constraints);        
        
        JLabel smoothingLengthLabel = new JLabel("Smoothing length for significance distribution: ");
        mSmoothingLengthLabel = smoothingLengthLabel;
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 2;
        //constraints.gridy++;          

        topPanel.add(smoothingLengthLabel);
        gridLayout.setConstraints(smoothingLengthLabel, constraints);

        JTextField smoothingLengthField = new JTextField(NUM_COLUMNS_NUMERIC_FIELD);
        mSmoothingLengthField = smoothingLengthField;
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 3;
        //constraints.gridy = 5;
        
        topPanel.add(smoothingLengthField);
        gridLayout.setConstraints(smoothingLengthField, constraints);
        
        JLabel weightLabel = new JLabel("Effective significance weighting scheme: ");
        mWeightLabel = weightLabel;
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;          
        
        topPanel.add(weightLabel);
        gridLayout.setConstraints(weightLabel, constraints);
        
        EvidenceWeightType []weightTypes = EvidenceWeightType.getAll();
        int numWeightTypes = weightTypes.length;
        String []weightTypeNames = new String[numWeightTypes];
        for(int i = 0; i < numWeightTypes; ++i)
        {
            weightTypeNames[i] = weightTypes[i].getName();
        }
        JComboBox weightBox = new JComboBox(weightTypeNames);
        mWeightBox = weightBox;
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 1;
        //constraints.gridy = 7;
        
        topPanel.add(weightBox);
        gridLayout.setConstraints(weightBox, constraints);

        JPanel buttonPanel = new JPanel();
        
        JButton inferButton = new JButton("infer affected elements");
        inferButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                inferNetworkStructure();
            }
                });
        mInferButton = inferButton;
        buttonPanel.add(inferButton);
        
        JButton resetButton = new JButton("reset form");
        mResetFormButton = resetButton;
        resetButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                setDefaults();
            }
                });
        buttonPanel.add(resetButton);
        
        mSaveWeightsButton = new JButton("save weights");
        mSaveWeightsButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handleSaveWeights();
                    }
                });
        buttonPanel.add(mSaveWeightsButton);
        
        JButton saveResultsButton = new JButton("save results");
        mSaveResultsButton = saveResultsButton;
        saveResultsButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                handleSaveResults();
            }
                });
        buttonPanel.add(saveResultsButton);
        
        JButton clearResultsButton = new JButton("clear results");
        mClearResultsButton = clearResultsButton;
        clearResultsButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                handleClearResults();
            }
                });
        buttonPanel.add(clearResultsButton);
        

        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(buttonPanel);
        gridLayout.setConstraints(buttonPanel, constraints);
        
        JPanel resultsStatisticsPanel = new JPanel();
        resultsStatisticsPanel.setLayout(new BoxLayout(resultsStatisticsPanel, BoxLayout.Y_AXIS));
        
        mResultsStatisticsLabel = new JLabel("Results statistics:");
        resultsStatisticsPanel.add(mResultsStatisticsLabel);
        
        JPanel numericResultsPanel = new JPanel();
        numericResultsPanel.setBorder(BorderFactory.createEtchedBorder());
        numericResultsPanel.setLayout(new BoxLayout(numericResultsPanel, BoxLayout.Y_AXIS));
                
        JPanel iterationsPanel = new JPanel();
        JLabel iterationsLabel = new JLabel("num iterations: ");        
        mIterationsLabel = iterationsLabel;
        iterationsPanel.add(iterationsLabel);
        JPanel iterationsDataPanel = new JPanel();
        iterationsDataPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel iterationsLabelData = new JLabel("");
        iterationsLabelData.setPreferredSize(new Dimension(50, 10));
        iterationsLabelData.setMinimumSize(new Dimension(50, 10));
        iterationsDataPanel.add(iterationsLabelData);
        iterationsPanel.add(iterationsDataPanel);
        numericResultsPanel.add(iterationsPanel);
        mIterationsLabelData = iterationsLabelData;
        
        JPanel finalSeparationPanel = new JPanel();
        JLabel finalSeparationLabel = new JLabel("final separation: ");
        finalSeparationPanel.add(finalSeparationLabel);
        mFinalSeparationLabel = finalSeparationLabel;
        JLabel finalSeparationLabelData = new JLabel("");
        finalSeparationLabelData.setPreferredSize(new Dimension(75, 10));
        finalSeparationLabelData.setPreferredSize(new Dimension(75, 10));
        mFinalSeparationLabelData = finalSeparationLabelData;
        JPanel finalSeparationDataPanel = new JPanel();
        finalSeparationDataPanel.setBorder(BorderFactory.createEtchedBorder());
        finalSeparationDataPanel.add(finalSeparationLabelData);
        finalSeparationPanel.add(finalSeparationDataPanel);
        numericResultsPanel.add(finalSeparationPanel);
        
        JPanel alphaPanel = new JPanel();
        JLabel alphaLabel = new JLabel("alpha parameter: ");
        alphaPanel.add(alphaLabel);
        mAlphaLabel = alphaLabel;
        JLabel alphaLabelData = new JLabel("");
        JPanel alphaDataPanel = new JPanel();
        alphaDataPanel.setBorder(BorderFactory.createEtchedBorder());
        alphaLabelData.setPreferredSize(new Dimension(75, 10));
        alphaLabelData.setMinimumSize(new Dimension(75, 10));
        mAlphaLabelData = alphaLabelData;
        alphaDataPanel.add(alphaLabelData);
        alphaPanel.add(alphaDataPanel);
        numericResultsPanel.add(alphaPanel);
        
        JPanel numAffectedPanel = new JPanel();
        mNumAffectedLabel = new JLabel("num affected: ");
        numAffectedPanel.add(mNumAffectedLabel);
        JPanel numAffectedFieldPanel = new JPanel();
        numAffectedFieldPanel.setBorder(BorderFactory.createEtchedBorder());
        mNumAffectedField = new JLabel();
        mNumAffectedField.setPreferredSize(new Dimension(75, 10));
        mNumAffectedField.setMinimumSize(new Dimension(75, 10));
        numAffectedFieldPanel.add(mNumAffectedField);
        numAffectedPanel.add(numAffectedFieldPanel);
        numericResultsPanel.add(numAffectedPanel);
        
        resultsStatisticsPanel.add(numericResultsPanel);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;    
        
        topPanel.add(resultsStatisticsPanel);
        gridLayout.setConstraints(resultsStatisticsPanel, constraints);
        
        JTable weightsTable = new JTable();
        mWeightsTable = weightsTable;
        JScrollPane weightsScrollPane = new JScrollPane(weightsTable);
        weightsScrollPane.setPreferredSize(new Dimension(200, 100));
        weightsScrollPane.setMinimumSize(new Dimension(200, 100));
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 1;
        //constraints.gridy++;        
        
        topPanel.add(weightsScrollPane);
        gridLayout.setConstraints(weightsScrollPane, constraints);
        
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 2;
//        constraints.gridy = 9;
        
        // results table here
        
        JTable resultsTable = new JTable();
        mResultsTable = resultsTable;
        JScrollPane resultsScrollPane = new JScrollPane(resultsTable);
        resultsScrollPane.setBorder(BorderFactory.createEtchedBorder());
        resultsScrollPane.setPreferredSize(new Dimension(400, 200));
        
        topPanel.add(resultsScrollPane);
        gridLayout.setConstraints(resultsScrollPane, constraints);
        
        setEnableStateForFields(false, false);
        
        // set all form controls to their default values
        clearFile();
        
        mContentPane.add(topPanel);
    }
       
    private void handleResults(EvidenceWeightedInfererResults pResults)
    {
        mEvidenceWeightedInfererResults = pResults;
        InferenceResultsTableModel tableModel = new InferenceResultsTableModel(pResults, mSignificancesData);
        InferenceResultsMouseHandler mouseListener = new InferenceResultsMouseHandler(tableModel);
        mResultsTable.setModel(tableModel);
        JTableHeader resultsTableHeader = mResultsTable.getTableHeader();
        resultsTableHeader.addMouseListener(mouseListener);
        resultsTableHeader.setToolTipText("Click to sort; Shift-Click to sort in reverse order");
        mIterationsLabelData.setText(Integer.toString(pResults.mNumIterations));
        mFinalSeparationLabelData.setText(mNumberFormat.format(pResults.mSignificanceDistributionSeparation));
        double alphaValue = pResults.mAlphaParameter;
        mAlphaLabelData.setText(mNumberFormat.format(alphaValue));
        double []weights = pResults.mWeights;
        String []evidences = mSignificancesData.getEvidenceNames();
        WeightsTableModel weightsTableModel = new WeightsTableModel(evidences, weights);
        mWeightsTable.setModel(weightsTableModel);
        mNumAffectedField.setText(Integer.toString(pResults.mNumAffected));
        if(Math.abs(1.0 - alphaValue) > ALPHA_VALUE_DEVIATION_WARNING_THRESHOLD)
        {
            mAlphaLabelData.setForeground(Color.RED);
        }
        else
        {
            mAlphaLabelData.setForeground(Color.BLACK);
        }        
        setEnableStateForFields(true, true);
    }
    
    private void handleHelp()
    {
        String resourceHelpSet = mAppConfig.getAppHelpSetName();
        if(null != resourceHelpSet)
        {
            try
            {
                if(null == mHelpBrowser)
                {
                    mHelpBrowser = new HelpBrowser(mParent, resourceHelpSet, mProgramName);
                }
            //mHelpBrowser.displayHelpBrowser(HELP_SET_MAP_ID, null);  // :TODO: uncomment this, when setCurrentID() bug in JavaHelp is fixed
                mHelpBrowser.displayHelpBrowser(null, null);
            }
            catch(Exception e)
            {
                handleMessage("Unable to display help; error message is: " + e.getMessage(), "No help available", 
                              JOptionPane.INFORMATION_MESSAGE);            
            }
            
        }
        else
        {
            handleMessage("Sorry, no help is available in this version of the application", "No help available", 
                          JOptionPane.INFORMATION_MESSAGE);            
        }
    }
    
    private void handleClearResults()
    {
        mResultsTable.setModel(mEmptyTableModel);
        mWeightsTable.setModel(mEmptyTableModel);
        mEvidenceWeightedInfererResults = null;
        mAlphaLabelData.setText("");
        mIterationsLabelData.setText("");
        mFinalSeparationLabelData.setText("");
        setEnableStateForFields(null != mSignificancesData, false);
    }
    
    private void writeWeights(File pFile, DataFileDelimiter pDelimiter)
    {
        StringBuffer buf = new StringBuffer();
        String delimiter = pDelimiter.getDelimiter();
        buf.append(pDelimiter.scrubIdentifier(COLUMN_NAME_EVIDENCE) + delimiter + 
                   pDelimiter.scrubIdentifier(COLUMN_NAME_WEIGHT) + "\n");
        int j = 0; 
        double []weights = mEvidenceWeightedInfererResults.mWeights;
        int numEvidences = weights.length;
        String []evidenceNames = mSignificancesData.getEvidenceNames();
        for(j = 0; j < numEvidences; ++j)
        {
            buf.append(pDelimiter.scrubIdentifier(evidenceNames[j]) + delimiter + mNumberFormat.format(weights[j]) + "\n");
        }
        try
        {
            FileWriter fileWriter = new FileWriter(pFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(buf.toString());
            printWriter.flush();
            JOptionPane.showMessageDialog(mParent, 
                                          new SimpleTextArea("weights were written to the file \"" + pFile.getAbsolutePath() + "\" successfully."), 
                                          "weights written successfully", JOptionPane.INFORMATION_MESSAGE);
        }
        catch(IOException e)
        {
            SimpleTextArea simpleTextArea = new SimpleTextArea("unable to write the weights to the file you requested, \"" + pFile.getAbsolutePath() + "\"; specific error message is: " + e.getMessage());
            JOptionPane.showMessageDialog(mParent, simpleTextArea, "unable to write weights to file", JOptionPane.ERROR_MESSAGE);
        }        
    }
    
    private void writeResults(File pFile, DataFileDelimiter pDelimiter)
    {
        boolean []affectedElements = mEvidenceWeightedInfererResults.mAffectedElements;
        double []combinedEffectiveSignificances = mEvidenceWeightedInfererResults.mCombinedEffectiveSignificances;
        int numElements = affectedElements.length;
        StringBuffer resultsBuf = new StringBuffer();
        String delimiter = pDelimiter.getDelimiter();
        SignificancesData significancesData = mSignificancesData;
        String elementName = null;
        
        InferenceResultsTableModel tableModel = (InferenceResultsTableModel) mResultsTable.getModel();
        Integer []sortedIndices = tableModel.getSortedIndices();
        int index = 0;
        
        resultsBuf.append(pDelimiter.scrubIdentifier(COLUMN_NAME_ELEMENT) + delimiter + 
                          pDelimiter.scrubIdentifier(COLUMN_NAME_AFFECTED) + delimiter + 
                          pDelimiter.scrubIdentifier(COLUMN_NAME_OVERALL_SIGNIFICANCE) + "\n"); 
        for(int i = 0; i < numElements; ++i)
        {
            index = sortedIndices[i].intValue();
            elementName = pDelimiter.scrubIdentifier(significancesData.getElementName(index));
            resultsBuf.append(elementName + delimiter);
            resultsBuf.append(affectedElements[index] + delimiter);
            resultsBuf.append(mNumberFormat.format(combinedEffectiveSignificances[index]) + "\n");
        }

        try
        {
            FileWriter fileWriter = new FileWriter(pFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(resultsBuf.toString());
            printWriter.flush();
            JOptionPane.showMessageDialog(mParent, 
                                          new SimpleTextArea("results were written to the file \"" + pFile.getAbsolutePath() + "\" successfully."), 
                                          "results written successfully", JOptionPane.INFORMATION_MESSAGE);
        }
        catch(IOException e)
        {
            SimpleTextArea simpleTextArea = new SimpleTextArea("unable to write the results to the file you requested, \"" + pFile.getAbsolutePath() + "\"; specific error message is: " + e.getMessage());
            JOptionPane.showMessageDialog(mParent, simpleTextArea, "unable to write results to file", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private File selectOutputFile()
    {
        DataFileDelimiter delimiter = getDelimiter();
        FileChooser fileChooser = new FileChooser(mWorkingDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showSaveDialog(mParent);
        File outputFile = null;
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
                outputFile = selectedFile;
            }
        }        
        return outputFile;
    }
    
    private void handleSaveWeights()
    {
        File outputFile = selectOutputFile();
        if(null != outputFile)
        {
            writeWeights(outputFile, getDelimiter());
        }
    }
    
    private void handleSaveResults()
    {
        File outputFile = selectOutputFile();
        if(null != outputFile)
        {
            writeResults(outputFile, getDelimiter());
        }
    }
    
    private void inferNetworkStructure()
    {
        EvidenceWeightedInfererParams params = new EvidenceWeightedInfererParams();
        boolean proceed = validateFields(params);
        if(proceed)
        {
            DoubleMatrix2D significances = mSignificancesData.getSignificancesMatrix();
            
            EvidenceWeightedInfererResults results = null;
            
            try
            {
                results = mEvidenceWeightedInferer.findAffectedElements(significances,
                                                                        params);
            }
            catch(Exception e)
            {
                String errorMessage = e.getMessage();
                //e.printStackTrace(System.err); // for debugging only
                handleMessage("Unable to infer the set of affected elements from the data and parameters you supplied.  The specific error message is: " + errorMessage,
                              "Unable to infer affected elements",
                              JOptionPane.ERROR_MESSAGE);            
                return;
            }
           
            handleResults(results);
        }
    }    
    
    private void run(String []pArgs)
    {
        String appDir = null;
        if(pArgs.length > 0)
        {
            appDir = FileUtils.fixWindowsCommandLineDirectoryNameMangling(pArgs[0]);
        }        
        try
        {
            mAppConfig = AppConfig.get(EvidenceWeightedInferer.class, appDir);
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, "Unable to load application configuration data, system exiting.  Error message is: " + e.getMessage(),
                                          "Unable to load application configuration data",
                                          JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }        
        String frameName = mAppConfig.getAppName() + ": " + FRAME_NAME;

        JFrame frame = new JFrame(frameName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = frame.getContentPane();
        initialize(contentPane, frame, frameName);
        frame.pack();
        FramePlacer.placeInCenterOfScreen(frame);
        

        
        frame.setVisible(true);
    }
    
    public static final void main(String []pArgs)
    {
        // try to create an instance of this class
        try
        {
            EvidenceWeightedInfererDriver app = new EvidenceWeightedInfererDriver();
            app.run(pArgs);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
