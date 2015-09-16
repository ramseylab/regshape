package isb.chem;

import isb.util.*;

/**
 * Contains a population value or expression for
 * a chemical species.
 *
 * @author Stephen Ramsey
 */
class SpeciesPopulation implements Cloneable
{
    /*========================================*
     * constants
     *========================================*/
    public static final long MIN_POPULATION = 0;

    /*========================================*
     * inner class
     *========================================*/

    /*========================================*
     * member data
     *========================================*/
    private MutableLong mPopulationValue;
    private MathExpression mPopulationExpression;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setPopulationValue(MutableLong pPopulationValue)
    {
        mPopulationValue = pPopulationValue;
    }

    MutableLong getPopulationValue()
    {
        return(mPopulationValue);
    }

    private void setPopulationExpression(MathExpression pPopulationExpression)
    {
        mPopulationExpression = pPopulationExpression;
    }

    MathExpression getPopulationExpression()
    {
        return(mPopulationExpression);
    }
    

    /*========================================*
     * initialization methods
     *========================================*/
    private void initialize()
    {
        setPopulationValue(null);
        setPopulationExpression(null);
    }

    /*========================================*
     * constructors
     *========================================*/
    public SpeciesPopulation(SpeciesPopulation pSpeciesPopulation) 
    {
        initialize();
        MutableLong populationValue = pSpeciesPopulation.getPopulationValue();
        if(null != populationValue)
        {
            populationValue = new MutableLong(populationValue.getValue());
        }
        setPopulationValue(populationValue);
        MathExpression speciesPopulationExpression = pSpeciesPopulation.getPopulationExpression();
        if(null != speciesPopulationExpression)
        {
            setPopulationExpression((MathExpression) speciesPopulationExpression.clone());
        }
        else
        {
            setPopulationExpression(null);
        }
    }
    
    public SpeciesPopulation(long pPopulationValue)
    {
        initialize();
        setValue(pPopulationValue);
    }

    public SpeciesPopulation(MathExpression pPopulationExpression)
    {
        initialize();
        setValue(pPopulationExpression);
    }

    /*========================================*
     * private methods
     *========================================*/

    /*========================================*
     * protected methods
     *========================================*/

    /**
     * Define the population to be an expression specified by
     * <code>pPopulationExpression</code>.  If the population
     * was previously set to be an integer, the integer population
     * is erased.  If the population was previously set to be an
     * expression, the new expression supersedes the old one.
     *
     * @param pPopulationExpression the species population expression
     */
    void setValue(MathExpression pPopulationExpression) throws IllegalArgumentException
    {
        if(null == pPopulationExpression)
        {
            throw new IllegalArgumentException("invalid argument for pPopulationExpression: null");
        }
        setPopulationValue(null);
        setPopulationExpression(pPopulationExpression);
    }

    /**
     * Define the population to be an integer value, specified by
     * <code>pPopulationValue</code>.  If the population was previously
     * set to be an expression, the expression is erased.  If the
     * population was previously set to be a different integer value,
     * the new value supersedes the old value.  The value must be
     * greater than or equal to zero.
     *
     * @param pPopulationValue the integer population value
     */
    void setValue(long pPopulationValue) throws IllegalArgumentException
    {
        if(pPopulationValue < MIN_POPULATION)
        {
            throw new IllegalArgumentException("invalid population value passed as argument: " + pPopulationValue);
        }
        MutableLong populationValueObj = getPopulationValue();
        if(null != populationValueObj)
        {
            populationValueObj.setValue(pPopulationValue);
        }
        else
        {
            populationValueObj = new MutableLong(pPopulationValue);
            setPopulationValue(populationValueObj);
        }
        setPopulationExpression(null);
    }

    /**
     * Returns the population value, if this object
     * contains an integer species population.  If this
     * object contains an expression, an exception is thrown.
     *
     * @return the population value, if this object
     * contains an integer species population.  If this
     * object contains an expression, an exception is thrown.
     */
    long getValue() throws IllegalStateException
    {
        MutableLong populationValueObj = getPopulationValue();
        long retVal = 0;
        if(null != populationValueObj)
        {
            retVal = populationValueObj.getValue();
        }
        else
        {
            throw new IllegalStateException("cannot call getValue() when the species population is defined to be an expression: " + getPopulationExpression());
        }

        return(retVal);
    }

    /*========================================*
     * public methods
     *========================================*/


    public Object clone()
    {
        SpeciesPopulation newSpeciesPopulation = new SpeciesPopulation(this);
        return(newSpeciesPopulation);
    }

}
