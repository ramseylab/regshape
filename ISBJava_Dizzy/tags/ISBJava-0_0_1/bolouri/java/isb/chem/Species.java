package isb.chem;

import isb.util.*;
import java.util.*;

/**
 * Represents a chemical species, as defined in the SBML level 1 
 * specification.  A chemical species can  
 * appear in one or more chemical {@linkplain Reaction reactions} as a
 * reactant or a product.  It contains the species name (which is
 * a string). Note that species population information
 * for a system is contained in the {@link SpeciesPopulations} object.
 * A chemical species is included in a {@link Model} through the
 * reactions that involve that species; a model contains a given
 * species only insofar as the model has at least one reaction 
 * in which the given species appears as either a reactant or a product.
 * A species is also associated with a {@link Compartment}, which must
 * be specified through the constructor.
 * <p />
 * For more details and sample code using this class, refer to the 
 * {@link GillespieSimulator} documentation.
 *
 * @see Model
 * @see GillespieSimulator
 * @see Reaction
 * @see Compartment
 * @see SpeciesPopulations
 *
 * @author Stephen Ramsey
 */

public class Species implements Comparable, Cloneable
{
    /*========================================*
     * constants
     *========================================*/
    private static final boolean DEFAULT_FLOATING = true;

    /*========================================*
     * member data
     *========================================*/
    private String mName;
    private boolean mFloating;  // true if this species is a floating species; false 
                                // if it is a "boundary" species (concentration never changes)
                                // (default is true)
    private Compartment mCompartment;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    /**
     * Returns the compartment to which this species
     * belongs, or <code>null</code> if the species does not
     * belong to any compartment.
     */
    Compartment getCompartment()
    {
        return(mCompartment);
    }

    /**
     * Sets the compartment for this species to
     * <code>pCompartment</code>.
     *
     * @param pCompartment the compartment to which this
     * species belongs (cannot be null) 
     */
    private void setCompartment(Compartment pCompartment)
    {
        if(null == pCompartment)
        {
            throw new IllegalArgumentException("invalid (null) compartment reference passed");
        }
        mCompartment = pCompartment;
    }

    /**
     * Returns the value for this species, for the
     * &quot;floating&quot; boolean field.  A value of
     * true indicates that it is a floating species.  A value
     * of false indicates that it is a &quot;boundary&quot;
     * (i.e., non-floating) species.
     *
     * @return a boolean value indicating whether the
     * species is a floating species (true) or a boundary species
     * (false).
     */
    public boolean getFloating()
    {
        return(mFloating);
    }

    /**
     * Set the value for this species, for the
     * &quot;floating&quot; boolean field.  A value of
     * true indicates that it is a floating species.  A value
     * of false indicates that it is a &quot;boundary&quot;
     * (i.e., non-floating) species.
     *
     * @param pFloating a boolean value indicating whether the
     * species is a floating species (true) or a boundary species
     * (false).
     */
    public void setFloating(boolean pFloating)
    {
        mFloating = pFloating;
    }

    /**
     * Returns the name of this species object.
     *
     * @return the name of this species object.
     */
    public String getName()
    {
        return(mName);
    }

    /**
     * Assigns the <code>String</code> <code>pName</code> as the
     * &quot;name&quot; of this chemical species.  This name must be
     * a valid &quot;symbol&quot; allowed by the 
     * {@link isb.util.MathExpression#isValidSymbol(String)} 
     * function. This means it cannot contain whitespace, parentheses, or arithmetic
     * operators; also, it may not parse as a floating-point or integer number.
     * The symbol &quot;time&quot; is also disallowed as the species name, because
     * it is reserved as the {@linkplain  Model#SYMBOL_TIME symbol for clock time}.
     *
     * @param pName the <code>String</code> containing the name
     * (or new name) of this chemical species.  This method may be
     * called more than once.  If you define a chemical species
     * with name &quot;s1&quot; using the 
     * {@linkplain #Species(String) constructor},
     * and then call <code>setName("s2")</code>, the <code>Species</code>
     * object will be re-named to the string &quot;s2&quot;.  This argument
     * cannot be null.
     */
    private void setName(String pName) throws IllegalArgumentException
    {
        if(null == pName || pName.length() == 0)
        {
            throw new IllegalArgumentException("attempt to define empty species name");
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
     * Constructs a <code>Species</code> object with name specified by
     * the string <code>pName</code> and the compartment specified by
     * <code>pCompartment</code>.  The species is set to be
     * a &quot;floating&quot; species, which is the default.  To
     * create a &quot;boundary&quot; species (i.e., not floating),
     * use the {@link #Species(String,Compartment,boolean)} constructor.
     * The species name must be
     * a valid &quot;symbol&quot; allowed by the 
     * {@link isb.util.MathExpression#isValidSymbol(String)} 
     * function. This means it cannot contain whitespace, parentheses, or arithmetic
     * operators; also, it may not parse as a floating-point or integer number.
     * A species name may not match any of the 
     * <a href="Model.html#reservedsymbols">reserved symbols</a>.
     *
     * @param pName the name of the species
     *
     * @param pCompartment the compartment with which this species
     * is associated; it cannot be null
     */
    public Species(String pName, Compartment pCompartment) throws IllegalArgumentException
    {
       	this(pName, pCompartment, DEFAULT_FLOATING);
    }

    /**
     * Constructs a <code>Species</code> object with name specified by
     * the string <code>pName</code> and the compartment specified by
     * <code>pCompartment</code>.  The species is set to be
     * a &quot;floating&quot; species.
     * The species name must be
     * a valid &quot;symbol&quot; allowed by the 
     * {@link isb.util.MathExpression#isValidSymbol(String)} 
     * function. This means it cannot contain whitespace, parentheses, or arithmetic
     * operators; also, it may not parse as a floating-point or integer number.
     * A species name may not match any of the 
     * <a href="Model.html#reservedsymbols">reserved symbols</a>.
     *
     * @param pName the name of the species
     *
     * @param pCompartment the compartment with which this species
     * is associated; it cannot be null
     *
     * @param pFloating boolean value indicating if the species is floating
     * (true) or a boundary species (false).
     */
    public Species(String pName, Compartment pCompartment, boolean pFloating) throws IllegalArgumentException
    {
        
        setName(pName);
        setCompartment(pCompartment);
        setFloating(pFloating);
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
    /**
     * Returns a clone of the {@link Compartment} object to which 
     * this species belongs, or <code>null</code> if the species does 
     * not belong to any compartment.
     */
    public Compartment getCompartmentCopy()
    {
        return((Compartment) getCompartment().clone());
    }

    public boolean equals(Object pSpecies)
    {
        Species species = (Species) pSpecies;
        return( getName().equals(species.getName()) &&
                getFloating() == species.getFloating() &&
                getCompartment().equals((Object) species.getCompartment()) );
    }

    public int compareTo(Object pSpecies)
    {
        return(getName().compareTo(((Species) pSpecies).getName()));
    }

    public Object clone()
    {
        Species newSpecies = new Species(getName(), getCompartment());
        newSpecies.setFloating(getFloating());
        return(newSpecies);
    }

    /**
     * Returns a string containing a textual description
     * of this species object.
     */
    public String toString()
    {
        return("species[name: " + getName() + "  floating: " + getFloating() + "  compartment: " + getCompartment().getName() + "]");
    }
}


