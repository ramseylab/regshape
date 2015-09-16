package org.systemsbiology.math;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Calculates the number of significant digits in a floating-point
 * number, based on the absolute and relative error tolerances.
 */
public class SignificantDigitsCalculator
{
    Double mRelTol;
    Double mAbsTol;

    public static final int SIGNIFICANT_DIGITS_UNKNOWN = -1;
    private static final Double DEFAULT_RELATIVE_TOLERANCE = new Double(1.0e-6);
    
    public SignificantDigitsCalculator()
    {
        this(DEFAULT_RELATIVE_TOLERANCE, null);
    }

    public SignificantDigitsCalculator(Double pRelTol, Double pAbsTol)
    {
        setRelTol(pRelTol);
        setAbsTol(pAbsTol);
    }

    public void setRelTol(Double pRelTol)
    {
        if(null != pRelTol)
        {
            double relTolValue = pRelTol.doubleValue();
            if(relTolValue <= 0.0 || relTolValue >= 1.0)
            {
                throw new IllegalArgumentException("illegal relative tolerance: " + pRelTol.toString());
            }
        }
        mRelTol = pRelTol;
    }

    public void setAbsTol(Double pAbsTol)
    {
        if(null != pAbsTol)
        {
            if(pAbsTol.doubleValue() <= 0.0)
            {
                throw new IllegalArgumentException("illegal absolutetolerance: " + pAbsTol.toString());
            }
        }
        mAbsTol = pAbsTol;
    }

    public Double getRelTol()
    {
        return(mRelTol);
    }

    public Double getAbsTol()
    {
        return(mAbsTol);
    }

    public int calculate(double pValue)
    {
        if(null == mRelTol && null == mAbsTol)
        {
            return(SIGNIFICANT_DIGITS_UNKNOWN);
        }
        
        if(0.0 == pValue)
        {
            if(null != mAbsTol)
            {
                double errorFromAbsTol = mAbsTol.doubleValue();
                if(errorFromAbsTol < 1.0)
                {
                    return(1 + (int) Math.rint(-1.0 * MathFunctions.log10(errorFromAbsTol)));
                }
                else
                {
                    return(SIGNIFICANT_DIGITS_UNKNOWN);
                }
            }
            else
            {
                return(SIGNIFICANT_DIGITS_UNKNOWN);
            }
        }

        double absValue = Math.abs(pValue);
        double log10val = MathFunctions.log10(absValue);

        double errorFromRelTol = Double.MAX_VALUE;
        if(null != mRelTol)
        {
            errorFromRelTol = absValue * mRelTol.doubleValue();
        }

        double errorFromAbsTol = Double.MAX_VALUE;
        if(null != mAbsTol)
        {
            errorFromAbsTol = mAbsTol.doubleValue();
        }

        double actualError = Math.min(errorFromRelTol, errorFromAbsTol);

        double log10actualError = MathFunctions.log10(actualError);

        double ratio = absValue / actualError;

        int numSignificantDigits = 1 + (int)  Math.rint(MathFunctions.log10( ratio ));

        return(numSignificantDigits);
    }

    public static final void main(String []pArgs)
    {

    }
}
 
