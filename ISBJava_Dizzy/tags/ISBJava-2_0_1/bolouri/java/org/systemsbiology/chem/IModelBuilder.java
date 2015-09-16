package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.*;
import org.systemsbiology.util.*;

public interface IModelBuilder
{
    public Model buildModel( InputStream pInputStream, IncludeHandler pIncludeHandler ) throws InvalidInputException, IOException;    
    public String getFileRegex();
    public BufferedReader getBufferedReader( InputStream pInputStream ) throws InvalidInputException;
}
