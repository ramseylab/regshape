package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;
import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

/**
 * Represents a chemical process that can take
 * place, transforming zero or more reactant
 * {@link Species} into zero or more product
 * {@link Species}.
 *
 * @author Stephen Ramsey
 */
public final class Reaction extends SymbolValue
{
    private final String mName;
    private HashMap mReactantsMap;
    private HashMap mProductsMap;
    private HashMap mLocalSymbolsValuesMap;
    private int mNumSteps;
    private double mDelay;

    private static final boolean DEFAULT_REACTANT_DYNAMIC = true;

    private HashMap mLocalSymbolsMap;
    private boolean mRateIsExpression;

    public Object clone()
    {
        Reaction reaction = new Reaction(mName);
        reaction.setRate((Value) getRate().clone());
        reaction.mReactantsMap = mReactantsMap;
        reaction.mProductsMap = mProductsMap;
        reaction.mLocalSymbolsValuesMap = mLocalSymbolsValuesMap;
        reaction.mRateIsExpression = mRateIsExpression;
        reaction.mNumSteps = mNumSteps;
        reaction.mDelay = mDelay;
        return(reaction);
    }

    public Reaction(String pName)
    {
        super(pName);
        mName = pName;
        mReactantsMap = new HashMap();
        mProductsMap = new HashMap();
        mLocalSymbolsValuesMap = new HashMap();
        mRateIsExpression = false;
        mNumSteps = 1;
        mDelay = 0.0;
    }

    boolean containsReactant(String pReactantName)
    {
        return(null != mReactantsMap.get(pReactantName));
    }


    public Collection getParameters()
    {
        return(mLocalSymbolsValuesMap.values());
    }

    public void addParameter(Parameter pParameter)
    {
        pParameter.addSymbolToMap(mLocalSymbolsValuesMap, pParameter.getSymbolName());
    }

    public boolean hasLocalSymbols()
    {
        return(mLocalSymbolsValuesMap.keySet().size() > 0);
    }

    public void setNumSteps(int pNumSteps)
    {
        if(pNumSteps < 1)
        {
            throw new IllegalArgumentException("the number of steps for a reaction must be greater than or equal to one");
        }

        mNumSteps = pNumSteps;
    }

    public double getDelay()
    {
        return(mDelay);
    }

    public void setDelay(double pDelay)
    {
        if(pDelay < 0.0)
        {
            throw new IllegalArgumentException("the delay time is invalid");
        }
        mDelay = pDelay;
    }

    public int getNumSteps()
    {
        return(mNumSteps);
    }

    public int getNumParticipants(ReactionParticipant.Type pParticipantType)
    {
        int numParticipants = 0;
        if(pParticipantType.equals(ReactionParticipant.Type.REACTANT))
        {
            numParticipants = mReactantsMap.values().size();
        }
        else
        {
            numParticipants = mProductsMap.values().size();
        }
        return(numParticipants);
    }

    static Species getIndexedSpecies(Species pSpecies,
                                     HashMap pSymbolMap,
                                     Species []pDynamicSymbolValues,
                                     SymbolValue []pNonDynamicSymbolValues) throws IllegalStateException
    {
        String speciesName = pSpecies.getName();
        Symbol extSymbol = (Symbol) pSymbolMap.get(speciesName);
        int extSpeciesIndex = extSymbol.getArrayIndex();
        Species species = null;
        if(null != extSymbol.getDoubleArray())
        {
            species = pDynamicSymbolValues[extSpeciesIndex];
        }
        else
        {
            species = (Species) pNonDynamicSymbolValues[extSpeciesIndex];
        }
        return(species);
    }
                

    public void constructSpeciesArrays(Species []pSpeciesArray,    // this is the array of species that we are constructing
                                       int []pStoichiometryArray,  // this is the array of stoichiometries that we are constructing
                                       boolean []pDynamicArray,
                                       ReactionParticipant.Type pParticipantType)
    {
        constructSpeciesArrays(pSpeciesArray,
                               pStoichiometryArray,
                               pDynamicArray,
                               null, null, null,
                               pParticipantType);
    }

