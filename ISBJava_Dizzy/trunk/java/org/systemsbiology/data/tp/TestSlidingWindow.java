package org.systemsbiology.data.tp;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.data.SlidingWindowTimeSeriesQueue;

public class TestSlidingWindow
{
    private static final int QUEUE_SIZE = 10;

    public static final void main(String []pArgs)
    {
        SlidingWindowTimeSeriesQueue queue = new SlidingWindowTimeSeriesQueue(QUEUE_SIZE);
        for(int ctr = 0; ctr < 2 * QUEUE_SIZE; ++ctr)
        {
            double time = (double) ctr;
            queue.insertPoint(time, time);
            int endIndex = ctr;
            if(ctr >= QUEUE_SIZE)
            {
                endIndex = QUEUE_SIZE - 1;
            }
            System.out.println("min time: " + queue.getMinTime() + "; last time: " + queue.getLastTimePoint() + "; end time: " + queue.getTimePoint(endIndex));
        }
        
    }
}
