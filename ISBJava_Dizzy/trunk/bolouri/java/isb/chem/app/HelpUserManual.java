package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.io.File;
import javax.swing.event.*;
import javax.swing.text.html.*;
import isb.util.*;

public class HelpUserManual implements HyperlinkListener
{
    private static final int WIDTH = 630;
    private static final int HEIGHT = 480;

    JFrame mMainFrame;
    private static final String USER_MANUAL_NAME = "UserManual.html";

    public HelpUserManual(JFrame pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane   pane = (JEditorPane)e.getSource();
            if (e instanceof HTMLFrameHyperlinkEvent) 
            {
                HTMLFrameHyperlinkEvent    evt = (HTMLFrameHyperlinkEvent)e;
                URL hyperlinkURL = evt.getURL();
                String protocol = hyperlinkURL.getProtocol();
                if(protocol.equals("file"))
                {
                    HTMLDocument   doc = (HTMLDocument)pane.getDocument();
                    doc.processHTMLFrameHyperlinkEvent(evt);
                }
            } 
            else 
            {
                try 
                {
                    URL hyperlinkURL = e.getURL();
                    String protocol = hyperlinkURL.getProtocol();
                    if(protocol.equals("file"))
                    {
                        pane.setPage(e.getURL());
                    }
                } 
                catch (Throwable t) 
                {
                    t.printStackTrace();
                }
            }
        }
    }

    public void displayUserManual()
    {
        JFrame helpFrame = null;
        try
        {
            MainApp theApp = MainApp.getApp();
            helpFrame = new JFrame(theApp.getAppConfig().getAppName() + ": Help");

            JEditorPane helpPane = new JEditorPane();
            helpPane.setEditable(false);
            File appDir = theApp.getAppDir();
            URL helpFileURL = null;
            if(null != appDir)
            {
                String helpFileName = theApp.getAppDir().getAbsolutePath() + "/docs/" + USER_MANUAL_NAME;
                File helpFile = new File(helpFileName);
                helpFileURL = new URL(URLUtils.createFileURL(helpFile));
            }
            else
            {
                helpFileURL = HelpUserManual.class.getResource(USER_MANUAL_NAME);
            }

            helpPane.setPage(helpFileURL);
            helpPane.addHyperlinkListener(this);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            helpFrame.setLocation((screenSize.width - WIDTH) / 2,
                                  (screenSize.height - HEIGHT) / 2);
            JScrollPane helpScrollPane = new JScrollPane(helpPane);
            helpScrollPane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            helpFrame.setContentPane(helpScrollPane);
            helpFrame.pack();
        }
        catch(Exception e)
        {
            ExceptionDialogOperationCancelled exceptionDialog = new ExceptionDialogOperationCancelled(mMainFrame,
                                                                                                      "Error displaying online help",
                                                                                                      e);
            exceptionDialog.show();
            return;
        }        
        helpFrame.setVisible(true);
    }
}
