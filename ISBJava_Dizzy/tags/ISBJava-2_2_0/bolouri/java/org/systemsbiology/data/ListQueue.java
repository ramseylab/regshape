package org.systemsbiology.data;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * Implementation of a thread-safe FIFO queue using a linked list
 * data structure.
 */

import java.util.*;

public final class ListQueue extends Queue
{
    private List mQueue;
    public ListQueue()
    {
        mQueue = new LinkedList();
    }

    public synchronized boolean add(Object pElement)
    {
        return(mQueue.add(pElement));
    }

    public synchronized Object peekNext()
    {
        Object element = null;
        if(mQueue.size() > 0)
        {
            element = (Object) mQueue.get(0);
        }
        return(element);
    }

    public synchronized Object getNext()
    {
        Object element = null;
        if(mQueue.size() > 0)
        {
            element = (Object) mQueue.remove(0);
        }
        return(element);
    }
    
    
}
