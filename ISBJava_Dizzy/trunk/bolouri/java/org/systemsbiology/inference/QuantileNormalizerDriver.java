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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import cern.colt.matrix.*;
import javax.swing.*;
import org.systemsbiology.data.DataFileDelimiter;
import org.systemsbiology.gui.*;
import org.systemsbiology.math.*;
import java.text.NumberFormat;
import org.systemsbiology.util.InvalidInputException;

/**
 * A graphical user interface for normalizing raw observations using the
 * {@link QuantileNormalizer}.  The data may be optionally rescaled before
 * the normalization, using the {@link QuantileNormalizationScale} parameter
 * to the {@link QuantileNormalizerParams} object. The algorithms used in this 
 * class are based on the ideas and designs of Daehee Hwang at Institute for 
 * Systems Biology.
 * 
 * @author sramsey
 *
 */
public class QuantileNormalizerDriver
{
    private Container mContentPane;
    private Component mParent;
    private ObservationsData mObservationsData;
    private ObservationsData mResultsData;
    private File mWorkingDirectory;
    private EmptyTableModel mEmptyTableModel;
    private NumberFormat mNumberFormat;
    private QuantileNormalizer mQuantileNormalizer;
    
    private JTable mObservationsTable;
    private JLabel mFileNameLabel;
    private JComboBox mDelimiterBox;
    private JComboBox mScalesBox;
    private JButton mFileLoadButton;
    private JButton mFileClearButton;
    private JLabel mDelimiterLabel;
    private JLabel mScalesLabel;
    private JButton mNormalizeButton;
    private JButton mResetFormButton;
    private JButton mClearResultsButton;
    private JButton mSaveResultsButton;
    private JTable mResultsTable;
    private JLabel mErrorToleranceLabel;
    private JTextField mErrorToleranceField;
    private JLabel mFixNegativesLabel;
    private JCheckBox mFixNegativesBox;
    private JLabel mIterationCountLabel;
    private JLabel mIterationCountText;
    private JLabel mErrorLabel;
    private JLabel mErrorText;
    
    private static final double DEFAULT_ERROR_TOLERANCE = 1.0e-3;
    private static final String TOOL_TIP_ERROR_TOLERANCE = "Specify the (optional) error tolerance for exiting the iterative normalization procedure; if left blank, only one iteration will be run.";
    private static final String TOOL_TIP_SAVE_RESULTS_BUTTON = "Save the normalized observations to a file.";
    private static final String TOOL_TIP_CLEAR_RESULTS_BUTTON = "Clear the normalized observations from this form.";
    private static final String TOOL_TIP_RESET_FORM_BUTTON = "Reset the form to the default values.";
    private static final String TOOL_TIP_NORMALIZE_BUTTON = "Normalize the raw observations.";
    private static final String TOOL_TIP_METHODS_BOX = "Specify the scale to be used for normalizing the raw observations.";
    private static final String TOOL_TIP_FILE_LOAD_BUTTON = "Load the file of normalized observations.  The first column should be element names.  The column headers (first row) should be evidence names.";
    private static final String TOOL_TIP_FILE_CLEAR_BUTTON = "Clear the file of normalized observations that was loaded into this form.";
    private static final String TOOL_TIP_DELIMITER = "Indicates the type of separator that is used in the data file you want to load.";
    private static final String TOOL_TIP_HELP = "Display the help screen that explains how to use this program";
    private static final String RESOURCE_HELP_ICON = "Help24.gif";
    private static final String TOOL_TIP_OBSERVATIONS_TABLE = "This is the table of observations that you loaded from the data file.";
    private static final String TOOL_TIP_FIX_NEGATIVES = "Shift all observations by an additive constant to ensure no observation is less than or equal to zero.";
    private static final String TOOL_TIP_ITERATION_COUNT = "The number of iterations the normalization algorithm needed in order to converge, to the requested accuracy.";
    private static final String TOOL_TIP_FINAL_ERROR = "The final fractional error, for missing data estimation, at the point where the normalization algorithm exited.";
            
