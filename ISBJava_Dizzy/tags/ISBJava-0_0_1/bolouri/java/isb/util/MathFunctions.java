package isb.util;

/**
 * This class is a collection of useful mathematical functions.
 *
 * @author Stephen Ramsey [souce code for gammaln taken from
 * the Cephes Math Library, (c) Stephen Moshier]
 */
public class MathFunctions
{
    private static final double SQTPI  =  2.50662827463100050242E0;
    private static final double LOGPI  =  1.14472988584940017414;

    /**
     * Returns the natural logarithm of the gamma function
     * of the argument.
     *
     * @return the natural logarithm of the gamma function 
     * of the argument
     */
    static public double gammaln(double x)
        throws ArithmeticException {
        double p, q, w, z;

        double A[] = {
            8.11614167470508450300E-4,
            -5.95061904284301438324E-4,
            7.93650340457716943945E-4,
            -2.77777777730099687205E-3,
            8.33333333333331927722E-2
        };
        double B[] = {
            -1.37825152569120859100E3,
            -3.88016315134637840924E4,
            -3.31612992738871184744E5,
            -1.16237097492762307383E6,
            -1.72173700820839662146E6,
            -8.53555664245765465627E5
        };
        double C[] = {
            1.00000000000000000000E0, 
            -3.51815701436523470549E2,
            -1.70642106651881159223E4,
            -2.20528590553854454839E5,
            -1.13933444367982507207E6,
            -2.53252307177582951285E6,
            -2.01889141433532773231E6
        };

        if( x < -34.0 ) {
            q = -x;
            w = gammaln(q);
            p = Math.floor(q);
            if( p == q ) throw new ArithmeticException("lgam: Overflow");
            z = q - p;
            if( z > 0.5 ) {
                p += 1.0;
                z = p - q;
            }
            z = q * Math.sin( Math.PI * z );
            if( z == 0.0 ) throw new 
                               ArithmeticException("lgamma: Overflow");
            z = LOGPI - Math.log( z ) - w;
            return z;
        }

        if( x < 13.0 ) {
            z = 1.0;
            while( x >= 3.0 ) {
                x -= 1.0;
                z *= x;
            }
            while( x < 2.0 ) {
                if( x == 0.0 ) throw new 
                                   ArithmeticException("lgamma: Overflow");
                z /= x;
                x += 1.0;
            }
            if( z < 0.0 ) z = -z;
            if( x == 2.0 ) return Math.log(z);
            x -= 2.0;
            p = x * polevl( x, B, 5 ) / p1evl( x, C, 6);
            return( Math.log(z) + p );
        }

        if( x > 2.556348e305 ) throw new 
                                   ArithmeticException("lgamma: Overflow");

        q = ( x - 0.5 ) * Math.log(x) - x + 0.91893853320467274178;
        if( x > 1.0e8 ) return( q );

        p = 1.0/(x*x);
        if( x >= 1000.0 )
            q += ((   7.9365079365079365079365e-4 * p
                      - 2.7777777777777777777778e-3) *p
                  + 0.0833333333333333333333) / x;
        else
            q += polevl( p, A, 4 ) / x;
        return q;
    }




    /** Returns the gamma function of the argument.
     *
     * @return the gamma function of the argument.
     */
    static public double gamma(double x) throws ArithmeticException {

        double P[] = {
            1.60119522476751861407E-4,
            1.19135147006586384913E-3,
            1.04213797561761569935E-2,
            4.76367800457137231464E-2,
            2.07448227648435975150E-1,
            4.94214826801497100753E-1,
            9.99999999999999996796E-1
        };
        double Q[] = {
            -2.31581873324120129819E-5,
            5.39605580493303397842E-4,
            -4.45641913851797240494E-3,
            1.18139785222060435552E-2,
            3.58236398605498653373E-2,
            -2.34591795718243348568E-1,
            7.14304917030273074085E-2,
            1.00000000000000000320E0
        };
        double MAXGAM = 171.624376956302725;
        double LOGPI  = 1.14472988584940017414;

        double p, z;
        int i;

        double q = Math.abs(x);

        if( q > 33.0 ) {
            if( x < 0.0 ) {
                p = Math.floor(q);
                if( p == q ) throw new ArithmeticException("gamma: overflow");
                i = (int)p;
                z = q - p;
                if( z > 0.5 ) {
                    p += 1.0;
                    z = q - p;
                }
                z = q * Math.sin( Math.PI * z );
                if( z == 0.0 ) throw new ArithmeticException("gamma: overflow");
                z = Math.abs(z);
                z = Math.PI/(z * stirf(q) );

                return -z;
            } else {
                return stirf(x);
            }
        }

        z = 1.0;
        while( x >= 3.0 ) {
            x -= 1.0;
            z *= x;
        }

        while( x < 0.0 ) {
            if( x == 0.0 ) {
                throw new ArithmeticException("gamma: singular");
            } else
                if( x > -1.E-9 ) {
                    return( z/((1.0 + 0.5772156649015329 * x) * x) );
                }
            z /= x;
            x += 1.0;
        }

        while( x < 2.0 ) {
            if( x == 0.0 ) {
                throw new ArithmeticException("gamma: singular");
            } else
                if( x < 1.e-9 ) {
                    return( z/((1.0 + 0.5772156649015329 * x) * x) );
                }
            z /= x;
            x += 1.0;
        }

        if( (x == 2.0) || (x == 3.0) ) 	return z;

        x -= 2.0;
        p = polevl( x, P, 6 );
        q = polevl( x, Q, 7 );
        return  z * p / q;

    }
    static  private double polevl( double x, double coef[], int N )
        throws ArithmeticException {

        double ans;

        ans = coef[0];

        for(int i=1; i<=N; i++) { ans = ans*x+coef[i]; }

        return ans;
    }

/* Gamma function computed by Stirling's formula.
 * The polynomial STIR is valid for 33 <= x <= 172.

 Cephes Math Library Release 2.2:  July, 1992
 Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier
 Direct inquiries to 30 Frost Street, Cambridge, MA 02140
*/
    static private double stirf(double x) throws ArithmeticException {
        double STIR[] = {
            7.87311395793093628397E-4,
            -2.29549961613378126380E-4,
            -2.68132617805781232825E-3,
            3.47222221605458667310E-3,
            8.33333333333482257126E-2,
        };
        double MAXSTIR = 143.01608;

        double w = 1.0/x;
        double  y = Math.exp(x);

        w = 1.0 + w * polevl( w, STIR, 4 );

        if( x > MAXSTIR ) {
            /* Avoid overflow in Math.pow() */
            double v = Math.pow( x, 0.5 * x - 0.25 );
            y = v * (v / y);
        } else {
            y = Math.pow( x, x - 0.5 ) / y;
        }
        y = SQTPI * y * w;
        return y;
    }

    static  private double p1evl( double x, double coef[], int N )
        throws ArithmeticException {

        double ans;

        ans = x + coef[0];

        for(int i=1; i<N; i++) { ans = ans*x+coef[i]; }

        return ans;
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
     * argument is nonnegative, and 0 if the argument is 0.
     *
     * @return 0 if the argument is negative, and 1 if the
     * argument is nonnegative, and 0 if the argument is 0.
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

    public static void main(String []pArgs)
    {
        System.out.println("gamma(2)): " + gamma(2.0));
    }
}
