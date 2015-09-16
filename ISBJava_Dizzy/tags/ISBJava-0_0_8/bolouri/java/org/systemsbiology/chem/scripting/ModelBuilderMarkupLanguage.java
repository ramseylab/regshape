package org.systemsbiology.chem.scripting;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.InvalidInputException;
import org.systemsbiology.util.IAliasableClass;
import org.systemsbiology.math.Expression;
import org.systemsbiology.chem.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author Stephen Ramsey
 */ 
public class ModelBuilderMarkupLanguage implements IModelBuilder, IAliasableClass
{
    /*========================================*
     * constants
     *========================================*/
    private static final String DEFAULT_MODEL_NAME = "model";
    public static final String CLASS_ALIAS = "markup-language";

    /*========================================*
     * inner classes
     *========================================*/
    
    /*========================================*
     * member data
     *========================================*/
    MarkupLanguageImporter mMarkupLanguageImporter;

    /*========================================*
     * accessor/mutator methods
     *========================================*/


    /*========================================*
     * initialization methods
     *========================================*/
    /*========================================*
     * constructors
     *========================================*/


    /*========================================*
     * private methods
     *========================================*/    



    /*========================================*
     * protected methods
     *========================================*/    


    /*========================================*
     * public methods
     *========================================*/
    public ModelBuilderMarkupLanguage()
    {
        mMarkupLanguageImporter = new MarkupLanguageImporter();
    }

    /**
     * Processes an SBML model into a {@link Model} object, and returns the model name.
     * Note that according to the SBML specification, there can be only one model
     * per SBML document or data stream.
     */
    public Model buildModel( BufferedReader pInputReader,
                             IncludeHandler pIncludeHandler ) throws InvalidInputException, IOException
    {
        StringBuffer modelDefinitionStringBuffer = new StringBuffer();
        String line = null;
        while((line = pInputReader.readLine()) != null)
        {
            modelDefinitionStringBuffer.append(line);
        }
        String modelDefinition = modelDefinitionStringBuffer.toString();

        mMarkupLanguageImporter.readModelDescription(modelDefinition);


        // process model name
        String modelName = mMarkupLanguageImporter.getModelName();

        // model name is an optional attribute; if it is specified, store it for this model
        if(null == modelName)
        {
            modelName = DEFAULT_MODEL_NAME;
        }

        Model model = new Model(modelName);
        model.setSpeciesRateFactorEvaluator(new SpeciesRateFactorEvaluatorConcentration());

        // process compartments and store them in a global map
        int numCompartments = mMarkupLanguageImporter.getNumCompartments();
        if(numCompartments <= 0)
        {
            throw new InvalidInputException("invalid number of compartments specified in the model: " + numCompartments);
        }
        
        HashMap compartmentsMap = new HashMap();

        for(int compartmentCtr = 0; compartmentCtr < numCompartments; ++compartmentCtr)
        {
            String compartmentName = mMarkupLanguageImporter.getNthCompartmentName(compartmentCtr);

            assert (null != compartmentName) : "null compartment name returned";

            Compartment compartment = null;
            if(mMarkupLanguageImporter.hasValue(compartmentName))
            {
                double compVolume = mMarkupLanguageImporter.getValue(compartmentName);
                compartment = new Compartment(compartmentName, compVolume);
            }
            else
            {
                compartment = new Compartment(compartmentName);
            }
                
            SymbolValueChemSimulation.addSymbolValueToMap(compartmentsMap, compartmentName, compartment);
        }

        HashMap globalParamsMap = new HashMap();

        // process global parameters
        int numGlobalParams = mMarkupLanguageImporter.getNumGlobalParameters();
        for(int globalParamCtr = 0; globalParamCtr < numGlobalParams; ++globalParamCtr)
        {
            // get global parameter name
            String globalParamName = mMarkupLanguageImporter.getNthGlobalParameterName(globalParamCtr);
            assert (null != globalParamName) : "null global parameter name returned";

            if(! mMarkupLanguageImporter.hasValue(globalParamName))
            {
                throw new InvalidInputException("global parameter " + globalParamName + " has no value associated with it");
            }
            double globalParamValue = mMarkupLanguageImporter.getValue(globalParamName);

            Parameter parameter = new Parameter(globalParamName, globalParamValue);
            model.addParameter(parameter);
        }
        
        // process floating species and store them in the species map
        int numSpecies = mMarkupLanguageImporter.getNumFloatingSpecies();
        if(numSpecies <= 0)
        {
            throw new IllegalArgumentException("at least one floating species must be defined");
        }

        HashMap dynamicalSpeciesMap = new HashMap();

        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = mMarkupLanguageImporter.getNthFloatingSpeciesName(speciesCtr);

            String compartmentName = mMarkupLanguageImporter.getNthFloatingSpeciesCompartmentName(speciesCtr);
            Compartment compartment = (Compartment) compartmentsMap.get(compartmentName);
            if(null == compartment)
            {
                throw new InvalidInputException("unknown compartment name \"" + compartmentName + "\" referenced from species \"" + speciesName + "\"");
            }

            Species species = new Species(speciesName, compartment);

            // get initial population for species
            if(! mMarkupLanguageImporter.hasValue(speciesName))
            {
                throw new IllegalArgumentException("floating species " + speciesName + " has no initial population value defined");
            }

            double initialSpeciesPopulation = mMarkupLanguageImporter.getValue(speciesName);
            species.setSpeciesPopulation(initialSpeciesPopulation);

            SymbolValueChemSimulation.addSymbolValueToMap(dynamicalSpeciesMap, speciesName, species);
        }

