package org.systemsbiology.chem.app.tp;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import org.systemsbiology.chem.*;
import org.systemsbiology.chem.sbml.*;
import org.systemsbiology.chem.app.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TestDizzy
{
    public static final void main(String []pArgs)
    {
        try
        {
            if(pArgs.length < 1)
            {
                throw new InvalidInputException("missing file argument");
            }

            String fileName = pArgs[0];

            File file = new File(fileName);
            if(! file.exists())
            {
                throw new InvalidInputException("file not found: " + fileName);
            }

            IModelBuilder modelBuilder = null;

            if(fileName.endsWith(".dizzy"))
            {
                modelBuilder = new ModelBuilderCommandLanguage();
            }
            else if(fileName.endsWith(".xml"))
            {
                modelBuilder = new ModelBuilderMarkupLanguage();
            }
            else
            {
                throw new InvalidInputException("unrecognized file extension");
            }

            IncludeHandler includeHandler = new IncludeHandler();
            includeHandler.setDirectory(new File(file.getParentFile().getAbsolutePath()));

            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            Model model = modelBuilder.buildModel(bufferedReader, includeHandler);

            SimulationLauncher launcher = new SimulationLauncher("Dizzy", model, true);
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
