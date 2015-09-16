package isb.chem;

/**
 * Represents a numeric exception that caused a simulation
 * to fail.  This exception is thrown by the 
 * <code>evolve()</code> methods of the
 * {@link ISimulator} interface.
 *
 * @see ISimulator
 *
 * @author Stephen Ramsey
 */
public class SimulationFailedException extends Exception
{
    public SimulationFailedException(String pMessage)
    {
        super(pMessage);
    }

    public SimulationFailedException(String pMessage, Throwable pCause)
    {
        super(pMessage, pCause);
    }
}
