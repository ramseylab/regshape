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

public final class DelayedReactionSolver 
{
    private static final double LAMBDA_MAX = 1.1;
    private static final int MIN_NUM_TIME_POINTS = 4000;
    private static final int REACTION_TIMES_DOUBLE_POOL_SIZE = 100;

    private final Species mReactant;
    private final Species mIntermedSpecies;
    private final double mRate;
    private final double mTimeResolution;
    private final double mPeakTimeRel;
    private boolean mFirstTimePoint;

    // used only for stochastic simulations
    private final Queue mReactionTimes;
    private final LinkedList mReactionTimesDoublePool;

    // used only for deterministic simulation
    private final SlidingWindowTimeSeriesQueue mReactantHistory;
    private final SlidingWindowTimeSeriesQueue mIntermedSpeciesHistory;
    private final int mNumTimePoints;

    private final boolean mIsStochasticSimulator;

    private final int mReactionIndex;
    private final double mDelay;

    public String toString()
    {
        return(mIntermedSpecies.getName());
    }

    public double getRate()
    {
        return(mRate);
    }

    public double getDelay()
    {
        return(mDelay);
    }

    public Species getIntermedSpecies()
    {
        return(mIntermedSpecies);
    }

    public DelayedReactionSolver(Species pReactant,
                                 Species pIntermedSpecies, 
                                 double pDelay,
                                 double pRate,
                                 int pReactionIndex,
                                 boolean pIsStochasticSimulator)
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

        mReactionIndex = pReactionIndex;

        mIsStochasticSimulator = pIsStochasticSimulator;

        if(mIsStochasticSimulator)
        {
//             mReactionTimes = new PriorityQueue( new AbstractComparator()
//             {
//                 public final int compare(Object p1, Object p2)
//                 {
//                     return(MutableDouble.compare((MutableDouble) p1, (MutableDouble) p2));
//                 }
//             });
            mReactionTimes = new Queue();

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

    void addReactant(SymbolEvaluatorChem pSymbolEvaluator)
    {
        MutableDouble newReactionTime = null;
        double reactionTime = pSymbolEvaluator.getTime() + mPeakTimeRel;
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

        mReactionTimes.add(newReactionTime);
    }

//     void addReactantOld(SymbolEvaluatorChem pSymbolEvaluator)
//     {
//         double time = pSymbolEvaluator.getTime();
//         MutableDouble newReactionTime = null;

//         double reactionTime = time + mPeakTimeRel;

//         LinkedList reactionTimesDoublePool = mReactionTimesDoublePool;


//         if(reactionTimesDoublePool.size() > 0)
//         {
//             newReactionTime = (MutableDouble) reactionTimesDoublePool.getLast();
//             reactionTimesDoublePool.removeLast();
//             newReactionTime.setValue(reactionTime);
//         }
//         else
//         {
//             newReactionTime = new MutableDouble(reactionTime);
//         }

//         mReactionTimes.offer(newReactionTime);
//     }

    double pollNextReactionTime() throws IllegalStateException
    {
        MutableDouble reactionTime = (MutableDouble) mReactionTimes.getNext();
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
        return(null != mReactionTimes.peekNext());
    }

    // used for stochastic simulator
    double peekNextReactionTime() throws IllegalStateException
    {
        MutableDouble reactionTime = (MutableDouble) mReactionTimes.peekNext();
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
            while(null != mReactionTimes.peekNext())
            {
                MutableDouble reactionTime = (MutableDouble) mReactionTimes.getNext();
                reactionTime.setValue(0.0);
                mReactionTimesDoublePool.addLast(reactionTime);
            }
        }
        else
        {
//            System.out.println("clearing reactant history");
            mReactantHistory.clear();
            mIntermedSpeciesHistory.clear();
            mFirstTimePoint = true;
        }
    }

    void initializeSpeciesSymbols(HashMap pSymbolMap,
                                  Species []pDynamicSymbolValues, // a vector of all species in the model
                                  SymbolValue []pNonDynamicSymbolValues) throws IllegalStateException
    {
        Symbol intermedSymbol = mIntermedSpecies.getSymbol();
        intermedSymbol.copyIndexInfo(Reaction.getIndexedSpecies(mIntermedSpecies,
                                                                pSymbolMap,
                                                                pDynamicSymbolValues,
                                                                pNonDynamicSymbolValues).getSymbol());

        Symbol reactantSymbol = mReactant.getSymbol();
        reactantSymbol.copyIndexInfo(Reaction.getIndexedSpecies(mReactant,
                                                                pSymbolMap,
                                                                pDynamicSymbolValues,
                                                                pNonDynamicSymbolValues).getSymbol());
    }

    public void update(SymbolEvaluator pSymbolEvaluator, double pTime) throws DataNotFoundException
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

            reactantHistory.insertPoint(pTime, reactantValue);
            intermedSpeciesHistory.insertPoint(pTime, intermedSpeciesValue);

