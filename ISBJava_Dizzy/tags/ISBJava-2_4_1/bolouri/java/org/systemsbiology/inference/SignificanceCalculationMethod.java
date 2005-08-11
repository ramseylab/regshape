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
public class SignificanceCalculationMethod implements Comparable
{
    private final String mName;
    private static final HashMap sMap;
    private int mCode;
    static
    {
        sMap = new HashMap();
    }
    public static final int CODE_CDF_NONPARAMETRIC = 0;
    public static final int CODE_PDF_NONPARAMETRIC = 1;
    public static final int CODE_CDF_PARAMETRIC = 2;
    public static final int CODE_PDF_PARAMETRIC = 3;
    public static final SignificanceCalculationMethod CDF_NONPARAMETRIC = new SignificanceCalculationMethod("cdf_nonparametric", CODE_CDF_NONPARAMETRIC);
    public static final SignificanceCalculationMethod PDF_NONPARAMETRIC = new SignificanceCalculationMethod("pdf_nonparametric", CODE_PDF_NONPARAMETRIC);
    public static final SignificanceCalculationMethod PDF_PARAMETRIC = new SignificanceCalculationMethod("pdf_parametric", CODE_PDF_PARAMETRIC);
    public static final SignificanceCalculationMethod CDF_PARAMETRIC = new SignificanceCalculationMethod("cdf_parametric", CODE_CDF_PARAMETRIC);

    public int getCode()
    {
        return mCode;
    }
    
    private SignificanceCalculationMethod(String pName, int pCode)
    {
        mName = pName;
        mCode = pCode;
        sMap.put(mName, this);
    }
    public String toString()
    {
        return mName;
    }
    public int compareTo(Object pObject)
    {
        return mName.compareTo(((SignificanceCalculationMethod) pObject).mName); 
    }
    public static SignificanceCalculationMethod get(String pName)
    {
        return (SignificanceCalculationMethod) sMap.get(pName);
    }

    public static SignificanceCalculationMethod []getAll()
    {
        LinkedList linkedList = new LinkedList(sMap.values());
        Collections.sort(linkedList);
        return (SignificanceCalculationMethod []) linkedList.toArray(new SignificanceCalculationMethod[0]);
    }
    public String getName()
    {
    	return mName;
    }
}
