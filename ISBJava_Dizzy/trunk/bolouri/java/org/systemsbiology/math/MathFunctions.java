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

    public static void vectorZeroNegativeElements(double []vec)
    {
        int size = vec.length;

        for(int ctr = size; --ctr >= 0; )
        {
            if(vec[ctr] < 0.0)
            {
                vec[ctr] = 0.0;
            }
        }
    }

    public static void vectorZeroElements(double []vec)
    {
        int size = vec.length;

        for(int ctr = size; --ctr >= 0; )
        {
            vec[ctr] = 0.0;
        }
    }

    public static double vectorMaxElements(double []vec)
    {
        assert (vec.length > 0) : "computing the maximum of a vector requires non-zero length";
        double retVal = -1.0 * Double.MAX_VALUE;

        int size = vec.length;

        for(int ctr = size; --ctr >= 0; )
        {
            if(vec[ctr] > retVal)
            {
                retVal = vec[ctr];
            }
        }

        return(retVal);
    }

    public static double vectorSumElements(double []vec)
    {
        double retVal = 0.0;

        int size = vec.length;

        for(int ctr = size; --ctr >= 0; )
        {
            retVal += vec[ctr];
        }

        return(retVal);
    }

    public static void vectorAdd(double []addendX, double []addendY, double []sum)
    {
        int size1 = addendX.length;
        int size2 = addendY.length;
        int size3 = sum.length;

        assert (size1 == size2 && size2 == size3) : "inconsistent vector size";

        for(int ctr = size1; --ctr >= 0; )
        {
            sum[ctr] = addendX[ctr] + addendY[ctr];
        }
    }

    public static void vectorSubtract(double []addendX, double []addendY, double []sum)
    {
        int size1 = addendX.length;
        int size2 = addendY.length;
        int size3 = sum.length;

        assert (size1 == size2 && size2 == size3) : "inconsistent vector size";

        for(int ctr = size1; --ctr >= 0; )
        {
            sum[ctr] = addendX[ctr] - addendY[ctr];
        }
    }

    public static void vectorScalarMultiply(double []vector, double scalar, double []product)
    {
        int size1 = vector.length;
        int size2 = product.length;
        assert (size1 == size2) : "inconsistent vector size";

        for(int ctr = size1; --ctr >= 0; )
        {
            product[ctr] = scalar * vector[ctr];
        }
    }

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
}
