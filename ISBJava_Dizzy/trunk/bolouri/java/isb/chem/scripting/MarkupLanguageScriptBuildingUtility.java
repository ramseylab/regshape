package isb.chem.scripting;

import java.io.*;
import SBMLValidate.NOMService;
import isb.chem.*;
import isb.util.*;
import java.util.*;

/**
 * This class contains static methods for building a {@link Script} from a text input
 * stream containing an SBML description of a {@link isb.chem.Model}. 
 * This class is used by the {@link isb.chem.sbw.MarkupLanguageParser} class.
 *
 * @see isb.chem.sbw.MarkupLanguageParser
 * @see isb.chem.Model
 * @see Script
 *
 * @author Stephen Ramsey
 */ 
public class MarkupLanguageScriptBuildingUtility
{
    /*========================================*
     * constants
     *========================================*/
    private static final double MAX_INITIAL_SPECIES_POPULATION = Double.MAX_VALUE;
    static final String INITIAL_SPECIES_POPULATIONS_NAME = "initialSpeciesPopulations";
    private static final String DEFAULT_MODEL_NAME = "sbmlModel";

    /*========================================*
     * inner classes
     *========================================*/
    
    /*========================================*
     * member data
     *========================================*/

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
    private static double convertSpeciesPopulation(double pSpeciesPopulationsCorrectionFactor, 
                                                   double pSpeciesPopulationMoles) throws IllegalArgumentException
    {
        double initialSpeciesPopulationMolecules = (double) Math.round(Constants.AVOGADRO_CONSTANT * pSpeciesPopulationMoles);
        double correctedInitialSpeciesPopulation = pSpeciesPopulationsCorrectionFactor * initialSpeciesPopulationMolecules;

        if(correctedInitialSpeciesPopulation >= MAX_INITIAL_SPECIES_POPULATION)
        {
            throw new IllegalArgumentException("floating species has an initial population value that is too large for this simulator: " + pSpeciesPopulationMoles + " moles; corrected initial species population is: " + correctedInitialSpeciesPopulation);
        }
        return(correctedInitialSpeciesPopulation);
    }

    private static double convertSpeciesPopulation(ReactionRateSpeciesMode pReactionRateSpeciesMode, 
                                                   double pSpeciesPopulationMoles) throws IllegalArgumentException
    {
        double retVal = 0.0;
        if(pReactionRateSpeciesMode.equals(ReactionRateSpeciesMode.MOLECULES))
        {
            retVal = pSpeciesPopulationMoles;
        }
        else if(pReactionRateSpeciesMode.equals(ReactionRateSpeciesMode.CONCENTRATION))
        {
            retVal = pSpeciesPopulationMoles * Constants.AVOGADRO_CONSTANT;
        }
        else
        {
            throw new IllegalArgumentException("invalid reaction rate species mode: " + pReactionRateSpeciesMode);
        }
        retVal = (double) Math.round(retVal);
    
        if(retVal >= MAX_INITIAL_SPECIES_POPULATION)
        {
            throw new IllegalArgumentException("floating species has an initial population value that is too large for this simulator: " + pSpeciesPopulationMoles + " moles; corrected initial species population is: " + retVal);
        }
        return(retVal);
    }


    /*========================================*
     * protected methods
     *========================================*/    


    /*========================================*
     * public methods
     *========================================*/

