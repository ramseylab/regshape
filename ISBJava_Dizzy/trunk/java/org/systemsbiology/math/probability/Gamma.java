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
package org.systemsbiology.math.probability;

import cern.jet.stat.Probability;

/**
 * @author sramsey
 *
 */
public class Gamma implements IContinuousDistribution
{
    private static final double THRESHOLD_USE_STIRLINGS_APPROXIMATION = 6.0;
    
    private double mAlpha;
    private double mTheta;
    public Gamma(double pMean, double pVariance)
    {
        mAlpha = pMean*pMean/pVariance;
        mTheta = pMean / mAlpha;
    }
    public double mean()
    {
        return mTheta*mAlpha;
    }
    public double pdf(double x)
    {
        if(x < 0.0)
        {
            throw new IllegalArgumentException("x value out of range");
        }
        double retVal = 0.0;
        if(mAlpha < THRESHOLD_USE_STIRLINGS_APPROXIMATION)
        {
            retVal = Math.pow(x, mAlpha - 1.0) * Math.exp(-1.0 * x / mTheta) / (Math.pow(mTheta, mAlpha) *
                     cern.jet.stat.Gamma.gamma(mAlpha));
        }
        else
        {
            double xDivTheta = x / mTheta;
            double alphaMinus1 = mAlpha  - 1.0;
            retVal = Math.pow( Math.PI * ((2.0 * alphaMinus1) + (1.0/3.0)), -0.5) * 
                     Math.pow( xDivTheta / alphaMinus1, alphaMinus1 ) *
                     Math.exp( alphaMinus1 - xDivTheta ) / mTheta;
        }
        return retVal;
               
    }
    
    public double cdf(double x)
    {
        return Probability.gamma(1.0/mTheta, mAlpha, x);
    }
    
    public double domainMin()
    {
        return 0.0;
    }
    
    public double domainMax()
    {
        return Double.POSITIVE_INFINITY;
    }
    public double variance()
    {
        return mAlpha*mTheta*mTheta;
    }
    public String name()
    {
        return "Gamma";
    }
}
