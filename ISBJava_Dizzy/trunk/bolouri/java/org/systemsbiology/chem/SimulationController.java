package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

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


    public final synchronized void setCancelled(boolean pCancelled)
    {
        mCancelled = pCancelled;
        if(pCancelled == true)
        {
            setStopped(true);
            notify();
        }
    }

    public final synchronized boolean getCancelled()
    {
        return(mCancelled);
    }

    public final synchronized void setStopped(boolean pStopped)
    {
        mStopped = pStopped;
        if(! mStopped)
        {
            notify();
        }
    }

    public final synchronized boolean getStopped()
    {
        return(mStopped);
    }

    public final synchronized boolean handlePauseOrCancel()
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
