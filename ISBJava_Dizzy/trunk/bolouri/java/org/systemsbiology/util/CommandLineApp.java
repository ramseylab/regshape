package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.*;

/**
 * Class template for a commane-line application
 *
 * @author Stephen Ramsey
 */
public abstract class CommandLineApp
{
    protected static final String HELP_ARG = "-help";

    protected abstract void printUsage(OutputStream pOutputStream);

    protected void handleCommandLineError(String pMessage)
    {
        System.err.println(pMessage);
        System.err.println("Please refer to the usage description that follows: ");
        printUsage(System.err);
        System.exit(1);
    }

    protected String getRequiredArgumentModifier(String pArgument, String []pArgs, int pCtr)
    {
        if(pCtr == pArgs.length)
        {
            handleCommandLineError("argument \"" + pArgument + "\" has a required element after it; please refer to the usage description that follows\n");
        }
        return(pArgs[pCtr]);
    }

    protected Double getRequiredDoubleArgumentModifier(String pArgument, String []pArgs, int pCtr)
    {
        String valueString = getRequiredArgumentModifier(pArgument, pArgs, pCtr);
        Double retVal = null;
        try
        {
            retVal = Double.valueOf(valueString);
        }
        catch(NumberFormatException e)
        {
            handleCommandLineError("invalid floating-point value for argument \"" + pArgument + "\"; value was: " + valueString);
        }
        return(retVal);
    }

    protected Integer getRequiredIntegerArgumentModifier(String pArgument, String []pArgs, int pCtr)
    {
        String valueString = getRequiredArgumentModifier(pArgument, pArgs, pCtr);
        Integer retVal = null;
        try
        {
            retVal = Integer.valueOf(valueString);
        }
        catch(NumberFormatException e)
        {
            handleCommandLineError("invalid integer value for argument \"" + pArgument + "\"; value was: " + valueString);
        }
        return(retVal);
    }

    protected Long getRequiredLongArgumentModifier(String pArgument, String []pArgs, int pCtr)
    {
        String valueString = getRequiredArgumentModifier(pArgument, pArgs, pCtr);
        Long retVal = null;
        try
        {
            retVal = Long.valueOf(valueString);
        }
        catch(NumberFormatException e)
        {
            handleCommandLineError("invalid long value for argument \"" + pArgument + "\"; value was: " + valueString);
        }
        return(retVal);
    }

    protected void checkAndHandleHelpRequest(String []pArgs)
    {
        int numArgs = pArgs.length;

        for(int argCtr = 0; argCtr < numArgs; ++argCtr)
        {
            String arg = pArgs[argCtr];

            if(arg.equals(HELP_ARG))
            {
                printUsage(System.out);
                System.exit(0);
            }
        }
    }

    protected abstract void handleCommandLine(String []pArgs);
}
