package isb.chem.app;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MainMenu extends JMenuBar
{
    MainApp mApp;
    private JMenuItem mSimulateMenuItem;
    private JMenuItem mExportMenuItem;

    private static final String ACTION_COMMAND_FILE_OPEN = "Open...";
    private static final String ACTION_COMMAND_FILE_QUIT = "Quit";

    private static final String ACTION_COMMAND_HELP_ABOUT = "About...";
    private static final String ACTION_COMMAND_HELP_USER_MANUAL = "User manual...";

    private static final String ACTION_COMMAND_TOOLS_EXPORT = "Export...";
    private static final String ACTION_COMMAND_TOOLS_SIMULATE = "Simulate...";



    class MenuListener implements ActionListener
    {
        public void actionPerformed(ActionEvent pEvent)
        {
            String actionCommand = pEvent.getActionCommand();
            if(actionCommand.equals(ACTION_COMMAND_FILE_OPEN))
            {
                mApp.handleOpen();
            }
            else if(actionCommand.equals(ACTION_COMMAND_FILE_QUIT))
            {
                mApp.handleQuit();
            }
            else if(actionCommand.equals(ACTION_COMMAND_HELP_ABOUT))
            {
                mApp.handleAbout();
            }
            else if(actionCommand.equals(ACTION_COMMAND_HELP_USER_MANUAL))
            {
                mApp.handleHelpUserManual();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_EXPORT))
            {
                mApp.handleExport();
            }
            else if(actionCommand.equals(ACTION_COMMAND_TOOLS_SIMULATE))
            {
                mApp.handleSimulate();
            }
        }
    }

    JMenuItem getSimulateMenuItem()
    {
        return(mSimulateMenuItem);
    }

    JMenuItem getExportMenuItem()
    {
        return(mExportMenuItem);
    }

    private void createSingleMenu(String pMenuName, String []pMenuItems, int []pShortcuts)
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
            if(menuItemName.equals(ACTION_COMMAND_TOOLS_EXPORT))
            {
                mExportMenuItem = item;
            }
            menu.add(item);
        }

        add(menu);
    }


    private void createMenu()
    {
        String []fileItems = new String[] {ACTION_COMMAND_FILE_OPEN,
                                           ACTION_COMMAND_FILE_QUIT};
        int []fileShortcuts = {'O', 'Q'};
        
        createSingleMenu("File", fileItems, fileShortcuts);

        String []toolsItems = new String[] {ACTION_COMMAND_TOOLS_EXPORT,
                                            ACTION_COMMAND_TOOLS_SIMULATE};
        int []toolsShortcuts = {'E', 'S'};

        createSingleMenu("Tools", toolsItems, toolsShortcuts);

        String []helpItems = new String[] {ACTION_COMMAND_HELP_ABOUT,
                                           ACTION_COMMAND_HELP_USER_MANUAL};
        int []helpShortcuts = {'A', 'U'};
        
        mSimulateMenuItem.setEnabled(false);
        mExportMenuItem.setEnabled(false);

        createSingleMenu("Help", helpItems, helpShortcuts);
    }

    public MainMenu(MainApp pApp)
    {
        mApp = pApp;
        createMenu();
    }
}
