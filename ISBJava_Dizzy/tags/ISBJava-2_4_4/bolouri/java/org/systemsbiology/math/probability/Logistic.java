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

/**
 * @author sramsey
 *
 */
public class Logistic implements IContinuousDistribution
{
    private double mB;
    private double mM;
    
    public Logistic(double pMean, double pVariance)
    {
        mM = pMean;
        if(pVariance <= 0.0)
        {
            throw new IllegalArgumentException("invalid variance");
        }
        mB = Math.sqrt(3.0 * pVariance)/Math.PI;
    }
    
    public double pdf(double x)
    {
        double tau = Math.exp((mM - x)/mB);
        return tau / ((1.0 + tau)*(1.0 + tau)*mB);
    }
    
    public double cdf(double x)
    {
        return 1.0/(1.0 + Math.exp(-1.0 * (x - mM)/mB));
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
        return mM;
    }
    public double variance()
    {
        return mB*mB*Math.PI*Math.PI/3.0;
    }
    public String name()
    {
        return "Logistic";
    }
}
