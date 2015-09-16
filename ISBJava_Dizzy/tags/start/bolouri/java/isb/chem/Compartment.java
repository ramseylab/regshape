package isb.chem;

/**
 * Represents a physical volume that can contain one or 
 * more chemical species.  This compartment class has 
 * been designed for consistency with the notion of a 
 * Compartment as defined in the
 * <a href="package-summary.html#sbml">SBML level 1 specification</a>.  
 * A compartment is a named
 * entity that has a floating-point volume associated with
 * it.  The volume defaults to &quot;1.0&quot;, in the default
 * units of volume (liters).  A {@link Species} object is always associated 
 * with a particular compartment.  A {@link Model} must always
 * contain at least one compartment.
 *
 * @see Species
 * @see Model
 *
 * @author Stephen Ramsey
 */
public class Compartment implements Comparable, Cloneable
{
    /*========================================*
     * constants
     *========================================*/
    /**
     * Defines the default volume of a compartment, in liters.
     * This is set to 1.0 liters, in accordance with the SBML specification.
     */
    public static final double DEFAULT_VOLUME_LITERS = 1.0;

    /*========================================*
     * member data
     *========================================*/
    private String mName;
    private double mVolumeLiters;  // volume of the compartment, in liters

    /*========================================*
     * accessor/mutator methods
     *========================================*/

    /**
     * returns the volume of this compartment, in liters
     * 
     * @return the volume of this compartment, in liters
     */

    public double getVolumeLiters()
    {
        return(mVolumeLiters);
    }

    /**
     * Sets the volume of this compartment to 
     * the double-precision floating-point value
     * <code>pVoulme</code>
     *
     * @param pVolumeLiters the volume, which must be
     * a positive number (in liters).
     */
    public void setVolumeLiters(double pVolumeLiters) throws IllegalArgumentException
    {
        if(pVolumeLiters <= 0.0)
        {
            throw new IllegalArgumentException("invalid volume parameter passed: " + pVolumeLiters);
        }
        mVolumeLiters = pVolumeLiters;
    }

    /**
     * Returns the name of this compartment object.
     *
     * @return the name of this compartment object.
     */
    public String getName()
    {
        return(mName);
    }

    /**
     * Assigns the <code>String</code> <code>pName</code> as the
     * &quot;name&quot; of this compartment.
     *
     * @param pName the <code>String</code> containing the name
     * (or new name) of this compartment.  This method may be
     * called more than once.  If you define a compartment
     * with name &quot;s1&quot; using the 
     * {@linkplain #Compartment(String) constructor},
     * and then call <code>setName("s2")</code>, the <code>Compartment</code>
     * object will be re-named to the string &quot;s2&quot;.  This argument
     * cannot be null.
     */
    private void setName(String pName) throws IllegalArgumentException
    {
        if(null == pName || pName.length() == 0)
        {
            throw new IllegalArgumentException("attempt to define empty compartment name");
        }
        mName = pName;
    }

    /*========================================*
     * initialization methods
     *========================================*/

    /*========================================*
     * constructors
     *========================================*/
    /**
     * Constructs a <code>Compartment</code> object with name specified by
     * the string <code>pName</code>.  The volume is initialized to a default
     * value of 1.0 liters.
     *
     * @param pName the name of the compartment being constructed
     */
    public Compartment(String pName) throws IllegalArgumentException
    {
        this(pName, DEFAULT_VOLUME_LITERS);
    }

    /**
     * Parameterized constructor in which the name and volume
     * are specified (the latter being in liters).
     *
     * @param pName the name of the compartment being constructed
     * @param pVolumeLiters the volume of the compartment, in liters
     */
    public Compartment(String pName, double pVolumeLiters) throws IllegalArgumentException
    {
        setName(pName);
        setVolumeLiters(pVolumeLiters);
    }

    /*========================================*
     * private methods
     *========================================*/

    /*========================================*
     * protected methods
     *========================================*/

    /*========================================*
     * public methods
     *========================================*/

    public boolean equals(Object pCompartment)
    {
        Compartment compartment = (Compartment) pCompartment;
        return(getName().equals(compartment.getName()) &&
               getVolumeLiters() == compartment.getVolumeLiters());
    }

    public int compareTo(Object pCompartment)
    {
        return(getName().compareTo(((Compartment) pCompartment).getName()));
    }

    public String toString()
    {
        return("compartment[name: " + getName() + "  volume: " + getVolumeLiters() + " liters]");
    }

    public Object clone()
    {
        Compartment newCompartment = new Compartment(getName());
        newCompartment.setVolumeLiters(getVolumeLiters());
        return(newCompartment);
    }
}
