package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class AboutDialog extends SimpleDialog
{

    public AboutDialog(Component pMainFrame)
    {
        super(pMainFrame, "About " + MainApp.getApp().getAppConfig().getAppName(), (Object) 
              new String(MainApp.getApp().getAppConfig().getAppName() + " version " + 
                         MainApp.getApp().getAppConfig().getAppVersion() + "\nReleased " + 
                         MainApp.getApp().getAppConfig().getAppDate() + "\nHome page: " +
                         MainApp.getApp().getAppConfig().getAppHomePage() ));
        String iconFileName = MainApp.getApp().getAppConfig().getAppIconFile();
        if(iconFileName != null && iconFileName.trim().length() > 0)
        {
            File iconFile = new File(iconFileName);
            if(iconFile.exists())
            {
                ImageIcon imageIcon = new ImageIcon(iconFileName);
                setIcon((Icon) imageIcon);
            }
        }
    }
}