    /**
     * Processes an SBML model into a {@link Script}, and returns the model name.
     * Note that according to the SBML specification, there can be only one model
     * per SBML document or data stream.
     */
    public static String processMarkupLanguage( BufferedReader pInputReader,
                                                IModelInstanceImporter pModelInstanceImporter,
                                                ReactionRateSpeciesMode pReactionRateSpeciesMode,
                                                Script pScript ) throws ModelInstanceImporterException, IllegalArgumentException, IOException
    {
        StringBuffer modelDefinitionStringBuffer = new StringBuffer();
        String line = null;
        while((line = pInputReader.readLine()) != null)
        {
            modelDefinitionStringBuffer.append(line);
        }
        String modelDefinition = modelDefinitionStringBuffer.toString();

        pModelInstanceImporter.readModelDescription(modelDefinition);

        // process compartments and store them in a global map
        int numCompartments = pModelInstanceImporter.getNumCompartments();
        if(numCompartments <= 0)
        {
            throw new IllegalArgumentException("invalid number of compartments specified in the model: " + numCompartments);
        }
        
        HashSet compSet = new HashSet();

        for(int compartmentCtr = 0; compartmentCtr < numCompartments; ++compartmentCtr)
        {
            String compartmentName = pModelInstanceImporter.getNthCompartmentName(compartmentCtr);
            if(null == compartmentName || 0 == compartmentName.length())
            {
                throw new IllegalArgumentException("compartment number " + compartmentCtr + " has a null or empty name element");
            }

            if(! compSet.contains(compartmentName))
            {
                compSet.add(compartmentName);
                Statement compartmentStatement = new Statement(Statement.Type.COMPARTMENT);
                compartmentStatement.setName(compartmentName);
                if(pModelInstanceImporter.hasValue(compartmentName))
                {
                    double compVolume = pModelInstanceImporter.getValue(compartmentName);
                    Element volumeElement = new Element(Element.Type.VOLUME);
                    volumeElement.setNumberValue(new Double(compVolume));
                    compartmentStatement.putElement(volumeElement);
                }
                pScript.addStatement(compartmentStatement);
            }
        }

        HashSet globalParamsSet = new HashSet();

        // process global parameters
        int numGlobalParams = pModelInstanceImporter.getNumGlobalParameters();
        for(int globalParamCtr = 0; globalParamCtr < numGlobalParams; ++globalParamCtr)
        {
            // get global parameter name
            String globalParamName = pModelInstanceImporter.getNthGlobalParameterName(globalParamCtr);
            if(null == globalParamName || 0 == globalParamName.length())
            {
                throw new IllegalArgumentException("global parameter number:  " + globalParamCtr + " has length zero or is null");
            }
            if(! pModelInstanceImporter.hasValue(globalParamName))
            {
                throw new IllegalArgumentException("global parameter " + globalParamName + " has no value associated with it");
            }
            globalParamsSet.add(globalParamName);

            // get global parameter value
            double globalParamValue = pModelInstanceImporter.getValue(globalParamName);
            
            Statement parameterStatement = new Statement(Statement.Type.PARAMETER);
            parameterStatement.setName(globalParamName);
            Element valueElement = new Element(Element.Type.VALUE);
            valueElement.setNumberValue(new Double(globalParamValue));
            parameterStatement.putElement(valueElement);
            pScript.addStatement(parameterStatement);
        }
        
        Element speciesPopulationSymbols = new Element(Element.Type.SPECIESLIST);
        speciesPopulationSymbols.createDataList();
        Element speciesPopulationValues = new Element(Element.Type.POPULATIONS);
        speciesPopulationValues.createDataList();

        // process floating species and store them in the species map
        int numSpecies = pModelInstanceImporter.getNumFloatingSpecies();
        if(numSpecies <= 0)
        {
            throw new IllegalArgumentException("at least one floating species must be defined");
        }

        HashSet speciesSet = new HashSet();

        boolean floating = true;

        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = pModelInstanceImporter.getNthFloatingSpeciesName(speciesCtr);
            if(null == speciesName || speciesName.length() == 0)
            {
                throw new IllegalArgumentException("could not find species name for species number: " + speciesCtr);
            }

            if(speciesSet.contains(speciesName))
            {
                throw new IllegalArgumentException("species " + speciesName + " was defined more than once");
            }
            else
            {
                speciesSet.add(speciesName);
            }

            String compartmentName = pModelInstanceImporter.getNthFloatingSpeciesCompartmentName(speciesCtr);
            if(null == compartmentName || compartmentName.length() == 0)
            {
                throw new IllegalArgumentException("could not find compartment name for species: " + speciesName);
            }

            Statement speciesStatement = new Statement(Statement.Type.SPECIES);
            speciesStatement.setName(speciesName);
            Element speciesTypeElement = new Element(Element.Type.SPECIESTYPE);
            speciesTypeElement.setModifier(Element.ModifierCode.FLOATING);
            speciesStatement.putElement(speciesTypeElement);
            Element compartmentElement = new Element(Element.Type.COMPARTMENT);
            compartmentElement.setSymbolName(compartmentName);
            speciesStatement.putElement(compartmentElement);
            pScript.addStatement(speciesStatement);

            // get initial population for species
            if(! pModelInstanceImporter.hasValue(speciesName))
            {
                throw new IllegalArgumentException("floating species " + speciesName + " has no initial population value defined");
            }

            double initialSpeciesPopulationMoles = pModelInstanceImporter.getValue(speciesName);
            double correctedInitialSpeciesPopulation = 0.0;

            try
            {
                correctedInitialSpeciesPopulation = convertSpeciesPopulation(pReactionRateSpeciesMode, 
                                                                             initialSpeciesPopulationMoles);
            }
            catch(IllegalArgumentException e)
            {
                throw new IllegalArgumentException(e.getMessage() + "; for species: " + speciesName);
            }

            speciesPopulationSymbols.addDataToList(speciesName);
            speciesPopulationValues.addDataToList(new Double(correctedInitialSpeciesPopulation));
        }

