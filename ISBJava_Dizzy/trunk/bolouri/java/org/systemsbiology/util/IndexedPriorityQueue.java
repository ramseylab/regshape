package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;

public class IndexedPriorityQueue extends PriorityQueue
{
    private ArrayList mIndex;
    private static final int DEFAULT_CAPACITY = 11;

    class IndexedNode extends PriorityQueue.Node
    {
        private int mIndex;

        public IndexedNode(Object pPayload)
        {
            super(pPayload);
            mIndex = 0;
        }        
    }

    public IndexedPriorityQueue(AbstractComparator pAbstractComparator)
    {
        this(DEFAULT_CAPACITY, pAbstractComparator);
    }

    public IndexedPriorityQueue(int pInitialCapacity, AbstractComparator pAbstractComparator)
    {
        super(pAbstractComparator);
        mIndex = new ArrayList(pInitialCapacity);
    }

    public void update(int pIndex, Object pValue) throws DataNotFoundException
    {
        IndexedNode node = (IndexedNode) mIndex.get(pIndex);
        if(null == node)
        {
            throw new DataNotFoundException("no queue element exists with this index: " + pIndex);
        }
        
        remove(node, mAbstractComparator);
        
        node.clearTreeLinks();
        node.mPayload = pValue;

        insertRoot(node);
    }
    
    public void clear()
    {
        mIndex.clear();
        super.clear();   
    }

    public Object poll()
    {
        throw new IllegalStateException("poll() operation not supported on an IndexedPriorityQueue");
    }

    public int peekIndex()
    {
        int retIndex = -1;
        if(null != mRoot)
        {
            retIndex = ((IndexedNode) mRoot).mIndex;
        }
        return(retIndex);
    }

    public Object get(int pIndex)
    {
        return(((Node) mIndex.get(pIndex)).mPayload);
    }

    public boolean offer(Object pElement)
    {
        IndexedNode node = new IndexedNode(pElement);
        
        int index = mIndex.size();
        insertRoot(node);
        mIndex.add(node);
        node.mIndex = index;

        return(true);
    }


}
