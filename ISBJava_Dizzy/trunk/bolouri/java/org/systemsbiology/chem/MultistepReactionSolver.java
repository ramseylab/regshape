
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
    private static final double LAMBDA_MAX = 2.0;

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

    private double mRateSquared;
    private double mNumStepsCorrected;
    private double mSqrtTwoPiNumStepsCorrected;

    public MultistepReactionSolver(Species pReactant,
                                   Species pIntermedSpecies, 
                                   int pNumTimePoints, 
                                   int pNumSteps, 
                                   double pRate)
    {
        assert (pNumSteps > 2) : "invalid number of steps: " + pNumSteps;
        assert (pRate > 0.0) : "invalid rate: " + pRate;
        assert (pNumTimePoints > 0) : "invalid number of time points";

        mNumSteps = pNumSteps;
        System.out.println("number of steps: " + mNumSteps);

        mReactant = pReactant;
        mIntermedSpecies = pIntermedSpecies;

        mReactantHistory = new SlidingWindowTimeSeriesQueue(pNumTimePoints);
        mIntermedSpeciesHistory = new SlidingWindowTimeSeriesQueue(pNumTimePoints);
        mRate = pRate;
        System.out.println("base rate: " + mRate);
        
        mNumTimePoints = pNumTimePoints;
        mFirstTimePoint = true;

        double numStepsCorrected = ((double) mNumSteps - 1);
        mNumStepsCorrected = numStepsCorrected;

        mPeakTimeRel = numStepsCorrected/mRate;
        System.out.println("peak time relative: " + mPeakTimeRel);

        mTimeResolution = LAMBDA_MAX * mPeakTimeRel / ((double) pNumTimePoints);
        System.out.println("time resolution for history: " + mTimeResolution);

        mRateSquared = mRate * mRate;
        mSqrtTwoPiNumStepsCorrected = Math.sqrt( 2.0 * Math.PI * numStepsCorrected );
    }

    double getCompositeTimeScale()
    {
        return(mPeakTimeRel);
    }

    boolean canHaveMoreReactions(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        return( pSymbolEvaluator.getValue(mIntermedSpecies.getSymbol()) > 0.0 );
    }

    void clear()
    {
        mReactantHistory.clear();
        mIntermedSpeciesHistory.clear();
        mFirstTimePoint = true;
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

    public double computeValue2(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        SymbolEvaluatorChemSimulation symbolEvaluator = (SymbolEvaluatorChemSimulation) pSymbolEvaluator;

        double currentTime = symbolEvaluator.getTime();
        update(symbolEvaluator, currentTime);

        double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());

        double prodRate = 0.0;

        double numStepsCorrected = mNumStepsCorrected;

        if(intermedSpeciesValue > 0.0)
        {
            SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
            double rate = mRate;
            double minTime = reactantHistory.getMinTime();
            double peakTimeRel = mPeakTimeRel;
            double peakTime = currentTime - peakTimeRel;
            if(peakTime >= minTime)
            {
                int peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                double peakValue = reactantHistory.getValue(peakIndex);
//              System.out.println("at time: " + currentTime + "; for past time: " + peakTime + "; got value: " + peakValue + "; minTime: " + minTime);
                prodRate = computeIntegral(reactantHistory,
                                           mTimeResolution,
                                           mNumTimePoints,
                                           mSqrtTwoPiNumStepsCorrected,
                                           numStepsCorrected,
                                           rate,
                                           rate*rate,
                                           currentTime);

                if(prodRate == 0.0)
                {
                    peakTimeRel -= 1.0/rate;
                    peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                    assert (peakTimeRel > 0.0) : "invalid peak time";
                    peakValue = mIntermedSpeciesHistory.getValue(peakIndex);
                    if(intermedSpeciesValue > peakValue)
                    {
                        prodRate = rate * peakValue / (numStepsCorrected);
                    }
                }
            }
            else
            {
                // do nothing; rate of production is zero, due to causality
            }
        }
        else
        {
            // do nothing; rate of production is zero, by mass conservation law
        }

        return(prodRate);
    }


    public double computeValue(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        SymbolEvaluatorChemSimulation symbolEvaluator = (SymbolEvaluatorChemSimulation) pSymbolEvaluator;

        double currentTime = symbolEvaluator.getTime();
        update(symbolEvaluator, currentTime);

        double intermedSpeciesValue = symbolEvaluator.getValue(mIntermedSpecies.getSymbol());

        double prodRate = 0.0;

        double numStepsCorrected = mNumStepsCorrected;

        if(intermedSpeciesValue > 0.0)
        {
            SlidingWindowTimeSeriesQueue reactantHistory = mReactantHistory;
            double rate = mRate;
            double minTime = reactantHistory.getMinTime();
            double peakTimeRel = mPeakTimeRel;
            double peakTime = currentTime - peakTimeRel;
            if(peakTime >= minTime)
            {
                int peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                double peakValue = reactantHistory.getValue(peakIndex);
//              System.out.println("at time: " + currentTime + "; for past time: " + peakTime + "; got value: " + peakValue + "; minTime: " + minTime);
                prodRate = rate * peakValue;

                if(prodRate == 0.0)
                {
                    peakTimeRel -= 1.0/rate;
                    peakIndex = (int) Math.floor( (peakTime - minTime)/mTimeResolution );
                    assert (peakTimeRel > 0.0) : "invalid peak time";
                    peakValue = mIntermedSpeciesHistory.getValue(peakIndex);
                    if(intermedSpeciesValue > peakValue)
                    {
                        prodRate = rate * peakValue / (numStepsCorrected);
                    }
                }
            }
            else
            {
                // do nothing; rate of production is zero, due to causality
            }
        }
        else
        {
            // do nothing; rate of production is zero, by mass conservation law
        }

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
        if(retVal >= Double.MAX_VALUE)
        {
             System.out.println("timePointIndex: " + pTimePointIndex + "; lambda: " + lambda + "; time: " + timePoint + "; A pop: " + reactantValue);
        }
        return( retVal );
    }
}
