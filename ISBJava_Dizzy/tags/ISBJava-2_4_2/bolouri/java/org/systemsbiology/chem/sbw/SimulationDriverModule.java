/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.chem.sbw;

import java.io.FileNotFoundException;

import org.systemsbiology.gui.ExceptionNotificationOptionPane;
import org.systemsbiology.util.AppConfig;
import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.InvalidInputException;

import edu.caltech.sbw.*;

/**
 * @author sramsey
 *
 */
public class SimulationDriverModule
{
    public static final String MODULE_UNIQUE_NAME = "org.systemsbiology.chem.sbw.gui";
    public static final String MODULE_DISPLAY_NAME = "Simulation Driver";
    public static final String CATEGORY_NAME = "Analysis";    
    
    private static AppConfig mConfig;
    
    static
    {
        mConfig = null;
    }
    
    public static AppConfig getConfig()
    {
        return mConfig;
    }
    
    private static void loadConfig(String pAppDir) throws InvalidInputException, FileNotFoundException, DataNotFoundException
    {
        mConfig = AppConfig.get(SimulationDriverModule.class, pAppDir);
    }
    
    public static void main(String []pArgs)
    {
        try
        {
            if(pArgs.length < 2)
            {
                System.err.println("usage:  -sbwregister|-sbwmodule <config-dir-name>");
                System.err.println("command-line is: " + pArgs[0]);
                System.exit(1);
            }

            loadConfig(FileUtils.fixWindowsCommandLineDirectoryNameMangling(pArgs[1]));
            String appName = mConfig.getAppName();
            
            ModuleImpl moduleImp = new ModuleImpl(MODULE_UNIQUE_NAME, 
                                                  appName + " " + MODULE_DISPLAY_NAME, 
                                                  ModuleImpl.UNIQUE,
                                                  SimulationDriverModule.class);
            String commandLine = moduleImp.getCommandLine();

            moduleImp.addService(SimulationDriverService.SERVICE_NAME,
                    appName + " " + SimulationDriverService.SERVICE_DESCRIPTION,
                    CATEGORY_NAME,
                    SimulationDriverService.class);

            moduleImp.setCommandLine(commandLine + " \"" + pArgs[1] + "\"");

            if(pArgs[0].equals("-sbwregister"))
            {
                System.out.println("registering SBW simulation module");
            }
            else if(pArgs[0].equals("-sbwmodule"))
            {
                System.out.println("running SBW simulation module");
            }
            
            moduleImp.run(pArgs);            
             
        }
        
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e, e.getMessage());
            optionPane.createDialog(null, "Unable to register/launch simulation driver").show();            
        }
    }
}
