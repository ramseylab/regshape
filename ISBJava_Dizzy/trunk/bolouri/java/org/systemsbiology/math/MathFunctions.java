package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * (Except those portions of code that are copyright 
 * Stephen L. Moshier, as specifically indicated herein)
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * This class is a collection of useful mathematical functions.
 */
public final class MathFunctions
{
    private static final double SQTPI  =  2.50662827463100050242E0;
    private static final double LOGPI  =  1.14472988584940017414;

    /**
     * Returns the factorial of an integer argument.
     *
     * @return the factorial of an integer argument.
     */
    public static long factorial(int pArg)
    {
        assert (pArg > 0);

        long retVal = 1L;
        for(int ctr = 1; ctr <= pArg; ++ctr)
        {
            retVal *= ctr;
        }

        return(retVal);
    }

    /**
     * Returns 0 if the argument is negative, and 1 if the
     * argument is nonnegative.
     *
     * @return 0 if the argument is negative, and 1 if the
     * argument is nonnegative.
     */
    public static double thetaFunction(double pArg)
    {
        double retVal = 0.0;
        if(pArg > 0.0)
        {
            retVal = 1.0;
        }
        return(retVal);
    }

    /**
     * This function computes N choose M, for small values of M.  It is required
     * that  N > 0, M >= 0, and M <= N.  
     * <p />
     * <b>WARNING:</b> This implementation will generate an overflow
     * or underflow for large values of M, but it will work for small values
     * of M.
     */
    public static double chooseFunction(long N, int M) throws IllegalArgumentException
    {
        if(N < 0)
        {
            throw new IllegalArgumentException("invalid parameter for choose function; N=" + N);
        }
        if(M < 0)
        {
            throw new IllegalArgumentException("invalid parameter for choose function; M=" + M);
        }
        if(M > N)
        {
            throw new IllegalArgumentException("invalid parameters for choose function; M=" + M + "; N=" + N);
        }
        double retVal = 1.0;
        for(long ctr = 0; ctr < M; ++ctr)
        {
            retVal *= (double) (N - ctr);
        }
        retVal /= ((double) factorial((int) M));
        return(retVal);
    }

    public static final double LN10 = Math.log(10.0);
    
    /**
     * Returns the logarithm base 10, of the argument.
     */
    // NOTE:  In Java 1.5, switch to using Math.log10()
    public static double log10(double pArg)
    {
        return(Math.log(pArg)/LN10);
    }
    
    public static void stats(double []pVec, MutableDouble pMean, MutableDouble pStdDev)
    {
        int num = pVec.length;
        if(num <= 1)
        {
            throw new IllegalArgumentException("minimum vector length for computing statistics is 2");
        }
        double mean = 0.0;
        for(int i = 0; i < num; ++i)
        {
            mean += pVec[i];
        }
        mean /= ((double) num);
        pMean.setValue(mean);
        double stdev = 0.0;
        for(int i = 0; i < num; ++i)
        {
            stdev += Math.pow( pVec[i] - mean, 2.0 );
        }
        stdev = Math.sqrt(stdev/((double) num));
        pStdDev.setValue(stdev);
    }    
    
    public static double sign(double x)
    {
        double retVal = 0.0;
        if(x > 0.0)
        {
            retVal = 1.0;
        }
        else if(x < 0.0)
        {
            retVal = -1.0;
        }
        return retVal;
    }
    
    public static double extendedSimpsonsRule(double []pVals, double pXmin, double pXmax, int pNmin, int pNmax)
    {
        double retVal = 0.0;
        
        if(pXmax <= pXmin)
        {
            throw new IllegalArgumentException("max value must exceed the min value");
        }

        if(pNmax <= pNmin)
        {
            throw new IllegalArgumentException("max bin number must exceed the min bin number");
        }
        
        if(pNmin < 0)
        {
            throw new IllegalArgumentException("min bin number must be nonnegative");
        }
        
        if(pNmax >= pVals.length || pNmin >= pVals.length)
        {
            throw new IllegalArgumentException("bin range is out of range, for the data array supplied");
        }
        
        int numBins = pNmax - pNmin + 1;
        
        if(numBins < 3)
        {
            throw new IllegalArgumentException("at least three bins required for using Extended Simpsons Rule");
        }
        
        double cumSum = 0.0;
        double fac = 0.0;
        
        double h = (pXmax - pXmin)/((double) numBins);
        
        for(int k = numBins; --k >= 0; )
        {
            if(k == numBins - 1)
            {
                fac = 1.0/3.0;
            }
            else if(k == 0)
            {
                fac = 1.0/3.0;
            }
            else if(k % 2 == 1)  // this means k+1 is even
            {
                fac = 4.0/3.0;
            }
            else  // this means k+1 is odd
            {
                fac = 2.0/3.0;  
            }
        
            cumSum += h*fac*pVals[k + pNmin];
        }
        
        return cumSum;
    }
    
 
}
