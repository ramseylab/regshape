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

public class PriorityQueue
{
    protected class Node
    {
        protected int mSubtreePopulation;
        protected Node mFirstChild;
        protected Node mSecondChild;
        protected Node mParent;
        protected Object mPayload;

        public Node(Object pPayload)
        {
            mPayload = pPayload;
            clearTreeLinks();
        }

        public void clearTreeLinks()
        {
            mParent = null;
            mSubtreePopulation = 0;
            mFirstChild = null;
            mSecondChild = null;
        }
    }

    protected AbstractComparator mAbstractComparator;
    protected Node mRoot;

    public PriorityQueue(AbstractComparator pAbstractComparator)
    {
        mAbstractComparator = pAbstractComparator;
        mRoot = null;
    }

    public Object peek()
    {
        Object retObj = null;
        if(null != mRoot)
        {
            retObj = mRoot.mPayload;
        }
        return(retObj);
    }

    public void checkIntegrity(Node pNode)
    {
        if(null != pNode)
        {
            assert (null != pNode.mPayload) : "null payload";

            if(null != pNode.mParent)
            {
                assert (null != pNode.mParent.mFirstChild) : "invalid parent-child link";
                assert (pNode.mParent.mFirstChild.equals(pNode) ||
                        pNode.mParent.mSecondChild.equals(pNode)) : "parent-child link broken";
                assert (mAbstractComparator.compare(pNode.mPayload, pNode.mParent.mPayload) >= 0.0) : "parent has a value greater than child";
            }

            if(null != pNode.mFirstChild)
            {
                checkIntegrity(pNode.mFirstChild);
                if(null != pNode.mSecondChild)
                {
                    checkIntegrity(pNode.mSecondChild);
                }
            }
            else
            {
                assert (null == pNode.mSecondChild) : "second child without first child";
            }
        }
    }

    protected void remove(Node pNode)
    {
        Node firstChild = pNode.mFirstChild;
        Node secondChild = pNode.mSecondChild;
        Node replacement = null;

        Node parent = pNode.mParent;

        if(null != firstChild)
        {
            if(null != secondChild)
            {
                int comp = mAbstractComparator.compare(firstChild.mPayload, secondChild.mPayload);
                if(comp > 0)
                {
                    // second child is smaller than first child
                    insert(secondChild, firstChild);
                    replacement = secondChild;
                }
                else
                {
                    // first child is smaller than second child
                    insert(firstChild, secondChild);
                    replacement = firstChild;
                }
            }
            else
            {
                replacement = firstChild;
            }
            replacement.mParent = parent;
        }
        else
        {
            // do nothing, leave variable "replacement" as null
        }

        if(null != parent)
        {
            assert (null != parent.mFirstChild) : "parent-child relationship broken";
            if(parent.mFirstChild.equals(pNode))
            {
                if(null != replacement)
                {
                    parent.mFirstChild = replacement;
                }
                else
                {
                    parent.mFirstChild = parent.mSecondChild;
                    parent.mSecondChild = null;
                }
            }
            else
            {
                parent.mSecondChild = replacement; 
            }
            parent.mSubtreePopulation--;
            assert(parent.mSubtreePopulation >= 0) : "invalid subtree population";
        }
        else
        {
            assert (pNode.equals(mRoot)) : "non-root node without parent";
            mRoot = replacement;
        }

    }

    public Object poll()
    {
        Object retObj = null;
        if(null != mRoot)
        {
            retObj = mRoot.mPayload;
            remove(mRoot);
        }
        return(retObj);
    }

    protected void insert(Node pTree, Node pNode)
    {
        assert(mAbstractComparator.compare(pNode.mPayload, pTree.mPayload) >= 0) : "invalid node passed to insert()";

        int nodeSubtreePop = pNode.mSubtreePopulation;

        if(null != pTree.mFirstChild)
        {
            if(null != pTree.mSecondChild)
            {
                int firstPop = pTree.mFirstChild.mSubtreePopulation;

                int secondPop = pTree.mSecondChild.mSubtreePopulation;

                if(secondPop > firstPop)
                {
                    int firstComp = mAbstractComparator.compare(pTree.mFirstChild.mPayload, pNode.mPayload);

                    if(firstComp >= 0)
                    {
                        // pNode is smaller than first child of pTree
                        Node subtree = pTree.mFirstChild; 
                        pTree.mFirstChild = pNode;
                        pNode.mParent = pTree;
                        insert(pNode, subtree);
                    }
                    else
                    {
                        // pNode is bigger than first child
                        insert(pTree.mFirstChild, pNode);
                    }
                }
                else
                {
                    int secondComp = mAbstractComparator.compare(pTree.mSecondChild.mPayload, pNode.mPayload);

                    if(secondComp >= 0)
                    {
                        // pNode is smaller than second child
                        Node subtree = pTree.mSecondChild;
                        pTree.mSecondChild = pNode;
                        pNode.mParent = pTree;
                        insert(pNode, subtree);
                    }
                    else
                    {
                        // pNode is bigger than second child
                        insert(pTree.mSecondChild, pNode);
                    }
                }
            }
            else
            {
                pTree.mSecondChild = pNode;
                pNode.mParent = pTree;
            }
        }
        else
        {
            pTree.mFirstChild = pNode;
            pNode.mParent = pTree;
        }

        pTree.mSubtreePopulation += nodeSubtreePop + 1;
    }

    protected void insertRoot(Node pNode)
    {
        if(null != mRoot)
        {
            Object rootObj = mRoot.mPayload;

            if(mAbstractComparator.compare(rootObj, pNode.mPayload) < 0)
            {
                insert(mRoot, pNode);
                // pElement is bigger than mRoot
            }
            else
            {
                pNode.mFirstChild = mRoot;
                pNode.mSubtreePopulation = mRoot.mSubtreePopulation + 1;
                mRoot.mParent = pNode;
                mRoot = pNode;
            }
        }
        else
        {
            mRoot = pNode;
        }
    }

    public boolean offer(Object pElement)
    {
        Node node = new Node(pElement);

        insertRoot(node);

        return(true);
    }

    public int size()
    {
        int retVal = 0;
        if(null != mRoot)
        {
            retVal = mRoot.mSubtreePopulation;
        }
        return(retVal);
    }

    private void printRecursive(Node pNode, StringBuffer pStringBuffer)
    {
        if(null != pNode)
        {
            pStringBuffer.append(pNode.mPayload);
            pStringBuffer.append("(child1=");
            if(null != pNode.mFirstChild)
            {
                printRecursive(pNode.mFirstChild, pStringBuffer);
            }
            else
            {
                pStringBuffer.append("null");
            }
            pStringBuffer.append(",child2=");
            if(null != pNode.mSecondChild)
            {
                printRecursive(pNode.mSecondChild, pStringBuffer);
            }
            else
            {
                pStringBuffer.append("null");
            }
            pStringBuffer.append(")");
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        printRecursive(mRoot, sb);
        return(sb.toString());
    }

    public void clear()
    {
        mRoot = null;
    }
}
