package isb.util;

/**
 * A container class for a <code>double</code> native data type.
 * This class allows you to alter the <code>double</code> variable
 * that it contains; it is a fully mutable object.  The purpose
 * of this class is to provide a mechanism to use <code>double</code>
 * values as values of a <code>HashMap</code>, while allowing those
 * values to be mutable as well; this cannot be done with the standard
 * Java class <code>Double</code>, which is immutable.
 *
 * @see MutableLong
 * @see MutableInteger
 *
 * @author Stephen Ramsey
 */
public class MutableDouble
{
    double mDouble;

    private double getDouble()
    {
        return(mDouble);
    }

    private void setDouble(double pDouble)
    {
        mDouble = pDouble;
    }

    public MutableDouble(double pDouble)
    {
        setDouble(pDouble);
    }
    
    public double getValue()
    {
        return(getDouble());
    }

    public void setValue(double pDouble)
    {
        setDouble(pDouble);
    }

    public Object clone()
    {
        MutableDouble md = new MutableDouble(getValue());
        return(md);
    }
}
