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

public abstract class SpeciesRateFactorEvaluator
{
    public abstract double computeRateFactorForSpecies(SymbolEvaluator pSymbolEvaluator,
                                                       Species pSpecies,
                                                       int pStoichiometry) throws DataNotFoundException;
}
