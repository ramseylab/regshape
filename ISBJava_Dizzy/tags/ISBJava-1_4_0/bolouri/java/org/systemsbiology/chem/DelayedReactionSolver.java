package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;
import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

/**
 * Implements a chemical reaction containing a specified delay
 * time.  The reactant is immediately converted to a (hidden)
 * "intermediate species".  The reaction converting the intermediate
 * species to the product species occurs after the specified delay.
 *
 * @author Stephen Ramsey
 */

public class DelayedReactionSolver extends Expression
{
    private static final double LAMBDA_MAX = 4.0;
    private static final int MIN_NUM_TIME_POINTS = 4000;
    private static final int REACTION_TIMES_DOUBLE_POOL_SIZE = 100;

    private Species mReactant;
    private Species mIntermedSpecies;
    private double mRate;
    private boolean mFirstTimePoint;
    private double mTimeResolution;
    private double mPeakTimeRel;

    // used only for stochastic simulations
    private PriorityQueue mReactionTimes;
    private LinkedList mReactionTimesDoublePool;

    // used only for deterministic simulation
    private SlidingWindowTimeSeriesQueue mReactantHistory;
    private SlidingWindowTimeSeriesQueue mIntermedSpeciesHistory;
    private int mNumTimePoints;

    private boolean mIsStochasticSimulator;

    private double mRateSquared;
    private int mReactionIndex;
    private double mDelay;

    public DelayedReactionSolver(Species pReactant,
                                 Species pIntermedSpecies, 
                                 double pDelay,
                                 double pRate,
                                 int pReactionIndex)
    {
        assert (pDelay > 0.0) : "invalid delay";
        mDelay = pDelay;

        int numTimePoints = (int) (pDelay * LAMBDA_MAX / pRate);
        if(numTimePoints < MIN_NUM_TIME_POINTS)
        {
            numTimePoints = MIN_NUM_TIME_POINTS;
        }
        mNumTimePoints = numTimePoints;
        assert (numTimePoints > 0) : "invalid number of time points";

        mReactant = pReactant;
        mIntermedSpecies = pIntermedSpecies;

        mRate = pRate;
        
        mFirstTimePoint = true;

        mPeakTimeRel = pDelay;

        mTimeResolution = LAMBDA_MAX * mPeakTimeRel / ((double) numTimePoints);

        mRateSquared = mRate * mRate;

        mReactionIndex = pReactionIndex;
    }

    public void initialize(ISimulator pSimulator)
    {
        mIsStochasticSimulator = pSimulator.isStochasticSimulator();

        if(mIsStochasticSimulator)
        {
            mReactionTimes = new PriorityQueue( new AbstractComparator()
            {
                public int compare(Object p1, Object p2)
                {
                    return(MutableDouble.compare((MutableDouble) p1, (MutableDouble) p2));
                }
            });
            
            mReactionTimesDoublePool = new LinkedList();
            for(int ctr = 0; ctr < REACTION_TIMES_DOUBLE_POOL_SIZE; ++ctr)
            {
                mReactionTimesDoublePool.add(new MutableDouble(0.0));
            }

            mReactantHistory = null;
            mIntermedSpeciesHistory = null;
        }
        else
        {
            int numTimePoints = mNumTimePoints;
            mReactantHistory = new SlidingWindowTimeSeriesQueue(numTimePoints);
            mIntermedSpeciesHistory = new SlidingWindowTimeSeriesQueue(numTimePoints);

            mReactionTimesDoublePool = null;
            mReactionTimes = null;
        }
    }

    int getReactionIndex()
    {
        return(mReactionIndex);
    }

    boolean hasIntermedSpecies(Species pSpecies)
    {
        return(mIntermedSpecies.equals(pSpecies));
    }

    void addReactant(SymbolEvaluatorChemSimulation pSymbolEvaluator)
    {
        double time = pSymbolEvaluator.getTime();
        MutableDouble newReactionTime = null;

        double reactionTime = time + mPeakTimeRel;

        LinkedList reactionTimesDoublePool = mReactionTimesDoublePool;


        if(reactionTimesDoublePool.size() > 0)
        {
            newReactionTime = (MutableDouble) reactionTimesDoublePool.getLast();
            reactionTimesDoublePool.removeLast();
            newReactionTime.setValue(reactionTime);
        }
        else
        {
            newReactionTime = new MutableDouble(reactionTime);
        }

        mReactionTimes.offer(newReactionTime);
    }

