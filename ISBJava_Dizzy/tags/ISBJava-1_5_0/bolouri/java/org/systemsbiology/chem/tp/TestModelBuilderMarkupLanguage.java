package org.systemsbiology.chem.tp;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import org.systemsbiology.chem.*;
import org.systemsbiology.util.*;
import org.systemsbiology.chem.sbml.*;

public class TestModelBuilderMarkupLanguage
{
    private static final int NUM_TIME_POINTS = 100;

    public static final void main(String []pArgs)
    {
        try
        {
            if(pArgs.length > 0)
            {
                String fileName = pArgs[0];
                File file = new File(fileName);
                if(file.exists())
                {
                    FileReader fileReader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    MarkupLanguageImporter importer = new MarkupLanguageImporter();
//                    ModelBuilderMarkupLanguage builder = new ModelBuilderMarkupLanguage(importer);
                    ModelBuilderCommandLanguage builder = new ModelBuilderCommandLanguage();
                    IncludeHandler includeHandler = new IncludeHandler();
                    includeHandler.setDirectory(new File(file.getParentFile().getAbsolutePath()));
                    Model model = builder.buildModel(bufferedReader, includeHandler);
                    System.out.println(model.toString());
                    System.exit(1);
                    SimulatorDeterministicRungeKuttaFixed simulator = new SimulatorDeterministicRungeKuttaFixed();
                    simulator.initialize(model);
                    String []requestedSymbolNames = {"PX", "PY", "PZ"};
                    double []timeValues = new double[NUM_TIME_POINTS];
                    Object []symbolValues = new Object[NUM_TIME_POINTS];
                    SimulatorParameters simParams = simulator.getDefaultSimulatorParameters();
                    simulator.simulate(0.0,
                                       100.0,
                                       simParams,
                                       NUM_TIME_POINTS,
                                       requestedSymbolNames,
                                       timeValues,
                                       symbolValues);

                    TimeSeriesSymbolValuesReporter.reportTimeSeriesSymbolValues(new PrintWriter(System.out),
                                                                                requestedSymbolNames,
                                                                                timeValues,
                                                                                symbolValues,
                                                                                TimeSeriesOutputFormat.CSV_EXCEL);
                }
                else
                {
                    System.err.println("sorry, the file does not exist: " + fileName);
                }
            }
            else
            {
                System.err.println("please supply a file name");
            }
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
