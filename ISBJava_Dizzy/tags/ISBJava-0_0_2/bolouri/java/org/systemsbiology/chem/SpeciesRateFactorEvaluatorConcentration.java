package org.systemsbiology.chem;

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
        if(pStoichiometry - 2 == 0)
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
