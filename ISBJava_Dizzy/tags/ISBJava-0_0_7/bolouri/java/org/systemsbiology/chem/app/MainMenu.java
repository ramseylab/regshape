package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MainMenu extends JMenuBar
{
    MainApp mApp;
    private JMenuItem mSimulateMenuItem;
    private JMenuItem mSaveMenuItem;
    private JMenuItem mCloseMenuItem;
    private JMenuItem mProcessMenuItem;
    private JMenu mToolsMenu;

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
    private static final String ACTION_COMMAND_TOOLS_CYTOSCAPE = "View in Cytoscape";



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
        }
    }

    JMenuItem getSaveMenuItem()
    {
        return(mSaveMenuItem);
    }

    JMenuItem getCloseMenuItem()
    {
        return(mCloseMenuItem);
    }

    JMenuItem getSimulateMenuItem()
    {
        return(mSimulateMenuItem);
    }

    JMenu getToolsMenu()
    {
        return(mToolsMenu);
    }

    private JMenu createSingleMenu(String pMenuName, String []pMenuItems, int []pShortcuts)
    {
        JMenu menu = new JMenu(pMenuName);
        for(int i = 0; i < pMenuItems.length; ++i)
        {
            String menuItemName = pMenuItems[i];
            JMenuItem item = new JMenuItem(menuItemName, pShortcuts[i]);
            item.addActionListener(new MenuListener());
            if(menuItemName.equals(ACTION_COMMAND_TOOLS_SIMULATE))
            {
                mSimulateMenuItem = item;
            }
            if(menuItemName.equals(ACTION_COMMAND_FILE_SAVE))
            {
                mSaveMenuItem = item;
            }
            if(menuItemName.equals(ACTION_COMMAND_FILE_CLOSE))
            {
                mCloseMenuItem = item;
            }
            menu.add(item);
        }

        add(menu);
        return(menu);
    }


    private void createMenu()
    {
        String []fileItems = new String[] {ACTION_COMMAND_FILE_OPEN,
                                           ACTION_COMMAND_FILE_SAVE,
                                           ACTION_COMMAND_FILE_SAVE_AS,
                                           ACTION_COMMAND_FILE_CLOSE,
                                           ACTION_COMMAND_FILE_QUIT};
        int []fileShortcuts = {'O', 'S', 'A', 'C', 'Q'};
        
        createSingleMenu("File", fileItems, fileShortcuts);

        String []editItems = new String[] {ACTION_COMMAND_EDIT_CUT,
                                           ACTION_COMMAND_EDIT_COPY,
                                           ACTION_COMMAND_EDIT_PASTE};
        int []editShortcuts = {'T', 'C', 'P'};

        createSingleMenu("Edit", editItems, editShortcuts);

        String []toolsItems = new String[] {ACTION_COMMAND_TOOLS_EXPORT,
                                            ACTION_COMMAND_TOOLS_SIMULATE,
                                            ACTION_COMMAND_TOOLS_CYTOSCAPE};
        int []toolsShortcuts = {'E', 'S', 'V'};

        mToolsMenu = createSingleMenu("Tools", toolsItems, toolsShortcuts);

        String []helpItems = new String[] {ACTION_COMMAND_HELP_ABOUT,
                                           ACTION_COMMAND_HELP_BROWSER};
        int []helpShortcuts = {'A', 'B'};
        
        mSimulateMenuItem.setEnabled(false);
        mToolsMenu.setEnabled(false);
        mSaveMenuItem.setEnabled(false);
        mCloseMenuItem.setEnabled(false);

        createSingleMenu("Help", helpItems, helpShortcuts);
    }

    public MainMenu(MainApp pApp)
    {
        mApp = pApp;
        createMenu();
    }
}
