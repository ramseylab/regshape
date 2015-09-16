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
import org.systemsbiology.util.InvalidInputException;
import edu.caltech.sbw.SBWException;
import edu.caltech.sbml.SBMLReader;

/**
 * Proxy class for interrogating a parsed SBML document.
 * Uses the SBMLValidate.SBMLReader class to parse and
 * query an SBML document contained in a String.  Used by
 * the {@link ModelBuilderMarkupLanguage} class.
 *
 * @see org.systemsbiology.chem.sbml.ModelBuilderMarkupLanguage
 *
 * @author Stephen Ramsey
 *
 */
public class MarkupLanguageImporter
{
    /*========================================*
     * constants
     *========================================*/

    /*========================================*
     * member data
     *========================================*/
    private SBMLReader mSBMLReader;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setSBMLReader(SBMLReader pSBMLReader)
    {
        mSBMLReader = pSBMLReader;
    }

    private SBMLReader getSBMLReader()
    {
        return(mSBMLReader);
    }

    /*========================================*
     * initialization methods
     *========================================*/

    /*========================================*
     * constructors
     *========================================*/
    public MarkupLanguageImporter()
    {
        setSBMLReader(new SBMLReader());
    }

    /*========================================*
     * private methods
     *========================================*/

    /*========================================*
     * protected methods
     *========================================*/
    
    /*========================================*
     * public methods
     *========================================*/    

    public void readModelDescription(String pModelDescription) throws InvalidInputException
    {
        try
        {
            getSBMLReader().loadSBML(pModelDescription);
        }
        catch(SBWException e)
        {
            throw new InvalidInputException(e.getDetailedMessage(), e);
        }
    }

    public String getModelName()
    {
        return(getSBMLReader().getModelName());
    }

    public int getNumCompartments()
    {
        return(getSBMLReader().getNumCompartments());
    }

    public int getNumReactions() 
    {
        return(getSBMLReader().getNumReactions());
    }

    public int getNumFloatingSpecies()
    {
        return(getSBMLReader().getNumFloatingSpecies());
    }

    public int getNumBoundarySpecies()
    {
        return(getSBMLReader().getNumBoundarySpecies());
    }

    public int getNumGlobalParameters()
    {
        return(getSBMLReader().getNumGlobalParameters());
    }

    public String getNthCompartmentName(int compartment)
    {
        return(getSBMLReader().getNthCompartmentName(compartment));
    }

    public String getNthFloatingSpeciesName(int floatingSpecies)
    {
        return(getSBMLReader().getNthFloatingSpeciesName(floatingSpecies));
    }

    public String getNthBoundarySpeciesName(int boundarySpecies)
    {
        return(getSBMLReader().getNthBoundarySpeciesName(boundarySpecies));
    }

    public String getNthFloatingSpeciesCompartmentName(int floatingSpecies)
    {
        return(getSBMLReader().getNthFloatingSpeciesCompartmentName(floatingSpecies));
    }

    public String getNthBoundarySpeciesCompartmentName(int boundarySpecies)
    {
        return(getSBMLReader().getNthBoundarySpeciesCompartmentName(boundarySpecies));
    }

    public String getNthReactionName(int reaction)
    {
        return(getSBMLReader().getNthReactionName(reaction));
    }

    public int getNumReactants(int reaction) 
    {
        return(getSBMLReader().getNumReactants(reaction));
    }

    public int getNumProducts(int reaction)
    {
        return(getSBMLReader().getNumProducts(reaction));
    }

    public String getNthReactantName(int reaction, int reactant)
    {
        return(getSBMLReader().getNthReactantName(reaction, reactant));
    }

    public String getNthProductName(int reaction, int product)
    {
        return(getSBMLReader().getNthProductName(reaction, product));
    }

    public String getKineticLaw(int reaction)
    {
        return(getSBMLReader().getKineticLaw(reaction));
    }

    public int getNthReactantStoichiometry(int reaction, int reactant)
    {
        return(getSBMLReader().getNthReactantStoichiometry(reaction, reactant));
    }

    public int getNthProductStoichiometry(int reaction, int product)
    {
        return(getSBMLReader().getNthProductStoichiometry(reaction, product));
    }

    public int getNumParameters(int reaction)
    {
        return(getSBMLReader().getNumParameters(reaction));
    }

    public String getNthParameterName(int reaction, int parameter)
    {
        return(getSBMLReader().getNthParameterName(reaction, parameter));
    }

    public double getNthParameterValue(int reaction, int parameter)
    {
        return(getSBMLReader().getNthParameterValue(reaction, parameter));
    }

    public boolean getNthParameterHasValue(int reaction, int parameter)
    {
        return(getSBMLReader().getNthParameterHasValue(reaction, parameter));
    }

    public String getNthGlobalParameterName(int globalParameter)
    {
        return(getSBMLReader().getNthGlobalParameterName(globalParameter));
    }

    public boolean hasValue(String name) throws InvalidInputException
    {
        try
        {
            return(getSBMLReader().hasValue(name));
        }
        catch(SBWException e)
        {
            throw new InvalidInputException(e.toString());
        }
    }

    public double getValue(String name) throws InvalidInputException
    {
        try
        {
            return(getSBMLReader().getValue(name));
        }
        catch(SBWException e)
        {
            throw new InvalidInputException(e.toString());
        }
    }

    public String[] getBuiltinFunctionInfo(String name)
    {
        return(getSBMLReader().getBuiltinFunctionInfo(name));
    }

    public String[] getBuiltinFunctions()
    {
        return(getSBMLReader().getBuiltinFunctions());
    }

    public int getNumRules()
    {
        return(getSBMLReader().getNumRules());
    }

    public String getNthRuleName(int pIndex)
    {
        return(getSBMLReader().getNthRuleName(pIndex));
    }

    public String getNthRuleFormula(int pIndex)
    {
        return(getSBMLReader().getNthRuleFormula(pIndex));
    }

    public String getNthRuleType(int pIndex)
    {
        return(getSBMLReader().getNthRuleType(pIndex));
    }

    public String getSubstanceUnitsString()
    {
        return(getSBMLReader().getSubstanceUnitsString());
    }
}
