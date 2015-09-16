package isb.chem;
import isb.util.*;

/**
 * Represents a named parameter value that can appear in a 
 * rate law expression for a chemical {@link Reaction}.  Parameters
 * can be defined a specific {@link Reaction}, or they can be defined
 * to be global parameters, in which case they apply to all reactions.
 * Global parameters are associated with the {@link Model} class.
 * <p />
 * For more details and sample code using this class, refer to the 
 * {@link GillespieSimulator} documentation.
 *
 * @see Reaction
 * @see GillespieSimulator
 *
 * @author Stephen Ramsey
 */
public class Parameter implements Comparable, Cloneable
{
    /*========================================*
     * constants
     *========================================*/
    /*========================================*
     * member data
     *========================================*/
    private String mName;
    private Double mValue;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    /**
     * Returns the name of this parameter object.
     *
     * @return the name of this parameter object.
     */
    public String getName()
    {
        return(mName);
    }

    /**
     * Assigns the name of this parameter to be <code>pName</code>.  
     * The name must be an allowed symbol name as defined by the 
     * {@link isb.util.MathExpression#isValidSymbol(String)}
     * function.  This means it may not contain whitespace, parentheses, or arithmetic
     * operators; also, it also may not parse as a floating-point or integer number.
     * A parameter name may not match any of the 
     * <a href="Model.html#reservedsymbols">reserved symbols</a>.
     *
     * @param pName the name of the parameter
     */
    private void setName(String pName) throws IllegalArgumentException
    {
        if(null == pName || pName.length() == 0)
        {
            throw new IllegalArgumentException("attempt to define empty parameter name");
        }
        mName = pName;
    }

    /**
     * Returns the value of this parameter object.
     *
     * @return the value of this parameter object
     */
    public double getValue()
    {
        return(mValue.doubleValue());
    }

    private void setValue(Double pValue)
    {
        mValue = pValue;
    }

    /*========================================*
     * initialization methods
     *========================================*/


    /*========================================*
     * constructors
     *========================================*/
    /**
     * Constructs a <code>Parameter</code> object with name specified by
     * the string <code>pName</code> and the value specified by
     * <code>pValue</code>.
     * The name must be an allowed symbol name as defined by the 
     * {@link isb.util.MathExpression#isValidSymbol(String)}
     * function.  This means it may not contain whitespace, parentheses, or arithmetic
     * operators; also, it also may not parse as a floating-point or integer number.
     * A parameter name may not match any of the 
     * <a href="Model.html#reservedsymbols">reserved symbols</a>.
     *
     * @param pName the name of the parameter
     *
     * @param pValue the value of the parameter
     */
    public Parameter(String pName, double pValue) throws IllegalArgumentException
    {
        setName(pName);
        setValue(new Double(pValue));
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
    public Object clone()
    {
        Parameter newParameter = new Parameter(getName(), getValue());
        return(newParameter);
    }

    public boolean equals(Object pParameter)
    {
        Parameter parameter = (Parameter) pParameter;
        return( getName().equals(parameter.getName()) &&
                getValue() == parameter.getValue() );
    }

    public int hashCode()
    {
        int hashCode = (mName.hashCode()) ^ (mValue.hashCode());
        return(hashCode);
    }

    public String toString()
    {
        return("param[name: " + getName() + "  value: " + getValue() + "]");
    }

    public int compareTo(Object pParameter)
    {
        return(getName().compareTo(((Parameter) pParameter).getName()));
    }
}
