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
 * Species how the {@link DataNormalizer} should normalize
 * a matrix of raw observations.
 * 
 * @author sramsey
 *
 */
public class DataNormalizationMethod
{
    private final String mName;
    private static final HashMap sMap;

    static
    {
        sMap = new HashMap();
    }
    private DataNormalizationMethod(String pName)
    {
        mName = pName;
        sMap.put(pName, this);
    }
    public static DataNormalizationMethod get(String pName)
    {
        return (DataNormalizationMethod) sMap.get(pName);
    }
    public String getName()
    {
        return mName;
    }
    public static DataNormalizationMethod []getAll()
    {
        return (DataNormalizationMethod []) sMap.values().toArray(new DataNormalizationMethod[0]);
    }
    public static final DataNormalizationMethod QUANTILE = new DataNormalizationMethod("quantile");

}
