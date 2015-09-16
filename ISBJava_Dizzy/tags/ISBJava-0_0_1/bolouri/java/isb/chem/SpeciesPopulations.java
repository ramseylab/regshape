package isb.chem;

import java.util.*;
import isb.util.*;

/**
 * A class that contains numeric (floating-point) population data for a number of
 * chemical {@link Species}.  This class contains a <code>HashMap</code>
 * that relates a species to its integer population value.  The population
 * value is stored as a {@link MutableDouble} object, for each species object.
 * Objects of this class are used to store initial conditions for a model
 * (population values at the start of the simulation), as well as intermediate
 * population values at various <em>sample times</em> during the simulation.
 * In the latter case, there would be one <code>SpeciesPopulations</code> 
 * object for each sample time.  The units for storing and retrieving
 * species populations to/from this class are defined to be in <b>molecules</b>.
 * <p />
 * For more details and sample code using this class, refer to the 
 * {@link GillespieSimulator} documentation.
 *
 * @see Reaction
 * @see Species
 * @see GillespieSimulator
 * @see Model
 *
 * @author Stephen Ramsey
 */
public class SpeciesPopulations implements Cloneable
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
    private HashMap mSpeciesPopulationsMap;  // keys are species names (as strings); 
                                             //  values are double ints (species populations)
    private String mName;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private HashMap getSpeciesPopulationsMap()
    {
        return(mSpeciesPopulationsMap);
    }

    private void setSpeciesPopulationsMap(HashMap pSpeciesPopulationsMap)
    {
        mSpeciesPopulationsMap = pSpeciesPopulationsMap;
    }

    /**
     * Returns the name of this model.  This name does
     * not affect the functioning of the model; it is just
     * a container for a model name that the caller can store
     * and retrieve.
     * 
     * @return the name of this model.
     */
    public String getName()
    {
        return(mName);
    }

    /**
     * Sets the name of this model.  You may
     * call this method more than once; each time
     * this method is called, the previously
     * stored name is erased, in favor of the name
     * you specify with the <code>pName</code> parameter.
     *
     * @param pName the name of this model.
     */
    public void setName(String pName)
    {
        mName = pName;
    }


    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeSpeciesPopulationsMap()
    {
        setSpeciesPopulationsMap(new HashMap());
    }

    private void initialize()
    {
        initializeSpeciesPopulationsMap();
    }

    /*========================================*
     * constructors
     *========================================*/
    public SpeciesPopulations()
    {
        this("");
    }

    public SpeciesPopulations(String pName)
    {
        initialize();
        setName(pName);
    }

    public SpeciesPopulations(SpeciesPopulations pSpeciesPopulations)
    {
        initialize();
        setName(pSpeciesPopulations.getName());
        HashMap speciesPopulationsMap = pSpeciesPopulations.getSpeciesPopulationsMap();
        Iterator speciesIter = speciesPopulationsMap.keySet().iterator();
        while(speciesIter.hasNext())
        {
            String speciesName = (String) speciesIter.next();
            SpeciesPopulation speciesPopulation = (SpeciesPopulation) speciesPopulationsMap.get(speciesName);
            setSpeciesPopulationObj(speciesName, (SpeciesPopulation) speciesPopulation.clone());
        }
    }

    /*========================================*
     * private methods
     *========================================*/

    private void setSpeciesPopulationObj(String pSpeciesName, SpeciesPopulation pPopulation)
    {
        HashMap speciesPopulationsMap = getSpeciesPopulationsMap();
        speciesPopulationsMap.put(pSpeciesName, pPopulation);
    }

    /*========================================*
     * protected methods
     *========================================*/
    SpeciesPopulation getSpeciesPopulationObj(String pSpeciesName)
    {
        HashMap speciesPopulationsMap = getSpeciesPopulationsMap();
        return((SpeciesPopulation) speciesPopulationsMap.get(pSpeciesName));
    }

    /*========================================*
     * public methods
     *========================================*/

    /**
     * For the array of requested species name <code>pRequestedSpecies</code>,
     * stores the population value (as a double) into the array 
     * <code>pSpeciesPopulations</code>.
     *
     * @param pRequestedSpecies the array of species names for which population
     * data is requested
     *
     * @param pSpeciesPopulations the array into which the species population
     * values are stored, in the same order they are requested
     */
    public void copyPopulationDataToArray(String []pRequestedSpecies,
                                          double []pSpeciesPopulations) throws DataNotFoundException
    {
        int numSpecies = pRequestedSpecies.length;

        for(int speciesIndex = 0; speciesIndex < numSpecies; ++speciesIndex)
        {
            String speciesName = pRequestedSpecies[speciesIndex];
            double speciesPopulation = getSpeciesPopulation(speciesName);
                    
            pSpeciesPopulations[speciesIndex] = speciesPopulation;
        }
    }

    /**
     * Store the long integer population value <code>pPopulation</code> with
     * the species <code>pSpecies</code>/  The species population is in
     * <b>molecules</b>.  The species population is converted to a double
     * value before it is stored.
     *
     * @param pSpecies the species whose population is being specified.  This
     * can be any species object.  If it is the first time that 
     * the <code>setSpeciesPopulation</code> method is being called for this
     * species object, a new container is created to store the population 
     * value.  Otherwise, the population value is stored in the container
     * that already exists (from the previouc call to 
     * <code>setSpeciesPopulation</code>).
     *
     * @param pPopulation The long integer population value for this species.
     */
    public void setSpeciesPopulation(Species pSpecies, long pPopulation) throws IllegalArgumentException
    {
        setSpeciesPopulation(pSpecies, (double) pPopulation);
    }


    /**
     * Store the double population value <code>pPopulation</code> with
     * the species <code>pSpecies</code>/  The species population is in
     * <b>molecules</b>.
     *
     * @param pSpecies the species whose population is being specified.  This
     * can be any species object.  If it is the first time that 
     * the <code>setSpeciesPopulation</code> method is being called for this
     * species object, a new container is created to store the population 
     * value.  Otherwise, the population value is stored in the container
     * that already exists (from the previouc call to 
     * <code>setSpeciesPopulation</code>).
     *
     * @param pPopulation The double population value for this species.
     */
    public void setSpeciesPopulation(Species pSpecies, double pPopulation) throws IllegalArgumentException
    {
        String speciesName = pSpecies.getName();
        setSpeciesPopulation(speciesName, pPopulation);
    }


    /**
     * Sets the species <code>pSpecies</code> to use the
     * expression <code>pPopulationExpression</code> as its population.
     * If the species <code>pSpecies</code> is not a boundary species,
     * an exception will be thrown.  The value of the expression should
     * be the number of <b>molecules</b> of the species <code>pSpecies</code>.
     */
    public void setSpeciesPopulation(Species pSpecies, MathExpression pPopulationExpression) throws IllegalArgumentException
    {
        if(pSpecies.getFloating())
        {
            throw new IllegalArgumentException("you may not set the species population of a floating species to be an expression; species: " + pSpecies.getName());
        }
        String speciesName = pSpecies.getName();
        SpeciesPopulation speciesPopulationObj = getSpeciesPopulationObj(speciesName);
        MathExpression newSpeciesPopulation = (MathExpression) pPopulationExpression.clone();
        if(null != speciesPopulationObj)
        {
            speciesPopulationObj.setValue(newSpeciesPopulation);
        }
        else
        {
            speciesPopulationObj = new SpeciesPopulation(newSpeciesPopulation);
            setSpeciesPopulationObj(speciesName, speciesPopulationObj);
        }
    }


    /**
     * Returns the species population for the specified {@link Species}, 
     * or throws a <code>DataNotFoundException</code> if the species has no population 
     * data stored.
     *
     * @param pSpeciesName the species name for which population data is to be returned.
     * 
     * @return the species population for the specified {@link Species}, 
     * or throws a <code>DataNotFoundException</code> if the species has no population 
     * data stored
     */
    public double getSpeciesPopulation(String pSpeciesName) throws DataNotFoundException, IllegalStateException
    {
        SpeciesPopulation speciesPopulationObj = getSpeciesPopulationObj(pSpeciesName);
        double retVal = 0.0;
        if(null != speciesPopulationObj)
        {
            retVal = speciesPopulationObj.getValue();
        }
        else
        {
            throw new DataNotFoundException("could not find species population value for species:  " + pSpeciesName);
        }
        return(retVal);
    }

    /**
     * Returns the species population for the specified chemical species,
     * even in the case where the species population is a mathematical
     * expression.  
     *
     * @param pSpecies the species whose population is to be returned
     *
     * @param pModel the model containing the parameter values, which 
     * can be referenced in a mathematical expression for a species population
     *
     * @param pTime the simulation time, which can be referenced in a
     * mathematical expression for a species population
     *
     * @return the species population for the specified chemical species,
     * even in the case where the species population is a mathematical
     * expression.  
     */
    public double getSpeciesPopulation(Species pSpecies, Model pModel, double pTime) throws DataNotFoundException
    {
        return(getSpeciesPopulation(pSpecies.getName(), pModel, pTime));
    }

    public void setSpeciesPopulation(String pSpeciesName, double pPopulation)
    {
        SpeciesPopulation speciesPopulationObj = getSpeciesPopulationObj(pSpeciesName);
        if(null != speciesPopulationObj)
        {
            speciesPopulationObj.setValue(pPopulation);
        }
        else
        {
            speciesPopulationObj = new SpeciesPopulation(pPopulation);
            setSpeciesPopulationObj(pSpeciesName, speciesPopulationObj);
        }
    }

    /**
     * Returns the species population for the specified chemical species,
     * even in the case where the species population is a mathematical
     * expression.  
     *
     * @param pSpecies the species whose population is to be returned
     *
     * @param pModel the model containing the parameter values, which 
     * can be referenced in a mathematical expression for a species population
     *
     * @param pTime the simulation time, which can be referenced in a
     * mathematical expression for a species population
     *
     * @return the species population for the specified chemical species,
     * even in the case where the species population is a mathematical
     * expression.  
     */
    public double getSpeciesPopulation(String pSpeciesName, Model pModel, double pTime) throws DataNotFoundException
    {
        double retVal = 0.0;

        SpeciesPopulation speciesPopulationObj = getSpeciesPopulationObj(pSpeciesName);
        if(null == speciesPopulationObj)
        {
            throw new DataNotFoundException("no population data found for species: " + pSpeciesName);
        }
        MathExpression speciesPopulationExpression = speciesPopulationObj.getPopulationExpression();
        if(null == speciesPopulationExpression)
        {
            retVal = (double) speciesPopulationObj.getValue();
        }
        else
        {
            retVal = pModel.computeValueOfSpeciesPopulationExpression(speciesPopulationExpression, pTime);
        }
        return(retVal);
    }

    /**
     * Returns a boolean value indicating whether the species specified by
     * <code>pSpeciesName</code> has a population value that is an expression.
     * If the species value is an exprssion, <code>true</code> is returned.
     * If the species value is a double value, <code>false</code> is returned.
     * If no species population object is defined for this species name,
     * an exception is thrown.
     *
     * @param pSpeciesName the species name 
     *
     * @return a boolean value indicating whether the species specified by
     * <code>pSpeciesName</code> has a population value that is an expression.
     */
    public boolean speciesPopulationIsExpression(String pSpeciesName) throws DataNotFoundException
    {
        SpeciesPopulation speciesPopulationObj = getSpeciesPopulationObj(pSpeciesName);
        boolean retVal = false;
        if(null != speciesPopulationObj)
        {
            if(null != speciesPopulationObj.getPopulationExpression())
            {
                retVal = true;
            }
        }
        else
        {
            throw new DataNotFoundException("could not find species population value for species:  " + pSpeciesName);
        }
        return(retVal);
    }

    /**
     * Returns a copy of the mathematical expression for the population
     * of a given species, specified by the name <code>pSpeciesName</code>.
     * If this species has a population that is not an expression, an exception
     * is thrown. 
     *
     * @param pSpeciesName the species name for which the population expression is
     * to be returned
     *
     * @return a copy of the mathematical expression for the population
     * of a given species, specified by the name <code>pSpeciesName</code>.
     */
    public MathExpression getSpeciesPopulationExpressionCopy(String pSpeciesName) throws DataNotFoundException, IllegalStateException
    {
        SpeciesPopulation speciesPopulationObj = getSpeciesPopulationObj(pSpeciesName);
        MathExpression retVal = null;
        if(null != speciesPopulationObj)
        {
            MathExpression exp = speciesPopulationObj.getPopulationExpression();
            if(null != exp)
            {
                retVal = (MathExpression) exp.clone();
            }
            else
            {
                throw new IllegalStateException("species population is not an expression: " + pSpeciesName);
            }
        }
        else
        {
            throw new DataNotFoundException("could not find species population value for species:  " + pSpeciesName);
        }
        return(retVal);
        
    }

    /**
     * Returns the species population for the specified {@link Species}, 
     * or throws a <code>DataNotFoundException</code> if the species has no population 
     * data stored.
     *
     * @param pSpecies the species for which population data is to be returned.
     * 
     * @return the species population for the specified {@link Species}, 
     * or throws a <code>DataNotFoundException</code> if the species has no population 
     * data stored
     */
    public double getSpeciesPopulation(Species pSpecies) throws DataNotFoundException, IllegalStateException
    {
        String speciesName = pSpecies.getName();
        return(getSpeciesPopulation(speciesName));
    }

    public Object clone() 
    {
        SpeciesPopulations newSpeciesPopulations = new SpeciesPopulations(this);
        return(newSpeciesPopulations);
    }

    /**
     * This method implements an assignment operator with a deep copy.  It makes the
     * object whose <code>copy</code> method is being called, a copy of
     * the object referenced by the argument <code>pSpeciesPopulations</code>.
     */
    public void copy(SpeciesPopulations pSpeciesPopulations)
    {
        initialize();
        setName(pSpeciesPopulations.getName());
        HashMap speciesPopulationsMap = pSpeciesPopulations.getSpeciesPopulationsMap();
        Iterator speciesIter = speciesPopulationsMap.keySet().iterator();
        while(speciesIter.hasNext())
        {
            String speciesName = (String) speciesIter.next();
            SpeciesPopulation speciesPopulation = (SpeciesPopulation) speciesPopulationsMap.get(speciesName);
            SpeciesPopulation newSpeciesPopulation = (SpeciesPopulation) speciesPopulation.clone();
            setSpeciesPopulationObj(speciesName, newSpeciesPopulation);
        }
    }

    public void scalarMult(double pMult)
    {
        HashMap speciesPopulationsMap = getSpeciesPopulationsMap();
        Iterator speciesIter = speciesPopulationsMap.keySet().iterator();
        while(speciesIter.hasNext())
        {
            String speciesName = (String) speciesIter.next();
            SpeciesPopulation speciesPopulation = (SpeciesPopulation) speciesPopulationsMap.get(speciesName);
            assert (null != speciesPopulation) : "null species population encountered";

            MutableDouble popValue = speciesPopulation.getPopulationValue();
            if(null != popValue)
            {
                popValue.setValue(popValue.getValue() * pMult);
            }
            else
            {
                // do nothing, must be an expression
            }
        }        
    }

    /**
     * Goes through the list of species contained in <code>pSpeciesPopulations</code>
     * and adds the population of each species to the species populations object whose
     * <code>addSpeciesPopulations</code> method is being called.  If for a given species, 
     * no population is defined within the species populations object whose 
     * <code>addSpeciesPopulations</code> method is being called, zero is assumed, and
     * the population for that species from the <code>pSpeciesPopulations</code> object
     * is added to it.
     */
    public void vectorAdd(SpeciesPopulations pSpeciesPopulations)
    {
        HashMap speciesPopulationsMap = pSpeciesPopulations.getSpeciesPopulationsMap();
        Iterator speciesIter = speciesPopulationsMap.keySet().iterator();
        while(speciesIter.hasNext())
        {
            String speciesName = (String) speciesIter.next();
            SpeciesPopulation operandSpeciesPopulation = (SpeciesPopulation) speciesPopulationsMap.get(speciesName);
            SpeciesPopulation mySpeciesPopulation = getSpeciesPopulationObj(speciesName);

            if(null == mySpeciesPopulation)
            {
                mySpeciesPopulation = new SpeciesPopulation(operandSpeciesPopulation);
                setSpeciesPopulationObj(speciesName, mySpeciesPopulation);
            }
            else
            {
                MutableDouble operandPopulationValueObj = operandSpeciesPopulation.getPopulationValue();
                if(null != operandPopulationValueObj)
                {
                    double operandPopulationValue = operandPopulationValueObj.getValue();
                    MutableDouble myPopulationValueObj = mySpeciesPopulation.getPopulationValue();
                    double myPopulationValue = 0.0;
                    if(null != myPopulationValueObj)
                    {
                        myPopulationValue = myPopulationValueObj.getValue() + operandPopulationValue;
                    }
                    else
                    {
                        myPopulationValue = operandPopulationValue;
                    }
                    mySpeciesPopulation.setValue(myPopulationValue);
                }
                else
                {
                    // must be an expression; just copy it
                    mySpeciesPopulation.setValue(operandSpeciesPopulation.getPopulationExpression());
                }
            }
            setSpeciesPopulationObj(speciesName, mySpeciesPopulation);
        }        
    }


    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("speciesPopulationsName: " + getName() + "\n\n");
        
        HashMap speciesPopulationsMap = getSpeciesPopulationsMap();
        Set speciesNames = speciesPopulationsMap.keySet();
        List speciesNamesList = new LinkedList(speciesNames);
        Collections.sort(speciesNamesList);
        Iterator speciesIter = speciesNamesList.iterator();
        while(speciesIter.hasNext())
        {
            String speciesName = (String) speciesIter.next();
            SpeciesPopulation speciesPopulationObj = getSpeciesPopulationObj(speciesName);
            MutableDouble speciesPopulationValue = speciesPopulationObj.getPopulationValue();
            String speciesDesc = null;
            if(null != speciesPopulationValue)
            {
                speciesDesc = new String(speciesPopulationValue + " molecules");
            }
            else
            {
                MathExpression speciesPopulationExpression = speciesPopulationObj.getPopulationExpression();
                if(null != speciesPopulationExpression)
                {
                    speciesDesc = speciesPopulationExpression.toString();
                }
                else
                {
                    throw new IllegalStateException("object of uknown type found in species population map; species: " + speciesName);
                }
            }
            sb.append("species: " + speciesName + "; population: " + speciesDesc + "\n");
        }
        return(sb.toString());
    }
}
