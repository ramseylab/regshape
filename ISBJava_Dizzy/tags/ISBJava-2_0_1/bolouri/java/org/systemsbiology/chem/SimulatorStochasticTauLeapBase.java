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
 * described by {@link Reaction} objects using the Gillespie stochastic
 * algorithm, "tau-leap method".
 *
 * @author Stephen Ramsey
 */
public abstract class SimulatorStochasticTauLeapBase extends SimulatorStochasticBase 
{
    private static final long NUM_FIRINGS_GILLESPIE = 1;
    private static final double DEFAULT_MAX_ALLOWED_RELATIVE_ERROR = 0.005;
    private static final double DEFAULT_STEP_SIZE_FRACTION = 0.1;
    private static final long MULTIPLIER_FOR_MIN_NUM_GILLESPIE_STEPS = 4;
    private static final int MAX_FAILED_LEAP_ATTEMPTS_BEFORE_ABORT = 40;
    private static final int NUM_EVALUATIONS_BEFORE_RECOMPUTE_FJJP = 10;
    private static final long MIN_NUM_MILLISECONDS_FOR_PROGRESS_UPDATE = 4000;

    private double []mEstimatedSpeciesChange;
    protected boolean []mReactionHasLocalSymbolsFlags;

    protected double mAllowedError;
    protected long mNumNonLeapIterationsSinceLastLeapCheck;
    protected boolean mLastIterationWasLeap;
    protected long mMinRatioOfLeapTimeToReactionTimeScale;

    public static final String CLASS_ALIAS = "gillespie-tauleap"; 

    protected void prepareForStochasticSimulation(double pStartTime,
                                                  SimulatorParameters pSimulatorParameters) throws IllegalArgumentException
    {
        Double maxAllowedError = pSimulatorParameters.getMaxAllowedRelativeError();
        if(null == maxAllowedError)
        {
            throw new IllegalArgumentException("required simulator parameter maxAllowedRelativeError was not specified");
        }
        mAllowedError = maxAllowedError.doubleValue();

        Double stepSizeFraction = pSimulatorParameters.getStepSizeFraction();
        if(null == stepSizeFraction)
        {
            throw new IllegalArgumentException("required simulator step size fraction was not supplied");
        }

        mMinRatioOfLeapTimeToReactionTimeScale = (long) (1.0 / stepSizeFraction.doubleValue());
        mNumNonLeapIterationsSinceLastLeapCheck = 0;
        mLastIterationWasLeap = true;
    }

