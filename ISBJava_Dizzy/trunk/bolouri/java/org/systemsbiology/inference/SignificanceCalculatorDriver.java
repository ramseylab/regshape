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
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.io.*;
import cern.jet.stat.Descriptive;
import cern.colt.list.DoubleArrayList;
import javax.swing.table.AbstractTableModel;
import org.systemsbiology.data.DataFileDelimiter;
import org.systemsbiology.gui.*;
import org.systemsbiology.util.InvalidInputException;
import org.systemsbiology.math.ScientificNumberFormat;
import org.systemsbiology.math.SignificantDigitsCalculator;
import org.systemsbiology.data.MatrixString;
import java.util.ArrayList;

/**
 * Graphical user interface for calling the {@link SignificanceCalculator}
 * to obtain significances of observations.  Multiple types of evidence can
 * be processed in a single run of the program, with distinct parameter values
 * for the different types of evidence. The algorithms used in this class are 
 * based on the ideas and designs of Daehee Hwang at Institute for Systems Biology.
 *  
 * @author sramsey
 *
 */
public class SignificanceCalculatorDriver
{
    private static final String TOOL_TIP_FILE_LOAD_BUTTON = "Load the file of normalized observations.  The first column should be element names.  The column headers (first row) should be evidence names.";
    private static final String TOOL_TIP_FILE_CLEAR_BUTTON = "Clear the file of normalized observations that was loaded into this form.";
    private static final String TOOL_TIP_DELIMITER = "Indicates the type of separator that is used in the data file you want to load.";
    private static final String TOOL_TIP_HELP = "Display the help screen that explains how to use this program";
    private static final String RESOURCE_HELP_ICON = "Help24.gif";
    private static final String TOOL_TIP_OBSERVATIONS_TABLE = "This is the table of observations that you loaded from the data file.";
    private static final String TOOL_TIP_SET_NEGATIVE_CONTROL_DATA = "Select the file containing the negative control data for this evidence type.";
    private static final String TOOL_TIP_CLEAR_NEGATIVE_CONTROL_DATA = "Clear the negative control data for this evidence type.";
    private static final String TOOL_TIP_FORMULAS_BOX = "Specify the method (formula) used to compute the significance values";
    private static final String TOOL_TIP_NUM_BINS = "Specify the number of bins to be sued for computing the distribution of observations.";
    private static final String TOOL_TIP_MAX_CHI_SQUARE = "Specify the maximum allowed reduced chi-square for the best-fit distribution to the observations.";
    private static final String TOOL_TIP_CALCULATE_SIGNIFICANCES_BUTTON = "Calculate the significance values for all observations.  This is only enabled if at least one evidence has the \"calculate significances\" field set.";
    private static final String TOOL_TIP_TEXT_CLEAR_RESULTS_BUTTON = "Clear the calculated significances from the form.";
    private static final String TOOL_TIP_TEXT_SAVE_RESULTS_BUTTON = "Save the calculated significances to a file using the same delimiter type as the observations data file.";
    private static final String TOOL_TIP_RESET_FORM_BUTTON = "Reset the form to the original default values.";
    
    private static final double DEFAULT_MAX_CHI_SQUARE = 1.0;
    private static final int MIN_NUM_BINS = 10;
    private static final int NUM_COLUMNS_TEXT_FIELD_NUMERIC = 15;
    private static final SignificanceCalculationFormula DEFAULT_SIGNIFICANCE_CALCULATION_FORMULA = SignificanceCalculationFormula.CDF;    
    private static final DataFileDelimiter DEFAULT_DATA_FILE_DELIMITER = DataFileDelimiter.COMMA;
    private static final double DEFAULT_SMOOTHING_LENGTH = 1.0;
    
    private Component mParent;
    private Container mContentPane;
    private ScientificNumberFormat mNumberFormat;
    private JLabel mFormulaLabel;
    private JComboBox mFormulasBox;
    private JButton mCalcSigsButton;
    private JButton mResetFormButton;
    private JButton mSaveResultsButton;
    private JButton mClearResultsButton; 
    private ObservationsData mObservationsData;
    private JTable mObservationsTable;
    private JButton mFileLoadButton;
    private JButton mFileClearButton;
    private JLabel mDelimiterLabel;
    private JComboBox mDelimiterBox;
    private JLabel mFileNameLabel;
    private EmptyTableModel mEmptyTableModel;
    private File mWorkingDirectory;
    private SignificanceCalculationResults []mResults;
    private JTable mEvidenceChoicesTable;
    private SignificanceCalculator mSignificanceCalculator;
    private JTable mResultsTable;
    private TableColumn mColumnSmoothingLength;
    private TableColumn mColumnMaxChiSquare;
    
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
    
