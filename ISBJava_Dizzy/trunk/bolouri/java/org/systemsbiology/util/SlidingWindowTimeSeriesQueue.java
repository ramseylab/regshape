package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.*;

/**
 * Implements a queue of ordered pairs of floating-point
 * values.  The first element of the ordered pair is the time,
 * and the second element of the ordered pair is the value
 * of some variable at that time.  When the queue fills up,
 * it start overwriting itself, discarding the oldest point
 * first.  Therefore it is a FIFO (first-in, first-out) queue.
 * The temporal ordering of the timestamps is not enforced.
 * 
 * @author Stephen Ramsey
 */
public class SlidingWindowTimeSeriesQueue
{
    private int mNumTimePoints;
    private int mQueueIndex;
    private double []mTimePoints;
    private double []mValues;
    private int mNumStoredPoints;
    private int mMinIndex;
    private double mLastTime;
    private double mAverageValue;

    private double mTimeLastNonzeroValue;
    private boolean mHasNonzeroValue;
    private int mCounterForRecomputeAverage;

    public SlidingWindowTimeSeriesQueue(int pNumTimePoints)
    {
        initialize(pNumTimePoints);
    }

    public void initialize(int pNumTimePoints)
    {
        assert (pNumTimePoints > 0) : "invalid number of time points";

        mTimePoints = new double[pNumTimePoints];
        mValues = new double[pNumTimePoints];
        mNumTimePoints = pNumTimePoints;        
        clear();
    }

    public double getValue(int pIndex)
    {
        return(mValues[getInternalIndex(pIndex)]);
    }

    public void clear()
    {
        mQueueIndex = 0;
        MathFunctions.vectorZeroElements(mTimePoints);
        MathFunctions.vectorZeroElements(mValues);
        mNumStoredPoints = 0;
        mMinIndex = 0;
        mLastTime = 0.0;
        mAverageValue = 0.0;

        mTimeLastNonzeroValue = 0.0;
        mHasNonzeroValue = false;
        mCounterForRecomputeAverage = 0;
    }

    public boolean hasNonzeroValue()
    {
        return(mHasNonzeroValue);
    }

    public double getTimeLastNonzeroValue() throws IllegalStateException
    {
        if(! mHasNonzeroValue)
        {
            throw new IllegalStateException("there is no nonzero value in the history");
        }

        return(mTimeLastNonzeroValue);
    }

    public double getLastTimePoint()
    {
        return(mLastTime);
    }

    public int getNumStoredPoints()
    {
        return(mNumStoredPoints);
    }

    public double getTimePoint(int pIndex)
    {
        return(mTimePoints[getInternalIndex(pIndex)]);
    }

    public double getAverageValue()
    {
        return(mAverageValue);
    }

    private double getExactAverageValue()
    {
        int numPoints = mNumStoredPoints;
        double avg = 0.0;
        for(int ctr = 0; ctr < numPoints; ++ctr)
        {
            avg += getValue(ctr);
        }
        avg /= ((double) numPoints);
        return(avg);
    }

    public void insertPoint(double pTime, double pValue)
    {
        assert (pValue >= 0.0) : "invalid value in history";
        mLastTime = pTime;
        int queueIndex = mQueueIndex;
        int numTimePoints = mNumTimePoints;

        double newAverage = mAverageValue * mNumStoredPoints;

        double lastValue = 0.0;

        if(mNumStoredPoints < numTimePoints)
        {
            if(mNumStoredPoints == 0)
            {
                mMinIndex = mQueueIndex;
            }

            ++mNumStoredPoints;
        }
        else
        {
            // we are about to overwrite the min time point; 
            int nextIndex = queueIndex + 1;
            if(nextIndex >= numTimePoints)
            {
                nextIndex -= numTimePoints;
            }

            lastValue = mValues[queueIndex];

            mMinIndex = nextIndex;
        }

        mTimePoints[queueIndex] = pTime;
        mValues[queueIndex] = pValue;
        if(queueIndex < mNumTimePoints - 1)
        {
            mQueueIndex++;
        }
        else
        {
            mQueueIndex = 0;
        }

        if(pValue > 0.0)
        {
            // need to update the mTimeLastNonzeroValue
            mTimeLastNonzeroValue = pTime;
            mHasNonzeroValue = true;
        }
        else
        {
            if(mHasNonzeroValue)
            {
                if(mTimeLastNonzeroValue < mTimePoints[mMinIndex])
                {
                    mHasNonzeroValue = false;
                    mTimeLastNonzeroValue = 0.0;
                }
            }
        }

        ++mCounterForRecomputeAverage;
        if(mCounterForRecomputeAverage <= mNumTimePoints)
        {
            newAverage = (Math.abs(newAverage - lastValue) + pValue)/mNumStoredPoints;
            mAverageValue = newAverage;
        }
        else
        {
            mAverageValue = getExactAverageValue();
            mCounterForRecomputeAverage = 0;
        }
        assert (newAverage >= 0.0) : "invalid average value (negative); lastValue: " + lastValue + "; numStoredPoints: " + mNumStoredPoints + "; newAverage: " + newAverage + "; pValue: " + pValue;
    }

    public double getMinTime()
    {
        return(mTimePoints[mMinIndex]);
    }

    private int getInternalIndex(int pExternalIndex)
    {
        assert (pExternalIndex < mNumTimePoints) : "invalid external index";
        assert (pExternalIndex >= 0) : "invalid external index";

        int tempIndex = 0;
        if(mNumStoredPoints >= mNumTimePoints)
        {
            tempIndex = mQueueIndex + pExternalIndex;
            if(tempIndex >= mNumTimePoints)
            {
                tempIndex -= mNumTimePoints;
            }
        }
        else
        {
            if(pExternalIndex >= mNumStoredPoints)
            {
                throw new IllegalStateException("no data point has yet been stored for that index; num stored points is " + mNumStoredPoints + " and requested index is " + pExternalIndex);
            }
            tempIndex = pExternalIndex;
        }
 
        return(tempIndex);
    }

    public double []getTimePoints()
    {
        return(mTimePoints);
    }

    public double []getValues()
    {
        return(mValues);
    }
}
