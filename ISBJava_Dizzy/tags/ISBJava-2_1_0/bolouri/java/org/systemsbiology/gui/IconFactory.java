/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.gui;

import java.net.*;
import javax.swing.*;

/**
 * @author sramsey
 *
 */
public class IconFactory
{
    public static ImageIcon getIconByName(String pIconName)
    {
        ImageIcon retIcon = null;

        String resourceName = "images/" + pIconName;
        URL imageURL = org.systemsbiology.gui.IconFactory.class.getResource(resourceName);
        if(null != imageURL)
        {
            retIcon = new ImageIcon(imageURL, pIconName);
        }
        
        return retIcon;
    }
}