    static class EvidenceChoices
    {
        public Double []mObservationsData;
        public Double []mControlData;
        public SignificanceCalculatorParams mParams;
    }

    static class EvidenceChoicesTableModel extends AbstractTableModel
    {
    	public static final int COLUMN_EVIDENCE_NAME = 0;
        public static final int COLUMN_NUM_OBSERVATIONS = 1;
    	public static final int COLUMN_SINGLE_TAILED = 2;
    	public static final int COLUMN_NUM_BINS = 3;
        public static final int COLUMN_SMOOTHING_LENGTH = 4;
        public static final int COLUMN_MAX_CHI_SQUARE = 5;
    	public static final int COLUMN_COMPUTE_SIGNIFICANCES = 6;
    	public static final int COLUMN_NEGATIVE_CONTROLS = 7;
        
        public static final int COLUMN_SWITCHABLE = 4;
        
        private static final boolean []IS_CELL_EDITABLE = {false, false, true, true, true, true, true, false};
        private static final String []COLUMN_NAMES = {"evidence name",
                                                      "num observations",
                                                      "single-tailed",
                                                      "number of bins",
                                                      "smoothing length",
                                                      "max chi square",
                                                      "compute significances",
                                                      "negative control observations" };

        public static final String []TOOL_TIPS_EVIDENCE_CHOICES_TABLE = {"The evidence name",
                "The number of observations recorded for this evidence type.",
                "Specify whether the observations for this evidence are single-tailed (nonnegative) or two-tailed (positive and negative).",
                "Specify the number of bins to be used in generating the histogram of observations for this evidence type.",
                "Specify the smoothing length to be used for generating the nonparametric distribution of observations for this evidence type.",
                "Specify the maximum allowed reduced chi square for fitting a probability distribution to the observations for this evidence type.",
                "Compute significances for this evidence type; at least one evidence type must have this field enabled, in order to proceed.",
                "Specify (optionally) the negative control observations for this evidence.  Use the delete key to clear this field.  Double-click to set."};

        public static final String TOOL_TIP_NO_EDITING_SINGLE_TAILED_FIELD = "The single-tailed field cannot be edited for this evidence type, because this evidence has at least one negative observation and you have specified the PDF formula.";
        
        private ObservationsData mObservations;
        private NegativeControlData []mNegativeControlData;
        private Boolean []mSingleTailed;
        private Boolean []mComputeSignificances;
        private Integer []mNumBins;
        private Integer []mNumObservations;
        private Component mParent;
        private Double []mMaxChiSquare;
        private Double []mSmoothingLength;
        private SignificanceCalculationFormula mFormula;
        private double []mObsMins;
        private double []mObsMaxs;
        
        public EvidenceChoicesTableModel(ObservationsData pObservations, Component pParent)
        {
            mParent = pParent;
            mFormula = DEFAULT_SIGNIFICANCE_CALCULATION_FORMULA;
            mObservations = pObservations;
            int numEvidences = pObservations.getNumEvidences();
            mNegativeControlData = new NegativeControlData[numEvidences];
            mSingleTailed = new Boolean[numEvidences];
            mComputeSignificances = new Boolean[numEvidences];
            mNumBins = new Integer[numEvidences];
            mNumObservations = new Integer[numEvidences];
            mSmoothingLength = new Double[numEvidences];
            mMaxChiSquare = new Double[numEvidences];
            mObsMins = new double[numEvidences];
            mObsMaxs = new double[numEvidences];
            
            DoubleArrayList columnList = null;
            double min = 0.0;
            double max = 0.0;
            for(int j = 0; j < numEvidences; ++j)
            {
                columnList = mObservations.getNonMissingColumnVals(j);
                min = Descriptive.min(columnList);
                max = Descriptive.max(columnList);
                mObsMins[j] = min;
                mObsMaxs[j] = max;
            }
            setDefaultTableValues();
        }
        
        public void setSignificanceCalculationFormula(SignificanceCalculationFormula pFormula)
        {
            mFormula = pFormula;
        }
        
