package isb.chem;

import java.util.*;
import isb.util.*;

/**
 * Represents a system of coupled chemical reactions, species, and 
 * parameters.  The dynamics of a model may be simulated (evolved in
 * time) using a {@link ISimulator} object, such as {@link GillespieSimulator}.
 * A model is essentially a container class with a <code>Vector</code> of
 * {@link Reaction} objects, each representing a specific 
 * uni-directional chemical reaction that (in turn) maps reactant 
 * {@link Species} to product {@link Species}.   The model class also contains 
 * a collection of {@linkplain Parameter parameters}. Finally, a Model contains
 * a list of global symbols (reacation names, species names, compartment names,
 * and parameter names) defined within the model.  Symbols defined within 
 * the model are required to be unique, which means that a reaction and a 
 * species cannot be defined that have the same name.  
 * <p />
 * A species in a model may be defined to be a &quot;floating&quot; or
 * a &quot;boundary&quot; species.  A floating species is dynamical,
 * with a population that fluctuates based on reactions that are occurring in
 * the model).  A boundary species is a species whose population is fixed
 * to be a constant or a specified function of time; its value is not
 * affected by the dynamics of the model, but is instead a boundary condition.
 * <p />
 * Parameters may be used to define values that can be referenced
 * in custom reaction rate laws.  Parameters defined for the model are
 * called <b>global parameters</b> (to contrast them with parameters 
 * defined for a specific {@link Reaction}, which are called <b>local parameters</b>).
 * A model indirectly contains 
 * {@link Compartment} objects and {@link Species} objects, through 
 * the {@link Reaction} objects.
 * <p />
 * Reactions are <b>uni-directional</b>, so to create a model with a reversible
 * chemical reaction, you would need to define two separate 
 * <code>Reaction</code> objects, and you would need to call the 
 * {@link #addReaction(Reaction)} method on both of them.
 * <p />
 * A model must contain at least one {@link Compartment}, at least one
 * {@link Species}, and at least one {@link Reaction}, in order to be 
 * <em>valid</em>.  If a model is not valid, an attempt to run a
 * {@link GillespieSimulator} simulation on it will result in an exception.
 * <p />
 * <a name="namespaces"></a>
 * The names of Compartments, Species, Reactions, and Parameters in a model 
 * all occupy the <b>global namespace</b>.  A namespace is defined as follows:
 * Two symbols or objects in the same namespace cannot have the same name.  
 * For example, in a {@link Model}, it is not permitted to
 * have a species and a parameter of the same name.  Or a species and a
 * reaction of the same name.
 * A parameter that is added <em>to</em> a reaction, on the other hand,
 * occupies the <b>local namespace</b> for that reaction.  It <em>is</em>
 * allowed to have a symbol of a given name, defined in both the global
 * and in a local namespace.  For example, it is permitted to define a global
 * parameter and a local parameter with the same name.  The local parameter's
 * value will supersede the value of the global parameter, in the reaction
 * where the local parameter is defined.
 * <p />
 * <a name="reservedsymbols" />
 * <b>Reserved Symbols:</b>
 * <p />
 * There is a list of symbol names that are reserved, and not permitted
 * for user definition in either the global or local namespaces.  These
 * are:  
 * <dl>
 * <dt><b>time</b>:</dt>
 * <dd>clock time since the start of the simulation, in seconds</dd>
 * <dt><b>N</b>:</dt>
 * <dd>Avogadro's constant, as defined in {@link Constants}.</dd>
 * </dl>
 * No parameter, species, reaction, or compartment may have a name that
 * matches any of the above reserved symbols.
 * <p />
 * For more details and sample code using this class, refer to the 
 * {@link GillespieSimulator} documentation.
 *
 * @see Reaction
 * @see Species
 * @see Parameter
 * @see Compartment
 * @see ISimulator
 * @see GillespieSimulator
 *
 * @author Stephen Ramsey
 */
public class Model
{
    /*========================================*
     * constants
     *========================================*/


    /**
     * Defines the symbol used to represent clock time.  No other
     * symbol in any namespace (global or local) may match this string.
     */
    public static final String SYMBOL_TIME = "time";

    /**
     * Defines the symbol used to represent Avogadro's constant.
     * No other symbol in any namespace (global or local) may match this
     * string.
     */
    public static final String SYMBOL_AVOGADRO_CONSTANT = "N";



