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

public class SimulatorOdeToJavaRungeKuttaAdaptive extends SimulatorOdeToJavaBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "ODEtoJava-dopr54-adaptive";

    protected void runExternalSimulation(Span pSimulationTimeSpan,
                                         double []pInitialDynamicSymbolValues,
                                         double pInitialStepSize,
                                         double pMaxAllowedRelativeError,
                                         double pMaxAllowedAbsoluteError,
                                         String pTempOutputFileName)
    {
        Btableau simulationButcherTableau = new Btableau("dopr54");
        ODE simulationModel = (ODE) this;
        double highOrderArgument = 5.0;

        int numSpecies = pInitialDynamicSymbolValues.length;
        double[] relativeTolerance = new double[numSpecies];
        for(int i = 0; i < numSpecies; ++i)
        {
            relativeTolerance[i] = pMaxAllowedRelativeError;
        }

        double[] absoluteTolerance = new double[numSpecies];
        for(int i = 0; i < numSpecies; ++i)
        {
            absoluteTolerance[i] = pMaxAllowedAbsoluteError;
        }

        ErkTriple erkTriple = new ErkTriple(simulationModel,
                                            pSimulationTimeSpan,
                                            pInitialDynamicSymbolValues,
                                            pInitialStepSize,
                                            simulationButcherTableau,
                                            highOrderArgument,
                                            absoluteTolerance,
                                            relativeTolerance,
                                            pTempOutputFileName,
                                            "StiffDetect_Off",
                                            "EventLoc_Halt",
                                            "Stats_Off");
        erkTriple.setAppend(false);
        erkTriple.setNPoints(1000);
        erkTriple.setRecorder(this);

        erkTriple.routine();

//         ErkTriple.erk_triple(simulationModel, 
//                              pSimulationTimeSpan, 
//                              pInitialDynamicSymbolValues, 
//                              pInitialStepSize,
//                              simulationButcherTableau,
//                              highOrderArgument,
//                              absoluteTolerance,
//                              relativeTolerance,
//                              pTempOutputFileName,
//                              "StiffDetect_Off",
//                              "EventLoc_Off",
//                              "Stats_Off");
    }

    public boolean allowsInterrupt()
    {
        return(true);
    }

    
}
