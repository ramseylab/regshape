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
import org.systemsbiology.gui.ExceptionNotificationOptionPane;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import org.systemsbiology.data.*;
import org.systemsbiology.gui.*;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;

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
    private SignificantDigitsCalculator mSignificantDigitsCalculator;
    private ScientificNumberFormat mScientificNumberFormat;
    
    public NetworkIntegrationAction(App pApp)
    {
        mApp = pApp;
        mSignificantDigitsCalculator = new SignificantDigitsCalculator();
        mScientificNumberFormat = new ScientificNumberFormat(mSignificantDigitsCalculator);
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
            return;
        }
        
        Preferences prefs = mApp.getPreferences();
        String matlabScriptsDirName = prefs.get(App.PREFERENCES_KEY_MATLAB_SCRIPTS_LOCATION, "");
        if(matlabScriptsDirName.length() == 0)
        {
            throw new IllegalStateException("unable to obtain matlab scripts directory");
        }
        File matlabScriptsDir = new File(matlabScriptsDirName);
        if(! matlabScriptsDir.exists() || ! matlabScriptsDir.isDirectory())
        {
            throw new IllegalStateException("invalid matlab scripts directory \"" + matlabScriptsDirName + "\"");
        }
        
        String statFileName = matlabScriptsDir + File.separator + "stat.txt";
        File statFile = new File(statFileName);
        boolean gotValidResults = true;
        if(statFile.exists() && statFile.isFile())
        {
            try
            {
                FileReader fileReader = new FileReader(statFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = null;
                ArrayList colNames = new ArrayList();
                ArrayList values = new ArrayList();
                MatrixString matrixString = new MatrixString();
                int colCtr = 0;
                while(null != (line = bufferedReader.readLine()))
                {
                    StringTokenizer st = new StringTokenizer(line);
                    while(st.hasMoreTokens())
                    {
                        String token = (String) st.nextToken();
                        Double value = new Double(token);
                        if(0 == colCtr)
                        {
                            colNames.add("num. of diff. exp. genes");
                        }
                        else
                        {
                            colNames.add("iter #" + colCtr);
                        }
                        values.add(mScientificNumberFormat.format(value.doubleValue()));
                        colCtr++;
                    }
                }
                matrixString.addRow(colNames);
                matrixString.addRow(values);
                DataColumnSelector dcs = new DataColumnSelector("iteration summary", matrixString, false);
                mApp.getFramePlacer().placeInCascadeFormat(dcs);
                dcs.setVisible(true);
            }
            catch(Exception e)
            {
                ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
                optionPane.createDialog(mApp, "unable to obtain output from matlab").show();
                return;
            }
        }
        else
        {
            SimpleTextArea textArea = new SimpleTextArea("unable to find iteration summary file \"" + statFileName + "\"");
            JOptionPane.showMessageDialog(mApp, textArea, "unable to find iteration summary file", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
//        boolean success = mApp.handleMatlabResult(response, "network integration", outputFile);
//        
//        if(success)
//        {
//            
//        }
    }
}
