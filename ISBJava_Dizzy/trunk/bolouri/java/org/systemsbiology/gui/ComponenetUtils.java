package org.systemsbiology.gui;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.awt.*;
import javax.swing.*;

public class ComponentUtils
{
    public static void disableDoubleMouseClick(Component c) 
    {
      if (c instanceof JList)
      {             
           java.util.EventListener[] listeners=c.getListeners(java.awt.event.MouseListener.class);
           for(int i=0; i<listeners.length; i++) 
           {
               if (listeners[i].toString().indexOf("SingleClickListener") != -1) 
               {
                   c.removeMouseListener((java.awt.event.MouseListener)listeners[i]);
               }
           }
           return; 
       }
       Component[] children = null;
       if (c instanceof Container) 
       {
           children = ((Container)c).getComponents();
       }
       if (children != null) 
       {
           for(int i = 0; i < children.length; i++) 
           {
               disableDoubleMouseClick(children[i]);
           }
       }
   } 
}
