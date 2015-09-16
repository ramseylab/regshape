package org.systemsbiology.gui;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import javax.swing.filechooser.*;
import java.io.File;

public class RegexFileFilter extends FileFilter
{
    private String mRegex;
    private String mDescription;

    public RegexFileFilter(String pRegex, String pDescription)
    {
        mRegex = pRegex;
        mDescription = pDescription;
    }

    public boolean accept(File file)
    {
        boolean accept = false;
        String fileName = file.getName();
        if(file.isDirectory())
        {
            accept = true;
        }
        else
        {
            if(fileName.matches(mRegex))
            {
                accept = true;
            }
        }
        return(accept);
    }

    public String getDescription()
    {
        return(mDescription);
    }
}
