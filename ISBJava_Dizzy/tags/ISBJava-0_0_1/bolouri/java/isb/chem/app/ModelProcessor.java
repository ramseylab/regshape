package isb.chem.app;

import isb.chem.scripting.*;
import isb.util.*;
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
    private IStatementFilter mStatementFilter;

    public ModelProcessor(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
        mStatementFilter = new StatementFilterDefinitionCategory();
    }

    public void processModel(String pFileName, BufferedReader pInputStream, String pParserAlias)
    {
        String shortFileName = null;
        if(null != pFileName)
        {
            File inputFile = new File(pFileName);
            shortFileName = inputFile.getName();
        }
        else
        {
            shortFileName = "(unknown)";
        }

        Script script = null;
        MainApp app = MainApp.getApp();

        try
        {
            ScriptBuilder scriptBuilder = app.getScriptBuilder();
            script = new Script();
            script = scriptBuilder.buildFromInputStream(pParserAlias, pInputStream, mStatementFilter);
        }

        catch(InvalidInputException e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "error in input file: "+shortFileName, e);
            dialog.show();
            return;
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
            return;
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
            UnexpectedErrorDialog errorDialog = new UnexpectedErrorDialog(mMainFrame, "unexpected error; the application is now exiting");
            errorDialog.show();
            app.exit(1);
        }

        try
        {
            ScriptRuntime scriptRuntime = app.getScriptRuntime();
            scriptRuntime.execute(script);
            app.updateOutputText();
            app.updateRuntimePane();
        }

        catch(ScriptRuntimeException e)
        {
            // display a dialog box forcing the user to choose between clearing the ScriptRuntime, and exiting
            ScriptExecutionFailureDialog dialog = new ScriptExecutionFailureDialog(mMainFrame, e);
            dialog.show();
            String response = dialog.getValue();
            if(null != response)
            {
                if(response.equals(ScriptExecutionFailureDialog.OPTION_RESTART))
                {
                    app.clearRuntime();
                    app.clearOutputText();
                }
                else if(response.equals(ScriptExecutionFailureDialog.OPTION_EXIT))
                {
                    app.exit(0);
                }
                else
                {
                    // this should not happen
                    assert false : "the ScriptExecutionFailureDialog returned an unknown value";
                }
            }
            else
            {
                // this should not happen
                assert false : "the ScriptExecutionFailureDialog should never return null";
            }
            return;            
        }
    }
}
