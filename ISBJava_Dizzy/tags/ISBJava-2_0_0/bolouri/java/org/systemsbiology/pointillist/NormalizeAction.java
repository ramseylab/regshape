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

import java.io.*;
import org.systemsbiology.gui.*;
import java.util.prefs.*;

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
        mApp.checkIfConnectedToMatlab();
        
        File inputFile = mApp.handleInputFileSelection();
        if(null == inputFile)
        {
            return;
        }
        
        File outputFile = mApp.handleOutputFileSelection(inputFile, NORMALIZED_FILE_SUFFIX);

        Preferences prefs = mApp.getPreferences();
        String missingDataValueString = prefs.get(App.PREFERENCES_KEY_MISSING_DATA_VALUE, "");

        StringBuffer commandBuf = new StringBuffer();
        commandBuf.append(MATLAB_FUNCTION_NAME + "(\'");
        String selectedFileName = inputFile.getAbsolutePath();
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
            response = mApp.getMatlabConnectionManager().executeMatlabCommand(matlabCommand);
        }
        catch(IOException e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mApp, "unable to execute matlab command \"" + matlabCommand + "\"").show();
        }
        
        boolean success = mApp.handleMatlabResult(response, "normalization", outputFile);
    }
}
