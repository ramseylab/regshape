/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.inference;

import org.systemsbiology.math.probability.IContinuousDistribution;

/**
 * Data structure containing the results of a call to the
 * {@link SignificanceCalculator}.  It contains the significance
 * values of the observations that were passed to the significance
 * calculator. 
 * 
 * @author sramsey
 *
 */
public class SignificanceCalculationResults
{
    public SignificanceCalculationResults()
    {
        mSignificances = null;
    }
    
    public SignificanceCalculationResults(int pNumObservations)
    {
        if(pNumObservations < 0)
        {
            throw new IllegalArgumentException("invalid number of observations");
        }
        mSignificances = new double[pNumObservations];
    }
    public double []mSignificances;
    public double mReducedChiSquare;
    public IContinuousDistribution mBestFitDistribution;
}
