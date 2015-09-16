/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.chem;

import java.io.PrintWriter;
import java.util.*;
import org.systemsbiology.util.*;
import org.systemsbiology.math.*;

/**
 * Saves a {@link org.systemsbiology.chem.Model} in the CMDL format.
 * 
 * @author sramsey
 *
 */
public class ModelExporterCommandLanguage implements IModelExporter, IAliasableClass
{
    public static final String CLASS_ALIAS = "command-language";

    class CMDLSymbolPrinter implements Expression.SymbolPrinter
    {
        private String mReactionName;
        private HashSet mLocalSymbols;
        private HashMap mSymbolsMap;
        private SymbolEvaluationPostProcessor mSymbolEvaluationPostProcessor;
        public CMDLSymbolPrinter(HashMap pSymbolsMap)
        {
            mReactionName = null;
            mLocalSymbols = null;
            mSymbolsMap = pSymbolsMap;
            mSymbolEvaluationPostProcessor = null;
        }
        public void setSymbolEvaluationPostProcessor(SymbolEvaluationPostProcessor pSymbolEvaluationPostProcessor)
        {
            mSymbolEvaluationPostProcessor = pSymbolEvaluationPostProcessor;
        }
        public String printSymbol(Symbol pSymbol) throws DataNotFoundException
        {
            String symbolName = pSymbol.getName();
            SymbolValue symbolValue = (SymbolValue) mSymbolsMap.get(symbolName);
            String translatedSymbolName = null;
            if(null != mLocalSymbols && null != mReactionName)
            {
                if(mLocalSymbols.contains(symbolName))
                {
                    translatedSymbolName = mReactionName + Model.INTERNAL_SYMBOL_PREFIX + symbolName;
                }
            } 
            if(null == translatedSymbolName)
            {
                if(null != mSymbolEvaluationPostProcessor)
                {
                    translatedSymbolName = mSymbolEvaluationPostProcessor.modifySymbol(pSymbol);
                }
                else
                {
                    translatedSymbolName = symbolName;
                }
            }
            return translateSymbolName(translatedSymbolName);
        }
        public void setLocalSymbolsAndReactionName(HashSet pLocalSymbols, String pReactionName)
        {
            mLocalSymbols = pLocalSymbols;
            mReactionName = pReactionName;
        }
    };    
    
    private CMDLSymbolPrinter mSymbolPrinter;
    
    public ModelExporterCommandLanguage()
    {
        mSymbolPrinter = null;
    }
    
    
    
