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

import org.systemsbiology.data.*;
import org.systemsbiology.math.*;
import org.systemsbiology.util.*;
import cern.jet.random.*;

/**
 * Used to simulate a chemical reaction containing a specified delay
 * time.  The reactant is immediately converted to a (hidden)
 * "intermediate species".  The reaction converting the intermediate
 * species to the product species occurs after the specified delay.
 * This class is used by subclasses of the {@link Simulator} class.
 * The application developer will rarely need to work directly with 
 * an instance of this class.
 * 
 * @author Stephen Ramsey
 */

public final class DelayedReactionSolver 
{
    private static final double LAMBDA_MAX = 1.1;
    public static final int MIN_NUM_HISTORY_BINS = 10;
    public static final int DEFAULT_NUM_HISTORY_BINS = 400;

    private final Species mReactant;
    private final Species mIntermedSpecies;
    private final double mRate;
    private double mTimeResolution;
    private boolean mFirstTimePoint;
    private boolean mIsMultistep;

    // used only for stochastic simulations
    private final Queue mReactionTimes;
    private final LinkedList mReactionTimesDoublePool;

    // used only for deterministic simulation
    private SlidingWindowTimeSeriesQueue mReactantHistory;
    private int mNumTimePoints;

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

    public int getNumHistoryBins()
    {
        return(mNumTimePoints);
    }

    public void setNumHistoryBins(int pNumHistoryBins)
    {       
        if(pNumHistoryBins < MIN_NUM_HISTORY_BINS)
        {
            throw new IllegalArgumentException("invalid history bin size, must be at least " + MIN_NUM_HISTORY_BINS);
        }

        mNumTimePoints = pNumHistoryBins;
        mTimeResolution = LAMBDA_MAX * mDelay / ((double) pNumHistoryBins);

        if(mIsStochasticSimulator)
        {
            mReactionTimesDoublePool.clear();
            for(int ctr = 0; ctr < pNumHistoryBins; ++ctr)
            {
                mReactionTimesDoublePool.add(new MutableDouble(0.0));
            }
        }
        else
        {
            mReactantHistory.initialize(mNumTimePoints);
        }
    }

    public DelayedReactionSolver(Species pReactant,
                                 Species pIntermedSpecies, 
                                 double pDelay,
                                 double pRate,
                                 boolean pIsMultistep,
                                 int pReactionIndex,
                                 boolean pIsStochasticSimulator)
    {
        assert (pDelay > 0.0) : "invalid delay";
        mDelay = pDelay;

        mReactant = pReactant;
        mIntermedSpecies = pIntermedSpecies;

        mRate = pRate;
        
        mFirstTimePoint = true;

        mIsMultistep = pIsMultistep;
        mReactionIndex = pReactionIndex;

        mIsStochasticSimulator = pIsStochasticSimulator;

        if(mIsStochasticSimulator)
        {
            if(mIsMultistep)
            {
                mReactionTimes = new PriorityQueue( new AbstractComparator()
                {
                    public final int compare(Object p1, Object p2)
                    {
                        return(MutableDouble.compare((MutableDouble) p1, (MutableDouble) p2));
                    }
                });
            }
            else
            {
                mReactionTimes = new ListQueue();
            }
            mReactionTimesDoublePool = new LinkedList();
            mReactantHistory = null;
        }
        else
        {
            mReactantHistory = new SlidingWindowTimeSeriesQueue(1);
            mReactionTimes = null;
            mReactionTimesDoublePool = null;
        }

        setNumHistoryBins(DEFAULT_NUM_HISTORY_BINS);
    }

    int getReactionIndex()
    {
        return(mReactionIndex);
    }

