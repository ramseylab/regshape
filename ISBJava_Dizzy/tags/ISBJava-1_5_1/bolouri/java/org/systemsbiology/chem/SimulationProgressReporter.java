package org.systemsbiology.chem;

public class SimulationProgressReporter
{
    private double mFractionComplete;
    private long mIterationCounter;
    private long mTimeOfLastUpdateMillis;
    private boolean mSimulationFinished;

    public final synchronized void setSimulationFinished(boolean pSimulationFinished)
    {
        mSimulationFinished = pSimulationFinished;
    }

    public final synchronized boolean getSimulationFinished()
    {
        return(mSimulationFinished);
    }

    public final synchronized void waitForUpdate()
    {
        try
        {
            wait();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    public final synchronized long getIterationCounter()
    {
        return(mIterationCounter);
    }

    public final synchronized void updateProgressStatistics(boolean pSimulationFinished, 
                                                            double pFractionComplete, 
                                                            long pIterationCounter)
    {
        mSimulationFinished = pSimulationFinished;
        mFractionComplete = pFractionComplete;
        mIterationCounter = pIterationCounter;
        mTimeOfLastUpdateMillis = System.currentTimeMillis();
        notifyAll();
    }

    public final synchronized long getTimeOfLastUpdateMillis()
    {
        return(mTimeOfLastUpdateMillis);
    }

    public final synchronized double getFractionComplete()
    {
        return(mFractionComplete);
    }
    
    public SimulationProgressReporter()
    {
        mFractionComplete = 0.0;
        mIterationCounter = 0;
        mTimeOfLastUpdateMillis = 0;
        mSimulationFinished = false;
    }
}
