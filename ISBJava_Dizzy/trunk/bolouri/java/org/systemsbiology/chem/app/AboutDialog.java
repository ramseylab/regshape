package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.*;

public class AboutDialog
{
    private Component mMainFrame;
    private AppConfig mAppConfig;
    private JOptionPane mOptionPane;

    public AboutDialog(Component pMainFrame)
    {
        mAppConfig = MainApp.getApp().getAppConfig();

        String message = new String( mAppConfig.getAppName() + " version " + 
                                     mAppConfig.getAppVersion() + "\nReleased " + 
                                     mAppConfig.getAppDate() + "\nHome page: " +
                                     mAppConfig.getAppHomePage() );

        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(message);

        String iconRelativeURL = MainApp.getApp().getAppConfig().getAppIconURL();
        if(iconRelativeURL != null && iconRelativeURL.trim().length() > 0)
        {
            URL iconResource = ClassLoader.getSystemResource(iconRelativeURL);
            if(null != iconResource)
            {
                ImageIcon imageIcon = new ImageIcon(iconResource);
                optionPane.setIcon((Icon) imageIcon);
            }
        }

        mOptionPane = optionPane;
    }

    public void show()
    {
        mOptionPane.createDialog(mMainFrame,
                                 "About " + mAppConfig.getAppName()).show();
    }
}
