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
    static
    {
        mMap = new HashMap();
    }
    private TimeSeriesOutputFormat(String pName, char pCommentChar)
    {
        mName = pName;
        mCommentChar = pCommentChar;
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
    public static final TimeSeriesOutputFormat CSV_EXCEL = new TimeSeriesOutputFormat("CSV-excel", '#');
    public static final TimeSeriesOutputFormat CSV_MATLAB = new TimeSeriesOutputFormat("CSV-matlab", '%');
    public static final TimeSeriesOutputFormat CSV_GNUPLOT = new TimeSeriesOutputFormat("CSV-gnuplot", '#');
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

