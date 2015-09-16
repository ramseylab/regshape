package org.systemsbiology.chem;

/**
 * A data structure that allows starting and stopping 
 * a simulation while it is in progress.  This class 
 * is intended to be thread-safe.
 *
 * @see ISimulator
 *
 * @author Stephen Ramsey
 */
public class SimulationController
{
    private boolean mStopped;
    private boolean mCancelled;

    public synchronized void setCancelled(boolean pCancelled)
    {
        mCancelled = pCancelled;
        if(pCancelled == true)
        {
            setStopped(true);
            notify();
        }
    }

    public synchronized boolean getCancelled()
    {
        return(mCancelled);
    }

    public synchronized void setStopped(boolean pStopped)
    {
        mStopped = pStopped;
        if(! mStopped)
        {
            notify();
        }
    }

    public synchronized boolean getStopped()
    {
        return(mStopped);
    }

    public synchronized boolean checkIfCancelled()
    {
        boolean stop = false;
        try
        {
            if(getCancelled())
            {
                stop = true; 
            }
            else
            {
                if(getStopped())
                {
                    wait();
                }
            }
        }
        catch(InterruptedException e)
        {
            // do nothing
        }
        return(stop);
    }

    public SimulationController()
    {
        setStopped(false);
        setCancelled(false);
    }
}
