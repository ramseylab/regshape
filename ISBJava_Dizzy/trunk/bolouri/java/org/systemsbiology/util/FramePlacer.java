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
import javax.swing.*;

/**
 * Methods for selecting the default location for a GUI frame.
 */
public class FramePlacer
{
    private static final int PLOT_POSITION_STAGGER_AMOUNT_PIXELS = 20;
    private static final int INITIAL_STAGGER = 50;
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

    public void placeInCascadeFormat(JFrame pFrame)
    {
        Dimension frameSize = pFrame.getSize();
        Point location = placeInCascadeFormat(frameSize.width,
                                              frameSize.height);
        pFrame.setLocation(location);
    }

    /**
     * Arranges subsequent frames in a diagonal cascade (down and to the right).
     */
    public Point placeInCascadeFormat(int pFrameWidth, int pFrameHeight)
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        double aspectRatio = screenWidth / screenHeight;
        int numPixelsStagger = INITIAL_STAGGER + (mFrameCtr * PLOT_POSITION_STAGGER_AMOUNT_PIXELS);
        int placeY = numPixelsStagger;
        int placeX = (int) (numPixelsStagger * aspectRatio);
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

    public void placeInCenterOfScreen(JFrame pFrame)
    {
        Dimension frameSize = pFrame.getSize();
        Point location = placeInCenterOfScreen(frameSize.width,
                                               frameSize.height);
        pFrame.setLocation(location);
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
