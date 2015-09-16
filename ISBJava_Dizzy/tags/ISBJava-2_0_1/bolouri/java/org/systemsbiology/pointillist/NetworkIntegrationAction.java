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
import java.io.File;
import java.io.IOException;

import org.systemsbiology.gui.ExceptionNotificationOptionPane;
import org.systemsbiology.util.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NetworkIntegrationAction implements IAction
{
    private static final String INTEGRATED_FILE_SUFFIX = "_point.txt";
    private static final String MATLAB_FUNCTION_NAME = "mainpointillist";
    
    private App mApp;
    
    public NetworkIntegrationAction(App pApp)
    {
        mApp = pApp;
    }
    
    public static double parseCutoff(String pCutoff) throws InvalidInputException
    {
        if(pCutoff.length() == 0)
        {
            throw new InvalidInputException("undefined p-value cutoff");
        }
        double cutoff = 0.0;
        try
        {
            cutoff = Double.parseDouble(pCutoff);
        }
        catch(NumberFormatException e)
        {
            throw new InvalidInputException("invalid p-value cutoff: " + pCutoff);
        }
        if(cutoff <= 0.0)
        {
            throw new InvalidInputException("invalid p-value cutoff: " + pCutoff);
        }      
        return cutoff;
    }
    
    public void doAction()
    {
        boolean connected = mApp.checkIfConnectedToMatlab();
        if(! connected)
        {
            return;
        }
        
        String pvalueCutoff = mApp.getPreferences().get(App.PREFERENCES_KEY_PVALUE_CUTOFF, "");
        double cutoff = 0.0;
        try
        {
            cutoff = parseCutoff(pvalueCutoff);
        }
        catch(InvalidInputException e)
        {
            throw new IllegalStateException("invalid p-value cutoff: " + pvalueCutoff);
        }

        File inputFile = mApp.handleInputFileSelection();
        if(null == inputFile)
        {
            return;
        }
        
        File outputFile = mApp.handleOutputFileSelection(inputFile, INTEGRATED_FILE_SUFFIX);
        if(null == outputFile)
        {
            return;
        }
        
        String inputFileName = inputFile.getAbsolutePath();
        
        StringBuffer commandBuf = new StringBuffer();
        commandBuf.append(MATLAB_FUNCTION_NAME + "(\'" + inputFileName + "\'");
        commandBuf.append(", " + Double.toString(cutoff) + ");");
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
        
        boolean success = mApp.handleMatlabResult(response, "network integration", outputFile);
    }
}
