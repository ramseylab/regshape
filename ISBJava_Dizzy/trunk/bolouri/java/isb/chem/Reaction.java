package isb.chem;

import isb.util.*;
import java.util.*;

/**
 * Represents a uni-directional chemical reaction involving one or 
 * more chemical {@link Species}.  In general a reaction can have both 
 * <b>reactants</b> and <b>products</b>.  These are each sets of distinct 
 * chemical {@link Species}, with a multiplicity factor (stoichiometry)
 * specified for each species.  For example, a given reaction may produce
 * two molecules of species H20, which would mean that the reactants set
 * contains the species H20 with a multiplicity of 2.  One or more 
 * reactions are assembled to define a {@link Model}.  In addition to
 * reactants and products, a reaction has a <b>reaction rate method</b> defined.
 * This specifies the method used to compute the reaction's probability density 
 * (of initiating) per unit time.  One of two methods is permitted:
 * <dl>
 * <dt><b>the standard method</b></dt>
 * <dd>In the standard method, the reaction rate is computed using
 * standard reaction kinetics.  The computation involves the
 * <b>reaction parameter</b>, a constant specified using the
 * {@link #setRate(double)} or {@link #setRate(String)} method.
 * The exact method of calculation depends on the setting for the
 * {@link ReactionRateSpeciesMode} mode in the model.
 * If the <code>ReactionRateSpeciesMode</code> is set to 
 * {@link ReactionRateSpeciesMode#MOLECULES}, the reaction
 * rate is the product of the reaction parameter and the number of 
 * distinct combinations of reactant molecules, for the particular
 * set of reactants (and their stoichiometries) for this reaction.
 * (Note that this is the method of computing the reaction rate
 * that is intended for use with the {@link GillespieSimulator}.)
 * If the <code>ReactionRateSpeciesMode</code> is set to
 * {@link ReactionRateSpeciesMode#CONCENTRATION}, the reaction
 * rate is the product of the reaction parameter and the molar
 * concentrations of the reactant species, each raised to the power
 * of the particular species' stoichiometry in this reaction.
 * This method of conputing the reaction rate is intended for use
 * with a deterministic (differential-equation-based) simulator.<p /></dd>
 * <dt><b>the custom rate expression method</b></dt>
 * <dd>In this method, the user specifies a user-defined mathematical 
 * expression for computing a reaction rate, using the 
 * {@link #setRate(MathExpression)} method.  This mathematical expression
 * may make reference to parameters defined for this reaction.  An example
 * rate expression would be:
 * <blockquote>
 * <pre>
 * s1 * s2 * k
 * </pre>
 * </blockquote>
 * Where <code>s1</code> and <code>s2</code> are the names of two chemical
 * species in the model, and <code>k</code> is a {@link Parameter} that
 * has been defined for either the Reaction or the Model.  How the
 * species symbols are interpreted depends on the 
 * <code>ReactionRateSpeciesMode</code> setting, as described above.
 * </dd>
 * </dl>
 * In both methods, the resultant reaction probability per unit time has
 * dimensions of inverse time.  When multiplied by a (presumably infinitesimal)
 * time interval, the result is the probability that this reaction will occur.
 * For more information on the physical motivation for using this parameter
 * to describe a chemical reaction, see Gillespie's paper (the reference
 * can be found in the documentation for the {@link GillespieSimulator} class).
 * <p />
 * In defining a reaction object, please note that there is a limit
 * on the number of reactants of a given species, that this class will
 * allow.  That limit is currently set to 10 (but can be changed by 
 * modifying a constant in the java code for the class).  The purpose
 * of this limitation is to ensure that the computation of the symmetry
 * factor for computing the number of distinct combinations of reactant
 * species, does not result in a numerical overflow (it involves the 
 * factorial of the number of reactants of a given species).
 * <p />
 * In addition to being a container class for two different sets of 
 * {@link Species} objects (corresponding to the reactants and products
 * of the reaction), this class has a number of computational methods
 * used to derive symmetry factors and reaction probability densities
 * for the given chemical reaction.  These methods are only used by the
 * {@link GillespieSimulator} class, and are thus not publically visible.
 * <p />
 * This class does not currently do any caching of the symmetry factors
 * for the reactant multiplicities; it recomputes the factorial of the
 * reactant multiplicity for each reactant species, for each invocation
 * of the <code>recomputeSpeciesPopulationsForReaction()</code> method.
 * <p />
 * This class permits defining a reaction with no reactants, and one or
 * more products.  Such a reaction would represent a spontaneous creation
 * of one or more chemical species (e.g., spontanous creation of particles
 * in a time-dependent, nonequilibrium background environment).  In such a
 * situation, the reaction parameter corresponds directly to the probability
 * density per unit time, of such a particle creation event occurring.
 * Similarly, you may define a reaction with multiple reactants and no products,
 * which would represent an event in which various particles are annihilated
 * (e.g., particle-antiparticle pair annihilation).  Note that it is
 * <em>not</em> allowed to define a reaction with no products and no reactants.
 * <p />
 * Note that is allowed to define a reaction in which a given species,
 * say S1, appears as both a reactant and a product.  You would need to
 * invoke {@link #addProduct(Species)} and {@link #addReactant(Species)}
 * separately, for the S1 species, in order to define such a reaction.
 * <p />
 * Here is a fragment of sample code showing how to define a reaction
 * with the standard method of computing the reaction rate:
 * <blockquote>
 * <pre>
 *             Compartment univ = new Compartment("univ");
 *             Species s1 = new Species("s1", univ);
 *             Species s2 = new Species("s2", univ);
 *             Species s3 = new Species("s3", univ);

 *             Reaction r1 = new Reaction("r1");
 *             r1.addReactant(s1);
 *             r1.addReactant(s2, 2);
 *             r1.addProduct(s3)
 *             r1.setRate(1.0);
 *
 *             Model model = new Model("myModel");
 *             model.addReaction(r1);
 * </pre>
 * </blockquote>
 * The above code sample defines the reaction 
 * <blockquote>
 * <pre>
 * s1 + s2 + s2 -&gt; s3
 * </pre>
 * </blockquote>
 * where <code>s1</code>, <code>s2</code>, and <code>s3</code> are
 * chemical {@link Species}, and the reaction rate parameter is the
 * floating-point value 1.0.
 * <p />
 * Here is a fragment of sample code showing how to define the same
 * reaction, with a custom expression for computing the reaction rate:
 * <blockquote>
 * <pre>
 *             Compartment univ = new Compartment("univ");
 *             Species s1 = new Species("s1", univ);
 *             Species s2 = new Species("s2", univ);
 *             Species s3 = new Species("s3", univ);

 *             Reaction r1 = new Reaction("r1");
 *             r1.addReactant(s1);
 *             r1.addReactant(s2, 2);
 *             r1.addProduct(s3);
 *
 *             Parameter p1 = new Parameter("p1", 1.0);
 *             Parameter p2 = new Parameter("p2", 1.0);
 *             r1.addParameter(p1);
 *             r1.addParameter(p2);
 *
 *             MathExpression exp1 = new MathExpression("s1 * s2 * (s2 - p1) / p2");
 *             r1.setRate(exp1);
 *
 *             Model model = new Model("myModel");
 *             model.addReaction(r1);
 * </pre>
 * </blockquote>
 * The above code defines the same reactants, products, and multiplicities
 * (stoichiometries) for the reactants and products.  But it defines a custom
 * reaction rate given by the expression:
 * <blockquote>
 * <pre>
 * s1 * s2 * (s2 - 1.0) / 2.0
 * </pre>
 * </blockquote>
 * Where the <code>s1</code> and <code>s2</code> symbols denote species 
 * populations.  Note that (as described above) one can also change the 
 * {@link ReactionRateSpeciesMode} setting for the <code>Model</code>,
 * in order to have the reaction rate expression parsed to interpret species 
 * symbols as representing molar concentrations rather than molecular 
 * populations.  This would simply involve adding the statement
 * <blockquote>
 * <pre>
 * model.setReactionRateSpeciesMode(ReactionRateSpeciesMode.CONCENTRATION);
 * </pre>
 * </blockquote>
 * before the <code>model.addReaction(r1)</code> statement in the above example.
 * <p />
 * For more details and sample code using this class, refer to the
 * {@link GillespieSimulator} documentation.
 *
 * @see GillespieSimulator
 * @see Species
 * @see Model
 * @see SpeciesPopulations
 * @see MathExpression
 *
 * @author Stephen Ramsey
 */

