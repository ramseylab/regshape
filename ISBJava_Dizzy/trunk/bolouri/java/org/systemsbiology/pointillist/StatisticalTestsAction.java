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

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StatisticalTestsAction implements IAction
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
        boolean connected = mApp.checkIfConnectedToMatlab();
        if(! connected)
        {
            return;
        }
        
        File inputFile = mApp.handleInputFileSelection();
        if(null == inputFile)
        {
            return;
        }
        
        File outputFile = mApp.handleOutputFileSelection(inputFile, TESTED_FILE_SUFFIX);
        if(null == outputFile)
        {
            return;
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
        String selectedFileName = inputFile.getAbsolutePath();
        commandBuf.append(selectedFileName);
        commandBuf.append("\', ");
        commandBuf.append(dataTypeCode + ");");
        String response = null;
        String matlabCommand = commandBuf.toString();
        
        try
        {
            response = mApp.getMatlabConnectionManager().executeMatlabCommand(matlabCommand);
            System.out.println(response);
        }
        catch(IOException e)
        {
            optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mApp, "unable to execute matlab command \"" + matlabCommand + "\"").show();
        }
        
        boolean success = mApp.handleMatlabResult(response, "statistical tests", outputFile);

    }
}
