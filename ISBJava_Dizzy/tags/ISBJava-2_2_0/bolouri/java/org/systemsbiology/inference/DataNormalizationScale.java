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

import java.util.HashMap;

/**
 * Specifies the scale to be used for the {@link DataNormalizer}.
 * This is essentially an enumeration class.  An object of this class
 * is a field in the {@link DataNormalizerParams} class, which is
 * passed to the quantile normalizer.  Note that if you choose
 * the <code>DataNormalizationScale.LOGARITHM</code> object, you
 * should set the <code>mFixNonpositiveValues</code> field in the
 * {@link DataNormalizerParams} object, to ensure that any observations
 * less than or equal to zero are fixed (by a global additive shift of the
 * raw observation values) prior to the logarithmic rescaling.
 * 
 * @author sramsey
 *
 */
public class DataNormalizationScale
{
    private final String mName;
    private static final HashMap sMap;
    private boolean mAllowsNonpositiveArgument;
    static
    {
        sMap = new HashMap();
    }
    private DataNormalizationScale(String pName, boolean pAllowsNonpositiveArgument)
    {
        mName = pName;
        mAllowsNonpositiveArgument = pAllowsNonpositiveArgument;
        sMap.put(pName, this);
    }
    public static DataNormalizationScale get(String pName)
    {
        return (DataNormalizationScale) sMap.get(pName);
    }
    public String getName()
    {
        return mName;
    }
    public static DataNormalizationScale []getAll()
    {
        return (DataNormalizationScale []) sMap.values().toArray(new DataNormalizationScale[0]);
    }
    public static final DataNormalizationScale LOGARITHM = new DataNormalizationScale("logarithm", false);
    public static final DataNormalizationScale NORM_ONLY = new DataNormalizationScale("norm_only", true);
    public boolean allowsNonpositiveArgument()
    {
        return mAllowsNonpositiveArgument;
    }
}
