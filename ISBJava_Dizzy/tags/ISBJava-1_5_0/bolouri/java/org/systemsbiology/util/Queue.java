package org.systemsbiology.util;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * Imports a thread-safe FIFO queue.
 */

import java.util.*;

public class Queue
{
    private List mQueue;
    public Queue()
    {
        mQueue = new LinkedList();
    }

    public synchronized void add(Object pElement)
    {
        mQueue.add(pElement);
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
