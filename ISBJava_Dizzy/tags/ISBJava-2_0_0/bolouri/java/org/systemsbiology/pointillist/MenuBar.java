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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.*;


/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MenuBar extends JMenuBar
{
    public static final String ACTION_QUIT = "Quit";
    public static final String ACTION_NORMALIZE = "Normalize...";
    public static final String ACTION_STATISTICAL_TESTS = "Statistical Tests...";
    public static final String ACTION_ABOUT = "About...";
    public static final String ACTION_HELP = "Display Help...";
    public static final String ACTION_MATLAB_CONNECT = "Connect to Matlab";
    public static final String ACTION_MATLAB_DISCONNECT = "Disconnect from Matlab";
    public static final String ACTION_PREFERENCES = "Preferences...";
    public static final String ACTION_OPEN = "Open...";
    public static final String ACTION_INTEGRATE = "Network integration...";
    
    private App mApp;
    protected final HashMap mMenuItems;
    
    private void handleMenuChoice(String pActionCommand)
    {
        if(pActionCommand.equals(ACTION_QUIT))
        {
            QuitAction quitAction = new QuitAction(mApp);
            quitAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_NORMALIZE))
        {
            NormalizeAction normalizeAction = new NormalizeAction(mApp);
            normalizeAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_STATISTICAL_TESTS))
        {
            StatisticalTestsAction statisticalTestsAction = new StatisticalTestsAction(mApp);
            statisticalTestsAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_HELP))
        {
            DisplayHelpAction helpAction = new DisplayHelpAction(mApp);
            helpAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_ABOUT))
        {
            AboutAction aboutAction = new AboutAction(mApp);
            aboutAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_MATLAB_CONNECT))
        {
            MatlabConnectAction matlabAction = new MatlabConnectAction(mApp);
            matlabAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_MATLAB_DISCONNECT))
        {
            MatlabDisconnectAction matlabAction = new MatlabDisconnectAction(mApp);
            matlabAction.doAction();
        }            
        else if(pActionCommand.equals(ACTION_PREFERENCES))
        {
            EditPreferencesAction preferencesAction = new EditPreferencesAction(mApp);
            preferencesAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_OPEN))
        {
            FileOpenAction fileOpenAction = new FileOpenAction(mApp);
            fileOpenAction.doAction();
        }
        else if(pActionCommand.equals(ACTION_INTEGRATE))
        {
            NetworkIntegrationAction networkIntegrationAction = new NetworkIntegrationAction(mApp);
            networkIntegrationAction.doAction();
        }
        else
        {
            throw new IllegalStateException("unknown action selected \"" + pActionCommand + "\"");
        }           
    }
    
    class MenuListener implements ActionListener
    {
        public void actionPerformed(ActionEvent pEvent)
        {
            String actionCommand = pEvent.getActionCommand();
            handleMenuChoice(actionCommand);
        }
    }
    
    public MenuBar(App pApp)
    {
        mApp = pApp;
        mMenuItems = new HashMap();
        initializeMenuBar();
    }
    
    public void setMenuItemEnabled(String pAction,
                                   boolean pEnabled)
    {
        JMenuItem menuItem = (JMenuItem) mMenuItems.get(pAction);
        if(null == menuItem)
        {
            throw new IllegalStateException("could not find menu item for action: " + pAction);
        }
        menuItem.setEnabled(pEnabled);
    }
    
    class Menu extends JMenu
    {
        ActionListener mActionListener;
        public Menu(String pName,
                    ActionListener pActionListener)
        {
            super(pName);
            mActionListener = pActionListener;
        }
        public void createMenuItem(String pAction,
                                   int pShortcut,
                                   boolean pEnabled)
        {
            JMenuItem menuItem = new JMenuItem(pAction, pShortcut);
            if(null != mActionListener)
            {
                menuItem.addActionListener(mActionListener);
            }
            if(null != mMenuItems.get(pAction))
            {
                throw new IllegalStateException("a menu item is already defined for this action: " + pAction);
            }
            mMenuItems.put(pAction, menuItem);
            menuItem.setEnabled(pEnabled);
            this.add(menuItem);
        }
        public void createMenuItem(String pAction,
                                   int pShortcut)
        {
            createMenuItem(pAction, pShortcut, true);
        }        
    }
    
    private void initializeMenuBar()
    {
        ActionListener menuActionListener = new MenuListener();
        Menu fileMenu = new Menu("File", menuActionListener);
        fileMenu.createMenuItem(ACTION_OPEN, 'O');
        fileMenu.createMenuItem(ACTION_QUIT, 'Q');
        add(fileMenu);
        
        Menu editMenu = new Menu("Edit", menuActionListener);
        editMenu.createMenuItem(ACTION_PREFERENCES, 'P');
        add(editMenu);
        
        Menu connectionsMenu = new Menu("Connections", menuActionListener);
        connectionsMenu.createMenuItem(ACTION_MATLAB_CONNECT, 'C');
        connectionsMenu.createMenuItem(ACTION_MATLAB_DISCONNECT, 'D', false);
        add(connectionsMenu);
        
        Menu analysisMenu = new Menu("Analysis", menuActionListener);
        analysisMenu.createMenuItem(ACTION_NORMALIZE, 'N');
        analysisMenu.createMenuItem(ACTION_STATISTICAL_TESTS, 'S');
        analysisMenu.createMenuItem(ACTION_INTEGRATE, 'N');
        add(analysisMenu);
        
        Menu helpMenu = new Menu("Help", menuActionListener);
        helpMenu.createMenuItem(ACTION_ABOUT, 'A');
        helpMenu.createMenuItem(ACTION_HELP, 'D');
        add(helpMenu);
    }
   
}
