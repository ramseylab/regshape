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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Enumerates the possible formulas for computing the significance
 * of an observation, based on the distribution of "negative control"
 * observations.  Passed to the {@link SignificanceCalculator} as a
 * parameter.
 * 
 * @author sramsey
 *
 */
public class SignificanceCalculationFormula implements Comparable
{
    private final String mName;
    private static final HashMap sMap;
    static
    {
        sMap = new HashMap();
    }
    public static final SignificanceCalculationFormula CDF = new SignificanceCalculationFormula("cdf");
    public static final SignificanceCalculationFormula PDF = new SignificanceCalculationFormula("pdf");
       
    private SignificanceCalculationFormula(String pName)
    {
        mName = pName;
        sMap.put(mName, this);
    }
    public String toString()
    {
        return mName;
    }
    public int compareTo(Object pObject)
    {
        return mName.compareTo(((SignificanceCalculationFormula) pObject).mName); 
    }
    public static SignificanceCalculationFormula get(String pName)
    {
        return (SignificanceCalculationFormula) sMap.get(pName);
    }

    public static SignificanceCalculationFormula []getAll()
    {
        LinkedList linkedList = new LinkedList(sMap.values());
        Collections.sort(linkedList);
        return (SignificanceCalculationFormula []) linkedList.toArray(new SignificanceCalculationFormula[0]);
    }
    public String getName()
    {
    	return mName;
    }
}
