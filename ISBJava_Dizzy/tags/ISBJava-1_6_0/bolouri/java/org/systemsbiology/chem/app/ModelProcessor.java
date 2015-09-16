package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.chem.*;
import org.systemsbiology.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

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
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "error in input file: " + shortFileName, e);
            dialog.show();
            return(model);
        }

        catch(IOException e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "I/O error loading input file: "+shortFileName, e);
            dialog.show();
            return(model);
        }

        return(model);
    }
}
