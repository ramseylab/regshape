package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import org.systemsbiology.chem.*;
import org.systemsbiology.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

public class ModelExporter
{
    private Component mMainFrame;

    public ModelExporter(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    public void exportModel(Model pModel, ClassRegistry pModelExporterRegistry)
    {
        MainApp app = MainApp.getApp();
        
        Set exporterAliases = pModelExporterRegistry.getRegistryAliasesCopy();
            
        java.util.List exporterAliasesList = new LinkedList(exporterAliases);
        Collections.sort(exporterAliasesList);
        Object []options = exporterAliasesList.toArray();
            
        JOptionPane optionPane = new JOptionPane("Which exporter plugin do you wish to use?",
                                                 JOptionPane.QUESTION_MESSAGE,
                                                 JOptionPane.DEFAULT_OPTION,
                                                 null,
                                                 options);
        JDialog selectDialog = optionPane.createDialog(mMainFrame, "Please select an exporter");
        selectDialog.show();

        String exporterAlias = (String) optionPane.getValue();
        if(null != exporterAlias)
        {
            FileChooser fileChooser = new FileChooser(mMainFrame);
            fileChooser.setApproveButtonText("export");
            fileChooser.setDialogTitle("Please specify the file to export");
            fileChooser.show();
            File outputFile = fileChooser.getSelectedFile();
            if(null != outputFile)
            {
                String fileName = outputFile.getAbsolutePath();
                IModelExporter exporter = null;
                try
                {
                    exporter = (IModelExporter) pModelExporterRegistry.getInstance(exporterAlias);
                }
                catch(DataNotFoundException e)
                {
                    JOptionPane.showMessageDialog(mMainFrame, 
                                                  "unable to create an instance of exporter: " + exporterAlias,
                                                  MainApp.UNEXPECTED_ERROR_MESSAGE,
                                                  JOptionPane.ERROR_MESSAGE);
                }
                if(null != exporter)
                {
                    String fileRegex = exporter.getFileRegex();
                    boolean fileNameMatchesRegex = fileName.matches(fileRegex);
                    boolean doExport = false;
                    
                    if(! fileNameMatchesRegex)
                    {
                        SimpleTextArea textArea = new SimpleTextArea("Your export file name has a non-standard extension:\n" + fileName + "\nThe preferred extension regex is: " + fileRegex + 
                                         "\nUsing this file name may make it difficult to load the model in the future.  Are you sure you want to proceed?");
                        JOptionPane exportOptionPane = new JOptionPane();
                        exportOptionPane.setMessageType(JOptionPane.WARNING_MESSAGE);
                        exportOptionPane.setOptionType(JOptionPane.YES_NO_OPTION);
                        exportOptionPane.setMessage(textArea);
                        exportOptionPane.createDialog(mMainFrame,
                                                      "Non-standard export filename").show();
                        Integer response = (Integer) exportOptionPane.getValue();
                        if(null != response &&
                           response.intValue() == JOptionPane.YES_OPTION)
                        {
                            doExport = true;
                        }
                        else
                        {
                            // do nothing
                        }
                    }
                    else
                    {
                        doExport = true;
                    }
                    
                    if(doExport && outputFile.exists())
                    {
                        doExport = FileChooser.handleOutputFileAlreadyExists(mMainFrame,
                                                                             fileName);
                    }
                    
                    if(doExport)
                    {
                        boolean showSuccessfulDialog = false;
                        String shortName = outputFile.getName();
                        try
                        {
                            FileWriter outputFileWriter = new FileWriter(outputFile);
                            PrintWriter printWriter = new PrintWriter(outputFileWriter);
                            exporter.export(pModel, printWriter);
                            showSuccessfulDialog = true;
                        }
                        catch(Exception e)
                        {
                            ExceptionNotificationOptionPane errorOptionPane = new ExceptionNotificationOptionPane(e);
                            errorOptionPane.createDialog(mMainFrame, "Export operation failed: " + shortName).show();

                        }
                        if(showSuccessfulDialog)
                        {
                            JOptionPane.showMessageDialog(mMainFrame,
                                                          "The file export operation succeeded.\nThe output was saved in file: " + shortName,
                                                          "Export was successful",
                                                          JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
                else
                {
                    // exporter is null; do nothing because we have already displayed a dialog
                }
            }
            else
            {
                // do nothing; user pressed the cancel button
            }
        }
        else
        {
            // do nothing
        }
    }
}
