package org.systemsbiology.util.tp;

import org.systemsbiology.util.*;
import java.util.*;

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
            pq.offer(o1);
            pq.offer(o2);
            pq.offer(o3);
            pq.offer(o4);
            pq.offer(o5);
            pq.update(4, new Integer(10));

            System.out.println(pq);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
