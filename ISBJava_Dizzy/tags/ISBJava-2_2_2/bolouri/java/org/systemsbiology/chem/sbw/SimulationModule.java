package org.systemsbiology.chem.sbw;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import edu.caltech.sbw.*;

public class SimulationModule
{
    public static final String MODULE_UNIQUE_NAME = "org.systemsbiology.chem.Simulator";
    public static final String MODULE_DISPLAY_NAME = "ISBSimulator";

    public static void main(String []pArgs)
    {
        try
        {
            // -------------------------------
            // :TODO: remove this next section when we upgrade to the SBWCore.jar from 
            // SBW version 2.3.1.  This block of code works around a SBW bug on non-Windows 
            // platforms.
            if (edu.caltech.sbw.Sys.OSIsMac() ||
                    edu.caltech.sbw.Sys.OSIsUnix())
            {
                System.setProperty("java.net.preferIPv4Stack", "true");
                System.setProperty("sbw.broker.allow-remote-modules", "true");
            }
            // -------------------------------
            
            ModuleImpl moduleImp = new ModuleImpl(MODULE_UNIQUE_NAME, 
                    MODULE_DISPLAY_NAME, 
                    ModuleImpl.SELF_MANAGED,
                    SimulationModule.class);
            
            moduleImp.addService(GillespieService.SERVICE_NAME,
                    GillespieService.SERVICE_DESCRIPTION,
                    "Simulation",
                    GillespieService.class);
            
            moduleImp.addService(GibsonBruckService.SERVICE_NAME,
                    GibsonBruckService.SERVICE_DESCRIPTION,
                    "Simulation",
                    GibsonBruckService.class);
            
            moduleImp.addService(DeterministicService.SERVICE_NAME,
                    DeterministicService.SERVICE_DESCRIPTION,
                    "Simulation",
                    DeterministicService.class);
            
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

        catch(SBWException e)
            {
                e.handleWithDialog();
            }
    }
}