public class Reaction implements Comparable, Cloneable
{
    /*========================================*
     * constants
     *========================================*/
    private static final int MAX_NUM_REACTANTS_OF_GIVEN_SPECIES = 10;
    private static final int MAX_NUM_PRODUCTS_OF_GIVEN_SPECIES = 10;
    static final double MIN_SPECIES_POPULATION = 0.0;

    /*========================================*
     * inner class
     *========================================*/

    // this is a simple data structure that bundles together a
    // species and its reaction stoichiometry (multiplicity)
    class ReactionElement implements Cloneable, Comparable
    {
        public Species mSpecies;
        public Integer mMultiplicity;
        public ReactionElement(Species pSpecies, Integer pMultiplicity)
        {
            mSpecies = pSpecies;
            mMultiplicity = pMultiplicity;
        }

        public Object clone()
        {
            Species species = null;
            if(null != mSpecies)
            {
                species = (Species) mSpecies.clone();
            }
            else
            {
                species = null;
            }
            ReactionElement newReactionElement = new ReactionElement(species, mMultiplicity);
            return(newReactionElement);
        }
    }

    // this is a helper class that allows doing lookups of a symbol in
    // three different symbol tables:  the reaction parameter table, the 
    // model parameter table, and the table of species populations
    class RateExpressionSymbolMap implements ISymbolDoubleMap
    {
        private double mTime = 0.0;
        private Model mModel = null;

        protected void setTime(double pTime)
        {
            mTime = pTime;
        }
        
        protected double getTime()
        {
            return(mTime);
        }

        protected void setModel(Model pModel)
        {
            mModel = pModel;
        }

        protected Model getModel()
        {
            return(mModel);
        }

        public SpeciesPopulations mSpeciesPopulations = null;

        private SpeciesPopulations getSpeciesPopulations()
        {
            return(mSpeciesPopulations);
        }

        private void setSpeciesPopulations(SpeciesPopulations pSpeciesPopulations)
        {
            mSpeciesPopulations = pSpeciesPopulations;
        }

