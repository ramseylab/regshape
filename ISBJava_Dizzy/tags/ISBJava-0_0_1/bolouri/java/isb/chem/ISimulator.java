package isb.chem;
import java.io.PrintWriter;
import isb.util.*;

/**
 * A class that can simulate the dynamics of a set of coupled
 * chemical {@linkplain Reaction reactions}, contained in a {@link Model}.
 * The {@link GillespieSimulator} simulator is a sample implementation of this
 * interface; it uses a stochastic algorithm to solve for the dynamics 
 * of the chemical reactions in the model.
 *
 * @see GillespieSimulator
 *
 * @author Stephen Ramsey
 */
public interface ISimulator
{
   /**
     * Returns the <code>PrintWriter</code> output for debug printing.
     * This is set to <code>null</code> by default, which inhibits
     * any printing of debugging information. 
     *
     * @return the <code>PrintWriter</code> output for debug printing.
     */
    public PrintWriter getDebugOutput();

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
    public void setDebugOutput(PrintWriter pDebugOutput);

    /**
     * Sets the integer debug level; default is zero (no debugging).
     * Other possible values are 1 (minimum verbosity) through 2 (maximum verbosity).
     */
    public void setDebugLevel(DebugOutputVerbosityLevel pDebugLevel) throws IllegalArgumentException;

    /**
     * Returns the integer debug level.
     *
     * @return the integer debug level.
     */
    public DebugOutputVerbosityLevel getDebugLevel();

    /**
     * Conducts a simulation of the chemical {@linkplain Reaction reactions} 
     * in the specified {@link Model} with 
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
     * the method is called, a new simulation will be conducted.  
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
     * to allow an external thread to halt or stop the simulation in progress.
     * To disable use of the simulation controller, a <code>null</code> may be
     * passed in place of this argument.
     */
    public void evolve(Model pModel, 
                       SpeciesPopulations pInitialSpeciesPopulations,
                       double []pSampleTimes, 
                       double pStartTime,
                       double pStopTime,
                       SpeciesPopulations []pPopulationSamples,
                       SimulationController pSimulationController) throws IllegalArgumentException, IllegalStateException, DataNotFoundException, SimulationFailedException;

   /**
     * This method calls {@link #evolve(Model,SpeciesPopulations,double[],double,double,SpeciesPopulations[],SimulationController)}
     * in order to run a simulation.  The species populations are sampled
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
     * @param pSimulationController a {@link SimulationController} object
     * containing a boolean flag (called &quot;stopped&quot;) whose purpose is 
     * to allow an external thread to halt or stop the simulation in progress
     *
     * @see #evolve(Model,SpeciesPopulations,double[],double,double,SpeciesPopulations[],SimulationController)
     */
    public void evolve(Model pModel, 
                       SpeciesPopulations pInitialSpeciesPopulations,
                       double pStartTime,
                       double pStopTime,
                       SpeciesPopulations []pPopulationSamples,
                       SimulationController pSimulationController) throws IllegalArgumentException, IllegalStateException, DataNotFoundException, SimulationFailedException;
}

