package org.systemsbiology.chem;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.*;
import java.util.*;
import odeToJava.*;
import odeToJava.modules.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;

public abstract class SimulatorOdeToJavaBase extends Simulator implements ODE
{
    public static final int DEFAULT_MIN_NUM_STEPS = 10000;
    public static final double DEFAULT_MAX_ALLOWED_RELATIVE_ERROR = 0.0001;
    public static final double DEFAULT_MAX_ALLOWED_ABSOLUTE_ERROR = 0.01;

    private double []mDerivative;
    private double []mScratch;

    public void initialize(Model pModel, SimulationController pSimulationController) throws DataNotFoundException
    {
        initializeSimulator(pModel, pSimulationController);
        initializeDynamicSymbolAdjustmentVectors(mDynamicSymbols);
        int numSpecies = mDynamicSymbolValues.length;
        mDerivative = new double[numSpecies];
        mScratch = new double[numSpecies];
    }

    public SimulatorParameters getDefaultSimulatorParameters()
    {
        SimulatorParameters sp = new SimulatorParameters();
        sp.setMaxAllowedRelativeError(DEFAULT_MAX_ALLOWED_RELATIVE_ERROR);
        sp.setMaxAllowedAbsoluteError(DEFAULT_MAX_ALLOWED_ABSOLUTE_ERROR);
        return(sp);
    }

    public boolean isStochasticSimulator()
    {
        return(false);
    }

    public boolean allowsInterrupt()
    {
        return(false);
    }

    protected abstract void runExternalSimulation(Span pSimulationTimeSpan,
                                                  double []pInitialDynamicSymbolValues,
                                                  double pInitialStepSize,
                                                  double pMaxAllowedRelativeError,
                                                  double pMaxAllowedAbsoluteError,
                                                  String pTempOutputFileName);

