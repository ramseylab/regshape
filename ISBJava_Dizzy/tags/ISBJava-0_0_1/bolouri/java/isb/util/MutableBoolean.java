package isb.util;

/**
 * A container class for a <code>boolean</code> native data type.
 * This class allows you to alter the <code>boolean</code> variable
 * that it contains; it is a fully mutable object.  The purpose
 * of this class is to provide a mechanism to use <code>boolean</code>
 * values as values of a <code>HashMap</code>, while allowing those
 * values to be mutable as well; this cannot be done with the standard
 * Java class <code>Boolean</code>, which is immutable.
 *
 * @see MutableLong
 * @see MutableInteger
 * @see MutableDouble
 *
 * @author Stephen Ramsey
 */
public class MutableBoolean
{
    private boolean mBoolean;

    private boolean getBoolean()
    {
        return(mBoolean);
    }

    private void setBoolean(boolean pBoolean)
    {
        mBoolean = pBoolean;
    }

    public MutableBoolean(boolean pBoolean)
    {
        setBoolean(pBoolean);
    }
    
    public boolean getValue()
    {
        return(getBoolean());
    }

    public void setValue(boolean pBoolean)
    {
        setBoolean(pBoolean);
    }

    public Object clone()
    {
        MutableBoolean md = new MutableBoolean(getValue());
        return(md);
    }
}
