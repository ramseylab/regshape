package org.systemsbiology.chem.sbw;
import org.systemsbiology.chem.*;
import edu.caltech.sbw.*;

public class GillespieService extends SimulationService
{
    /*========================================*
     * constants
     *========================================*/
    public static final String SERVICE_NAME = "Gillespie";
    public static final String SERVICE_DESCRIPTION = "Simulate this model using Gillespie\'s stochastic algorithm";

    /*========================================*
     * member data
     *========================================*/
    /*========================================*
     * accessor/mutator methods
     *========================================*/

    /*========================================*
     * initialization methods
     *========================================*/

    /*========================================*
     * constructors
     *========================================*/
    public GillespieService() throws SBWException
    {
        super();
    }

    /*========================================*
     * private methods
     *========================================*/

    /*========================================*
     * protected methods
     *========================================*/
    public ISimulator createSimulator()
    {
        return(new GillespieSimulator());
    }

    /*========================================*
     * public methods
     *========================================*/
}