        // process boundary species and store them in the species map
        numSpecies = pModelInstanceImporter.getNumBoundarySpecies();
        if(numSpecies < 0)
        {
            throw new IllegalArgumentException("invalid number of boundary species: " + numSpecies);
        }

        floating = false;
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = pModelInstanceImporter.getNthBoundarySpeciesName(speciesCtr);
            if(null == speciesName || speciesName.length() == 0)
            {
                throw new IllegalArgumentException("could not find species name for species number: " + speciesCtr);
            }

            if(speciesSet.contains(speciesName))
            {
                throw new IllegalArgumentException("species " + speciesName + " was defined more than once");
            }
            else
            {
                speciesSet.add(speciesName);
            }

            String compartmentName = pModelInstanceImporter.getNthFloatingSpeciesCompartmentName(speciesCtr);
            if(null == compartmentName || compartmentName.length() == 0)
            {
                throw new IllegalArgumentException("could not find compartment name for species: " + speciesName);
            }

            Statement speciesStatement = new Statement(Statement.Type.SPECIES);
            speciesStatement.setName(speciesName);
            Element speciesTypeElement = new Element(Element.Type.SPECIESTYPE);
            speciesTypeElement.setModifier(Element.ModifierCode.BOUNDARY);
            speciesStatement.putElement(speciesTypeElement);
            Element compartmentElement = new Element(Element.Type.COMPARTMENT);
            compartmentElement.setSymbolName(compartmentName);
            speciesStatement.putElement(compartmentElement);
            pScript.addStatement(speciesStatement);

            // get initial population for species
            if(! pModelInstanceImporter.hasValue(speciesName))
            {
                throw new IllegalArgumentException("boundary species " + speciesName + " has no initial population value defined");
            }


            double initialSpeciesPopulationMoles = pModelInstanceImporter.getValue(speciesName);
            double correctedInitialSpeciesPopulation = 0.0;

            try
            {
                correctedInitialSpeciesPopulation = convertSpeciesPopulation(pReactionRateSpeciesMode,
                                                                             initialSpeciesPopulationMoles);
            }
            catch(IllegalArgumentException e)
            {
                throw new IllegalArgumentException(e.getMessage() + "; for species: " + speciesName);
            }

