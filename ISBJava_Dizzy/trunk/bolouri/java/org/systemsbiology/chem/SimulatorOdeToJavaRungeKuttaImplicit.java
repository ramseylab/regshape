package org.systemsbiology.chem;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import odeToJava.*;
import odeToJava.modules.*;
import org.systemsbiology.util.*;

public class SimulatorOdeToJavaRungeKuttaImplicit extends SimulatorOdeToJavaBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "ODEtoJava-imex443-stiff";

    protected void runExternalSimulation(Span pSimulationTimeSpan,
                                         double []pInitialDynamicSymbolValues,
                                         double pInitialStepSize,
                                         double pMaxAllowedRelativeError,
                                         double pMaxAllowedAbsoluteError,
                                         String pTempOutputFileName)
    {
        Btableau simulationButcherTableau = new Btableau("imex443");
        ODE simulationModel = (ODE) this;

        ImexSD.imex_sd(simulationModel, 
                       pSimulationTimeSpan, 
                       pInitialDynamicSymbolValues, 
                       pInitialStepSize,
                       simulationButcherTableau,
                       pMaxAllowedAbsoluteError,
                       pMaxAllowedRelativeError,
                       pTempOutputFileName,
                       "Stats_Off");
    }

    
}