    /*========================================*
     * inner classes
     *========================================*/

    // This is a helper class that can look up a global parameter value based
    // on the parameter ("symbol") name.  It is passed to MathExpression.computeValue().
    class SpeciesExpressionSymbolMap implements ISymbolDoubleMap
    {
        private double mTime = 0.0;

        protected void setTime(double pTime)
        {
            mTime = pTime;
        }
        
        protected double getTime()
        {
            return(mTime);
        }

        public double getValue(String pSymbol) throws DataNotFoundException
        {
            SpeciesPopulations speciesPopulations = null; // passing null instructs the getSymbolValue()
                                                          // method to throw an exception if it encounters
                                                          // a species symbol
            double retVal = getSymbolValue(pSymbol, speciesPopulations, getTime());
            return(retVal);
        }
    }


    /*========================================*
     * member data
     *========================================*/
    private Vector mReactions;
    private HashMap mParameters;
    private String mName;
    private Vector mCompartments;
    private HashMap mSymbolTable;  // the global symbol table
                                   // (contains names of species, parameters,
                                   // reactions, and containers)
    private SpeciesExpressionSymbolMap mSpeciesExpressionSymbolMap;
    private ReactionRateSpeciesMode mReactionRateSpeciesMode;  // mode of interpretation of a species symbol
                                                   // occuring in a custom rate expression:
                                                   // 1 = interpret as a concentration (moles/L)
                                                   // 2 = interpret as number of molecules

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    /**
     * Sets the mode of interpretation for a species symbol appearing
     * in a custom reaction rate expression, to the integer value
     * <code>pReactionRateSpeciesMode</code>.  
     *
     * @param pReactionRateSpeciesMode the rate expression species mode
     * to be used.  By default, the Model will use 
     * {@link ReactionRateSpeciesMode#CONCENTRATION}.
     */
    public void setReactionRateSpeciesMode(ReactionRateSpeciesMode pReactionRateSpeciesMode)
    {
        mReactionRateSpeciesMode = pReactionRateSpeciesMode;
    }
    
    /**
     * Returns the Model's current setting for the  
     * mode of interpretation for species symbols appearing
     * in a custom reaction rate expression, as an integer value.
     * By default, the Model will use 
     * {@link ReactionRateSpeciesMode#MOLECULES}.
     *
     * @return the Model's current setting for the  
     * mode of interpretation for species symbols appearing
     * in a custom reaction rate expression, as an the integer value.
     */
    public ReactionRateSpeciesMode getReactionRateSpeciesMode()
    {
        return(mReactionRateSpeciesMode);
    }

    private SpeciesExpressionSymbolMap getSpeciesExpressionSymbolMap()
    {
        return(mSpeciesExpressionSymbolMap);
    }

    private void setSpeciesExpressionSymbolMap(SpeciesExpressionSymbolMap pSpeciesExpressionSymbolMap)
    {
        mSpeciesExpressionSymbolMap = pSpeciesExpressionSymbolMap;
    }

    private void setSymbolTable(HashMap pSymbolTable)
    {
        mSymbolTable = pSymbolTable;
    }

    private HashMap getSymbolTable()
    {
        return(mSymbolTable);
    }
    
    private void setCompartments(Vector pCompartments)
    {
        mCompartments = pCompartments;
    }

