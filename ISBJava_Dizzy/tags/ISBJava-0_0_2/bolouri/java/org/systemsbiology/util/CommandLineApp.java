package org.systemsbiology.util;

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
