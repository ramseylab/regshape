package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;
import java.text.*;

/**
 * Enumeration class for output file formats that are supported by the
 * {@link TimeSeriesSymbolValuesReporter} class.
 * 
 * @author Stephen Ramsey
 */
public class TimeSeriesOutputFormat
{
    private final String mName;
    private final char mCommentChar;
    private static final HashMap mMap;
    private final String mNaN;
    private final String mInfinity;
    private final boolean mLocalizeDecimal;

    static
    {
        mMap = new HashMap();
    }
    private TimeSeriesOutputFormat(String pName, char pCommentChar, String pNaN, String pInfinity, boolean pLocalizeDecimal)
    {
        mName = pName;
        mCommentChar = pCommentChar;
        mNaN = pNaN;
        mInfinity = pInfinity;
	mLocalizeDecimal = pLocalizeDecimal;
        mMap.put(pName, this);
    }
    public String toString()
    {
        return(mName);
    }
    public static TimeSeriesOutputFormat get(String pName)
    {
        return((TimeSeriesOutputFormat) mMap.get(pName));
    }
    
    public void updateDecimalFormatSymbols(DecimalFormatSymbols pDecimalFormatSymbols)
    {
        if(null != mNaN)
        {
            pDecimalFormatSymbols.setNaN(mNaN);
        }
        if(null != mInfinity)
        {
            pDecimalFormatSymbols.setInfinity(mInfinity);
        }
	if(! mLocalizeDecimal)
	{
	    pDecimalFormatSymbols.setDecimalSeparator('.');
	}
    }
    
    public String getNaN()
    {
        return(mNaN);
    }
    
    public String getInfinity()
    {
        return(mInfinity);
    }
    
    public static final TimeSeriesOutputFormat CSV_EXCEL = new TimeSeriesOutputFormat("CSV-excel", '#', null, null, false);
    
    public static final TimeSeriesOutputFormat CSV_MATLAB = new TimeSeriesOutputFormat("CSV-matlab", '%', "nan", "inf", false);
    
    public static final TimeSeriesOutputFormat CSV_GNUPLOT = new TimeSeriesOutputFormat("CSV-gnuplot", '#', "nan", "inf", false);
 
    public static String []getSortedFileFormatNames()
    {
        Set fileFormatNamesSet = mMap.keySet();
        LinkedList fileFormatNamesList = new LinkedList(fileFormatNamesSet);
        Collections.sort(fileFormatNamesList);
        return((String []) fileFormatNamesList.toArray(new String[0]));
    }

    public char getCommentChar()
    {
        return(mCommentChar);
    }
}

