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

import cern.colt.matrix.DoubleMatrix1D;

/**
 * @author sramsey
 *
 */
public class Empirical implements IContinuousDistribution
{
    private DoubleMatrix1D mDist;
    private double mMin;
    private double mMax;
    private double mMean;
    private double mVariance;
    
    public Empirical(DoubleMatrix1D pDist, double pMin, double pMax)
    {
        if(pMin >= pMax)
        {
            throw new IllegalArgumentException("max must exceed min");
        }
        mDist = pDist;
        int numBins = pDist.size();
        double binSize = (pMax - pMin)/((double) numBins);
        double mean = 0.0;
        double xk = 0.0;
        double yk = 0.0;
        for(int k = numBins; --k >= 0; )
        {
            yk = pDist.get(k);
            xk = pMin + (((double) k) + 0.5)*binSize;
            mean += yk * xk * binSize;
            //                System.out.println("N[" + k + "] = " + Nk + "; y[" + k + "] = " + yk);
        }
        double variance = 0.0;
        for(int k = numBins; --k >= 0; )
        {
            yk = pDist.get(k);
            xk = pMin + (((double) k) + 0.5)*binSize;
            variance += yk * (xk - mean) * (xk - mean) * binSize;
        }

        mMin = pMin;
        mMax = pMax;
        mMean = mean;
        mVariance = variance;
    }
    
    public double cdf(double x)
    {
        if(x <= mMin)
        {
            return 0.0;
        }
        if(x >= mMax)
        {
            return 1.0;
        }
        int numBins = mDist.size();
        double binSize = (mMax - mMin)/((double) numBins);
        double Nmaxdouble = (x - mMin)/binSize;
        int Nmax = (int) Nmaxdouble;
        if(Nmax == numBins)
        {
            --Nmax;
        }
        double cumSum = 0.0;
        int k = 0;
        for(k = 0; k < Nmax; ++k)
        {
            cumSum += mDist.get(k)*binSize;
        }
        double remainder = Nmaxdouble - ((double) Nmax);
        if(remainder > 0.0)
        {
            cumSum += remainder*mDist.get(k)*binSize;
        }
        return cumSum;
    }
    
    public double pdf(double x)
    {
        // get num bins
        int numBins = mDist.size();
        
        if(x < mMin || x > mMax)
        {
            throw new IllegalArgumentException("out of bounds value for x; x: " + x + "; min: " + mMin + "; max: " + mMax);
        }
        
        double binSize = (mMax - mMin) / ((double) numBins);
        
        int k =  (int) (((x - mMin)/binSize) + 0.5);
        if(k == 0)
        {
            return mDist.get(0);
        }
        
        double yleft = mDist.get(k - 1);
        double yright = 0.0;
        double xleft = mMin + (((double) k) - 0.5)*binSize;
        double xright = 0.0;
        if(k < numBins)
        {
            yright = mDist.get(k);
            xright = xleft + binSize;
        }
        else
        {
            yright = 0.0;
            xright = mMax;
        }
        double slope = (yright - yleft)/(xright - xleft);
        double retVal = yleft + slope*(x - xleft);
        if(retVal < 0.0)
        {
            // correct for slight arighmetic error
            retVal = 0.0;
        }
        return retVal;
    }
    
    public double mean()
    {
        return mMean;
    }
    
    public double domainMin()
    {
        return mMin;
    }
    
    public double domainMax()
    {
        return mMax;
    }
        
    public double variance()
    {
        return mVariance;
    }
    
    public String name()
    {
        return "Empirical";
    }
}