        public EvidenceChoices []getEvidenceChoices()
        {
            int numEvidences = mObservations.getNumEvidences();
            EvidenceChoices []evidenceChoicesArray = new EvidenceChoices[numEvidences];
            EvidenceChoices evidenceChoices = null;
            NegativeControlData negativeControlData = null;
            Double []controlData = null;
            for(int j = 0; j < numEvidences; ++j)
            {
                if(mComputeSignificances[j].booleanValue())
                {
                    evidenceChoices = new EvidenceChoices();
                    SignificanceCalculatorParams params = new SignificanceCalculatorParams();
                    params.setMaxReducedChiSquare(mMaxChiSquare[j]);
                    params.setNumBins(mNumBins[j]);
                    params.setSingleTailed(mSingleTailed[j]);
                    params.setSmoothingLength(mSmoothingLength[j]);
                    params.setSignificanceCalculationFormula(mFormula);
                    evidenceChoices.mParams = params;
                    evidenceChoices.mObservationsData = mObservations.getColumn(j);
                    negativeControlData = mNegativeControlData[j];
                    if(null != negativeControlData)
                    {
                        controlData = negativeControlData.mValues;
                    }
                    else
                    {
                        controlData = evidenceChoices.mObservationsData;
                    }
                    evidenceChoices.mControlData = controlData;
                }
                else
                {
                    evidenceChoices = null;
                }
                evidenceChoicesArray[j] = evidenceChoices;
            }
            return evidenceChoicesArray;
        }
                
        public boolean atLeastOneEvidenceSelected()
        {
            boolean atLeastOneEvidenceSelected = false;
            int numEvidences = mObservations.getNumEvidences();
            for(int j = 0; j < numEvidences; ++j)
            {
                if(mComputeSignificances[j].booleanValue())
                {
                    atLeastOneEvidenceSelected = true;
                    break;
                }
            }
            return atLeastOneEvidenceSelected;
        }
        
        public void setDefaultTableValues()
        {
            int numEvidences = mObservations.getNumEvidences();
            boolean hasNegatives = false;
            double columnMin = 0.0;
            double smoothingLength = 0.0;
            for(int j = 0; j < numEvidences; ++j)
            {
                mSingleTailed[j] = new Boolean(mObsMins[j] >= 0.0);
                mComputeSignificances[j] = new Boolean(true);
                int numObs = mObservations.getNumObservations(j);
                mNumObservations[j] = new Integer(numObs);
                int numBins = (int) Math.max(((double) numObs)/10.0, MIN_NUM_BINS); 
                mNumBins[j] = new Integer(numBins);
                mMaxChiSquare[j] = new Double(DEFAULT_MAX_CHI_SQUARE);
                smoothingLength = 10.0 * (mObsMaxs[j]-mObsMins[j])/((double) numBins);
                mSmoothingLength[j] = new Double(smoothingLength);
            }            
            fireTableDataChanged();
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
            return IS_CELL_EDITABLE.length;
        }
        
        public int getRowCount()
        {
            return mObservations.getNumEvidences();
        }
        
