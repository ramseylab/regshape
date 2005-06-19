package org.systemsbiology.chem.sbw;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;
import java.io.*;
import org.systemsbiology.math.*;
import org.systemsbiology.chem.*;
import org.systemsbiology.chem.sbml.*;
import org.systemsbiology.util.*;
import edu.caltech.sbw.*;

public abstract class SimulationService implements ISimulationService
{
    /*========================================*
     * constants
     *========================================*/
    public static final String METHOD_SIGNATURE_LOAD_SBML = "string []loadSBMLModel(string)";
    private static final String SERVICE_NAME_SIMULATION_CALLBACK = "SimulationCallback";

    /*========================================*
     * member data
     *========================================*/
    private SimulationController mSimulationController;
    private ISimulator mSimulator;

    /*========================================*
     * accessor/mutator methods
     *========================================*/

    private ISimulator getSimulator()
    {
        return(mSimulator);
    }

    private void setSimulator(ISimulator pSimulator)
    {
        mSimulator = pSimulator;
    }

    private SimulationController getSimulationController()
    {
        return(mSimulationController);
    }

    private void setSimulationController(SimulationController pSimulationController)
    {
        mSimulationController = pSimulationController;
    }

    /*========================================*
     * initialization methods
     *========================================*/
    private void initialize() throws SBWException
    {
        SimulationController simulationController = new SimulationController();
        setSimulationController(simulationController);

        ISimulator simulator = createSimulator();
        setSimulator(simulator);
    }

    /*========================================*
     * constructors
     *========================================*/
    public SimulationService() throws SBWException
    {
        initialize();
    }

    /*========================================*
     * private methods
     *========================================*/

    private static ISimulationCallback getFrontEnd(Module pSource) throws SBWException
    {
        return( (ISimulationCallback) pSource.findServiceByName(SERVICE_NAME_SIMULATION_CALLBACK).getServiceObject(ISimulationCallback.class));
    }


    /*========================================*
     * protected methods
     *========================================*/
    
    /*========================================*
     * public methods
     *========================================*/
    /**
     * Returns an instance of an {@link org.systemsbiology.chem.ISimulator}
     *
     * @return an instance of an {@link org.systemsbiology.chem.ISimulator}
     */
    abstract ISimulator createSimulator();

    public String []optionsSupported()
    {
        String []retArray = new String[0];
        return(retArray);
    }

    private String []getDynamicSpecies(Model pModel)
    {
        Collection dynamicSymbols = pModel.getDynamicSymbols();
        List dynamicSymbolsList = new LinkedList(dynamicSymbols);
        Collections.sort(dynamicSymbolsList);
        String []floatingSpecies = new String[dynamicSymbols.size()];
        Iterator dynamicSymbolsIter = dynamicSymbolsList.iterator();
        int symCtr = 0;
        while(dynamicSymbolsIter.hasNext())
        {
            SymbolValue symbolValue = (SymbolValue) dynamicSymbolsIter.next();
            String symbolName = symbolValue.getSymbol().getName();
            floatingSpecies[symCtr] = symbolName;
            ++symCtr;
        }

        return(floatingSpecies);        
    }
    
    public String []loadCMDLModel(String sbml) throws SBWApplicationException
    {
        try
        {
            byte []cmdlBytes = sbml.getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(cmdlBytes);
            ModelBuilderCommandLanguage modelBuilder = new ModelBuilderCommandLanguage();
            IncludeHandler fileIncludeHandler = null;  // file inclusion not allowed, for security reasons
            Model model = modelBuilder.buildModel(inputStream, fileIncludeHandler);

            String []dynamicSpecies = getDynamicSpecies(model);

            ISimulator simulator = getSimulator();
            SimulationController simulationController = getSimulationController();
            simulator.initialize(model);
            simulator.setController(simulationController);

            return(dynamicSpecies);
        }
        catch(Exception e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            String stackTrace = sw.toString();
            throw new SBWApplicationException("exception in loadSBMLModel()", "an exception occurred in loadSBMLModel(); error message is: " + e.toString() + "\n" + "stacktrace: " + stackTrace);
        }
    }
    
    public String []loadSBMLModel(String sbml) throws SBWApplicationException
    {
        try
        {
            byte []sbmlBytes = sbml.getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(sbmlBytes);
            ModelBuilderMarkupLanguage modelBuilder = new ModelBuilderMarkupLanguage();
            IncludeHandler fileIncludeHandler = null;
            Model model = modelBuilder.buildModel(inputStream, fileIncludeHandler);

            String []dynamicSpecies = getDynamicSpecies(model);
            
            ISimulator simulator = getSimulator();
            SimulationController simulationController = getSimulationController();
            simulator.initialize(model);
            simulator.setController(simulationController);

            return(dynamicSpecies);
        }
        catch(Exception e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            String stackTrace = sw.toString();
            throw new SBWApplicationException("exception in loadSBMLModel()", "an exception occurred in loadSBMLModel(); error message is: " + e.toString() + "\n" + "stacktrace: " + stackTrace);
        }
    }

    public void doTimeBasedSimulation(Module pSource,
                                      double pStartTime, 
                                      double pEndTime, 
                                      int pNumPoints, 
                                      String []pFilter) throws SBWException
    {
        ISimulationCallback frontEnd = getFrontEnd(pSource);
        try
        {
            ISimulator simulator = getSimulator();
            if(pNumPoints <= 0)
            {
                throw new SBWApplicationException("invalid number of points", "number of points requested is less than or equal to zero: " + pNumPoints);
            }

            SimulatorParameters simulatorParameters = simulator.getDefaultSimulatorParameters();

            SimulationController simulationController = getSimulationController();

            SimulationResults simulationResults = simulator.simulate(pStartTime,
                                                                     pEndTime,
                                                                     simulatorParameters,
                                                                     pNumPoints,
                                                                     pFilter);


            if(! simulationController.getCancelled() && null != simulationResults)
            {
                double []timeValues = simulationResults.getResultsTimeValues();
                Object []symbolValues = simulationResults.getResultsSymbolValues();
                
                for(int timeIndex = 0; timeIndex < pNumPoints; ++timeIndex)
                {
                    double []dataPoints = (double []) symbolValues[timeIndex];
                    
                    // pass the data for this time point back to the caller
                    frontEnd.onRowData(dataPoints);

                    Thread.sleep(10);  // this is needed to work around an apparent thread
                                       // timing bug in the SBW simulation driver callback 
                }
            }

        }

        catch(Exception e)
        {
            frontEnd.onError(e.toString());
        }
    }


    public void stop() throws SBWException
    {
        getSimulationController().setStopped(true);
    }

    public void restart() throws SBWException
    {
        SimulationController simController = getSimulationController();
        simController.setStopped(false);
        simController.notifyAll();
    }

    public void doSteadyStateAnalysis(String []pFilter) throws SBWException
    {
        throw new SBWApplicationException("doSteadyStateAnalysis not supported", "method doSteadyStateAnalysis is not supported by this service");
    }


}
