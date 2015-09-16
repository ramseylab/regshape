package org.systemsbiology.chem.app;

import org.systemsbiology.chem.scripting.*;
import org.systemsbiology.chem.*;
import org.systemsbiology.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.PrintWriter;

public class ModelProcessor
{
    private Component mMainFrame;

    public ModelProcessor(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    public Model processModel(String pFileName, BufferedReader pInputStream, String pParserAlias)
    {
        String shortFileName = null;
        IncludeHandler includeHandler = new IncludeHandler();
        if(null != pFileName)
        {
            File inputFile = new File(pFileName);
            shortFileName = inputFile.getName();
            File inputFileDir = inputFile.getParentFile();
            includeHandler.setDirectory(inputFileDir);
        }
        else
        {
            shortFileName = "(unknown)";
        }

        Model model = null;
        MainApp app = MainApp.getApp();
        ClassRegistry modelBuilderRegistry = app.getModelBuilderRegistry();

        try
        {
            IModelBuilder modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(pParserAlias);
            model = modelBuilder.buildModel(pInputStream, includeHandler);
        }

        catch(InvalidInputException e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "error in input file: " + shortFileName, e);
            dialog.show();
            return(model);
        }

        catch(DataNotFoundException e)
        {
            UnexpectedErrorDialog errorDialog = new UnexpectedErrorDialog(mMainFrame, "unable to find parser with parser alias: " + pParserAlias + "; the application is now exiting");
            errorDialog.show();
        }

        catch(IOException e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "I/O error loading input file: "+shortFileName, e);
            dialog.show();
            return(model);
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
            UnexpectedErrorDialog errorDialog = new UnexpectedErrorDialog(mMainFrame, "unexpected error; the application is now exiting");
            errorDialog.show();
            app.exit(1);
        }



        return(model);
    }
}
