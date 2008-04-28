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
public class HalfNormal implements IContinuousDistribution
{
    private double mTheta;
    public HalfNormal(double pMean)
    {
        if(pMean <= 0.0)
        {
            throw new IllegalArgumentException("mean value out of range");
        }
        mTheta = 1.0/pMean;
    }
    public double pdf(double x)
    {
        if(x < 0.0)
        {
            throw new IllegalArgumentException("x value out of range");
        }
        return 2.0 * Normal.pdf(0.0, Math.PI/(2.0*mTheta*mTheta), x);
    }
    public double cdf(double x)
    {
        if(x < 0.0)
        {
            throw new IllegalArgumentException("x value out of range");
        }
        return 2.0 * (Probability.normal(0.0, Math.PI/(2.0*mTheta*mTheta), x) - 0.5);
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
        return 1.0/mTheta;
    }
    public double variance()
    {
        return (Math.PI - 2.0)/(2.0 * mTheta * mTheta);
    }
    public String name()
    {
        return "HalfNormal";
    }
}
