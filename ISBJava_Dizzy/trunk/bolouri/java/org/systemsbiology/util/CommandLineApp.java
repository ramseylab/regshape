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
        printUsage(System.err);
        System.exit(1);
    }

    protected void checkAndHandleHelpRequest(String []pArgs)
    {
        int numArgs = pArgs.length;

        for(int argCtr = 0; argCtr < numArgs; ++argCtr)
        {
            String arg = pArgs[argCtr];

            if(arg.substring(0, HELP_ARG.length()).equals(HELP_ARG))
            {
                printUsage(System.out);
                System.exit(0);
            }
        }
    }

    protected abstract void handleCommandLine(String []pArgs);
}
