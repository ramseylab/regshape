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
import java.io.File;
import java.io.IOException;
import java.net.*;

public class AboutDialog extends SimpleDialog
{

    public AboutDialog(Component pMainFrame)
    {
        super(pMainFrame, "About " + MainApp.getApp().getAppConfig().getAppName(), (Object) 
              new String(MainApp.getApp().getAppConfig().getAppName() + " version " + 
                         MainApp.getApp().getAppConfig().getAppVersion() + "\nReleased " + 
                         MainApp.getApp().getAppConfig().getAppDate() + "\nHome page: " +
                         MainApp.getApp().getAppConfig().getAppHomePage() ));

        String iconRelativeURL = MainApp.getApp().getAppConfig().getAppIconURL();
        if(iconRelativeURL != null && iconRelativeURL.trim().length() > 0)
        {
            URL iconResource = ClassLoader.getSystemResource(iconRelativeURL);
            if(null != iconResource)
            {
                ImageIcon imageIcon = new ImageIcon(iconResource);
                setIcon((Icon) imageIcon);
            }
        }
    }
}
