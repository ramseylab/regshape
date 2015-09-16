package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

public class DebugUtils
{
    public static void printDoubleVector(double []pVec)
    {
        int numElements = pVec.length;
        for(int ctr = 0; ctr < numElements; ++ctr)
        {
            System.out.println("index: " + ctr + "; value: " + pVec[ctr]);
        }
    }

    public static void describeSortedObjectList(StringBuffer pStringBuffer,
                                                HashMap pObjectMap,
                                                Class pClassTypeFilter)
    {
        String separatorString = ", ";
        describeSortedObjectList(pStringBuffer, pObjectMap, pClassTypeFilter, separatorString);
    }

    public static void describeSortedObjectList(StringBuffer pStringBuffer,
                                                HashMap pObjectMap,
                                                String pSeparatorString)
    {
        Class classTypeFilter = null;
        describeSortedObjectList(pStringBuffer, pObjectMap, classTypeFilter, pSeparatorString);
    }

    public static void describeSortedObjectList(StringBuffer pStringBuffer,
                                                HashMap pObjectMap)
    {
        String separatorString = ", ";
        Class classTypeFilter = null;
        describeSortedObjectList(pStringBuffer, pObjectMap, classTypeFilter, separatorString);
    }

    public static void describeSortedObjectList(StringBuffer pStringBuffer,
                                                HashMap pObjectMap, 
                                                Class pClassTypeFilter,
                                                String pSeparatorString)
    {
        List objectList = new LinkedList(pObjectMap.values());
        Collections.sort(objectList);
        Iterator iter = objectList.iterator();
        StringBuffer sb = pStringBuffer;
        sb.append("{");
        while(iter.hasNext())
        {
            Object object = iter.next();
            if(null != pClassTypeFilter)
            {
                if(! (object.getClass().isAssignableFrom(pClassTypeFilter)))
                {
                    continue;
                }
            }
            sb.append(object.toString());
            if(iter.hasNext())
            {
                sb.append(pSeparatorString);
            }
        }
        sb.append("}");
    }
}
