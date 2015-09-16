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
 * Describes how the {@link QuantileNormalizer} is to normalize the data.
 * In particular, the {@link QuantileNormalizationScale} is defined, as
 * well as the error tolerance (only applicable if there is missing data),
 * and where non-positive values should be fixed by a uniform additive
 * adjustment to the data prior to the rescaling.
 * 
 * @author sramsey
 *
 */
public class QuantileNormalizerParams
{
    public QuantileNormalizationScale mScale;
    public Double mErrorTolerance;
    public boolean mFixNonpositiveValues;
    public Integer mMaxIterations;
}