    double pollNextReactionTime() throws IllegalStateException
    {
        MutableDouble reactionTime = (MutableDouble) mReactionTimes.poll();
        if(null == reactionTime)
        {
            throw new IllegalStateException("no molecules are in the multistep reaction queue");
        }
        double nextReactionTime = reactionTime.getValue();
        reactionTime.setValue(0.0);
        mReactionTimesDoublePool.addLast(reactionTime);
        return(nextReactionTime);
    }

    // used for stochastic simulator
    boolean canHaveReaction() 
    {
        return(null != mReactionTimes.peek());
    }

    // used for stochastic simulator
    double peekNextReactionTime() throws IllegalStateException
    {
        MutableDouble reactionTime = (MutableDouble) mReactionTimes.peek();
        if(null == reactionTime)
        {
            throw new IllegalStateException("no molecules are in the multistep reaction queue");
        }
        return(reactionTime.getValue());
    }

    double getEstimatedAverageFutureRate(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        double rate = pSymbolEvaluator.getValue(mIntermedSpecies.getSymbol()) / mPeakTimeRel;
        return(rate);
    }

    void clear()
    {
        if(mIsStochasticSimulator)
        {
            while(null != mReactionTimes.peek())
            {
                MutableDouble reactionTime = (MutableDouble) mReactionTimes.poll();
                reactionTime.setValue(0.0);
                mReactionTimesDoublePool.addLast(reactionTime);
            }
        }
        else
        {
            mReactantHistory.clear();
            mIntermedSpeciesHistory.clear();
            mFirstTimePoint = true;
        }
    }

    void initializeSpeciesSymbols(HashMap pSymbolMap,
                                  Species []pDynamicSymbolValues, // a vector of all species in the model
                                  SymbolValue []pNonDynamicSymbolValues) throws IllegalStateException
    {
        mIntermedSpecies = Reaction.getIndexedSpecies(mIntermedSpecies,
                                                      pSymbolMap,
                                                      pDynamicSymbolValues,
                                                      pNonDynamicSymbolValues);

        mReactant = Reaction.getIndexedSpecies(mReactant,
                                               pSymbolMap,
                                               pDynamicSymbolValues,
                                               pNonDynamicSymbolValues);
    }

    void update(SymbolEvaluator pSymbolEvaluator, double pTime) throws DataNotFoundException
    {
        SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
        SlidingWindowTimeSeriesQueue intermedSpeciesHistory = mIntermedSpeciesHistory;
        if(! mFirstTimePoint)
        {
            double lastTime = reactantHistory.getLastTimePoint();
            boolean gotValue = false;
            double reactantValue = 0.0;
            double intermedSpeciesValue = 0.0;
            while(pTime - lastTime > mTimeResolution)
            {
                if(! gotValue)
                {
                    reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
                    intermedSpeciesValue = pSymbolEvaluator.getValue(mIntermedSpecies.getSymbol());
                }
                lastTime += mTimeResolution;

                assert (reactantValue >= 0.0) : "invalid value";
//                System.out.println("inserting reactant value: " + reactantValue);
                reactantHistory.insertPoint(lastTime, reactantValue);

                assert (intermedSpeciesValue >= 0.0) : "invalid value";
                intermedSpeciesHistory.insertPoint(lastTime, intermedSpeciesValue);
            }
        }
        else
        {
            double reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
            assert (reactantValue >= 0.0) : "invalid value";

            double intermedSpeciesValue = pSymbolEvaluator.getValue(mIntermedSpecies.getSymbol());
            assert (intermedSpeciesValue >= 0.0) : "invalid value";

//            System.out.println("inserting reactant value: " + reactantValue);
            reactantHistory.insertPoint(pTime, reactantValue);
            intermedSpeciesHistory.insertPoint(pTime, intermedSpeciesValue);

//            System.out.println("inserting point into history; time: " + pTime + "; value: " + value);
            mFirstTimePoint = false;
        }
    }

