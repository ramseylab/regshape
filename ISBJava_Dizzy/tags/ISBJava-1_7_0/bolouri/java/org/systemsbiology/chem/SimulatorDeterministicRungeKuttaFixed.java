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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

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

    protected void setupScratchPad(double pStartTime,
                                   double pEndTime,
                                   SimulatorParameters pSimulatorParams, 
                                   RKScratchPad pRKScratchPad)
    {
        Double maxAbsoluteErrorObj = pSimulatorParams.getMaxAllowedAbsoluteError();
        if(null != maxAbsoluteErrorObj)
        {
            double maxAbsoluteError = maxAbsoluteErrorObj.doubleValue();
            pRKScratchPad.maxAbsoluteError = maxAbsoluteError;
        }
        else
        {
            throw new IllegalArgumentException("max absolute error must be specified");
        }

        Double maxRelativeErrorObj = pSimulatorParams.getMaxAllowedRelativeError();
        if(null != maxRelativeErrorObj)
        {
            double maxRelativeError = maxRelativeErrorObj.doubleValue();
            pRKScratchPad.maxRelativeError = maxRelativeError;
        }
        else
        {
            throw new IllegalArgumentException("max relative error must be specified");
        }

        long minNumSteps;
        Long minNumStepsObj = pSimulatorParams.getMinNumSteps();
        if(null != minNumStepsObj)
        {
            minNumSteps = minNumStepsObj.longValue();
            if(minNumSteps <= 0)
            {
                throw new IllegalArgumentException("illegal value for number of steps");
            }
        }
        else
        {
            throw new IllegalArgumentException("required minimum number of steps was not provided");
        }

        double stepSize = (pEndTime - pStartTime) / ((double) minNumSteps);

        pRKScratchPad.stepSize = stepSize;
    }

    public String getAlias()
    {
        return(CLASS_ALIAS);
    }
}
