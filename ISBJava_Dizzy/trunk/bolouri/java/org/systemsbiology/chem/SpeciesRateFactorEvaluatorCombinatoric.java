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

public class SpeciesRateFactorEvaluatorCombinatoric extends SpeciesRateFactorEvaluator
{
    private static final long MIN_POPULATION_FOR_COMBINATORIC_EFFECTS = 10000;

    public final double computeRateFactorForSpecies(SymbolEvaluator pSymbolEvaluator,
                                                    Species pSpecies,
                                                    int pStoichiometry) throws DataNotFoundException
    {
        double speciesValue = pSymbolEvaluator.getValue(pSpecies.getSymbol());
        double numReactantCombinations = 1.0;

        if(pStoichiometry > 1)
        {
            if(speciesValue < MIN_POPULATION_FOR_COMBINATORIC_EFFECTS &&
               speciesValue - Math.floor(speciesValue) == 0.0)
            {
                long longSpeciesValue = (long) speciesValue;
                if(longSpeciesValue >= pStoichiometry)
                {
                    numReactantCombinations *= MathFunctions.chooseFunction((long) speciesValue, pStoichiometry);
                }
                else
                {
                    numReactantCombinations = 0.0;
                }
            }
            else
            {
                numReactantCombinations *= Math.pow(speciesValue, pStoichiometry);
            }
        }
        else
        {
            numReactantCombinations *= speciesValue;
        }

        return(numReactantCombinations);
    }
}