    // used for deterministic simulator
    public double computeValue(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        double prodRate = 0.0;

        if(! mIsStochasticSimulator)
        {

            SymbolEvaluatorChemSimulation symbolEvaluator = (SymbolEvaluatorChemSimulation) pSymbolEvaluator;

            double currentTime = symbolEvaluator.getTime();

            SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
            SlidingWindowTimeSeriesQueue intermedSpeciesHistory = mIntermedSpeciesHistory;

            double reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
            
            double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());

            if(intermedSpeciesValue >= 0.0)
            {
                update(symbolEvaluator, currentTime);

                double averageIntermedValue = mIntermedSpeciesHistory.getAverageValue();
                double numIntermedSpeciesExpected = (mDelay * mRate) * reactantValue;
        
                double minTime = reactantHistory.getMinTime();
                double peakTimeRel = mPeakTimeRel;
                double peakTime = currentTime - peakTimeRel;
            
                if(intermedSpeciesValue > 0.0 &&
                   intermedSpeciesValue > numIntermedSpeciesExpected && peakTime >= minTime)
                {
                    double rate = mRate;
                    int peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                    double peakValue = reactantHistory.getValue(peakIndex);
                    
                    if(peakValue > 0.0)
                    {
                        prodRate = rate * peakValue;
                    }
                    else
                    {
                        if(averageIntermedValue > numIntermedSpeciesExpected)
                        {
                            double excessIntermed = averageIntermedValue - numIntermedSpeciesExpected;
                            prodRate = rate * excessIntermed;
                        }
                        else
                        {
                            // do nothing
                        }
                    }
                }
                else
                {
                    // do nothing; rate of production is zero, by mass conservation law
                }
            }
            else
            {
                // do nothing; rate of production is zero
            }
        }
        else
        {
            // do nothing; just use a rate of zero
        }

        return(prodRate);
    }



    // keeping this around for historical purposes
    private static final double computeIntegral(SlidingWindowTimeSeriesQueue history,
                                                double h,
                                                int numTimePoints,
                                                double sqrtTwoPiNumStepsCorrected,
                                                double numStepsCorrected,
                                                double rate,
                                                double rateSquared,
                                                double currentTime)
    {
        double value = 0.0;
        double prodRate = 0.0;
        int numPoints = history.getNumStoredPoints();

        for(int ctr = numPoints; --ctr >= 0; )
        {
            value = h * computeIntegrandValue(history,
                                              ctr,
                                              rate,
                                              rateSquared,
                                              sqrtTwoPiNumStepsCorrected,
                                              numStepsCorrected,
                                              currentTime);

//                System.out.println("evaluating integrand; time index: " + ctr + "; value: " + value);
            if(ctr == 0 || ctr == numTimePoints - 1)
            {
                prodRate += value/3.0;
            }
            else if((ctr % 2) == 1)
            {
                prodRate += 2.0 * value / 3.0;
            }
            else
            {
                prodRate += 4.0 * value / 3.0;
            }
        }
//        System.out.println("rate: " + prodRate);
        return(prodRate);
    }

    private static final double computeIntegrandValue(SlidingWindowTimeSeriesQueue pReactantHistory,
                                                      int pTimePointIndex,
                                                      double pRate,
                                                      double pRateSquared,
                                                      double pSqrtTwoPiNumStepsCorrected,
                                                      double numStepsCorrected,
                                                      double pCurrentTime)
    {
        double reactantValue = pReactantHistory.getValue(pTimePointIndex);
        double timePoint = pReactantHistory.getTimePoint(pTimePointIndex);
        assert (pCurrentTime >= timePoint) : "time point is in the future";
        double rate = pRate;
        double lambda = rate * (pCurrentTime - timePoint);
        double retVal = reactantValue * pRateSquared * 
                        Math.pow(lambda * Math.E / numStepsCorrected, numStepsCorrected) / 
                        (Math.exp(lambda) * pSqrtTwoPiNumStepsCorrected) ;
        return( retVal );
    }
}
