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

import org.systemsbiology.util.*;
import java.io.*;
import java.util.prefs.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MatlabConnectionManager
{
    private static final String MATLAB_OPTIONS = "-nojvm -nodesktop"; 
    private MatlabInterface mMatlabInterface;
    private boolean mConnected;
    private Preferences mPreferences;
    private String mMatlabScriptsLocation;
    private static final int READ_MAX_CHARS = 1024;
    
    public static class ActionType
    {
        private final String mName;
        
        private ActionType(String pName)
        {
            mName = pName;
        }
        
        public String toString()
        {
            return(mName);
        }
        
        public static final ActionType CONNECT = new ActionType("connect");
        public static final ActionType DISCONNECT = new ActionType("disconnect");
    }
    
    
    public MatlabConnectionManager(Preferences pPreferences)
    {
        mPreferences = pPreferences;
        mMatlabInterface = new MatlabInterface();
        mConnected = false;
    }
    
    public String getMatlabLocation()
    {
        String matlabLocation = mPreferences.get(App.PREFERENCES_KEY_MATLAB_LOCATION, "");
        return matlabLocation;
    }
    
    public String getMatlabScriptsLocation()
    {
        String matlabScriptsLocation = mPreferences.get(App.PREFERENCES_KEY_MATLAB_SCRIPTS_LOCATION, "");
        return matlabScriptsLocation;
    }
    
    public void disconnect() throws InterruptedException, IOException
    {
        if(mConnected)
        {
            mMatlabInterface.close();
            mConnected = false;
        }
    }
    
    public boolean isConnected()
    {
        return(mConnected);
    }
    
    public void connect() throws IOException
    {
        if(! mConnected)
        {
            String matlabLocation = getMatlabLocation();

            if(matlabLocation.length() == 0)
            {
                throw new IllegalStateException("matlab location not defined");
            }

            String matlabScriptsLocation = getMatlabScriptsLocation();
            if(matlabScriptsLocation.length() == 0)
            {
                throw new IllegalStateException("matlab scripts location not defined");
            }
            
            File file = new File(matlabLocation);
            if(! file.exists() || file.isDirectory())
            {
                throw new IllegalStateException("invalid matlab location: " + matlabLocation); 
            }
            String matlabLocationAbsolutePath = file.getAbsolutePath();
            StringBuffer matlabCommandBuffer = new StringBuffer(matlabLocationAbsolutePath);
            if(MATLAB_OPTIONS.length() > 0)
            {
                matlabCommandBuffer.append(" " + MATLAB_OPTIONS);
            }
            String matlabCommand = matlabCommandBuffer.toString();   
            mMatlabInterface.open(matlabCommand);
            
            mConnected = true;
            
            changeDirectoryInMatlabProgram(matlabScriptsLocation);
        }
    }
    
    public String executeMatlabCommand(String pCommand) throws IOException
    {
        if(! mConnected)
        {
            throw new IllegalStateException("cannot change directory in matlab program, if not connected");
        }
        mMatlabInterface.evalString(pCommand);
        String outputString = mMatlabInterface.getOutputString(READ_MAX_CHARS);
        int indexOfLastGreaterThan = outputString.lastIndexOf('>');
        if(indexOfLastGreaterThan > 0)
        {
            outputString = outputString.substring(0, indexOfLastGreaterThan - 1).trim();
        }
        return outputString;
    }
    
    public void changeDirectoryInMatlabProgram(String pDirectory) throws IOException
    {
        executeMatlabCommand("cd \'" + pDirectory + "\'");
    }
    
}
