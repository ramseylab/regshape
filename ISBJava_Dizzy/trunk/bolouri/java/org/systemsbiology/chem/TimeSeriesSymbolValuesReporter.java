package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.*;

public class TimeSeriesSymbolValuesReporter
{
    public static class OutputFormat
    {
        private final String mName;
        private final char mCommentChar;
        private static final HashMap mMap;
        static
        {
            mMap = new HashMap();
        }
        private OutputFormat(String pName, char pCommentChar)
        {
            mName = pName;
            mCommentChar = pCommentChar;
            mMap.put(pName, this);
        }
        public String toString()
        {
            return(mName);
        }
        public static OutputFormat get(String pName)
        {
            return((OutputFormat) mMap.get(pName));
        }
        public static final OutputFormat CSV_EXCEL = new OutputFormat("CSV-excel", '#');
        public static final OutputFormat CSV_MATLAB = new OutputFormat("CSV-matlab", '%');
        public static final OutputFormat CSV_GNUPLOT = new OutputFormat("CSV-gnuplot", '#');
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

    public static final void reportTimeSeriesSymbolValues(PrintWriter pPrintWriter, 
                                                          String []pRequestedSymbolNames, 
                                                          double []pTimeValues,
                                                          Object []pSymbolValues,
                                                          OutputFormat pOutputFormat) throws IllegalArgumentException
    {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(6);
        nf.setGroupingUsed(false);
        reportTimeSeriesSymbolValues(pPrintWriter,
                                     pRequestedSymbolNames,
                                     pTimeValues,
                                     pSymbolValues,
                                     nf,
                                     pOutputFormat);
    }

    public static final void reportTimeSeriesSymbolValues(PrintWriter pPrintWriter, 
                                                          String []pRequestedSymbolNames, 
                                                          double []pTimeValues,
                                                          Object []pSymbolValues,
                                                          NumberFormat pNumberFormat,
                                                          OutputFormat pOutputFormat) throws IllegalArgumentException
    
    {
        int numSymbols = pRequestedSymbolNames.length;

        if(null == pOutputFormat)
        {
            throw new IllegalArgumentException("required argument pOutputFormat was passed as null");
        }

        if(null == pNumberFormat)
        {
            throw new IllegalArgumentException("required argument pNumberFormat was passed as null");
        }

        StringBuffer sb = new StringBuffer();
        sb.append(Character.toString(pOutputFormat.getCommentChar()));
        sb.append(" time, ");
        for(int symCtr = 0; symCtr < numSymbols; ++symCtr)
        {
            sb.append(pRequestedSymbolNames[symCtr]);
            if(symCtr < numSymbols - 1)
            {
                sb.append(", ");
            }
        }
        sb.append("\n");
        int numTimePoints = pTimeValues.length;
        for(int ctr = 0; ctr < numTimePoints; ++ctr)
        {
            double []symbolValue = (double []) pSymbolValues[ctr];
            if(null == symbolValue)
            {
                break;
            }
            sb.append(pNumberFormat.format(pTimeValues[ctr]) + ", ");
            for(int symCtr = 0; symCtr < numSymbols; ++symCtr)
            {
                sb.append(pNumberFormat.format(symbolValue[symCtr]));
                if(symCtr < numSymbols - 1)
                {
                    sb.append(", ");
                }
            }
            sb.append("\n");
        }

        pPrintWriter.println(sb.toString());
    }    
}
