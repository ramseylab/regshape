package org.systemsbiology.chem;

import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.math.SymbolEvaluator;

public abstract class SpeciesRateFactorEvaluator
{
    public abstract double computeRateFactorForSpecies(SymbolEvaluator pSymbolEvaluator,
                                                       Species pSpecies,
                                                       int pStoichiometry) throws DataNotFoundException;
}