    protected double iterate(MutableInteger pLastReactionIndex) throws DataNotFoundException, IllegalStateException, SimulationAccuracyException
    {
        double time = mSymbolEvaluator.getTime();
//        System.out.println("time at start of iteration: " + time);

        int lastReactionIndex = pLastReactionIndex.getValue();
        if(NULL_REACTION != lastReactionIndex)
        {
            updateSymbolValuesForReaction(lastReactionIndex,
                                          mDynamicSymbolValues,
                                          mDynamicSymbolDelayedReactionAssociations,
                                          NUM_FIRINGS_GILLESPIE);
        }

        int numReactions = mReactions.length;
        int numSpecies = mDynamicSymbols.length;

        computeReactionProbabilities();

        double aggregateReactionProbability = MathFunctions.vectorSumElements(mReactionProbabilities);

        double leapTime = 0.0;
        boolean doLeap = false;
        if(mLastIterationWasLeap || 
           mNumNonLeapIterationsSinceLastLeapCheck >= MULTIPLIER_FOR_MIN_NUM_GILLESPIE_STEPS * mMinRatioOfLeapTimeToReactionTimeScale)
        {
//              getMaxNumberFiringsForReactions(pSymbolEvaluator,
//                                              pReactions,
//                                              pDynamicSymbolValues,
//                                              mMaxNumReactionFirings);

             leapTime = getLargestJumpConsistentWithAllowedError(aggregateReactionProbability,
                                                                 mEstimatedSpeciesChange);

            if(leapTime >= (mMinRatioOfLeapTimeToReactionTimeScale / aggregateReactionProbability))
            {
                doLeap = true;
            }

            mNumNonLeapIterationsSinceLastLeapCheck = 0;
        }

//        System.out.println("max allowed jump time: " + leapTime + "; avg reaction time: " + Double.toString(1.0/aggregateReactionProbability));

//        System.out.println("time: " + time + "; tau: " + leapTime);

        mLastIterationWasLeap = doLeap;

        if(! doLeap)
        {
            // it is not worth it to leap
            double deltaTimeToNextReaction = Double.POSITIVE_INFINITY;
            ++mNumNonLeapIterationsSinceLastLeapCheck;

            if(aggregateReactionProbability > 0.0)
            {
                deltaTimeToNextReaction = chooseDeltaTimeToNextReaction(aggregateReactionProbability);
            }            

            int reactionIndex = -1;

            if(null == mDelayedReactionSolvers)
            {
                // do nothing
            }
            else
            {
                int nextDelayedReactionIndex = getNextDelayedReactionIndex(mDelayedReactionSolvers);
                if(nextDelayedReactionIndex >= 0)
                {
                    DelayedReactionSolver solver = mDelayedReactionSolvers[nextDelayedReactionIndex];
                    double nextDelayedReactionTime = solver.peekNextReactionTime();
//                System.out.println("next delayed reaction will occur at: " + nextDelayedReactionTime);
                    if(nextDelayedReactionTime < time + deltaTimeToNextReaction)
                    {
                        // execute delayed reaction
                        deltaTimeToNextReaction = nextDelayedReactionTime - time;
                        reactionIndex = solver.getReactionIndex();
//                    System.out.println("delayed reaction selected: " + pReactions[reactionIndex]);
                        solver.pollNextReactionTime();
                    }
                }
            }

            if(-1 == reactionIndex && aggregateReactionProbability > 0.0)
            {
                reactionIndex = chooseIndexOfNextReaction(aggregateReactionProbability);
            }


            if(-1 != reactionIndex)
            {
//                System.out.println("normal gillespie step, delta time: " + deltaTimeToNextReaction + "; reaction: " + pReactions[reactionIndex].toString());

                pLastReactionIndex.setValue(reactionIndex);
                
                time += deltaTimeToNextReaction;
                
//            System.out.println("time: " + time + "; reaction occurred: " + reaction);
            }
            else
            {
                time = Double.POSITIVE_INFINITY;
            }
            mSymbolEvaluator.setTime(time);
        }
        else
        {
//            System.out.println("LEAP TIME: " + leapTime);
            // leaping will help us
            double allowedError = mAllowedError;
            time += leapTime;

            pLastReactionIndex.setValue(NULL_REACTION);

            // update symbol values right away, since we are no longer assuming that
            // no reactions occur during the time jump

//            System.out.println("tau-leap");

            boolean successfulLeap = false;
            for(int failedLeaps = 0; failedLeaps < MAX_FAILED_LEAP_ATTEMPTS_BEFORE_ABORT; ++failedLeaps)
            {
                successfulLeap = attemptLeap(leapTime);
                if(successfulLeap)
                {
                    break;
                }
            }

            if(! successfulLeap)
            {
                throw new SimulationAccuracyException("simulation became unstable; please re-run with a smaller value for the error control parameter");
            }

            MathFunctions.vectorAdd(mEstimatedSpeciesChange, mDynamicSymbolValues, mDynamicSymbolValues);

            if(null != mDelayedReactionSolvers)
            {
                double nextDelayedReactionTime = 0.0;

                DelayedReactionSolver solver = null;
                int nextDelayedReactionIndex = NULL_REACTION;

                while(nextDelayedReactionTime <= time)
                {
                    nextDelayedReactionIndex = getNextDelayedReactionIndex(mDelayedReactionSolvers);
                    if(nextDelayedReactionIndex > NULL_REACTION)
                    {
                        solver = mDelayedReactionSolvers[nextDelayedReactionIndex];
                        nextDelayedReactionTime = solver.peekNextReactionTime();
                        if(nextDelayedReactionTime <= time)
                        {
                            updateSymbolValuesForReaction(solver.getReactionIndex(),
                                                          mDynamicSymbolValues,
                                                          mDynamicSymbolDelayedReactionAssociations,
                                                          NUM_FIRINGS_GILLESPIE);
                            solver.pollNextReactionTime();
                        }
                    }      
                    else
                    {
                        break;
                    }
                }
            }            

            mSymbolEvaluator.setTime(time);
        }

//        System.out.println("time at end of iteration: " + time + "\n");

        return(time);
    }

    private boolean attemptLeap(double pLeapTime) throws DataNotFoundException
    {
        MathFunctions.vectorZeroElements(mEstimatedSpeciesChange);

        int numSpecies = mDynamicSymbolValues.length;
        int numReactions = mReactions.length;

        double lambda = 0.0;
        long numFirings = 0;
        boolean gotSuccessfulNumFirings = false;
        for(int j = numReactions; --j >= 0; )
        {
            lambda = pLeapTime * mReactionProbabilities[j];
            if(lambda > 0.0)
            {
                if(1.0/Math.sqrt(lambda) > mAllowedError)
                {
                    numFirings = (long) getPoissonEvent(mPoissonEventGenerator, lambda);
                }
                else
                {
                    numFirings = Math.round(lambda);
                }
                updateSymbolValuesForReaction(j, 
                                              mEstimatedSpeciesChange, 
                                              null, 
                                              numFirings);
            }
        }

        boolean succeeded = true;
        for(int i = numSpecies; --i >= 0; )
        {
            if(mEstimatedSpeciesChange[i] + mDynamicSymbolValues[i] < 0.0)
            {
                succeeded = false;
                break;
            }
        }

        return(succeeded);
    }

