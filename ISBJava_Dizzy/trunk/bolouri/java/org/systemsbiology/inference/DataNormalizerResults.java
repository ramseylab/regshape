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

import cern.colt.matrix.ObjectMatrix2D;

/**
 * The results of a call to the {@link QuantileNormalizer}.
 * Contains the normalized observations, which are stored in
 * a matrix <code>cern.colt.matrix.ObjectMatrix2D</code>, where
 * null entries represent missing data (i.e., observations that
 * were missing in the un-normalized raw observations that were
 * originally passed to the quantile normalizer).  The number of
 * iterations needed for the quantile normalizer to converge is
 * returned in the field <code>mNumIterations</code> (if there is
 * no missing data, this field will always be unity).  The final
 * error field <code>mFinalError</code> 
 * is set to the value of the final fractional error in
 * the normalization (coming from estimation of missing data);
 * this field is set to <code>null</code> if there is no missing data.
 * 
 * @author sramsey
 *
 */
public class DataNormalizerResults
{
    public ObjectMatrix2D mNormalizedObservations;
    public int mNumIterations;
    public Double mFinalError;
}
