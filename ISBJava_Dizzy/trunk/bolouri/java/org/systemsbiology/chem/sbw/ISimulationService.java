package org.systemsbiology.chem.sbw;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import edu.caltech.sbw.*;

public interface ISimulationService
{
    String []optionsSupported();
    String []loadSBMLModel(String sbml) throws SBWException;
    String []loadCMDLModel(String cmdl) throws SBWException;
    void doTimeBasedSimulation(Module pSource, double startTime, double endTime, int noOfPoints, String[] filter) throws SBWException;
    void stop() throws SBWException;
    void restart() throws SBWException;
    void doSteadyStateAnalysis(String []filter) throws SBWException;
}
