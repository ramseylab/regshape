package org.systemsbiology.chem;

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
            sb.append(pNumberFormat.format(pTimeValues[ctr]) + ", ");
            double []symbolValue = (double []) pSymbolValues[ctr];
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