    void constructSpeciesArrays(Species []pSpeciesArray,    // this is the array of species that we are constructing
                                int []pStoichiometryArray,  // this is the array of stoichiometries that we are constructing
                                boolean []pDynamicArray,
                                Species []pDynamicSymbolValues, // a vector of all species in the model
                                SymbolValue []pNonDynamicSymbolValues,
                                HashMap pSymbolMap,         // a map between species names and the index in previous vector
                                ReactionParticipant.Type pParticipantType)
    {
        Collection speciesColl = null;
        if(pParticipantType.equals(ReactionParticipant.Type.REACTANT))
        {
            speciesColl = mReactantsMap.values();
        }
        else
        {
            speciesColl = mProductsMap.values();
        }
        int numSpecies = speciesColl.size();
        if(pSpeciesArray.length < numSpecies)
        {
            throw new IllegalArgumentException("insufficient array size");
        }
        if(pStoichiometryArray.length < numSpecies)
        {
            throw new IllegalArgumentException("insufficient array size");
        }
        Iterator speciesIter = speciesColl.iterator();

        int reactantCtr = 0;
        while(speciesIter.hasNext())
        {
            ReactionParticipant participant = (ReactionParticipant) speciesIter.next();
            Species species = participant.mSpecies;
            if(null != pSymbolMap)
            {
                species = getIndexedSpecies(species,
                                            pSymbolMap,
                                            pDynamicSymbolValues,
                                            pNonDynamicSymbolValues);
            }
            else
            {
                // do nothing
            }
            pSpeciesArray[reactantCtr] = species;
            pDynamicArray[reactantCtr] = participant.mDynamic;
            int stoic = participant.mStoichiometry;
            pStoichiometryArray[reactantCtr] = stoic;

            reactantCtr++;
        }
    }

    public int getNumReactants()
    {
        return(mReactantsMap.values().size());
    }

    public int getNumProducts()
    {
        return(mProductsMap.values().size());
    }

    public int getNumLocalSymbols()
    {
        return(mLocalSymbolsValuesMap.values().size());
    }

    SymbolValue []getLocalSymbolValues()
    {
        SymbolValue []dummyArray = new SymbolValue[0];
        return((SymbolValue []) mLocalSymbolsValuesMap.values().toArray(dummyArray));
    }


