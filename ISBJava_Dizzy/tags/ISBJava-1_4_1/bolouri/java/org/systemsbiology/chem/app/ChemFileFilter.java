package org.systemsbiology.chem.app;
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

public class ChemFileFilter extends FileFilter
{
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
            if(null != ParserPicker.processFileName(fileName))
            {
                accept = true;
            }
        }
        return(accept);
    }

    public String getDescription()
    {
        return("chemical simulation model definition files");
    }
}
