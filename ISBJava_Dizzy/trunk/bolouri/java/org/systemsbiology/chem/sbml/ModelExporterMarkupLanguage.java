package org.systemsbiology.chem.sbml;
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

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

/**
 * Exports a {@link org.systemsbiology.chem.Model} to
 * the Systems Biology Markup Language (SBML) format.
 *
 * @author Stephen Ramsey
 */
public class ModelExporterMarkupLanguage implements IModelExporter, IAliasableClass
{
    public static final String CLASS_ALIAS = "markup-language";

    private static final String ELEMENT_NAME_SBML = "sbml";
    private static final String ELEMENT_NAME_MODEL = "model";
    private static final String ELEMENT_NAME_LIST_OF_COMPARTMENTS = "listOfCompartments";
    private static final String ELEMENT_NAME_LIST_OF_RULES = "listOfRules";
    private static final String ELEMENT_NAME_COMPARTMENT_VOLUME_RULE = "compartmentVolumeRule";
    private static final String ELEMENT_NAME_SPECIES_CONCENTRATION_RULE = "speciesConcentrationRule";
    private static final String ELEMENT_NAME_PARAMETER_RULE = "parameterRule";
    private static final String ELEMENT_NAME_LIST_OF_SPECIES = "listOfSpecies";
    private static final String ELEMENT_NAME_LIST_OF_PARAMETERS = "listOfParameters";
    private static final String ELEMENT_NAME_SPECIES_V1 = "specie";
    private static final String ELEMENT_NAME_SPECIES_V2 = "species";
    private static final String ELEMENT_NAME_COMPARTMENT = "compartment";
    private static final String ELEMENT_NAME_PARAMETER = "parameter";
    private static final String ELEMENT_NAME_LIST_OF_REACTIONS = "listOfReactions";
    private static final String ELEMENT_NAME_REACTION = "reaction";
    private static final String ELEMENT_NAME_LIST_OF_REACTANTS = "listOfReactants";
    private static final String ELEMENT_NAME_LIST_OF_PRODUCTS = "listOfProducts";
    private static final String ELEMENT_NAME_SPECIES_REFERENCE_V1 = "specieReference";
    private static final String ELEMENT_NAME_SPECIES_REFERENCE_V2 = "speciesReference";
    private static final String ELEMENT_NAME_KINETIC_LAW = "kineticLaw";
    private static final String ELEMENT_NAME_LIST_OF_UNIT_DEFINITIONS = "listOfUnitDefinitions";
    private static final String ELEMENT_NAME_LIST_OF_UNITS = "listOfUnits";
    private static final String ELEMENT_NAME_UNIT_DEFINITION = "unitDefinition";
    private static final String ELEMENT_NAME_UNIT = "unit";

    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_VOLUME = "volume";
    private static final String ATTRIBUTE_BOUNDARY_CONDITION = "boundaryCondition";
    private static final String ATTRIBUTE_INITIAL_AMOUNT = "initialAmount";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String ATTRIBUTE_STOICHIOMETRY = "stoichiometry";
    private static final String ATTRIBUTE_FORMULA = "formula";
    private static final String ATTRIBUTE_SPECIES_V1 = "specie";
    private static final String ATTRIBUTE_SPECIES_V2 = "species";
    private static final String ATTRIBUTE_REVERSIBLE = "reversible";
    private static final String ATTRIBUTE_COMPARTMENT = "compartment";
    private static final String ATTRIBUTE_KIND = "kind";

    private static final String UNIT_NAME_VOLUME = "volume";
    private static final String UNIT_NAME_SUBSTANCE = "substance";
    private static final String UNIT_KIND_DIMENSIONLESS = "dimensionless";
    private static final String UNIT_KIND_ITEM = "item";

    private static final String XMLNS_NAME = "xmlns";
    private static final String XMLNS_VALUE = "http://www.sbml.org/sbml/level";
    private static final String LEVEL_NAME = "level";
    private static final String VERSION_NAME = "version";

