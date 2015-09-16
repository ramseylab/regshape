package org.systemsbiology.chem.scripting.tp;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import org.systemsbiology.chem.*;
import org.systemsbiology.chem.scripting.*;

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
                    DeterministicSimulatorFixed simulator = new DeterministicSimulatorFixed();
                    simulator.initialize(model, null);
                    String []requestedSymbolNames = {"PX", "PY", "PZ"};
                    double []timeValues = new double[NUM_TIME_POINTS];
                    Object []symbolValues = new Object[NUM_TIME_POINTS];
                    simulator.simulate(0.0,
                                       100.0,
                                       NUM_TIME_POINTS,
                                       1000,
                                       requestedSymbolNames,
                                       timeValues,
                                       symbolValues);

                    TimeSeriesSymbolValuesReporter.reportTimeSeriesSymbolValues(new PrintWriter(System.out),
                                                                                requestedSymbolNames,
                                                                                timeValues,
                                                                                symbolValues);
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
