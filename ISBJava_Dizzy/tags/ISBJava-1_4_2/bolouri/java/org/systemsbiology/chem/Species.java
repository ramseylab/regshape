package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.HashMap;

import org.systemsbiology.math.Expression;
import org.systemsbiology.math.SymbolValue;
import org.systemsbiology.math.Value;
import org.systemsbiology.math.Symbol;

/**
 * Represents a distinct, named chemical species.  Must
 * reside in a {@link Compartment}.  The species name 
 * does not have to be globally unique; two species can
 * have the same name, if they reside in a different 
 * compartment.  The "symbol name" is constructed using
 * the species name and the compartment name, and it
 * will be different between the two species.  A species
 * object has a value associated with it, by virtue of
 * the superclass {@link org.systemsbiology.math.SymbolValue}.
 * This value represents the amount of the species present,
 * in molecules.
 *
 * @author Stephen Ramsey
 */
public class Species extends SymbolValue
{
    private final String mName;  // species name; does not have to be globally unique
    private final Compartment mCompartment;

    public Species(String pName, Compartment pCompartment) throws IllegalArgumentException
    {
        super(pName);
        mName = pName;
        mCompartment = pCompartment;
    }

    public Species(SymbolValue pSymbolValue, Compartment pCompartment) throws IllegalArgumentException
    {
        super(pSymbolValue);
        mName = pSymbolValue.getSymbol().getName();
        mCompartment = pCompartment;
    }

    public void setSpeciesPopulation(double pSpeciesPopulation)
    {
        setValue(new Value(pSpeciesPopulation));
    }

    public void setSpeciesPopulation(Expression pSpeciesPopulation)
    {
        setValue(new Value(pSpeciesPopulation));
    }

    void addSymbolsToGlobalSymbolMap(HashMap pSymbolMap)
    {
        SymbolValueChemSimulation.addSymbolValueToMap(pSymbolMap, getName(), this);
        Compartment compartment = getCompartment();
        SymbolValueChemSimulation.addSymbolValueToMap(pSymbolMap, compartment.getName(), compartment);
    }

    public String getName()
    {
        return(mName);
    }

    public Compartment getCompartment()
    {
        return(mCompartment);
    }

    public boolean equals(Species pSpecies)
    {
        return(mName.equals(pSpecies.mName) &&
               super.equals(pSpecies) &&
               mCompartment.equals(pSpecies.mCompartment));
    }   

    public Object clone()
    {
        Species species = new Species(mName, mCompartment);
        species.setValue((Value) getValue().clone());
        return(species);
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Species: ");
        sb.append(getName());
        sb.append(" [Value: ");
        sb.append(getValue().toString());
        sb.append(", Compartment: ");
        sb.append(getCompartment().getName());
        sb.append("]");
        return(sb.toString());
    }
}
