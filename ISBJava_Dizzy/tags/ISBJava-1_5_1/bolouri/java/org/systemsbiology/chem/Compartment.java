package org.systemsbiology.chem;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.SymbolValue;
import org.systemsbiology.math.Expression;
import org.systemsbiology.math.Value;
import org.systemsbiology.math.Symbol;

/**
 * Represents a named, well-mixed reaction compartment,
 * which has a numeric volume (in milliliters).
 *
 * @author Stephen Ramsey
 */
public class Compartment extends SymbolValue
{
    private final String mName;
    public static final double DEFAULT_VOLUME = 1.0;

    public void setVolume(double pVolume)
    {
        setValue(new Value(pVolume));
    }

    public void setVolume(Expression pVolume) 
    {
        setValue(new Value(pVolume));
    }

    /**
     * Creates a compartment.  The compartment name may not contain
     * the NAME_DELIMITER string, which is a single colon character.
     */
    public Compartment(String pName, double pVolume) 
    {
        super(pName);
        mName = pName;
        setVolume(pVolume);
    }

    public Compartment(SymbolValue pSymbolValue)
    {
        super(pSymbolValue);
        mName = pSymbolValue.getSymbol().getName();
    }

    public Compartment(String pName)
    {
        this(pName, DEFAULT_VOLUME);
    }

    public String getName()
    {
        return(mName);
    }

    public boolean equals(Compartment pCompartment)
    {
        return(mName.equals(pCompartment.mName) &&
               super.equals(pCompartment));
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Compartment: ");
        sb.append(getName());
        sb.append(" [Value: ");
        sb.append(getValue().toString());
        sb.append("]");
        return(sb.toString());
    }

    public Object clone()
    {
        Compartment compartment = new Compartment(mName);
        compartment.setValue((Value) getValue().clone());
        return(compartment);
    }
}
