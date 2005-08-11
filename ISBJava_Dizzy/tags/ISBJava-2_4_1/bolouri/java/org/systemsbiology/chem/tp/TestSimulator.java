package org.systemsbiology.chem.tp;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.chem.*;
import java.io.PrintWriter;

public class TestSimulator
{
    private static final int NUM_TIME_POINTS = 100;

    public static final void main(String []pArgs)
    {
        try
        {
            Compartment compartment = new Compartment("univ");
            Species speciesA = new Species("A", compartment);
            speciesA.setSpeciesPopulation(100.0);
            Species speciesB = new Species("B", compartment);
            speciesB.setSpeciesPopulation(500.0);
            Reaction reactionX = new Reaction("X");
            reactionX.addReactant(speciesA, 1);
            reactionX.addProduct(speciesB, 1);
            Reaction reactionY = new Reaction("Y");
            reactionY.addReactant(speciesB, 1);
            reactionY.addProduct(speciesA, 1);
            reactionX.setRate(1.0);
            reactionY.setRate(1.0);
            Model model = new Model("model");
            model.addReaction(reactionX);
            model.addReaction(reactionY);
            // this statement is really only needed for models that contain expressions
            // that reference reserved symbols such as "Navo" or "time":
            model.setReservedSymbolMapper(new ReservedSymbolMapperChemCommandLanguage());
            System.out.println(model.toString());
            SimulatorStochasticGillespie simulator = new SimulatorStochasticGillespie();
            simulator.initialize(model);
            SimulatorParameters simParams = new SimulatorParameters();
            simParams.setEnsembleSize(new Integer(40));
            String []requestedSymbolNames = { "A", "B" };
            
            long curTime = System.currentTimeMillis();

            SimulationResults simulationResults = simulator.simulate(0.0, 
                                                                     1000.0, 
                                                                     simParams,
                                                                     NUM_TIME_POINTS,
                                                                     requestedSymbolNames);

            long finalTime = System.currentTimeMillis();
            double elapsedTimeSec = (double) (finalTime - curTime) / 1000.0;
            System.out.println("elapsed time: " + elapsedTimeSec);

            int numSymbols = requestedSymbolNames.length;

            double []timeValues = simulationResults.getResultsTimeValues();
            Object []symbolValues = simulationResults.getResultsSymbolValues();

            TimeSeriesSymbolValuesReporter.reportTimeSeriesSymbolValues(new PrintWriter(System.out),
                                                                        requestedSymbolNames,
                                                                        timeValues,
                                                                        symbolValues,
                                                                        TimeSeriesOutputFormat.CSV_EXCEL);
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
