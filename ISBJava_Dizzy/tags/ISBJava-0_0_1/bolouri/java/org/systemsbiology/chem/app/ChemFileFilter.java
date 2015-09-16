package org.systemsbiology.chem.app;

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