    private Vector getCompartments()
    {
        return(mCompartments);
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

    private HashMap getParameters()
    {
        return(mParameters);
    }

    private void setParameters(HashMap pParameters)
    {
        mParameters = pParameters;
    }

    private Vector getReactions()
    {
        return(mReactions);
    }

    private void setReactions(Vector pReactions)
    {
        mReactions = pReactions;
    }

    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeReactionRateSpeciesMode()
    {
        setReactionRateSpeciesMode(ReactionRateSpeciesMode.MOLECULES);
    }

    private void initializeSpeciesExpressionSymbolMap()
    {
        setSpeciesExpressionSymbolMap(new SpeciesExpressionSymbolMap());
    }

    private void initializeSymbolTable()
    {
        setSymbolTable(new HashMap());
    }

    private void initializeCompartments()
    {
        setCompartments(new Vector());
    }

    private void initializeParameters()
    {
        setParameters(new HashMap());
    }

    private void initializeReactions()
    {
        setReactions(new Vector());
    }

    private void initialize()
    {
        initializeReactionRateSpeciesMode();
        initializeSpeciesExpressionSymbolMap();
        initializeSymbolTable();
        initializeReactions();
        initializeParameters();
        initializeCompartments();
    }

    /*========================================*
     * constructors
     *========================================*/
    /**
     * Constructs a new model with an empty &quot;name&quot; field.
     * To assign a name of the model, call {@link #setName(String)}.
     */
    public Model()
    {
        this("");
    }

    /**
     * Constructs a new model with name <code>pName</code>.
     */
    public Model(String pName)
    {
        initialize();
        setName(pName);
    }

    /*========================================*
     * private methods
     *========================================*/

    private void addSymbol(String pSymbolName, Object pSymbolObject) throws IllegalArgumentException
    {
        checkForValidSymbol(pSymbolName);

        HashMap symbolTable = getSymbolTable();
        Object symbolObject = symbolTable.get(pSymbolName);
        if(null != symbolObject)
        {
            // an object with name pSymbolName already exists in the symbol table
            // need to check it for sameness
            if(! pSymbolObject.equals(symbolObject))
            {
                throw new IllegalArgumentException("symbol: " + pSymbolName + " is already defined in the global symbol table");
            }
        }

        symbolTable.put(pSymbolName, pSymbolObject);
    }


    private Object getSymbol(String pSymbolName)
    {
        return(getSymbolTable().get(pSymbolName));
    }



    private static void checkForReservedCharacters(String pSymbolName) throws IllegalArgumentException
    {
        if(-1 != pSymbolName.indexOf("\""))
        {
            throw new IllegalArgumentException("symbol name cannot contain a double-quote character: " + pSymbolName);
        }
    }

    private static void checkForReservedSymbolNames(String pSymbolName) throws IllegalArgumentException
    {
        if(pSymbolName.equals(SYMBOL_TIME))
        {
            throw new IllegalArgumentException("symbol name is a reserved keyword: " + SYMBOL_TIME);
        }
        else if(pSymbolName.equals(SYMBOL_AVOGADRO_CONSTANT))
        {
            throw new IllegalArgumentException("symbol name is a reserved keyword: " + SYMBOL_AVOGADRO_CONSTANT);
        }
    }

    /*========================================*
     * protected methods
     *========================================*/


    double computeValueOfSpeciesPopulationExpression(MathExpression pSpeciesPopulationExpression,
                                                             double pTime) throws DataNotFoundException, IllegalArgumentException
    {
        SpeciesExpressionSymbolMap symbolMap = getSpeciesExpressionSymbolMap();
        symbolMap.setTime(pTime);
        double speciesPopulationValueDouble = pSpeciesPopulationExpression.computeValue(symbolMap);
        if(speciesPopulationValueDouble < ((double) (SpeciesPopulation.MIN_POPULATION)))
        {
            throw new IllegalArgumentException("expression for species population resulted in an invalid population value: " + pSpeciesPopulationExpression + "; time value: " + pTime);
        }
        return(speciesPopulationValueDouble);
    }


    void initializeReactionProbabilityDensities(Vector pReactionProbabilityDensities)
    {
        Vector reactionsVec = getReactions();
        Iterator reactionsIter = reactionsVec.iterator();
        int reactionsCtr = 0;
        while(reactionsIter.hasNext())
        {
            Reaction reaction = (Reaction) reactionsIter.next();
            pReactionProbabilityDensities.add(new ReactionProbabilityDensity(reaction));
            ++reactionsCtr;
        }
    }

 


    static void checkForValidSymbol(String pSymbolName)
    {
        if(null == pSymbolName)
        {
            throw new IllegalArgumentException("invalid (null) symbol name cannot be added to the symbol table");
        }

        // check to make sure this symbol would parse as a valid mathematical "symbol"
        // (i.e., make sure it does not contain any mathematical operators or parentheses)
        if(! MathExpression.isValidSymbol(pSymbolName))
        {
            throw new IllegalArgumentException("symbol name is not a valid symbol: " + pSymbolName);
        }

        // check for reserved symbol names (such as SYMBOL_TIME)
        checkForReservedSymbolNames(pSymbolName);

        // check for reserved characters
        checkForReservedCharacters(pSymbolName);
    }

    double getSymbolValue(String pSymbolName, SpeciesPopulations pSpeciesPopulations, double pTime) throws DataNotFoundException
    {
        double retVal = 0.0;

        if(pSymbolName.equals(SYMBOL_TIME))
        {
            // the symbol is "time"; return the time
            retVal = pTime;
        }
        else if(pSymbolName.equals(SYMBOL_AVOGADRO_CONSTANT))
        {
            retVal = Constants.AVOGADRO_CONSTANT;
        }
        else
        {
            Object symbolObject = getSymbol(pSymbolName);
            if(null != symbolObject)
            {
                if(symbolObject instanceof Parameter)
                {
                    // the symbol is a global parameter; return the parameter value
                    Parameter parameter = getParameter(pSymbolName);
                    retVal = parameter.getValue();
                }
                else if(symbolObject instanceof Species)
                {
                    if(null != pSpeciesPopulations)
                    {
                        // the symbol is a species; return the species concentration in moles/liter
                        Species species = (Species) symbolObject;
                        retVal = getSpeciesSymbolValue(species, pTime, pSpeciesPopulations);
                    }
                    else
                    {
                        throw new DataNotFoundException("could not find value for species symbol: " + pSymbolName + "; cannot use a species symbol in a species population expression");
                    }
                }
                else if(symbolObject instanceof Compartment)
                {
                    // the symbol is a compartment; return the compartment volume in liters
                    Compartment compartment = (Compartment) symbolObject;
                    retVal = compartment.getVolumeLiters();
                }
                else
                {
                    throw new IllegalStateException("global symbol encountered in a reaction rate expression, that has a disallowed type; symbol is: " + pSymbolName);
                }
            }
            else
            {
                throw new DataNotFoundException("could not find value for symbol: " + pSymbolName);
            }
        }
        return(retVal);
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
     * Returns an iterator for an ordered collection of 
     * the Reaction objects associated with this model.
     */
    Iterator getReactionsOrderedIter()
    {
        return(getReactions().iterator());
    } 

    /**
     * Returns the set of all global {@link Parameter} objects
     * defined for the model.
     *
     * @return the set of all global {@link Parameter} objects
     * defined for the model.
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

    /**
     * Returns the set of all {@link Compartment} objects
     * in the .
     *
     * @return the set of all {@link Compartment} objects
     * in the model.
     */
    static Set getCompartmentsSet(Set pSpeciesSet)
    {
        HashSet compartments = new HashSet();
        Iterator speciesIter = pSpeciesSet.iterator();
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            Compartment compartment = species.getCompartment();
            compartments.add(compartment);
        }
        return(compartments);
    }

    /**
     * Returns the set of all {@link Species} objects 
     * in the model.
     *
     * @return the set of all {@link Species} objects
     * in the model.
     */
    Set getSpeciesSet()
    {
        HashSet speciesSet = new HashSet();
        Vector reactionsVec = getReactions();
        Iterator reactionsIter = reactionsVec.iterator();
        while(reactionsIter.hasNext())
        {
            Reaction reaction = (Reaction) reactionsIter.next();
            reaction.getReactants(speciesSet);
            reaction.getProducts(speciesSet);
        }
        return(speciesSet);
    }

    /**
     * Returns the numeric value associated with a species symbol
     * appearing in a reaction rate expression.  This value is computed
     * from the species population using a method that depends on 
     * the {@link ReactionRateSpeciesMode} setting for the model.
     * If this setting is {@link ReactionRateSpeciesMode#MOLECULES}
     * (the default), then the value returned is simply the number of
     * molecules of the given species.  If the setting is
     * {@link ReactionRateSpeciesMode#CONCENTRATION}, the value
     * returned is the molar concentration of the given species.
     */
    double getSpeciesSymbolValue(Species pSpecies, double pTime, SpeciesPopulations pSpeciesPopulations) throws DataNotFoundException
    {
        double retVal = 0.0;

        // get the species population, which is either a lookup or an expression computation
        double speciesPopulation = pSpeciesPopulations.getSpeciesPopulation(pSpecies.getName(), this, pTime);
        ReactionRateSpeciesMode customRateExpressionSpeciesMode = getReactionRateSpeciesMode();
        if(customRateExpressionSpeciesMode == ReactionRateSpeciesMode.CONCENTRATION)
        {
            Compartment compartment = pSpecies.getCompartment();
            double compVolume = compartment.getVolumeLiters();
            double speciesPopulationMoles = speciesPopulation / Constants.AVOGADRO_CONSTANT;
            retVal = speciesPopulationMoles/compVolume;
        }
        else if(customRateExpressionSpeciesMode == ReactionRateSpeciesMode.MOLECULES)
        {
            retVal = speciesPopulation;
        }
        else
        {
            throw new IllegalStateException("invalid custom rate expression species mode: " + customRateExpressionSpeciesMode);
        }

        return(retVal);
    }

    /*========================================*
     * public methods
     *========================================*/

    /**
     * Returns the set of all global {@link Parameter} objects
     * defined for the model.
     *
     * @return the set of all global {@link Parameter} objects
     * defined for the model.
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
     * Returns the set of all {@link Compartment} objects
     * in the .
     *
     * @return the set of all {@link Compartment} objects
     * in the model.
     */
    static public Set getCompartmentsSetCopy(Set pSpeciesSet)
    {
        HashSet compartmentsSet = (HashSet) getCompartmentsSet(pSpeciesSet);
        HashSet newCompartmentsSet = new HashSet();
        Iterator compartmentsIter = compartmentsSet.iterator();
        while(compartmentsIter.hasNext())
        {
            Compartment compartment = (Compartment) compartmentsIter.next();
            newCompartmentsSet.add(compartment.clone());
        }
        return(newCompartmentsSet);
    }

    /**
     * Returns an iterator for an ordered collection of 
     * the Reaction objects associated with this model.
     */
    public Iterator getReactionsOrderedIterCopy()
    {
        Vector reactions = getReactions();
        Vector newReactions = new Vector();
        Iterator reactionsIter = reactions.iterator();
        while(reactionsIter.hasNext())
        {
            Reaction reaction = (Reaction) reactionsIter.next();
            newReactions.add(reaction.clone());
        }
        return(newReactions.iterator());
    } 

    /**
     * Returns the set of all {@link Species} objects 
     * in the model.
     *
     * @return the set of all {@link Species} objects
     * in the model.
     */
    public Set getSpeciesSetCopy()
    {
        HashSet speciesSet = (HashSet) getSpeciesSet();
        HashSet newSpeciesSet = new HashSet();
        Iterator speciesIter = speciesSet.iterator();
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            newSpeciesSet.add(species.clone());
        }
        return(newSpeciesSet);
    }

