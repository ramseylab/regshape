/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.inference;

/**
 * Data structure that holds the parameters that are
 * passed to the {@link EvidenceWeightedInferer}, to describe
 * how the Pointillist algorithm should run.  The parameters 
 * include the number of bins used to calculate the nonparametric
 * distribution, the initial cutoff for the evidence-specific
 * significances, the final cutoff for the combined significances,
 * the smoothing length for the log-ignificance distribution,
 * and the type of formula to be used to compute the evidence-specific
 * weights.
 * 
 * @author sramsey
 *
 */
public class EvidenceWeightedInfererParams
{
    private Integer mNumBins;
    private Double mInitialSignificanceCutoff;
    private Double mCombinedSignificanceCutoff;
    private Double mFractionToRemove;
    private Double mMinFractionalCostChange;
    private Double mSmoothingLength;
    private EvidenceWeightType mEvidenceWeightType;
    
    public void setNumBins(Integer pNumBins)
    {
        mNumBins = pNumBins;
    }
    
    public Integer getNumBins()
    {
        return mNumBins;
    }
    
    public void setInitialSignificanceCutoff(Double pInitialSignificanceCutoff)
    {
        mInitialSignificanceCutoff = pInitialSignificanceCutoff;
    }
    
    public Double getInitialSignificanceCutoff()
    {
        return mInitialSignificanceCutoff;
    }
    
    public void setCombinedSignificanceCutoff(Double pCombinedSignificanceCutoff)
    {
        mCombinedSignificanceCutoff = pCombinedSignificanceCutoff;
    }
    
    public Double getCombinedSignificanceCutoff()
    {
        return mCombinedSignificanceCutoff;
    }
    
    public void setFractionToRemove(Double pFractionToRemove)
    {
        mFractionToRemove = pFractionToRemove;
    }
    
    public Double getFractionToRemove()
    {
        return mFractionToRemove;
    }
    
    public void setMinFractionalCostChange(Double pMinFractionalCostChange)
    {
        mMinFractionalCostChange = pMinFractionalCostChange;
    }

    public Double getMinFractionalCostChange()
    {
        return mMinFractionalCostChange;
    }
    
    public void setSmoothingLength(Double pSmoothingLength)
    {
        mSmoothingLength = pSmoothingLength;
    }
    
    public Double getSmoothingLength()
    {
        return mSmoothingLength;
    }
    
    public void setEvidenceWeightType(EvidenceWeightType pEvidenceWeightType)
    {
        mEvidenceWeightType = pEvidenceWeightType;
    }
    
    public EvidenceWeightType getEvidenceWeightType()
    {
        return mEvidenceWeightType;
    }
    
}
