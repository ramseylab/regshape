package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.math.SymbolEvaluator;
import org.systemsbiology.math.MathFunctions;

public class SpeciesRateFactorEvaluatorConcentration extends SpeciesRateFactorEvaluator
{
    private static final long MIN_POPULATION_FOR_COMBINATORIC_EFFECTS = 10000;

    public final double computeRateFactorForSpecies(SymbolEvaluator pSymbolEvaluator,
                                                    Species pSpecies,
                                                    int pStoichiometry) throws DataNotFoundException
    {
        assert (pStoichiometry > 0) : "invalid stoichiometry";
        double speciesValue = pSymbolEvaluator.getValue(pSpecies.getSymbol());
        double compartmentVolume = pSymbolEvaluator.getValue(pSpecies.getCompartment().getSymbol());
        assert (compartmentVolume > 0.0) : "invalid compartment volume";
        double concentration = speciesValue / compartmentVolume;

        double rateFactor = concentration;
        if(pStoichiometry == 1)
        {
            // do nothing; rateFactor has already been set
        }
        else if(pStoichiometry == 2)
        {
            rateFactor *= concentration;
        }
        else 
        {
            rateFactor = Math.pow(concentration, (double) pStoichiometry);
        }

        return(rateFactor);
    }
}
