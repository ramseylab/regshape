package org.systemsbiology.chem.sbw;

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

                moduleImp.run(pArgs);
            }

        catch(SBWException e)
            {
                e.handleWithDialog();
            }
    }
}
