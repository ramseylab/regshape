package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.awt.*;

/**
 * Methods for selecting the default location for a GUI frame.
 */
public class FramePlacer
{
    private static final int PLOT_POSITION_STAGGER_AMOUNT_PIXELS = 20;
    private static Dimension sScreenSize;
    private int mFrameCtr;
    
    static
    {
        sScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
    }


    public FramePlacer()
    {
        mFrameCtr = 0;
    }

    /**
     * Arranges subsequent frames in a diagonal cascade (down and to the right).
     */
    public Point placeInCascadeFormat(int pFrameWidth, int pFrameHeight)
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int numPixelsStagger = mFrameCtr * PLOT_POSITION_STAGGER_AMOUNT_PIXELS;
        int placeX = (sScreenSize.width - pFrameWidth)/4 + numPixelsStagger;
        int placeY = (sScreenSize.height - pFrameHeight)/4 + numPixelsStagger;
        if(numPixelsStagger < (sScreenSize.height - pFrameHeight)/4)
        {
            ++mFrameCtr;
        }
        else
        {
            mFrameCtr = 0;
        }        
        Point point = new Point(placeX, placeY);
        return(point);
    }

    /**
     * Places a frame in the center of the screen
     */
    public static Point placeInCenterOfScreen(int pFrameWidth, int pFrameHeight)
    {
        int screenWidth = sScreenSize.width;
        int screenHeight = sScreenSize.height;
        int placeX = (screenWidth - pFrameWidth)/2;
        if(placeX < 0)
        {
            placeX = 0;
        }
        int placeY = (screenHeight - pFrameHeight)/2;
        if(placeY < 0)
        {
            placeY = 0;
        }
        Point point = new Point(placeX, placeY);
        return(point);
    }
}
