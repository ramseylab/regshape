/*
 * Copyright (C) 2004 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.data;

import java.util.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DataFileDelimiter implements Comparable
{
    private final String mName;
    private final String mDelimiter;
    private static final HashMap sMap;
    
    static
    {
        sMap = new HashMap();
    }
    
    public static final DataFileDelimiter TAB = new DataFileDelimiter("tab", "\t");
    public static final DataFileDelimiter COMMA = new DataFileDelimiter("comma", ",");
    public static final DataFileDelimiter SPACE = new DataFileDelimiter("space", " ");
    

    
    public int compareTo(Object pObject)
    {
        return this.mName.compareTo(((DataFileDelimiter) pObject).mName);
    }
    
    public String toString()
    {
        return mName;
    }
    
    public String getDelimiter()
    {
        return mDelimiter;
    }
    
    public String getName()
    {
        return mName;
    }
        
    public static DataFileDelimiter forName(String pName)
    {
        return (DataFileDelimiter) sMap.get(pName);
    }
    
    public static DataFileDelimiter []getAll()
    {
        DataFileDelimiter []retArray = null;
        
        LinkedList delimitersList = new LinkedList(sMap.values());
        Collections.sort(delimitersList);
        retArray = (DataFileDelimiter []) delimitersList.toArray(new DataFileDelimiter [0]);
        return retArray;
    }
    
    private DataFileDelimiter(String pName, String pDelimiter)
    {
        mName = pName;
        mDelimiter = pDelimiter;
        sMap.put(pName, this); 
    }
}
