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

                moduleImp.run(pArgs);
            }

        catch(SBWException e)
            {
                e.handleWithDialog();
            }
    }
}
