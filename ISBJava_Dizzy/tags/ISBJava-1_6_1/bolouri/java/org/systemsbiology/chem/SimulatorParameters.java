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
    private Long mEnsembleSize;
    private Long mMinNumSteps;
    private Double mMaxAllowedRelativeError;
    private Double mMaxAllowedAbsoluteError;
    private Boolean mFlagGetFinalSymbolFluctuations;

    public SimulatorParameters()
    {
        mEnsembleSize = null;
        mMinNumSteps = null;
        mMaxAllowedRelativeError = null;
        mFlagGetFinalSymbolFluctuations = null;
    }

    public void setFlagGetFinalSymbolFluctuations(boolean pFlagGetFinalSymbolFluctuations)
    {
        mFlagGetFinalSymbolFluctuations = new Boolean(pFlagGetFinalSymbolFluctuations);
    }

    public Boolean getFlagGetFinalSymbolFluctuations()
    {
        return(mFlagGetFinalSymbolFluctuations);
    }

    public void setEnsembleSize(long pEnsembleSize)
    {
        mEnsembleSize = new Long(pEnsembleSize);
    }

    public void setMinNumSteps(long pMinNumSteps)
    {
        mMinNumSteps = new Long(pMinNumSteps);
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

    public Long getMinNumSteps()
    {
        return(mMinNumSteps);
    }

    public Long getEnsembleSize()
    {
        return(mEnsembleSize);
    }
}
