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

    public boolean isStochasticSimulator();

    public boolean allowsInterrupt();

    /**
     * Conduct a simulation of the dynamics of the {@link Model} passed to the
     * {@link #initialize(Model,SimulationController)} method.  The end time
     * must be greater than the start time.  The integer <code>pNumResultsTimePoints</code>
     * must be greater than zero.  The size of the results arrays must be equal to
     * one plus <code>pNumResultsTimePoints</code> (the extra element in the arrays is to
     * hold the initial data).
     *
     */
    public void simulate(double pStartTime, 
                         double pEndTime,
                         SimulatorParameters pSimulatorParameters,
                         int pNumResultsTimePoints,
                         String []pResultsSymbolNames,
                         double []pRetResultsTimeValues,
                         Object []pRetResultsSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, SimulationAccuracyException, SimulationFailedException;
}
