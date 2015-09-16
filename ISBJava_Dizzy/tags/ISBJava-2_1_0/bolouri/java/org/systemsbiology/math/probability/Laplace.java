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

import org.systemsbiology.math.MathFunctions;

/**
 * @author sramsey
 *
 */
public class Laplace implements IContinuousDistribution
{
    private double mMean;
    private double mB;
    
    public Laplace(double pMean, double pVariance)
    {
        mMean = pMean;
        if(pVariance <= 0.0)
        {
            throw new IllegalArgumentException("invalid variance");
        }
        mB = Math.sqrt(pVariance/2.0);
    }
    
    public double pdf(double x)
    {
        return Math.exp(-1.0 * Math.abs(x - mMean)/mB)/(2.0*mB);
    }
    
    public double cdf(double x)
    {
        return 0.5 * ( 1.0 + MathFunctions.sign(x - mMean)*(1.0 - Math.exp(-1.0 * Math.abs(x - mMean)/mB)));
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
        return 2.0*mB*mB;
    }
    public String name()
    {
        return "Laplace";
    }
}