    protected abstract double computeLeapTime(double pSumReactionProbabilities) throws DataNotFoundException;

    private double getLargestJumpConsistentWithAllowedError(double pSumReactionProbabilities,
                                                            double []pEstimatedSpeciesChange) throws DataNotFoundException
    {
        double jumpTime = computeLeapTime(pSumReactionProbabilities);

        int numReactions = mReactionProbabilities.length;
        int numSpecies = mDynamicSymbolValues.length;

        double estimatedSpeciesChange = 0.0;
        double estimatedNumFirings = 0.0;
        double rate = 0.0;
        double numFirings = 0.0;
        double []reactionProbabilities = mReactionProbabilities;
        MathFunctions.vectorZeroElements(pEstimatedSpeciesChange);

        for(int j = numReactions; --j >= 0; )
        {
            rate = reactionProbabilities[j];
            numFirings = rate * jumpTime;

            if(numFirings > 0.0)
            {
                updateSymbolValuesForReaction(j, 
                                              pEstimatedSpeciesChange, 
                                              null,
                                              (long) numFirings);
            }
        }

        double frac = 0.0;
        double minFrac = 1.0;
        double oldSpeciesValue = 0.0;
        double []dynamicSpeciesValues = mDynamicSymbolValues;

        for(int i = numSpecies; --i >= 0; )
        {
            oldSpeciesValue = dynamicSpeciesValues[i];
            if(oldSpeciesValue > 0.0)
            {
                if(oldSpeciesValue + pEstimatedSpeciesChange[i] < 0.0)
                {
                    frac = -0.5 * oldSpeciesValue/pEstimatedSpeciesChange[i];
                    if(frac < minFrac)
                    {
                        minFrac = frac;
                    }
                }
            }
        }

        if(minFrac < 1.0)
        {
//            System.out.println("modifying jump time from : " + jumpTime + " by fraction: " + minFrac);
            jumpTime *= minFrac;
        }

        return(jumpTime);
    }

    protected abstract void initializeTauLeap(SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException, InvalidInputException;


    private void initializeReactionHasLocalSymbolsFlags()
    {
        int numReactions = mReactions.length;
        mReactionHasLocalSymbolsFlags = new boolean[numReactions];
        for(int j = 0; j < numReactions; ++j)
        {
            mReactionHasLocalSymbolsFlags[j] = mReactions[j].hasLocalSymbols();
        }
    }


    public void initialize(Model pModel) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel);
        initializeSimulatorStochastic(pModel);
        initializeDynamicSymbolAdjustmentVectors();
        initializeReactionHasLocalSymbolsFlags();
        initializeTauLeap(mSymbolEvaluator);
        mEstimatedSpeciesChange = new double[mDynamicSymbolValues.length];
        mMinNumMillisecondsForUpdate = MIN_NUM_MILLISECONDS_FOR_PROGRESS_UPDATE;
    }

    public String getAlias()
    {
        return(CLASS_ALIAS);
    }

    protected void modifyDefaultSimulatorParameters(SimulatorParameters pSimulatorParameters)
    {
        SimulatorParameters sp = pSimulatorParameters;
        sp.setMaxAllowedRelativeError(DEFAULT_MAX_ALLOWED_RELATIVE_ERROR);
        sp.setStepSizeFraction(DEFAULT_STEP_SIZE_FRACTION);
    }

    protected void checkSimulationParametersImpl(SimulatorParameters pSimulatorParameters,
                                                 int pNumResultsTimePoints)
    {
        super.checkSimulationParametersImpl(pSimulatorParameters,
                                            pNumResultsTimePoints);

        Double maxAllowedRelativeErrorObj = pSimulatorParameters.getMaxAllowedRelativeError();
        if(null == maxAllowedRelativeErrorObj)
        {
            throw new IllegalArgumentException("missing max allowed relative error");
        }
        double maxAllowedRelativeError = maxAllowedRelativeErrorObj.doubleValue();
        if(maxAllowedRelativeError <= 0.0 || maxAllowedRelativeError >= 1.0)
        {
            throw new IllegalArgumentException("invalid max allowed relative error: " + maxAllowedRelativeError);
        }
        Double stepSizeFractionObj = pSimulatorParameters.getStepSizeFraction();
        if(null == stepSizeFractionObj)
        {
            throw new IllegalArgumentException("missing step size fraction");
        }
        double stepSizeFraction = stepSizeFractionObj.doubleValue();
        if(stepSizeFraction <= 0.0)
        {
            throw new IllegalArgumentException("invalid step size fraction: " + stepSizeFraction);
        }
    }

}