    /**
     * Adds the contents of the sub-model <code>pModel</code>
     * to this model.  All reactions and parameters of the
     * sub-model are added to this model, which (implicitly)
     * adds all species and compartments as well.
     *
     * @param pModel the sub-model to be added to this model
     */
    public void addSubModel(Model pModel)
    {
        // add parameters in submodel
        Collection parameters = pModel.getParameters().values();
        Iterator paramIter = parameters.iterator();
        while(paramIter.hasNext())
        {
            Parameter parameter = (Parameter) paramIter.next();
            addParameter(parameter);
        }

        Vector reactions = pModel.getReactions();
        Iterator reacIter = reactions.iterator();
        while(reacIter.hasNext())
        {
            Reaction reaction = (Reaction) reacIter.next();
            addReaction(reaction);
        }
    }

    /**
     * Validates the model for self-consistency, and sufficiency of initial data.
     * If a reaction in the model involves a species for which initial data is
     * not present in the <code>pInitialSpeciesPopulations</code> data structure,
     * an exception will be thrown.
     *
     * @param pInitialSpeciesPopulations the initial species populations (initial 
     * data for the simulation).  
     */
    public void validate(SpeciesPopulations pInitialSpeciesPopulations,
                         Double pStartTime) throws IllegalStateException, IllegalArgumentException
    {
        Iterator reactionsIter = getReactionsOrderedIter();
        if(null == pInitialSpeciesPopulations)
        {
            throw new IllegalStateException("no initial species populations data has been specified for this model");
        }

        int reactionCounter = 0;

        while(reactionsIter.hasNext())
        {
            ++reactionCounter;

            Reaction reaction = (Reaction) reactionsIter.next();

            reaction.validateInitialData(pInitialSpeciesPopulations, this, pStartTime);

            try
            {
                reaction.validate();
            }
            catch(Exception e)
            {
                throw new IllegalStateException("invalid reaction found; error message is: " + e.toString() + "; reaction is: " + reaction.toString());
            }
        }

        if(0 == reactionCounter)
        {
            throw new IllegalStateException("no reactions have been specified for this model");
        }
    }

