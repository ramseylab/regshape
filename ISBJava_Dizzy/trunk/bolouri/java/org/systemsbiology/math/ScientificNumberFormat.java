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

/**
 * Truncates and formats a floating-point number based on a relative and
 * an absolute tolerance
 */

public class ScientificNumberFormat extends DecimalFormat
{
    private SignificantDigitsCalculator mSignificantDigitsCalculator;
    private int mMinimumDigitsForScientificNotation;
    private NumberFormat mDefaultNumberFormat;
        
    private static final int DEFAULT_MINIMUM_DIGITS_FOR_SCIENTIFIC_NOTATION = 4;

    public ScientificNumberFormat(SignificantDigitsCalculator pSignificantDigitsCalculator)
    {
        super();
        mSignificantDigitsCalculator = pSignificantDigitsCalculator;
        setMinimumDigitsForScientificNotation(DEFAULT_MINIMUM_DIGITS_FOR_SCIENTIFIC_NOTATION);
        mDefaultNumberFormat = getInstance();
        setNaNString(null);
        setInfinityString(null);
    }

    public void setNaNString(String pNaNString)
    {
        DecimalFormatSymbols decimalFormatSymbols = getDecimalFormatSymbols();
        decimalFormatSymbols.setNaN(pNaNString);
        setDecimalFormatSymbols(decimalFormatSymbols);
    }
    
    public String getNaNString()
    {
        DecimalFormatSymbols decimalFormatSymbols = getDecimalFormatSymbols();
        return(decimalFormatSymbols.getNaN());
    }
    
    public void setInfinityString(String pInfinityString)
    {
        DecimalFormatSymbols decimalFormatSymbols = getDecimalFormatSymbols();
        decimalFormatSymbols.setInfinity(pInfinityString);
        setDecimalFormatSymbols(decimalFormatSymbols);
    }
    
    public String getInfinityString()
    {
        DecimalFormatSymbols decimalFormatSymbols = getDecimalFormatSymbols();
        return(decimalFormatSymbols.getInfinity());
    }
    
    public int getMinimumDigitsForScientificNotation()
    {
        return(mMinimumDigitsForScientificNotation);
    }

    public void setMinimumDigitsForScientificNotation(int pMinimumDigitsForScientificNotation)
    {
        if(pMinimumDigitsForScientificNotation <= 0)
        {
            throw new IllegalArgumentException("invalid minimum number of digits for scientific notation: " + pMinimumDigitsForScientificNotation);
        }
        mMinimumDigitsForScientificNotation = pMinimumDigitsForScientificNotation;
    }

    

    public StringBuffer format(double pValue, StringBuffer pResults, FieldPosition pFieldPosition)
    {
        if(Double.isNaN(pValue) || Double.isInfinite(pValue))
        {
            return super.format(pValue, pResults, pFieldPosition);
        }
        
        int numSignificantDigits = mSignificantDigitsCalculator.calculate(pValue);
     
        if(numSignificantDigits == SignificantDigitsCalculator.SIGNIFICANT_DIGITS_UNKNOWN)
        {
            return(super.format(pValue, pResults, pFieldPosition));
        }

        // save the state of this object
        String pattern = toPattern();
        int minFractionDigits = getMinimumFractionDigits();
        int maxFractionDigits = getMaximumFractionDigits();

        StringBuffer retBuf = null;

        double log10val = 0.0;
        if(pValue != 0.0)
        {
            log10val = MathFunctions.log10(pValue);
        }
        
        if(Math.abs(log10val) < mMinimumDigitsForScientificNotation)
        {
            applyPattern("0.0");
            int numDigitsToLeftOfDecimalPoint = 1 + (int) Math.floor(log10val);
            int numDigitsToRightOfDecimalPoint = numSignificantDigits - numDigitsToLeftOfDecimalPoint;
            assert (numDigitsToRightOfDecimalPoint >= 0) : "negative number of digits to the right of the decimal point";
            // use decimal notation
            setMaximumFractionDigits(numDigitsToRightOfDecimalPoint);
            setMinimumFractionDigits(numDigitsToRightOfDecimalPoint);
            retBuf = super.format(pValue, pResults, pFieldPosition);
        }
        else
        {
            applyPattern("0.######E0");
            setMaximumFractionDigits(numSignificantDigits - 1);
            setMinimumFractionDigits(numSignificantDigits - 1);
            // use scientific notation
            retBuf = super.format(pValue, pResults, pFieldPosition);
        }

        // restore the state of the object
        applyPattern(pattern);
        setMinimumFractionDigits(minFractionDigits);
        setMaximumFractionDigits(maxFractionDigits);

        return(retBuf);
    }

   
    public static final void main(String []pArgs)
    {
        SignificantDigitsCalculator sigCalc = new SignificantDigitsCalculator(new Double(1.0e-4), 
                                                                              new Double(1.0e-2));
        ScientificNumberFormat sciForm = new ScientificNumberFormat(sigCalc);
        InputStream in = System.in;
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufReader = new BufferedReader(reader);
        String line = null;
        try
        {
            while(null != (line = bufReader.readLine()))
            {
                Double value = null;
                try
                {
                    value = new Double(line);
                }
                catch(NumberFormatException e)
                {
                    System.err.println("not a double: " + line);
                    System.exit(1);
                }
                System.out.println("truncated to: " + sciForm.format(value.doubleValue()));
            }
        }
        catch(IOException e)
        {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
 
