package org.systemsbiology.chem;

import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;

import org.systemsbiology.math.Expression;
import org.systemsbiology.math.SymbolValue;
import org.systemsbiology.math.Value;
import org.systemsbiology.math.Symbol;
import org.systemsbiology.math.SymbolEvaluator;
import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.DebugUtils;

/**
 * Represents a chemical process that can take
 * place, transforming zero or more reactant
 * {@link Species} into zero or more product
 * {@link Species}.
 *
 * @author Stephen Ramsey
 */
public class Reaction extends SymbolValue
{
    public static class ParticipantType 
    {
        private final String mName;
        private ParticipantType(String pName)
        {
            mName = pName;
        }
        
        public String toString()
        {
            return(mName);
        }

        public static final ParticipantType REACTANT = new ParticipantType("reactant");
        public static final ParticipantType PRODUCT = new ParticipantType("product");
    }


    class ReactionElement implements Comparable
    {
        private final Species mSpecies;
        private final int mStoichiometry;
        private final boolean mDynamic;

        public ReactionElement(Species pSpecies, int pStoichiometry, boolean pDynamic) throws IllegalArgumentException
        {
            if(pSpecies.getValue().isExpression() && pDynamic == true)
            {
                throw new IllegalArgumentException("attempt to use a species with a population expression, as a dynamic element of a reaction");
            }
            if(pStoichiometry < 1)
            {
                throw new IllegalArgumentException("illegal stoichiometry value: " + pStoichiometry);
            }

            mStoichiometry = pStoichiometry;
            mSpecies = pSpecies;
            mDynamic = pDynamic;
        }

        public int getStoichiometry()
        {
            return(mStoichiometry);
        }

        public boolean getDynamic()
        {
            return(mDynamic);
        }

        public Species getSpecies()
        {
            return(mSpecies);
        }

        public boolean equals(ReactionElement pReactionElement)
        {
            return( mSpecies.equals(pReactionElement.mSpecies) &&
                    mStoichiometry == pReactionElement.mStoichiometry &&
                    mDynamic == pReactionElement.mDynamic );
        }

