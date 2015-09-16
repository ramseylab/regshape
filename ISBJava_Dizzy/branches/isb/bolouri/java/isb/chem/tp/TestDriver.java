package isb.chem.tp;

import isb.chem.*;
import java.io.*;

/**
 * Sample program that uses the {@link GillespieSimulator} algorithm to simulate
 * the dynamics of a pair of simple chemical {@linkplain Reaction reactions} 
 * involving three chemical {@linkplain Species species}.
 *
 * @see Species
 * @see Reaction
 * @see GillespieSimulator
 */
public class TestDriver
{
    public static final int NUMBER_TIME_POINTS = 10;

    public static void main(String []pArgs)
    {
        try
        {
            Compartment universal = new Compartment("universal");
            Species s1 = new Species("s1", universal);
            Species s2 = new Species("s2", universal);
            Species s3 = new Species("s3", universal);

            Reaction r1 = new Reaction("r1");
            r1.addReactant(s1);
            r1.addReactant(s2, 2);
            r1.addProduct(s3);
            r1.setRate(1.0);

            Reaction r2 = new Reaction("r2");
            r2.addReactant(s3);
            r2.addProduct(s2);
            r2.addProduct(s1);
            r2.setRate(0.1);

            SpeciesPopulations initialData = new SpeciesPopulations();
            initialData.setSpeciesPopulation(s1, 100);
            initialData.setSpeciesPopulation(s2, 101);
            initialData.setSpeciesPopulation(s3, 5);

            Model model = new Model("TestDriver");
            model.addReaction(r1);
            model.addReaction(r2);

            SpeciesPopulations []populationSamples = new SpeciesPopulations[NUMBER_TIME_POINTS];
            GillespieSimulator gillespie = new GillespieSimulator();
            gillespie.setDebugOutput(new PrintWriter(System.out, true));
            double startTime = 0.0;
            double timeConstant = gillespie.computeInitialAggregateTimeConstant(model, initialData, startTime);
            double[] timeSamples = new double[NUMBER_TIME_POINTS];
            double stopTime = 100000.0 * timeConstant;
            System.out.println("stopTime: " + stopTime);
            for(int ctr = 0; ctr < NUMBER_TIME_POINTS; ++ctr)
            {
                timeSamples[ctr] = stopTime * ((double) ctr)/((double) NUMBER_TIME_POINTS);
            }
            gillespie.evolve(model, 
                             initialData, 
                             timeSamples, 
                             startTime,
                             stopTime,
                             populationSamples);
            for(int ctr = 0; ctr < NUMBER_TIME_POINTS; ++ctr)
            {
                System.out.println("time: " + timeSamples[ctr] + "; s1: " + populationSamples[ctr].getSpeciesPopulation(s1) + "; s2: " + populationSamples[ctr].getSpeciesPopulation(s2));
            }
            
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
