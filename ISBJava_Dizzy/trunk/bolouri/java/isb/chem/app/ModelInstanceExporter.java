package isb.chem.app;

import isb.util.*;
import isb.chem.*;
import isb.chem.scripting.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

public class ModelInstanceExporter
{
    private Component mMainFrame;

    public ModelInstanceExporter(Component pMainFrame)
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

    public void exportModelInstance(String pModelName, String pSpeciesPopulationName)
    {
        MainApp app = MainApp.getApp();
        if(null != pModelName && null != pSpeciesPopulationName)
        {
            ScriptRuntime scriptRuntime = app.getScriptRuntime();
            Model model = null;
            SpeciesPopulations speciesPopulations = null;
            try
            {
                model = scriptRuntime.getModel(pModelName);
                speciesPopulations = scriptRuntime.getSpeciesPopulations(pSpeciesPopulationName);
            }
            catch(DataNotFoundException e)
            {
                String message = null;
                if(null != model)
                {
                    message = "unable to find information about species populations set: " + pSpeciesPopulationName;
                }
                else
                {
                    message = "unable to find information about model: " + pModelName;
                }
                UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(mMainFrame, message + pSpeciesPopulationName);
                dialog.show();
                return;
            }
            
            Set exporterAliases = scriptRuntime.getExporterAliasesCopy();
            
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
                    IModelInstanceExporter exporter = null;
                    try
                    {
                        exporter = scriptRuntime.getExporter(exporterAlias);
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
                                exporter.exportModelInstance(model, speciesPopulations, printWriter);
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
        else
        {
            SimpleTextArea textArea = new SimpleTextArea("Both a model and a species populations set must be selected, in order to export a model instance.  Please select a model and a species populations set, and try again.  This operation is cancelled.");
            
            SimpleDialog messageDialog = new SimpleDialog(mMainFrame, 
                                                          "Export operation cancelled", 
                                                          textArea);
            messageDialog.setMessageType(JOptionPane.WARNING_MESSAGE);
            messageDialog.show();
        }
    }
}