        public void setValueAt(Object pValue, int pRow, int pColumn)
        {
        	switch(pColumn)
			{
        	case COLUMN_EVIDENCE_NAME:
        		throw new IllegalStateException("cannot edit the evidence name");
        	
        	case COLUMN_NUM_OBSERVATIONS:
                throw new IllegalStateException("cannot edit the number of observations");
                
            case COLUMN_SINGLE_TAILED:
                mSingleTailed[pRow] = (Boolean) pValue;
                fireTableCellUpdated(pRow, pColumn);
                break;
                
            case COLUMN_NUM_BINS:
                Integer value = (Integer) pValue;
                if(value.intValue() <= 0)
                {
                    JOptionPane.showMessageDialog(mParent, 
                                 "The number of bins must be a positive number", "Invalid number of bins",
                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
            	mNumBins[pRow] = value;
            	fireTableCellUpdated(pRow, pColumn);
            	break;
            	
            case COLUMN_SMOOTHING_LENGTH:
                Double valueDbl = (Double) pValue;
                if(valueDbl.doubleValue() <= 0.0)
                {
                    JOptionPane.showMessageDialog(mParent,
                            "The smoothing length must be a positive number", "Invalid smoothing length",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                mSmoothingLength[pRow] = valueDbl;
                break;
                
            case COLUMN_MAX_CHI_SQUARE:
                valueDbl = (Double) pValue;
                if(valueDbl.doubleValue() <= 0.0)
                {
                    JOptionPane.showMessageDialog(mParent,
                            "The max chi square must be a positive number", "Invalid max chi square",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                mMaxChiSquare[pRow] = valueDbl;
                break;
                
                
            case COLUMN_COMPUTE_SIGNIFICANCES:
            	mComputeSignificances[pRow] = (Boolean) pValue;
                fireTableCellUpdated(pRow, pColumn);
                break;

            case COLUMN_NEGATIVE_CONTROLS:
      			throw new IllegalStateException("cannot edit the negative control field");

            
            default:
            	throw new IllegalStateException("cannot edit unknown column type: " + pColumn);
            		
			}
        }
        
        public boolean isCellEditable(int pRow, int pColumn)
        {
            switch(pColumn)
            {
            case COLUMN_SINGLE_TAILED:
                return (mObsMins[pRow] >= 0.0 || !(mFormula.equals(SignificanceCalculationFormula.PDF)));
                
            default:
                return IS_CELL_EDITABLE[pColumn];
            }
        }
        
        public String getColumnName(int pColumn)
        {
        	return COLUMN_NAMES[pColumn];
        }
        
        public Object getValueAt(int pRow, int pColumn)
        {
            switch(pColumn)
            {
            case COLUMN_EVIDENCE_NAME:
                return mObservations.getEvidenceName(pRow);
            
            case COLUMN_NUM_OBSERVATIONS:
                return mNumObservations[pRow];
                
            case COLUMN_SINGLE_TAILED:
                return mSingleTailed[pRow];
            
            case COLUMN_NUM_BINS:
            	return mNumBins[pRow];
            
            case COLUMN_SMOOTHING_LENGTH:
                return mSmoothingLength[pRow];
                
            case COLUMN_MAX_CHI_SQUARE:
                return mMaxChiSquare[pRow];
                
            case COLUMN_COMPUTE_SIGNIFICANCES:
            	return mComputeSignificances[pRow];
            	
            case COLUMN_NEGATIVE_CONTROLS:
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
                
            default:
                throw new IllegalStateException("unexpected column number: " + pColumn);
            }
        }
        
        
        public String getToolTipText(int pRow, int pColumn)
        {
        	if(pRow >= mObsMins.length)
        	{
        		throw new IllegalStateException("invalid row: " + pRow);
        	}
        	if(pColumn == -1 || pColumn > COLUMN_NEGATIVE_CONTROLS)
        	{
        		throw new IllegalStateException("invalid column: " + pColumn);
        	}
        	String tip = null;
            
            if(pRow != -1)
            {
                switch(pColumn)
                {                    
                case COLUMN_SINGLE_TAILED:
                    if(mObsMins[pRow] < 0.0 && mFormula.equals(SignificanceCalculationFormula.PDF))
                    {
                        tip = TOOL_TIP_NO_EDITING_SINGLE_TAILED_FIELD;
                    }
                    else
                    {
                        tip = TOOL_TIPS_EVIDENCE_CHOICES_TABLE[pColumn];
                    }
                    break;
                    
                default:
                    tip = TOOL_TIPS_EVIDENCE_CHOICES_TABLE[pColumn];
                break;
                }
            }
            else
            {
                tip = TOOL_TIPS_EVIDENCE_CHOICES_TABLE[pColumn];
            }

        	return tip;
        }        
    }
    
    public SignificanceCalculatorDriver()
    {
        mSignificanceCalculator = new SignificanceCalculator();
        mNumberFormat = new ScientificNumberFormat(new SignificantDigitsCalculator());
    }
    
    public void initialize(Container pContentPane, Component pParent)
    {
        mContentPane = pContentPane;
        
        mParent = pParent;
        mEmptyTableModel = new EmptyTableModel();
        mWorkingDirectory = null;
        mObservationsData = null;
        
        initializeContentPane();
    }
    
    private void setToolTipsForFields(boolean pFileLoaded, boolean pResultsObtained)
    {
        if(pFileLoaded)
        {
            mFileLoadButton.setToolTipText(null);
            mFileClearButton.setToolTipText(TOOL_TIP_FILE_CLEAR_BUTTON);
            mObservationsTable.setToolTipText(TOOL_TIP_OBSERVATIONS_TABLE);
            mFormulaLabel.setToolTipText(TOOL_TIP_FORMULAS_BOX);
            mFormulasBox.setToolTipText(TOOL_TIP_FORMULAS_BOX);
            mCalcSigsButton.setToolTipText(TOOL_TIP_CALCULATE_SIGNIFICANCES_BUTTON);
            mResetFormButton.setToolTipText(TOOL_TIP_RESET_FORM_BUTTON);
        }
        else
        {
            mFileLoadButton.setToolTipText(TOOL_TIP_FILE_LOAD_BUTTON);
            mFileClearButton.setToolTipText(null);
            mObservationsTable.setToolTipText(null);
            mFormulaLabel.setToolTipText(null);
            mFormulasBox.setToolTipText(null);
            mCalcSigsButton.setToolTipText(null);
            mResetFormButton.setToolTipText(null);
        }
        if(pResultsObtained)
        {
            mClearResultsButton.setToolTipText(TOOL_TIP_TEXT_CLEAR_RESULTS_BUTTON);
            mSaveResultsButton.setToolTipText(TOOL_TIP_TEXT_SAVE_RESULTS_BUTTON);
        }
        else
        {
            mClearResultsButton.setToolTipText(null);
            mSaveResultsButton.setToolTipText(null);
        }
    }
    
    private void setEnableStateForFields(boolean pFileLoaded, boolean pResultsObtained)
    {
        mFileLoadButton.setEnabled(! pFileLoaded);
        mFileClearButton.setEnabled(pFileLoaded);
        mFormulaLabel.setEnabled(pFileLoaded);
        mFormulasBox.setEnabled(pFileLoaded);
        mCalcSigsButton.setEnabled(pFileLoaded && ((EvidenceChoicesTableModel) mEvidenceChoicesTable.getModel()).atLeastOneEvidenceSelected());
        mClearResultsButton.setEnabled(pResultsObtained);
        mSaveResultsButton.setEnabled(pResultsObtained);
        mResetFormButton.setEnabled(pFileLoaded);
    }
    
    private void handleMessage(String pMessage, String pTitle, int pMessageType)
    {
        SimpleTextArea simpleArea = new SimpleTextArea(pMessage);
        JOptionPane.showMessageDialog(mParent, simpleArea, pTitle, pMessageType);
    }
    
    private void setDefaults()
    {
        clearResults();
        mFormulasBox.setSelectedItem(DEFAULT_SIGNIFICANCE_CALCULATION_FORMULA.getName());
        TableModel tableModel = mEvidenceChoicesTable.getModel();
        if(tableModel instanceof EvidenceChoicesTableModel)
        {
            ((EvidenceChoicesTableModel) tableModel).setDefaultTableValues();
            TableCellEditor cellEditor = mEvidenceChoicesTable.getCellEditor();
            if(null != cellEditor)
            {
                cellEditor.cancelCellEditing();
            }
        }
    }
    
    private void initializeEvidenceChoicesTable()
    {
        EvidenceChoicesTableModel evidenceChoicesTableModel = new EvidenceChoicesTableModel(mObservationsData, mParent);
        mEvidenceChoicesTable.setModel(evidenceChoicesTableModel);
        TableColumnModel columnModel = mEvidenceChoicesTable.getColumnModel();
        mColumnMaxChiSquare = columnModel.getColumn(EvidenceChoicesTableModel.COLUMN_MAX_CHI_SQUARE);
        mColumnSmoothingLength = columnModel.getColumn(EvidenceChoicesTableModel.COLUMN_SMOOTHING_LENGTH);
        columnModel.removeColumn(mColumnMaxChiSquare);
        columnModel.removeColumn(mColumnSmoothingLength);
        setSignificanceCalculationFormula(DEFAULT_SIGNIFICANCE_CALCULATION_FORMULA);
    }
    
    private SignificanceCalculationFormula getSignificanceCalculationFormula()
    {
        SignificanceCalculationFormula formula = null;
        String formulaName = (String) mFormulasBox.getSelectedItem();
        formula = SignificanceCalculationFormula.get(formulaName);
        if(null == formula)
        {
            throw new IllegalStateException("unrecognized significance calculation formula name: " + formulaName);
        }
        return formula;
    }
    
    private void openFile(File pFile, DataFileDelimiter pDelimiter)
    {
        try
        {
            ObservationsData observationsData = new ObservationsData();
            FileReader fileReader = new FileReader(pFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            observationsData.loadFromFile(bufferedReader, pDelimiter);
            mObservationsData = observationsData;
            ObservationsTableModel observationsTableModel = new ObservationsTableModel(observationsData);
            mObservationsTable.setModel(observationsTableModel);
            mFileNameLabel.setText(pFile.getAbsolutePath());
            initializeEvidenceChoicesTable();
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
        DataFileDelimiter delimiter = DataFileDelimiter.get(delimiterName);
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
        
    private void clearResults()
    {
        mResultsTable.setModel(mEmptyTableModel);
        setEnableStateForFields(null != mObservationsData, false);
        setToolTipsForFields(null != mObservationsData, false);
    }
    
    private void setSignificanceCalculationFormula(SignificanceCalculationFormula pFormula)
    {
        TableColumnModel columnModel = mEvidenceChoicesTable.getColumnModel();
        if(pFormula.equals(SignificanceCalculationFormula.PDF))
        {
            columnModel.removeColumn(mColumnSmoothingLength);
            columnModel.addColumn(mColumnMaxChiSquare);
        }
        else if(pFormula.equals(SignificanceCalculationFormula.CDF))
        {
            columnModel.removeColumn(mColumnMaxChiSquare);
            columnModel.addColumn(mColumnSmoothingLength);
        }
        else
        {
            throw new IllegalStateException("unknown significance calculation formula: " + pFormula.getName());
        }
        columnModel.moveColumn(columnModel.getColumnCount() - 1, EvidenceChoicesTableModel.COLUMN_SWITCHABLE);
        EvidenceChoicesTableModel tableModel = (EvidenceChoicesTableModel) mEvidenceChoicesTable.getModel();
        tableModel.setSignificanceCalculationFormula(pFormula);
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
        fileNameLabel.setPreferredSize(new Dimension(400, 10));
        fileNameLabel.setMinimumSize(new Dimension(400, 10));
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
        observationsScrollPane.setPreferredSize(new Dimension(500, 200));
        observationsScrollPane.setMinimumSize(new Dimension(500, 200));
        observationsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(observationsScrollPane);
        gridLayout.setConstraints(observationsScrollPane, constraints);
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.3;
        constraints.gridx = 0;
        constraints.gridy++;
        
        JTable evidenceChoicesTable = new JTable()
		{
        	public String getToolTipText(MouseEvent e)
        	{
        		TableModel model = getModel();
        		if(model instanceof EvidenceChoicesTableModel)
        		{
                    java.awt.Point p = e.getPoint();
                    int rowIndex = rowAtPoint(p);
                    int colIndex = columnAtPoint(p);
                    int realColumnIndex = convertColumnIndexToModel(colIndex);
                    EvidenceChoicesTableModel evidenceChoicesTableModel = (EvidenceChoicesTableModel) model;
                    return evidenceChoicesTableModel.getToolTipText(rowIndex, realColumnIndex);
        		}
        		else
        		{
        			return super.getToolTipText(e);
        		}
        	}
        	
        	protected JTableHeader createDefaultTableHeader()
        	{
        		TableModel tableModel = getModel();
        		if(null == tableModel || tableModel instanceof EmptyTableModel)
        		{
        			return super.createDefaultTableHeader();
        		}
        		else
        		{
        			return new JTableHeader(columnModel)
					{
        	        	public String getToolTipText(MouseEvent e)
        	        	{
        	        		JTable table = getTable();
        	        		TableModel tableModel = table.getModel();
        	        		if(null == tableModel || tableModel instanceof EmptyTableModel)
        	        		{
        	        			return super.getToolTipText(e);
        	        		}
        	        		else
        	        		{
        	                    java.awt.Point p = e.getPoint();
        	                    int index = columnModel.getColumnIndexAtX(p.x);
        	                    int realIndex = 
        	                            columnModel.getColumn(index).getModelIndex();
        	                    return EvidenceChoicesTableModel.TOOL_TIPS_EVIDENCE_CHOICES_TABLE[realIndex];
        	        		}
        	        	}
					};
        		}
        	}
		};
        mEvidenceChoicesTable = evidenceChoicesTable;
        evidenceChoicesTable.setColumnSelectionAllowed(true);
        evidenceChoicesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        JScrollPane evidenceChoicesScrollPane = new JScrollPane(evidenceChoicesTable);
        evidenceChoicesScrollPane.setPreferredSize(new Dimension(350, 100));
        evidenceChoicesScrollPane.setMinimumSize(new Dimension(350, 100));
        evidenceChoicesTable.addMouseListener(
                new MouseAdapter()
	            {
	                public void mouseClicked(MouseEvent e)
	                {
	                	if(e.getClickCount() == 2 &&
	                	   mEvidenceChoicesTable.getSelectedColumn() == EvidenceChoicesTableModel.COLUMN_NEGATIVE_CONTROLS)
	                	{
	                		setNegativeControlFile();
	                	}
	                }
	            });
        evidenceChoicesTable.addKeyListener(
        		new KeyAdapter()
				{
        			public void keyPressed(KeyEvent e)
        			{
        				if(e.getKeyCode() == KeyEvent.VK_DELETE ||
        				   e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
        				{
        					if(mEvidenceChoicesTable.getSelectedColumn() == EvidenceChoicesTableModel.COLUMN_NEGATIVE_CONTROLS)
        					{
        						clearNegativeControlFile();
        					}
        				}
        			}
				});
        topPanel.add(evidenceChoicesScrollPane);
        gridLayout.setConstraints(evidenceChoicesScrollPane, constraints);
        

        JPanel choicesPanel = new JPanel();
        
        JLabel formulaLabel = new JLabel("Calculate the significances using the formula: ");
        choicesPanel.add(formulaLabel);
        mFormulaLabel = formulaLabel;
        SignificanceCalculationFormula []formulas = SignificanceCalculationFormula.getAll();
        int numFormulas = formulas.length;
        String []formulaNames = new String[numFormulas];
        for(int i = 0; i < numFormulas; ++i)
        {
        	formulaNames[i] = formulas[i].getName();
        }
        JComboBox formulasBox = new JComboBox(formulaNames);
        formulasBox.addItemListener(
                new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        if(e.getStateChange() == ItemEvent.SELECTED)
                        {
                            String formulaStr = (String) mFormulasBox.getSelectedItem();
                            SignificanceCalculationFormula formula = SignificanceCalculationFormula.get(formulaStr);
                            if(null == formula)
                            {
                                throw new IllegalStateException("unrecognized significance calculation formula name: " + formulaStr);
                            }
                            setSignificanceCalculationFormula(formula);
                        }
                    }
                });
        choicesPanel.add(formulasBox);
        mFormulasBox = formulasBox;
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(choicesPanel);
        gridLayout.setConstraints(choicesPanel, constraints);
        
        JPanel controlsPanel = new JPanel();
        mCalcSigsButton = new JButton("calculate significances");
        mCalcSigsButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        calculateSignificances();
                    }
                });
        controlsPanel.add(mCalcSigsButton);
        mResetFormButton = new JButton("reset form");
        mResetFormButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        setDefaults();
                    }
                });
        controlsPanel.add(mResetFormButton);
        mSaveResultsButton = new JButton("save results");
        mSaveResultsButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        saveResults();
                    }
                });
        controlsPanel.add(mSaveResultsButton);
        mClearResultsButton = new JButton("clear results");
        controlsPanel.add(mClearResultsButton);
        mClearResultsButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        clearResults();
                    }
                });

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(controlsPanel);
        gridLayout.setConstraints(controlsPanel, constraints);
        
        mResultsTable = new JTable();
        JScrollPane resultsScrollPane = new JScrollPane(mResultsTable);
        resultsScrollPane.setPreferredSize(new Dimension(500, 200));
        resultsScrollPane.setMinimumSize(new Dimension(500, 200));
        resultsScrollPane.setBorder(BorderFactory.createEtchedBorder());
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy++;

        topPanel.add(resultsScrollPane);
        gridLayout.setConstraints(resultsScrollPane, constraints);
        
        // add components to the topPanel
        
        mContentPane.add(topPanel);

        clearFile();
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
            tableModel.fireTableCellUpdated(mEvidenceNum, EvidenceChoicesTableModel.COLUMN_NEGATIVE_CONTROLS);
        }
    }
    
    private void calculateSignificances()
    {
        EvidenceChoicesTableModel tableModel = (EvidenceChoicesTableModel) mEvidenceChoicesTable.getModel();
        EvidenceChoices []evidenceChoicesArray = tableModel.getEvidenceChoices();

        EvidenceChoices evidenceChoices = null;
        int numEvidences = evidenceChoicesArray.length;
        SignificanceCalculationFormula sigCalcFormula = getSignificanceCalculationFormula();

        
        mResults = new SignificanceCalculationResults[numEvidences];
        int evidenceCtr = 0;
        try
        {
            for(; evidenceCtr < numEvidences; ++evidenceCtr)
            {
                evidenceChoices = evidenceChoicesArray[evidenceCtr];
                if(null != evidenceChoices)
                {
                    mResults[evidenceCtr] = mSignificanceCalculator.calculateSignificances(evidenceChoices.mObservationsData,
                                                                                           evidenceChoices.mControlData,
                                                                                           evidenceChoices.mParams);
                }
                else
                {
                    mResults[evidenceCtr] = null;
                }
            }
            handleResults();
        }
        catch(Exception e)
        {
            String evidenceName = mObservationsData.getEvidenceName(evidenceCtr);
            mResults = null;
            handleMessage("Sorry, the significance calculation failed, for evidence \"" + evidenceName + "\"; the specific error message is: " + e.getMessage(),
                          "Significance calculation failed",
                          JOptionPane.ERROR_MESSAGE);
            return;
        }
    }
    
    class ResultsTableModel extends AbstractTableModel
    {
        private SignificanceCalculationResults []mResultsArray;
        private int mNumElements;
        private String []mEvidenceNames;
        private String []mElementNames;
        
        public ResultsTableModel(SignificanceCalculationResults []pResultsArray, 
                                 ObservationsData pObservationsData)
        {
            mElementNames = pObservationsData.getElementNames();
            int numResults = 0;
            int numEvidences = pResultsArray.length;
            ArrayList resultsList = new ArrayList();
            ArrayList evidencesList = new ArrayList();
            int numElements = 0;
            for(int j = 0; j < numEvidences; ++j)
            {
                if(null != pResultsArray[j])
                {
                    resultsList.add(pResultsArray[j]);
                    evidencesList.add(pObservationsData.getEvidenceName(j));
                    numElements = pResultsArray[j].mSignificances.length;
                    if(0 == mNumElements)
                    {
                        mNumElements = numElements;
                    }
                    else
                    {
                        if(numElements != mNumElements)
                        {
                            throw new IllegalStateException("inconsistent number of elements in significance results");
                        }
                    }
                }
            }
            mResultsArray = (SignificanceCalculationResults []) resultsList.toArray(new SignificanceCalculationResults[0]);
            mEvidenceNames = (String []) evidencesList.toArray(new String[0]);
        }
        
        public SignificanceCalculationResults []getResults()
        {
            return mResultsArray;
        }
        
        public String []getEvidenceNames()
        {
            return mEvidenceNames;
        }
        
        public String []getElementNames()
        {
            return mElementNames;
        }
            
        public Object getValueAt(int pRow, int pColumn)
        {
            if(0 == pColumn)
            {
                return mElementNames[pRow];
            }
            else
            {
                double value = mResultsArray[pColumn - 1].mSignificances[pRow];
                if(value >= 0.0)
                {
                    return mNumberFormat.format(mResultsArray[pColumn - 1].mSignificances[pRow]);
                }
                else
                {
                    return "";
                }
            }
        }
        
        public int getColumnCount()
        {
            return mResultsArray.length + 1;
        }
        
        public int getRowCount()
        {
            return mNumElements;
        }
        
        public String getColumnName(int pColumn)
        {
            if(0 == pColumn)
            {
                return ObservationsTableModel.COLUMN_NAME_ELEMENT;
            }
            else
            {
                return mEvidenceNames[pColumn - 1];
            }
        }
    }
    
    private void handleResults()
    {
        ResultsTableModel tableModel = new ResultsTableModel(mResults, mObservationsData);
        mResultsTable.setModel(tableModel);
        setEnableStateForFields(true, true);
        setToolTipsForFields(true, true);
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
            tableModel.fireTableCellUpdated(selectedRow, EvidenceChoicesTableModel.COLUMN_NEGATIVE_CONTROLS);
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
        clearResults();
        setEnableStateForFields(false, false);
        setToolTipsForFields(false, false);
        mDelimiterBox.setSelectedItem(DEFAULT_DATA_FILE_DELIMITER.getName());
        setDefaults();
    }
    
    private void saveResults()
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
                writeResults(selectedFile, delimiter);
            }
        }
    }
        
    private void writeResults(File pSelectedFile, DataFileDelimiter pDelimiter)
    {
        ResultsTableModel tableModel = (ResultsTableModel) mResultsTable.getModel();
        String []evidences = tableModel.getEvidenceNames();
        String []elements = tableModel.getElementNames();
        SignificanceCalculationResults []results = tableModel.getResults();
        
        
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
        double sig = 0.0;
        String elementName = null;
        for(int i = 0; i < numElements; ++i)
        {
            elementName = elements[i];
            if(-1 != elementName.indexOf(delimiter)) 
            {
                elementName = elementName.replaceAll(delimiter, "_");
            }
            sb.append(elementName + delimiter);
            for(int j = 0; j < numEvidences; ++j)
            {
                sig = results[j].mSignificances[i];
                if(sig >= 0.0)
                {
                    sb.append(mNumberFormat.format(sig));
                }
                else
                {
                    if(! pDelimiter.getSingle())
                    {
                        sb.append("-1");
                    }
                    else
                    {
                        // do nothing; just skip this 
                    }
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
                                          new SimpleTextArea("results were written to the file \"" + pSelectedFile.getAbsolutePath() + "\" successfully."), 
                                          "results written successfully", JOptionPane.INFORMATION_MESSAGE);
        }
        catch(IOException e)
        {
            SimpleTextArea simpleTextArea = new SimpleTextArea("unable to write the results to the file you requested, \"" + pSelectedFile.getAbsolutePath() + "\"; specific error message is: " + e.getMessage());
            JOptionPane.showMessageDialog(mParent, simpleTextArea, "unable to write results to file", JOptionPane.ERROR_MESSAGE);
        }        
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
