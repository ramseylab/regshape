package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */


import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

import java.io.*;
import java.util.*;

/**
 * Writes the model output in human-readable format
 *
 * @author Stephen Ramsey
 */
public class ModelExporterHumanReadable implements IModelExporter, IAliasableClass
{
    public static final String CLASS_ALIAS = "human-readable";



   /**
    * Given a {@link org.systemsbiology.chem.Model} object
    * defining a system of chemical reactions and the initial species populations,
    * writes out the model in the column format specified by David Orrell
    */
    public void export(Model pModel, PrintWriter pOutputWriter) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException
    {
        pOutputWriter.println(pModel.toString());
        pOutputWriter.flush();
    }

    public String getFileRegex()
    {
        return(".*");
    }
}
