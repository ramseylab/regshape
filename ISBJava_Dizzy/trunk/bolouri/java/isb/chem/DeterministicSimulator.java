package isb.chem;

import java.util.*;
import java.io.PrintWriter;
import isb.util.*;

/**
 * Conducts a deterministic simulation of the reaction
 * kinetics of a system of interacting chemical species.
 * Uses a 5th order Runge-Kutta algorithm with adaptive
 * step-size control.
 *
 * @see Reaction
 * @see Model
 * @see SpeciesPopulations
 * @see Species
 * 
 * @author Stephen Ramsey
 */

public class DeterministicSimulator implements ISimulator, IAliasableClass
{
    /*========================================*
     * constants
     *========================================*/
    public static final String CLASS_ALIAS = "ODE";
    private static final int SIMULATION_CANCELLED = -1;
    private static final double PGROW = -0.20;
    private static final double PSHRINK = -0.25;
    private static final double SAFETY = 0.9;
    private static final double ERRCON = Math.pow(4.0/SAFETY, 1.0/PGROW);
    private static final double FCORR = 1.0/15.0;
    private static final double MINSCALE = 1.0;
    private static final double DEFAULT_ERROR_TOLERANCE = 1.0e-2;
    private static final double DEFAULT_GROW_STEPSIZE_MULTIPLIER = 1.1;

    /*========================================*
     * member data
     *========================================*/
    private PrintWriter mDebugOutput;
    private DebugOutputVerbosityLevel mDebugLevel;
    private double mErrorTolerance;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    public void setErrorTolerance(double pErrorTolerance)
    {
        mErrorTolerance = pErrorTolerance;
    }

    public double getErrorTolerance()
    {
        return(mErrorTolerance);
    }

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


    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeDebug()
    {
        setDebugOutput(null);
        setDebugLevel(DebugOutputVerbosityLevel.NONE);
    }


    private void initializeErrorTolerance()
    {
        setErrorTolerance(DEFAULT_ERROR_TOLERANCE);
    }

    private void initialize()
    {
        initializeDebug();
        initializeErrorTolerance();
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
    public DeterministicSimulator()
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

    private void zeroFloatingSpeciesPopulations(Vector pFloatingSpecies,
                                                SpeciesPopulations pSpeciesPopulations) throws IllegalArgumentException
    {
        int numSpecies = pFloatingSpecies.size();
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            pSpeciesPopulations.setSpeciesPopulation(speciesName, 0.0);
        }
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

    private Vector mFloatingSpeciesVec;
    private SpeciesPopulations k1;
    private SpeciesPopulations k2;
    private SpeciesPopulations k3;
    private SpeciesPopulations k4;
    private SpeciesPopulations q1;
    private SpeciesPopulations q2;
    private SpeciesPopulations q3;
    private SpeciesPopulations ymid;
    private SpeciesPopulations ymid2;
    private SpeciesPopulations yfull;
    private SpeciesPopulations yscal;
    private SpeciesPopulations ytemp;

    private void initRK(Model pModel, SpeciesPopulations pInitialSpeciesPopulations)
    {
        mFloatingSpeciesVec = new Vector();
        Set speciesSet = pModel.getSpeciesSet();
        Iterator speciesIter = speciesSet.iterator();
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            if(species.getFloating())
            {
                mFloatingSpeciesVec.add(species.getName());
            }
        }

        k1 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        k2 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        k3 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        k4 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        q1 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        q2 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        q3 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        ymid = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        ymid2 = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        yfull = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        yscal = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
        ytemp = (SpeciesPopulations) pInitialSpeciesPopulations.clone();
    }

    

