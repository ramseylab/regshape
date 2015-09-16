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

import org.systemsbiology.gui.*;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MatlabDisconnectAction
{
    private App mApp;
    
    public MatlabDisconnectAction(App pApp)
    {
        mApp = pApp;
    }
    
    public void doAction()
    {
        MatlabConnectionManager mgr = mApp.getMatlabConnectionManager();
        try
        {
            if(mgr.isConnected())
            {
                mgr.disconnect();
                MenuBar menuBar = mApp.getMenu();
                menuBar.setMenuItemEnabled(MenuBar.ACTION_MATLAB_CONNECT, true);
                menuBar.setMenuItemEnabled(MenuBar.ACTION_MATLAB_DISCONNECT, false);
            }
        }
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
            optionPane.createDialog(mApp, "unable to disconnect from matlab session").show();
        }
    }
}