    /**
     * Adds the parameter <code>pParameter</code> to the model, which
     * contains a parameter name and a parameter value.  The parameter
     * must be unique in the global namespace for the Model, or else
     * an exception will be thrown.
     * These parameters are symbols that may appear in rate function
     * definitions for {@linkplain Reaction reactions}.
     *
     * @param pParameter the parameter to add to the model
     */
    public void addParameter(Parameter pParameter)
    {
        String parameterName = pParameter.getName();
        getParameters().put(parameterName, pParameter);
        addSymbol(parameterName, pParameter);
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
     * Adds a specific {@link Reaction} to this model.  The reaction
     * must be unique, for this given model.  This means that if you
     * call <code>addReaction(myReaction)</code> twice for the
     * <code>myReaction</code> object, the second call does nothing.
     * 
     * @param pReaction the reaction to be added to the model
     */
    public void addReaction(Reaction pReaction) throws IllegalArgumentException, IllegalStateException
    {
        pReaction.validate();

        Iterator reactionsIter = getReactionsOrderedIter();
        boolean alreadyStoredInVector = false;
        while(reactionsIter.hasNext())
        {
            Reaction reaction = (Reaction) reactionsIter.next();
            if(reaction.equals(pReaction))
            {
                alreadyStoredInVector = true;
            }
        }
        if(! alreadyStoredInVector)
        {
            getReactions().add(pReaction);
        }

        // get the set of all species in this reaction
        HashSet speciesSet = new HashSet();
        pReaction.getReactants(speciesSet);
        pReaction.getProducts(speciesSet);

        // for each species in the reaction, check to make sure 
        // the symbol name is not taken up by a different entity
        // (e.g., a reaction, compartment, or different species)
        Iterator speciesIter = speciesSet.iterator();
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            addSymbol(species.getName(), species);
            
            Compartment compartment = species.getCompartment();
            addSymbol(compartment.getName(), compartment);
        }

        addSymbol(pReaction.getName(), pReaction);


    }


