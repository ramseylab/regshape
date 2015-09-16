package org.systemsbiology.chem.tp;

import org.systemsbiology.chem.*;
import org.systemsbiology.math.Expression;
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
            DeterministicSimulatorFixed simulator = new DeterministicSimulatorFixed();
            simulator.initialize(model, null);
            String []requestedSymbolNames = { "A", "B" };
            double []timeValues = new double[NUM_TIME_POINTS];
            Object []symbolValues = new Object[NUM_TIME_POINTS];
            
            long curTime = System.currentTimeMillis();

            simulator.simulate(0.0, 
                               200.0, 
                               NUM_TIME_POINTS,
                               1,
                               requestedSymbolNames,
                               timeValues,
                               symbolValues);

            long finalTime = System.currentTimeMillis();
            double elapsedTimeSec = (double) (finalTime - curTime) / 1000.0;
            System.out.println("elapsed time: " + elapsedTimeSec);

            int numSymbols = requestedSymbolNames.length;

            TimeSeriesSymbolValuesReporter.reportTimeSeriesSymbolValues(new PrintWriter(System.out),
                                                                        requestedSymbolNames,
                                                                        timeValues,
                                                                        symbolValues);
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
