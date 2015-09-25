package org.systemsbiology.util.tp;

import org.systemsbiology.util.*;

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