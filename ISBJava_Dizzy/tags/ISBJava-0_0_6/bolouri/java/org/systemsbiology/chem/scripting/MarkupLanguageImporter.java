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
import org.systemsbiology.util.InvalidInputException;
import edu.caltech.sbw.SBWException;
import SBMLValidate.NOMService;

/**
 * Proxy class for interrogating a parsed SBML document.
 * Uses the SBMLValidate.NOMService class to parse and
 * query an SBML document contained in a String.  Used by
 * the {@link ModelBuilderMarkupLanguage} class.
 *
 * @see org.systemsbiology.chem.scripting.ModelBuilderMarkupLanguage
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
    private NOMService mNOMService;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setNOMService(NOMService pNOMService)
    {
        mNOMService = pNOMService;
    }

    private NOMService getNOMService()
    {
        return(mNOMService);
    }

    /*========================================*
     * initialization methods
     *========================================*/

    /*========================================*
     * constructors
     *========================================*/
    public MarkupLanguageImporter()
    {
        setNOMService(new NOMService());
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
            getNOMService().loadSBML(pModelDescription);
        }
        catch(SBWException e)
        {
            throw new InvalidInputException(e.getDetailedMessage(), e);
        }
    }

    public String getModelName()
    {
        return(getNOMService().getModelName());
    }

    public int getNumCompartments()
    {
        return(getNOMService().getNumCompartments());
    }

    public int getNumReactions() 
    {
        return(getNOMService().getNumReactions());
    }

    public int getNumFloatingSpecies()
    {
        return(getNOMService().getNumFloatingSpecies());
    }

    public int getNumBoundarySpecies()
    {
        return(getNOMService().getNumBoundarySpecies());
    }

    public int getNumGlobalParameters()
    {
        return(getNOMService().getNumGlobalParameters());
    }

    public String getNthCompartmentName(int compartment)
    {
        return(getNOMService().getNthCompartmentName(compartment));
    }

    public String getNthFloatingSpeciesName(int floatingSpecies)
    {
        return(getNOMService().getNthFloatingSpeciesName(floatingSpecies));
    }

    public String getNthBoundarySpeciesName(int boundarySpecies)
    {
        return(getNOMService().getNthBoundarySpeciesName(boundarySpecies));
    }

    public String getNthFloatingSpeciesCompartmentName(int floatingSpecies)
    {
        return(getNOMService().getNthFloatingSpeciesCompartmentName(floatingSpecies));
    }

    public String getNthBoundarySpeciesCompartmentName(int boundarySpecies)
    {
        return(getNOMService().getNthBoundarySpeciesCompartmentName(boundarySpecies));
    }

    public String getNthReactionName(int reaction)
    {
        return(getNOMService().getNthReactionName(reaction));
    }

    public int getNumReactants(int reaction) 
    {
        return(getNOMService().getNumReactants(reaction));
    }

    public int getNumProducts(int reaction)
    {
        return(getNOMService().getNumProducts(reaction));
    }

    public String getNthReactantName(int reaction, int reactant)
    {
        return(getNOMService().getNthReactantName(reaction, reactant));
    }

    public String getNthProductName(int reaction, int product)
    {
        return(getNOMService().getNthProductName(reaction, product));
    }

    public String getKineticLaw(int reaction)
    {
        return(getNOMService().getKineticLaw(reaction));
    }

    public int getNthReactantStoichiometry(int reaction, int reactant)
    {
        return(getNOMService().getNthReactantStoichiometry(reaction, reactant));
    }

    public int getNthProductStoichiometry(int reaction, int product)
    {
        return(getNOMService().getNthProductStoichiometry(reaction, product));
    }

    public int getNumParameters(int reaction)
    {
        return(getNOMService().getNumParameters(reaction));
    }

    public String getNthParameterName(int reaction, int parameter)
    {
        return(getNOMService().getNthParameterName(reaction, parameter));
    }

    public double getNthParameterValue(int reaction, int parameter)
    {
        return(getNOMService().getNthParameterValue(reaction, parameter));
    }

    public boolean getNthParameterHasValue(int reaction, int parameter)
    {
        return(getNOMService().getNthParameterHasValue(reaction, parameter));
    }

    public String getNthGlobalParameterName(int globalParameter)
    {
        return(getNOMService().getNthGlobalParameterName(globalParameter));
    }

    public boolean hasValue(String name) throws InvalidInputException
    {
        try
        {
            return(getNOMService().hasValue(name));
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
            return(getNOMService().getValue(name));
        }
        catch(SBWException e)
        {
            throw new InvalidInputException(e.toString());
        }
    }

    public String[] getBuiltinFunctionInfo(String name)
    {
        return(getNOMService().getBuiltinFunctionInfo(name));
    }

    public String[] getBuiltinFunctions()
    {
        return(getNOMService().getBuiltinFunctions());
    }
}