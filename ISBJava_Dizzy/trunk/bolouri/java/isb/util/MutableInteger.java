package isb.util;

/**
 * A container class for a <code>int</code> native data type.
 * This class allows you to alter the <code>int</code> variable
 * that it contains; it is a fully mutable object.  The purpose
 * of this class is to provide a mechanism to use <code>int</code>
 * values as values of a <code>HashMap</code>, while allowing those
 * values to be mutable as well; this cannot be done with the standard
 * Java class <code>Integer</code>, which is immutable.
 *
 * @see MutableDouble
 * @see MutableLong
 *
 * @author Stephen Ramsey
 */
public class MutableInteger
{
    int mInteger;

    private int getInteger()
    {
        return(mInteger);
    }

    private void setInteger(int pInteger)
    {
        mInteger = pInteger;
    }

    public MutableInteger(int pInteger)
    {
        setInteger(pInteger);
    }
    
    public int getValue()
    {
        return(getInteger());
    }

    public void setValue(int pInteger)
    {
        setInteger(pInteger);
    }

    public String toString()
    {
        return(String.valueOf(mInteger));
    }

    public Object clone()
    {
        MutableInteger md = new MutableInteger(getValue());
        return(md);
    }
}
