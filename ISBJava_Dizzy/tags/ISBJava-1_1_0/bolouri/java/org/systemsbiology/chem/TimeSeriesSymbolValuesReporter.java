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

public class TimeSeriesSymbolValuesReporter
{
    public static final void reportTimeSeriesSymbolValues(PrintWriter pPrintWriter, 
                                                          String []pRequestedSymbolNames, 
                                                          double []pTimeValues,
                                                          Object []pSymbolValues)
    {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(6);
        nf.setGroupingUsed(false);
        reportTimeSeriesSymbolValues(pPrintWriter,
                                     pRequestedSymbolNames,
                                     pTimeValues,
                                     pSymbolValues,
                                     nf);
    }


    public static final void reportTimeSeriesSymbolValues(PrintWriter pPrintWriter, 
                                                          String []pRequestedSymbolNames, 
                                                          double []pTimeValues,
                                                          Object []pSymbolValues,
                                                          NumberFormat pNumberFormat)
    
    {
        int numSymbols = pRequestedSymbolNames.length;

        StringBuffer sb = new StringBuffer();
        sb.append("# time, ");
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
