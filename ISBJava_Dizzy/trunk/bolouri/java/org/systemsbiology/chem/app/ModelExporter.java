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
import org.systemsbiology.chem.scripting.*;
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

    private void handleCancel()
    {
        SimpleDialog messageDialog = new SimpleDialog(mMainFrame, "Export cancelled", 
                                                      "Your export operation has been cancelled");
        messageDialog.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        messageDialog.show();
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
            fileChooser.setDialogTitle("Please specify the file to export");
            fileChooser.show();
            String fileName = fileChooser.getFileName();
            if(null != fileName)
            {
                IModelExporter exporter = null;
                try
                {
                    exporter = (IModelExporter) pModelExporterRegistry.getInstance(exporterAlias);
                }
                catch(DataNotFoundException e)
                {
                    UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(mMainFrame, "unable to create an instance of exporter: " + exporterAlias);
                    dialog.show();
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
                        
                        SimpleDialog messageDialog = new SimpleDialog(mMainFrame, 
                                                                      "Non-standard export filename specified",
                                                                      textArea);
                        messageDialog.setMessageType(JOptionPane.WARNING_MESSAGE);
                        messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
                        messageDialog.show();
                        Integer response = (Integer) messageDialog.getValue();
                        if(null != response &&
                           response.intValue() == JOptionPane.YES_OPTION)
                        {
                            doExport = true;
                        }
                        else
                        {
                            if(null == response)
                            {
                                handleCancel();
                            }
                        }
                    }
                    else
                    {
                        doExport = true;
                    }
                    
                    File outputFile = new File(fileName);

                    if(doExport)
                    {
                        if(outputFile.exists())
                        {
                            // need to ask user to confirm whether the file should be overwritten
                            SimpleTextArea textArea = new SimpleTextArea("The export file you selected already exists:\n" + fileName + "\nThe export operation will overwrite this file.\nAre you sure you want to proceed?");
                            
                            SimpleDialog messageDialog = new SimpleDialog(mMainFrame, 
                                                                          "Overwrite existing file?",
                                                                          textArea);
                            messageDialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
                            messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
                            messageDialog.show();
                            Integer response = (Integer) messageDialog.getValue();
                            if(null != response &&
                               response.intValue() == JOptionPane.YES_OPTION)
                            {
                                // do nothing, export of file will happen below
                            }
                            else
                            {
                                doExport = false;
                                if(null == response)
                                {
                                    handleCancel();
                                }
                                }
                        }
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
                            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "Export operation failed: " + shortName, e);
                            dialog.show();
                        }
                        if(showSuccessfulDialog)
                        {
                            SimpleDialog successDialog = new SimpleDialog(mMainFrame, "Export was successful", "The file export operation succeeded.\nThe output was saved in file: " + shortName);
                            successDialog.show();
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
            // display "operation cancelled" in case user accidentally hit close button
            handleCancel();
        }
    }
}
