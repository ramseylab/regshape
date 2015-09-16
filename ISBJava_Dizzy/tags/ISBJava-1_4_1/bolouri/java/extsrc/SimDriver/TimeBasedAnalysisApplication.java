/*
** Filename    : TimeBasedAnalysisApplication
** Description : main entry point to SBW SimDriver module
** Author(s)   : SBW Development Group <sysbio-team@caltech.edu>
** Organization: Caltech ERATO Kitano Systems Biology Project
** Created     : 2001-07-07
** Revision    : $Id$
** $Source$
**
** Copyright 2001 California Institute of Technology and
** Japan Science and Technology Corporation.
**
** This library is free software; you can redistribute it and/or modify it
** under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 2.1 of the License, or
** any later version.
**
** This library is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
** documentation provided hereunder is on an "as is" basis, and the
** California Institute of Technology and Japan Science and Technology
** Corporation have no obligations to provide maintenance, support,
** updates, enhancements or modifications.  In no event shall the
** California Institute of Technology or the Japan Science and Technology
** Corporation be liable to any party for direct, indirect, special,
** incidental or consequential damages, including lost profits, arising
** out of the use of this software and its documentation, even if the
** California Institute of Technology and/or Japan Science and Technology
** Corporation have been advised of the possibility of such damage.  See
** the GNU Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public License
** along with this library; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
**
** The original code contained here was initially developed by:
**
**     Andrew Finney, Herbert Sauro, Michael Hucka, Hamid Bolouri
**     The Systems Biology Workbench Development Group
**     ERATO Kitano Systems Biology Project
**     Control and Dynamical Systems, MC 107-81
**     California Institute of Technology
**     Pasadena, CA, 91125, USA
**
**     http://www.cds.caltech.edu/erato
**     mailto:sysbio-team@caltech.edu
**
** Contributor(s):
**
*/
package edu.caltech.sbw.analyses;

import edu.caltech.sbw.*;
/**
 * Title:        Analyses
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

import java.util.HashMap;
import java.util.Iterator;

public class TimeBasedAnalysisApplication
{
  public static void main(String[] args)
  {
      try
      {
          if (args[0].equals("-sbwregister"))
          {
              SBW.connect();
              ServiceDescriptor[] simulators = SBW.findServices("Simulation", true);

              int i = 0 ;

              HashMap modules = new HashMap();

              while (i != simulators.length)
              {
                  ServiceDescriptor service = simulators[i];
                  ModuleDescriptor module = service.getModuleDescriptor();
                  String moduleName = module.getName();
                  String serviceName = service.getName();

                  String uniqueName = moduleName + "." + serviceName;

                  ModuleImpl moduleImpl = new ModuleImpl(
                          uniqueName + ".gui",
                          module.getDisplayName() + " " + serviceName + " GUI",
                          ModuleImpl.SELF_MANAGED,
                          TimeBasedAnalysisApplication.class,
                          "Module implementing a generic GUI interface for"
                          + " module '" + uniqueName + "'.  "
                          + module.getHelp());

                  moduleImpl.setCommandLine(moduleImpl.getCommandLine()
                                            + " \"" + moduleName + "\" \""
                                            + serviceName + "\"");

                  moduleImpl.addService(
                      serviceName, service.getDisplayName(),
                      "Analysis", TimeBasedAnalysisFrame.class,
                      service.getHelp());

                  moduleImpl.addService(
                      "SimulationCallback", "Time Based Simulation Callback",
                      "", TimeBasedAnalysisFrame.class,
                      "Callback for passing data back to the calling module.");

                  moduleImpl.registerModule();
                  i++ ;
              }
	      SBW.disconnect();
              System.exit(0);
          }
          else
	  {
	      if (args.length < 3)
	      {
		  System.out.println("Usage: SimDriver modulename servicename");
		  System.exit(0);
	      }

	      String moduleName = args[1];
	      String serviceName = args[2];
          String uniqueName = moduleName + "." + serviceName;
	      TimeBasedAnalysisFrame frame = new TimeBasedAnalysisFrame();
	      ModuleImpl moduleImpl =
		  new ModuleImpl(
		      uniqueName + ".gui",
		      "GUI",
		      ModuleImpl.SELF_MANAGED,
		      TimeBasedAnalysisApplication.class);

	      frame.init(moduleName, serviceName);
	      moduleImpl.addService(serviceName, "GUI", "Analysis", frame);
	      moduleImpl.addService("SimulationCallback", "Time Based Simulation Callback", "", frame);
	      moduleImpl.enableModuleServices();
	  }
      }
      catch (Throwable t)
      {
          SBWException.translateException(t).handleWithDialog();
          System.exit(0);
      }
  }
}