    private void addSymbolsToGlobalSymbolsMap(HashMap pReactionSpecies, HashMap pSymbols, ReservedSymbolMapper pReservedSymbolMapper)
    {
        Collection speciesCollection = pReactionSpecies.values();
        Iterator speciesIter = speciesCollection.iterator();
        while(speciesIter.hasNext())
        {
            ReactionParticipant reactionParticipant = (ReactionParticipant) speciesIter.next();
            Species species = reactionParticipant.getSpecies();
            String speciesSymbolName = species.getSymbol().getName();

            species.addSymbolToMap(pSymbols, speciesSymbolName, pReservedSymbolMapper);
        }
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

    public void addReactionParticipantToMap(ReactionParticipant pReactionParticipant, HashMap pMap) throws IllegalArgumentException
    {
        Species species = pReactionParticipant.getSpecies();
        if(null == species.getValue())
        {
            throw new IllegalArgumentException("species has no initial value defined");
        }
        String speciesSymbolName = species.getName();
        ReactionParticipant reactionParticipant = (ReactionParticipant) pMap.get(speciesSymbolName);
        if(null != reactionParticipant)
        {
            throw new IllegalStateException("Species is already defined for this reaction.  Species name: " + speciesSymbolName);
        }

        pMap.put(speciesSymbolName, pReactionParticipant);
    }

    void addReactant(ReactionParticipant pReactionParticipant) throws IllegalStateException
    {
        addReactionParticipantToMap(pReactionParticipant, mReactantsMap);
    }

    public void addReactant(Species pSpecies, int pStoichiometry, boolean pDynamic) throws IllegalStateException
    {
        addReactant(new ReactionParticipant(pSpecies, pStoichiometry, pDynamic));
    }

    public void addReactant(Species pSpecies, int pStoichiometry) throws IllegalStateException
    {
        addReactant(new ReactionParticipant(pSpecies, pStoichiometry, DEFAULT_REACTANT_DYNAMIC));
    }


    void addProduct(ReactionParticipant pReactionParticipant) throws IllegalStateException
    {
        addReactionParticipantToMap(pReactionParticipant, mProductsMap);
    }

    public void addProduct(Species pSpecies, int pStoichiometry) throws IllegalStateException
    {
        boolean dynamic = true;
        addProduct(new ReactionParticipant(pSpecies, pStoichiometry, dynamic));
    }

    public void addProduct(Species pSpecies, int pStoichiometry, boolean pDynamic) throws IllegalStateException
    {
        addProduct(new ReactionParticipant(pSpecies, pStoichiometry, pDynamic));
    }

    public void addSpecies(Species pSpecies, 
                           int pStoichiometry, 
                           boolean pDynamic, 
                           ReactionParticipant.Type pParticipantType) throws IllegalArgumentException
    {
        if(pParticipantType.equals(ReactionParticipant.Type.REACTANT))
        {
            addReactant(pSpecies, pStoichiometry, pDynamic);
        }
        else if(pParticipantType.equals(ReactionParticipant.Type.PRODUCT))
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
// ---------------------------------------------------------------------
// FOR DEBUGGING PURPOSES:
        sb.append("Reaction: ");

        Iterator reactantsIter = mReactantsMap.keySet().iterator();
        sb.append(getName() + ", ");
        while(reactantsIter.hasNext())
        {
            String reactant = (String) reactantsIter.next();
            ReactionParticipant participant = (ReactionParticipant) mReactantsMap.get(reactant);
            int stoic = participant.mStoichiometry;
            boolean dynamic = participant.mDynamic;
            for(int i = 0; i < stoic; ++i)
            {
                if(! dynamic)
                {
                    sb.append("$");
                }
                sb.append(reactant);
                if(i < stoic - 1)
                {
                    sb.append(" + ");
                }
            }
            if(reactantsIter.hasNext())
            {
                sb.append(" + ");
            }
        }
        sb.append(" -> ");
        Iterator productsIter = getProductsMap().keySet().iterator();
        while(productsIter.hasNext())
        {
            String product = (String) productsIter.next();
            ReactionParticipant participant = (ReactionParticipant) mProductsMap.get(product);
            int stoic = participant.mStoichiometry;
            for(int i = 0; i < stoic; ++i)
            {
                sb.append(product);
                if(i < stoic - 1)
                {
                    sb.append(" + ");
                }
            }
            if(productsIter.hasNext())
            {
                sb.append(" + ");
            }
        }
        sb.append(", ");
        sb.append(" [Rate: ");
        sb.append(getRate().toString());
        sb.append("]");
        return(sb.toString());
    }


    private void addSymbolsFromReactionSpeciesMapToGlobalSymbolMap(HashMap pReactionSpeciesMap, HashMap pSymbolMap, ReservedSymbolMapper pReservedSymbolMapper)
    {
        Collection speciesCollection = pReactionSpeciesMap.values();
        Iterator speciesIter = speciesCollection.iterator();
        while(speciesIter.hasNext())
        {
            ReactionParticipant reactionParticipant = (ReactionParticipant) speciesIter.next();
            Species species = reactionParticipant.getSpecies();
            species.addSymbolsToGlobalSymbolMap(pSymbolMap, pReservedSymbolMapper);
        }        
    }

    void addSymbolsToGlobalSymbolMap(HashMap pSymbolMap, 
                                     ReservedSymbolMapper pReservedSymbolMapper)
    {
        addSymbolsFromReactionSpeciesMapToGlobalSymbolMap(getReactantsMap(), pSymbolMap, pReservedSymbolMapper);
        addSymbolsFromReactionSpeciesMapToGlobalSymbolMap(getProductsMap(), pSymbolMap, pReservedSymbolMapper);
    }

    private void addDynamicSpeciesFromReactionSpeciesMapToGlobalSpeciesMap(HashMap pReactionSpecies, HashMap pDynamicSpecies, ReservedSymbolMapper pReservedSymbolMapper)
    {
        Collection speciesCollection = pReactionSpecies.values();
        Iterator speciesIter = speciesCollection.iterator();
        while(speciesIter.hasNext())
        {
            ReactionParticipant reactionParticipant = (ReactionParticipant) speciesIter.next();
            if(reactionParticipant.getDynamic())
            {
                Species species = reactionParticipant.getSpecies();
                String speciesSymbolName = species.getSymbol().getName();
                species.addSymbolToMap(pDynamicSpecies, speciesSymbolName, pReservedSymbolMapper);
            }
        }
    }
    

    void addDynamicSpeciesToGlobalSpeciesMap(HashMap pDynamicSpecies, 
                                             ReservedSymbolMapper pReservedSymbolMapper)
    {
        addDynamicSpeciesFromReactionSpeciesMapToGlobalSpeciesMap(getReactantsMap(), pDynamicSpecies, pReservedSymbolMapper);
        addDynamicSpeciesFromReactionSpeciesMapToGlobalSpeciesMap(getProductsMap(), pDynamicSpecies, pReservedSymbolMapper);
    }


    Expression getRateExpression() throws DataNotFoundException
    {
        Expression retVal = null;

        Value rateValue = getRate();

        if(rateValue.isExpression())
        {
            retVal = rateValue.getExpressionValue();
        }
        else
        {
            Iterator reactantsIter = mReactantsMap.values().iterator();

            StringBuffer expBuf = new StringBuffer();

            boolean firstReactant = true;
            while(reactantsIter.hasNext())
            {
                ReactionParticipant participant = (ReactionParticipant) reactantsIter.next();
                Species species = participant.mSpecies;
                int stoic = participant.mStoichiometry;
                String speciesName = species.getName();
                if(! firstReactant)
                {
                    expBuf.append("*");
                }
                else
                {
                    firstReactant = false;
                }
                if(stoic > 1)
                {
                    expBuf.append(speciesName + "^" + stoic);
                }
                else
                {
                    expBuf.append(speciesName);
                }
            }


            double rateVal = rateValue.getValue();
            Expression rateExp = new Expression(rateVal);
            if(expBuf.length() > 0)
            {
                retVal = Expression.multiply(rateExp, new Expression(expBuf.toString()));
            }
            else
            {
                retVal = rateExp;
            }
        }
        return(retVal);
    }


    Expression computeRatePartialDerivativeExpression(Expression pRateExpression,
                                                      SymbolValue pSymbolValue,
                                                      SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        SymbolEvaluationPostProcessor symbolEvaluationPostProcessor = pSymbolEvaluator.getSymbolEvaluationPostProcessor();
        boolean speciesSymbolsRepresentConcentration = (null != symbolEvaluationPostProcessor && (symbolEvaluationPostProcessor instanceof SymbolEvaluationPostProcessorChemMarkupLanguage));

        Symbol derivSymbol = pSymbolValue.getSymbol();
        pSymbolEvaluator.setLocalSymbolsMap(mLocalSymbolsMap);
        Expression retVal = pRateExpression.computePartialDerivative(derivSymbol, pSymbolEvaluator);
        if(speciesSymbolsRepresentConcentration && (pSymbolValue instanceof Species))
        {
            Species species = (Species) pSymbolValue;
            Compartment compartment = species.getCompartment();
            Value compartmentVolumeValue = compartment.getValue();
            if(compartmentVolumeValue.isExpression())
            {
                Expression compartmentVolumeExpression = compartmentVolumeValue.getExpressionValue();
                Expression compartmentVolumeDeriv = compartmentVolumeExpression.computePartialDerivative(derivSymbol,
                                                                                                         pSymbolEvaluator);
                if(! compartmentVolumeDeriv.isSimpleNumber()  || compartmentVolumeDeriv.getSimpleNumberValue() != 0.0)
                {
                    Expression speciesExpression = new Expression(species.getName());
                    Expression concExpression = Expression.divide(speciesExpression, compartmentVolumeExpression);
                    Expression concDerivExpression = Expression.divide(compartmentVolumeDeriv, compartmentVolumeExpression);
                    Expression prod = Expression.multiply(concExpression, concDerivExpression);
                    Expression oneOverVolume = Expression.divide(Expression.ONE, compartmentVolumeExpression);
                    Expression compartmentVolumeModifier = Expression.subtract(oneOverVolume, prod);
                    retVal = Expression.multiply(retVal, compartmentVolumeModifier);
                }
                else
                {
                    retVal = Expression.divide(retVal, compartmentVolumeExpression);
                }
            }
            else
            {
                Expression compartmentVolumeExpression = new Expression(compartmentVolumeValue.getValue());
                retVal = Expression.divide(retVal, compartmentVolumeExpression);
            }
        }
        pSymbolEvaluator.setLocalSymbolsMap(null);

        return(retVal);
    }


}
