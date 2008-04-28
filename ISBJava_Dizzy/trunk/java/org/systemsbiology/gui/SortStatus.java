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
package org.systemsbiology.gui;

/**
 * @author sramsey
 *
 */
public class SortStatus
{
    private String mName;
    protected int mCode;
    private SortStatus(String pName, int pCode)
    {
        mName = pName;
        mCode = pCode;
    }
    public int getCode()
    {
        return mCode;
    }
    public String getName()
    {
        return mName;
    }
    public SortStatus getNextSortStatusInCycle()
    {
        SortStatus retStatus = null;
        switch(mCode)
        {
        case CODE_NONE:
            retStatus =  SortStatus.ASCENDING;
            break;
            
        case CODE_ASCENDING:
            retStatus = SortStatus.DESCENDING;
            break;
            
        case CODE_DESCENDING:
            retStatus = SortStatus.NONE;
            break;
        }
        
        return retStatus;
    }
    public static final int CODE_NONE = 0;
    public static final int CODE_ASCENDING = 1;
    public static final int CODE_DESCENDING = -1;
    public static final SortStatus NONE = new SortStatus("not sorted", CODE_NONE);
    public static final SortStatus ASCENDING = new SortStatus("ascending", CODE_ASCENDING);
    public static final SortStatus DESCENDING = new SortStatus("descending", CODE_DESCENDING);
    public static SortStatus []getAll()
    {
        SortStatus []array = new SortStatus[3];
        array[0] = SortStatus.NONE;
        array[1] = SortStatus.ASCENDING;
        array[2] = SortStatus.DESCENDING;
        return array;
    }
}
