package isb.chem.sbw;

import isb.chem.*;
import isb.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

import isb.chem.scripting.IModelInstanceExporter;
import isb.chem.scripting.ModelInstanceExporterException;

/**
 * Given a {@link isb.chem.Model} and a {@link isb.chem.SpeciesPopulations}
 * defining a system of chemical reactions and the initial species populations,
 * writes out an SBML level 1 description of the model and initial species
 * populations, to the specified output stream.
 *
 * @see isb.chem.Model
 * @see isb.chem.SpeciesPopulations
 *
 * @author Stephen Ramsey
 */
public class MarkupLanguageModelInstanceExporter implements IModelInstanceExporter, IAliasableClass
{
    public static final String CLASS_ALIAS = "markup-language";

    private static final String ELEMENT_NAME_SBML = "sbml";
    private static final String ELEMENT_NAME_MODEL = "model";
    private static final String ELEMENT_NAME_LIST_OF_COMPARTMENTS = "listOfCompartments";
    private static final String ELEMENT_NAME_LIST_OF_SPECIES = "listOfSpecies";
    private static final String ELEMENT_NAME_LIST_OF_PARAMETERS = "listOfParameters";
    private static final String ELEMENT_NAME_SPECIE = "specie";
    private static final String ELEMENT_NAME_COMPARTMENT = "compartment";
    private static final String ELEMENT_NAME_PARAMETER = "parameter";
    private static final String ELEMENT_NAME_LIST_OF_REACTIONS = "listOfReactions";
    private static final String ELEMENT_NAME_REACTION = "reaction";
    private static final String ELEMENT_NAME_LIST_OF_REACTANTS = "listOfReactants";
    private static final String ELEMENT_NAME_LIST_OF_PRODUCTS = "listOfProducts";
    private static final String ELEMENT_NAME_SPECIE_REFERENCE = "specieReference";
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
    private static final String ATTRIBUTE_SPECIE = "specie";
    private static final String ATTRIBUTE_REVERSIBLE = "reversible";
    private static final String ATTRIBUTE_COMPARTMENT = "compartment";
    private static final String ATTRIBUTE_KIND = "kind";

    private static final String UNIT_NAME_VOLUME = "volume";
    private static final String UNIT_NAME_SUBSTANCE = "substance";
    private static final String UNIT_KIND_DIMENSIONLESS = "dimensionless";
    private static final String UNIT_KIND_ITEM = "item";

    private static final String XMLNS_NAME = "xmlns";
    private static final String XMLNS_VALUE = "http://www.sbml.org/sbml/level1";
    private static final String LEVEL_NAME = "level";
    private static final String LEVEL_VALUE = "1";
    private static final String VERSION_NAME = "version";
    private static final String VERSION_VALUE = "1";

    private static final String DEFAULT_REACTION_PARAMETER_SYMBOL_NAME = "__RATE__";


