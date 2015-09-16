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

public class TestMultistep
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
            speciesB.setSpeciesPopulation(0.0);

            Species speciesC = new Species("C", compartment);
            speciesC.setSpeciesPopulation(0.0);
            Reaction reactionY = new Reaction("Y");
            reactionY.addProduct(speciesC, 1);
            reactionY.setRate(1.0);

            Reaction reactionX = new Reaction("X");
            reactionX.addReactant(speciesA, 1);
            reactionX.addProduct(speciesB, 1);
            reactionX.setRate(1.0);
            reactionX.setNumSteps(12);

            Model model = new Model("model");
            model.addReaction(reactionX);
            model.addReaction(reactionY);

            System.out.println(model.toString());
            SimulatorDeterministicRungeKuttaFixed simulator = new SimulatorDeterministicRungeKuttaFixed();
            simulator.initialize(model);
            String []requestedSymbolNames = { "A", "B" };
            
            long curTime = System.currentTimeMillis();

            SimulatorParameters simParams = simulator.getDefaultSimulatorParameters();

            SimulationResults simulationResults = simulator.simulate(0.0, 
                                                                     200.0, 
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