    /**
     * Returns a human-readable string representation of the model.
     *
     * @return a human-readable string representation of the model.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("modelName: " + getName() + "\n\n");
        sb.append("reaction-rate-species-mode: " + getReactionRateSpeciesMode() + "\n\n");
        sb.append("reactions: \n");
        Vector reactions = getReactions();
        List reactionsList = new LinkedList(reactions);
        Collections.sort(reactionsList);
        Iterator reactionIter = reactionsList.iterator();
        int reactionCtr = 0;
        while(reactionIter.hasNext())
        {
            Reaction reaction = (Reaction) reactionIter.next();
            sb.append("  (" + reactionCtr + ") " + reaction.toString() + "\n");
            ++reactionCtr;
        }
        sb.append("\n  compartments: \n");
        Set speciesSet = getSpeciesSet();
        List speciesList = new LinkedList(speciesSet);
        Collections.sort(speciesList);
        Set compartments = getCompartmentsSet(speciesSet);
        List compartmentsList = new LinkedList(compartments);
        Collections.sort(compartmentsList);
        Iterator compIter = compartmentsList.iterator();
        int compCtr = 0;
        while(compIter.hasNext())
        {
            Compartment comp = (Compartment) compIter.next();
            sb.append("  (" + compCtr + ") " + comp.toString() + "\n");
            ++compCtr;
        }
        sb.append("\n  species: \n");
        Iterator speciesIter = speciesList.iterator();
        int speciesCtr = 0;
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            sb.append("  (" + speciesCtr + ") " + species.toString() + "\n");
            ++speciesCtr;
        }
        sb.append("\n  parameters: \n");
        HashMap paramsMap = getParameters();
        Set paramNames = paramsMap.keySet();
        List paramNamesList = new LinkedList(paramNames);
        Collections.sort(paramNamesList);
        Iterator paramsIter = paramNamesList.iterator();
        while(paramsIter.hasNext())
        {
            String paramName = (String) paramsIter.next();
            Parameter parameter = (Parameter) paramsMap.get(paramName);
            double paramValue = parameter.getValue();
            sb.append("  " + parameter.toString() + "\n");
        }
        return(sb.toString());
    }

}

