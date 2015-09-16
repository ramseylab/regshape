package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

/**
 * Simulates the dynamics of a set of coupled chemical reactions
 * described by {@link Reaction} objects using the Runge-Kutta
 * algorithm (fifth order with adaptive step-size control).
 *
 * @author Stephen Ramsey
 */
public final class SimulatorDeterministicRungeKuttaFixed extends SimulatorDeterministicBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "ODE-RK5-fixed";
    private static final int NUM_ITERATIONS_BEFORE_ERROR_CHECK = 10;

    // fixed step-size integrator
    protected double iterate(double []pNewDynamicSymbolValues) throws DataNotFoundException, SimulationAccuracyException
    {
        double stepSize = mRKScratchPad.stepSize;

        int numIterations = mRKScratchPad.numIterations;
        if(0 != numIterations % NUM_ITERATIONS_BEFORE_ERROR_CHECK)
        {
            rk4step(stepSize,  
                    pNewDynamicSymbolValues);
        }
        else
        {
            double []yscale = mRKScratchPad.yscale;

            computeScale(stepSize, yscale);

            MutableDouble relativeErrorObj = mRKScratchPad.relativeError;
            MutableDouble absoluteErrorObj = mRKScratchPad.absoluteError;

            rkqc(stepSize,  
                 yscale,
                 pNewDynamicSymbolValues,
                 relativeErrorObj,
                 absoluteErrorObj);

            double maxRelativeError = mRKScratchPad.maxRelativeError;
            double relativeError = relativeErrorObj.getValue();

            if(maxRelativeError - relativeError < 0.0)
            {
                throw new SimulationAccuracyException("numeric approximation error exceeded threshold; try a larger value for \"min number of timesteps\"");
            }

            double maxAbsoluteError = mRKScratchPad.maxAbsoluteError;
            double absoluteError = absoluteErrorObj.getValue();

            if(maxAbsoluteError - absoluteError < 0.0)
            {
                throw new SimulationAccuracyException("numeric approximation error exceeded threshold; try a larger value for \"min number of timesteps\"");
            }
        }

        mSymbolEvaluator.setTime(mSymbolEvaluator.getTime() + stepSize);

        return(mSymbolEvaluator.getTime());
    }

    protected double getMaxStepSize(double pDeltaTime,
                                    long pNumResultsTimePoints,
                                    SimulatorParameters pSimulatorParams)
    {
        // let the maximum step-size be constrained only by the number of results
        // time-points requested 
        double maxStepSize = pDeltaTime / ((double) pNumResultsTimePoints);
        return(maxStepSize);
    }

    protected void setupImpl(double pDeltaTime,
                             int pNumResultsTimePoints,
                             SimulatorParameters pSimulatorParams,
                             RKScratchPad pRKScratchPad)
    {
        // nothing to do
    }

    public String getAlias()
    {
        return(CLASS_ALIAS);
    }
}
