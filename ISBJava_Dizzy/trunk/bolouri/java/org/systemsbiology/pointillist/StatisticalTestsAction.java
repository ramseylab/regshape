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
import javax.swing.*;
import java.util.*;
import org.systemsbiology.gui.*;
import org.systemsbiology.util.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StatisticalTestsAction
{
    private App mApp;
    private static final String MATLAB_FUNCTION_NAME = "pcomp";
    private static final String TESTED_FILE_SUFFIX = "_test.txt";
    
    public StatisticalTestsAction(App pApp)
    {
        mApp = pApp;
    }
    
    static class DataType implements Comparable
    {
        private final String mName;
        private final int mCode;
        private static final HashMap sMap;
        
        static
        {
            sMap = new HashMap();
        }
        
        public int compareTo(Object pObject)
        {
            return this.mName.compareTo(((DataType) pObject).mName);
        }
        
        public static DataType forName(String pName)
        {
            return (DataType) sMap.get(pName);
        }
        
        public String toString()
        {
            return mName;
        }
        
        public int getCode()
        {
            return mCode;
        }
        
        public String getName()
        {
            return mName;
        }
        
        private DataType(String pName, int pCode)
        {
            mName = pName;
            mCode = pCode;
            sMap.put(pName, this);
        }
        
        public static DataType []getAll()
        {
            LinkedList linkedList = new LinkedList(sMap.values());
            Collections.sort(linkedList);
            DataType []retArray = (DataType []) linkedList.toArray(new DataType[0]);
            return retArray;
        }
        
        public static final DataType STATIC = new DataType("static", 0);
        public static final DataType DYNAMIC = new DataType("dynamic / time-course", 1);
        public static final DataType KNOCKOUT = new DataType("knock-out / deletion", 2);
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
        String statisticalTestsDataFileName = FileUtils.addSuffixToFilename(selectedFileName, TESTED_FILE_SUFFIX);
        File statisticalTestsDataFile = new File(statisticalTestsDataFileName);
        if(statisticalTestsDataFile.exists())
        {
            boolean overwriteFile = FileChooser.handleOutputFileAlreadyExists(mApp,
                                                                              statisticalTestsDataFileName);
            if(! overwriteFile)
            {
                return;
            }
            statisticalTestsDataFile.delete();
        }
        
        DataType []dataTypes = DataType.getAll();
        int numDataTypes = dataTypes.length;
        String []dataTypeNames = new String[numDataTypes];
        for(int i = 0; i < numDataTypes; ++i)
        {
            dataTypeNames[i] = dataTypes[i].getName();
        }
        
        SimpleTextArea textArea = new SimpleTextArea("Please select one of the following data types that best describes the file you selected");
        
        JOptionPane optionPane = new JOptionPane(textArea,
                                                 JOptionPane.QUESTION_MESSAGE,
                                                 JOptionPane.DEFAULT_OPTION,
                                                 null,
                                                 dataTypeNames);
        optionPane.createDialog(mApp,
                                "Please select the data type").show();
        String selectedValue = (String) optionPane.getValue();
        if(null == selectedValue)
        {
            return;
        }
        DataType dataType = DataType.forName(selectedValue);
        if(null == dataType)
        {
            throw new IllegalStateException("Unknown data type: " + dataType.getName());
        }
        int dataTypeCode = dataType.getCode();
        
        StringBuffer commandBuf = new StringBuffer();
        commandBuf.append(MATLAB_FUNCTION_NAME + "(\'");
        commandBuf.append(selectedFileName);
        commandBuf.append("\', ");
        commandBuf.append(dataTypeCode + ");");
        String response = null;
        String matlabCommand = commandBuf.toString();
        
        try
        {
            response = mgr.executeMatlabCommand(matlabCommand);
            System.out.println(response);
        }
        catch(IOException e)
        {
            optionPane = new ExceptionNotificationOptionPane(e);
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
            File outputFile = new File(statisticalTestsDataFileName);
            if(outputFile.exists() && outputFile.isFile())
            {
                JOptionPane.showMessageDialog(mApp, 
                        "statistical test succeeded, saved as:\n" + outputFile.getName(), 
                        "statistical test succeeded", 
                         JOptionPane.INFORMATION_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog(mApp,
                        "statistical test failed; cannot locate output file:\n" + outputFile.getName(),
                        "statistical test failed",
                        JOptionPane.WARNING_MESSAGE);
            }            
        }
    }
}
