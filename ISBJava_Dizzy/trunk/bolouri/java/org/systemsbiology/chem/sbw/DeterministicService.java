package org.systemsbiology.chem.sbw;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

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
        return(new SimulatorDeterministicRungeKuttaAdaptive());
    }

    /*========================================*
     * public methods
     *========================================*/
}
