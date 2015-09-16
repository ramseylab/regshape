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
public class Rayleigh implements IContinuousDistribution
{
    private double mS;
    public Rayleigh(double pMean)
    {
        if(pMean <= 0.0)
        {
            throw new IllegalArgumentException("invalid mean");
        }
        mS = pMean * Math.sqrt(2.0/Math.PI);
    }
    public double pdf(double x)
    {
        if(x < 0.0)
        {
            throw new IllegalArgumentException("x value out of range: " + x);
        }
        return  x * Math.exp(-1.0 * x * x / (2.0 * mS* mS))/(mS * mS);
    }
    public double cdf(double x)
    {
        if(x < 0.0)
        {
            throw new IllegalArgumentException("x value out of range");
        }
        return 1.0 - Math.exp( -1.0 * x*x/(2.0 * mS*mS));
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
        return mS * Math.sqrt(Math.PI/2.0);
    }
    public double variance()
    {
        return (4.0 - Math.PI)*mS*mS/2.0;
    }
    public String name()
    {
        return "Rayleigh";
    }
}
