package org.systemsbiology.data.tp;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.data.AbstractComparator;
import org.systemsbiology.data.IndexedPriorityQueue;

public class TestPriorityQueue
{
    public static final void main(String []pArgs)
    {
        try
        {
            AbstractComparator comp = new AbstractComparator()
            {
                public int compare(Object p1, Object p2)
                {
                    int retVal = ((Integer) p1).intValue() - ((Integer) p2).intValue();
                    return(retVal);
                }
            };
        
            IndexedPriorityQueue pq = new IndexedPriorityQueue(comp);
            
            Integer o1 = new Integer(0);
            Integer o2 = new Integer(1);
            Integer o5 = new Integer(-13);
            Integer o3 = new Integer(3);
            Integer o4 = new Integer(7);
            pq.add(o1);
            pq.add(o2);
            pq.add(o3);
            pq.add(o4);
            pq.add(o5);
            pq.update(4, new Integer(10));

            System.out.println(pq);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
