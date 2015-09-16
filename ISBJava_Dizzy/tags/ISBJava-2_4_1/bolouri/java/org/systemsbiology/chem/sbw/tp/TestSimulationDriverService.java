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

import edu.caltech.sbw.Module;
import edu.caltech.sbw.SBW;
import edu.caltech.sbw.Service;

import java.io.*;

/**
 * @author sramsey
 *
 */
public class TestSimulationDriverService
{
    public static final void main(String []pArgs)
    {
        try
        {
            File sbmlFile = new File(pArgs[0]);
            FileReader fileReader = new FileReader(sbmlFile);
            BufferedReader bufRdr = new BufferedReader(fileReader);
            StringBuffer sb = new StringBuffer();
            String line = null;
            while(null != (line = bufRdr.readLine()))
            {
                sb.append(line + "\n");
            }
            SBW.connect();
            Module module = SBW.getModuleInstance("org.systemsbiology.chem.sbw.gui");
            Service service = module.findServiceByName("asim");
            ISimulationDriverService serviceObject = (ISimulationDriverService) service.getServiceObject(ISimulationDriverService.class);
            serviceObject.doAnalysis(sb.toString());
            System.out.println("test successful");            
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
