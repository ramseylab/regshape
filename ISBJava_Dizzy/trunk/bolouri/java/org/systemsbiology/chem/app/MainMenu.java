package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import org.systemsbiology.util.*;

public class MainMenu extends JMenuBar
{
    MainApp mApp;
    private HashMap mMenuItemMap;
    private HashMap mMenuMap;

    private static final String ACTION_COMMAND_FILE_OPEN = "Open...";
    private static final String ACTION_COMMAND_FILE_SAVE_AS = "Save As...";
    private static final String ACTION_COMMAND_FILE_SAVE = "Save";
    private static final String ACTION_COMMAND_FILE_CLOSE = "Close";
    private static final String ACTION_COMMAND_FILE_QUIT = "Quit";

    private static final String ACTION_COMMAND_EDIT_CUT = "Cut";
    private static final String ACTION_COMMAND_EDIT_COPY = "Copy";
    private static final String ACTION_COMMAND_EDIT_PASTE = "Paste";

    private static final String ACTION_COMMAND_HELP_ABOUT = "About...";
    private static final String ACTION_COMMAND_HELP_BROWSER = "Browse help...";

    private static final String ACTION_COMMAND_TOOLS_EXPORT = "Export...";
    private static final String ACTION_COMMAND_TOOLS_SIMULATE = "Simulate...";
    private static final String ACTION_COMMAND_TOOLS_RELOAD = "Reload model";
    private static final String ACTION_COMMAND_TOOLS_CYTOSCAPE = "View in Cytoscape";
    private static final String ACTION_COMMAND_TOOLS_HUMAN_READABLE = "View in human-readable format";

    public static class Menu
    {
        private final String mName;
        public String toString()
        {
            return(mName);
        }
        private static final HashMap mInstanceMap;
        static
        {
            mInstanceMap = new HashMap();
        }
        private Menu(String pName)
        {
            mName = pName;
            mInstanceMap.put(pName, this);
        }
        public Menu get(String pName)
        {
            return((Menu) mInstanceMap.get(pName));
        }
        public String getName()
        {
            return(mName);
        }
        public static final Menu FILE = new Menu("File");
        public static final Menu EDIT = new Menu("Edit");
        public static final Menu TOOLS = new Menu("Tools");
        public static final Menu HELP = new Menu("Help");
    }

    public static class MenuItem
    {
        private final String mName;
        public String toString()
        {
            return(mName);
        }
        private static final HashMap mInstanceMap;
        static
        {
            mInstanceMap = new HashMap();
        }
        private MenuItem(String pName)
        {
            mName = pName;
            mInstanceMap.put(pName, this);
        }
        public MenuItem get(String pName)
        {
            return((MenuItem) mInstanceMap.get(pName));
        }
        public String getName()
        {
            return(mName);
        }
        public static final MenuItem FILE_OPEN = new MenuItem(ACTION_COMMAND_FILE_OPEN);
        public static final MenuItem FILE_SAVE_AS = new MenuItem(ACTION_COMMAND_FILE_SAVE_AS);
        public static final MenuItem FILE_SAVE = new MenuItem(ACTION_COMMAND_FILE_SAVE);
        public static final MenuItem FILE_CLOSE = new MenuItem(ACTION_COMMAND_FILE_CLOSE);
        public static final MenuItem FILE_QUIT = new MenuItem(ACTION_COMMAND_FILE_QUIT);
        public static final MenuItem EDIT_CUT = new MenuItem(ACTION_COMMAND_EDIT_CUT);
        public static final MenuItem EDIT_COPY = new MenuItem(ACTION_COMMAND_EDIT_COPY);
        public static final MenuItem EDIT_PASTE = new MenuItem(ACTION_COMMAND_EDIT_PASTE);
        public static final MenuItem TOOLS_EXPORT = new MenuItem(ACTION_COMMAND_TOOLS_EXPORT);
        public static final MenuItem TOOLS_SIMULATE = new MenuItem(ACTION_COMMAND_TOOLS_SIMULATE);
        public static final MenuItem TOOLS_RELOAD = new MenuItem(ACTION_COMMAND_TOOLS_RELOAD);
        public static final MenuItem TOOLS_CYTOSCAPE = new MenuItem(ACTION_COMMAND_TOOLS_CYTOSCAPE);
        public static final MenuItem TOOLS_HUMAN_READABLE = new MenuItem(ACTION_COMMAND_TOOLS_HUMAN_READABLE);
        public static final MenuItem HELP_ABOUT = new MenuItem(ACTION_COMMAND_HELP_ABOUT);
        public static final MenuItem HELP_BROWSER = new MenuItem(ACTION_COMMAND_HELP_BROWSER);
    }


