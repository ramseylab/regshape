package org.systemsbiology.gui;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import javax.help.plaf.basic.*;
import javax.help.*;
import javax.help.search.SearchEvent;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.awt.*;

class ExHelpSearchNavigatorUI extends BasicSearchNavigatorUI
{
    private int mSearchHitIndex;
    private JEditorPane mHelpContentEditorPane;
    private JScrollPane mHelpContentScrollPane;
    private boolean mLookedForHelpContentViewer;
    private JTextArea mDoubleClickReminderArea;

    public ExHelpSearchNavigatorUI(JHelpSearchNavigator b)
    {
        super(b);
        mSearchHitIndex = 0;
        mHelpContentEditorPane = null;
        mLookedForHelpContentViewer = false;
    }

    public void findHelpContentEditorPane()
    {
        Frame []frames = Frame.getFrames();
        int numFrames = frames.length;
        for(int ctr = 0; ctr < numFrames; ++ctr)
        {
            Frame frame = frames[ctr];
            if(! (frame instanceof JFrame))
            {
                continue;
            }
            JFrame jframe = (JFrame) frame;
            Container contentPane = jframe.getContentPane();
            if(contentPane.getComponentCount() == 0)
            {
                continue;
            }
            Component component = contentPane.getComponent(0);
            if(null == component) 
            {
                continue;
            }
            if(! (component instanceof JHelp))
            {
                continue;
            }
            JHelp helpFrame = (JHelp) component;
            JHelpContentViewer contentViewer = helpFrame.getContentViewer();
            Component []components = contentViewer.getComponents();
            int numComponents = components.length;
            for(int i = 0; i < numComponents; ++i)
            {
                component = components[i];
                if(null == component)
                {
                    continue;
                }
                if(component instanceof JScrollPane)
                {
                    mHelpContentScrollPane = (JScrollPane) component;
                    mHelpContentEditorPane = (JEditorPane) (mHelpContentScrollPane.getViewport().getComponent(0));
                }
            }
        }
    }

    protected void setCellRenderer(NavigatorView view, JTree tree)
    {
        if (view == null) {
            return;
        }
        Map map = view.getHelpSet().getCombinedMap();
        BasicSearchCellRenderer renderer = new BasicSearchCellRenderer(map);
        renderer.setToolTipText("double-click to jump to next search hit");
        tree.setCellRenderer(renderer);
    }

    private void handleDoubleClick()
    {
        if(! mLookedForHelpContentViewer)
        {
            findHelpContentEditorPane();
            mLookedForHelpContentViewer = true;
        }

        // user double-clicked inside the JTree control
        TreePath path = tree.getSelectionPath();
        if(path != null)
        {
            HelpModel helpModel = searchnav.getModel();
            if(helpModel instanceof TextHelpModel)
            {
                TextHelpModel textHelpModel = (TextHelpModel) helpModel;
                int hitIndex = mSearchHitIndex;
                
                // user double-clicked on an actual node within the JTree
            
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                SearchTOCItem searchTOCItem = (SearchTOCItem) node.getUserObject();
                Enumeration searchHits = searchTOCItem.getSearchHits();
                int hitCtr = 0;
                int beginPos = 0;
                boolean foundHit = false;
                while(searchHits.hasMoreElements())
                {
                    SearchHit searchHit = (SearchHit) searchHits.nextElement();
                    if(hitCtr == hitIndex)
                    {
                        foundHit = true;
                        beginPos = searchHit.getBegin();
                    }
                    hitCtr++;
                }
            
                if(foundHit)
                {
                    if(null != mHelpContentEditorPane)
                    {
                        mHelpContentEditorPane.setCaretPosition(beginPos);
                        try
                        {
                            JViewport viewport = mHelpContentScrollPane.getViewport();
                            Point viewPosition = viewport.getViewPosition();
                            int curY = viewPosition.y;
                            int curX = viewPosition.x;
                            Rectangle viewLocation = mHelpContentEditorPane.modelToView(beginPos);
                            int Y = viewLocation.y;
                            viewport.setViewPosition(new Point(curX, Y));
                            mSearchHitIndex++;
                            if(mSearchHitIndex >= searchTOCItem.hitCount())
                            {
                                mSearchHitIndex = 0;
                            }
                        }

                        catch(Exception e)
                        {
                            e.printStackTrace(System.err);
                        }
                    }
                }
            }
            
        }
    }

    public synchronized void searchStarted(SearchEvent e)
    {
        mSearchHitIndex = 0;
        super.searchStarted(e);
        mDoubleClickReminderArea.replaceRange(null, 0, mDoubleClickReminderArea.getText().length());
        mDoubleClickReminderArea.append("double-click on a search hit above, to advance to the next instance of your search string \"" + searchparams.getText() + "\" in the file");
        mDoubleClickReminderArea.setVisible(true);
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        JHelpSearchNavigator searchNav = (JHelpSearchNavigator) c;
        JTextArea doubleClickReminderArea = new JTextArea(4, 20);
        doubleClickReminderArea.setEditable(false);
        doubleClickReminderArea.setWrapStyleWord(true);
        doubleClickReminderArea.setLineWrap(true);
        mDoubleClickReminderArea = doubleClickReminderArea;
        doubleClickReminderArea.setVisible(false);
        searchNav.add("South", doubleClickReminderArea);
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.setEnabled(true);
        toolTipManager.registerComponent(tree);        
        tree.addMouseListener(
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    if(2 == e.getClickCount())
                    {
                        handleDoubleClick();
                    }
                }
            });

    }


}




