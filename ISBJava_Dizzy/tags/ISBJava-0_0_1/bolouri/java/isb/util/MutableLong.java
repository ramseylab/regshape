package isb.util;

/**
 * A container class for a <code>long</code> native data type.
 * This class allows you to alter the <code>long</code> variable
 * that it contains; it is a fully mutable object.  The purpose
 * of this class is to provide a mechanism to use <code>long</code>
 * values as values of a <code>HashMap</code>, while allowing those
 * values to be mutable as well; this cannot be done with the standard
 * Java class <code>Long</code>, which is immutable.
 *
 * @see MutableDouble
 * @see MutableInteger
 *
 * @author Stephen Ramsey
 */
public class MutableLong
{
    private long mLong;

    private long getLong()
    {
        return(mLong);
    }

    private void setLong(long pLong)
    {
        mLong = pLong;
    }

    public MutableLong(long pLong)
    {
        setLong(pLong);
    }
    
    public long getValue()
    {
        return(getLong());
    }

    public void setValue(long pLong)
    {
        setLong(pLong);
    }

    public String toString()
    {
        return(String.valueOf(mLong));
    }

    public Object clone()
    {
        MutableLong md = new MutableLong(getValue());
        return(md);
    }
}
