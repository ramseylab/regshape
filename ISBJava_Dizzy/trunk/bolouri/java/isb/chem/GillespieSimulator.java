package isb.chem;

import java.util.*;
import java.io.PrintWriter;
import isb.util.*;

/**
 * Conducts a stochastic simulation of a {@link Model} consisting of
 * one or more coupled chemical {@linkplain Reaction reactions} involving 
 * various chemical
 * {@linkplain Species species}, using the Gillespie algorithm.  
 * A detailed description of the Gillespie stochastic algorithm can be found 
 * in the article
 * <a name="reference"></a>
 * <blockquote>
 * <table border="1">
 * <tr><td>
 * D. T. Gillespie, 
 * &quot;A General Method for Numerically Simulating the Stochastic
 * Time Evolution of Coupled Chemical Species&quot;, <em>J. Comp.  Phys.</em>
 * <b>22</b>, 403-434 (1976).
 * </td></tr>
 * </table>
 * </blockquote>
 * The time simulation starts with
 * specified {@linkplain SpeciesPopulations inital data} for the chemical
 * species populations, and solves for the dynamics of the system during a
 * specified time range.   In its normal mode of operation, the simulator will 
 * conduct as many iterations as necessary until the specified time range 
 * of the simulation is exhausted, or until the species populations
 * get into a state whereby no further reactions can occur (whichever
 * comes first.  Chemical reactions in Gillespie's algorithm are 
 * <em>uni-directional</em> processes, meaning that a given chemical 
 * reaction with separate forward and reverse kinetic constants must be
 * defined as two separate &quot;reactions&quot; in Gillespie's stochastic
 * model.
 * <p />
 * This class uses the &quot;direct&quot; method of implementing the
 * Monte Carlo (stochastic) step in Gillespie's algorithm, which requires
 * only two random number generations per reaction iteration, but is 
 * slightly more complex than the alternative &quot;first-reaction&quot;
 * method of implementing the Monte Carlo step.  For more details, please
 * refer to Gillespie's paper.
 * <p />
 * It is important to note that this Java implementation of Gillespie's algorithm
 * employs the <code>java.util.Random</code> random number generator class.
 * The <code>getDouble()</code> method of this class can generate any one 
 * of about 2^53 different numeric responses with approximately equal 
 * probability.  That works out to about 14 decimal digits of solid randomness 
 * in the floating point number returned.  So, if you are specifying a 
 * reaction parameter R1 that is more than 10^14 times smaller than the
 * aggregate reaction parameter, it is to be expected that (for roughly equal
 * reactant populations between R1's reactants and the other reactions), 
 * this stochastic simulator will have difficulty generating any R1 reactions.
 * The moral of the story is, if your model has a range of more than about 10^10
 * between its smallest reaction parameter, and its aggregate (summed over all 
 * reactions) reaction parameter, then this simulator will almost certainly
 * not work properly on your model.  For further details on this point, please
 * see p.428 of the <a href="#reference">reference</a> for Gillespie's paper.
 * <p />
 * Here is a sample invocation of Gillespie's algorithm, for a simple
 * system consisting of species S1, S2, and S3, and two reactions R1 and R2:
 * <blockquote>
 * <pre>
 * R1:  S1 + S2 -&gt; S3
 * R2:  S3 -&gt; S2 + S2 + S1
 * </pre>
 * </blockquote>
 * To solve for the dynamics of this system with initial chemical populations 
 * of 1000, 500, and 300 respectively for S1, S2, and S3:
 * <blockquote>
 * <pre>
 *             import isb.chem.*;
 * 
 *             Compartment univ = new Compartment("univ");
 *             Species s1 = new Species("s1", univ);  // define species s1, s2, s3,
 *             Species s2 = new Species("s2", univ);  // which are the three chemical
 *             Species s3 = new Species("s3", univ);  // species in this "model"

 *             Reaction r1 = new Reaction("r1"); // define a chemical reaction
 *             r1.addReactant(s1);               // with species s1 and s2 as
 *             r1.addReactant(s2);               // reactants, and with species
 *             r1.addProduct(s3);                // s3 as a product
 *             r1.setRate(1.0);

 *             Reaction r2 = new Reaction("r2"); // define a chemical reaction
 *             r2.addReactant(s3);               // with species s3 as a 
 *             r2.addProduct(s2, 2);             // reactant, and 2*s2 + s1 as
 *             r2.addProduct(s1);                // reaction products
 *             r2.setRate(0.1);
 *
 *                                              // define the initial conditions
 *             SpeciesPopulations initialData = new SpeciesPopulations();
 *             initialData.setSpeciesPopulation(s1, 1000);
 *             initialData.setSpeciesPopulation(s2, 500);
 *             initialData.setSpeciesPopulation(s3, 300);
 *
 *             Model model = new Model("testModel");  // define a new "model" to hold
 *             model.addReaction(r1);                 // and the compartment "univ"
 *             model.addReaction(r2);

 *             SpeciesPopulations []populationSamples = new SpeciesPopulations[10];
 *             GillespieSimulator gillespie = new GillespieSimulator();
 *             double startTime = 0.0;
 *             double timeConstant = gillespie.computeInitialAggregateTimeConstant(model, initialData, startTime);
 *             double stopTime = 10.0 * timeConstant; // choose a "stop" time
 *
 *             gillespie.evolve(model, 
 *                              initialData, 
 *                              stopTime,
 *                              populationSamples);   // run the simulation
 * 
 * </pre>
 * </blockquote>
 * Note that in the above sample code fragment, all exception-handling code 
 * has been removed to enhance the readability of the code.  This code fragment
 * will sample the species populations at 10 different points at equal
 * time intervals during the simulation.
 *
 * @see Reaction
 * @see Model
 * @see SpeciesPopulations
 * @see Species
 * 
 * @author Stephen Ramsey
 */

