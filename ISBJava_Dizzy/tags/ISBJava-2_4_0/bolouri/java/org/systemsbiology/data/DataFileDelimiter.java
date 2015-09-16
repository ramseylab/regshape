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
    private final boolean mSingle;
    private static final HashMap sMap;
    private String mFilterRegex;
    private String mDefaultExtension;
    
    static
    {
        sMap = new HashMap();
    }
    
    public static final DataFileDelimiter TAB = new DataFileDelimiter("tab", "\t", true, ".*\\.tsv$", "tsv");
    public static final DataFileDelimiter COMMA = new DataFileDelimiter("comma", ",", true, ".*\\.csv$", "csv");
    public static final DataFileDelimiter SPACE = new DataFileDelimiter("space", " ", false, ".*\\.txt$", "txt");
    

    /**
     * Returns true if this delimiter type is just a single character per column.
     * Returns false if multiple characters of this delimiter can be used to span
     * between two adjacent columns (as with the "space" delimiter).
     * 
     */
    public boolean getSingle()
    {
        return mSingle;
    }
    
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
        
    public static DataFileDelimiter get(String pName)
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
    
    public String getFilterRegex()
    {
        return mFilterRegex;
    }
    
    public String getDefaultExtension()
    {
        return mDefaultExtension;
    }
    
    private DataFileDelimiter(String pName, String pDelimiter, boolean pSingle, String pFilterRegex, String pDefaultExtension)
    {
        mName = pName;
        mDelimiter = pDelimiter;
        mSingle = pSingle;
        mFilterRegex = pFilterRegex;
        mDefaultExtension = pDefaultExtension;
        sMap.put(pName, this); 
    }
    
    public String scrubIdentifier(String pName)
    {
        String scrubbedIdentifier = pName;
        
        if(-1 != scrubbedIdentifier.indexOf(mDelimiter))
        {
            scrubbedIdentifier = scrubbedIdentifier.replaceAll(mDelimiter, "_");
        }
        
        return scrubbedIdentifier;
    }
}
