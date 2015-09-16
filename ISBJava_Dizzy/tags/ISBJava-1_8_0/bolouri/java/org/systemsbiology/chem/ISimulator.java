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
    public static final int MIN_NUM_RESULTS_TIME_POINTS = 2;
    public String getAlias();

    public boolean isInitialized();

    public void initialize(Model pModel) throws DataNotFoundException, InvalidInputException;
    public void setProgressReporter(SimulationProgressReporter pSimulationProgressReporter);
    public void setController(SimulationController pSimulationController);
    public void checkSimulationParameters(double pStartTime,
                                          double pEndTime,
                                          SimulatorParameters pSimulatorParameters,
                                          int pNumResultsTimePoints);

    public SimulatorParameters getDefaultSimulatorParameters();

    /**
     * Set the update interval, in seconds.  The
     * updates are provided through the
     * {@link SimulationProgressReporter} class.  This
     * also sets the interval for checking the
     * {@link SimulationController} for pause or cancellation.
     * The value specified must be greater than zero.
     * If this method is not called, a default value of 1.0 
     * seconds is used.
     */
    public void setStatusUpdateIntervalSeconds(double pStatusUpdateIntervalSeconds) throws IllegalArgumentException;

    public boolean allowsInterrupt();

    /**
     * Conduct a simulation of the dynamics of the {@link Model} passed to the
     * {@link #initialize(Model)} method.  The end time
     * must be greater than the start time.  The integer <code>pNumResultsTimePoints</code>
     * must be greater than zero.  The size of the results arrays must be equal to
     * one plus <code>pNumResultsTimePoints</code> (the extra element in the arrays is to
     * hold the initial data).  The parameter <code>pNumResultsTimePoints</code> must
     * be greater than or equal to 2.
     *
     */
    public SimulationResults simulate(double pStartTime, 
                                      double pEndTime,
                                      SimulatorParameters pSimulatorParameters,
                                      int pNumResultsTimePoints,
                                      String []pResultsSymbolNames) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, SimulationAccuracyException, SimulationFailedException;
}