    /**
     * Given a {@link org.systemsbiology.chem.Model} object
     * defining a system of chemical reactions and the initial species populations,
     * writes out the model in Chemical Model Definition Language (CMDL)
     */
     public void export(Model pModel, PrintWriter pOutputWriter) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException
     {
         HashMap symbolsMap = pModel.getSymbolsMap();
         Set keySet = symbolsMap.keySet();
         Iterator keyIter = keySet.iterator();
         SymbolValue obj = null;
         String symbolName = null;
         
         SymbolEvaluationPostProcessor symbolEvaluationPostProcessor = pModel.getSymbolEvaluationPostProcessor();
         CMDLSymbolPrinter symbolPrinter = new CMDLSymbolPrinter(symbolsMap); 
         mSymbolPrinter = symbolPrinter;
         symbolPrinter.setSymbolEvaluationPostProcessor(symbolEvaluationPostProcessor);
         
         LinkedList compartments = new LinkedList();
         LinkedList parameters = new LinkedList();
         LinkedList species = new LinkedList();
         LinkedList reactions = new LinkedList();
         
         while(keyIter.hasNext())
         {
             symbolName = (String) keyIter.next();
             obj = (SymbolValue) symbolsMap.get(symbolName);
             
             if(obj instanceof Compartment)
             {
                 compartments.add(obj);
             }
             else if(obj instanceof Parameter)
             {
                 parameters.add(obj);
             }
             else if(obj instanceof Species)
             {
                 species.add(obj);
             }
             else if(obj instanceof Reaction)
             {
                 reactions.add(obj);
             }
             else
             {
                 throw new IllegalStateException("unknown symbol in model: " + obj.toString());
             }

         }

         StringBuffer sb = new StringBuffer();
         
         Compartment defaultCompartment = ModelBuilderCommandLanguage.getDefaultCompartment(symbolsMap);

         Compartment compartment = null;
         Collections.sort(compartments);
         Iterator iter = compartments.iterator();
         while(iter.hasNext())
         {
             compartment = (Compartment) iter.next();
             if(null != defaultCompartment && compartment.equals(defaultCompartment))
             {
                 continue;
             }
             sb.append(translateSymbolName(compartment.getName()) + " = " + printValue(compartment) + ";\n");
         }         
         
         Collections.sort(parameters);
         iter = parameters.iterator();
         Parameter parameter = null;
         while(iter.hasNext())
         {
             parameter = (Parameter) iter.next();
             sb.append(translateSymbolName(parameter.getName()) + " = " + printValue(parameter) + ";\n");
         }

         Species specie = null;
         Collections.sort(species);
         iter = species.iterator();
         while(iter.hasNext())
         {
             specie = (Species) iter.next();
             compartment = specie.getCompartment();
             sb.append(specie.getName() + " = " + printValue(specie) + ";\n");
             if(null == defaultCompartment || ! compartment.equals(defaultCompartment))
             {
                 sb.append(translateSymbolName(specie.getName()) + " @ " + compartment.getName() + ";\n");
             };
         }
         
         Reaction reaction = null;
         Collections.sort(reactions);
         iter = reactions.iterator();
         while(iter.hasNext())
         {
             reaction = (Reaction) iter.next();
             String reactionName = reaction.getName();
             SymbolValue []localSymbolValues = reaction.getLocalSymbolValues();
             int numLocalSymbols = localSymbolValues.length;
             SymbolValue localSymbol = null;
             HashSet symbolValuesSet = new HashSet();
             for(int i = 0; i < numLocalSymbols; ++i)
             {
                 localSymbol = localSymbolValues[i];
                 symbolValuesSet.add(localSymbol.getSymbol().getName());
                 sb.append(reactionName + Model.INTERNAL_SYMBOL_PREFIX + localSymbol.getSymbol().getName() + " = ");
                 sb.append(printValue(localSymbol) + ";\n");
             }
             mSymbolPrinter.setLocalSymbolsAndReactionName(symbolValuesSet, reactionName);
             HashMap products = reaction.getProductsMap();
             HashMap reactants = reaction.getReactantsMap();
             ReactionParticipant reacParticipant = null;
             if(! reactionName.startsWith(Model.INTERNAL_SYMBOL_PREFIX))
             {
                 sb.append(translateSymbolName(reaction.getName()) + ", ");
             }
             String speciesName = null;
             Iterator spIter = reactants.keySet().iterator();
             int stoic = 0;
             while(spIter.hasNext())
             {
                 speciesName = (String) spIter.next();
                 reacParticipant = (ReactionParticipant) reactants.get(speciesName);
                 stoic = reacParticipant.mStoichiometry;
                 while(--stoic >= 0)
                 {
                     if(! reacParticipant.mDynamic)
                     {
                         sb.append("$");
                     }
                     sb.append(reacParticipant.mSpecies.getName());
                     if(stoic > 0 || spIter.hasNext())
                     {
                         sb.append(" + ");
                     }
                 }
             }
             sb.append(" -> ");
             spIter = products.keySet().iterator();
             while(spIter.hasNext())
             {
                 speciesName = (String) spIter.next();
                 reacParticipant = (ReactionParticipant) products.get(speciesName);
                 stoic = reacParticipant.mStoichiometry;
                 while(--stoic >= 0)
                 {
                     sb.append(reacParticipant.mSpecies.getName());
                     if(stoic > 0 || spIter.hasNext())
                     {
                         sb.append(" + ");
                     }
                 }
             }
             sb.append(", " + printValue(reaction));
             int numSteps = reaction.getNumSteps();
             if(numSteps > 1)
             {
                 sb.append(", steps: " + numSteps);
             }
             else 
             {
                 double delay = reaction.getDelay();
                 if(delay > 0.0)
                 {
                     sb.append(", delay: " + delay);
                 }
             }
             sb.append(";\n");
             mSymbolPrinter.setLocalSymbolsAndReactionName(null, null);
         }    
         
         pOutputWriter.println(sb.toString());
         pOutputWriter.flush();
     }
     
     private String printValue(SymbolValue pSymbolValue) throws DataNotFoundException
     {
         Value value = pSymbolValue.getValue();
         String retStr = null;
         if(value.isExpression())
         {
             return "[" + value.getExpressionString(mSymbolPrinter) + "]";
         }
         else
         {
             retStr = Double.toString(value.getValue());
         }
         return retStr;
     }

     private String translateSymbolName(String pSymbolName)
     {
         return pSymbolName.replaceAll(Model.NAMESPACE_IDENTIFIER, Model.INTERNAL_SYMBOL_PREFIX);
     }
     
     public String getFileRegex()
     {
         return(".*");
     }
}
