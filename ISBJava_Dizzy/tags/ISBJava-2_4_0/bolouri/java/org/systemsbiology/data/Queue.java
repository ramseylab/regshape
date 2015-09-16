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
 * Queue interface.
 */


public abstract class Queue
{
    public abstract boolean add(Object pElement);
    public abstract Object peekNext();
    public abstract Object getNext();
}
