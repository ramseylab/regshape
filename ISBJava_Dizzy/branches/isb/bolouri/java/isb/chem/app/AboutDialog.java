package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AboutDialog extends SimpleDialog
{

    public AboutDialog(Component pMainFrame)
    {
        super(pMainFrame, "About " + MainApp.getApp().getAppConfig().getAppName(), (Object) 
              new String(MainApp.getApp().getAppConfig().getAppName() + " version " + 
                         MainApp.getApp().getAppConfig().getAppVersion() + "\nReleased " + 
                         MainApp.getApp().getAppConfig().getAppDate() + "\nHome page: " +
                         MainApp.getApp().getAppConfig().getAppHomePage() ));
    }
}
