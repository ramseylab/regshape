package isb.chem.scripting;

import isb.chem.*;
import isb.util.*;
import java.io.*;
import java.util.*;

/**
 * Default model description writer.  Just writes
 * a command language description of the model and
 * species populations data structure.
 *
 * @author Stephen Ramsey
 */
public class CommandLanguageModelInstanceExporter implements IModelInstanceExporter, IAliasableClass
{
    public static final String CLASS_ALIAS = "command-language";

    public void exportModelInstance(Model pModel, SpeciesPopulations pInitialSpeciesPopulations, PrintWriter pOutputWriter) throws DataNotFoundException
    {
        Script script = new Script();

        Set speciesSet = pModel.getSpeciesSetCopy();
        Set compartmentsSet = Model.getCompartmentsSetCopy(speciesSet);
        List compartmentsList = new LinkedList(compartmentsSet);
        Collections.sort(compartmentsList);
        Iterator compartmentsIter = compartmentsList.iterator();
        String defaultCompartmentName = null;
        while(compartmentsIter.hasNext())
        {
            Compartment compartment = (Compartment) compartmentsIter.next();
            String compartmentName = compartment.getName();
            double compartmentVolumeLiters = compartment.getVolumeLiters();
            Statement compartmentStatement = new Statement(Statement.Type.COMPARTMENT);
            compartmentStatement.setName(compartmentName);
            Element volumeElement = new Element(Element.Type.VOLUME);
            volumeElement.setNumberValue(new Double(compartmentVolumeLiters));
            compartmentStatement.putElement(volumeElement);
            script.addStatement(compartmentStatement);
        }

        List speciesList = new LinkedList(speciesSet);
        Collections.sort(speciesList);
        Iterator speciesIter = speciesList.iterator();
            
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            String speciesName = species.getName();
            Compartment compartment = species.getCompartmentCopy();
            String compartmentName = compartment.getName();
            boolean floating = species.getFloating();

            Statement speciesStatement = new Statement(Statement.Type.SPECIES);
            speciesStatement.setName(speciesName);
            Element speciesTypeElement = new Element(Element.Type.SPECIESTYPE);
                
            if(floating)
            {
                speciesTypeElement.setModifier(Element.ModifierCode.FLOATING);
            }
            else
            {
                speciesTypeElement.setModifier(Element.ModifierCode.BOUNDARY);
            }

            speciesStatement.putElement(speciesTypeElement);

            Element compartmentElement = new Element(Element.Type.COMPARTMENT);
            compartmentElement.setSymbolName(compartmentName);

            speciesStatement.putElement(compartmentElement);

            script.addStatement(speciesStatement);
        }

        Set parametersSet = pModel.getParametersSetCopy();
        List parametersList = new LinkedList(parametersSet);
        Collections.sort(parametersList);
        Iterator parametersIter = parametersList.iterator();

        while(parametersIter.hasNext())
        {
            Parameter parameter = (Parameter) parametersIter.next();
            String parameterName = parameter.getName();
            double parameterValue = parameter.getValue();
            Statement parameterStatement = new Statement(Statement.Type.PARAMETER);
            parameterStatement.setName(parameterName);
            Element valueElement = new Element(Element.Type.VALUE);
            valueElement.setNumberValue(new Double(parameterValue));
            parameterStatement.putElement(valueElement);
            script.addStatement(parameterStatement);
        }

        HashMap reactionParameters = new HashMap();
        Vector reactionNames = new Vector();

        Iterator reactionsIter = pModel.getReactionsOrderedIterCopy();
        while(reactionsIter.hasNext())
        {
            Reaction reaction = (Reaction) reactionsIter.next();
            String reactionName = reaction.getName();
            reactionNames.add(reactionName);
            parametersSet = reaction.getParametersSetCopy();
            parametersList = new LinkedList(parametersSet);
            Collections.sort(parametersList);
            parametersIter = parametersList.iterator();

            Statement reactionStatement = new Statement(Statement.Type.REACTION);
            reactionStatement.setName(reactionName);

            Element reactantsElement = new Element(Element.Type.REACTANTS);
            reactantsElement.createDataList();
            Vector reactants = new Vector();
            reaction.getReactantsCopy(reactants);
            speciesIter = reactants.iterator();
            while(speciesIter.hasNext())
            {
                Species species = (Species) speciesIter.next();
                String speciesName = species.getName();
                Integer stoicObj = reaction.getReactantMultiplicity(species);
                int stoic = stoicObj.intValue();
                for(int stoicCtr = 0; stoicCtr < stoic; ++stoicCtr)
                {
                    reactantsElement.addDataToList(speciesName);
                }
            }
            reactionStatement.putElement(reactantsElement);
                
            Element productsElement = new Element(Element.Type.PRODUCTS);
            productsElement.createDataList();
            Vector products = new Vector();
            reaction.getProductsCopy(products);
            speciesIter = products.iterator();
            while(speciesIter.hasNext())
            {
                Species species = (Species) speciesIter.next();
                String speciesName = species.getName();
                Integer stoicObj = reaction.getProductMultiplicity(species);
                int stoic = stoicObj.intValue();
                for(int stoicCtr = 0; stoicCtr < stoic; ++stoicCtr)
                {
                    productsElement.addDataToList(speciesName);
                }
            }
            reactionStatement.putElement(productsElement);

            Element rateElement = new Element(Element.Type.RATE);
            MathExpression rateExpression = reaction.getRateExpressionCopy();
            if(null == rateExpression)
            {
                rateElement.setNumberValue(reaction.getReactionRateParameter());
            }
            else
            {
                rateElement.setSymbolName(rateExpression.toString());
            }
            reactionStatement.putElement(rateElement);

            while(parametersIter.hasNext())
            {
                Parameter parameter = (Parameter) parametersIter.next();
                Vector paramReactions = (Vector) reactionParameters.get(parameter);
                if(null == paramReactions)
                {
                    paramReactions = new Vector();
                    reactionParameters.put(parameter, paramReactions);
                }
                paramReactions.add(reaction);
            }

            script.addStatement(reactionStatement);
        }