    public void simulate(double pStartTime, 
                         double pEndTime,
                         SimulatorParameters pSimulatorParameters,
                         int pNumResultsTimePoints,
                         String []pResultsSymbolNames,
                         double []pRetResultsTimeValues,
                         Object []pRetResultsSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, SimulationAccuracyException, SimulationFailedException
    {
        conductPreSimulationCheck(pStartTime,
                                  pEndTime,
                                  pSimulatorParameters,
                                  pNumResultsTimePoints,
                                  pResultsSymbolNames,
                                  pRetResultsTimeValues,
                                  pRetResultsSymbolValues);

        double []timesArray = createTimesArray(pStartTime, 
                                               pEndTime,
                                               pNumResultsTimePoints);
        int numTimePoints = timesArray.length;
        System.arraycopy(timesArray, 0, pRetResultsTimeValues, 0, numTimePoints);
        
        Symbol []requestedSymbols = createRequestedSymbolArray(mSymbolMap,
                                                               pResultsSymbolNames);

        double initialStepSize = (pEndTime - pStartTime)/((double) DEFAULT_MIN_NUM_STEPS);


        Double maxAllowedRelativeErrorObj = pSimulatorParameters.getMaxAllowedRelativeError();
        if(null == maxAllowedRelativeErrorObj)
        {
            throw new IllegalArgumentException("no maximum allowed relative error was supplied");
        }

        double maxAllowedRelativeError = maxAllowedRelativeErrorObj.doubleValue();
        if(maxAllowedRelativeError <= 0.0)
        {
            throw new IllegalArgumentException("invalid maximum allowed relative error was specified");
        }


        Double maxAllowedAbsoluteErrorObj = pSimulatorParameters.getMaxAllowedAbsoluteError();
        if(null == maxAllowedAbsoluteErrorObj)
        {
            throw new IllegalArgumentException("no maximum allowed absolute error was supplied");
        }

        double maxAllowedAbsoluteError = maxAllowedAbsoluteErrorObj.doubleValue();
        if(maxAllowedAbsoluteError <= 0.0)
        {
            throw new IllegalArgumentException("invalid maximum allowed absolute error was specified");
        }

        if(pNumResultsTimePoints <= 0)
        {
            throw new IllegalArgumentException("invalid number of time points requested");
        }

        Span simulationTimeSpan = new Span(timesArray);

        File tempOutputFile = null;
        try
        {
            tempOutputFile = File.createTempFile("simulationOutput", ".txt");
        }
        catch(IOException e)
        {
            throw new SimulationFailedException("unable to open temporary output file, error message is: " + e.toString());
        }
        String tempOutputFileName = tempOutputFile.getAbsolutePath();
//        System.err.println("writing simulation results to file: " + tempOutputFileName);

        prepareForSimulation(pStartTime);

        runExternalSimulation(simulationTimeSpan,
                              mDynamicSymbolValues,
                              initialStepSize,
                              maxAllowedRelativeError,
                              maxAllowedAbsoluteError,
                              tempOutputFileName);

        try
        {
            readSimulationOutput(tempOutputFile,
                                 mSymbolEvaluator,
                                 mDynamicSymbolValues,
                                 pStartTime,
                                 pEndTime,
                                 pNumResultsTimePoints,
                                 requestedSymbols,
                                 pRetResultsTimeValues,
                                 pRetResultsSymbolValues);
        }
        catch(InvalidInputException e)
        {
            throw new SimulationFailedException("unable to parse the output from the simulator; error message is: " + e.toString());
        }
        catch(FileNotFoundException e)
        {
            throw new SimulationFailedException("unable to parse the output from the simulator; error message is: " + e.toString());
        }
        catch(IOException e)
        {
            throw new SimulationFailedException("unable to parse the output from the simulator; error message is: " + e.toString());
        }
    }

    protected void readSimulationOutput(File pSimulationResultsFile, 
                                        SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                        double []pDynamicSymbolValues,
                                        double pStartTime,
                                        double pEndTime,
                                        int pNumResultsTimePoints,
                                        Symbol []pRequestedSymbols,
                                        double []pRetResultsTimeValues,
                                        Object []pRetResultsSymbolValues) throws InvalidInputException, DataNotFoundException, FileNotFoundException, IOException, SimulationFailedException
    {
        FileReader fileReader = new FileReader(pSimulationResultsFile);
        BufferedReader inputReader = new BufferedReader(fileReader);
        String line = null;
        int lineNumber = 0;
        SymbolEvaluatorChemSimulation symbolEvaluator = pSymbolEvaluator;
        int numDynamicSymbols = pDynamicSymbolValues.length;
        int numRequestedSymbols = pRequestedSymbols.length;
        int []requestedSymbolIndices = new int[numRequestedSymbols];

        int timeIndex = 0;
        while(null != (line = inputReader.readLine()))
        {
            ++lineNumber;
            if(0 == line.trim().length())
            {
                continue;
            }

            double curTime = pStartTime;
            StringTokenizer st = new StringTokenizer(line, " ");
            int index = -1;
            while(st.hasMoreTokens())
            {
                String token = st.nextToken();
                double value;
                Double valueObj = null;
                try
                {
                    valueObj = new Double(token);
                }
                catch(NumberFormatException e)
                {
                    throw new InvalidInputException("failed to parse simulation output file at line: " + lineNumber);
                }
                value = valueObj.doubleValue();
                
                if(index == -1)
                {
                    symbolEvaluator.setTime(value);
                    curTime = value;
                }
                else
                {
                    pDynamicSymbolValues[index] = value;
                }
                ++index;
            }

            if(index != numDynamicSymbols)
            {
                throw new InvalidInputException("failed to parse expected number of values from simulation output file at line: " + line);
            }
            
            timeIndex = addRequestedSymbolValues(curTime, 
                                                 timeIndex, 
                                                 pRequestedSymbols,
                                                 symbolEvaluator,
                                                 pRetResultsTimeValues,
                                                 pRetResultsSymbolValues);
        }

        if(timeIndex != pNumResultsTimePoints)
        {
            throw new SimulationFailedException("failed to obtain results from simulation; try making the \"max allowed relative error\" smaller");
        }
    }

    public double[] f(double t, double[] x)
    {
        // copy the values in x to the array that is read by the "symbol evaluator"
        System.arraycopy(x, 0, mDynamicSymbolValues, 0, mDynamicSymbolValues.length);

        SymbolEvaluatorChemSimulation symbolEvaluator = mSymbolEvaluator;
        // set the time to t
        symbolEvaluator.setTime(t);

        try
        {
            computeDerivative(mSpeciesRateFactorEvaluator,
                              mSymbolEvaluator,
                              mReactions,
                              mDynamicSymbolAdjustmentVectors,
                              mReactionProbabilities,
                              mScratch,
                              mDerivative);
        }
        catch(DataNotFoundException e)
        {
            throw new RuntimeException("data not found", e);
        }
        
        return(mDerivative);
    }

    public double[] g(double t, double[] x)
    {   // empty implementation of g because there are no events associated
        double[] event = new double[1];   // with this function
        return(event);
    }
}
