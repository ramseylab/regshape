package isb.util;

/**
 * Represents an error condition in which the caller has requested
 * a particular data element, but the data element was not found.
 *
 * @author Stephen Ramsey 
 */
public class DataNotFoundException extends Exception
{
    public DataNotFoundException(String pMessage)
    {
        super(pMessage);
    }

    public DataNotFoundException(String pMessage, Throwable pCause)
    {
        super(pMessage, pCause);
    }
}