   /**
    * Given a {@link isb.chem.Model} and a {@link isb.chem.SpeciesPopulations}
    * defining a system of chemical reactions and the initial species populations,
    * writes out the SBML description of the model and initial species
    * populations, to the output stream contained in <code>pOutputWriter</code>.
    */
    public void exportModelInstance(Model pModel, SpeciesPopulations pInitialSpeciesPopulations, PrintWriter pOutputWriter) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, ModelInstanceExporterException
    {
        try
        {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element sbmlElement = document.createElement(ELEMENT_NAME_SBML);
            sbmlElement.setAttribute(XMLNS_NAME, XMLNS_VALUE);
            sbmlElement.setAttribute(LEVEL_NAME, LEVEL_VALUE);
            sbmlElement.setAttribute(VERSION_NAME, VERSION_VALUE);
            document.appendChild(sbmlElement);

            Element modelElement = document.createElement(ELEMENT_NAME_MODEL);
            String modelName = pModel.getName();
            if(null == modelName)
            {
                throw new IllegalArgumentException("model name missing");
            }
            modelElement.setAttribute(ATTRIBUTE_NAME, modelName);
            sbmlElement.appendChild(modelElement);


            ReactionRateSpeciesMode reactionRateSpeciesMode = pModel.getReactionRateSpeciesMode();
            double speciesPopulationConversionMultiplier = 1.0;
            boolean convertVolume = false;
            if(reactionRateSpeciesMode.equals(ReactionRateSpeciesMode.CONCENTRATION))
            {
                // do nothing; the SBML default units (volume=liters, substance=moles) will suffice
                speciesPopulationConversionMultiplier = 1.0 / Constants.AVOGADRO_CONSTANT;
                convertVolume = true;
            }
            else
            {
                if(reactionRateSpeciesMode.equals(ReactionRateSpeciesMode.MOLECULES))
                {
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
                }
                else
                {
                    throw new IllegalArgumentException("unknown reaction rate species mode: " + reactionRateSpeciesMode);
                }
            }

            // handle compartments
            Element listOfCompartmentsElement = document.createElement(ELEMENT_NAME_LIST_OF_COMPARTMENTS);
            modelElement.appendChild(listOfCompartmentsElement);
            Set speciesSet = pModel.getSpeciesSetCopy();
            Set compartmentsSet = Model.getCompartmentsSetCopy(speciesSet);
            List compartmentsList = new LinkedList(compartmentsSet);
            Collections.sort(compartmentsList);
            Iterator compartmentsIter = compartmentsList.iterator();
            while(compartmentsIter.hasNext())
            {
                Compartment compartment = (Compartment) compartmentsIter.next();
                Element compartmentElement = document.createElement(ELEMENT_NAME_COMPARTMENT);
                String compartmentName = compartment.getName();
                compartmentElement.setAttribute(ATTRIBUTE_NAME, compartmentName);
                double volumeLiters = compartment.getVolumeLiters();
                Double volumeLitersObj = new Double(volumeLiters);
                compartmentElement.setAttribute(ATTRIBUTE_VOLUME, volumeLitersObj.toString());
                listOfCompartmentsElement.appendChild(compartmentElement);
            }

            // handle species
            Element listOfSpeciesElement = document.createElement(ELEMENT_NAME_LIST_OF_SPECIES);
            modelElement.appendChild(listOfSpeciesElement);

            List speciesList = new LinkedList(speciesSet);
            Collections.sort(speciesList);
            Iterator speciesIter = speciesList.iterator();
            
            while(speciesIter.hasNext())
            {
                Species species = (Species) speciesIter.next();
                Element speciesElement = document.createElement(ELEMENT_NAME_SPECIE);
                String speciesName = species.getName();
                speciesElement.setAttribute(ATTRIBUTE_NAME, speciesName);
                Boolean boundaryObj = new Boolean(! species.getFloating());
                speciesElement.setAttribute(ATTRIBUTE_BOUNDARY_CONDITION, boundaryObj.toString());
                Compartment compartment = species.getCompartmentCopy();
                String compartmentName = compartment.getName();
                speciesElement.setAttribute(ATTRIBUTE_COMPARTMENT, compartmentName);
                double initialSpeciesPopulation = 0.0;
                try
                {
                    initialSpeciesPopulation = pInitialSpeciesPopulations.getSpeciesPopulation(speciesName);
                }
                catch(IllegalStateException e)
                {
                    throw new ModelInstanceExporterException("Not able to export this model to markup language, because the model contains a boundary (non-floating) species whose population is a mathematical expression.  In SBML, a boundary species must have its population defined as a fixed number.  Species is: " + speciesName);
                }
                double initialSpeciesPopulationConverted = ((double) initialSpeciesPopulation) * speciesPopulationConversionMultiplier;
                Double initialSpeciesPopulationObj = new Double(initialSpeciesPopulationConverted);
                speciesElement.setAttribute(ATTRIBUTE_INITIAL_AMOUNT, initialSpeciesPopulationObj.toString());
                listOfSpeciesElement.appendChild(speciesElement);
            }

            // handle global parameters
            Element listOfParametersElement = document.createElement(ELEMENT_NAME_LIST_OF_PARAMETERS);
            Set parametersSet = pModel.getParametersSetCopy();
            List parametersList = new LinkedList(parametersSet);
            Collections.sort(parametersList);
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
                parameterElement.setAttribute(ATTRIBUTE_NAME, parameterName);
                double parameterValue = parameter.getValue();
                Double parameterValueObj = new Double(parameterValue);
                parameterElement.setAttribute(ATTRIBUTE_VALUE, parameterValueObj.toString());
                listOfParametersElement.appendChild(parameterElement);
            }

            // handle reactions
            Element listOfReactionsElement = document.createElement(ELEMENT_NAME_LIST_OF_REACTIONS);
            modelElement.appendChild(listOfReactionsElement);
            
            Iterator reactionsIter = pModel.getReactionsOrderedIterCopy();
            while(reactionsIter.hasNext())
            {
                Reaction reaction = (Reaction) reactionsIter.next();
                String reactionName = reaction.getName();
                Element reactionElement = document.createElement(ELEMENT_NAME_REACTION);
                listOfReactionsElement.appendChild(reactionElement);
                reactionElement.setAttribute(ATTRIBUTE_NAME, reactionName);
                reactionElement.setAttribute(ATTRIBUTE_REVERSIBLE, "false");

                // handle reactant species
                Element listOfReactantsElement = document.createElement(ELEMENT_NAME_LIST_OF_REACTANTS);
                reactionElement.appendChild(listOfReactantsElement);
                Vector reactants = new Vector();
                reaction.getReactantsCopy(reactants);
                speciesIter = reactants.iterator();
                while(speciesIter.hasNext())
                {
                    Species species = (Species) speciesIter.next();
                    String speciesName = species.getName();
                    Integer stoic = reaction.getReactantMultiplicity(species);
                    Element specieReferenceElement = document.createElement(ELEMENT_NAME_SPECIE_REFERENCE);
                    specieReferenceElement.setAttribute(ATTRIBUTE_SPECIE, speciesName);
                    specieReferenceElement.setAttribute(ATTRIBUTE_STOICHIOMETRY, stoic.toString());
                    listOfReactantsElement.appendChild(specieReferenceElement);
                }

                // handle product species
                Element listOfProductsElement = document.createElement(ELEMENT_NAME_LIST_OF_PRODUCTS);
                reactionElement.appendChild(listOfProductsElement);
                Vector products = new Vector();
                reaction.getProductsCopy(products);
                speciesIter = products.iterator();
                while(speciesIter.hasNext())
                {
                    Species species = (Species) speciesIter.next();
                    String speciesName = species.getName();
                    Integer stoic = reaction.getProductMultiplicity(species);
                    Element specieReferenceElement = document.createElement(ELEMENT_NAME_SPECIE_REFERENCE);
                    specieReferenceElement.setAttribute(ATTRIBUTE_SPECIE, speciesName);
                    specieReferenceElement.setAttribute(ATTRIBUTE_STOICHIOMETRY, stoic.toString());
                    listOfProductsElement.appendChild(specieReferenceElement);
                }

                // handle kinetic law
                Element kineticLawElement = document.createElement(ELEMENT_NAME_KINETIC_LAW);
                reactionElement.appendChild(kineticLawElement);
                listOfParametersElement = document.createElement(ELEMENT_NAME_LIST_OF_PARAMETERS);
                boolean addParametersElement = false;
                MathExpression rateExpression = reaction.getRateExpressionCopy();
                String kineticLaw = null;
                if(null != rateExpression)
                {
                        kineticLaw = rateExpression.toString();
                }
                else
                {
                    Double reactionParameter = reaction.getReactionRateParameter();
                    if(null != reactionParameter)
                    {
                        double rescaledReactionParameter = reactionParameter.doubleValue();
                        StringBuffer kineticLawBuf = new StringBuffer();
                        Iterator reactantsIter = reactants.iterator();
                        String reactionParameterSymbolName = DEFAULT_REACTION_PARAMETER_SYMBOL_NAME;
                        kineticLawBuf.append(reactionParameterSymbolName);
                        Element reactionParameterElement = document.createElement(ELEMENT_NAME_PARAMETER);
                        reactionParameterElement.setAttribute(ATTRIBUTE_NAME, reactionParameterSymbolName);
                        listOfParametersElement.appendChild(reactionParameterElement);
                        
                        if(reactantsIter.hasNext())
                        {
                            kineticLawBuf.append("*");
                        }
                        while(reactantsIter.hasNext())
                        {
                            Species reactantSpecies = (Species) reactantsIter.next();
                            String reactantName = reactantSpecies.getName();
                            Compartment compartment = reactantSpecies.getCompartmentCopy();
                            double compartmentVolumeLiters = compartment.getVolumeLiters();
                            Integer stoic = reaction.getReactantMultiplicity(reactantSpecies);
                            
                            assert (null != stoic) : new String("unexpected null stoichimetry for reactant: " + reactantName);

                            rescaledReactionParameter /= speciesPopulationConversionMultiplier;
                            if(convertVolume)
                            {
                                rescaledReactionParameter *= compartmentVolumeLiters;
                            }
                            rescaledReactionParameter /= ((double) MathFunctions.factorial(stoic.intValue()));
                            kineticLawBuf.append(reactantName);
                            if(1 != stoic.intValue())
                            {
                                kineticLawBuf.append("^" + stoic);
                            }
                            if(reactantsIter.hasNext())
                            {
                                kineticLawBuf.append("*");
                            }
                        }
                        kineticLaw = kineticLawBuf.toString();
                        Double rescaledReactionParameterObj = new Double(rescaledReactionParameter);
                        reactionParameterElement.setAttribute(ATTRIBUTE_VALUE, rescaledReactionParameterObj.toString());
                        addParametersElement = true;
                    }
                    else
                    {
                        throw new IllegalStateException("reaction rate has not been defined for reaction: " + reaction.toString());
                    }
                }
                kineticLawElement.setAttribute(ATTRIBUTE_FORMULA, kineticLaw);
                // handle reaction parameters
                parametersSet = reaction.getParametersSetCopy();
                parametersList = new LinkedList(parametersSet);
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
                    parameterElement.setAttribute(ATTRIBUTE_NAME, parameter.getName());
                    Double valueObj = new Double(parameter.getValue());
                    parameterElement.setAttribute(ATTRIBUTE_VALUE, valueObj.toString());
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
            throw new ModelInstanceExporterException("error in creating the DOM model of the markup language output", e);
        }

        catch(ParserConfigurationException e)
        {
            throw new ModelInstanceExporterException("error in configuring the parser for writing the markup language output", e);
        }

        catch(TransformerException e)
        {
            throw new ModelInstanceExporterException("error in transforming the DOM model into the markup language output", e);
        }
    }

    public String getFileRegex()
    {
        return(".*\\.(xml|sbml)$");
    }
}
