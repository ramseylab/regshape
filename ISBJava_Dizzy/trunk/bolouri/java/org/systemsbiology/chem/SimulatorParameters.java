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
    private Double mMaxAllowedRelativeError;
    private Double mMaxAllowedAbsoluteError;

    public SimulatorParameters()
    {
        mEnsembleSize = null;
        mMinNumSteps = null;
        mMaxAllowedRelativeError = null;
    }

    public void setEnsembleSize(int pEnsembleSize)
    {
        mEnsembleSize = new Integer(pEnsembleSize);
    }

    public void setMinNumSteps(int pMinNumSteps)
    {
        mMinNumSteps = new Integer(pMinNumSteps);
    }


    public void setMaxAllowedRelativeError(double pMaxAllowedRelativeError)
    {
        mMaxAllowedRelativeError = new Double(pMaxAllowedRelativeError);
    }

    public Double getMaxAllowedRelativeError()
    {
        return(mMaxAllowedRelativeError);
    }

    public void setMaxAllowedAbsoluteError(double pMaxAllowedAbsoluteError)
    {
        mMaxAllowedAbsoluteError = new Double(pMaxAllowedAbsoluteError);
    }

    public Double getMaxAllowedAbsoluteError()
    {
        return(mMaxAllowedAbsoluteError);
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
