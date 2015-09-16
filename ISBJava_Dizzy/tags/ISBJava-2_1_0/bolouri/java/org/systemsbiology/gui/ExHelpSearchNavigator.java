package org.systemsbiology.gui;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import javax.help.*;


import java.util.*;
import java.awt.*;

public class ExHelpSearchNavigator extends SearchView
{
    public ExHelpSearchNavigator(HelpSet hs,
                                 String name,
                                 String label,
                                 Hashtable params)
    {
        super(hs, name, label, params);
    }

    public ExHelpSearchNavigator(HelpSet hs,
                                 String name,
                                 String label,
                                 Locale locale,
                                 Hashtable params)
    {
        super(hs, name, label, locale, params);
    }


    public Component createNavigator(HelpModel model) 
    {
        JHelpSearchNavigator nav = new JHelpSearchNavigator(this, model);
        nav.setUI(new ExHelpSearchNavigatorUI(nav));
        return(nav);
    }
}