        // process boundary species and store them in the species map
        numSpecies = mMarkupLanguageImporter.getNumBoundarySpecies();
        if(numSpecies < 0)
        {
            throw new IllegalArgumentException("invalid number of boundary species: " + numSpecies);
        }


        HashMap boundarySpeciesMap = new HashMap();

        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = mMarkupLanguageImporter.getNthBoundarySpeciesName(speciesCtr);
            String compartmentName = mMarkupLanguageImporter.getNthFloatingSpeciesCompartmentName(speciesCtr);
            Compartment compartment = (Compartment) compartmentsMap.get(compartmentName);
            if(null == compartment)
            {
                throw new InvalidInputException("unknown compartment name \"" + compartmentName + "\" referenced from species \"" + speciesName + "\"");
            }

            Species species = new Species(speciesName, compartment);

            // get initial population for species
            if(! mMarkupLanguageImporter.hasValue(speciesName))
            {
                throw new IllegalArgumentException("boundary species " + speciesName + " has no initial population value defined");
            }

            double initialSpeciesPopulation = mMarkupLanguageImporter.getValue(speciesName);
            species.setSpeciesPopulation(initialSpeciesPopulation);

            SymbolValueChemSimulation.addSymbolValueToMap(boundarySpeciesMap, speciesName, species);

        }

        int numReactions = mMarkupLanguageImporter.getNumReactions();
        assert (numReactions >= 0) : "invalid number of reactions";

        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            // get reaction name
            String reactionName = mMarkupLanguageImporter.getNthReactionName(reactionCtr);
            Reaction reaction = new Reaction(reactionName);

            // get number of reactants
            int numReactants = mMarkupLanguageImporter.getNumReactants(reactionCtr);
            assert (numReactants >= 0) : "invalid number of reactants";

            // cycle through reactants
            for(int reactantCtr = 0; reactantCtr < numReactants; reactantCtr++)
            {
                // get reactant species name
                String reactantName = mMarkupLanguageImporter.getNthReactantName(reactionCtr, numReactants - reactantCtr - 1);
                
                // get reactant stoichiometry
                int reactantStoic = mMarkupLanguageImporter.getNthReactantStoichiometry(reactionCtr, numReactants - reactantCtr - 1);
                
                Species reactant = (Species) dynamicalSpeciesMap.get(reactantName);
                boolean isDynamical = true;
                if(null == reactant)
                {
                    reactant = (Species) boundarySpeciesMap.get(reactantName);
                    if(null == reactant)
                    {
                        throw new InvalidInputException("unknown species name \"" + reactantName + "\" listed as a reactant for reaction: " + reactionName);
                    }
                    else
                    {
                        isDynamical = false;
                    }
                }

                reaction.addReactant(reactant, reactantStoic, isDynamical);
            }

            // get number of products
            int numProducts = mMarkupLanguageImporter.getNumProducts(reactionCtr);
            assert (numProducts >= 0) : "invalid number of products";

            for(int productCtr = 0; productCtr < numProducts; ++productCtr)
            {
                // get product species name
                String productName = mMarkupLanguageImporter.getNthProductName(reactionCtr, numProducts - productCtr - 1);


                // get product stoichiometry
                int productStoic = mMarkupLanguageImporter.getNthProductStoichiometry(reactionCtr, numProducts - productCtr - 1);
                if(productStoic <= 0)
                {
                    throw new IllegalArgumentException("for reaction number " + reactionCtr + ", product species " + productName + " has an invalid stoichiometry: " + productStoic);
                }

                Species product = (Species) dynamicalSpeciesMap.get(productName);
                if(null == product)
                {
                    product = (Species) boundarySpeciesMap.get(productName);
                    if(null == product)
                    {
                        throw new InvalidInputException("unknown species name \"" + productName + "\" listed as a product for reaction: " + reactionName);
                    }
                }

                reaction.addProduct(product, productStoic);
            }

            // get kinetic law for the reaction
            String kineticLaw = mMarkupLanguageImporter.getKineticLaw(reactionCtr);
            assert (null != kineticLaw) : "null kinetic law encountered in model definition";
            if(kineticLaw.trim().length() == 0)
            {
                throw new InvalidInputException("missing or empty kinetic law for reaction number: " + reactionCtr);
            }
            
            Expression rateExpression = new Expression(kineticLaw);
            reaction.setRate(rateExpression);

            // get number of parameters
            int numParams = mMarkupLanguageImporter.getNumParameters(reactionCtr);
            for(int paramCtr = 0; paramCtr < numParams; ++paramCtr)
            {
                // get the name of the Nth parameter
                String paramName = mMarkupLanguageImporter.getNthParameterName(reactionCtr, paramCtr);

                if(! mMarkupLanguageImporter.getNthParameterHasValue(reactionCtr, paramCtr))
                {
                    throw new InvalidInputException("for reaction number " + reactionCtr + ", parameter " + paramName + " has no value associated with it");
                }

                // get value associated with the Nth parameter parameter
                double paramValue = mMarkupLanguageImporter.getNthParameterValue(reactionCtr, paramCtr);

                Parameter reactionParameter = new Parameter(paramName, paramValue);
                reaction.addParameter(reactionParameter);
            }

            model.addReaction(reaction);
        }

        return(model);
    }

    public String getFileRegex()
    {
        return(".*\\.(xml|sbml)$");
    }
}
