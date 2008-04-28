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
    private static final String ACTION_COMMAND_TOOLS_VIEW = "View...";
    private static final String ACTION_COMMAND_TOOLS_SIMULATE = "Simulate...";
    private static final String ACTION_COMMAND_TOOLS_RELOAD = "Reload model";
    
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
        private MenuItem []mSubMenu;
        private ActionListener mListener;
        
        public String toString()
        {
            return(mName);
        }
        private static final HashMap sInstanceMap;
        static
        {
            sInstanceMap = new HashMap();
        }
        private MenuItem(String pName)
        {
            this(pName, false);
        }
        private MenuItem(String pName, boolean pHidden)
        {
            mName = pName;
            mSubMenu = null;
            mListener = null;
            if(! pHidden)
            {
                sInstanceMap.put(pName, this);
            }
        }
        public static MenuItem get(String pName)
        {
            return((MenuItem) sInstanceMap.get(pName));
        }
        public String getName()
        {
            return(mName);
        }
        public void setSubMenu(MenuItem []pSubMenu, ActionListener pListener)
        {
            mSubMenu = pSubMenu;
            mListener = pListener;
        }
        public MenuItem []getSubMenu()
        {
            return mSubMenu;
        }
        public ActionListener getListener()
        {
            return mListener;
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
        public static final MenuItem TOOLS_VIEW = new MenuItem(ACTION_COMMAND_TOOLS_VIEW);
        public static final MenuItem HELP_ABOUT = new MenuItem(ACTION_COMMAND_HELP_ABOUT);
        public static final MenuItem HELP_BROWSER = new MenuItem(ACTION_COMMAND_HELP_BROWSER);
    }

    class ExportMenuListener implements ActionListener
    {
        public void actionPerformed(ActionEvent pEvent)
        {
            String actionCommand = pEvent.getActionCommand();
            mApp.handleExport(actionCommand);
        }
    }
    
    class ViewMenuListener implements ActionListener
    {
        public void actionPerformed(ActionEvent pEvent)
        {
            String actionCommand = pEvent.getActionCommand();
            mApp.handleView(actionCommand);
        }        
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
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_SIMULATE))
            {
                mApp.handleSimulate();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_RELOAD))
            {
                mApp.handleReload();
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
            else
            {
                throw new IllegalStateException("Unknown command: " + actionCommand);
            }
        }
    }


    
    private JMenu createSingleMenu(String pMenuName, MenuItem []pMenuItems, int []pShortcuts, ActionListener pListener)
    {
        JMenu menu = new JMenu(pMenuName);
        mMenuMap.put(pMenuName, menu);
        for(int i = 0; i < pMenuItems.length; ++i)
        {
            MenuItem menuItem = pMenuItems[i];
            MenuItem []subMenu = menuItem.getSubMenu();
            String menuItemName = menuItem.getName();
            if(null == subMenu)
            {
                JMenuItem item = null;
                if(null != pShortcuts)
                {
                    item = new JMenuItem(menuItemName, pShortcuts[i]);
                }
                else
                {
                    item = new JMenuItem(menuItemName);
                }
                item.addActionListener(pListener);
                mMenuItemMap.put(menuItemName, item);
                menu.add(item);
            }
            else
            {
                ActionListener listener = menuItem.getListener();
                JMenu subMenuJ = createSingleMenu(menuItemName,
                                                  subMenu,
                                                  null,
                                                  listener);
                mMenuItemMap.put(menuItemName, subMenuJ);
                menu.add(subMenuJ);
            }
        }

        return menu;
    }

    synchronized void setEnabledFlag(MenuItem pMenuItem, boolean pEnabled) throws DataNotFoundException
    {
        String menuItemName = pMenuItem.getName();
        assert (null != menuItemName) : "invalid (null) menu item name";

        JComponent menuItem = (JComponent) mMenuItemMap.get(menuItemName);
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

    private MenuItem []createRegistrySubMenu(String pName, ClassRegistry pRegistry)
    {
        MenuItem []retMenu = null;
        
        Set aliases = pRegistry.getRegistryAliasesCopy();
        int numAliases = aliases.size();
        if(numAliases > 0)
        {
            JMenu exportMenu = new JMenu(pName);
            LinkedList aliasesList = new LinkedList(aliases);
            Collections.sort(aliasesList);
            String []aliasesArray = new String[numAliases];
            retMenu = new MenuItem[numAliases];

            for(int i = 0; i < numAliases; ++i)
            {
                String alias = (String) aliasesList.get(i);
                retMenu[i] = new MenuItem(alias, true);
            }
        }
        return retMenu;
    }
    
    private void createMenu() throws DataNotFoundException
    {
        mMenuMap = new HashMap();
        mMenuItemMap = new HashMap();
        ActionListener listener = new MenuListener();
        
        MenuItem []fileItems = new MenuItem[] {MenuItem.FILE_OPEN,
                                               MenuItem.FILE_SAVE,
                                               MenuItem.FILE_SAVE_AS,
                                               MenuItem.FILE_CLOSE,
                                               MenuItem.FILE_QUIT};
        int []fileShortcuts = {'O', 'S', 'A', 'C', 'Q'};
        
        add(createSingleMenu(Menu.FILE.getName(), fileItems, fileShortcuts, listener));

        MenuItem []editItems = new MenuItem[] {MenuItem.EDIT_CUT,
                                               MenuItem.EDIT_COPY,
                                               MenuItem.EDIT_PASTE};
        int []editShortcuts = {'T', 'C', 'P'};

        add(createSingleMenu(Menu.EDIT.getName(), editItems, editShortcuts, listener));

        MenuItem []exportSubMenu = createRegistrySubMenu("Export", mApp.getModelExporterRegistry());
        ExportMenuListener exportListener = new ExportMenuListener();
        MenuItem exportMenuItem = MenuItem.get(ACTION_COMMAND_TOOLS_EXPORT);
        exportMenuItem.setSubMenu(exportSubMenu, exportListener);
        
        MenuItem []viewSubMenu = createRegistrySubMenu("View", mApp.getModelViewerRegistry());
        ViewMenuListener viewListener = new ViewMenuListener();
        MenuItem viewMenuItem = MenuItem.get(ACTION_COMMAND_TOOLS_VIEW);
        viewMenuItem.setSubMenu(viewSubMenu, viewListener);
        
        MenuItem []toolsItems = new MenuItem[] {exportMenuItem,
                                                MenuItem.TOOLS_SIMULATE,
                                                MenuItem.TOOLS_RELOAD,
                                                viewMenuItem};
        int []toolsShortcuts = {'E', 'S', 'R', 'V'};

        add(createSingleMenu(Menu.TOOLS.getName(), toolsItems, toolsShortcuts, listener));

        MenuItem []helpItems = new MenuItem[] {MenuItem.HELP_ABOUT,
                                               MenuItem.HELP_BROWSER};
        int []helpShortcuts = {'A', 'B'};

        add(createSingleMenu(Menu.HELP.getName(), helpItems, helpShortcuts, listener));
        

    }

    public MainMenu(MainApp pApp) throws DataNotFoundException
    {
        mApp = pApp;
        createMenu();
    }
}