        public int compareTo(Object pReactionElement)
        {
            return(mSpecies.getName().compareTo(((ReactionElement) pReactionElement).mSpecies.getName()));
        }

        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("ReactionElement[");
            sb.append(mSpecies.toString());
            sb.append(", Stoichiometry: ");
            sb.append(mStoichiometry);
            sb.append(", Dynamic: ");
            sb.append( mDynamic );
            sb.append("]");
            return(sb.toString());
        }
    }

    private String mName;
    private HashMap mReactantsMap;
    private HashMap mProductsMap;
    private static final boolean DEFAULT_REACTANT_DYNAMIC = true;
    private double []mDynamicSymbolAdjustmentVector;
    private Species []mReactantsSpeciesArray;
    private int []mReactantsStoichiometryArray;
    private HashMap mLocalSymbolsValuesMap;
    private HashMap mLocalSymbolsMap;
    private Value []mLocalSymbolsValues;
    private boolean mRateIsExpression;

    public Object clone()
    {
        Reaction reaction = new Reaction(mName);
        reaction.setRate((Value) getRate().clone());
        reaction.mReactantsMap = mReactantsMap;
        reaction.mProductsMap = mProductsMap;
        reaction.mDynamicSymbolAdjustmentVector = mDynamicSymbolAdjustmentVector;
        reaction.mReactantsSpeciesArray = null;
        reaction.mReactantsStoichiometryArray = null;
        reaction.mLocalSymbolsValuesMap = mLocalSymbolsValuesMap;
        reaction.mLocalSymbolsMap = mLocalSymbolsMap;
        reaction.mLocalSymbolsValues = mLocalSymbolsValues;
        reaction.mRateIsExpression = mRateIsExpression;
        return(reaction);
    }

    public Reaction(String pName)
    {
        super(pName);
        mName = pName;
        mReactantsMap = new HashMap();
        mProductsMap = new HashMap();
        mLocalSymbolsValuesMap = new HashMap();
        mLocalSymbolsMap = null;
        mLocalSymbolsValues = null;
        mDynamicSymbolAdjustmentVector = null;
        mReactantsSpeciesArray = null;
        mReactantsStoichiometryArray = null;
        mRateIsExpression = false;
    }

    double []getDynamicSymbolAdjustmentVector()
    {
        return(mDynamicSymbolAdjustmentVector);
    }

    public Collection getParameters()
    {
        return(mLocalSymbolsValuesMap.values());
    }

    public void addParameter(Parameter pParameter)
    {
        SymbolValueChemSimulation.addSymbolValueToMap(mLocalSymbolsValuesMap, pParameter.getSymbolName(), pParameter);
    }

    public int getNumParticipants(ParticipantType pParticipantType)
    {
        int numParticipants = 0;
        if(pParticipantType.equals(ParticipantType.REACTANT))
        {
            numParticipants = mReactantsMap.values().size();
        }
        else
        {
            numParticipants = mProductsMap.values().size();
        }
        return(numParticipants);
    }

    public void constructSpeciesArrays(Species []pSpeciesArray,    // this is the array of species that we are constructing
                                       int []pStoichiometryArray,  // this is the array of stoichiometries that we are constructing
                                       Species []pDynamicSymbolValues, // a vector of all species in the model
                                       HashMap pSymbolMap,         // a map between species names and the index in previous vector
                                       ParticipantType pParticipantType)
    {
        Collection reactantsColl = null;
        if(pParticipantType.equals(ParticipantType.REACTANT))
        {
            reactantsColl = mReactantsMap.values();
        }
        else
        {
            reactantsColl = mProductsMap.values();
        }
        int numReactants = reactantsColl.size();
        if(pSpeciesArray.length < numReactants)
        {
            throw new IllegalArgumentException("insufficient array size");
        }
        if(pStoichiometryArray.length < numReactants)
        {
            throw new IllegalArgumentException("insufficient array size");
        }
        Iterator reactantsIter = reactantsColl.iterator();

        int reactantCtr = 0;
        while(reactantsIter.hasNext())
        {
            ReactionElement element = (ReactionElement) reactantsIter.next();
            Species species = element.mSpecies;
            if(null != pSymbolMap)
            {
                String speciesName = species.getName();
                Symbol extSymbol = (Symbol) pSymbolMap.get(speciesName);
                int extSpeciesIndex = extSymbol.getArrayIndex();
                if(Symbol.NULL_ARRAY_INDEX == extSpeciesIndex)
                {
                    throw new IllegalStateException("invalid array index for species: " + speciesName);
                }
                species = pDynamicSymbolValues[extSpeciesIndex];
            }
            else
            {
                // do nothing
            }
            pSpeciesArray[reactantCtr] = species;
            
            int stoic = element.mStoichiometry;
            pStoichiometryArray[reactantCtr] = stoic;

            reactantCtr++;
        }
    }

    void prepareSymbolVectorsForSimulation(Species []pDynamicSymbolValues, HashMap pSymbolMap)
    {
        constructDynamicSymbolAdjustmentVector(pDynamicSymbolValues);

        int numReactants = mReactantsMap.values().size();
        mReactantsSpeciesArray = new Species[numReactants];
        mReactantsStoichiometryArray = new int[numReactants];

        constructSpeciesArrays(mReactantsSpeciesArray, 
                               mReactantsStoichiometryArray, 
                               pDynamicSymbolValues, 
                               pSymbolMap, 
                               ParticipantType.REACTANT);

        constructLocalSymbolsVector();
    }

    private void constructLocalSymbolsVector()
    {
        Collection localSymbols = mLocalSymbolsValuesMap.values();
        int numSymbols = localSymbols.size();
        SymbolValue []localSymbolsArray = new SymbolValue[numSymbols];
        int symCtr = 0;
        Iterator symIter = localSymbols.iterator();
        while(symIter.hasNext())
        {
            SymbolValue symValue = (SymbolValue) symIter.next();
            localSymbolsArray[symCtr] = symValue;
            symCtr++;
        }
        Value []mLocalSymbolsValues = new Value[numSymbols];
        HashMap localSymbolsMap = new HashMap();
        Simulator.indexSymbolArray(localSymbolsArray,
                                   localSymbolsMap,
                                   null,
                                   mLocalSymbolsValues);
        mLocalSymbolsMap = localSymbolsMap;
    }

    private void constructDynamicSymbolAdjustmentVector(SymbolValue []pDynamicSymbols)
    {
        int numSymbols = pDynamicSymbols.length;
        double []dynamicSymbolVector = new double[numSymbols];

        for(int symbolCtr = 0; symbolCtr < numSymbols; ++symbolCtr)
        {
            SymbolValue symbolValue = pDynamicSymbols[symbolCtr];
            Symbol symbol = symbolValue.getSymbol();
            String symbolName = symbol.getName();
            double vecElement = 0.0;
            ReactionElement reactantElement = (ReactionElement) mReactantsMap.get(symbolName);
            if(null != reactantElement && reactantElement.mDynamic)
            {
                vecElement -= reactantElement.mStoichiometry;
            }
            ReactionElement productElement = (ReactionElement) mProductsMap.get(symbolName);
            if(null != productElement && productElement.mDynamic)
            {
                vecElement += productElement.mStoichiometry;
            }

            dynamicSymbolVector[symbolCtr] = vecElement;
        }

        mDynamicSymbolAdjustmentVector = dynamicSymbolVector;
    }

    private void addSymbolsToGlobalSymbolsMap(HashMap pReactionSpecies, HashMap pSymbols)
    {
        Collection speciesCollection = pReactionSpecies.values();
        Iterator speciesIter = speciesCollection.iterator();
        while(speciesIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) speciesIter.next();
            Species species = reactionElement.getSpecies();
            String speciesSymbolName = species.getSymbol().getName();

            SymbolValueChemSimulation.addSymbolValueToMap(pSymbols, speciesSymbolName, species);

        }
    }

    private void addDynamicSpeciesFromReactionSpeciesMapToGlobalSpeciesMap(HashMap pReactionSpecies, HashMap pDynamicSpecies)
    {
        Collection speciesCollection = pReactionSpecies.values();
        Iterator speciesIter = speciesCollection.iterator();
        while(speciesIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) speciesIter.next();
            if(reactionElement.getDynamic())
            {
                Species species = reactionElement.getSpecies();
                String speciesSymbolName = species.getSymbol().getName();
                SymbolValueChemSimulation.addSymbolValueToMap(pDynamicSpecies, speciesSymbolName, species);
            }
        }
    }

    void addDynamicSpeciesToGlobalSpeciesMap(HashMap pDynamicSpecies)
    {
        addDynamicSpeciesFromReactionSpeciesMapToGlobalSpeciesMap(getReactantsMap(), pDynamicSpecies);
        addDynamicSpeciesFromReactionSpeciesMapToGlobalSpeciesMap(getProductsMap(), pDynamicSpecies);
    }
    
    void addSymbolsFromReactionSpeciesMapToGlobalSymbolMap(HashMap pReactionSpeciesMap, HashMap pSymbolMap)
    {
        Collection speciesCollection = pReactionSpeciesMap.values();
        Iterator speciesIter = speciesCollection.iterator();
        while(speciesIter.hasNext())
        {
            ReactionElement reactionElement = (ReactionElement) speciesIter.next();
            Species species = reactionElement.getSpecies();
            species.addSymbolsToGlobalSymbolMap(pSymbolMap);
        }        
    }

    void addSymbolsToGlobalSymbolMap(HashMap pSymbolMap)
    {
        addSymbolsFromReactionSpeciesMapToGlobalSymbolMap(getReactantsMap(), pSymbolMap);
        addSymbolsFromReactionSpeciesMapToGlobalSymbolMap(getProductsMap(), pSymbolMap);
    }

    public Symbol getSymbol()
    {
        return(mSymbol);
    }

    HashMap getReactantsMap()
    {
        return(mReactantsMap);
    }

    HashMap getProductsMap()
    {
        return(mProductsMap);
    }

    public boolean equals(Reaction pReaction)
    {
        return(mName.equals(pReaction.mName) &&
               mReactantsMap.equals(pReaction.mReactantsMap) &&
               mProductsMap.equals(pReaction.mProductsMap) &&
               super.equals(pReaction));
    }

    public String getName()
    {
        return(mName);
    }

    Value getRate()
    {
        return(getValue());
    }

    public void setRate(Value pRate)
    {
        setValue(pRate);
        mRateIsExpression = pRate.isExpression();
    }

    public void setRate(double pRate)
    {
        setRate(new Value(pRate));
    }

    public void setRate(Expression pRate)
    {
        setRate(new Value(pRate));
    }

    void checkReactantValues(SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException
    {
        Species []reactants = mReactantsSpeciesArray;
        int numReactants = reactants.length;

        for(int reactantCtr = numReactants; --reactantCtr >= 0; )
        {
            SymbolValue reactant = reactants[reactantCtr];
            Symbol symbol = reactant.getSymbol();
            double value = pSymbolEvaluator.getValue(reactant.getSymbol());
            String symbolName = symbol.getName();
        }
    }

    /**
     * The following design choices were motivated by performance:
     * 
     * (1) Used abstract class references, rather than interface references, 
     * for the arguments to this method. 
     * (2) Reaction loop counts down, so that the integer comparison is with zero.
     * (3) Using "*=" operator which is faster than the 'A = A * X" expression.
     *
     */
    final double computeRate(SpeciesRateFactorEvaluator pSpeciesRateFactorEvaluator,
                             SymbolEvaluatorChemSimulation pSymbolEvaluator) throws DataNotFoundException
    {
        Value rateValue = mValue;
        double rate = 0.0;

        if(! mRateIsExpression)
        {
            Species []reactants = mReactantsSpeciesArray;
            int []stoichiometries = mReactantsStoichiometryArray;
            rate = rateValue.getValue();

            int numReactants = reactants.length;

            double numReactantCombinations = 1.0;

            for(int reactantCtr = numReactants; --reactantCtr >= 0; )
            {
                numReactantCombinations *= pSpeciesRateFactorEvaluator.computeRateFactorForSpecies(pSymbolEvaluator,
                                                                                                   reactants[reactantCtr], 
                                                                                                   stoichiometries[reactantCtr]);
            }

            rate *= numReactantCombinations;
        }
        else
        {
            pSymbolEvaluator.setLocalSymbolsMap(mLocalSymbolsMap);
            rate = rateValue.getValue(pSymbolEvaluator);
            pSymbolEvaluator.setLocalSymbolsMap(null);
        }

        return(rate);
    }

    public void addReactionElementToMap(ReactionElement pReactionElement, HashMap pMap) throws IllegalArgumentException
    {
        Species species = pReactionElement.getSpecies();
        if(null == species.getValue())
        {
            throw new IllegalArgumentException("species has no initial value defined");
        }
        String speciesSymbolName = species.getName();
        ReactionElement reactionElement = (ReactionElement) pMap.get(speciesSymbolName);
        if(null != reactionElement)
        {
            throw new IllegalStateException("Species is already defined for this reaction.  Species name: " + speciesSymbolName);
        }

        pMap.put(speciesSymbolName, pReactionElement);
    }

    void addReactant(ReactionElement pReactionElement) throws IllegalStateException
    {
        addReactionElementToMap(pReactionElement, mReactantsMap);
    }

    public void addReactant(Species pSpecies, int pStoichiometry, boolean pDynamic) throws IllegalStateException
    {
        addReactant(new ReactionElement(pSpecies, pStoichiometry, pDynamic));
    }

    public void addReactant(Species pSpecies, int pStoichiometry) throws IllegalStateException
    {
        addReactant(new ReactionElement(pSpecies, pStoichiometry, DEFAULT_REACTANT_DYNAMIC));
    }


    void addProduct(ReactionElement pReactionElement) throws IllegalStateException
    {
        addReactionElementToMap(pReactionElement, mProductsMap);
    }

    public void addProduct(Species pSpecies, int pStoichiometry) throws IllegalStateException
    {
        boolean dynamic = true;
        addProduct(new ReactionElement(pSpecies, pStoichiometry, dynamic));
    }

    public void addProduct(Species pSpecies, int pStoichiometry, boolean pDynamic) throws IllegalStateException
    {
        addProduct(new ReactionElement(pSpecies, pStoichiometry, pDynamic));
    }

    public void addSpecies(Species pSpecies, 
                           int pStoichiometry, 
                           boolean pDynamic, 
                           ParticipantType pParticipantType) throws IllegalArgumentException
    {
        if(pParticipantType.equals(ParticipantType.REACTANT))
        {
            addReactant(pSpecies, pStoichiometry, pDynamic);
        }
        else if(pParticipantType.equals(ParticipantType.PRODUCT))
        {
            addProduct(pSpecies, pStoichiometry, pDynamic);
        }
        else
        {
            throw new IllegalArgumentException("unknown reaction participant type: " + pParticipantType);
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Reaction: ");
        sb.append(getName());
        sb.append(" [Rate: ");
        sb.append(getRate().toString());
        sb.append(", Reactants: ");
        DebugUtils.describeSortedObjectList(sb, mReactantsMap);
        sb.append(", Products: ");
        DebugUtils.describeSortedObjectList(sb, mProductsMap);
        sb.append(", Parameters: ");
        DebugUtils.describeSortedObjectList(sb, mLocalSymbolsValuesMap);
        sb.append("]");
        return(sb.toString());
    }
}
