package org.systemsbiology.chem.sbw;
import org.systemsbiology.chem.*;
import edu.caltech.sbw.*;

public class DeterministicService extends SimulationService
{
    /*========================================*
     * constants
     *========================================*/
    public static final String SERVICE_NAME = "Deterministic";
    public static final String SERVICE_DESCRIPTION = "Simulate this model using a deterministic ODE simulator";

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
    public DeterministicService() throws SBWException
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
        return(new DeterministicSimulatorAdaptive());
    }

    /*========================================*
     * public methods
     *========================================*/
}
