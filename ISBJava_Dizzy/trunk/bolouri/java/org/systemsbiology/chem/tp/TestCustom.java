package org.systemsbiology.chem.tp;

import org.systemsbiology.chem.*;
import org.systemsbiology.math.Expression;
import java.io.PrintWriter;

public class TestCustom
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
            reactionX.addParameter(new Parameter("k", new Expression("3.0")));
            reactionX.setRate(new Expression("k * A"));
            reactionY.setRate(1.0);
            Model model = new Model("model");
            model.addReaction(reactionX);
            model.addReaction(reactionY);
            System.out.println(model.toString());
            GillespieSimulator simulator = new GillespieSimulator(model);
            String []requestedSymbolNames = { "A", "B" };
            double []timeValues = new double[NUM_TIME_POINTS];
            Object []symbolValues = new Object[NUM_TIME_POINTS];
            
            long curTime = System.currentTimeMillis();

            simulator.simulate(0.0, 
                               1000.0, 
                               NUM_TIME_POINTS,
                               requestedSymbolNames,
                               timeValues,
                               symbolValues,
                               1);

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
