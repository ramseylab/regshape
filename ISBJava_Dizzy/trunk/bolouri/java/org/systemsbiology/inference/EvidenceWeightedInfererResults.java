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

/**
 * A data structure that holds the results of a call to
 * the {@link EvidenceWeightedInferer}.  A global set of
 * element names was passed to the inferer.  The boolean
 * values indicating whether each element is a member of the
 * putative set of affected elements, is included in this
 * data structure of results from the EvidenceWeightedInferer.
 * The combined effective significances are included as well.
 * 
 * @author sramsey
 *
 */
public class EvidenceWeightedInfererResults
{
    public double mSignificanceDistributionSeparation;
    public int mNumIterations;
    public double mAlphaParameter;
    public double []mCombinedEffectiveSignificances;
    public boolean []mAffectedElements;
    public double []mWeights;
    public int mNumAffected;
}
