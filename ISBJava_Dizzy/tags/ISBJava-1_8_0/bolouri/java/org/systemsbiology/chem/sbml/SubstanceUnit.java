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
import java.util.*;

/**
 * Enumeration of all allowed substance units, for SBML models
 * to be imported into a {@link org.systemsbiology.chem.Model}.
 */
public class SubstanceUnit
{
    private String mName;
    private double mConversionToMolecules;
    private static HashMap sNamesMap;

    static
    {
        sNamesMap = new HashMap();
    }

    private SubstanceUnit(String pName, double pConversionToMolecules)
    {
        mName = pName;
        mConversionToMolecules = pConversionToMolecules;
        sNamesMap.put(pName, this);
    }

    public static SubstanceUnit get(String pName)
    {
        return((SubstanceUnit) sNamesMap.get(pName));
    }

    public static final SubstanceUnit ITEM = new SubstanceUnit("item", 1.0);
    public static final SubstanceUnit MOLE = new SubstanceUnit("mole", Constants.AVOGADRO_CONSTANT);

    public String toString()
    {
        return(mName);
    }

    public double getConversionToMolecules()
    {
        return(mConversionToMolecules);
    }
}
