package org.systemsbiology.util;

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

    public SlidingWindowTimeSeriesQueue(int pNumTimePoints)
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
        mMinIndex = 0;
        mLastTime = 0.0;
        mNumStoredPoints = 0;
        MathFunctions.vectorZeroElements(mTimePoints);
        MathFunctions.vectorZeroElements(mValues);
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

    public void insertPoint(double pTime, double pValue)
    {
        mLastTime = pTime;
        int queueIndex = mQueueIndex;
        int numTimePoints = mNumTimePoints;

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
                throw new IllegalStateException("no data point has yet been stored for that index");
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
