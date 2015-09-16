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
public class Maxwell implements IContinuousDistribution
{
    private double mA;
    public Maxwell(double pMean)
    {
        if(pMean <= 0.0)
        {
            throw new IllegalArgumentException("invalid mean");
        }
        mA = 8.0 / (Math.PI * pMean * pMean);
    }
    public double cdf(double x)
    {
        if(x < 0.0)
        {
            throw new IllegalArgumentException("x value out of range");
        }            
        return cern.jet.stat.Gamma.incompleteGamma(1.5, 0.5*mA*x*x);
    }
    public double pdf(double x)
    {
        return Math.sqrt(2.0/Math.PI) * Math.pow(mA, 1.5) * x * x * Math.exp(-1.0 * mA * x * x / 2.0);
    }
    public double domainMin()
    {
        return 0.0;
    }
    public double domainMax()
    {
        return Double.POSITIVE_INFINITY;
    }
    public double mean()
    {
        return Math.sqrt(8.0 / (Math.PI * mA));
    }
    public double variance()
    {
        return (3.0*Math.PI - 8.0)/(Math.PI*mA);
    }
    public String name()
    {
        return "Maxwell";
    }
}