        public double getValue(String pSymbolName) throws DataNotFoundException
        {
            double retVal = 0.0;
            double time = getTime();
            Model model = getModel();
            SpeciesPopulations speciesPopulations = getSpeciesPopulations();

            Parameter parameter = null;
            parameter = getParameter(pSymbolName);

            // if the symbol matches a local (reaction) parameter, use the parameter value
            if(null != parameter)
            {
                retVal = parameter.getValue();
            }                
            else
            {
                // let the model figure out the value associated with this symbol
                retVal = model.getSymbolValue(pSymbolName, speciesPopulations, time);
            }

            return(retVal);
        }
    }

    /*========================================*
     * member data
     *========================================*/
    private Vector mReactantsVec;                 // contains the reactant species
    private Vector mProductsVec;                  // contains the product species
    private Double mReactionRateParameter;        // contains the reaction rate parameter 
                                                  // (for the case of standard method of computing the
                                                  //  reaction rate)
    private HashMap mParameters;                  // contains user-defined parameters for the reaction
    private MathExpression mRateExpression;       // contains the custom reaction rate expression
    private RateExpressionSymbolMap mRateExpressionSymbolMap; // helper class for parameter lookups
    private String mName;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setName(String pName)
    {
        mName = pName;
    }

    /**
     * Returns the name of the reaction.
     *
     * @return the name of the reaction.
     */
    public String getName()
    {
        return(mName);
    }

    private RateExpressionSymbolMap getRateExpressionSymbolMap()
    {
        return(mRateExpressionSymbolMap);
    }

    private void setRateExpressionSymbolMap(RateExpressionSymbolMap pRateExpressionSymbolMap)
    {
        mRateExpressionSymbolMap = pRateExpressionSymbolMap;
    }

    private MathExpression getRateExpression()
    {
        return(mRateExpression);
    }

    private void setRateExpression(MathExpression pRateExpression)
    {
        mRateExpression = pRateExpression;
    }

    private HashMap getParameters()
    {
        return(mParameters);
    }

    private void setParameters(HashMap pParameters)
    {
        mParameters = pParameters;
    }

    /**
     * Returns the numeric reaction rate parameter for this reaction, if
     * it is defined (null otherwise).
     *
     * @return the numeric reaction rate parameter for this reaction, if
     * it is defined (null otherwise.)
     */
    public Double getReactionRateParameter()
    {
        return(mReactionRateParameter);
    }

    private void setReactionRateParameter(Double pReactionRateParameter)
    {
        mReactionRateParameter = pReactionRateParameter;
    }

    private void setReactantsVec(Vector pReactantsVec)
    {
        mReactantsVec = pReactantsVec;
    }

    private Vector getReactantsVec()
    {
        return(mReactantsVec);
    }
    
    private void setProductsVec(Vector pProductsVec)
    {
        mProductsVec = pProductsVec;
    }

    private Vector getProductsVec()
    {
        return(mProductsVec);
    }
    

    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeName(String pName)
    {
        setName(pName);
    }

    private void initializeRateExpression()
    {
        setRateExpression(null);
    }
    
    private void initializeParameters()
    {
        setParameters(new HashMap());
    }

    private void initializeReactionRateParameter()
    {
        mReactionRateParameter = null;
    }

    private void initializeReactantsVec()
    {
        setReactantsVec(new Vector());
    }

    private void initializeProductsVec()
    {
        setProductsVec(new Vector());
    }

    private void initializeRateExpressionSymbolMap()
    {
        setRateExpressionSymbolMap(new RateExpressionSymbolMap());
    }

    private void initialize(String pName)
    {
        initializeName(pName);
        initializeRateExpression();
        initializeParameters();
        initializeReactantsVec();
        initializeProductsVec();
        initializeReactionRateParameter();
        initializeRateExpressionSymbolMap();
    }

    /*========================================*
     * constructors
     *========================================*/

    /**
     * Constructs a reaction object with name <code>pName</code>.
     * In order for this reaction to be a permitted element
     * of a {@link Model}, the reaction name must be unique in the 
     * <a href="#namespaces">global namespace</a> of the model.
     * This means that the reaction name must be different from
     * species names, container names, and global parameter names
     * (or else you will get an exception when you call
     * {@link Model#addReaction(Reaction)}).
     *
     * @param pName the name of the reaction
     */
    public Reaction(String pName)
    {
        initialize(pName);
    }

    /*========================================*
     * private methods
     *========================================*/

    private ReactionElement findReactionElement(Vector pVector, Species pSpecies)
    {
        int numSpecies = pVector.size();
        ReactionElement retReactionElement = null;

        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            ReactionElement reactionElement = (ReactionElement) pVector.elementAt(speciesCtr);
            if(reactionElement.mSpecies.equals((Object) pSpecies))
            {
                retReactionElement = reactionElement;
                break;
            }
        }