        Iterator reactionParamsIter = reactionParameters.keySet().iterator();
        while(reactionParamsIter.hasNext())
        {
            Parameter parameter = (Parameter) reactionParamsIter.next();
            String parameterName = parameter.getName();
            Double valueObj = new Double(parameter.getValue());
            Statement paramStatement = new Statement(Statement.Type.PARAMETER);
            paramStatement.setName(parameterName);
            Element valueElement = new Element(Element.Type.VALUE);
            valueElement.setNumberValue(valueObj);
            paramStatement.putElement(valueElement);
            Element reactionsElement = new Element(Element.Type.REACTIONS);
            reactionsElement.createDataList();
            Vector paramReactions = (Vector) reactionParameters.get(parameter);
            reactionsIter = paramReactions.iterator();
            while(reactionsIter.hasNext())
            {
                Reaction reaction = (Reaction) reactionsIter.next();
                String reactionName = reaction.getName();
                reactionsElement.addDataToList(reactionName);
            }
            paramStatement.putElement(reactionsElement);
            script.addStatement(paramStatement);
        }

        String modelName = pModel.getName();
        ReactionRateSpeciesMode reactionRateSpeciesMode = pModel.getReactionRateSpeciesMode();
        Statement modelStatement = new Statement(Statement.Type.MODEL);
        modelStatement.setName(modelName);
        Element speciesModeElement = new Element(Element.Type.SPECIESMODE);
        if(reactionRateSpeciesMode.equals(ReactionRateSpeciesMode.MOLECULES))
        {
            speciesModeElement.setModifier(Element.ModifierCode.MOLECULES);
        }
        else
        {
            if(reactionRateSpeciesMode.equals(ReactionRateSpeciesMode.CONCENTRATION))
            {
                speciesModeElement.setModifier(Element.ModifierCode.CONCENTRATION);
            }
            else
            {
                throw new IllegalArgumentException("unknown reaction rate species mode: " + reactionRateSpeciesMode);
            }
        }
        modelStatement.putElement(speciesModeElement);
        Element reactionsElement = new Element(Element.Type.REACTIONS);
        reactionsElement.createDataList();
        reactionsIter = reactionNames.iterator();
        while(reactionsIter.hasNext())
        {
            String reactionName = (String) reactionsIter.next();
            reactionsElement.addDataToList(reactionName);
        }
        modelStatement.putElement(reactionsElement);
        script.addStatement(modelStatement);

        speciesIter = speciesList.iterator();
            
        Statement speciesPopulationsStatement = new Statement(Statement.Type.SPECIESPOPULATIONS);
        String speciesPopulationsName = pInitialSpeciesPopulations.getName();
        speciesPopulationsStatement.setName(speciesPopulationsName);
        Element speciesListElement = new Element(Element.Type.SPECIESLIST);
        speciesListElement.createDataList();
        Element populationsElement = new Element(Element.Type.POPULATIONS);
        populationsElement.createDataList();
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            String speciesName = species.getName();
            speciesListElement.addDataToList(speciesName);
            if(! pInitialSpeciesPopulations.speciesPopulationIsExpression(speciesName))
            {
                long speciesPopulation = pInitialSpeciesPopulations.getSpeciesPopulation(speciesName);
                populationsElement.addDataToList(new Double(speciesPopulation));
            }
            else
            {
                MathExpression speciesPopulationExpression = pInitialSpeciesPopulations.getSpeciesPopulationExpressionCopy(speciesName);
                populationsElement.addDataToList(speciesPopulationExpression.toString());
            }
        }
        speciesPopulationsStatement.putElement(speciesListElement);
        speciesPopulationsStatement.putElement(populationsElement);
        script.addStatement(speciesPopulationsStatement);

        pOutputWriter.println(script.toString());
        pOutputWriter.flush();
    }

    public String getFileRegex()
    {
        return(".*\\.isbchem$");
    }
}
