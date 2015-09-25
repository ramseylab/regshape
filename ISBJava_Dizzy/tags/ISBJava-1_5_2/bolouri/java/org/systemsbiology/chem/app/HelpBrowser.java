package org.systemsbiology.chem.app;
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
import java.awt.event.*;
import java.net.URL;
import java.io.File;
import org.systemsbiology.util.*;
import javax.help.*;

public class HelpBrowser
{
    private static final String TOP_MAP_ID = "top";
    private static final int HELP_WINDOW_WIDTH = 800;
    private static final int HELP_WINDOW_HEIGHT = 600;

    JFrame mMainFrame;

    public HelpBrowser(JFrame pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    public void displayHelpBrowser(String pView)
    {
        try
        {
            MainApp theApp = MainApp.getApp();

            File appDir = theApp.getAppDir();
            String helpSetName = theApp.getAppConfig().getAppHelpSetName();

            if(helpSetName != null && helpSetName.trim().length() > 0)
            {

                URL helpPackageURL = HelpSet.findHelpSet(null, helpSetName);
                if(null != helpPackageURL)
                {

                    HelpSet hs = new HelpSet(null, helpPackageURL);
                    hs.setTitle(theApp.getAppConfig().getAppName() + ": help");
                    HelpBroker hb = hs.createHelpBroker();
                    hb.setCurrentID(TOP_MAP_ID);
                    if(null != pView)
                    {
                        hb.setCurrentView(pView);
                    }
                    Dimension frameSize = new Dimension(HELP_WINDOW_WIDTH, HELP_WINDOW_HEIGHT);
                    hb.setSize(frameSize);
                    Point location = FramePlacer.placeInCenterOfScreen(frameSize.width, frameSize.height);
                    hb.setLocation(location);
                    hb.initPresentation();
                    hb.setDisplayed(true);
                }
                else
                {
                    SimpleDialog notFoundDialog = new SimpleDialog(mMainFrame, "Help file not found", 
                                                                   "The help file was not found: " + helpSetName);
                    notFoundDialog.show();
                }
            }
            else
            {
                    SimpleDialog notFoundDialog = new SimpleDialog(mMainFrame, "No help is available", 
                                                                   "Sorry, no on-line help is available");
                    notFoundDialog.show();
            }
        }
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled exceptionDialog = new ExceptionDialogOperationCancelled(mMainFrame,
                                                                                                      "Error displaying online help",
                                                                                                      e);
            exceptionDialog.show();
            return;
        }        
    }
}