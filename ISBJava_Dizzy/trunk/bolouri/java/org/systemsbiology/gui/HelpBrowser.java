package org.systemsbiology.gui;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import javax.help.*;

public class HelpBrowser
{
    private Dimension mFrameSize;
    private String mHelpSetName;
    private String mAppName;
    private String mTitle;
    private Point mLocation;
    private Component mMainFrame;

    public HelpBrowser(Component pMainFrame, String pHelpSetName, String pAppName)
    {
        mMainFrame = pMainFrame;
        mHelpSetName = pHelpSetName;
        mAppName = pAppName;
        Dimension screenSize = FramePlacer.getScreenSize();
        setFrameSize(new Dimension((int) (screenSize.width * 0.8),
                                   (int) (screenSize.height * 0.8)));
        setFrameTitle(pAppName + ": help");
        setFrameLocation(null);
    }

    public void setFrameTitle(String pTitle)
    {
        mTitle = pTitle;
    }
    
    public void setFrameSize(Dimension pFrameSize)
    {
        mFrameSize = pFrameSize;
    }
    
    public void setFrameLocation(Point pLocation)
    {
        mLocation = pLocation;
    }
    
    public void displayHelpBrowser(String pMapID, String pView)
    {
        try
        {
            String helpSetName = mHelpSetName;

            if(helpSetName != null && helpSetName.trim().length() > 0)
            {

                URL helpPackageURL = HelpSet.findHelpSet(null, helpSetName);
                if(null != helpPackageURL)
                {

                    HelpSet hs = new HelpSet(null, helpPackageURL);
                    hs.setTitle(mTitle);
                    HelpBroker hb = hs.createHelpBroker();
                    hb.setCurrentID(pMapID);
                    if(null != pView)
                    {
                        hb.setCurrentView(pView);
                    }
                    hb.setSize(mFrameSize);
                    Point location = mLocation;
                    if(null == location)
                    {
                        location = FramePlacer.placeInCenterOfScreen(mFrameSize.width, mFrameSize.height);
                    }
                    hb.setLocation(location);
                    hb.initPresentation();
                    hb.setDisplayed(true);
                }
                else
                {
                    JOptionPane.showMessageDialog(mMainFrame,
                                                  "The help file was not found: " + helpSetName,
                                                  "Help file not found",
                                                  JOptionPane.WARNING_MESSAGE);
                }
            }
            else
            {
                JOptionPane.showMessageDialog(mMainFrame,
                                              "Sorry, no on-line help is available",
                                              "No help is available",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mMainFrame,
                                    "Error displaying online help").show();
            return;
        }        
    }
}
