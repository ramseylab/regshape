package org.systemsbiology.chem.scripting;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.chem.*;
import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

import java.io.*;
import java.util.*;

import org.systemsbiology.chem.scripting.IModelExporter;

/**
 * @author Stephen Ramsey
 */
public class ModelExporterOrrellColumnFormat implements IModelExporter, IAliasableClass
{
    public static final String CLASS_ALIAS = "orrell-column-format";



   /**
    * Given a {@link org.systemsbiology.chem.Model} object
    * defining a system of chemical reactions and the initial species populations,
    * writes out the model in the column format specified by David Orrell
    */
    public void export(Model pModel, PrintWriter pOutputWriter) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException
    {


        // get collection of all symbols
        Collection symbols = pModel.getSymbols();
        Iterator symbolsIter = symbols.iterator();
        List compartmentsList = new LinkedList();
        ArrayList speciesList = new ArrayList();
        while(symbolsIter.hasNext())
        {
            SymbolValue symbolValue = (SymbolValue) symbolsIter.next();
            if(symbolValue instanceof Compartment)
            {
                compartmentsList.add((Compartment) symbolValue);
            }
            else if(symbolValue instanceof Species)
            {
                speciesList.add((Species) symbolValue);
            }
        }
        Collections.sort(compartmentsList);
        Collections.sort(speciesList);

        // handle compartments
        Iterator compartmentsIter = compartmentsList.iterator();
        while(compartmentsIter.hasNext())
        {
            Compartment compartment = (Compartment) compartmentsIter.next();
            String compartmentName = compartment.getName();
            Value volumeValueObj = compartment.getValue();
            if(volumeValueObj.isExpression())
            {
                throw new UnsupportedOperationException("cannot export to SBML a model that has a compartment with a custom expression for the compartment volume");
            }
            double volumeLiters = volumeValueObj.getValue();
            Double volumeLitersObj = new Double(volumeLiters);
        }

        // handle species

        Collection dynamicSymbols = pModel.getDynamicSymbols();
        Iterator dynamicSymbolsIter = dynamicSymbols.iterator();
        HashSet dynamicSymbolNames = new HashSet();
        while(dynamicSymbolsIter.hasNext())
        {
            SymbolValue dynamicSymbol = (SymbolValue) dynamicSymbolsIter.next();
            dynamicSymbolNames.add(dynamicSymbol.getSymbol().getName());
        }

        HashMap speciesHash = new HashMap();
        int numSpecies = speciesList.size();

        Collection reactionsColl = pModel.getReactions();
        ArrayList reactionsList = new ArrayList(reactionsColl);
        int numReactions = reactionsList.size();
        Collections.sort(reactionsList);


        pOutputWriter.println(numSpecies + " " + numReactions);

        pOutputWriter.println("1.0 1.0");
        pOutputWriter.println("1 0.0");

        for(int ctr = 0; ctr < numSpecies; ++ctr)
        {
            Species species = (Species) speciesList.get(ctr);
            String speciesName = species.getName();
            speciesHash.put(speciesName, new Integer(ctr));

//                 double initialSpeciesPopulation = 0.0;
//                 Value initialValueObj = species.getValue();
//                 if(initialValueObj.isExpression())
//                 {
//                     throw new UnsupportedOperationException("Not able to export this model to markup language, because the model contains a boundary (non-floating) species whose population is a mathematical expression.  In SBML level 1, a boundary species must have its population defined as a fixed number.  Species is: " + speciesName);
//                 }
//                 initialSpeciesPopulation = initialValueObj.getValue();

//                 double initialSpeciesPopulationConverted = ((double) initialSpeciesPopulation) * speciesPopulationConversionMultiplier;
        }


        // handle global parameters

        symbolsIter = symbols.iterator();
        List parametersList = new LinkedList();
        while(symbolsIter.hasNext())
        {
            SymbolValue symbolValue = (SymbolValue) symbolsIter.next();
            if(symbolValue instanceof Parameter)
            {
                parametersList.add(symbolValue);
            }
        }
        Collections.sort(parametersList);

        Iterator reactionsIter = reactionsList.iterator();
        while(reactionsIter.hasNext())
        {
            Reaction reaction = (Reaction) reactionsIter.next();
            int numReactionSteps = reaction.getNumSteps();
            String reactionName = reaction.getName();

            // handle reactant species
            int numReactants = reaction.getNumParticipants(Reaction.ParticipantType.REACTANT);
            if(numReactants > 2)
            {
                throw new ModelExporterException("reaction " + reactionName + " contains more than two reactant species types");
            }

            int numProducts = reaction.getNumParticipants(Reaction.ParticipantType.PRODUCT);
            if(numProducts > 2)
            {
                throw new ModelExporterException("reaction " + reactionName + " contains more than two product species types");
            }

            Species []reactantSpecies = new Species[numReactants];
            int []reactantStoichiometries = new int[numReactants];
            boolean []reactantDynamic = new boolean[numReactants];
            reaction.constructSpeciesArrays(reactantSpecies, 
                                            reactantStoichiometries, 
                                            reactantDynamic, 
                                            null, null, null, 
                                            Reaction.ParticipantType.REACTANT);

            Species []productSpecies = new Species[numProducts];
            int []productStoichiometries = new int[numProducts];
            boolean []productDynamic = new boolean[numProducts];
            reaction.constructSpeciesArrays(productSpecies, 
                                            productStoichiometries, 
                                            productDynamic, 
                                            null, null, null, 
                                            Reaction.ParticipantType.PRODUCT);

            int reactant1 = 0;
            int reactant2 = 0;

            for(int ctr = 0; ctr < numReactants; ++ctr)
            {
                Species species = (Species) reactantSpecies[ctr];
                String speciesName = species.getName();
                int stoic = reactantStoichiometries[ctr];
                Integer speciesIndexObj = (Integer) speciesHash.get(speciesName);
                if(null == speciesIndexObj)
                {
                    throw new ModelExporterException("unknown species name: " + speciesName);
                }
                int speciesIndex = speciesIndexObj.intValue() + 1;
                if(stoic == 1)
                {
                    if(0 == reactant1)
                    {
                        reactant1 = speciesIndex;
                    }
                    else
                    {
                        if(0 == reactant2)
                        {
                            reactant2 = speciesIndex;
                        }
                        else
                        {
                            throw new ModelExporterException("too many reactants, for reaction: " + reactionName);
                        }
                    }
                }
                else
                {
                    if(stoic == 2)
                    {
                        if(numReactants < 2)
                        {
                            reactant1 = speciesIndex;
                            reactant2 = reactant1;
                        }
                        else
                        {
                            throw new ModelExporterException("reaction " + reactionName + " contains more than two reactant molecules");
                        }
                    }
                    else
                    {
                        throw new ModelExporterException("reaction " + reactionName + " contains a reactant with a stoichiometry of zero; reactant is: " + speciesName);
                    }
                }
            }

            int product1 = 0;
            int product2 = 0;

            for(int ctr = 0; ctr < numProducts; ++ctr)
            {
                Species species = (Species) productSpecies[ctr];
                String speciesName = species.getName();
                int stoic = productStoichiometries[ctr];
                Integer speciesIndexObj = (Integer) speciesHash.get(speciesName);
                if(null == speciesIndexObj)
                {
                    throw new ModelExporterException("unknown species name: " + speciesName);
                }
                int speciesIndex = speciesIndexObj.intValue() + 1;
                if(stoic == 1)
                {
                    if(0 == product1)
                    {
                        product1 = speciesIndex;
                    }
                    else
                    {
                        if(0 == product2)
                        {
                            product2 = speciesIndex;
                        }
                        else
                        {
                            throw new ModelExporterException("too many products, for reaction: " + reactionName);
                        }
                    }
                }
                else
                {
                    if(stoic == 2)
                    {
                        if(numProducts < 2)
                        {
                            product1 = speciesIndex;
                            product2 = product1;
                        }
                        else
                        {
                            throw new ModelExporterException("reaction " + reactionName + " contains more than two product molecules");
                        }
                    }
                    else
                    {
                        throw new ModelExporterException("reaction " + reactionName + " contains a product with a stoichiometry of zero; product is: " + speciesName);
                    }
                }
            }

            // handle kinetic law
            Value rateValue = reaction.getValue();
            double reactionParameter = 0.0;
            if(rateValue.isExpression())
            {
                throw new ModelExporterException("a reaction contains a custom reaction rate expression; reaction is: " + reactionName);
            }
            else
            {
                reactionParameter = rateValue.getValue();
            }

             
            pOutputWriter.println(reactant1 + " " + reactant2 + " " + product1 + " " + product2 + " " + reactionParameter + " 0");
        }

        pOutputWriter.println("");


        for(int ctr = 0; ctr < numSpecies; ++ctr)
        {
            Species species = (Species) speciesList.get(ctr);
            String speciesName = species.getName();
            Integer speciesIndexObj = (Integer) speciesHash.get(speciesName);
            if(null == speciesIndexObj)
            {
                throw new ModelExporterException("unknown species: " + speciesName);
            }
            int speciesIndex = speciesIndexObj.intValue() + 1;
            pOutputWriter.println(speciesIndex + " " + speciesName);
        }

        pOutputWriter.flush();
    }

    public String getFileRegex()
    {
        return(".*\\.txt$");
    }
}
