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
    private Double mMaxAllowedRelativeError;
    private Double mMaxAllowedAbsoluteError;
    private Double mStepSizeFraction;
    private Integer mNumHistoryBins;
    private Boolean mComputeFluctuations;

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("ensembleSize: " + mEnsembleSize + "\n");
        sb.append("maxAllowedRelativeError: " + mMaxAllowedRelativeError + "\n");
        sb.append("maxAllowedAbsoluteError: " + mMaxAllowedAbsoluteError + "\n");
        sb.append("stepSizeFraction: " + mStepSizeFraction + "\n");
        sb.append("numHistoryBins: " + mNumHistoryBins + "\n");
        sb.append("computeFluctuations: " + mComputeFluctuations + "\n");

        return(sb.toString());
    }

    public SimulatorParameters()
    {
        mEnsembleSize = null;
        mMaxAllowedRelativeError = null;
        mComputeFluctuations = null;
        mNumHistoryBins = null;
        mStepSizeFraction = null;
    }

    public void setStepSizeFraction(double pStepSizeFraction)
    {
        mStepSizeFraction = new Double(pStepSizeFraction);
    }

    public Double getStepSizeFraction()
    {
        return(mStepSizeFraction);
    }

    public void setNumHistoryBins(int pNumHistoryBins)
    {
        mNumHistoryBins = new Integer(pNumHistoryBins);
    }

    public Integer getNumHistoryBins()
    {
        return(mNumHistoryBins);
    }

    public void setComputeFluctuations(boolean pComputeFluctuations)
    {
        mComputeFluctuations = new Boolean(pComputeFluctuations);
    }

    public Boolean getComputeFluctuations()
    {
        return(mComputeFluctuations);
    }

    public void setEnsembleSize(int pEnsembleSize)
    {
        mEnsembleSize = new Integer(pEnsembleSize);
    }

    public Integer getEnsembleSize()
    {
        return(mEnsembleSize);
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
}
