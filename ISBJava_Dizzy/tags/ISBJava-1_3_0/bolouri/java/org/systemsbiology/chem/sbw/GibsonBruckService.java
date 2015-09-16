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

public class GibsonBruckService extends SimulationService
{
    /*========================================*
     * constants
     *========================================*/
    public static final String SERVICE_NAME = "GibsonBruck";
    public static final String SERVICE_DESCRIPTION = "Simulate this model using Gibson and Bruck\'s stochastic algorithm";

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
    public GibsonBruckService() throws SBWException
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
        return(new SimulatorStochasticGibsonBruck());
    }

    /*========================================*
     * public methods
     *========================================*/
}
