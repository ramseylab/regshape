
package org.systemsbiology.chem;

import java.util.*;
import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

/**
 * Helper class for simulating a cascade of N identical chemical
 * reactions that collectively transform a species S0 into a species
 * SN.  Useful for simulating the processes of transcription and
 * translation.  This class is only used if N is large (greater than
 * 10).  For N less than or equal to 15, the cascade is simulated
 * using N actual {@link Reaction} objects in the {@link Model}.
 *
 * @author Stephen Ramsey
 */

public class MultistepReactionSolver extends Expression
{
    private static final double LAMBDA_MAX = 4.0;
    private static final int MIN_NUM_TIME_POINTS = 4000;
    private static final int REACTION_TIMES_DOUBLE_POOL_SIZE = 100;

    private Species mReactant;
    private Species mIntermedSpecies;
    private double mRate;
    private int mNumSteps;
    private boolean mFirstTimePoint;
    private SlidingWindowTimeSeriesQueue mReactantHistory;
    private SlidingWindowTimeSeriesQueue mIntermedSpeciesHistory;
    private double mTimeResolution;
    private int mNumTimePoints;
    private double mPeakTimeRel;

    private PriorityQueue mReactionTimes;
    private LinkedList mReactionTimesDoublePool;
    private boolean mIsStochasticSimulator;

    private double mRateSquared;
    private double mNumStepsCorrected;
    private double mSqrtTwoPiNumStepsCorrected;
    private int mReactionIndex;

    public MultistepReactionSolver(Species pReactant,
                                   Species pIntermedSpecies, 
                                   int pNumSteps, 
                                   double pRate,
                                   int pReactionIndex)
    {
        assert (pNumSteps > 2) : "invalid number of steps: " + pNumSteps;
        assert (pRate > 0.0) : "invalid rate: " + pRate;

        mNumSteps = pNumSteps;

        int numTimePoints = (int) (((double) mNumSteps) * LAMBDA_MAX);
        if(numTimePoints < MIN_NUM_TIME_POINTS)
        {
            numTimePoints = MIN_NUM_TIME_POINTS;
        }
        mNumTimePoints = numTimePoints;
        assert (numTimePoints > 0) : "invalid number of time points";

        mReactant = pReactant;
        mIntermedSpecies = pIntermedSpecies;

        mRate = pRate;
        
        mNumTimePoints = numTimePoints;
        mFirstTimePoint = true;

        double numStepsCorrected = ((double) mNumSteps - 1);
        mNumStepsCorrected = numStepsCorrected;

        mPeakTimeRel = numStepsCorrected/mRate;

        mTimeResolution = LAMBDA_MAX * mPeakTimeRel / ((double) numTimePoints);

        mRateSquared = mRate * mRate;
        mSqrtTwoPiNumStepsCorrected = Math.sqrt( 2.0 * Math.PI * numStepsCorrected );

        mReactionIndex = pReactionIndex;
    }