    void addReactant(SymbolEvaluatorChem pSymbolEvaluator)
    {
        MutableDouble newReactionTime = null;
        double relTime;
        if(mIsMultistep)
        {
            relTime = Gamma.staticNextDouble(mRate*mDelay, mRate);
        }
        else
        {
            relTime = mDelay;
        }
        double reactionTime = pSymbolEvaluator.getTime() + relTime;
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
        double rate = pSymbolEvaluator.getValue(mIntermedSpecies.getSymbol()) / mDelay;
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
            mReactantHistory.clear();
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
            }
        }
        else
        {
            double reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
            assert (reactantValue >= 0.0) : "invalid value";

            double intermedSpeciesValue = pSymbolEvaluator.getValue(mIntermedSpecies.getSymbol());
            assert (intermedSpeciesValue >= 0.0) : "invalid value";

            reactantHistory.insertPoint(pTime, reactantValue);

            mFirstTimePoint = false;
        }
    }

    public double computeRate(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        if(! mIsStochasticSimulator)
        {
            if(mIsMultistep)
            {
                return(computeRateMultistep(pSymbolEvaluator));
            }
            else
            {
                return(computeRateDelay(pSymbolEvaluator));
            }
        }
        else
        {
            return(0.0);
        }
    }

    private double computeRateMultistep(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
       double prodRate = 0.0;

       SymbolEvaluatorChem symbolEvaluator = (SymbolEvaluatorChem) pSymbolEvaluator;

       double currentTime = symbolEvaluator.getTime();

       SlidingWindowTimeSeriesQueue reactantSpeciesHistory = mReactantHistory;

       double reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
            
       double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());
       double minTime = reactantSpeciesHistory.getMinTime();
       double peakTimeRel = mDelay;
       double peakTime = currentTime - peakTimeRel;

       if(intermedSpeciesValue > 0.0)
       {
           prodRate = computeIntegral(reactantSpeciesHistory,
                                      mTimeResolution,
                                      mNumTimePoints,
                                      mDelay,
                                      mRate,
                                      currentTime);
       }
       else
       {
           // do nothing; rate of production is zero
       }

       return(prodRate);
    }

    // used for deterministic simulator
    private double computeRateDelay(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        double prodRate = 0.0;

        SymbolEvaluatorChem symbolEvaluator = (SymbolEvaluatorChem) pSymbolEvaluator;

        double currentTime = symbolEvaluator.getTime();

        SlidingWindowTimeSeriesQueue reactantSpeciesHistory = mReactantHistory;

        double reactantValue = pSymbolEvaluator.getValue(mReactant.getSymbol());
            
        double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());
        double minTime = reactantSpeciesHistory.getMinTime();
        double peakTimeRel = mDelay;
        double peakTime = currentTime - peakTimeRel;

        if(intermedSpeciesValue > 0.0 && peakTime >= minTime)
        {
            double peakValue = 0.0;

            double peakIndexDouble = (peakTime - minTime)/mTimeResolution;
            double peakIndexDoubleFloor = Math.floor(peakIndexDouble);
            int peakIndex = (int) peakIndexDouble;
            if( peakIndexDouble > peakIndexDoubleFloor )
            {
                double valueLeft = reactantSpeciesHistory.getValue(peakIndex);
                peakValue = valueLeft + ((peakIndexDouble - peakIndexDoubleFloor) * 
                                         (reactantSpeciesHistory.getValue(peakIndex + 1) - 
                                          valueLeft));
            }
            else
            {
                peakValue = reactantSpeciesHistory.getValue(peakIndex);
            }

            prodRate = mRate * peakValue;
        }
        else
        {
            // do nothing; rate of production is zero
        }

        return(prodRate);
    }

    // keeping this around for historical purposes
    private static double computeIntegral(SlidingWindowTimeSeriesQueue history,
                                          double timeResolution,
                                          int numTimePoints,
                                          double delay,
                                          double rate,
                                          double currentTime)
    {
        double value = 0.0;
        double prodRate = 0.0;
        double numStepsCorrected = delay * rate;
        int numPoints = history.getNumStoredPoints();

        double sqrtTwoPiNumStepsCorrected = Math.sqrt(2.0 * Math.PI * numStepsCorrected);

        for(int ctr = numPoints; --ctr >= 0; )
        {
            value = timeResolution * computeIntegrandValue(history,
                                                           ctr,
                                                           rate,
                                                           rate * rate,
                                                           sqrtTwoPiNumStepsCorrected,
                                                           numStepsCorrected,
                                                           currentTime);

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
        return(prodRate);
    }

    private static double computeIntegrandValue(SlidingWindowTimeSeriesQueue pReactantHistory,
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
