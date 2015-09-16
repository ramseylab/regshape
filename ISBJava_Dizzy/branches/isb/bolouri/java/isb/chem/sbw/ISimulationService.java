package isb.chem.sbw;

import edu.caltech.sbw.*;

public interface ISimulationService
{
    String []optionsSupported();
    String []loadSBMLModel(String sbml) throws SBWException;
    void doTimeBasedSimulation(Module pSource, double startTime, double endTime, int noOfPoints, String[] filter) throws SBWException;
    void stop() throws SBWException;
    void restart() throws SBWException;
    void doSteadyStateAnalysis(String []filter) throws SBWException;
}