    public void initialize(boolean pIsStochasticSimulator)
    {
        mIsStochasticSimulator = pIsStochasticSimulator;

        if(pIsStochasticSimulator)
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
            assert(false) : "test";
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

    boolean canHaveReaction() 
    {
        return(null != mReactionTimes.peek());
    }

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

//            System.out.println("inserting point into history; time: " + pTime + "; value: " + value);
            mFirstTimePoint = false;
        }
    }


    public double computeValue(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        double prodRate = 0.0;

        if(! mIsStochasticSimulator)
        {

            SymbolEvaluatorChemSimulation symbolEvaluator = (SymbolEvaluatorChemSimulation) pSymbolEvaluator;

            double currentTime = symbolEvaluator.getTime();
            update(symbolEvaluator, currentTime);

//        System.out.println("intermed species value: " + intermedSpeciesValue);

            double numStepsCorrected = mNumStepsCorrected;

            SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
            SlidingWindowTimeSeriesQueue intermedSpeciesHistory = mIntermedSpeciesHistory;
            
            double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());
            assert (intermedSpeciesValue >= 0.0) : "invalid intermediate species value";

            double averageReactantValue = reactantHistory.getAverageValue();
            assert (averageReactantValue >= 0.0) : "invalid average reactant value";

            double averageIntermedValue = mIntermedSpeciesHistory.getAverageValue();
            double numIntermedSpeciesExpected = numStepsCorrected * averageReactantValue;
        
            double minTime = reactantHistory.getMinTime();
            double peakTimeRel = mPeakTimeRel;
            double peakTime = currentTime - peakTimeRel;
            
            if(intermedSpeciesValue > numIntermedSpeciesExpected && peakTime >= minTime)
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
                    peakTime += 1.0/rate;
                    peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                    peakValue = intermedSpeciesHistory.getValue(peakIndex);

                    if(peakValue > 0.0)
                    {
                        prodRate = rate * peakValue / (numStepsCorrected - 1);
                    }
                    else
                    {
                        if(averageIntermedValue > numIntermedSpeciesExpected)
                        {
                            prodRate = rate * (averageIntermedValue - numIntermedSpeciesExpected) / (numStepsCorrected - 1);
                        }
                        else
                        {
                            // do nothing
                        }
                    }
                }
            }
            else
            {
                // do nothing; rate of production is zero, by mass conservation law
            }

            assert (prodRate >= 0.0) : "invalid reaction probability density";
        }
        else
        {
            // do nothing; just use a rate of zero
        }

        return(prodRate);
    }


    public double computeValue3(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        SymbolEvaluatorChemSimulation symbolEvaluator = (SymbolEvaluatorChemSimulation) pSymbolEvaluator;

        double currentTime = symbolEvaluator.getTime();
        update(symbolEvaluator, currentTime);

//        System.out.println("intermed species value: " + intermedSpeciesValue);
        double prodRate = 0.0;

        double numStepsCorrected = mNumStepsCorrected;

        SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
        SlidingWindowTimeSeriesQueue intermedSpeciesHistory = mIntermedSpeciesHistory;
            
        double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());
        assert (intermedSpeciesValue >= 0.0) : "invalid intermediate species value";

        double averageReactantValue = reactantHistory.getAverageValue();
        assert (averageReactantValue >= 0.0) : "invalid average reactant value";

        double averageIntermedValue = mIntermedSpeciesHistory.getAverageValue();
        double numIntermedSpeciesExpected = numStepsCorrected * averageReactantValue;
        
        double minTime = reactantHistory.getMinTime();
        double peakTimeRel = mPeakTimeRel;
        double peakTime = currentTime - peakTimeRel;
            
        if(intermedSpeciesValue > numIntermedSpeciesExpected && peakTime >= minTime)
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
                    prodRate = rate * (averageIntermedValue - numIntermedSpeciesExpected) / numStepsCorrected;
                }
                else
                {
                    peakTime += 1.0/rate;
                    peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                    peakValue = intermedSpeciesHistory.getValue(peakIndex);

                    if(peakValue > 0.0)
                    {
                        prodRate = rate * peakValue / numStepsCorrected;
                    }
                    else
                    {
                        // do nothing
                    }
                }
            }
        }
        else
        {
            // do nothing; rate of production is zero, by mass conservation law
        }

        assert (prodRate >= 0.0) : "invalid reaction probability density";
        return(prodRate);
    }


    public double computeValue2(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        SymbolEvaluatorChemSimulation symbolEvaluator = (SymbolEvaluatorChemSimulation) pSymbolEvaluator;

        double currentTime = symbolEvaluator.getTime();
        update(symbolEvaluator, currentTime);

//        System.out.println("intermed species value: " + intermedSpeciesValue);
        double prodRate = 0.0;

        double numStepsCorrected = mNumStepsCorrected;

        SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
        SlidingWindowTimeSeriesQueue intermedSpeciesHistory = mIntermedSpeciesHistory;
            
        double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());
        assert (intermedSpeciesValue >= 0.0) : "invalid intermediate species value";

        double averageReactantValue = reactantHistory.getAverageValue();
        assert (averageReactantValue >= 0.0) : "invalid average reactant value";

        double averageIntermedValue = mIntermedSpeciesHistory.getAverageValue();
        double numIntermedSpeciesExpected = numStepsCorrected * averageReactantValue;
        
        double minTime = reactantHistory.getMinTime();
        double peakTimeRel = mPeakTimeRel;
        double peakTime = currentTime - peakTimeRel;
            
        if(intermedSpeciesValue > numIntermedSpeciesExpected && peakTime >= minTime)
        {
            double rate = mRate;
            int peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
            double peakValue = reactantHistory.getValue(peakIndex);
            prodRate = rate * peakValue;
            assert (prodRate >= 0.0) : "invalid reaction probability density";

            if(prodRate == 0.0)
            {
                peakTime += 1.0/rate;
                peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                peakValue = intermedSpeciesHistory.getValue(peakIndex);

                if(peakValue > 0.0)
                {
                    prodRate = rate * peakValue / numStepsCorrected;
                    assert (prodRate >= 0.0) : "invalid reaction probability density";
                }
                else
                {
                    if(averageIntermedValue > numIntermedSpeciesExpected)
                    {
                        prodRate = rate * (averageIntermedValue - numIntermedSpeciesExpected) / numStepsCorrected;
                        assert (prodRate >= 0.0) : "invalid reaction probability density";
                    }
                }
            }
        }
        else
        {
            // do nothing; rate of production is zero, by mass conservation law
        }

        assert (prodRate >= 0.0) : "invalid reaction probability density";
        return(prodRate);
    }


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
