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
import java.util.*;
import java.io.*;

/**
 * Command-line interface for building a {@link org.systemsbiology.chem.Model}
 * object from a model definition file, and for passing the model to
 * the {@link SimulationLauncher}. 
 *
 * @see org.systemsbiology.chem.Model
 * @see SimulationLauncher
 *
 * @author Stephen Ramsey
 */
public class ModelLoaderApp extends CommandLineApp
{
    private static final String DEBUG_ARG = "-debug";
    private static final String PARSER_ARG = "-parser";
    private static final int NUM_REQUIRED_ARGS = 1;
    private static final String DEFAULT_PARSER_ALIAS = org.systemsbiology.chem.ModelBuilderCommandLanguage.CLASS_ALIAS;
    private boolean mDebug;
    private String mParserAlias;
    private String mFileName;
    private ClassRegistry mModelBuilderRegistry;

    private boolean getDebug()
    {
        return(mDebug);
    }

    private void setDebug(boolean pDebug)
    {
        mDebug = pDebug;
    }

    private void setParserAlias(String pParserAlias)
    {
        mParserAlias = pParserAlias;
    }

    private String getParserAlias()
    {
        return(mParserAlias);
    }

    private String getFileName()
    {
        return(mFileName);
    }

    private void setFileName(String pFileName)
    {
        mFileName = pFileName;
    }
   

    public ModelLoaderApp() throws ClassNotFoundException, IOException
    {
        setDebug(false);
        setParserAlias(null);
        setFileName(null);
        ClassRegistry classRegistry = new ClassRegistry(org.systemsbiology.chem.IModelBuilder.class);
        classRegistry.buildRegistry();
        setModelBuilderRegistry(classRegistry);
    }

    private ClassRegistry getModelBuilderRegistry()
    {
        return(mModelBuilderRegistry);
    }

    private void setModelBuilderRegistry(ClassRegistry pModelBuilderRegistry)
    {
        mModelBuilderRegistry = pModelBuilderRegistry;
    }

    protected void printUsage(OutputStream pOutputStream)
    {
        PrintWriter pw = new PrintWriter(pOutputStream);
        pw.println("usage:    java " + getClass().getName() + " [-debug] [-parser <parserAlias>] <modelFile>");
        pw.println("  <parserAlias>:   the alias of the class implementing the interface ");
        pw.println("                   org.systemsbiology.chem.IModelBuilder (default is determined");
        pw.println("                   by file extension");
        pw.println("  <modelFile>:     the full filename of the model definition file to be loaded");
        pw.println("Parser aliases:    ");
        pw.println("                   command-language (for Chemical Model Definition Language input)");
        pw.println("                   markup-language (for Systems Biology Markup Language input)");
        pw.println("If you do not specify a parser, the file suffix is used to select one");
        pw.flush();
    }

    

    private String selectParserAliasFromFileName(String pFileName) throws DataNotFoundException
    {
        ClassRegistry modelBuilderRegistry = getModelBuilderRegistry();
        Set parserAliases = modelBuilderRegistry.getRegistryAliasesCopy();

        Iterator parserAliasesIter = parserAliases.iterator();
        String retParserAlias = DEFAULT_PARSER_ALIAS;
        while(parserAliasesIter.hasNext())
        {
            String parserAlias = (String) parserAliasesIter.next();
            IModelBuilder modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(parserAlias);
            String fileRegex = modelBuilder.getFileRegex();
            if(pFileName.matches(fileRegex))
            {
                retParserAlias = parserAlias;
                break;
            }
        }

        return(retParserAlias);
    }

    protected void handleCommandLine(String []pArgs) 
    {
        checkAndHandleHelpRequest(pArgs);

        int numArgs = pArgs.length;

        if(numArgs < NUM_REQUIRED_ARGS)
        {
            handleCommandLineError("the number of command-line arguments is insufficient; please refer to the usage description that follows");
        }

        for(int argCtr = 0; argCtr < numArgs; ++argCtr)
        {
            String arg = pArgs[argCtr];

            if(arg.substring(0, DEBUG_ARG.length()).equals(DEBUG_ARG))
            {
                setDebug(true);
                continue;
            }

            if(arg.substring(0, PARSER_ARG.length()).equals(PARSER_ARG))
            {
                if(argCtr == numArgs - 1)
                {
                    handleCommandLineError("argument \"" + PARSER_ARG + "\" has a required element after it; please refer to the usage description that follows");
                }

                ++argCtr;
                String parserAlias = pArgs[argCtr];
                setParserAlias(parserAlias);
                continue;
            }

            if(argCtr == numArgs - 1)
            {
                setFileName(arg);
            }
        }

        if(null == getFileName())
        {
            handleCommandLineError("required file name was not specified; please refer to the usage description that follows");
        }

        if(null == getParserAlias())
        {
            try
            {
                setParserAlias(selectParserAliasFromFileName(getFileName()));
            }
            catch(DataNotFoundException e)
            {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

        if(getDebug())
        {
            System.out.println("using parser alias: " + getParserAlias());
        }

    }

    private void run(String []pArgs)
    {
        try
        {
            ClassRegistry modelBuilderRegistry = getModelBuilderRegistry();

            handleCommandLine(pArgs);

            String parserAlias = getParserAlias();

            if(getDebug())
            {
                System.out.println("building script...\n");
            }
            IModelBuilder modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(parserAlias);
            String fileName = getFileName();
            File file = new File(fileName);
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            IncludeHandler includeHandler = new IncludeHandler();
            File fileDir = file.getParentFile();
            includeHandler.setDirectory(fileDir);
            Model model = modelBuilder.buildModel(bufferedReader, includeHandler);
            if(getDebug())
            {
                System.out.println("script build process complete; model is: \n");
                System.out.print(model.toString());
                System.out.println("\n\nloading GUI...\n");
            }

            boolean handleOutputInternally = true;
            SimulationLauncher simLauncher = new SimulationLauncher("Dizzy",
                                                                    model,
                                                                    handleOutputInternally);
            simLauncher.addListener(new SimulationLauncher.Listener()
            {
                public void simulationLauncherClosing()
                {
                    System.exit(0);
                }
                public void simulationStarting()
                {
                    // do nothing
                }
                public void simulationEnding()
                {
                    // do nothing
                }
            });

        }

        catch(Exception e)
        {
            System.err.println("an exception occurred; the error message is:");
            System.err.println(e.toString());
            Throwable cause = e.getCause();
            if(null != cause)
            {
                System.err.println("cause of exception: " + e.getCause().toString());
            }
            if(getDebug())
            {
                System.err.println("\nthe detailed error message of this exception is:\n");
                System.err.println(e.getMessage() + "\n");
                System.err.println("\nthe stack backtrace of this exception is:\n");
                e.printStackTrace(System.err);
                if(null != cause)
                {
                    System.err.println("\nthe stack backtrace of the cause exception is:\n");
                    cause.printStackTrace(System.err);
                }
            }
            else
            {
                System.err.println("\nto see a stack backtrace of the exception, simply re-run this program with the \"" + DEBUG_ARG + "\" argument before the required arguments");
            }
            System.exit(1);
        }
    }

    public static void main(String []pArgs)
    {
        try
        {
            ModelLoaderApp app = new ModelLoaderApp();
            app.run(pArgs);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
