package org.systemsbiology.chem;

public final class SimulationProgressReporter
{
    private double mFractionComplete;
    private long mIterationCounter;
    private long mTimeOfLastUpdateMillis;
    private boolean mSimulationFinished;

    public synchronized void setSimulationFinished(boolean pSimulationFinished)
    {
        mSimulationFinished = pSimulationFinished;
    }

    public synchronized boolean getSimulationFinished()
    {
        return(mSimulationFinished);
    }

    public synchronized void waitForUpdate()
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

    public synchronized long getIterationCounter()
    {
        return(mIterationCounter);
    }

    public synchronized void updateProgressStatistics(boolean pSimulationFinished, 
                                                      double pFractionComplete, 
                                                      long pIterationCounter)
    {
        mSimulationFinished = pSimulationFinished;
        mFractionComplete = pFractionComplete;
        mIterationCounter = pIterationCounter;
        mTimeOfLastUpdateMillis = System.currentTimeMillis();
        notifyAll();
    }

    public synchronized long getTimeOfLastUpdateMillis()
    {
        return(mTimeOfLastUpdateMillis);
    }

    public synchronized double getFractionComplete()
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