            speciesPopulationSymbols.addDataToList(speciesName);
            speciesPopulationValues.addDataToList(new Double(correctedInitialSpeciesPopulation));
        }

        Statement speciesPopulationsStatement = new Statement(Statement.Type.SPECIESPOPULATIONS);
        speciesPopulationsStatement.setName(INITIAL_SPECIES_POPULATIONS_NAME);
        speciesPopulationsStatement.putElement(speciesPopulationSymbols);
        speciesPopulationsStatement.putElement(speciesPopulationValues);
        pScript.addStatement(speciesPopulationsStatement);

        int numReactions = pModelInstanceImporter.getNumReactions();
        if(numReactions <= 0)
        {
            throw new IllegalArgumentException("invalid number of reactions specified: " + numReactions);
        }

        HashSet reactionSet = new HashSet();

        for(int reactionCtr = 0; reactionCtr < numReactions; ++reactionCtr)
        {
            // get reaction name
            String reactionName = pModelInstanceImporter.getNthReactionName(reactionCtr);
            if(null == reactionName)
            {
                throw new IllegalArgumentException("null reaction name encountered for reaction number: " + reactionCtr);
            }

            if(reactionSet.contains(reactionName))
            {
                throw new IllegalArgumentException("reaction " + reactionName + " was defined twice in the model");
            }
            else
            {
                reactionSet.add(reactionName);
            }

            Statement reactionStatement = new Statement(Statement.Type.REACTION);
            reactionStatement.setName(reactionName);

            // get number of reactants
            int numReactants = pModelInstanceImporter.getNumReactants(reactionCtr);
            if(numReactants < 0)
            {
                throw new IllegalArgumentException("encountered a reaction with an invalid number of reactants, for reaction number: " + reactionCtr);
            }            

            Element reactantsElement = new Element(Element.Type.REACTANTS);
            reactantsElement.createDataList();

            // cycle through reactants
            for(int reactantCtr = 0; reactantCtr < numReactants; reactantCtr++)
            {
                // get reactant species name
                String reactantName = pModelInstanceImporter.getNthReactantName(reactionCtr, numReactants - reactantCtr - 1);
                if(null == reactantName || reactantName.length() == 0)
                {
                    throw new IllegalArgumentException("for reaction number " + reactionCtr + ", the name of reactant number " + reactantCtr + " was a null or empty string");
                }
                
                if(! speciesSet.contains(reactantName))
                {
                    throw new IllegalArgumentException("reaction number " + reactionCtr + " has a reactant species that was not defined: " + reactantName);
                }

                // get reactant stoichiometry
                int reactantStoic = pModelInstanceImporter.getNthReactantStoichiometry(reactionCtr, numReactants - reactantCtr - 1);
                if(reactantStoic <= 0)
                {
                    throw new IllegalArgumentException("for reaction number " + reactionCtr + ", reactant species " + reactantName + " has an invalid stoichiometry: " + reactantStoic);
                }

                for(int stoicCtr = 0; stoicCtr < reactantStoic; ++stoicCtr)
                {
                    reactantsElement.addDataToList(reactantName);
                }
            }

            reactionStatement.putElement(reactantsElement);

            Element productsElement = new Element(Element.Type.PRODUCTS);
            productsElement.createDataList();

            // get number of products
            int numProducts = pModelInstanceImporter.getNumProducts(reactionCtr);
            if(numProducts < 0)
            {
                throw new IllegalArgumentException("encountered a reaction with an invalid number of products, for reaction number: " + reactionCtr);
            }

            for(int productCtr = 0; productCtr < numProducts; ++productCtr)
            {
                // get product species name
                String productName = pModelInstanceImporter.getNthProductName(reactionCtr, numProducts - productCtr - 1);
                if(null == productName || productName.length() == 0)
                {
                    throw new IllegalArgumentException("for reaction number " + reactionCtr + ", the name of product number " + productCtr + " was a null or empty string");
                }
                
                if(! speciesSet.contains(productName))
                {
                    throw new IllegalArgumentException("reaction number " + reactionCtr + " has a product species that was not defined: " + productName);
                }

                // get product stoichiometry
                int productStoic = pModelInstanceImporter.getNthProductStoichiometry(reactionCtr, numProducts - productCtr - 1);
                if(productStoic <= 0)
                {
                    throw new IllegalArgumentException("for reaction number " + reactionCtr + ", product species " + productName + " has an invalid stoichiometry: " + productStoic);
                }

                for(int stoicCtr = 0; stoicCtr < productStoic; ++stoicCtr)
                {
                    productsElement.addDataToList(productName);
                }
            }

            reactionStatement.putElement(productsElement);

            // get kinetic law for the reaction
            String kineticLaw = pModelInstanceImporter.getKineticLaw(reactionCtr);
            if(null == kineticLaw || kineticLaw.length() == 0)
            {
                throw new IllegalArgumentException("missing or empty kinetic law for reaction number: " + reactionCtr);
            }
            Element reactionRateElement = new Element(Element.Type.RATE);
            reactionRateElement.setSymbolName(kineticLaw);
            reactionStatement.putElement(reactionRateElement);
            pScript.addStatement(reactionStatement);

            // get number of parameters
            int numParams = pModelInstanceImporter.getNumParameters(reactionCtr);
            for(int paramCtr = 0; paramCtr < numParams; ++paramCtr)
            {
                // get the name of the Nth parameter
                String paramName = pModelInstanceImporter.getNthParameterName(reactionCtr, paramCtr);
                if(null == paramName || 0 == paramName.length())
                {
                    throw new IllegalArgumentException("for reaction number " + reactionCtr + ", parameter number " + paramCtr + " has a parameter name that is null or length zero");
                }

                if(! pModelInstanceImporter.getNthParameterHasValue(reactionCtr, paramCtr))
                {
                    throw new IllegalArgumentException("for reaction number " + reactionCtr + ", parameter " + paramName + " has no value associated with it");
                }

                // get value associated with the Nth parameter parameter
                double paramValue = pModelInstanceImporter.getNthParameterValue(reactionCtr, paramCtr);

                Statement reactionParameterStatement = new Statement(Statement.Type.PARAMETER);
                reactionParameterStatement.setName(paramName);
                Element reactionElement = new Element(Element.Type.REACTIONS);
                reactionElement.createDataList();
                reactionElement.addDataToList(reactionName);
                reactionParameterStatement.putElement(reactionElement);
                Element valueElement = new Element(Element.Type.VALUE);
                valueElement.setNumberValue(new Double(paramValue));
                reactionParameterStatement.putElement(valueElement);
                pScript.addStatement(reactionParameterStatement);
            }
        }

        // process model name
        String modelName = pModelInstanceImporter.getModelName();

        // model name is an optional attribute; if it is specified, store it for this model
        if(null == modelName)
        {
            modelName = DEFAULT_MODEL_NAME;
        }

        Statement modelStatement = new Statement(Statement.Type.MODEL);
        modelStatement.setName(modelName);
        Element speciesModeElement = new Element(Element.Type.SPECIESMODE);
        if(pReactionRateSpeciesMode.equals(ReactionRateSpeciesMode.MOLECULES))
        {
            speciesModeElement.setModifier(Element.ModifierCode.MOLECULES);
        }
        else if(pReactionRateSpeciesMode.equals(ReactionRateSpeciesMode.CONCENTRATION))
        {
            speciesModeElement.setModifier(Element.ModifierCode.CONCENTRATION);
        }
        else
        {
            throw new IllegalArgumentException("unknown reaction rate species mode: " + pReactionRateSpeciesMode);
        }
        modelStatement.putElement(speciesModeElement);

        Element reactionsElement = new Element(Element.Type.REACTIONS);
        reactionsElement.createDataList();

        List reactionList = new LinkedList(reactionSet);
        Collections.sort(reactionList);

        Iterator reactionsIter = reactionList.iterator();
        while(reactionsIter.hasNext())
        {
            String reactionName = (String) reactionsIter.next();
            reactionsElement.addDataToList(reactionName);
        }
        modelStatement.putElement(reactionsElement);
        Iterator paramsIter = globalParamsSet.iterator();
        if(paramsIter.hasNext())
        {
            Element paramsElement = new Element(Element.Type.PARAMETERS);
            paramsElement.createDataList();
            while(paramsIter.hasNext())
            {
                String paramName = (String) paramsIter.next();
                paramsElement.addDataToList(paramName);
            }
            modelStatement.putElement(paramsElement);
        }
        pScript.addStatement(modelStatement);
        return(modelName);
    }

    public static Script buildExportScript(String pModelName, String pExporterSymbolName)
    {
        Script script = new Script();
        Statement exportStatement = new Statement(Statement.Type.EXPORTMODELINSTANCE);
        exportStatement.setName(pModelName);
        Element speciesPopulationsElement = new Element(Element.Type.SPECIESPOPULATIONS);
        speciesPopulationsElement.setSymbolName(INITIAL_SPECIES_POPULATIONS_NAME);
        exportStatement.putElement(speciesPopulationsElement);
        Element exporterElement = new Element(Element.Type.EXPORTER);
        exporterElement.setSymbolName(pExporterSymbolName);
        exportStatement.putElement(exporterElement);
        script.addStatement(exportStatement);
        return(script);
    }

    public static Script buildSimulationScript(String pModelName, double pStartTime, double pStopTime, int pNumPoints, String []pFilter)
    {
        Script script = new Script();
        Statement simulateStatement = new Statement(Statement.Type.SIMULATE);
        simulateStatement.setName(pModelName);
        Element speciesPopulationsElement = new Element(Element.Type.SPECIESPOPULATIONS);
        speciesPopulationsElement.setSymbolName(INITIAL_SPECIES_POPULATIONS_NAME);
        simulateStatement.putElement(speciesPopulationsElement);
        Element startTimeElement = new Element(Element.Type.STARTTIME);
        startTimeElement.setNumberValue(new Double(pStartTime));
        simulateStatement.putElement(startTimeElement);
        Element stopTimeElement = new Element(Element.Type.STOPTIME);
        stopTimeElement.setNumberValue(new Double(pStopTime));
        simulateStatement.putElement(stopTimeElement);
        Element numPointsElement = new Element(Element.Type.NUMTIMEPOINTS);
        numPointsElement.setNumberValue(new Double((double) pNumPoints));
        simulateStatement.putElement(numPointsElement);
        Element viewSpeciesElement = new Element(Element.Type.VIEWSPECIES);
        viewSpeciesElement.createDataList();
        int numSpecies = pFilter.length;
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            String speciesName = pFilter[speciesCtr];
            viewSpeciesElement.addDataToList(speciesName);
        }
        simulateStatement.putElement(viewSpeciesElement);

        Element outputElement = new Element(Element.Type.OUTPUT);
        outputElement.setModifier(Element.ModifierCode.STORE);
        simulateStatement.putElement(outputElement);

        Element simulatorElement = new Element(Element.Type.SIMULATOR);
        simulatorElement.setSymbolName(GillespieSimulator.CLASS_ALIAS);
        simulateStatement.putElement(simulatorElement);

        script.addStatement(simulateStatement);

        return(script);
    }

    public static Script buildModelValidateStatement(String pModelName)
    {
        Script script = new Script();
        Statement statement = new Statement(Statement.Type.VALIDATEMODEL);
        statement.setName(pModelName);
        Element speciesPopulationElement = new Element(Element.Type.SPECIESPOPULATIONS);
        speciesPopulationElement.setSymbolName(INITIAL_SPECIES_POPULATIONS_NAME);
        statement.putElement(speciesPopulationElement);
        script.addStatement(statement);
        return(script);
    }

}
