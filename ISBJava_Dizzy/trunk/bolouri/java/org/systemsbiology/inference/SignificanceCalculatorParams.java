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
 * The set of parameters that are passed to the
 * {@link SignificanceCalculator}, describing how the
 * significance calculation should be performed.  The
 * parameters include the number of bins for the nonparametric
 * distribution, the smoothing length for the kernel density,
 * whether the distribution is single-tailed or two-tailed, etc.
 * 
 * @author sramsey
 *
 */
public class SignificanceCalculatorParams
{
    private Double mMaxReducedChiSquare;
    private Double mSmoothingLength;
    private Integer mNumBins;
    private Boolean mSingleTailed;
    private SignificanceCalculationFormula mFormula;
    
    public SignificanceCalculatorParams()
    {
        mMaxReducedChiSquare = null;
        mNumBins = null;
        mSmoothingLength = null;
        mSingleTailed = null;
        mFormula = null;
    }
    
    public void setSignificanceCalculationFormula(SignificanceCalculationFormula pFormula)
    {
        mFormula = pFormula;
    }
    
    public SignificanceCalculationFormula getSignificanceCalculationFormula()
    {
        return mFormula;
    }
    
    public void setSingleTailed(Boolean pSingleTailed)
    {
        mSingleTailed = pSingleTailed;
    }
    
    public Boolean getSingleTailed()
    {
        return mSingleTailed;
    }
    
    public void setNumBins(Integer pNumBins)
    {
        mNumBins = pNumBins;
    }
    
    public void setSmoothingLength(Double pSmoothingLength)
    {
        mSmoothingLength = pSmoothingLength;
    }
    
    public void setMaxReducedChiSquare(Double pMaxReducedChiSquare)
    {
        mMaxReducedChiSquare = pMaxReducedChiSquare;
    }
    
    public Double getMaxReducedChiSquare()
    {
        return mMaxReducedChiSquare;
    }
    
    public Double getSmoothingLength()
    {
        return mSmoothingLength;
    }
    
    public Integer getNumBins()
    {
        return mNumBins;
    }
}
