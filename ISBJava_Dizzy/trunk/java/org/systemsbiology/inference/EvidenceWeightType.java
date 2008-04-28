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
import java.util.LinkedList;
import java.util.Collections;

/**
 * Enumerates the formulas that may be used for computing
 * the evidence-specific weights.  Passed to the
 * {@link EvidenceWeightedInferer} as a parameter.  
 * 
 * @author sramsey
 *
 */
public class EvidenceWeightType implements Comparable
{
    private final String mName;
    private int mCode;
    private static final HashMap sMap;
    static
    {
        sMap = new HashMap();
    }
    private EvidenceWeightType(String pName, int pCode)
    {
        mName = pName;
        mCode = pCode;
        sMap.put(pName, this);
    }
    public int getCode()
    {
        return mCode;
    }
    public String toString()
    {
        return mName;
    }
    public static EvidenceWeightType get(String pName)
    {
        return (EvidenceWeightType) sMap.get(pName);
    }
    public int compareTo(Object pObject)
    {
        return mName.compareTo(((EvidenceWeightType) pObject).mName); 
    }
    public String getName()
    {
    	return mName;
    }
    public static EvidenceWeightType []getAll()
    {
        LinkedList linkedList = new LinkedList(sMap.values());
        Collections.sort(linkedList);
        return (EvidenceWeightType []) linkedList.toArray(new EvidenceWeightType[0]);
    }
    public static final int CODE_LINEAR = 0;
    public static final int CODE_POWER = 1;
    public static final int CODE_UNIFORM = 2;
    public static final EvidenceWeightType LINEAR = new EvidenceWeightType("linear", CODE_LINEAR);
    public static final EvidenceWeightType POWER = new EvidenceWeightType("power", CODE_POWER); 
    public static final EvidenceWeightType UNIFORM = new EvidenceWeightType("uniform", CODE_UNIFORM);
}
