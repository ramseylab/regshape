/*
 * Copyright (C) 2004 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.pointillist;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.systemsbiology.gui.*;
import org.systemsbiology.data.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.io.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EditPreferencesAction extends JFrame
{
    private App mApp;
    private static final int NUM_COLUMNS_MATLAB_TEXT_FIELD = 20;
    private static final int NUM_COLUMNS_MISSING_DATA_VALUE_FIELD = 10;
    private JTextField mMatlabLocationField;
    private String mOriginalMatlabLocation;
    private JButton mSaveButton;
    private JButton mRevertButton;
    private static final DataFileDelimiter DEFAULT_DATA_FILE_DELIMITER = DataFileDelimiter.TAB;
    private DataFileDelimiter mOriginalDataFileDelimiter;
    private JComboBox mDataFileDelimiterComboBox;
    private DataFileDelimiter []mDataFileDelimiters;
    private static final String DEFAULT_MISSING_DATA_VALUE = "";
    private JTextField mMissingDataField;
    private String mOriginalMissingDataValue;
    private JTextField mMatlabScriptsLocationField;
    private String mOriginalMatlabScriptsLocation;
    private static final int NUM_COLUMNS_MATLAB_SCRIPTS_LOCATION = 20;
    
    public EditPreferencesAction(App pApp)
    {
        super(pApp.getName() + ": Preferences");
        mApp = pApp;
    }
    
    private void handleSaveAction()
    {
        try
        {
            String matlabLocation = mMatlabLocationField.getText().trim();
            if(matlabLocation.length() == 0)
            {
                JOptionPane.showMessageDialog(this, "the matlab location you specified is empty", "matlab locatino not specified",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            File matlabFile = new File(matlabLocation);
            if(! matlabFile.exists() || matlabFile.isDirectory())
            {
                JOptionPane.showMessageDialog(this, "the matlab location you specified does not exist", "matlab not found",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            mOriginalMatlabLocation = matlabLocation;
            
            String matlabScriptsLocation = mMatlabScriptsLocationField.getText().trim();
            if(matlabScriptsLocation.length() == 0)
            {
                JOptionPane.showMessageDialog(this, "the matlab scripts location you specified does not exist", "matlab scripts not found",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            File matlabScriptsFile = new File(matlabScriptsLocation);
            if(! matlabScriptsFile.exists() || matlabScriptsFile.isFile())
            {
                JOptionPane.showMessageDialog(this, "the matlab scripts location you specified does not exist", "matlab scripts directory not found",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean needToNotifyMatlabOfNewScriptsLocation = false;
            if(! mOriginalMatlabScriptsLocation.equals(matlabScriptsLocation) &&
               mApp.getMatlabConnectionManager().isConnected())
            {
                needToNotifyMatlabOfNewScriptsLocation = true;
            }
            mOriginalMatlabScriptsLocation = matlabScriptsLocation;
            
            int dataFileDelimiterIndex = mDataFileDelimiterComboBox.getSelectedIndex();
            DataFileDelimiter dataFileDelimiter = mDataFileDelimiters[dataFileDelimiterIndex];
            String dataFileDelimiterName = dataFileDelimiter.getName();
            mOriginalDataFileDelimiter = dataFileDelimiter;
            
            String missingDataValue = mMissingDataField.getText().trim();
            if(missingDataValue.length() > 0)
            {
                boolean isValidNumber = true;
                try
                {
                    Double.parseDouble(missingDataValue);
                }
                catch(NumberFormatException e)
                {
                    isValidNumber = false;
                }
                if(! isValidNumber)
                {
                    JOptionPane.showMessageDialog(mApp, "invalid \"missing data\" value: " + missingDataValue, 
                                                  "invalid text field", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            if(! missingDataValue.equals(mOriginalMissingDataValue))
            {
                mOriginalMissingDataValue = missingDataValue;
            }

            
            Preferences prefs = mApp.getPreferences();
            prefs.put(App.PREFERENCES_KEY_MATLAB_LOCATION, matlabFile.getAbsolutePath());
            prefs.put(App.PREFERENCES_KEY_DATA_FILE_DELIMITER, dataFileDelimiterName);
            prefs.put(App.PREFERENCES_KEY_MISSING_DATA_VALUE, missingDataValue);
            prefs.put(App.PREFERENCES_KEY_MATLAB_SCRIPTS_LOCATION, matlabScriptsFile.getAbsolutePath());

            updateButtonStates();
            
            if(needToNotifyMatlabOfNewScriptsLocation)
            {
                try
                {
                    mApp.getMatlabConnectionManager().changeDirectoryInMatlabProgram(matlabScriptsFile.getAbsolutePath());
                }
                catch(IOException e)
                {
                    ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
                    optionPane.createDialog(this, "unable to set matlab scripts location; disconnecting from matlab").show();
                    MatlabDisconnectAction disconnectAction = new MatlabDisconnectAction(mApp);
                    disconnectAction.doAction();
                }
            }
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(this, "unable to save preferences").show();
        }
    }
    
    private void handleCloseAction()
    {
        setVisible(false);
        setMenuItemEnableState(MenuBar.ACTION_PREFERENCES, true);
        dispose();
    }
    
    private boolean hasChanged()
    {
        boolean hasChanged = false;
        String matlabLocation = mMatlabLocationField.getText().trim();
        if(! matlabLocation.equals(mOriginalMatlabLocation))
        {
            hasChanged = true;
        }
        
        int delimiterIndex = mDataFileDelimiterComboBox.getSelectedIndex();       
        DataFileDelimiter delimiter = mDataFileDelimiters[delimiterIndex];
        if(! delimiter.equals(mOriginalDataFileDelimiter))
        {
            hasChanged = true;
        }
        
        if(! mOriginalMissingDataValue.equals(mMissingDataField.getText().trim()))
        {
            hasChanged = true;
        }
        
        String matlabScriptsLocation = mMatlabScriptsLocationField.getText().trim();
        if(! matlabScriptsLocation.equals(mOriginalMatlabScriptsLocation))
        {
            hasChanged = true;
        }
        return hasChanged;
    }
    
    private void updateButtonStates()
    {
        boolean hasChanged = hasChanged();
        mSaveButton.setEnabled(hasChanged);
        mRevertButton.setEnabled(hasChanged);
    }
    
    private void setMenuItemEnableState(String pAction, boolean pEnabled)
    {
        mApp.getMenu().setMenuItemEnabled(pAction, pEnabled);
    }
    
    private void handleRevertButton()
    {
        String originalMatlabLocation = mOriginalMatlabLocation;
        mMatlabLocationField.setText(originalMatlabLocation);
        
        int numDataFileDelimiters = mDataFileDelimiters.length;
        DataFileDelimiter dataFileDelimiter = mOriginalDataFileDelimiter;
        
        int index = -1;
        for(int i = 0; i < numDataFileDelimiters; ++i)
        {
            if(mDataFileDelimiters[i].equals(dataFileDelimiter))
            {
                index = i;
                break;
            }
        }
        if(-1 == index)
        {
            throw new IllegalStateException("unknown data file delimiter: " + dataFileDelimiter);
        }
        mDataFileDelimiterComboBox.setSelectedIndex(index);
        
        mMissingDataField.setText(mOriginalMissingDataValue);
        mMatlabScriptsLocationField.setText(mOriginalMatlabScriptsLocation);
        
        updateButtonStates();
    }
        
    private void handleMatlabLocationBrowseButton()
    {
        try
        {
            File currentDirectory = null;
            File currentFile = null;
            String matlabLocation = mMatlabLocationField.getText().trim();
            if(matlabLocation.length() > 0)
            {
                currentFile = new File(matlabLocation);
                currentDirectory = currentFile.getParentFile();
            }
            FileChooser fileChooser = new FileChooser(currentDirectory);
            if(null != currentFile && currentFile.exists())
            {
                fileChooser.setSelectedFile(currentFile);
            }
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showDialog(this, "approve");
            if(JFileChooser.APPROVE_OPTION == result)
            {
                File selectedFile = fileChooser.getSelectedFile();
                if(! selectedFile.exists())
                {
                    JOptionPane.showMessageDialog(this, "file does not exist: " + selectedFile.getName(), "file does not exist", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                mMatlabLocationField.setText(selectedFile.getAbsolutePath());
                updateButtonStates();
            }
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(this, "unable to browse for matlab location").show();
        }
    }
    
    private void handleMatlabScriptsLocationBrowseButton()
    {
        try
        {
            File currentDirectory = null;
            File currentFile = null;
            String matlabScriptsLocation = mMatlabScriptsLocationField.getText().trim();
            if(matlabScriptsLocation.length() > 0)
            {
                currentFile = new File(matlabScriptsLocation);
                currentDirectory = currentFile.getParentFile();
            }
            FileChooser fileChooser = new FileChooser(currentDirectory);
            if(null != currentFile && currentFile.exists())
            {
                fileChooser.setSelectedFile(currentFile);
            }
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showDialog(this, "approve");
            if(JFileChooser.APPROVE_OPTION == result)
            {
                File selectedFile = fileChooser.getSelectedFile();
                if(! selectedFile.exists())
                {
                    JOptionPane.showMessageDialog(this, "directory does not exist: " + selectedFile.getName(), "directory does not exist", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                mMatlabScriptsLocationField.setText(selectedFile.getAbsolutePath());
                updateButtonStates();
            }
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(this, "unable to browse for matlab scripts location").show();
        }       
    }
    
    private void initialize()
    {
        setMenuItemEnableState(MenuBar.ACTION_PREFERENCES, false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleCloseAction();
            }
        });

        
        Preferences prefs = mApp.getPreferences();
        JPanel prefsPanel = new JPanel();
        
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();


        
        prefsPanel.setLayout(layout);        
        JLabel matlabLabel = new JLabel("Matlab Location: ");

        prefsPanel.add(matlabLabel);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.25;
        constraints.gridx = 0;
        constraints.gridy = 0; 
        layout.setConstraints(matlabLabel, constraints);
        
        String matlabFileLocation = prefs.get(App.PREFERENCES_KEY_MATLAB_LOCATION, "");
        JTextField matlabTextField = new JTextField(matlabFileLocation, NUM_COLUMNS_MATLAB_TEXT_FIELD);
        mMatlabLocationField = matlabTextField;
        mOriginalMatlabLocation = matlabFileLocation;

        DocumentListener documentListener = new DocumentListener()
        {
            public void changedUpdate(DocumentEvent e)
            {
                updateButtonStates();
            }

            public void insertUpdate(DocumentEvent e)
            {
                updateButtonStates();
            }

            public void removeUpdate(DocumentEvent e)
            {
                updateButtonStates();
            }
        };      
        mMatlabLocationField.getDocument().addDocumentListener(documentListener);
        
        prefsPanel.add(matlabTextField);
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.25;
        constraints.gridx = 1;
        constraints.gridy = 0;        
        layout.setConstraints(matlabTextField, constraints);

        JButton browseButton = new JButton("browse");
        browseButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handleMatlabLocationBrowseButton();
                    }
                });
        prefsPanel.add(browseButton);
        
        constraints.fill = GridBagConstraints.REMAINDER;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.25;
        constraints.gridx = 4;
        constraints.gridy = 0;          
        layout.setConstraints(browseButton, constraints);

        JLabel delimiterLabel = new JLabel("Data File Delimiter: ");
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.25;
        constraints.gridx = 0;
        constraints.gridy = 1;
        prefsPanel.add(delimiterLabel);
        layout.setConstraints(delimiterLabel, constraints);
        
        // get delimiter preference
        
        String preferredDelimiterName = prefs.get(App.PREFERENCES_KEY_DATA_FILE_DELIMITER, "");
        DataFileDelimiter preferredDelimiter = null;
        if(preferredDelimiterName.length() == 0)
        {
            throw new IllegalStateException("a default data file delimiter should already be configured");
        }

        preferredDelimiter = DataFileDelimiter.forName(preferredDelimiterName);
        if(null == preferredDelimiter)
        {
            throw new IllegalStateException("unknown data file delimiter: " + preferredDelimiterName);
        }

        mOriginalDataFileDelimiter = preferredDelimiter;
        DataFileDelimiter []delimiters = DataFileDelimiter.getAll();
        mDataFileDelimiters = delimiters;
        String []delimiterStrings = new String[delimiters.length];
        int numDelimiters = delimiters.length;
        int defaultIndex = 0;
        for(int i = 0; i < numDelimiters; ++i)
        {
            delimiterStrings[i] = delimiters[i].toString();
            if(delimiters[i].equals(preferredDelimiter))
            {
                defaultIndex = i;
            }
        }
        JComboBox delimitersComboBox = new JComboBox(delimiterStrings);
        mDataFileDelimiterComboBox = delimitersComboBox;
        delimitersComboBox.setSelectedIndex(defaultIndex);
        prefsPanel.add(delimitersComboBox);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.5;
        constraints.weighty = 0.25;
        constraints.gridx = 1;
        constraints.gridy = 1;
        prefsPanel.add(delimitersComboBox);
        delimitersComboBox.addItemListener(
                new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        if(e.getStateChange() == ItemEvent.SELECTED)
                        {
                            updateButtonStates();
                        }
                    }
                });
        layout.setConstraints(delimitersComboBox, constraints);
        
        // add text field for specifying the "missing data" value
        JLabel missingDataLabel = new JLabel("\"Missing data\" value: ");
        prefsPanel.add(missingDataLabel);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.25;
        constraints.gridx = 0;
        constraints.gridy = 2;
        layout.setConstraints(missingDataLabel, constraints);
        
        String missingDataValue = prefs.get(App.PREFERENCES_KEY_MISSING_DATA_VALUE, DEFAULT_MISSING_DATA_VALUE);
        mOriginalMissingDataValue = missingDataValue;
        JTextField missingDataField = new JTextField(missingDataValue, 
                                                     NUM_COLUMNS_MISSING_DATA_VALUE_FIELD);
        prefsPanel.add(missingDataField);
        mMissingDataField = missingDataField;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.25;
        constraints.gridx = 1;
        constraints.gridy = 2;
        layout.setConstraints(missingDataField, constraints);   
        missingDataField.getDocument().addDocumentListener(documentListener);
        
        JLabel missingDataExplanationLabel = new JLabel("(empty means no missing data value)");
        prefsPanel.add(missingDataExplanationLabel);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.25;
        constraints.gridx = 3;
        constraints.gridy = 2;
        layout.setConstraints(missingDataExplanationLabel, constraints);
        
        // matlab scripts location ----------------------
        
        JLabel matlabScriptsLocationLabel = new JLabel("Matlab scripts location: ");
        prefsPanel.add(matlabScriptsLocationLabel);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.25;
        constraints.gridx = 0;
        constraints.gridy = 3;
        layout.setConstraints(matlabScriptsLocationLabel, constraints);
        
        String matlabScriptsLocation = prefs.get(App.PREFERENCES_KEY_MATLAB_SCRIPTS_LOCATION, 
                                                 "");
        JTextField matlabScriptsLocationField = new JTextField(matlabScriptsLocation, 
                                                               NUM_COLUMNS_MATLAB_SCRIPTS_LOCATION);
        mOriginalMatlabScriptsLocation = matlabScriptsLocation;
        prefsPanel.add(matlabScriptsLocationField);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.25;
        constraints.gridx = 1;
        constraints.gridy = 3;
        layout.setConstraints(matlabScriptsLocationField, constraints);
        mMatlabScriptsLocationField = matlabScriptsLocationField;
        mMatlabScriptsLocationField.getDocument().addDocumentListener(documentListener);
        
        JButton matlabScriptsLocationButton = new JButton("browse");
        prefsPanel.add(matlabScriptsLocationButton);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0.25;
        constraints.gridx = 4;
        constraints.gridy = 3;
        matlabScriptsLocationButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handleMatlabScriptsLocationBrowseButton();
                    }
                });
        layout.setConstraints(matlabScriptsLocationButton, constraints);
        
        // REVERT button
        
        JButton revertButton = new JButton("revert");
        revertButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handleRevertButton();
                    }
                });
        prefsPanel.add(revertButton);
        mRevertButton = revertButton;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.333;
        constraints.weighty = 0;
        constraints.gridx = 1;
        constraints.gridy = 4;               
        layout.setConstraints(revertButton, constraints);
        
        JPanel buttonPanel = new JPanel();
        
        // SAVE button
        JButton saveButton = new JButton("save");
        saveButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handleSaveAction();
                    }
                });
        mSaveButton = saveButton;
        
        prefsPanel.add(saveButton);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.333;
        constraints.weighty = 0;
        constraints.gridx = 2;
        constraints.gridy = 4; 
        layout.setConstraints(saveButton, constraints);
        
        // CANCEL button
        JButton cancelButton = new JButton("cancel");
        cancelButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handleCloseAction();
                    }
                });
        prefsPanel.add(cancelButton);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.333;
        constraints.weighty = 0;
        constraints.gridx = 3;
        constraints.gridy = 4;         
        layout.setConstraints(cancelButton, constraints);

        setContentPane(prefsPanel);
        pack();
        mApp.getFramePlacer().placeInCascadeFormat(this);
        updateButtonStates();
        setVisible(true);        
    }
    
    public void doAction()
    {
        try
        {
            initialize();
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mApp, "unable to display preferences window").show();
        }
    }
}