        return(retReactionElement);
    }

    private double computeReactionProbabilityDensityStandardFormula(Model pModel,
                                                                    SpeciesPopulations pSpeciesPopulations,
                                                                    double pTime) throws DataNotFoundException
    {
        Vector reactantsVec = getReactantsVec();
        int numReactants = reactantsVec.size();
        double reactantCombinations = 1.0;  // if there are no reactants, just assume 1
                                            // (this is to allow spontaneous particle creation,
                                            // if the user wants to define such a process)
        for(int reactantCtr = 0; reactantCtr < numReactants; ++reactantCtr)
        {
            ReactionElement reactionElement = (ReactionElement) reactantsVec.elementAt(reactantCtr);
            Species reactantSpecies = reactionElement.mSpecies;
            Integer reactantMultiplicityObj = reactionElement.mMultiplicity;
            int reactantMultiplicity = reactantMultiplicityObj.intValue();
            assert (reactantMultiplicity > 0) : ("reactant multiplicity for species is less than zero: " + reactantSpecies.getName());

            // We need to determine the population of this species.  It is either
            // fixed or an expression.  We will handle the two cases separately.
            double speciesPopulation = pModel.getSpeciesSymbolValue(reactantSpecies, pTime, pSpeciesPopulations);
            if(reactantMultiplicity <= speciesPopulation)
            {
                reactantCombinations *= MathFunctions.chooseFunction((long) speciesPopulation, reactantMultiplicity);
            }
            else
            {
                reactantCombinations = 0.0;
                break;
            }
        }

        return(reactantCombinations);
    }

    /*========================================*
     * protected methods
     *========================================*/
    void validate() throws IllegalStateException
    {
        if( (null == getReactionRateParameter() && null == getRateExpression()) ||
            (null != getReactionRateParameter() && null != getRateExpression()) )
        {
            throw new IllegalStateException("reaction parameter has not been specified yet");
        }
        if(getReactantsVec().isEmpty() && getProductsVec().isEmpty())
        {
            throw new IllegalStateException("this reaction has no reactants and no products specified");
        }
    }

    void validateInitialData(SpeciesPopulations pInitialData, Model pModel, Double pStartTime) throws IllegalArgumentException
    {
        Vector reactantsVec = new Vector();
        getReactants(reactantsVec);
        Iterator reactantIter = reactantsVec.iterator();
        while(reactantIter.hasNext())
        {
            Species species = (Species) reactantIter.next();
            try
            {
                double speciesPop = 0.0;
                if(null != pStartTime)
                {
                    speciesPop = pInitialData.getSpeciesPopulation(species, pModel, pStartTime.doubleValue());
                }
                else
                {
                    speciesPop = pInitialData.getSpeciesPopulation(species);
                }
                if(speciesPop < MIN_SPECIES_POPULATION)
                {
                    throw new IllegalArgumentException("invalid initial species population for species: " + species.getName());
                }
            }
            catch(DataNotFoundException e)
            {
                throw new IllegalArgumentException("insufficient initial data for reaction: " + toString() + "; species " + species.getName() + " not found in the initial data");
            }
        }

        Vector productsVec = new Vector();
        getProducts(productsVec);
        
        Iterator productIter = productsVec.iterator();
        while(productIter.hasNext())
        {
            Species species = (Species) productIter.next();
            // a product species only needs initial data if it is a floating species;
            // if it is a boundary species, no initial data is needed, if the species
            // only appears as the product of a reaction [if the species appears as
            // a reactant in some reaction, then initial data will be required for that
            // reaction, when that reaciton is validated by calling this validate() method]
            if(species.getFloating())
            {
                try
                {
                    double speciesPop = 0.0;
                    if(null != pStartTime)
                    {
                        speciesPop = pInitialData.getSpeciesPopulation(species, pModel, pStartTime.doubleValue());
                    }
                    else
                    {
                        speciesPop = pInitialData.getSpeciesPopulation(species);
                    }
                    if(speciesPop < MIN_SPECIES_POPULATION)
                    {
                        throw new IllegalArgumentException("invalid initial species population for species: " + species.getName());
                    }
                }
                catch(DataNotFoundException e)
                {
                    throw new IllegalArgumentException("insufficient initial data for reaction: " + toString() + "; species " + species.getName() + " not found in the initial data");
                }
            }
        }
    }


    /*
     * Change the species populations to reflect the fact that a reaction
     * has taken place.  If a given species is a &quot;boundary&quot;
     * (i.e., non-floating) species, do not change its population.
     */
    void recomputeSpeciesPopulationsForReaction(SpeciesPopulations pSpeciesPopulations) throws DataNotFoundException, IllegalStateException
    {
        adjustSpeciesPopulationForReaction(pSpeciesPopulations, 1.0);
    }

    /*
     * Change the species populations to reflect the fact that a reaction
     * has taken place.  If a given species is a &quot;boundary&quot;
     * (i.e., non-floating) species, do not change its population.
     */
    void adjustSpeciesPopulationForReaction(SpeciesPopulations pSpeciesPopulations,
                                            double pReactionRate) throws DataNotFoundException, IllegalStateException
    {
        Vector productsVec = getProductsVec();
        int numProducts = productsVec.size();
        for(int productCtr = 0; productCtr < numProducts; ++productCtr)
        {
            ReactionElement reactionElement = (ReactionElement) productsVec.elementAt(productCtr);
            Species product = reactionElement.mSpecies;
            if(! product.getFloating())
            {
                continue;
            }

            Integer numProductsOfThisSpeciesObj = reactionElement.mMultiplicity;
            assert (numProductsOfThisSpeciesObj != null);
            int numProductsOfThisSpecies = numProductsOfThisSpeciesObj.intValue();
            // decrement this species population accordingly
            double speciesPopulationDelta = pSpeciesPopulations.getSpeciesPopulation(product);
            speciesPopulationDelta += ((double) numProductsOfThisSpecies) * pReactionRate;
            pSpeciesPopulations.setSpeciesPopulation(product, speciesPopulationDelta);
        }

        // get list of reactants
        Vector reactantsVec = getReactantsVec();
        int numReactants = reactantsVec.size();
        for(int reactantCtr = 0; reactantCtr < numReactants; ++reactantCtr)
        {
            ReactionElement reactionElement = (ReactionElement) reactantsVec.elementAt(reactantCtr);
            Species reactant = reactionElement.mSpecies;
            if(! reactant.getFloating())
            {
                continue;
            }

            Integer numReactantsOfThisSpeciesObj = reactionElement.mMultiplicity;
            assert (numReactantsOfThisSpeciesObj != null);
            int numReactantsOfThisSpecies = numReactantsOfThisSpeciesObj.intValue();
            // decrement this species population accordingly
            double speciesPopulationDelta = pSpeciesPopulations.getSpeciesPopulation(reactant);
            speciesPopulationDelta -= ((double) numReactantsOfThisSpecies) * pReactionRate;
            pSpeciesPopulations.setSpeciesPopulation(reactant, speciesPopulationDelta);
        }
    }


    double computeReactionProbabilityDensityPerUnitTime(Model pModel,
                                                        SpeciesPopulations pSpeciesPopulations,
                                                        double pTime) throws DataNotFoundException
    {
        double reactionProbabilityDensity = 0.0;
        if(null == getRateExpression())
        {
            // use the standard method of computing the reaction rate
            reactionProbabilityDensity =  computeReactionProbabilityDensityStandardFormula(pModel, pSpeciesPopulations, pTime) * 
                                          getReactionRateParameter().doubleValue();
        }
        else
        {
            // use the custom rate expression
            MathExpression rateExpression = getRateExpression();
            RateExpressionSymbolMap rateExpressionSymbolMap = getRateExpressionSymbolMap();
            rateExpressionSymbolMap.setModel(pModel);
            rateExpressionSymbolMap.setTime(pTime);
            rateExpressionSymbolMap.setSpeciesPopulations(pSpeciesPopulations);
            reactionProbabilityDensity = rateExpression.computeValue(rateExpressionSymbolMap);
            if(reactionProbabilityDensity < 0.0)
            {
                reactionProbabilityDensity = 0.0;
            }
        }
        return( reactionProbabilityDensity );
    }

    /**
     * fills a collection (supplied by caller) with the reactant species for this reaction
     */
    void getReactants(Collection pReactants)
    {
        Vector reactantsVec = getReactantsVec();
        Iterator reactantsIter = reactantsVec.iterator();
        while(reactantsIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) reactantsIter.next();
            Species species = reactionElement.mSpecies;
            pReactants.add(species);
        }
    }

    /**
     * fills a collection (supplied by caller) with the product species for this reaction
     */
    void getProducts(Collection pProducts)
    {
        Vector productsVec = getProductsVec();
        Iterator productsIter = productsVec.iterator();
        while(productsIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) productsIter.next();
            Species species = reactionElement.mSpecies;
            pProducts.add(species);
        }
    }

    /**
     * Returns the {@link Parameter} object associated with the
     * parameter name <code>pParameterName</code>.  If no parameter
     * is found with name <code>pParameterName</code>, null is 
     * returned.
     *
     * @param pParameterName the parameter name
     *
     * @return the {@link Parameter} object associated with the
     * parameter name <code>pParameterName</code>
     */
    Parameter getParameter(String pParameterName)
    {
        return((Parameter) getParameters().get(pParameterName));
    }

    /**
     * Returns the set of all local {@link Parameter} objects
     * defined for the reaction.
     *
     * @return the set of all local {@link Parameter} objects
     * defined for the reaction.
     */
    Set getParametersSet()
    {
        Set parametersSet = new HashSet();
        Collection parameters = getParameters().values();
        Iterator parametersIter = parameters.iterator();
        while(parametersIter.hasNext())
        {
            Parameter parameter = (Parameter) parametersIter.next();
            parametersSet.add(parameter);
        }
        return(parametersSet);
    }

    /*========================================*
     * public methods
     *========================================*/

    /**
     * Returns the set of all local {@link Parameter} objects
     * defined for the reaction.
     *
     * @return the set of all local {@link Parameter} objects
     * defined for the reaction.
     */
    public Set getParametersSetCopy()
    {
        HashSet parametersSet = (HashSet) getParametersSet();
        HashSet newParametersSet = new HashSet();
        Iterator parametersIter = parametersSet.iterator();
        while(parametersIter.hasNext())
        {
            Parameter parameter = (Parameter) parametersIter.next();
            newParametersSet.add(parameter.clone());
        }
        return(newParametersSet);
    }

    /**
     * fills a collection (supplied by caller) with copies of the reactant species for this reaction
     */
    public void getReactantsCopy(Collection pReactants)
    {
        Vector reactantsVec = getReactantsVec();
        Iterator reactantsIter = reactantsVec.iterator();
        while(reactantsIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) reactantsIter.next();
            Species species = reactionElement.mSpecies;
            pReactants.add((Species) species.clone());
        }
    }

    /**
     * fills a collection (supplied by caller) with copies of the product species for this reaction
     */
    public void getProductsCopy(Collection pProducts)
    {
        Vector productsVec = getProductsVec();
        Iterator productsIter = productsVec.iterator();
        while(productsIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) productsIter.next();
            Species species = reactionElement.mSpecies;
            pProducts.add((Species) species.clone());
        }
    }

    /**
     * Add the {@link Species} <code>pReactant</code> to the set of species
     * constituting the reaction's reactants, with unit multiplicity.
     * Note that if this species is already listed among the reaction's reactants, 
     * this method will replace the multiplicity of the reactant already defined 
     * for this reaction, with unit multiplicity.  To specify a multiplicity
     * greater than one, for a given reactant species, use the 
     * {@link #addReactant(Species,int)} method.
     *
     * @param pReactant The {@link Species} to be added to the list of reaction
     * reactants.  You may call this method more than once, with a given
     * species as the argument; but subsequent invocations of this method
     * have no effect.
     */
    public void addReactant(Species pReactant) throws IllegalStateException
    {
        addReactant(pReactant, 1);
    }

    /**
     * Add the {@link Species} <code>pProduct</code> to the set of species
     * constituting the reaction's products, with unit multiplicity.
     * Note that if this species is already listed among the reaction's products, 
     * this method will replace the multiplicity of the product already defined 
     * for this reaction, with unit multiplicity.  To specify a multiplicity
     * greater than one, for a given product species, use the 
     * {@link #addProduct(Species,int)} method.
     *
     * @param pProduct The {@link Species} to be added to the list of reaction
     * products.  You may call this method more than once, with a given
     * species as the argument; but subsequent invocations of this method
     * have no effect.
     */
    public void addProduct(Species pProduct) throws IllegalStateException
    {
        addProduct(pProduct, 1);
    }

    /**
     * Add the {@link Species} <code>pReactant</code> to the set of species
     * constituting the reaction's reactants, with multiplicity given by the
     * integer parameter <code>pMultiplicity</code>.  Note that if this species
     * is already listed among the reaction's reactants, this method will
     * replace the multiplicity of the reactant already defined for this reaction,
     * with the multiplicity you specified in the <code>pMultiplicity</code> argument.
     *
     * @param pReactant The {@link Species} to be added to the list of reaction
     * reactants.  You may call this method more than once, with a given
     * species as the argument; the multiplicity specified in the last call 
     * to this method, for the given species, is the one that is assigned for this 
     * species.
     */
    public void addReactant(Species pReactant, int pMultiplicity) throws IllegalStateException
    {
        if(pMultiplicity > MAX_NUM_REACTANTS_OF_GIVEN_SPECIES)
        {
            throw new IllegalStateException("you exceeded the maximum number of allowed reactants of a give species, for species " + pReactant + "; maximum number allowed is: " + MAX_NUM_REACTANTS_OF_GIVEN_SPECIES);
        }
        
        Vector reactantsVec = getReactantsVec();
        Integer multiplicity = new Integer(pMultiplicity);

        ReactionElement foundReactionElement = findReactionElement(reactantsVec, pReactant);
        if(null == foundReactionElement)
        {
            reactantsVec.add(new ReactionElement(pReactant, multiplicity));
        }
        else
        {
            foundReactionElement.mMultiplicity = multiplicity;
        }
    }



    /**
     * Add the {@link Species} <code>pProduct</code> to the set of species
     * constituting the reaction's products, with multiplicity given by the
     * integer parameter <code>pMultiplicity</code>.  Note that if this species
     * is already listed among the reaction's products, this method will
     * replace the multiplicity of the product already defined for this reaction,
     * with the multiplicity you specified in the <code>pMultiplicity</code> argument.
     *
     * @param pProduct The {@link Species} to be added to the list of reaction
     * products.  You may call this method more than once, with a given
     * species as the argument; the multiplicity specified in the last call 
     * to this method, for the given species, is the one that is assigned for this 
     * species.
     */
    public void addProduct(Species pProduct, int pMultiplicity) throws IllegalStateException
    {
        if(pMultiplicity > MAX_NUM_PRODUCTS_OF_GIVEN_SPECIES)
        {
            throw new IllegalStateException("you exceeded the maximum number of allowed products of a give species, for species " + pProduct + "; maximum number allowed is: " + MAX_NUM_PRODUCTS_OF_GIVEN_SPECIES);
        }
        
        Vector productsVec = getProductsVec();
        Integer multiplicity = new Integer(pMultiplicity);

        ReactionElement foundReactionElement = findReactionElement(productsVec, pProduct);
        if(null == foundReactionElement)
        {
            productsVec.add(new ReactionElement(pProduct, multiplicity));
        }
        else
        {
            foundReactionElement.mMultiplicity = multiplicity;
        }
    }


    /**
     * Query the multiplicity of a given {@link Species} specified
     * by the <code>pSpecies</code> argument, in the reactant list
     * for this reaction.  Returns the multiplicity of this species
     * as an <code>Integer</code>, or <code>null</code> if this species 
     * is not a reactant for this reaction.
     *
     * @param pSpecies the species for which the multiplicity in the
     * reaction's reactant list, is to be queried
     *
     * @return the multiplicity of this species as an <code>Integer</code>,
     * or <code>null</code> if this species is not a reactant for this
     * reaction
     */
    public Integer getReactantMultiplicity(Species pSpecies)
    {
        Vector reactantsVec = getReactantsVec();

        Integer multiplicity = null;

        ReactionElement reactionElement = findReactionElement(reactantsVec, pSpecies);
        if(null != reactionElement)
        {
            multiplicity = reactionElement.mMultiplicity;
        }

        return(multiplicity);
    }

    /**
     * Query the multiplicity of a given {@link Species} specified
     * by the <code>pSpecies</code> argument, in the product list
     * for this reaction.  Returns the multiplicity of this species
     * as an <code>Integer</code>, or <code>null</code> if this species 
     * is not a product for this reaction.
     *
     * @param pSpecies the species for which the multiplicity in the
     * reaction's product list, is to be queried
     *
     * @return the multiplicity of this species as an <code>Integer</code>,
     * or <code>null</code> if this species is not a product for this
     * reaction
     */
    public Integer getProductMultiplicity(Species pSpecies)
    {
        Vector productsVec = getProductsVec();

        Integer multiplicity = null;

        ReactionElement reactionElement = findReactionElement(productsVec, pSpecies);
        if(null != reactionElement)
        {
            multiplicity = reactionElement.mMultiplicity;
        }

        return(multiplicity);
    }

    /**
     * Returns a string summary description of this reaction
     * 
     * @return a string summary description of this reaction
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("reaction[name: " + getName() + "  reactants: ");

        Vector reactantsVec = getReactantsVec();
        Iterator reactantsIter = reactantsVec.iterator();
        while(reactantsIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) reactantsIter.next();
            Species reactantSpecies = reactionElement.mSpecies;
            Integer reactantMultiplicity = reactionElement.mMultiplicity;
            sb.append(reactantSpecies.getName() + "(" + reactantMultiplicity + ")");
            if(reactantsIter.hasNext())
            {
                sb.append(", ");
            }
        }

        sb.append("  products: ");

        Vector productsVec = getProductsVec();
        Iterator productsIter = productsVec.iterator();
        while(productsIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) productsIter.next();
            Species productSpecies = reactionElement.mSpecies;
            Integer reactantMultiplicity = reactionElement.mMultiplicity;
            sb.append(productSpecies.getName() + "(" + reactantMultiplicity + ")");
            if(productsIter.hasNext())
            {
                sb.append(", ");
            }
        }

        Double reactionRateParameter = getReactionRateParameter();
        if(null != reactionRateParameter)
        {
            sb.append("  reaction-parameter: " + reactionRateParameter);
        }
        else
        {
            MathExpression rateExpression = getRateExpression();
            sb.append("  custom-reaction-rate: " + rateExpression);
        }
        sb.append("]");
        return(sb.toString());
    }

    /**
     * Adds the parameter object <code>pParameter</code> to the reaction.
     * This {@link Parameter} object will contain a name and a value.
     * Parameter names are symbols that can be referenced in a custom rate 
     * law for the reaction.  Any parameter defined for a reaction takes precedence
     * (for that particular reaction only) over a parameter with the 
     * same name, defined for a {@link Model}.  A parameter can also be
     * referenced in setting the <b>reaction parameter</b> for the reaction 
     * (which means using the standard method for computing the
     * reaction rate), but the parameter must have been defined with a
     * call to <code>addParameter</code> before the {@link #setRate(String)} method
     * is invoked.  A parameter value can be changed by repeatedly
     * calling the <code>addParameter</code> method.
     *
     * @param pParameter the parameter to add to the reaction
     */
    public void addParameter(Parameter pParameter)
    {
        String parameterName = pParameter.getName();
        // check to see if the parameter name is disallowed because of
        // reserved symbol names defined in the {@link Model} class.
        Model.checkForValidSymbol(parameterName);

        // store the parameter value (as a Parameter object) in the map
        getParameters().put(parameterName, pParameter);
    }


    /**
     * Returns the floating-point (double) value associated with the
     * parameter name <code>pParameterName</code>.  If no parameter
     * is found with name <code>pParameterName</code>, an exception is
     * thrown.  The parameter must have been previously defined with
     * the {@link #addParameter(Parameter)} method.
     *
     * @param pParameterName the parameter name
     *
     * @return the floating-point (double) value associated with the
     * parameter name <code>pParameterName</code>
     */
    public double getParameterValue(String pParameterName) throws DataNotFoundException
    {
        Parameter parameter = getParameter(pParameterName);
        if(null == parameter)
        {
            throw new DataNotFoundException("requested parameter not found: " + pParameterName);
        }
        return(parameter.getValue());
    }

    /**
     * Defines the raction rate to be computed using the standard
     * method, with a reaction parameter given by the floating-point 
     * (double) value in argument <code>pReactionParameter</code>.  How
     * the reaction rate will be computed depends on the
     * {@link ReactionRateSpeciesMode} setting for the Model.
     * <p />
     * If the <code>ReactionRateSpeciesMode</code> is set to 
     * {@link ReactionRateSpeciesMode#MOLECULES}, the reaction
     * rate is the product of the reaction parameter and the number of 
     * distinct combinations of reactant molecules, for the particular
     * set of reactants (and their stoichiometries) for this reaction.
     * In this case, the reaction parameter has dimensions of inverse time.  
     * For a definition of the reaction parameter appropriate to this case, 
     * refer to p.406 {@linkplain GillespieSimulator Gillespie's paper}.  
     * <p />
     * If the <code>ReactionRateSpeciesMode</code> is set to
     * {@link ReactionRateSpeciesMode#CONCENTRATION}, the reaction
     * rate is the product of the reaction parameter and the molar
     * concentrations of the reactant species, each raised to the power
     * of the particular species' stoichiometry in this reaction.
     * In this case, the reaction parameter has dimensions of inverse time
     * times inverse molar concentration to the power of the sum of the
     * stoichiometries of all of the species in the reaction (this is
     * the usual dimensions of the kinetic constant for a reaction).
     * <p />
     * The double-precision floating-point value passed as the 
     * <code>pReactionParameter</code> parameter must be positive,
     * or else this method throws an <code>IllegalArgumentException</code>.
     * <p />
     * An alternative to using the standard method of computing the
     * reaction rate, is to specify a custom reaction rate formula using
     * the {@link #setRate(MathExpression)} method.  
     * <p />
     * If a custom reaction rate has been previously defined for
     * this reaction, calling this method erases that custom reaction
     * rate and instead configures this <code>Reaction</code> object
     * to use the combinatoric method of computing the reaction rate.
     * 
     * @param pReactionParameter the value of the reaction parameter
     * to set for this reaction
     */
    public void setRate(double pReactionParameter) throws IllegalArgumentException
    {
        if(pReactionParameter <= 0.0)
        {
            throw new IllegalArgumentException("you specified a reaction parameter that is not permitted: " + pReactionParameter);
        }
        setReactionRateParameter(new Double(pReactionParameter));
        setRateExpression(null);
    }

    /**
     * Defines the raction rate to be computed using the combinatoric
     * method, with a reaction parameter given by the floating-point 
     * value associated with symbol parameter 
     * <code>pReactionRateParameterName</code>.  This parameter must
     * have been previously defined using the 
     * {@link #addParameter(Parameter)} method, or an exception will
     * be thrown. 
     *  
     * The reaction rate will be computed as the product of the
     * number of distinct pairs of reactant molecules, and the reaction
     * parameter (the latter is specified as the argument to this method).
     * The <code>pReactionParameter</code> value must be nonnegative.
     *
     * The reaction parameter has dimensions of inverse time.  For 
     * a definition of the reaction parameter, refer to p.406 of
     * Gillespie's paper, referenced {@linkplain GillespieSimulator here}.  The
     * double-precision floating-point value passed as the 
     * <code>pReactionParameter</code> parameter must be positive,
     * or else this method throws an <code>IllegalArgumentException</code>.
     *
     * An alternative to using the combinatoric method of computing the
     * reaction rate, is to specify a custom reaction rate formula using
     * the {@link #setRate(MathExpression)} method.  
     *
     * If a custom reaction rate has been previously defined for
     * this reaction, calling this method erases that custom reaction
     * rate and instead configures this <code>Reaction</code> object
     * to use the combinatoric method of computing the reaction rate.
     * 
     * @param pReactionRateParameterName the string identifying the
     * parameter whose value defines the reaction rate constant
     */
    public void setRate(String pReactionRateParameterName) throws DataNotFoundException, IllegalArgumentException
    {
        setRate(getParameterValue(pReactionRateParameterName));
    }

    /**
     * Specify a custom reaction rate for this reaction.  This means that
     * the combinatoric method of computing the reaction rate will be
     * bypassed in favor of the custom rate expression you specify in the
     * <code>pRateExpression</code> argument.  This expression may make
     * reference to parameters defined in the reaction or in the {@link Model}.
     * If a reaction rate has been previously defined using the 
     * {@link #setRate(double)} or the {@link #setRate(String)} methods,
     * the previously set reaction rate will be erased, in favor of the
     * custom expression passed to this method.  Species symbols that
     * appear in a reaction rate expression are interpreted in one of two
     * ways, depending on the setting for the <b>custom rate expression
     * species mode</b> in the {@link Model}.  The possible modes are
     * {@link ReactionRateSpeciesMode#CONCENTRATION}
     * and {@link ReactionRateSpeciesMode#MOLECULES}.
     * In the former case, a species symbol appearing in a rate expression 
     * is assumed to represent a <b>concentration</b> value, in units of 
     * moles/liter (this mode is for consistency with the SBML specification).
     * In the latter case, a species symbol appearing in a rate expression
     * is assumed to represent the number of <b>molecules</b> of that 
     * species (this is the default mode for a Model).
     *
     * @param pRateExpression the mathematical expression for the reaction
     * rate
     */
    public void setRate(MathExpression pRateExpression)
    {
        setRateExpression(pRateExpression);
        setReactionRateParameter(null);
    }

    public int compareTo(Object pReaction)
    {
        return(getName().compareTo(((Reaction) pReaction).getName()));
    }

    /**
     * Returns a copy of the {@link isb.util.MathExpression} object
     * representing the rate expression for this reaction (if defined),
     * or null otherwise.
     *
     * @return a copy of the {@link isb.util.MathExpression} object
     * representing the rate expression for this reaction (if defined),
     * or null otherwise.
     */
    public MathExpression getRateExpressionCopy()
    {
        MathExpression retVal = null;
        MathExpression rateExpression = getRateExpression();
        if(null != rateExpression)
        {
            retVal = (MathExpression) rateExpression.clone();
        }
        return(retVal);
    }

    public Object clone()
    {
        Reaction newReaction = new Reaction(getName());
        newReaction.setReactantsVec((Vector) getReactantsVec().clone());
        newReaction.setProductsVec((Vector) getProductsVec().clone());
        newReaction.setParameters((HashMap) getParameters().clone());
        newReaction.setRateExpression(getRateExpressionCopy());
        newReaction.setReactionRateParameter(getReactionRateParameter());
        return(newReaction);
    }
}
