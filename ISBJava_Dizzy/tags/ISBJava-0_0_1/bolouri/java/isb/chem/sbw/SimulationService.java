package isb.chem.sbw;

import java.util.*;
import java.io.*;
import isb.chem.*;
import isb.chem.scripting.*;
import isb.util.*;
import edu.caltech.sbw.*;

public abstract class SimulationService implements ISimulationService
{
    /*========================================*
     * constants
     *========================================*/
    public static final String METHOD_SIGNATURE_LOAD_SBML = "string []loadSBMLModel(string)";
    private static final String SERVICE_NAME_SIMULATION_CALLBACK = "SimulationCallback";
    private static final double DEFAULT_START_TIME = 0.0;  // default start time used for validation only

    /*========================================*
     * member data
     *========================================*/
    private ScriptRuntime mScriptRuntime;
    private ScriptBuilder mScriptBuilder;
    private String mModelName;
    private SimulationController mSimulationController;
    private MarkupLanguageParser mMarkupLanguageParser;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    
    private MarkupLanguageParser getMarkupLanguageParser()
    {
        return(mMarkupLanguageParser);
    }

    private void setMarkupLanguageParser(MarkupLanguageParser pMarkupLanguageParser)
    {
        mMarkupLanguageParser = pMarkupLanguageParser;
    }

    private SimulationController getSimulationController()
    {
        return(mSimulationController);
    }

    private void setSimulationController(SimulationController pSimulationController)
    {
        mSimulationController = pSimulationController;
    }

    private String getModelName()
    {
        return(mModelName);
    }

    private void setModelName(String pModelName)
    {
        mModelName = pModelName;
    }

    private ScriptBuilder getScriptBuilder() throws SBWException
    {
        return(mScriptBuilder);
    }

    private void setScriptBuilder(ScriptBuilder pScriptBuilder)
    {
        mScriptBuilder = pScriptBuilder;
    }

    private ScriptRuntime getScriptRuntime()
    {
        return(mScriptRuntime);
    }

    private void setScriptRuntime(ScriptRuntime pScriptRuntime)
    {
        mScriptRuntime = pScriptRuntime;
    }

    /*========================================*
     * initialization methods
     *========================================*/
    private void initialize() throws SBWException
    {
        MarkupLanguageParser parser = new MarkupLanguageParser();
        setMarkupLanguageParser(parser);
        setModelName(null);
        SimulationController simulationController = new SimulationController();
        setSimulationController(simulationController);
        ScriptRuntime scriptRuntime = null;
        try
        {
            scriptRuntime = new ScriptRuntime();
        }
        catch(ScriptRuntimeException e)
        {
            throw new SBWApplicationException("unable to create ScriptRuntime", e.toString());
        }
        scriptRuntime.setSimulationController(simulationController);

        setScriptRuntime(scriptRuntime);
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
    
    private String []getFloatingSpecies(Model pModel)
    {
        Vector floatingSpecies = new Vector();
        Set speciesSet = pModel.getSpeciesSetCopy();
        List speciesList = new LinkedList(speciesSet);
        Collections.sort(speciesList);
        Iterator speciesIter = speciesList.iterator();
        while(speciesIter.hasNext())
        {
            Species species = (Species) speciesIter.next();
            if(species.getFloating())
            {
                floatingSpecies.add(species.getName());
            }
        }
        int numSpecies = floatingSpecies.size();
        String []floatingSpeciesArray = new String[numSpecies];
        for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
        {
            floatingSpeciesArray[speciesCtr] = (String) floatingSpecies.elementAt(speciesCtr);
        }
        return(floatingSpeciesArray);
    }

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
     * Returns an instance of an {@link isb.chem.ISimulator}
     *
     * @return an instance of an {@link isb.chem.ISimulator}
     */
    abstract ISimulator createSimulator();

    public String []optionsSupported()
    {
        String []retArray = new String[0];
        return(retArray);
    }

    public String []loadSBMLModel(String sbml) throws SBWApplicationException
    {
        try
        {
            MarkupLanguageParser parser = getMarkupLanguageParser();
            Script modelDefinitionScript = new Script();

            
            IFileIncludeHandler fileIncludeHandler = null;
            StringReader inputStringReader = new StringReader(sbml);
            BufferedReader bufferedInputReader = new BufferedReader(inputStringReader);
            String modelName = parser.processMarkupLanguage(bufferedInputReader,
                                                            modelDefinitionScript);
            setModelName(modelName);

            ScriptRuntime scriptRuntime = getScriptRuntime();

            // clear the runtime, to remove any previously defined model
            scriptRuntime.clear();

            // load the model into the runtime
            scriptRuntime.execute(modelDefinitionScript);

            // build a script to validate the model and the initial data
            Script modelValidationScript = MarkupLanguageScriptBuildingUtility.buildModelValidateStatement(modelName);

            // validate the model and the initial data
            scriptRuntime.execute(modelValidationScript);

            // obtain the list of floating species for this model
            Model model = scriptRuntime.getModel(modelName);
            String []floatingSpecies = getFloatingSpecies(model);

            return(floatingSpecies);
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
            String modelName = getModelName();
            if(null == modelName)
            {
                throw new SBWApplicationException("no model name found", "model has not been built yet - null model object found");
            }

            Script simulationScript = MarkupLanguageScriptBuildingUtility.buildSimulationScript(modelName, pStartTime, pEndTime, pNumPoints, pFilter);

            ScriptRuntime scriptRuntime = getScriptRuntime();

            if(pNumPoints <= 0)
            {
                throw new SBWApplicationException("invalid number of points", "number of points requested is less than or equal to zero: " + pNumPoints);
            }

            SimulationController simController = getSimulationController();
            scriptRuntime.setSimulationController(simController);
            scriptRuntime.execute(simulationScript);

            SpeciesPopulations []speciesPopulationsArray = scriptRuntime.getSpeciesPopulationsFromLastSimulation();
            if(null == speciesPopulationsArray)
            {
                throw new SBWApplicationException("null species population data", "retrieved a null species population data structure from the script runtime");
            }
            if(speciesPopulationsArray.length != pNumPoints)
            {
                throw new SBWApplicationException("invalid number of time points", "retrieved species population data with an invalid number of time points from the script runtime");
            }

            int numSpecies = pFilter.length;

            // extract data requested by caller
            double []dataPoints = new double[numSpecies];

            for(int timeIndex = 0; timeIndex < pNumPoints; ++timeIndex)
            {
                SpeciesPopulations speciesPopulations = speciesPopulationsArray[timeIndex];

                // extract the species populations for this time point
                speciesPopulations.copyPopulationDataToArray(pFilter, dataPoints);

                // convert data to moles, in order to conform to the SBW SimulationService specification
                for(int speciesCtr = 0; speciesCtr < numSpecies; ++speciesCtr)
                {
                    dataPoints[speciesCtr] = ((double) dataPoints[speciesCtr])/Constants.AVOGADRO_CONSTANT;
                }

                // pass the data for this time point back to the caller
                frontEnd.onRowData(dataPoints);
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