    private static final QuantileNormalizationScale DEFAULT_QUANTILE_NORMALIZATION_METHOD = QuantileNormalizationScale.NORM_ONLY;
    private static final DataFileDelimiter DEFAULT_DATA_FILE_DELIMITER = DataFileDelimiter.COMMA;
    private static final int NUM_COLUMNS_TEXT_FIELD_NUMERIC = 15;

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
        observationsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane observationsScrollPane = new JScrollPane(observationsTable);
        observationsScrollPane.setBorder(BorderFactory.createEtchedBorder());
        observationsScrollPane.setPreferredSize(new Dimension(500, 200));
        observationsScrollPane.setMaximumSize(new Dimension(500, 200));
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(observationsScrollPane);
        gridLayout.setConstraints(observationsScrollPane, constraints);
        
        JPanel controlsPanel = new JPanel();
        JPanel scalesPanel = new JPanel();
        mScalesLabel = new JLabel("Normalize based on quantiles using the scale: ");
        scalesPanel.add(mScalesLabel);
        QuantileNormalizationScale []scales = QuantileNormalizationScale.getAll();
        int numScales = scales.length;
        String []scaleNames = new String[numScales];
        for(int i = 0; i < numScales; ++i)
        {
            scaleNames[i] = scales[i].getName();
        }
        mScalesBox = new JComboBox(scaleNames);
        mScalesBox.addItemListener(
                new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        if(e.getStateChange() == ItemEvent.SELECTED)
                        {
                            setDefaultForFixNegatives();
                        }
                    }
                });

        scalesPanel.add(mScalesBox);
        controlsPanel.add(scalesPanel);
        mErrorToleranceLabel = new JLabel("Specify the error tolerance: ");
        mErrorToleranceField = new JTextField(NUM_COLUMNS_TEXT_FIELD_NUMERIC);
        JPanel errorTolerancePanel = new JPanel();
        errorTolerancePanel.add(mErrorToleranceLabel);
        errorTolerancePanel.add(mErrorToleranceField);
        controlsPanel.add(errorTolerancePanel);
        
        JPanel fixNegativesPanel = new JPanel();
        mFixNegativesLabel = new JLabel("Fix negative values: ");
        mFixNegativesBox = new JCheckBox();
        fixNegativesPanel.add(mFixNegativesLabel);
        fixNegativesPanel.add(mFixNegativesBox);
        controlsPanel.add(fixNegativesPanel);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(controlsPanel);
        gridLayout.setConstraints(controlsPanel, constraints);
        
        // create panel of buttons
        JPanel buttonsPanel = new JPanel();
        mNormalizeButton = new JButton("normalize raw observations");
        mNormalizeButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        normalizeRawObservations();
                    }
                });
        buttonsPanel.add(mNormalizeButton);
        mResetFormButton = new JButton("reset form");
        mResetFormButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        setDefaults();
                    }
                });        
        buttonsPanel.add(mResetFormButton);
        mSaveResultsButton = new JButton("save results");
        mSaveResultsButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        saveResults();
                    }
                });        
        buttonsPanel.add(mSaveResultsButton);
        mClearResultsButton = new JButton("clear results");
        mClearResultsButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        clearResults();
                    }
                });        
        buttonsPanel.add(mClearResultsButton);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;        
        
        topPanel.add(buttonsPanel);
        gridLayout.setConstraints(buttonsPanel, constraints);
        
        // create results table
        mResultsTable = new JTable();
        mResultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane resultsPane = new JScrollPane(mResultsTable);
        resultsPane.setBorder(BorderFactory.createEtchedBorder());
        resultsPane.setPreferredSize(new Dimension(500, 250));
        resultsPane.setMinimumSize(new Dimension(500, 250));
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy++;
                
        topPanel.add(resultsPane);
        gridLayout.setConstraints(resultsPane, constraints);
        
        JPanel statsPanel = new JPanel();
        mIterationCountLabel = new JLabel("iteration count: ");
        statsPanel.add(mIterationCountLabel);
        JPanel iterationCountPanel = new JPanel();
        iterationCountPanel.setBorder(BorderFactory.createEtchedBorder());
        mIterationCountText = new JLabel();
        mIterationCountText.setPreferredSize(new Dimension(75, 10));
        mIterationCountText.setMinimumSize(new Dimension(75, 10));
        iterationCountPanel.add(mIterationCountText);
        statsPanel.add(iterationCountPanel);
        
        mErrorLabel = new JLabel("fractional error: ");
        statsPanel.add(mErrorLabel);
        JPanel errorPanel = new JPanel();
        errorPanel.setBorder(BorderFactory.createEtchedBorder());
        mErrorText = new JLabel();
        mErrorText.setPreferredSize(new Dimension(75, 10));
        mErrorText.setMinimumSize(new Dimension(75, 10));
        errorPanel.add(mErrorText);
        statsPanel.add(errorPanel);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy++;
        
        topPanel.add(statsPanel);
        gridLayout.setConstraints(statsPanel, constraints);
        
        // add components to the topPanel
        
        mContentPane.add(topPanel);

        clearFile();        
    }
    
    public void initialize(Container pContentPane, Component pParent)
    {
        mContentPane = pContentPane;
        mParent = pParent;
        mQuantileNormalizer = new QuantileNormalizer();
        mObservationsData = null;
        mWorkingDirectory = null;
        mEmptyTableModel = new EmptyTableModel();
        mNumberFormat = new ScientificNumberFormat(new SignificantDigitsCalculator());
        initializeContentPane();
    }

    private void setToolTipsForFields(boolean pFileLoaded, boolean pResultsObtained)
    {
        if(pFileLoaded)
        {
            mFileClearButton.setToolTipText(TOOL_TIP_FILE_CLEAR_BUTTON);
            mFileLoadButton.setToolTipText(null);
            mObservationsTable.setToolTipText(TOOL_TIP_OBSERVATIONS_TABLE);
            mScalesLabel.setToolTipText(TOOL_TIP_METHODS_BOX);
            mScalesBox.setToolTipText(TOOL_TIP_METHODS_BOX);
            mNormalizeButton.setToolTipText(TOOL_TIP_NORMALIZE_BUTTON);
            mResetFormButton.setToolTipText(TOOL_TIP_RESET_FORM_BUTTON);
            if(hasMissingData())
            {
                mErrorToleranceLabel.setToolTipText(TOOL_TIP_ERROR_TOLERANCE);
                mErrorToleranceField.setToolTipText(TOOL_TIP_ERROR_TOLERANCE);
            }
            else
            {
                mErrorToleranceLabel.setToolTipText(null);
                mErrorToleranceField.setToolTipText(null);
            }
        }
        else
        {
            mFileClearButton.setToolTipText(null);
            mFileLoadButton.setToolTipText(TOOL_TIP_FILE_LOAD_BUTTON);
            mObservationsTable.setToolTipText(null);
            mScalesLabel.setToolTipText(null);
            mScalesBox.setToolTipText(null);
            mNormalizeButton.setToolTipText(null);
            mResetFormButton.setToolTipText(null);
            mErrorToleranceLabel.setToolTipText(null);
            mErrorToleranceField.setToolTipText(null);
        }
        if(pResultsObtained)
        {
            boolean hasMissingData = hasMissingData();
            mClearResultsButton.setToolTipText(TOOL_TIP_CLEAR_RESULTS_BUTTON);
            mSaveResultsButton.setToolTipText(TOOL_TIP_SAVE_RESULTS_BUTTON);
            mIterationCountLabel.setToolTipText(TOOL_TIP_ITERATION_COUNT);
            mIterationCountText.setToolTipText(TOOL_TIP_ITERATION_COUNT);
            if(hasMissingData)
            {            
                mErrorLabel.setToolTipText(TOOL_TIP_FINAL_ERROR);
                mErrorText.setToolTipText(TOOL_TIP_FINAL_ERROR);
            }
            else
            {
                mErrorLabel.setToolTipText(null);
                mErrorText.setToolTipText(null);
            }
        }
        else
        {
            mClearResultsButton.setToolTipText(null);
            mSaveResultsButton.setToolTipText(null);
            mIterationCountLabel.setToolTipText(null);
            mIterationCountText.setToolTipText(null);
            mErrorLabel.setToolTipText(null);
            mErrorText.setToolTipText(null);
        }
    }    
    
    private boolean hasMissingData()
    {
        return mObservationsData.getMissingDataRate() > 0.0;
    }
    
    private void setEnableStateForFields(boolean pFileLoaded, boolean pResultsObtained)
    {
        mFileLoadButton.setEnabled(! pFileLoaded);
        mFileClearButton.setEnabled(pFileLoaded);
        mScalesLabel.setEnabled(pFileLoaded);
        mScalesBox.setEnabled(pFileLoaded);
        mFixNegativesLabel.setEnabled(pFileLoaded);
        mFixNegativesBox.setEnabled(pFileLoaded);
        mNormalizeButton.setEnabled(pFileLoaded);
        mClearResultsButton.setEnabled(pResultsObtained);
        mSaveResultsButton.setEnabled(pResultsObtained);
        mResetFormButton.setEnabled(pFileLoaded);        
        boolean showErrorTolerance = pFileLoaded && (hasMissingData());
        mErrorToleranceLabel.setEnabled(showErrorTolerance);
        mErrorToleranceField.setEnabled(showErrorTolerance);
        mIterationCountLabel.setEnabled(pResultsObtained);
        mIterationCountText.setEnabled(pResultsObtained);
        boolean showErrorLabel = pResultsObtained && hasMissingData();
        mErrorLabel.setEnabled(showErrorLabel);
        mErrorText.setEnabled(showErrorLabel);
    }
    
    private void clearResults()
    {
        mResultsData = null;
        mResultsTable.setModel(mEmptyTableModel);
        setEnableStateForFields(null != mObservationsData, false);
        setToolTipsForFields(null != mObservationsData, false);
        mIterationCountText.setText("");
        mErrorText.setText("");
    }
    
    private void setDefaultForFixNegatives()
    {
        boolean allowNonpositiveObservations = getScale().allowsNonpositiveArgument();
        mFixNegativesBox.setSelected(! allowNonpositiveObservations);
    }
    
    private void setDefaults()
    {
        clearResults();
        mScalesBox.setSelectedItem(DEFAULT_QUANTILE_NORMALIZATION_METHOD.getName());
        
        if(null != mObservationsData && mObservationsData.getMissingDataRate() > 0.0)
        {
            mErrorToleranceField.setText(Double.toString(DEFAULT_ERROR_TOLERANCE));
        }
        else
        {
            mErrorToleranceField.setText("");
        }
        setDefaultForFixNegatives();
    }
    
    private void handleMessage(String pMessage, String pTitle, int pMessageType)
    {
        SimpleTextArea simpleArea = new SimpleTextArea(pMessage);
        JOptionPane.showMessageDialog(mParent, simpleArea, pTitle, pMessageType);
    }
        
    private QuantileNormalizationScale getScale()
    {
        String scaleName = (String) mScalesBox.getSelectedItem();
        return QuantileNormalizationScale.get(scaleName);
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
    
    private void clearFile()
    {
        mFileNameLabel.setText("");
        mObservationsData = null;
        mObservationsTable.setModel(mEmptyTableModel);
        clearResults();
        setEnableStateForFields(false, false);
        setToolTipsForFields(false, false);
        mDelimiterBox.setSelectedItem(DEFAULT_DATA_FILE_DELIMITER.getName()); 
        setDefaults();
    }
        
//    private void initializeLoadFileChooser()
//    {
//        mFileChooser = new JFileChooser(mWorkingDirectory);
//        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        DataFileDelimiter []delimiters = DataFileDelimiter.getAll();
//        int numDelimiters = delimiters.length;
//        DataFileDelimiter delimiter = null;
//        RegexFileFilter fileFilter = null;
//        RegexFileFilter defaultFilter = null;
//        for(int i = 0; i < numDelimiters; ++i)
//        {
//            delimiter = delimiters[i];
//            fileFilter = new RegexFileFilter(delimiter.getFilterRegex(), 
//                    "data file in " + delimiter.getName() + "-delimited format");
//            if(null != mDelimiter && mDelimiter.equals(delimiter))
//            {
//                defaultFilter = fileFilter; 
//            }
//            mFileChooser.addChoosableFileFilter(fileFilter);
//        }
//        if(null != defaultFilter)
//        {
//            mFileChooser.setFileFilter(defaultFilter);
//        }
//    }
    

    
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
    
    private void normalizeRawObservations()
    {
        if(null == mObservationsData)
        {
            throw new IllegalStateException("no raw observations data was found");
        }
        ObjectMatrix2D rawObservations = mObservationsData.getObservations();
        ObjectMatrix2D normalizedObservations = rawObservations.like();
        
        // handle error tolerance
        String errorToleranceString = mErrorToleranceField.getText().trim();
        Double errorTolerance = null;
        if(errorToleranceString.length() > 0)
        {
            try
            {
                errorTolerance = new Double(errorToleranceString);
                if(errorTolerance.doubleValue() <= 0.0)
                {
                    handleMessage("The error tolerance must be greater than zero",
                                  "Invalid error tolerance",
                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            catch(NumberFormatException e)
            {
                handleMessage("The error tolerance you specified is not a valid floating-point number, \"" + errorToleranceString + "\"",
                              "Invalid error tolerance",
                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        // handle normalization scale
        QuantileNormalizationScale scale = getScale();
        boolean fixNegatives = mFixNegativesBox.isSelected();
        QuantileNormalizationResults results = new QuantileNormalizationResults();
        results.mNormalizedObservations = normalizedObservations;
        QuantileNormalizerParams params = new QuantileNormalizerParams();
        params.mFixNonpositiveValues = fixNegatives;
        params.mErrorTolerance = errorTolerance;
        params.mScale = scale;
        try
        {
            mQuantileNormalizer.normalize(rawObservations, params, results);
        }
        catch(Exception e)
        {
            handleMessage("The quantile normalization procedure failed.  The specific error message is: " + e.getMessage(),
                          "Quantile normalization failed",
                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        handleResults(results);
    }
    
    private void handleResults(QuantileNormalizationResults pResults)
    {
        ObservationsData resultsData = (ObservationsData) mObservationsData.clone();
        resultsData.setObservations(pResults.mNormalizedObservations);
        mResultsData = resultsData;
        mResultsTable.setModel(new ObservationsTableModel(resultsData));
        String errorText = "";
        Double error = pResults.mFinalError;
        if(null != error)
        {
            errorText = mNumberFormat.format(error.doubleValue());
        }
        mErrorText.setText(errorText);
        mIterationCountText.setText(Integer.toString(pResults.mNumIterations));
        setEnableStateForFields(true, true);
        setToolTipsForFields(true, true);
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
        ObservationsTableModel tableModel = (ObservationsTableModel) mResultsTable.getModel();
        ObservationsData resultsData = mResultsData;
        String []evidences = resultsData.getEvidenceNames();
        String []elements = resultsData.getElementNames();
        
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
            elementName = elements[i];
            if(-1 != elementName.indexOf(delimiter)) 
            {
                elementName = elementName.replaceAll(delimiter, "_");
            }
            sb.append(elementName + delimiter);
            for(int j = 0; j < numEvidences; ++j)
            {
                obsObj = resultsData.getValueAt(i, j);
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
        String programName = "Quantile Normalizer";
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
            QuantileNormalizerDriver app = new QuantileNormalizerDriver();
            app.run(pArgs);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
