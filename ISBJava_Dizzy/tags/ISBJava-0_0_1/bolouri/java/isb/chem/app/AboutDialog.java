package isb.chem.app;

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
//             File iconFile = new File(iconFileName);
//             if(iconFile.exists())
//             {
//                 ImageIcon imageIcon = new ImageIcon(iconFileName);
//                 setIcon((Icon) imageIcon);
//             }
        }
    }
}