    // modifies first argument
    private void vectorAddFloatingSpecies(Vector pFloatingSpecies, 
                                          SpeciesPopulations operand, 
                                          SpeciesPopulations addToOperand) throws DataNotFoundException
    {
        int numSpecies = pFloatingSpecies.size();
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            double operandValue = operand.getSpeciesPopulation(speciesName);
            double addToOperandValue = addToOperand.getSpeciesPopulation(speciesName);
            operand.setSpeciesPopulation(speciesName, operandValue + addToOperandValue);
        }
    }

    // modifies first argument
    private void scalarMultFloatingSpecies(Vector pFloatingSpecies, 
                                           SpeciesPopulations operand, 
                                           double multiplier) throws DataNotFoundException
    {
        int numSpecies = pFloatingSpecies.size();
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            double operandValue = multiplier * operand.getSpeciesPopulation(speciesName);
            operand.setSpeciesPopulation(speciesName, operandValue);
        }
    }

    // modifies first argument
    private void absFloatingSpecies(Vector pFloatingSpecies, 
                                    SpeciesPopulations operand) throws DataNotFoundException
    {
        int numSpecies = pFloatingSpecies.size();
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            double operandValue = Math.abs(operand.getSpeciesPopulation(speciesName));
            operand.setSpeciesPopulation(speciesName, operandValue);
        }
    }

    private void printFloatingSpecies(String pLabel,
                                      Vector pFloatingSpecies, 
                                    SpeciesPopulations operand) throws DataNotFoundException
    {
        int numSpecies = pFloatingSpecies.size();
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            double operandValue = operand.getSpeciesPopulation(speciesName);
//            System.out.println(pLabel + ";  species: " + speciesName + "; value: " + operandValue);
        }        
    }


    // modifies first argument
    private void scalarAddFloatingSpecies(Vector pFloatingSpecies, 
                                           SpeciesPopulations operand, 
                                           double adder) throws DataNotFoundException
    {
        int numSpecies = pFloatingSpecies.size();
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            double operandValue = adder + operand.getSpeciesPopulation(speciesName);
            operand.setSpeciesPopulation(speciesName, operandValue);
        }
    }


    private void copyFloatingSpeciesValues(Vector pFloatingSpecies,
                                           SpeciesPopulations operand, 
                                           SpeciesPopulations toCopy) throws DataNotFoundException
    {
        int numSpecies = pFloatingSpecies.size();
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            double copyValue = toCopy.getSpeciesPopulation(speciesName);
            operand.setSpeciesPopulation(speciesName, copyValue);
        }

    }

    private double computeError(SpeciesPopulations y1, 
                                SpeciesPopulations y2, 
                                SpeciesPopulations yscal, 
                                Vector pFloatingSpecies) throws DataNotFoundException
    {
        int numSpecies = pFloatingSpecies.size();
        double maxErr = 0.0;
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = (String) pFloatingSpecies.elementAt(speciesCtr);
            double y1val = y1.getSpeciesPopulation(speciesName);
            double y2val = y2.getSpeciesPopulation(speciesName);
            double yscalval = yscal.getSpeciesPopulation(speciesName);
            if(yscalval <= 0.0)
            {
                throw new IllegalArgumentException("invalid yscal value for species " + speciesName + ": " + yscalval);
            }
            double err = Math.abs(y2val - y1val)/yscalval;
            if(err > maxErr)
            {
                maxErr = err;
            }
//            System.out.println("species: " + speciesName + "; y1val: " + y1val + "; y2val: " + y2val + "; yscalval: " + yscalval + "; err: " + err);
        }

        
        return(maxErr);
    }


    private void computeDerivative(Vector pFloatingSpecies,
                                   Model pModel,
                                   SpeciesPopulations pSpeciesPopulations, 
                                   double pTime,
                                   Vector pReactionRates,
                                   SpeciesPopulations pDerivSpeciesPopulations) throws DataNotFoundException
    {
        computeReactionProbabilityDensities(pModel,
                                            pSpeciesPopulations,
                                            pReactionRates,
                                            pTime);
        
        zeroFloatingSpeciesPopulations(pFloatingSpecies, pDerivSpeciesPopulations);

        int numReactions = pReactionRates.size();
        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            ReactionProbabilityDensity reactionProbabilityDensity = (ReactionProbabilityDensity) pReactionRates.elementAt(reactionCtr);
            Reaction reaction = reactionProbabilityDensity.getReaction();

            double reactionRate = reactionProbabilityDensity.getProbabilityDensity();

            reaction.adjustSpeciesPopulationForReaction(pDerivSpeciesPopulations,
                                                        reactionRate);
        }

