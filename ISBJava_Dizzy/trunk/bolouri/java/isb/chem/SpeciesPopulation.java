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

    /*========================================*
     * inner class
     *========================================*/

    /*========================================*
     * member data
     *========================================*/
    private MutableDouble mPopulationValue;
    private MathExpression mPopulationExpression;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setPopulationValue(MutableDouble pPopulationValue)
    {
        mPopulationValue = pPopulationValue;
    }

    MutableDouble getPopulationValue()
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
        MutableDouble populationValue = pSpeciesPopulation.getPopulationValue();
        if(null != populationValue)
        {
            populationValue = new MutableDouble(populationValue.getValue());
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
    
    public SpeciesPopulation(double pPopulationValue)
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
     * was previously set to be a double, the double population value
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
     * Define the population to be a double value, specified by
     * <code>pPopulationValue</code>.  If the population was previously
     * set to be an expression, the expression is erased.  If the
     * population was previously set to be a different double value,
     * the new value supersedes the old value.  The value may be less
     * than zero, which is convenient for modeling a "delta" (change in)
     * a species population between two points in time.
     *
     * @param pPopulationValue the double population value
     */
    void setValue(double pPopulationValue) throws IllegalArgumentException
    {
        MutableDouble populationValueObj = getPopulationValue();
        if(null != populationValueObj)
        {
            populationValueObj.setValue(pPopulationValue);
        }
        else
        {
            populationValueObj = new MutableDouble(pPopulationValue);
            setPopulationValue(populationValueObj);
        }
        setPopulationExpression(null);
    }

    /**
     * Returns the population value, if this object
     * contains an double species population.  If this
     * object contains an expression, an exception is thrown.
     *
     * @return the population value, if this object
     * contains a double species population.  If this
     * object contains an expression, an exception is thrown.
     */
    double getValue() throws IllegalStateException
    {
        MutableDouble populationValueObj = getPopulationValue();
        double retVal = 0.0;
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
