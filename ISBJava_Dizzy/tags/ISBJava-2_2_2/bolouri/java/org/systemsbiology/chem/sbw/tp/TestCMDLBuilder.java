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
package org.systemsbiology.chem.sbw.tp;
import org.systemsbiology.chem.sbw.*;
import edu.caltech.sbw.*;
import java.io.*;

/**
 * Loads a CMDL model description file, and passes the file's contents
 * to the GibsonBruck simulation service via the SBW.
 * 
 * @author sramsey
 *
 */
public class TestCMDLBuilder
{
    public static final void main(String []pArgs)
    {
        try
        {
            if(pArgs.length < 1)
            {
                throw new IllegalArgumentException("please pass the model file as the command-line argument");
            }
            
            String modelFileName = pArgs[0];
            File modelFile = new File(modelFileName);
            FileReader modelFileReader = new FileReader(modelFile);
            StringBuffer sb = new StringBuffer();
            BufferedReader bufReader = new BufferedReader(modelFileReader);
            String line = null;
            while(null != (line = bufReader.readLine()))
            {
                sb.append(line);
            }
            String modelText = sb.toString();
            
            SBW.connect();
            Module module = SBW.getModuleInstance("org.systemsbiology.chem.Simulator");
            Service service = module.findServiceByName("GibsonBruck");
            ISimulationService serviceObject = (ISimulationService) service.getServiceObject(ISimulationService.class);
            String []dynamicSpecies = serviceObject.loadCMDLModel(modelText);
            int numSpecies = dynamicSpecies.length;
            for(int i = 0; i < numSpecies; ++i)
            {
                System.out.println("species " + i + " = \"" + dynamicSpecies[i] + "\"");
            }
            module.shutdown();
            SBW.disconnect();
            System.out.println("test successful");
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