            mFirstTimePoint = false;
        }
    }

    // used for deterministic simulator
    public double computeRate(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        double prodRate = 0.0;

        if(! mIsStochasticSimulator)
        {

            SymbolEvaluatorChem symbolEvaluator = (SymbolEvaluatorChem) pSymbolEvaluator;

            double currentTime = symbolEvaluator.getTime();

            SlidingWindowTimeSeriesQueue reactantSpeciesHistory = mReactantHistory;
            SlidingWindowTimeSeriesQueue intermedSpeciesHistory = mIntermedSpeciesHistory;

            double reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
            
            double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());
//            System.out.println("intermed species value: " + intermedSpeciesValue + "; average intermed value: " + averageIntermedValue);
            double minTime = reactantSpeciesHistory.getMinTime();
            double peakTimeRel = mPeakTimeRel;
            double peakTime = currentTime - peakTimeRel;

//            double minIntermed = Math.min(intermedSpeciesValue, ((LAMBDA_MAX - 1.0)*intermedSpeciesValue + averageIntermedValue)/LAMBDA_MAX);

            double averageIntermedValue = mIntermedSpeciesHistory.getAverageValue();

            if(intermedSpeciesValue > 0.0 && averageIntermedValue > 0.0 && peakTime >= minTime)
            {
                double averageReactantValue = reactantSpeciesHistory.getAverageValue();

                double minIntermed = Math.min(intermedSpeciesValue, averageIntermedValue);

                double numIntermedSpeciesExpected = (mDelay * mRate * averageReactantValue);

                double excessIntermed = minIntermed - numIntermedSpeciesExpected;
                if(excessIntermed < 0.0)
                {
                    excessIntermed = 0.0;
                }

                int peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                double peakValue = reactantSpeciesHistory.getValue(peakIndex);

//                System.out.println("peak value: " + peakValue + "; excessIntermed: " + excessIntermed);

                prodRate = mRate * Math.max(peakValue, excessIntermed);
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

    // used for deterministic simulator
    public double computeRateSave(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        double prodRate = 0.0;

        if(! mIsStochasticSimulator)
        {

            SymbolEvaluatorChem symbolEvaluator = (SymbolEvaluatorChem) pSymbolEvaluator;

            double currentTime = symbolEvaluator.getTime();

            SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
            SlidingWindowTimeSeriesQueue intermedSpeciesHistory = mIntermedSpeciesHistory;

            double reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
            
            double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());
            double averageIntermedValue = mIntermedSpeciesHistory.getAverageValue();
//            System.out.println("intermed species value: " + intermedSpeciesValue + "; average intermed value: " + averageIntermedValue);
            double minTime = reactantHistory.getMinTime();
            double peakTimeRel = mPeakTimeRel;
            double peakTime = currentTime - peakTimeRel;
            
            if(intermedSpeciesValue >= 0.0 && peakTime >= minTime)
            {
                double numIntermedSpeciesExpected = (mDelay * mRate) * reactantValue;
        
                if(intermedSpeciesValue > 0.0 &&
                   intermedSpeciesValue > numIntermedSpeciesExpected)
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
//     private static double computeIntegral(SlidingWindowTimeSeriesQueue history,
//                                           double h,
//                                           int numTimePoints,
//                                           double sqrtTwoPiNumStepsCorrected,
//                                           double numStepsCorrected,
//                                           double rate,
//                                           double currentTime)
//     {
//         double value = 0.0;
//         double prodRate = 0.0;
//         int numPoints = history.getNumStoredPoints();

//         for(int ctr = numPoints; --ctr >= 0; )
//         {
//             value = h * computeIntegrandValue(history,
//                                               ctr,
//                                               rate,
//                                               rate * rate,
//                                               sqrtTwoPiNumStepsCorrected,
//                                               numStepsCorrected,
//                                               currentTime);

//             if(ctr == 0 || ctr == numTimePoints - 1)
//             {
//                 prodRate += value/3.0;
//             }
//             else if((ctr % 2) == 1)
//             {
//                 prodRate += 2.0 * value / 3.0;
//             }
//             else
//             {
//                 prodRate += 4.0 * value / 3.0;
//             }
//         }
// //        System.out.println("rate: " + prodRate);
//         return(prodRate);
//     }

//     private static double computeIntegrandValue(SlidingWindowTimeSeriesQueue pReactantHistory,
//                                                 int pTimePointIndex,
//                                                 double pRate,
//                                                 double pRateSquared,
//                                                 double pSqrtTwoPiNumStepsCorrected,
//                                                 double numStepsCorrected,
//                                                 double pCurrentTime)
//     {
//         double reactantValue = pReactantHistory.getValue(pTimePointIndex);
//         double timePoint = pReactantHistory.getTimePoint(pTimePointIndex);
//         assert (pCurrentTime >= timePoint) : "time point is in the future";
//         double rate = pRate;
//         double lambda = rate * (pCurrentTime - timePoint);
//         double retVal = reactantValue * pRateSquared * 
//                         Math.pow(lambda * Math.E / numStepsCorrected, numStepsCorrected) / 
//                         (Math.exp(lambda) * pSqrtTwoPiNumStepsCorrected) ;
//         return( retVal );
//     }
}