    class MenuListener implements ActionListener
    {
        public void actionPerformed(ActionEvent pEvent)
        {
            String actionCommand = pEvent.getActionCommand();
            if(actionCommand.equals(ACTION_COMMAND_FILE_OPEN))
            {
                mApp.getEditorPane().open();
            }
            else if(actionCommand.equals(ACTION_COMMAND_FILE_SAVE_AS))
            {
                mApp.getEditorPane().saveAs();
            }
            else if(actionCommand.equals(ACTION_COMMAND_FILE_SAVE))
            {
                mApp.getEditorPane().save();
            }
            else if(actionCommand.equals(ACTION_COMMAND_FILE_CLOSE))
            {
                mApp.getEditorPane().close();
            }
            else if(actionCommand.equals(ACTION_COMMAND_FILE_QUIT))
            {
                mApp.handleQuit();
            }
            else if(actionCommand.equals(ACTION_COMMAND_HELP_ABOUT))
            {
                mApp.handleAbout();
            }
            else if(actionCommand.equals(ACTION_COMMAND_HELP_BROWSER))
            {
                mApp.handleHelpBrowser();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_EXPORT))
            {
                mApp.handleExport();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_SIMULATE))
            {
                mApp.handleSimulate();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_RELOAD))
            {
                mApp.handleReload();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_CYTOSCAPE))
            {
                mApp.handleViewInCytoscape();
            }
            else if(actionCommand.equals(ACTION_COMMAND_EDIT_CUT))
            {
                mApp.getEditorPane().handleCut();
            }
            else if(actionCommand.equals(ACTION_COMMAND_EDIT_COPY))
            {
                mApp.getEditorPane().handleCopy();
            }
            else if(actionCommand.equals(ACTION_COMMAND_EDIT_PASTE))
            {
                mApp.getEditorPane().handlePaste();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_HUMAN_READABLE))
            {
                mApp.handleViewModelHumanReadable();
            }
        }
    }

    private void createSingleMenu(Menu pMenu, MenuItem []pMenuItems, int []pShortcuts)
    {
        String menuName = pMenu.getName();
        JMenu menu = new JMenu(menuName);
        mMenuMap.put(menuName, menu);
        for(int i = 0; i < pMenuItems.length; ++i)
        {
            MenuItem menuItem = pMenuItems[i];
            String menuItemName = menuItem.getName();
            JMenuItem item = new JMenuItem(menuItemName, pShortcuts[i]);
            item.addActionListener(new MenuListener());
            mMenuItemMap.put(menuItemName, item);
            menu.add(item);
        }

        add(menu);
    }

    synchronized void setEnabledFlag(MenuItem pMenuItem, boolean pEnabled) throws DataNotFoundException
    {
        String menuItemName = pMenuItem.getName();
        assert (null != menuItemName) : "invalid (null) menu item name";

        JMenuItem menuItem = (JMenuItem) mMenuItemMap.get(menuItemName);
        if(null == menuItem)
        {
            throw new DataNotFoundException("unable to find menu item name: " + menuItemName);
        }
        menuItem.setEnabled(pEnabled);
    }

    synchronized void setEnabledFlag(Menu pMenu, boolean pEnabled) throws DataNotFoundException
    {
        String menuName = pMenu.getName();
        assert (null != menuName) : "invalid (null) menu name";

        JMenu menu = (JMenu) mMenuMap.get(menuName);
        if(null == menu)
        {
            throw new DataNotFoundException("unable to find menu name: " + menuName);
        }
        menu.setEnabled(pEnabled);
    }

    private void createMenu() throws DataNotFoundException
    {
        mMenuMap = new HashMap();
        mMenuItemMap = new HashMap();

        MenuItem []fileItems = new MenuItem[] {MenuItem.FILE_OPEN,
                                               MenuItem.FILE_SAVE,
                                               MenuItem.FILE_SAVE_AS,
                                               MenuItem.FILE_CLOSE,
                                               MenuItem.FILE_QUIT};
        int []fileShortcuts = {'O', 'S', 'A', 'C', 'Q'};
        
        createSingleMenu(Menu.FILE, fileItems, fileShortcuts);

        MenuItem []editItems = new MenuItem[] {MenuItem.EDIT_CUT,
                                               MenuItem.EDIT_COPY,
                                               MenuItem.EDIT_PASTE};
        int []editShortcuts = {'T', 'C', 'P'};

        createSingleMenu(Menu.EDIT, editItems, editShortcuts);

        MenuItem []toolsItems = new MenuItem[] {MenuItem.TOOLS_EXPORT,
                                                MenuItem.TOOLS_SIMULATE,
                                                MenuItem.TOOLS_RELOAD,
                                                MenuItem.TOOLS_CYTOSCAPE,
                                                MenuItem.TOOLS_HUMAN_READABLE};
        int []toolsShortcuts = {'E', 'S', 'R', 'C', 'H'};

        createSingleMenu(Menu.TOOLS, toolsItems, toolsShortcuts);

        MenuItem []helpItems = new MenuItem[] {MenuItem.HELP_ABOUT,
                                               MenuItem.HELP_BROWSER};
        int []helpShortcuts = {'A', 'B'};

        createSingleMenu(Menu.HELP, helpItems, helpShortcuts);
        

    }

    public MainMenu(MainApp pApp) throws DataNotFoundException
    {
        mApp = pApp;
        createMenu();
    }
}
