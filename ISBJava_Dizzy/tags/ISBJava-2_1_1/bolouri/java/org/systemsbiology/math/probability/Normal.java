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
public class Normal implements IContinuousDistribution
{
    private double mMean;
    private double mVariance;
    
    public static double pdf(double pMean, double pVariance, double x)
    {
        return Math.exp(-1.0 * (x-pMean)*(x-pMean)/(2.0 * pVariance)) /
               Math.sqrt(2.0 * Math.PI * pVariance);
    }
    
    public Normal(double pMean, double pVariance)
    {
        mMean = pMean;
        mVariance = pVariance;
    }
    
    public double pdf(double x)
    {
        return pdf(mMean, mVariance, x);
    }
    
    public double cdf(double x)
    {
        return Probability.normal(mMean, mVariance, x);
    }
    public double domainMin()
    {
        return Double.NEGATIVE_INFINITY;
    }
    public double domainMax()
    {
        return Double.POSITIVE_INFINITY;
    }
    public double mean()
    {
        return mMean;
    }
    public double variance()
    {
        return mVariance;
    }
    public String name()
    {
        return "Normal";
    }
}
