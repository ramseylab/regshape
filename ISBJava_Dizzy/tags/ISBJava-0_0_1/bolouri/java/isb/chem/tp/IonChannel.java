package isb.chem.tp;

import isb.chem.*;
import java.util.Date;

/**
 * Sample program that uses the {@link GillespieSimulator} algorithm to simulate
 * the dynamics of an ion channel system (two species representing a switch
 * being in the &quot;open&quot; or &quot;closed&quot; states, respectively.
 * For more information, refer to Chapter 11 of: 
 * <blockquote>
 * <table border="1">
 * <tr><td>
 * <em>Computational cell biology</em>, by C. P. Fall, E. S. Marland,
 * J. M. Wagner, and J. J. Tyson. Springer-Verlag, New York, 2002.
 * </td></tr>
 * </table>
 * </blockquote>
 * This book defines the model system being simulated in this test program,
 * and shows what output (in terms of steady-state species populations) is 
 * expected.
 *
 * @see Species
 * @see Reaction
 * @see GillespieSimulator
 */
public class IonChannel
{
    public static final int NUMBER_TIME_POINTS = 10;

    public static void main(String []pArgs)
    {
        try
        {
            Compartment universal = new Compartment("universal");
            Species s1 = new Species("s1", universal);
            Species s2 = new Species("s2", universal);

            Reaction r1 = new Reaction("r1");
            r1.addReactant(s1);
            r1.addProduct(s2);
            r1.setRate(1.0);

            Reaction r2 = new Reaction("r2");
            r2.addReactant(s2);
            r2.addProduct(s1);
            r2.setRate(3.0);

            SpeciesPopulations initialData = new SpeciesPopulations();
            initialData.setSpeciesPopulation(s1, 500);
            initialData.setSpeciesPopulation(s2, 500);

            Model model = new Model("IonChannel");
            model.addReaction(r1);
            model.addReaction(r2);

            System.out.println(model.toString());

            GillespieSimulator gillespie = new GillespieSimulator();

            System.out.println("date: " + new Date(System.currentTimeMillis()));

            SpeciesPopulations speciesPops = new SpeciesPopulations();

            double startTime = 0.0;

            for(int simCtr = 0; simCtr < 10; ++simCtr)
            {
  
                double timeConstant = gillespie.computeInitialAggregateTimeConstant(model, initialData, 0.0);
                gillespie.evolve(model, 
                                 initialData, 
                                 startTime,
                                 500000,
                                 speciesPops);

                double s1pop = speciesPops.getSpeciesPopulation(s1);
                double s2pop = speciesPops.getSpeciesPopulation(s2);
//                System.out.println(s1pop + "," + s2pop);
            }

            System.out.println("date: " + new Date(System.currentTimeMillis()));
//            System.out.println("model: " + model.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