//        printFloatingSpecies("SpeciesPops", pFloatingSpecies, pSpeciesPopulations);
//        printFloatingSpecies("Derivatives", pFloatingSpecies, pDerivSpeciesPopulations);
    }

    private void rk4step(Vector pFloatingSpecies,
                         Model pModel,
                         SpeciesPopulations pSpeciesPopulations, 
                         double pTime,
                         Vector pReactionRates,
                         double pStepSize,
                         SpeciesPopulations pNextSpeciesPopulations) throws DataNotFoundException
    {
        computeDerivative(pFloatingSpecies, 
                          pModel,
                          pSpeciesPopulations,
                          pTime,
                          pReactionRates,
                          k1);
        scalarMultFloatingSpecies(pFloatingSpecies, k1, pStepSize);

        copyFloatingSpeciesValues(pFloatingSpecies, q1, k1);
        scalarMultFloatingSpecies(pFloatingSpecies, q1, 0.5);
        vectorAddFloatingSpecies(pFloatingSpecies, q1, pSpeciesPopulations);

        double timePlusHalfStep = pTime + (0.5 * pStepSize);

        computeDerivative(pFloatingSpecies,
                          pModel,
                          q1,
                          timePlusHalfStep,
                          pReactionRates,
                          k2);
        scalarMultFloatingSpecies(pFloatingSpecies, k2, pStepSize);

        copyFloatingSpeciesValues(pFloatingSpecies, q2, k2);
        scalarMultFloatingSpecies(pFloatingSpecies, q2, 0.5);
        vectorAddFloatingSpecies(pFloatingSpecies, q2, pSpeciesPopulations);

        computeDerivative(pFloatingSpecies,
                          pModel,
                          q2,
                          timePlusHalfStep,
                          pReactionRates,
                          k3);
        scalarMultFloatingSpecies(pFloatingSpecies, k3, pStepSize);        
        
        copyFloatingSpeciesValues(pFloatingSpecies, q3, k3);
        vectorAddFloatingSpecies(pFloatingSpecies, q3, pSpeciesPopulations);

        double timePlusFullStep = pTime + pStepSize;

        computeDerivative(pFloatingSpecies,
                          pModel,
                          q3,
                          timePlusFullStep,
                          pReactionRates,
                          k4);

        scalarMultFloatingSpecies(pFloatingSpecies, k4, pStepSize);                

        copyFloatingSpeciesValues(pFloatingSpecies, pNextSpeciesPopulations, pSpeciesPopulations);
        scalarMultFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, 6.0);
        vectorAddFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, k1);
        vectorAddFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, k4);
        vectorAddFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, k2);
        vectorAddFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, k2);
        vectorAddFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, k3);
        vectorAddFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, k3);
        scalarMultFloatingSpecies(pFloatingSpecies, pNextSpeciesPopulations, 1.0/6.0);
    }


    private int adaptiveStep(SpeciesPopulations pSpeciesPopulations, 
                                 MutableDouble pTime,
                                 Vector pFloatingSpecies,
                                 Vector pReactionProbabilityDensities,
                                 MutableDouble pStepSize,
                                 Model pModel,
                                 double pErrorTolerance,
                                 SpeciesPopulations pNextSpeciesPopulations,
                             SpeciesPopulations pSpeciesPopulationScales) throws IllegalArgumentException, DataNotFoundException, SimulationFailedException
    {
        double stepSize = pStepSize.getValue();
        double maxErr = 0.0;
        int numTries = 0;

        assert (pErrorTolerance > 0.0) : "invalid error tolerance";

        do
        {
            double halfStepSize = 0.5 * stepSize;
            double time = pTime.getValue();
            ++numTries;
            rk4step(pFloatingSpecies,
                    pModel, 
                    pSpeciesPopulations,
                    time,
                    pReactionProbabilityDensities,
                    halfStepSize,
                    ymid);
            
            double timePlusHalfStep = time + halfStepSize;

            rk4step(pFloatingSpecies,
                    pModel, 
                    ymid,
                    timePlusHalfStep,
                    pReactionProbabilityDensities,
                    halfStepSize,
                    ymid2);

            double timePlusFullStep = time + stepSize;

            rk4step(pFloatingSpecies,
                    pModel,
                    pSpeciesPopulations,
                    timePlusFullStep,
                    pReactionProbabilityDensities,
                    stepSize,
                    yfull);

            maxErr = computeError(ymid2, yfull, pSpeciesPopulationScales, pFloatingSpecies);
            
//            System.out.println("at time: " + time + "; using stepsize: " + stepSize + "; maxerr: " + maxErr);

            double errRatio = maxErr / pErrorTolerance;

            if(errRatio <= 1.0)
            {
                // succeeded; record this time jump
                pTime.setValue(time + stepSize);

                // estimate correct step size for next step
                double nextStepSize = 0.0;
                if(errRatio > ERRCON)
                {
                    nextStepSize = SAFETY * stepSize * Math.exp(PGROW * Math.log(errRatio));
                }
                else
                {
                    // our step size is too small; try increasing it a bit
                    nextStepSize = DEFAULT_GROW_STEPSIZE_MULTIPLIER * stepSize;
                }
                stepSize = nextStepSize;
                pStepSize.setValue(nextStepSize);


                // clean up 5th order truncation error
                scalarMultFloatingSpecies(pFloatingSpecies, yfull, - 1.0);
                vectorAddFloatingSpecies(pFloatingSpecies, yfull, ymid2);
                scalarMultFloatingSpecies(pFloatingSpecies, yfull, FCORR);
                vectorAddFloatingSpecies(pFloatingSpecies, ymid2, yfull);

                copyFloatingSpeciesValues(pFloatingSpecies, pNextSpeciesPopulations, ymid2);
            }
            else
            {
                // failed; make setp size smaller
                stepSize *= SAFETY * Math.exp(PSHRINK * Math.log(errRatio));
                pStepSize.setValue(stepSize);
                if(time + stepSize == time)
                {
                    throw new SimulationFailedException("step size has shrunk to zero; probably this model contains too much stiffness for this ODE simulator to handle; try exporting to SBML and using a more sophisticated ODE solver");
                }
            }
        }
        while(maxErr > pErrorTolerance);

        return(numTries);
    }

    private void computeScale(Vector pFloatingSpecies,
                              Model pModel,
                              SpeciesPopulations pSpeciesPopulations,
                              double pTime,
                              double pStepSize,
                              Vector pReactionProbabilityDensities,
                              SpeciesPopulations pSpeciesPopulationScales) throws DataNotFoundException
    {
        computeDerivative(pFloatingSpecies,
                          pModel,
                          pSpeciesPopulations,
                          pTime,
                          pReactionProbabilityDensities,
                          pSpeciesPopulationScales);

        copyFloatingSpeciesValues(pFloatingSpecies, ytemp, pSpeciesPopulations);
        absFloatingSpecies(pFloatingSpecies, ytemp);
        scalarMultFloatingSpecies(pFloatingSpecies, pSpeciesPopulationScales, pStepSize);
        absFloatingSpecies(pFloatingSpecies, pSpeciesPopulationScales);
        scalarAddFloatingSpecies(pFloatingSpecies, pSpeciesPopulationScales, MINSCALE);
        vectorAddFloatingSpecies(pFloatingSpecies, pSpeciesPopulationScales, ytemp);
    }

    private int iterate(Model pModel,
                         Vector pFloatingSpecies,
                         SpeciesPopulations pSpeciesPopulations,
                         MutableDouble pStepSize,
                         double pStopTime,
                         double pErrorTolerance,
                         Vector pReactionProbabilityDensities,
                         MutableDouble pTime) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, SimulationFailedException
    {
        // compute derivatives
        double time = pTime.getValue();
        double stepSize = pStepSize.getValue();

        computeScale(pFloatingSpecies,
                     pModel,
                     pSpeciesPopulations,
                     time,
                     stepSize,
                     pReactionProbabilityDensities,
                     yscal);
                     
        // check to see if suggested time step can overshoot
        if(time + stepSize > pStopTime)
        {
            stepSize = pStopTime - time;
            pStepSize.setValue(stepSize);
        }

        int numTries = adaptiveStep(pSpeciesPopulations,
                                       pTime,
                                       pFloatingSpecies,
                                       pReactionProbabilityDensities,
                                       pStepSize,
                                       pModel,
                                       pErrorTolerance,
                                       ytemp,
                                       yscal);

        if(debuggingIsEnabled())
        {
            debugPrintln("time: " + pTime + "; stepsize: " + pStepSize + "; numtries: " + numTries, 
                         DebugOutputVerbosityLevel.HIGH);
        }

        copyFloatingSpeciesValues(pFloatingSpecies, pSpeciesPopulations, ytemp);

        return(numTries);
    }






                                                                               
    /*========================================*
     * protected methods
     *========================================*/

    /*========================================*
     * public methods
     *========================================*/


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
                       SimulationController pSimulationController) throws IllegalArgumentException, IllegalStateException, DataNotFoundException, SimulationFailedException
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
                       SpeciesPopulations []pPopulationSamples) throws IllegalArgumentException, IllegalStateException, DataNotFoundException, SimulationFailedException
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
                       SpeciesPopulations []pPopulationSamples) throws IllegalArgumentException, IllegalStateException, DataNotFoundException, SimulationFailedException
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
    public synchronized void evolve(Model pModel, 
                        SpeciesPopulations pInitialSpeciesPopulations,
                        double []pSampleTimes, 
                        double pStartTime,
                        double pStopTime,
                        SpeciesPopulations []pPopulationSamples,
                        SimulationController pSimulationController) throws IllegalArgumentException, IllegalStateException, DataNotFoundException, SimulationFailedException
    {
        validateSimulationParameters(pModel, 
                                     pInitialSpeciesPopulations, 
                                     pSampleTimes, 
                                     pStartTime,
                                     pStopTime,
                                     pPopulationSamples);

        initRK(pModel, pInitialSpeciesPopulations);

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

        boolean firstIteration = true;
        double curTime = 0.0;

        double minNumSubIntervals = 10.0 * ((double) pPopulationSamples.length) + 1000.0;
        assert (minNumSubIntervals > 0) : "invalid number of subintervals";

        MutableDouble stepSize = new MutableDouble((pStopTime - pStartTime)/minNumSubIntervals);

        int totalTries = 0;
        int totalSteps = 0;

        while(true)
        {
            if(! firstIteration)
            {
                if(nextSampleTime < curTime)
                {
                    nextSampleCtr = savePopulationData(nextSampleCtr,
                                                       maxSampleCtr,
                                                       curTime,
                                                       pPopulationSamples,
                                                       speciesPopulations, 
                                                       pSampleTimes,
                                                       pSimulationController);
                    if(SIMULATION_CANCELLED == nextSampleCtr)
                    {
                        // simulation has been cancelled; just return immediately
                        return;
                    }            
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

            int numTries = iterate(pModel,
                                   mFloatingSpeciesVec,
                                   speciesPopulations,
                                   stepSize,
                                   pStopTime,
                                   getErrorTolerance(),
                                   reactionProbabilityDensities,
                                   time);
            totalTries += numTries;
            totalSteps++;

            curTime = time.getValue();
        }

        if(debuggingIsEnabled())
        {
            debugPrintln("final species populations:\n" + speciesPopulations, DebugOutputVerbosityLevel.MEDIUM);
            debugPrintln("number of iterations completed:   " + iterationCtr, DebugOutputVerbosityLevel.LOW);
            Date curDateTime = new Date(System.currentTimeMillis());
            debugPrintln("simulation ending at time:        " + curDateTime + "\n", DebugOutputVerbosityLevel.LOW);
            debugPrintln("number of tries:           " + totalTries + "\n", DebugOutputVerbosityLevel.LOW);
            debugPrintln("number of steps:           " + totalSteps + "\n", DebugOutputVerbosityLevel.LOW);
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
