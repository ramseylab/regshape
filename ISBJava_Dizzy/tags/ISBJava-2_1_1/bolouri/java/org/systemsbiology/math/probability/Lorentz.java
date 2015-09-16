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
public class Lorentz implements IContinuousDistribution
{
    private double mWidth;
    private double mMean;    

    public static double pdf(double pMean, double pWidth, double x)
    {
        return pWidth / (2.0 * Math.PI * ((x - pMean)*(x - pMean) + pWidth*pWidth/4.0));
    }
    
    public static double cdf(double pMean, double pWidth, double x)
    {
        return 0.5 + Math.atan(2.0 * (x - pMean)/pWidth)/Math.PI;
    }

    public static final double RATIO_WIDTH_TO_STDEV = 0.5;
    
    public Lorentz(double pMean, double pStdev)
    {
        mMean = pMean;
        mWidth = RATIO_WIDTH_TO_STDEV*pStdev;
    }
    
    public double pdf(double x)
    {
        return pdf(mMean, mWidth, x);
    }
    
    public double cdf(double x)
    {
        return cdf(mMean, mWidth, x);
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
        return 0.0;
    }
    public double variance()
    {
        //return Double.POSITIVE_INFINITY;
        return mWidth*mWidth;
    }
    public String name()
    {
        return "Lorentz";
    }
}
