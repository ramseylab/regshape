package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

public class SimulatorParameters
{
    private Integer mEnsembleSize;
    private Integer mMinNumSteps;
    private Double mMaxAllowedError;

    public SimulatorParameters()
    {
        mEnsembleSize = null;
        mMinNumSteps = null;
        mMaxAllowedError = null;
    }

    public void setEnsembleSize(int pEnsembleSize)
    {
        mEnsembleSize = new Integer(pEnsembleSize);
    }

    public void setMinNumSteps(int pMinNumSteps)
    {
        mMinNumSteps = new Integer(pMinNumSteps);
    }

    public void setMaxAllowedError(double pMaxAllowedError)
    {
        mMaxAllowedError = new Double(pMaxAllowedError);
    }

    public Double getMaxAllowedError()
    {
        return(mMaxAllowedError);
    }

    public Integer getMinNumSteps()
    {
        return(mMinNumSteps);
    }

    public Integer getEnsembleSize()
    {
        return(mEnsembleSize);
    }
}
