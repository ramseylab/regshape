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

import javax.swing.*;

import java.io.*;

import org.systemsbiology.gui.*;

import java.util.prefs.*;
import org.systemsbiology.util.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NormalizeAction
{
    private App mApp;
    private static final String MATLAB_FUNCTION_NAME = "qnorm";
    private static final String NORMALIZED_FILE_SUFFIX = "_norm.txt";
    
    public NormalizeAction(App pApp)
    {
        mApp = pApp;
    }
    
    public void doAction()
    {
        // check to see if we are connected
        MatlabConnectionManager mgr = mApp.getMatlabConnectionManager();
        if(! mgr.isConnected())
        {
            mApp.handleNotConnectedToMatlab();
            return;
        }
        
        // user must select filename
        File currentDirectory = mApp.getWorkingDirectory();
        FileChooser fileChooser = new FileChooser(currentDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(mApp);
        if(result != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        File selectedFile = fileChooser.getSelectedFile();
        if(! selectedFile.exists())
        {
            JOptionPane.showMessageDialog(mApp, "The file you selected does not exist: " + selectedFile.getName(), 
                                          "File does not exist", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File selectedFileDirectory = selectedFile.getParentFile();
        if(null == currentDirectory || !selectedFileDirectory.equals(currentDirectory))
        {
            mApp.setWorkingDirectory(selectedFileDirectory);
        }
        
        String selectedFileName = selectedFile.getAbsolutePath();
        String normalizedDataFileName = FileUtils.addSuffixToFilename(selectedFileName, NORMALIZED_FILE_SUFFIX);
        File normalizedDataFile = new File(normalizedDataFileName);
        if(normalizedDataFile.exists())
        {
            boolean overwriteFile = FileChooser.handleOutputFileAlreadyExists(mApp,
                                                                              normalizedDataFileName);
            if(! overwriteFile)
            {
                return;
            }
            normalizedDataFile.delete();
        }
        Preferences prefs = mApp.getPreferences();
        String missingDataValueString = prefs.get(App.PREFERENCES_KEY_MISSING_DATA_VALUE, "");

        StringBuffer commandBuf = new StringBuffer();
        commandBuf.append(MATLAB_FUNCTION_NAME + "(\'");
        commandBuf.append(selectedFileName);
        commandBuf.append("\'");
        if(missingDataValueString.length() > 0)
        {
            commandBuf.append(", " + missingDataValueString);
        }
        commandBuf.append(")");
        
        String matlabCommand = commandBuf.toString();
        String response = null;
        try
        {
            response = mgr.executeMatlabCommand(matlabCommand);
        }
        catch(IOException e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mApp, "unable to execute matlab command \"" + matlabCommand + "\"").show();
        }
        
        int responseLength = response.length();
        if(responseLength > 0)
        {
            SimpleTextArea simpleTextArea = new SimpleTextArea("matlab response:\n" + response);
            JOptionPane.showMessageDialog(mApp,
                    simpleTextArea,
                    "matlab response",
                    JOptionPane.WARNING_MESSAGE);
        }
        else
        {
            File normalizedFile = new File(normalizedDataFileName);
            if(normalizedFile.exists() && normalizedFile.isFile())
            {
                JOptionPane.showMessageDialog(mApp, 
                        "normalization succeeded, saved as:\n" + normalizedFile.getName(), 
                        "normalization succeeded", 
                        JOptionPane.INFORMATION_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog(mApp,
                        "normalization failed; cannot locate normalized file:\n" + normalizedFile.getName(),
                        "normalization failed",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
