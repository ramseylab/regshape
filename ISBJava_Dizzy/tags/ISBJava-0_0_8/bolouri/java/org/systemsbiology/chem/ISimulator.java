package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.InvalidInputException;

public interface ISimulator
{
    public boolean isInitialized();

    public void initialize(Model pModel, 
                           SimulationController pSimulationController) throws DataNotFoundException, InvalidInputException;

    public SimulatorParameters getDefaultSimulatorParameters();
    
    public void simulate(double pStartTime, 
                         double pEndTime,
                         SimulatorParameters pSimulatorParameters,
                         int pNumResultsTimePoints,
                         String []pResultsSymbolNames,
                         double []pRetResultsTimeValues,
                         Object []pRetResultsSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, SimulationAccuracyException;
}