public class GillespieSimulator implements ISimulator, IAliasableClass
{
    /*========================================*
     * constants
     *========================================*/
    public static final String CLASS_ALIAS = "gillespie";
    private static final int SIMULATION_CANCELLED = -1;

    /*========================================*
     * member data
     *========================================*/
    private Random mRandomNumberGenerator;
    private PrintWriter mDebugOutput;
    private DebugOutputVerbosityLevel mDebugLevel;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    public DebugOutputVerbosityLevel getDebugLevel()
    {
        return(mDebugLevel);
    }
    
    public void setDebugLevel(DebugOutputVerbosityLevel pDebugLevel) throws IllegalArgumentException
    {
        if(null == pDebugLevel)
        {
            throw new IllegalArgumentException("invalid debug output verbosity level");
        }
        mDebugLevel = pDebugLevel;
    }

    private boolean debuggingIsEnabled()
    {
        return(null != getDebugOutput());
    }

    /**
     * Returns the <code>PrintWriter</code> output for debug printing.
     * This is set to <code>null</code> by default, which inhibits
     * any printing of debugging information. 
     *
     * @return the <code>PrintWriter</code> output for debug printing.
     */
    public PrintWriter getDebugOutput()
    {
        return(mDebugOutput);
    }

    /**
     * Sets the <code>PrintWriter</code> output for debug printing.
     * This is set to <code>null</code> by default, which inhibits
     * any printing of debugging information.  Calling this method
     * and specifying a non-null <code>PrintWriter</code> object
     * turns on debug printing, to the specified <code>PrintWriter</code>
     * stream.  Examples include <code>System.out</code> (for the standard
     * output stream) and <code>System.err</code> (for the standard error
     * stream).  Calling this method and specifying
     * <code>null</code> turns off debug printing, until the next time
     * this method is called with a non-null argument.
     */
    public void setDebugOutput(PrintWriter pDebugOutput)
    {
        mDebugOutput = pDebugOutput;
    }

    private void setRandomNumberGenerator(Random pRandomNumberGenerator)
    {
        mRandomNumberGenerator = pRandomNumberGenerator;
    }

    private Random getRandomNumberGenerator()
    {
        return(mRandomNumberGenerator);
    }

    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeDebug()
    {
        setDebugOutput(null);
        setDebugLevel(DebugOutputVerbosityLevel.NONE);
    }

    private void initializeRandomNumberGenerator()
    {
        setRandomNumberGenerator(new Random(System.currentTimeMillis()));
    }

    private void initialize()
    {
        initializeDebug();
        initializeRandomNumberGenerator();
    }

    /*========================================*
     * constructors
     *========================================*/
    /**
     * Creates an instance of the Gillespie simulator.
     *
     * This method creates and initializes a <code>java.util.Random</code>
     * random number generator with a seed given by the 
     * <code>System.currentTimeMillis</code> (current epoch time in 
     * milliseconds since midnight on January 1st 1970).  
     * The seed for the random number generator can be changed by
     * calling {@link #setRandomNumberGeneratorSeed(long)}.
     *
     * The initial value of the debug output <code>PrintWriter</code>
     * is set to <code>null</code>, meaning that debugging output
     * is disabled.  This can be altered by calling the
     * {@link #setDebugOutput(PrintWriter)} method and specifying 
     * a non-null <code>PrintWriter</code> object.
     */
    public GillespieSimulator()
    {
        initialize();
    }

    /*========================================*
     * private methods
     *========================================*/



    private void debugPrintln(String pMessage, DebugOutputVerbosityLevel pMinDebugLevel)
    {
        PrintWriter debugOutput = getDebugOutput();
        if(debuggingIsEnabled() && null != debugOutput && 
           (getDebugLevel().greaterThan(pMinDebugLevel) ||
            getDebugLevel().equals(pMinDebugLevel)))
        {
            debugOutput.println(pMessage);
        }
    }

    /**
     * Returns a random number within the unit interval (0,1]
     * with uniform probability distribution.  Uses the 
     * java.util.Random facility to obtain the random number.
     * 
     * @return a random number within the unit interval (0,1]
     * with uniform probability distribution. 
     */
    private double getRandomNumberUniformInterval()
    {
        return( 1.0 - getRandomNumberGenerator().nextDouble() );
    }


