/*
 * Copyright (C) 2004 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.pointillist;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.systemsbiology.util.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AboutAction
{
    private App mApp;
    
    public AboutAction(App pApp)
    {
        mApp = pApp;
    }
    
    public void doAction()
    {
        AppConfig appConfig = mApp.getAppConfig();

        String message = new String( appConfig.getAppName() + " version " + 
                                     appConfig.getAppVersion() + "\nReleased " + 
                                     appConfig.getAppDate() + "\nHome page: " +
                                     appConfig.getAppHomePage() );

        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(message);

        String iconRelativeURL = appConfig.getAppIconURL();
        if(iconRelativeURL != null && iconRelativeURL.trim().length() > 0)
        {
            URL iconResource = ClassLoader.getSystemResource(iconRelativeURL);
            if(null != iconResource)
            {
                ImageIcon imageIcon = new ImageIcon(iconResource);
                optionPane.setIcon((Icon) imageIcon);
            }
        }

        optionPane.createDialog(mApp, "About: " + appConfig.getAppName()).show();
    }
}