    public static final String DEFAULT_REACTION_PARAMETER_SYMBOL_NAME = "__RATE__";

    public static final Specification DEFAULT_SPECIFICATION = Specification.LEVEL1_VERSION2;


    public static class Specification
    {
        private final String mName;
        private final String mLevel;
        private final String mVersion;
        private Specification(String pLevel, String pVersion)
        {
            mLevel = pLevel;
            mVersion = pVersion;
            mName = "level " + mLevel + ", version " + pVersion;
        }
        public String getLevel()
        {
            return(mLevel);
        }
        public String getVersion()
        {
            return(mVersion);
        }
        public String toString()
        {
            return(mName);
        }
        public static final Specification LEVEL1_VERSION1 = new Specification("1", "1");
        public static final Specification LEVEL1_VERSION2 = new Specification("1", "2");
    }

    public ModelExporterMarkupLanguage()
    {
        // do nothing
    }

    public void export(Model pModel, PrintWriter pOutputWriter) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException
    {
        export(pModel, pOutputWriter, DEFAULT_SPECIFICATION);
    }

    private String convertSymbolToLegalSName(String pSymbol)
    {
        return(pSymbol.replace(':', '_'));
    }

   /**
    * Given a {@link org.systemsbiology.chem.Model} object
    * defining a system of chemical reactions and the initial species populations,
    * writes out the SBML description of the model and initial species
    * populations, to the output stream contained in <code>pOutputWriter</code>.
    */
    public void export(Model pModel, PrintWriter pOutputWriter, Specification pSpecification) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException
    {
        try
        {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element sbmlElement = document.createElement(ELEMENT_NAME_SBML);
            String level = pSpecification.getLevel();
            sbmlElement.setAttribute(XMLNS_NAME, XMLNS_VALUE + level);
            sbmlElement.setAttribute(LEVEL_NAME, level);
            sbmlElement.setAttribute(VERSION_NAME, pSpecification.getVersion());
            document.appendChild(sbmlElement);

            Element modelElement = document.createElement(ELEMENT_NAME_MODEL);
            String modelName = pModel.getName();
            if(null == modelName)
            {
                throw new IllegalArgumentException("model name missing");
            }
            modelElement.setAttribute(ATTRIBUTE_NAME, modelName);
            sbmlElement.appendChild(modelElement);

            double speciesPopulationConversionMultiplier = 1.0;

            HashMap globalSymbolValues = new HashMap();

            // set volume units to dimensionless, and substance units to "item"
            Element listOfUnitDefinitionsElement = document.createElement(ELEMENT_NAME_LIST_OF_UNIT_DEFINITIONS);
            modelElement.appendChild(listOfUnitDefinitionsElement);
            Element unitDefinitionElement = document.createElement(ELEMENT_NAME_UNIT_DEFINITION);
            listOfUnitDefinitionsElement.appendChild(unitDefinitionElement);

            Element listOfUnitsElement = document.createElement(ELEMENT_NAME_LIST_OF_UNITS);
            unitDefinitionElement.appendChild(listOfUnitsElement);

            unitDefinitionElement.setAttribute(ATTRIBUTE_NAME, UNIT_NAME_VOLUME);
            Element unit = document.createElement(ELEMENT_NAME_UNIT);
            listOfUnitsElement.appendChild(unit);
            unit.setAttribute(ATTRIBUTE_KIND, UNIT_KIND_DIMENSIONLESS);
                    
            unitDefinitionElement = document.createElement(ELEMENT_NAME_UNIT_DEFINITION);
            listOfUnitDefinitionsElement.appendChild(unitDefinitionElement);
            listOfUnitsElement = document.createElement(ELEMENT_NAME_LIST_OF_UNITS);
            unitDefinitionElement.appendChild(listOfUnitsElement);
            unitDefinitionElement.setAttribute(ATTRIBUTE_NAME, UNIT_NAME_SUBSTANCE);
            unit = document.createElement(ELEMENT_NAME_UNIT);
            listOfUnitsElement.appendChild(unit);
            unit.setAttribute(ATTRIBUTE_KIND, UNIT_KIND_ITEM);                    

            // get collection of all symbols
            Collection symbols = pModel.getSymbols();
            Iterator symbolsIter = symbols.iterator();
            List compartmentsList = new LinkedList();
            List speciesList = new LinkedList();
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
            Element listOfCompartmentsElement = document.createElement(ELEMENT_NAME_LIST_OF_COMPARTMENTS);
            modelElement.appendChild(listOfCompartmentsElement);
            Iterator compartmentsIter = compartmentsList.iterator();
            ArrayList rules = new ArrayList();
            while(compartmentsIter.hasNext())
            {
                Compartment compartment = (Compartment) compartmentsIter.next();
                Element compartmentElement = document.createElement(ELEMENT_NAME_COMPARTMENT);
                String compartmentName = compartment.getName();
                compartmentElement.setAttribute(ATTRIBUTE_NAME, convertSymbolToLegalSName(compartmentName));
                Value volumeValueObj = compartment.getValue();
                if(volumeValueObj.isExpression())
                {
                    rules.add(compartment);
                }
                else
                {
                    double volumeLiters = volumeValueObj.getValue();
                    Double volumeLitersObj = new Double(volumeLiters);
                    compartmentElement.setAttribute(ATTRIBUTE_VOLUME, volumeLitersObj.toString());
                    globalSymbolValues.put(compartmentName, compartment);
                }
                listOfCompartmentsElement.appendChild(compartmentElement);
            }

            // handle species
            Element listOfSpeciesElement = document.createElement(ELEMENT_NAME_LIST_OF_SPECIES);
            modelElement.appendChild(listOfSpeciesElement);

            Collection dynamicSymbols = pModel.getDynamicSymbols();
            Iterator dynamicSymbolsIter = dynamicSymbols.iterator();
            HashSet dynamicSymbolNames = new HashSet();
            while(dynamicSymbolsIter.hasNext())
            {
                SymbolValue dynamicSymbol = (SymbolValue) dynamicSymbolsIter.next();
                dynamicSymbolNames.add(dynamicSymbol.getSymbol().getName());
            }

            Iterator speciesIter = speciesList.iterator();
            while(speciesIter.hasNext())
            {
                Species species = (Species) speciesIter.next();
                Element speciesElement = null;
                if(pSpecification.equals(Specification.LEVEL1_VERSION1))
                {
                    speciesElement = document.createElement(ELEMENT_NAME_SPECIES_V1);
                }
                else
                {
                    speciesElement = document.createElement(ELEMENT_NAME_SPECIES_V2);
                }
                String speciesName = species.getName();
                speciesElement.setAttribute(ATTRIBUTE_NAME, convertSymbolToLegalSName(speciesName));
                Boolean boundaryObj = new Boolean(! dynamicSymbolNames.contains(speciesName));
                speciesElement.setAttribute(ATTRIBUTE_BOUNDARY_CONDITION, boundaryObj.toString());
                Compartment compartment = species.getCompartment();
                String compartmentName = compartment.getName();
                speciesElement.setAttribute(ATTRIBUTE_COMPARTMENT, convertSymbolToLegalSName(compartmentName));
                double initialSpeciesPopulation = 0.0;
                Value initialValueObj = species.getValue();
                if(initialValueObj.isExpression())
                {
                    rules.add(species);
                }
                else
                {
                    initialSpeciesPopulation = initialValueObj.getValue();
                    double initialSpeciesPopulationConverted = ((double) initialSpeciesPopulation) * speciesPopulationConversionMultiplier;
                    Double initialSpeciesPopulationObj = new Double(initialSpeciesPopulationConverted);
                    speciesElement.setAttribute(ATTRIBUTE_INITIAL_AMOUNT, initialSpeciesPopulationObj.toString());
                    globalSymbolValues.put(speciesName, species);
                }
                listOfSpeciesElement.appendChild(speciesElement);
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

            Element listOfParametersElement = document.createElement(ELEMENT_NAME_LIST_OF_PARAMETERS);
            Iterator parametersIter = parametersList.iterator();
            if(parametersIter.hasNext())
            {
                modelElement.appendChild(listOfParametersElement);
            }
            while(parametersIter.hasNext())
            {
                Parameter parameter = (Parameter) parametersIter.next();
                Element parameterElement = document.createElement(ELEMENT_NAME_PARAMETER);
                String parameterName = parameter.getName();
                parameterElement.setAttribute(ATTRIBUTE_NAME, convertSymbolToLegalSName(parameterName));
                Value paramValueObj = parameter.getValue();
                if(paramValueObj.isExpression())
                {
                    rules.add(parameter);
                }
                else
                {
                    double parameterValue = paramValueObj.getValue();
                    Double parameterValueObj = new Double(parameterValue);
                    parameterElement.setAttribute(ATTRIBUTE_VALUE, parameterValueObj.toString());
                    globalSymbolValues.put(parameterName, parameter);
                }
                listOfParametersElement.appendChild(parameterElement);
            }

            // handle rules

            Element listOfRulesElement = document.createElement(ELEMENT_NAME_LIST_OF_RULES);

            ListIterator rulesListIter = null;
            boolean useExpressionValueCaching = false;
            SymbolEvaluatorHashMap symbolEvaluator = new SymbolEvaluatorHashMap(globalSymbolValues, useExpressionValueCaching);
            HashSet reservedSymbolNames = new HashSet();
            SymbolEvaluatorChemSimulation.getReservedSymbolNames(reservedSymbolNames);
            Iterator reservedSymbolNamesIter = reservedSymbolNames.iterator();
            while(reservedSymbolNamesIter.hasNext())
            {
                String reservedSymbolName = (String) reservedSymbolNamesIter.next();
                globalSymbolValues.put(reservedSymbolName, new SymbolValue(reservedSymbolName, 0.0));
            }

            Vector orderedRules = new Vector();

            while(rules.size() > 0)
            {
                rulesListIter = rules.listIterator();
                boolean success = false;
                while(rulesListIter.hasNext())
                {
                    SymbolValue ruleObj = (SymbolValue) rulesListIter.next();
                    Value ruleValue = ruleObj.getValue();
                    assert (ruleValue.isExpression()) : "encountered a rule object with non-expression value";
                    try
                    {
                        ruleValue.getValue(symbolEvaluator);
                        orderedRules.add(ruleObj);
                        rulesListIter.remove();
                        globalSymbolValues.put(ruleObj.getSymbol().getName(), ruleObj);
                        success = true;
                    }
                    catch(DataNotFoundException e)
                    {
                        // dependent on stuff that has not been defined yet
                    }
                }
                if(! success && rules.size() > 0)
                {
                    throw new ModelExporterException("cannot export model that contains cyclic rule dependencies");
                }
            }


            modelElement.appendChild(listOfRulesElement);
            Iterator rulesIter = orderedRules.iterator();
            while(rulesIter.hasNext())
            {
                SymbolValue rule = (SymbolValue) rulesIter.next();
                String name = rule.getSymbol().getName();
                if(rule instanceof Compartment)
                {
                    Compartment compartment = (Compartment) rule;
                    Value compartmentVolumeObj = compartment.getValue();
                    assert (compartmentVolumeObj.isExpression()) : "compartment value not an expression";
                    Element compartmentRuleElement = document.createElement(ELEMENT_NAME_COMPARTMENT_VOLUME_RULE);
                    compartmentRuleElement.setAttribute(ATTRIBUTE_COMPARTMENT, convertSymbolToLegalSName(name));
                    compartmentRuleElement.setAttribute(ATTRIBUTE_FORMULA, convertSymbolToLegalSName(compartmentVolumeObj.getExpressionString()));
                    listOfRulesElement.appendChild(compartmentRuleElement);
                }
                else if(rule instanceof Species)
                {
                    Species species = (Species) rule;
                    Value speciesConcentrationObj = species.getValue();
                    assert (speciesConcentrationObj.isExpression()) : "species value not an expression";
                    Element speciesConcentrationRuleElement = document.createElement(ELEMENT_NAME_SPECIES_CONCENTRATION_RULE);
                    if(pSpecification.equals(Specification.LEVEL1_VERSION1))
                    {
                        speciesConcentrationRuleElement.setAttribute(ATTRIBUTE_SPECIES_V1, convertSymbolToLegalSName(name));
                    }
                    else
                    {
                        speciesConcentrationRuleElement.setAttribute(ATTRIBUTE_SPECIES_V2, convertSymbolToLegalSName(name));
                    }
                    speciesConcentrationRuleElement.setAttribute(ATTRIBUTE_FORMULA, convertSymbolToLegalSName(speciesConcentrationObj.getExpressionString()));
                    listOfRulesElement.appendChild(speciesConcentrationRuleElement);
                }
                else if(rule instanceof Parameter)
                {
                    Parameter parameter = (Parameter) rule;
                    Value parameterValueObj = parameter.getValue();
                    Element parameterRuleElement = document.createElement(ELEMENT_NAME_PARAMETER_RULE);
                    assert (parameterValueObj.isExpression()) : "parameter value not an expression";
                    parameterRuleElement.setAttribute(ATTRIBUTE_NAME, convertSymbolToLegalSName(name));
                    parameterRuleElement.setAttribute(ATTRIBUTE_FORMULA, convertSymbolToLegalSName(parameterValueObj.getExpressionString()));
                    listOfRulesElement.appendChild(parameterRuleElement);
                }
                else
                {
                    assert false : "unknown rule object encountered";
                }
            }

            // handle reactions
            Element listOfReactionsElement = document.createElement(ELEMENT_NAME_LIST_OF_REACTIONS);
            modelElement.appendChild(listOfReactionsElement);
            
            Collection reactionsColl = pModel.getReactions();
            List reactionsList = new LinkedList(reactionsColl);
            Collections.sort(reactionsList);
            Iterator reactionsIter = reactionsList.iterator();
            while(reactionsIter.hasNext())
            {
                Reaction reaction = (Reaction) reactionsIter.next();
                int numReactionSteps = reaction.getNumSteps();
                String reactionName = reaction.getName();
                Element reactionElement = document.createElement(ELEMENT_NAME_REACTION);
                listOfReactionsElement.appendChild(reactionElement);
                reactionElement.setAttribute(ATTRIBUTE_NAME, convertSymbolToLegalSName(reactionName));
                reactionElement.setAttribute(ATTRIBUTE_REVERSIBLE, "false");

                // handle reactant species
                int numReactants = reaction.getNumParticipants(Reaction.ParticipantType.REACTANT);
                Species []reactantSpecies = new Species[numReactants];
                int []reactantStoichiometries = new int[numReactants];
                boolean []reactantDynamic = new boolean[numReactants];
                reaction.constructSpeciesArrays(reactantSpecies, 
                                                reactantStoichiometries, 
                                                reactantDynamic, 
                                                null, null, null, 
                                                Reaction.ParticipantType.REACTANT);

                Element listOfReactantsElement = document.createElement(ELEMENT_NAME_LIST_OF_REACTANTS);
                reactionElement.appendChild(listOfReactantsElement);
                for(int ctr = 0; ctr < numReactants; ++ctr)
                {
                    Species species = (Species) reactantSpecies[ctr];
                    int stoic = reactantStoichiometries[ctr];
                    String speciesName = species.getName();
                    Element specieReferenceElement = null;
                    if(pSpecification.equals(Specification.LEVEL1_VERSION1))
                    {
                        specieReferenceElement = document.createElement(ELEMENT_NAME_SPECIES_REFERENCE_V1);
                    }
                    else
                    {
                        specieReferenceElement = document.createElement(ELEMENT_NAME_SPECIES_REFERENCE_V2); 
                    }
                    if(pSpecification.equals(Specification.LEVEL1_VERSION1))
                    {
                        specieReferenceElement.setAttribute(ATTRIBUTE_SPECIES_V1, convertSymbolToLegalSName(speciesName));
                    }
                    else
                    {
                        specieReferenceElement.setAttribute(ATTRIBUTE_SPECIES_V2, convertSymbolToLegalSName(speciesName));
                    }
                    specieReferenceElement.setAttribute(ATTRIBUTE_STOICHIOMETRY, Integer.toString(stoic));
                    listOfReactantsElement.appendChild(specieReferenceElement);
                }

                // handle product species
                int numProducts = reaction.getNumParticipants(Reaction.ParticipantType.PRODUCT);
                Species []productSpecies = new Species[numProducts];
                int []productStoichiometries = new int[numProducts];
                boolean []productDynamic = new boolean[numProducts];
                reaction.constructSpeciesArrays(productSpecies, 
                                                productStoichiometries, 
                                                productDynamic, 
                                                null, null, null, 
                                                Reaction.ParticipantType.PRODUCT);

                Element listOfProductsElement = document.createElement(ELEMENT_NAME_LIST_OF_PRODUCTS);
                reactionElement.appendChild(listOfProductsElement);
                for(int ctr = 0; ctr < numProducts; ++ctr)
                {
                    Species species = (Species) productSpecies[ctr];
                    int stoic = productStoichiometries[ctr];
                    String speciesName = species.getName();
                    Element specieReferenceElement = null;
                    if(pSpecification.equals(Specification.LEVEL1_VERSION1))
                    {
                        specieReferenceElement = document.createElement(ELEMENT_NAME_SPECIES_REFERENCE_V1);
                    }
                    else
                    {
                        specieReferenceElement = document.createElement(ELEMENT_NAME_SPECIES_REFERENCE_V2); 
                    }
                    if(pSpecification.equals(Specification.LEVEL1_VERSION1))
                    {
                        specieReferenceElement.setAttribute(ATTRIBUTE_SPECIES_V1, convertSymbolToLegalSName(speciesName));
                    }
                    else
                    {
                        specieReferenceElement.setAttribute(ATTRIBUTE_SPECIES_V2, convertSymbolToLegalSName(speciesName));
                    }
                    specieReferenceElement.setAttribute(ATTRIBUTE_STOICHIOMETRY, Integer.toString(stoic));
                    listOfProductsElement.appendChild(specieReferenceElement);
                }

                // handle kinetic law
                Element kineticLawElement = document.createElement(ELEMENT_NAME_KINETIC_LAW);
                reactionElement.appendChild(kineticLawElement);
                listOfParametersElement = document.createElement(ELEMENT_NAME_LIST_OF_PARAMETERS);
                boolean addParametersElement = false;
                Value rateValue = reaction.getValue();
                String kineticLaw = null;
                if(rateValue.isExpression())
                {
                        kineticLaw = rateValue.getExpressionString();
                }
                else
                {
                    double rescaledReactionParameter = rateValue.getValue();
                    StringBuffer kineticLawBuf = new StringBuffer();
                    String reactionParameterSymbolName = DEFAULT_REACTION_PARAMETER_SYMBOL_NAME;
                    kineticLawBuf.append(reactionParameterSymbolName);
                    if(numReactants > 0)
                    {
                        kineticLawBuf.append("*");
                    }
                    Element reactionParameterElement = document.createElement(ELEMENT_NAME_PARAMETER);
                    reactionParameterElement.setAttribute(ATTRIBUTE_NAME, convertSymbolToLegalSName(reactionParameterSymbolName));
                    listOfParametersElement.appendChild(reactionParameterElement);
                    
                    for(int ctr = 0; ctr < numReactants; ++ctr)
                    {
                        Species species = reactantSpecies[ctr];
                        int stoic = reactantStoichiometries[ctr];
                        assert (stoic > 0) : "invalid stoichiometry";
                        String reactantName = species.getName();
                        Compartment compartment = species.getCompartment();
                        double compartmentVolumeLiters = compartment.getValue().getValue();
                        
                        rescaledReactionParameter /= speciesPopulationConversionMultiplier;
                        rescaledReactionParameter /= ((double) MathFunctions.factorial(stoic));
                        if(1 == numReactionSteps)
                        {
                            double delayTime = reaction.getDelay();
                            if(delayTime == 0.0)
                            {
                                kineticLawBuf.append(reactantName);
                            }
                            else
                            {
                                if(rescaledReactionParameter == 0.0)
                                {
                                    throw new IllegalArgumentException("delayed reaction with zero reaction parameter");
                                }
                                kineticLawBuf.append("(delay(" + reactantName + ", " + delayTime + "))");
                            }
                        }
                        else
                        {
                            if(rescaledReactionParameter == 0.0)
                            {
                                throw new IllegalArgumentException("multistep reaction with zero reaction parameter");
                            }
                            double delayTime = ((double) numReactionSteps) / rescaledReactionParameter;
                            kineticLawBuf.append("(delay(" + reactantName + ", " + delayTime + "))");
                        }
                        if(1 != stoic)
                        {
                            kineticLawBuf.append("^" + stoic);
                        }
                        if(ctr < numReactants - 1)
                        {
                            kineticLawBuf.append("*");
                        }
                    }

                    kineticLaw = kineticLawBuf.toString();
                    Double rescaledReactionParameterObj = new Double(rescaledReactionParameter);
                    reactionParameterElement.setAttribute(ATTRIBUTE_VALUE, rescaledReactionParameterObj.toString());
                    addParametersElement = true;
                }
                kineticLawElement.setAttribute(ATTRIBUTE_FORMULA, convertSymbolToLegalSName(kineticLaw));
                // handle reaction parameters

                parametersList = new LinkedList(reaction.getParameters());
                Collections.sort(parametersList);
                parametersIter = parametersList.iterator();
                if(parametersIter.hasNext())
                {
                    addParametersElement = true;
                }
                while(parametersIter.hasNext())
                {
                    Parameter parameter = (Parameter) parametersIter.next();
                    Element parameterElement = document.createElement(ELEMENT_NAME_PARAMETER);
                    listOfParametersElement.appendChild(parameterElement);
                    parameterElement.setAttribute(ATTRIBUTE_NAME, convertSymbolToLegalSName(parameter.getName()));
                    Value valueObj = parameter.getValue();
                    if(valueObj.isExpression())
                    {
                        throw new UnsupportedOperationException("Not able to export this model to markup language, because the model contains reaction containing a parameter whose value is a mathematical expression.  In SBML level 1, a parameter must have its value defined as a fixed number.  Parameter is: " + parameter.getName());
                    }
                    parameterElement.setAttribute(ATTRIBUTE_VALUE, Double.toString(valueObj.getValue()));
                }
                if(addParametersElement)
                {
                    kineticLawElement.appendChild(listOfParametersElement);
                }
            }

            // write out the XML 
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();   
            transformer.setOutputProperty("indent", "yes");
            Properties properties = transformer.getOutputProperties();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(pOutputWriter);
            transformer.transform(source, result);
        }

        catch(DOMException e)
        {
            throw new ModelExporterException("error in creating the DOM model of the markup language output", e);
        }

        catch(ParserConfigurationException e)
        {
            throw new ModelExporterException("error in configuring the parser for writing the markup language output", e);
        }

        catch(TransformerException e)
        {
            throw new ModelExporterException("error in transforming the DOM model into the markup language output", e);
        }
    }

    public String getFileRegex()
    {
        return(".*\\.(xml|sbml)$");
    }
}