    private void computeReactionProbabilityDensities(Model pModel,
                                                     SpeciesPopulations pSpeciesPopulations, 
                                                     Vector pReactionProbabilityDensities,
                                                     double pTime) throws IllegalArgumentException
    {
        int numReactions = pReactionProbabilityDensities.size();
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            ReactionProbabilityDensity reactionProbabilityDensity = (ReactionProbabilityDensity) pReactionProbabilityDensities.elementAt(reactionCtr);
            Reaction reaction = reactionProbabilityDensity.getReaction();
            double probabilityDensity;
            try
            {
                probabilityDensity = reaction.computeReactionProbabilityDensityPerUnitTime(pModel, pSpeciesPopulations, pTime);
            }
            catch(DataNotFoundException e)
            {
                throw new IllegalArgumentException("unable to obtain reaction probability density for reaction " + reaction.toString() + "; error message is: " + e.toString());
            }
            reactionProbabilityDensity.setProbabilityDensity(probabilityDensity);
        }
    }

    private void validateSimulationParameters(Model pModel, 
                                              SpeciesPopulations pInitialSpeciesPopulations,
                                              double []pSampleTimes, 
                                              double pStartTime,
                                              double pStopTime,
                                              SpeciesPopulations []pPopulationSamples) throws IllegalArgumentException, IllegalStateException
    {
        Double startTime = new Double(pStartTime);

        pModel.validate(pInitialSpeciesPopulations, startTime);

        if(pStopTime <= pStartTime)
        {
            throw new IllegalArgumentException("the stop time you specified is nonpositive: " + pStopTime);
        }

        int numTimeSamples = pSampleTimes.length;

        if(pPopulationSamples.length != numTimeSamples)
        {
            throw new IllegalArgumentException("the number of population samples and the number of time points are not equal; they are " + pPopulationSamples.length + " and " + numTimeSamples + ", respectively");
        }

        if(numTimeSamples <= 0)
        {
            throw new IllegalArgumentException("number of samples is not a natural number: " + numTimeSamples);
        }

        for(int timeCtr = 0; timeCtr < numTimeSamples; ++timeCtr)
        {
            double sampleTime = pSampleTimes[timeCtr];
            if(sampleTime < pStartTime)
            {
                throw new IllegalArgumentException("the " + timeCtr + "th time point is invalid: " + sampleTime);
            }

            if(timeCtr > 0)
            {
                if( sampleTime <= (pSampleTimes[timeCtr - 1]) )
                {
                    throw new IllegalArgumentException("the time points are invalid or out of order:  time point number " + timeCtr + " has a value " + sampleTime + " which is is less than or equal to time point number " + (timeCtr - 1) + " which has a value " + pSampleTimes[timeCtr - 1]);
                }                   
            }

            if(sampleTime > pStopTime)
            {
                throw new IllegalArgumentException("time point number " + timeCtr + " is greater than the stop time, " + pStopTime);
            }
        }    
    }

    private double computeAggregateReactionProbabilityDensity(Vector pReactionProbabilityDensities)
    {
        double aggregateReactionProbabilityDensity = 0.0;
        int numReactions = pReactionProbabilityDensities.size();
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            ReactionProbabilityDensity reactionProbabilityDensity = (ReactionProbabilityDensity) pReactionProbabilityDensities.elementAt(reactionCtr);
            Reaction reaction = reactionProbabilityDensity.getReaction();
            double probabilityDensity = reactionProbabilityDensity.getProbabilityDensity();
            aggregateReactionProbabilityDensity += probabilityDensity;
        }
        return(aggregateReactionProbabilityDensity);
    }


    /**
     * Carries out a single iteration of the Gillespie stochastic algorithm.
     * Uses a random number to pick the time of the next reaction, and then
     * uses a second random number to select which reaction occurs at that time.
     * Then implements the reaction and adjusts the species populations accordingly.
     */
    private boolean iterate(Model pModel,
                            SpeciesPopulations pSpeciesPopulations,
                            Vector pReactionProbabilityDensities,
                            MutableDouble pTime) throws IllegalArgumentException, DataNotFoundException, IllegalStateException
    {
        boolean canHaveAnotherIteration = false;

        double time = pTime.getValue();

        // compute reaction probability densities and store in container
        computeReactionProbabilityDensities(pModel,
                                            pSpeciesPopulations,
                                            pReactionProbabilityDensities,
                                            time);
        
        // compute the aggregate reaction probability density
        double aggregateProbabilityDensity = computeAggregateReactionProbabilityDensity(pReactionProbabilityDensities);

        if(aggregateProbabilityDensity > 0.0)
        {
            // select the time of next reaction
            double deltaTimeToNextReaction = chooseDeltaTimeToNextReaction(aggregateProbabilityDensity);

            if(debuggingIsEnabled())
            {
                debugPrintln("delta time to next reaction: " + deltaTimeToNextReaction, DebugOutputVerbosityLevel.HIGH);
            }

            // select the type of next reaction
            Reaction nextReaction = chooseTypeOfNextReaction(aggregateProbabilityDensity,
                                                             pReactionProbabilityDensities);

            if(debuggingIsEnabled())
            {
                debugPrintln("reaction selected: " + nextReaction, DebugOutputVerbosityLevel.MEDIUM);
            }

            // carry out reaction and adjust species populations
            nextReaction.recomputeSpeciesPopulationsForReaction(pSpeciesPopulations);

            // adjust time to time of next reaction
            time += deltaTimeToNextReaction;

            pTime.setValue(time);

            if(debuggingIsEnabled())
            {
                debugPrintln("time: " + time, DebugOutputVerbosityLevel.MEDIUM);
                debugPrintln("species populations:\n" + pSpeciesPopulations, DebugOutputVerbosityLevel.HIGH);
            }

            canHaveAnotherIteration = true;
        }
        else
        {
            if(debuggingIsEnabled())
            {
                debugPrintln("no more reactions can happen", DebugOutputVerbosityLevel.MEDIUM);
            }
        }

        return(canHaveAnotherIteration);
    }


    /**
     * According to Gillespie's probability distribution
     * function, returns a randomly selected time of the
     * &quot;next reaction&quot;, given a time constant which
     * is the reciprocal of the composite rate constant for the
     * system of interacting chemical species, noted as "a" in
     * Gillespie's notation in his 1976 J. Comp. Phys. paper).
     * The units of the time returned are the inverse of
     * the units of the rate constant supplied.  This function
     * uses the built-in Java random number generator.
     */
    private double chooseDeltaTimeToNextReaction(double pAggregateReactionProbabilityDensity)
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval();
        double inverseRandomNumberUniformInterval = 1.0 / randomNumberUniformInterval;
        assert (randomNumberUniformInterval >= 0.0) : ("randomNumberUniformInterval: " + randomNumberUniformInterval);
        double logInverseRandomNumberUniformInterval = Math.log(inverseRandomNumberUniformInterval);
        double timeConstant = 1.0 / pAggregateReactionProbabilityDensity;
        if(debuggingIsEnabled())
        {
            debugPrintln("aggregate reaction probability density: " + pAggregateReactionProbabilityDensity, DebugOutputVerbosityLevel.HIGH);
            debugPrintln("timeConstant: " + timeConstant, DebugOutputVerbosityLevel.HIGH);
            debugPrintln("logInverseRandomNumberUniformInterval: " + logInverseRandomNumberUniformInterval, DebugOutputVerbosityLevel.HIGH);
        }
        double deltaTime = timeConstant * logInverseRandomNumberUniformInterval;
        return(deltaTime);
    }

    private Reaction chooseTypeOfNextReaction(double pAggregateReactionProbabilityDensity, 
                                              Vector pReactionProbabilityDensities) throws IllegalArgumentException
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval();

        double cumulativeReactionProbabilityDensity = 0.0;

        double fractionOfAggregateReactionProbabilityDensity = randomNumberUniformInterval * pAggregateReactionProbabilityDensity;

        if(pAggregateReactionProbabilityDensity <= 0.0)
        {
            throw new IllegalArgumentException("invalid aggregate reaction probability density: " + pAggregateReactionProbabilityDensity);
        }

        int numReactions = pReactionProbabilityDensities.size();
        Reaction reaction = null;
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            ReactionProbabilityDensity reactionProbabilityDensity = (ReactionProbabilityDensity) pReactionProbabilityDensities.elementAt(reactionCtr);
            reaction = reactionProbabilityDensity.getReaction();
            cumulativeReactionProbabilityDensity += reactionProbabilityDensity.getProbabilityDensity();
            if(cumulativeReactionProbabilityDensity >= fractionOfAggregateReactionProbabilityDensity)
            {
                break;
            }
        }
        assert (null != reaction) : "null reaction found in chooseTypeOfNextReaction";
        return(reaction);
    }

                                                                               
    /*========================================*
     * protected methods
     *========================================*/

    /*========================================*
     * public methods
     *========================================*/

    /**
     * Sets the seed value for the random number generator to the
     * value specified by <code>pSeed</code>.  This method is optional.
     * If this method is not called, the random number generator is 
     * seeded with the clock time <code>System.currentTimeMillis</code>.
     * This method may be called more than once; each time it is called,
     * the random number generator is re-seeded with the value specified
     * by <code>pSeed</code>.
     *
     * @param pSeed the random number generator seed
     */
    public void setRandomNumberGeneratorSeed(long pSeed)
    {
        getRandomNumberGenerator().setSeed(pSeed);
    }


    /**
     * Returns the initial aggregate (overall) time scale for reactions
     * to occur, in the model specified by <code>pModel</code> and the
     * initial species populations specified by 
     * <code>pInitialSpeciesPopulations</code>.  The reaction probability
     * densities are evaluated at the clock time specified by 
     * <code>pStartTime</code>, which is necessary in case a custom reaction rate
     * law involves &quot;clock time&quot; as a parameter.  It is not mandatory
     * to call this method, before calling any of the <code>evolve()</code>
     * methods.  This method does not change the internal state of any of
     * the arguments.  The units of the time value returned are the same
     * as the units of the inverse of the reaction parameters specified
     * for each of the reactions with the 
     * {@link Reaction#setRate(double)} method.
     * <p />
     * Note that if you have defined a model that has no possible
     * reactions given the supplied initial conditions, this method
     * will return <code>Infinity</code> for the time scale.  You will
     * still be able to call <code>evolve()</code> on this (somewhat
     * pathological) model, but the first iteration will cause an exception
     * to be thrown.
     *
     * @param pModel the {@link Model} for which the reaction time scale is
     * to be computed, which includes the {@link Reaction} data structures
     * that constitute the model.
     *
     * @param pInitialSpeciesPopulations the {@link SpeciesPopulations} 
     * of the species
     * in the model, at the time the simulation starts.  These are the initial
     * data for the simulation.
     *
     * @param pStartTime the clock time at which the simulation is to start
     *
     * @return the initial aggregate (overall) time scale for reactions
     * to occur, in the model specified by <code>pModel</code> and the
     * initial species populations specified by 
     * <code>pInitialSpeciesPopulations</code>.
     */
    public double computeInitialAggregateTimeConstant(Model pModel, 
                                                      SpeciesPopulations pInitialSpeciesPopulations,
                                                      double pStartTime) throws IllegalArgumentException
    {
        Vector reactionProbabilityDensities = new Vector();
        pModel.initializeReactionProbabilityDensities(reactionProbabilityDensities);

        computeReactionProbabilityDensities(pModel,
                                            pInitialSpeciesPopulations,
                                            reactionProbabilityDensities,
                                            pStartTime);
        double aggregateReactionProbabilityDensity;

        aggregateReactionProbabilityDensity = computeAggregateReactionProbabilityDensity(reactionProbabilityDensities);
        assert (aggregateReactionProbabilityDensity > 0.0);

        return(1.0 / aggregateReactionProbabilityDensity);
    }


    /**
     * Perform an evolution with a specific number of iterations of the
     * Gillespie algorithm, specified by the integer <code>pNumIterations</code>
     * (which must be positive).  
     *
     * @param pModel the {@link Model} for which this simulation is to be
     * conducted, including the {@linkplain Reaction reactions} in the model.
     *
     * @param pInitialSpeciesPopulations the initial data for the simulation,
     * specifying initial populations for each {@link Species} in the simulation.
     * 
     * @param pStartTime the nonnegative time value at which the simulation starts.
     *
     * @param pNumIterations the integer number of iterations of the stochastic
     * algorithm that should be executed
     *
     * @param pFinalSpeciesPopulations the final population data for the chemical
     * {@link Species} in the simulation, after <code>pNumIterations</code> iterations.
     */
    public void evolve(Model pModel,
                       SpeciesPopulations pInitialSpeciesPopulations,
                       double pStartTime,
                       int pNumIterations,
                       SpeciesPopulations pFinalSpeciesPopulations) throws IllegalArgumentException, DataNotFoundException
    {
        pFinalSpeciesPopulations.copy(pInitialSpeciesPopulations);

        if(pNumIterations <= 0)
        {
            throw new IllegalArgumentException("number of iterations specified is illegal: " + pNumIterations);
        }

        if(debuggingIsEnabled())
        {
            Date curDateTime = new Date(System.currentTimeMillis());
            debugPrintln("simulation starting at time:      " + curDateTime, DebugOutputVerbosityLevel.LOW);
            debugPrintln("initial species populations:\n" + pInitialSpeciesPopulations, DebugOutputVerbosityLevel.MEDIUM);
        }

        boolean canHaveAnotherIteration = true;

        Vector reactionProbabilityDensities = new Vector();
        pModel.initializeReactionProbabilityDensities(reactionProbabilityDensities);

        MutableDouble time = new MutableDouble(pStartTime);

        int iterationCtr = 0;

        for(; iterationCtr < pNumIterations; ++iterationCtr)
        {

            if(debuggingIsEnabled())
            {
                debugPrintln("iteration number: " + iterationCtr++, DebugOutputVerbosityLevel.MEDIUM);
            }

            canHaveAnotherIteration = iterate(pModel,
                                              pFinalSpeciesPopulations,
                                              reactionProbabilityDensities,
                                              time);
            if(! canHaveAnotherIteration)
            {
                break;
            }
        }

        if(debuggingIsEnabled())
        {
            debugPrintln("final species populations:\n" + pFinalSpeciesPopulations, DebugOutputVerbosityLevel.MEDIUM);
            debugPrintln("number of iterations completed:   " + iterationCtr, DebugOutputVerbosityLevel.LOW);
            Date curDateTime = new Date(System.currentTimeMillis());
            debugPrintln("simulation ending at time:        " + curDateTime + "\n", DebugOutputVerbosityLevel.LOW);
        }
    }

   /**
     * This method calls <a href="#evolve"><code>evolve()</code></a> in order
     * to run a stochastic simulation.  The species populations are sampled
     * on even time intervals equal to the <code>pStopTime</code> variable
     * divided by the number of entries in the <code>pPopulationSamples</code>
     * array.  
     *
     * This method is the one that is used by the 
     * {@link isb.chem.sbw.SimulationService} class.
     *
     * @param pModel the {@link Model} for which this simulation is to be
     * conducted, including the {@linkplain Reaction reactions} in the model.
     *
     * @param pInitialSpeciesPopulations the initial data for the simulation,
     * specifying initial populations for each {@link Species} in the simulation.
     * 
     * @param pPopulationSamples an array of {@link SpeciesPopulations} data
     * structures into which the sampled species populations are stored.  This
     * array must have the same number of entries as the 
     * <code>pSampleTimes</code> array.  
     *
     * @param pStartTime the nonnegative time value at which the simulation starts.
     *
     * @param pStopTime The positive time value (must be greater than 0.0) at
     * which the simulation will stop, if it has not stopped previously due
     * to encountering a zero value for the aggregate reaction probability
     * density.  The units of this parameter are the same as the units of the
     * inverse of the reaction parameters specified for each of the reactions 
     * with the {@link Reaction#setRate(double)} method. 
     *  
     * @param pSimulationController a {@link SimulationController} object
     * containing a boolean flag (called &quot;stopped&quot;) whose purpose is 
     * to allow an external thread to halt or stop the simulation in progress
     *
     * @see #evolve(Model,SpeciesPopulations,double[],double,double,SpeciesPopulations[])
     */
    public void evolve(Model pModel, 
                       SpeciesPopulations pInitialSpeciesPopulations,
                       double pStartTime,
                       double pStopTime,
                       SpeciesPopulations []pPopulationSamples,
                       SimulationController pSimulationController) throws IllegalArgumentException, IllegalStateException, DataNotFoundException
    {
        int numSamples = pPopulationSamples.length;
        if(numSamples <= 0)
        {
            throw new IllegalArgumentException("invalid number of samples requested: " + numSamples);
        }
        if(pStartTime >= pStopTime)
        {
            throw new IllegalArgumentException("start time " + pStartTime + " exceeds or equals stop time " + pStopTime);
        }

        double []timeSamples = new double[numSamples];
        for(int ctr = 0; ctr < numSamples; ++ctr)
        {
            timeSamples[ctr] = pStartTime + ((pStopTime-pStartTime) * ((double) ctr)/((double) numSamples));
        }

        evolve(pModel,
               pInitialSpeciesPopulations,
               timeSamples,
               pStartTime,
               pStopTime,
               pPopulationSamples,
               pSimulationController);
    }

   /**
     * This method calls <a href="#evolve"><code>evolve()</code></a> in order
     * to run a stochastic simulation.  The species populations are sampled
     * on even time intervals equal to the <code>pStopTime</code> variable
     * divided by the number of entries in the <code>pPopulationSamples</code>
     * array.  
     *
     * @param pModel the {@link Model} for which this simulation is to be
     * conducted, including the {@linkplain Reaction reactions} in the model.
     *
     * @param pInitialSpeciesPopulations the initial data for the simulation,
     * specifying initial populations for each {@link Species} in the simulation.
     * 
     * @param pPopulationSamples an array of {@link SpeciesPopulations} data
     * structures into which the sampled species populations are stored.  This
     * array must have the same number of entries as the 
     * <code>pSampleTimes</code> array.  
     *
     * @param pStartTime the nonnegative time value at which the simulation starts.
     *
     * @param pStopTime The positive time value (must be greater than 0.0) at
     * which the simulation will stop, if it has not stopped previously due
     * to encountering a zero value for the aggregate reaction probability
     * density.  The units of this parameter are the same as the units of the
     * inverse of the reaction parameters specified for each of the reactions 
     * with the {@link Reaction#setRate(double)} method. 
     *  
     * @see GillespieSimulator#evolve(Model,SpeciesPopulations,double[],double,double,SpeciesPopulations[])
     */
    public void evolve(Model pModel, 
                       SpeciesPopulations pInitialSpeciesPopulations,
                       double pStartTime,
                       double pStopTime,
                       SpeciesPopulations []pPopulationSamples) throws IllegalArgumentException, IllegalStateException, DataNotFoundException
    {
        evolve(pModel,
               pInitialSpeciesPopulations,
               pStartTime,
               pStopTime,
               pPopulationSamples,
               null);
    }




    /**
     * Conducts a stochastic simulation of the specified {@link Model} with 
     * the specified {@linkplain SpeciesPopulations inital data}, for the
     * time range from 0 to <code>pStopTime</code>, in units of the inverse
     * of the reaction parameters specified for each of the reactions with the 
     * {@link Reaction#setRate(double)} method. The algorithm will 
     * conduct as many iterations as necessary until the elapsed (simulation)
     * time exceeds <code>pStopTime</code>, or until the species populations
     * get into a state whereby no further reactions can occur (whichever
     * comes first).  Samples the species populations for all species in the
     * model, at the time values specified by <code>pSampleTimes</code>.  At
     * least one sample time must be specified, or else an exception is thrown.
     * At each time specified in the <code>pSampleTimes</code> array, the
     * species populations at that moment are stored in the corresponding 
     * element of the <code>pPopulationSamples</code> array.  This method
     * will optionally print debugging information to standard output,
     * depending on whether the {@link #setDebugOutput(PrintWriter)} method has 
     * been called with a non-null argument (the default debug output stream
     * is set to <code>null</code>, which inhibits debug output printing).
     * 
     * This method only modifies the <code>pPopulationSamples</code> data
     * structures.  This method may be called more than once; each time
     * the method is called, a new simulation will be conducted.  Note that
     * the random number generator used for the simulation will have a different
     * ``starting'' value each time this method is invoked, unless the
     * {@link #setRandomNumberGeneratorSeed(long)} method is called (with the
     * same integer random number generator seed) before this method is called.
     *
     * @param pModel the {@link Model} for which this simulation is to be
     * conducted, including the {@linkplain Reaction reactions} in the model.
     *
     * @param pInitialSpeciesPopulations the initial data for the simulation,
     * specifying initial populations for each {@link Species} in the simulation.
     * 
     * @param pSampleTimes the array of time values at which the species
     * populations should be sampled and stored into <code>pPopulationSamples</code>.
     * The array must contain at least one time value, and the values in the
     * elements of the array must be monotonically increasing with the array
     * index.  All elements of the array must be nonnegative.  The units of
     * the time values are the same as for the <code>pStopTime</code> parameter.
     *
     * @param pPopulationSamples an array of {@link SpeciesPopulations} data
     * structures into which the sampled species populations are stored.  This
     * array must have the same number of entries as the 
     * <code>pSampleTimes</code> array.  
     *
     * @param pStartTime the positive time value at which the simulation starts
     *
     * @param pStopTime The positive time value (must be greater than <code>pStartTime</code>) at
     * which the simulation will stop, if it has not stopped previously due
     * to encountering a zero value for the aggregate reaction probability
     * density.  The units of this parameter are the same as the units of the
     * inverse of the reaction parameters specified for each of the reactions 
     * with the {@link Reaction#setRate(double)} method. 
     */

    public void evolve(Model pModel, 
                       SpeciesPopulations pInitialSpeciesPopulations,
                       double []pSampleTimes, 
                       double pStartTime,
                       double pStopTime,
                       SpeciesPopulations []pPopulationSamples) throws IllegalArgumentException, IllegalStateException, DataNotFoundException
    {
        evolve(pModel,
               pInitialSpeciesPopulations,
               pSampleTimes,
               pStartTime,
               pStopTime,
               pPopulationSamples,
               null);
    }

    /**
     * <a name="evolve"></a>
     * Conducts a stochastic simulation of the specified {@link Model} with 
     * the specified {@linkplain SpeciesPopulations inital data}, for the
     * time range from 0 to <code>pStopTime</code>, in units of the inverse
     * of the reaction parameters specified for each of the reactions with the 
     * {@link Reaction#setRate(double)} method. The algorithm will 
     * conduct as many iterations as necessary until the elapsed (simulation)
     * time exceeds <code>pStopTime</code>, or until the species populations
     * get into a state whereby no further reactions can occur (whichever
     * comes first).  Samples the species populations for all species in the
     * model, at the time values specified by <code>pSampleTimes</code>.  At
     * least one sample time must be specified, or else an exception is thrown.
     * At each time specified in the <code>pSampleTimes</code> array, the
     * species populations at that moment are stored in the corresponding 
     * element of the <code>pPopulationSamples</code> array.  This method
     * will optionally print debugging information to standard output,
     * depending on whether the {@link #setDebugOutput(PrintWriter)} method has 
     * been called with a non-null argument (the default debug output stream
     * is set to <code>null</code>, which inhibits debug output printing).
     * 
     * This method only modifies the <code>pPopulationSamples</code> data
     * structures.  This method may be called more than once; each time
     * the method is called, a new simulation will be conducted.  Note that
     * the random number generator used for the simulation will have a different
     * ``starting'' value each time this method is invoked, unless the
     * {@link #setRandomNumberGeneratorSeed(long)} method is called (with the
     * same integer random number generator seed) before this method is called.
     *
     * @param pModel the {@link Model} for which this simulation is to be
     * conducted, including the {@linkplain Reaction reactions} in the model.
     *
     * @param pInitialSpeciesPopulations the initial data for the simulation,
     * specifying initial populations for each {@link Species} in the simulation.
     * 
     * @param pSampleTimes the array of time values at which the species
     * populations should be sampled and stored into <code>pPopulationSamples</code>.
     * The array must contain at least one time value, and the values in the
     * elements of the array must be monotonically increasing with the array
     * index.  All elements of the array must be nonnegative.  The units of
     * the time values are the same as for the <code>pStopTime</code> parameter.
     *
     * @param pPopulationSamples an array of {@link SpeciesPopulations} data
     * structures into which the sampled species populations are stored.  This
     * array must have the same number of entries as the 
     * <code>pSampleTimes</code> array.  
     *
     * @param pStartTime the positive time value at which the simulation starts
     *
     * @param pStopTime The positive time value (must be greater than <code>pStartTime</code>) at
     * which the simulation will stop, if it has not stopped previously due
     * to encountering a zero value for the aggregate reaction probability
     * density.  The units of this parameter are the same as the units of the
     * inverse of the reaction parameters specified for each of the reactions 
     * with the {@link Reaction#setRate(double)} method. 
     *
     * @param pSimulationController a {@link SimulationController} object
     * containing a boolean flag (called &quot;stopped&quot;) whose purpose is 
     * to allow an external thread to halt or stop the simulation in progress
     */
    public void evolve(Model pModel, 
                        SpeciesPopulations pInitialSpeciesPopulations,
                        double []pSampleTimes, 
                        double pStartTime,
                        double pStopTime,
                        SpeciesPopulations []pPopulationSamples,
                        SimulationController pSimulationController) throws IllegalArgumentException, IllegalStateException, DataNotFoundException
    {
        validateSimulationParameters(pModel, 
                                     pInitialSpeciesPopulations, 
                                     pSampleTimes, 
                                     pStartTime,
                                     pStopTime,
                                     pPopulationSamples);
        
        // create container to hold species populations
        SpeciesPopulations speciesPopulations = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        
        if(debuggingIsEnabled())
        {
            Date curDateTime = new Date(System.currentTimeMillis());
            debugPrintln("simulation starting at time:      " + curDateTime, DebugOutputVerbosityLevel.LOW);
            debugPrintln("initial species populations:\n" + speciesPopulations, DebugOutputVerbosityLevel.MEDIUM);
        }

        // create container to hold reaction probability densities
        Vector reactionProbabilityDensities = new Vector();
        pModel.initializeReactionProbabilityDensities(reactionProbabilityDensities);
            
        MutableDouble time = new MutableDouble(pStartTime);

        int nextSampleCtr = 0;
        int maxSampleCtr = pSampleTimes.length;
        double nextSampleTime = pSampleTimes[nextSampleCtr];

        int iterationCtr = 0;

        boolean canHaveAnotherIteration = true;
        boolean firstIteration = true;
        double curTime = 0.0;

        SpeciesPopulations lastSpeciesPopulations = (SpeciesPopulations) speciesPopulations.clone();

        while(true)
        {
            if(! firstIteration)
            {
                if(! canHaveAnotherIteration)
                {
                    time.setValue(pStopTime);
                    curTime = pStopTime;
                }

                if(nextSampleTime < curTime)
                {
                    nextSampleCtr = savePopulationData(nextSampleCtr,
                                                       maxSampleCtr,
                                                       curTime,
                                                       pPopulationSamples,
                                                       lastSpeciesPopulations, 
                                                       pSampleTimes,
                                                       pSimulationController);
                    if(SIMULATION_CANCELLED == nextSampleCtr)
                    {
                        // simulation has been cancelled; just return immediately
                        return;
                    }            
                }

                if(! canHaveAnotherIteration)
                {
                    break;
                }

                if(nextSampleCtr < maxSampleCtr)
                {
                    nextSampleTime = pSampleTimes[nextSampleCtr];
                }
            }
            else
            {
                firstIteration = false;
            }

            if(curTime >= pStopTime)
            {
                break;
            }

            iterationCtr++;

            if(debuggingIsEnabled())
            {
                debugPrintln("iteration number: " + iterationCtr, DebugOutputVerbosityLevel.MEDIUM);
            }

            lastSpeciesPopulations.copy(speciesPopulations);

            canHaveAnotherIteration = iterate(pModel,
                                              speciesPopulations,
                                              reactionProbabilityDensities,
                                              time);

            curTime = time.getValue();
        }

        if(debuggingIsEnabled())
        {
            debugPrintln("final species populations:\n" + speciesPopulations, DebugOutputVerbosityLevel.MEDIUM);
            debugPrintln("number of iterations completed:   " + iterationCtr, DebugOutputVerbosityLevel.LOW);
            Date curDateTime = new Date(System.currentTimeMillis());
            debugPrintln("simulation ending at time:        " + curDateTime + "\n", DebugOutputVerbosityLevel.LOW);
        }
    }

    private int savePopulationData(int pNextSampleCtr, 
                                    int pMaxSampleCtr, 
                                    double pCurTime, 
                                    SpeciesPopulations []pPopulationSamples,
                                    SpeciesPopulations pCurrentSpeciesPopulations,
                                    double []pSampleTimes,
                                    SimulationController pSimulationController)
    {
        double nextSampleTime = 0.0;

        while(pNextSampleCtr < pMaxSampleCtr &&
              (nextSampleTime = pSampleTimes[pNextSampleCtr]) < pCurTime)
        {
            pPopulationSamples[pNextSampleCtr] = (SpeciesPopulations) pCurrentSpeciesPopulations.clone();
            ++pNextSampleCtr;
            if(null != pSimulationController)
            {
                if(pSimulationController.checkIfDone())
                {
                    return(SIMULATION_CANCELLED);
                }
            }
        }

        return(pNextSampleCtr);
    }
}
