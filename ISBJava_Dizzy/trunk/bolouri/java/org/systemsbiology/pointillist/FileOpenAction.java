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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import javax.swing.*;
import org.systemsbiology.gui.*;
import org.systemsbiology.data.*;
import java.util.prefs.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FileOpenAction
{
    private App mApp;
    private File mFile;
    
    public FileOpenAction(App pApp)
    {
        mApp = pApp;
        mFile = null;
    }
    
    public void doAction()
    {
        try
        {
            File workingDirectory = mApp.getWorkingDirectory();
            FileChooser fileChooser = new FileChooser(workingDirectory);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int value = fileChooser.showSaveDialog(mApp);
            File selectedFile = null;
            if(value == JFileChooser.APPROVE_OPTION)
            {
                selectedFile = fileChooser.getSelectedFile();
            }
            if(null != selectedFile)
            {
                mFile = selectedFile;
                String selectedFileName = selectedFile.getAbsolutePath();
                JFrame dataFrame = mApp.getDataFrame(selectedFileName);
                if(null != dataFrame)
                {
                    dataFrame.toFront();
                }
                else
                {
                    workingDirectory = selectedFile.getParentFile();
                    mApp.setWorkingDirectory(workingDirectory);
                    
                    FileReader fileReader = new FileReader(selectedFile);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    MatrixString matrixString = new MatrixString();
                    
                    Preferences prefs = mApp.getPreferences();
                    String delimiterName = prefs.get(App.PREFERENCES_KEY_DATA_FILE_DELIMITER, "");
                    if(delimiterName.length() == 0)
                    {
                        throw new IllegalStateException("no data file delimiter found");
                    }
                    DataFileDelimiter delimiter = DataFileDelimiter.forName(delimiterName);
                    if(null == delimiter)
                    {
                        throw new IllegalStateException("unknown delimiter name: " + delimiterName);
                    }
                    String delimiterString = delimiter.getDelimiter();
                    matrixString.buildFromLineBasedStringDelimitedInput(bufferedReader,
                            delimiterString);
                    String appName = mApp.getAppConfig().getAppName();
                    DataColumnSelector dataColumnSelector = new DataColumnSelector(appName + ": " + selectedFile.getName(), 
                            matrixString);
                    dataColumnSelector.setDelimiter(delimiterString);
                    
                    FramePlacer framePlacer = mApp.getFramePlacer();
                    framePlacer.placeInCascadeFormat(dataColumnSelector);
                    
                    mApp.addDataFrame(selectedFile.getAbsolutePath(), dataColumnSelector);
                    dataColumnSelector.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                        mApp.removeDataFrame(mFile.getAbsolutePath());
                    }});
                dataColumnSelector.setVisible(true);
                }
            }
            
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mApp, "unable to load file").show();
        }
    }
}
