package isb.chem.scripting;

import java.util.*;
import java.io.*;
import java.text.*;
import isb.chem.*;
import isb.util.*;

/**
 * Runtime environment for execution of {@link Script} objects.
 * Contains an {@link isb.chem.ISimulator} for conducting model
 * simulations.  
 * 
 * @see isb.chem.ISimulator
 * @see Script
 *
 * @author Stephen Ramsey
 */
public class ScriptRuntime
{
    /*========================================*
     * constants
     *========================================*/
    /**
     * Defines the default number of time points for a simulation
     */
    public static final int DEFAULT_NUMBER_TIME_POINTS = 100;

    /**
     * Defines the time value at which simulations always start. This parameter
     * is defined to be 0.0, by default.
     */
    public static final double DEFAULT_START_TIME = 0.0;

    private static final int MINIMUM_ENSEMBLE_SIZE = 1;

    private static final String DEFAULT_SIMULATOR_ALIAS = isb.chem.GillespieSimulator.CLASS_ALIAS;
    private static final String DEFAULT_EXPORTER_ALIAS = "markup-language";

    /*========================================*
     * inner classes
     *========================================*/

    /*========================================*
     * member data
     *========================================*/
    private HashMap mSpecies;
    private HashMap mCompartments;
    private HashMap mGlobalParameters;
    private HashMap mReactions;
    private HashMap mModels;
    private HashMap mSpeciesPopulations;
    private ClassRegistry mSimulatorRegistry;
    private SimulationController mSimulationController;
    private SpeciesPopulations []mSpeciesPopulationsFromLastSimulation;
    private ClassRegistry mExporterRegistry;
    private PrintWriter mOutputWriter;
    private PrintWriter mDebugOutputWriter;

    /*========================================*
     * accessor/mutator methods
     *========================================*/

    /**
     * Sets the debug output writer for this object to be <code>pDebugOutputWriter</code>.
     * The default value is <code>System.out</code>.
     *
     * @param pDebugOutputWriter the debug output writer for this object
     */
    public void setDebugOutputWriter(PrintWriter pDebugOutputWriter)
    {
        mDebugOutputWriter = pDebugOutputWriter;
    }

    /**
     * Returns the debug output writer for this object.
     *
     * @return the debug output writer for this object.
     */
    public PrintWriter getDebugOutputWriter()
    {
        return(mDebugOutputWriter);
    }

    /**
     * Sets the output writer for this object to be <code>pOutputWriter</code>.
     * The default value is <code>System.out</code>.
     *
     * @param pOutputWriter the output writer for this object
     */
    public void setOutputWriter(PrintWriter pOutputWriter)
    {
        mOutputWriter = pOutputWriter;
    }

    /**
     * Returns the output writer for this object.
     *
     * @return the output writer for this object.
     */
    public PrintWriter getOutputWriter()
    {
        return(mOutputWriter);
    }

    private void setExporterRegistry(ClassRegistry pExporterRegistry)
    {
        mExporterRegistry = pExporterRegistry;
    }

    private ClassRegistry getExporterRegistry()
    {
        return(mExporterRegistry);
    }

    private HashMap getSpecies()
    {
        return(mSpecies);
    }

    private void setSpecies(HashMap pSpecies)
    {
        mSpecies = pSpecies;
    }

    private HashMap getCompartments()
    {
        return(mCompartments);
    }

    private void setCompartments(HashMap pCompartments)
    {
        mCompartments = pCompartments;
    }

    private HashMap getReactions()
    {
        return(mReactions);
    }

    private void setReactions(HashMap pReactions)
    {
        mReactions = pReactions;
    }

    private HashMap getGlobalParameters()
    {
        return(mGlobalParameters);
    }

    private void setGlobalParameters(HashMap pGlobalParameters)
    {
        mGlobalParameters = pGlobalParameters;
    }

    private HashMap getModels()
    {
        return(mModels);
    }

    private void setModels(HashMap pModels)
    {
        mModels = pModels;
    }

    private HashMap getSpeciesPopulations()
    {
        return(mSpeciesPopulations);
    }

    private void setSpeciesPopulations(HashMap pSpeciesPopulations)
    {
        mSpeciesPopulations = pSpeciesPopulations;
    }
    
    private ClassRegistry getSimulatorRegistry()
    {
        return(mSimulatorRegistry);
    }

    private void setSimulatorRegistry(ClassRegistry pSimulatorRegistry)
    {
        mSimulatorRegistry = pSimulatorRegistry;
    }
    
    public SimulationController getSimulationController()
    {
        return(mSimulationController);
    }

    public void setSimulationController(SimulationController pSimulationController)
    {
        mSimulationController = pSimulationController;
    }

    private void setSpeciesPopulationsFromLastSimulation(SpeciesPopulations []pSpeciesPopulationsFromLastSimulation)
    {
        mSpeciesPopulationsFromLastSimulation = pSpeciesPopulationsFromLastSimulation;
    }

    public SpeciesPopulations []getSpeciesPopulationsFromLastSimulation()
    {
        return(mSpeciesPopulationsFromLastSimulation);
    }

    /*========================================*
     * initialization methods
     *========================================*/
    private void initializeSimulatorRegistry() throws ClassNotFoundException, IOException, IllegalArgumentException
    {
        ClassRegistry simulatorRegistry = new ClassRegistry(isb.chem.ISimulator.class);
        simulatorRegistry.buildRegistry();
        setSimulatorRegistry(simulatorRegistry);
    }

    private void initializeExporterRegistry() throws ClassNotFoundException, IOException, IllegalArgumentException
    {
        ClassRegistry exporterRegistry = new ClassRegistry(isb.chem.scripting.IModelInstanceExporter.class);
        exporterRegistry.buildRegistry();
        setExporterRegistry(exporterRegistry);
    }


    /*========================================*
     * constructors
     *========================================*/
    public ScriptRuntime() throws ScriptRuntimeException
    {
        try
        {
            setSpecies(new HashMap());
            setCompartments(new HashMap());
            setGlobalParameters(new HashMap());
            setReactions(new HashMap());
            setModels(new HashMap());
            setSpeciesPopulations(new HashMap());
            initializeSimulatorRegistry();
            initializeExporterRegistry();
            setSimulationController(null);
            setSpeciesPopulationsFromLastSimulation(null);
            boolean autoFlush = true;
            setOutputWriter(new PrintWriter(System.out, autoFlush));
            setDebugOutputWriter(new PrintWriter(System.err, autoFlush));
        }
        catch(Exception e)
        {
            throw new ScriptRuntimeException(e.toString(), e);
        }
    }

    /*========================================*
     * private methods
     *========================================*/

    private Compartment getCompartment(String pCompartmentName) throws DataNotFoundException
    {
        Compartment compartment = (Compartment) (getCompartments().get(pCompartmentName));
        if(null == compartment)
        {
            throw new DataNotFoundException("could not find compartment: " + pCompartmentName);
        }
        return(compartment);
    }

    private void addCompartment(Compartment pCompartment) throws IllegalStateException
    {
        HashMap compartments = getCompartments();
        String compartmentName = pCompartment.getName();
        Compartment existCompartment = (Compartment) compartments.get(compartmentName);
        if(null != existCompartment)
        {
            if(! existCompartment.equals(pCompartment))
            {
                throw new IllegalStateException("compartment already exists: " + compartmentName);
            }
        }
        else
        {
            compartments.put(compartmentName, pCompartment);
        }
    }

    private Species getSpecies(String pSpeciesName) throws DataNotFoundException
    {
        Species species = (Species) (getSpecies().get(pSpeciesName));
        if(null == species)
        {
            throw new DataNotFoundException("could not find species: " + pSpeciesName);
        }
        return(species);
    }

    private void addSpecies(Species pSpecies) throws IllegalStateException
    {
        HashMap speciesMap = getSpecies();
        String speciesName = pSpecies.getName();
        Species existSpecies = (Species) speciesMap.get(speciesName);
        if(null != existSpecies)
        {
            if(! existSpecies.equals(pSpecies))
            {
                throw new IllegalStateException("species already exists: " + speciesName);
            }
        }
        else
        {
            speciesMap.put(speciesName, pSpecies);
        }
    }


    private Reaction getReaction(String pReactionName) throws DataNotFoundException
    {
        Reaction reaction = (Reaction) (getReactions().get(pReactionName));
        if(null == reaction)
        {
            throw new DataNotFoundException("could not find reaction: " + pReactionName);
        }
        return(reaction);
    }

    private void addReaction(Reaction pReaction) throws IllegalStateException
    {
        HashMap reactions = getReactions();
        String reactionName = pReaction.getName();
        if(null != reactions.get(reactionName))
        {
            throw new IllegalStateException("reaction already exists: " + reactionName);
        }
        reactions.put(reactionName, pReaction);
    }

    private Parameter getGlobalParameter(String pGlobalParameterName) throws DataNotFoundException
    {
        Parameter globalParameter = (Parameter) (getGlobalParameters().get(pGlobalParameterName));
        if(null == globalParameter)
        {
            throw new DataNotFoundException("could not find global parameter: " + pGlobalParameterName);
        }
        return(globalParameter);
    }

    private void addGlobalParameter(Parameter pGlobalParameter) throws IllegalStateException
    {
        HashMap globalParameters = getGlobalParameters();
        String globalParameterName = pGlobalParameter.getName();
        if(null != globalParameters.get(globalParameterName))
        {
            throw new IllegalStateException("global parameter already exists: " + globalParameterName);
        }
        globalParameters.put(globalParameterName, pGlobalParameter);
    }


    public Model getModel(String pModelName) throws DataNotFoundException
    {
        Model model = (Model) (getModels().get(pModelName));
        if(null == model)
        {
            throw new DataNotFoundException("could not find model: " + pModelName);
        }
        return(model);
    }

    private void addModel(Model pModel) throws IllegalStateException
    {
        HashMap models = getModels();
        String modelName = pModel.getName();
        if(null != models.get(modelName))
        {
            throw new IllegalStateException("model already exists: " + modelName);
        }
        models.put(modelName, pModel);
    }

    public SpeciesPopulations getSpeciesPopulations(String pSpeciesPopulationsName) throws DataNotFoundException
    {
        SpeciesPopulations speciesPopulations = (SpeciesPopulations) (getSpeciesPopulations().get(pSpeciesPopulationsName));
        if(null == speciesPopulations)
        {
            throw new DataNotFoundException("could not find speciesPopulations: " + pSpeciesPopulationsName);
        }
        return(speciesPopulations);
    }

    private void addSpeciesPopulations(SpeciesPopulations pSpeciesPopulations) throws IllegalStateException
    {
        HashMap speciesPopulations = getSpeciesPopulations();
        String speciesPopulationsName = pSpeciesPopulations.getName();
        if(null != speciesPopulations.get(speciesPopulationsName))
        {
            throw new IllegalStateException("speciesPopulations already exists: " + speciesPopulationsName);
        }
        speciesPopulations.put(speciesPopulationsName, pSpeciesPopulations);
    }


    private Element getRequiredElement(Statement pStatement, Element.Type pElementType) throws DataNotFoundException
    {
        Element element = getOptionalElement(pStatement, pElementType);
        if(null == element)
        {
            throw new DataNotFoundException("missing required element type: " + pElementType + " in statement: " + pStatement);
        }
        return(element);
    }

    private Element getOptionalElement(Statement pStatement, Element.Type pElementType) throws DataNotFoundException
    {
        return(pStatement.getElement(pElementType));
    }

    private void executeStatementPrintModel(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.PRINTMODEL == statementType) : "invalid statement type";
        String modelName = pStatement.getName();
        assert (null != modelName) : "parser improperly defined: empty statement name encountered";
        
        Model model = getModel(modelName);
        getOutputWriter().println(model);
    }

    private void executeStatementAddParameterToModel(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.ADDPARAMETERTOMODEL == statementType) : "invalid statement type";
        String modelName = pStatement.getName();
        assert (null != modelName) : "parser improperly defined: empty statement name encountered";
        Model model = getModel(modelName);
        Element parameterElement = getRequiredElement(pStatement, Element.Type.PARAMETER);
        String parameterName = parameterElement.getSymbolName();
        assert (null != parameterName) : "parser improperly defined:  empty element symbol name for element type PARAMETER";
        Parameter parameter = getGlobalParameter(parameterName);
        model.addParameter(parameter);
    }

    private void executeStatementAddReactionToModel(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.ADDREACTIONTOMODEL == statementType) : "invalid statement type";
        String modelName = pStatement.getName();
        assert (null != modelName) : "parser improperly defined: empty statement name encountered";
        Model model = getModel(modelName);
        Element reactionElement = getRequiredElement(pStatement, Element.Type.REACTION);
        String reactionName = reactionElement.getSymbolName();
        assert (null != reactionName) : "parser improperly defined:  empty element symbol name for element type REACTION";
        Reaction reaction = getReaction(reactionName);
        model.addReaction(reaction);
    }

    private void executeStatementPrintSpeciesPopulations(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.PRINTSPECIESPOPULATIONS == statementType) : "invalid statement type";
        String speciesPopulationsName = pStatement.getName();
        assert (null != speciesPopulationsName) : "parser improperly defined: empty statement name encountered";
        
        SpeciesPopulations speciesPopulations = getSpeciesPopulations(speciesPopulationsName);
        getOutputWriter().println(speciesPopulations);
    }

    private void executeStatementValidateModel(Statement pStatement) throws DataNotFoundException, IllegalStateException, IllegalArgumentException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.VALIDATEMODEL == statementType) : "invalid statement type";
        String modelName = pStatement.getName();
        assert (null != modelName) : "parser improperly defined: empty statement name encountered";
        Model model = getModel(modelName);
        Element speciesPopulationsElement = getRequiredElement(pStatement, Element.Type.SPECIESPOPULATIONS);
        String speciesPopulationsName = speciesPopulationsElement.getSymbolName();
        assert (null != speciesPopulationsName) : "parser improperly defined:  empty symbol name for element type SPECIESPOPULATIONS";
        SpeciesPopulations speciesPopulations = getSpeciesPopulations(speciesPopulationsName);
        Element startTimeElement = getOptionalElement(pStatement, Element.Type.STARTTIME);
        Double startTime = null;
        if(null != startTimeElement)
        {
            startTime = startTimeElement.getNumberValue();
            assert (null != startTime) : "parser improperly defined:  empty number value for element type STARTTIME";
        }
        model.validate(speciesPopulations, startTime);
    }

    static ReactionRateSpeciesMode getReactionRateSpeciesMode(Element.ModifierCode pReactionRateSpeciesModeModifier) 
    {
        ReactionRateSpeciesMode retVal = null;
        if(pReactionRateSpeciesModeModifier.equals(Element.ModifierCode.CONCENTRATION))
        {
            retVal = ReactionRateSpeciesMode.CONCENTRATION;
        }
        else if(pReactionRateSpeciesModeModifier.equals(Element.ModifierCode.MOLECULES))
        {
            retVal = ReactionRateSpeciesMode.MOLECULES;
        }
        else
        {
            assert false: new String("parser improperly defined:  invalid modifier for element type SPECIESMODE; modifier is: " + pReactionRateSpeciesModeModifier);
        }
        return(retVal);
    }

    private void executeStatementModel(Statement pStatement) throws DataNotFoundException, IllegalArgumentException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.MODEL == statementType) : "invalid statement type";
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";

        Model model = new Model(statementName);
        Element speciesModeElement = getOptionalElement(pStatement, Element.Type.SPECIESMODE);
        if(null != speciesModeElement)
        {
            Element.ModifierCode speciesModeModifier = speciesModeElement.getModifier();
            assert (null != speciesModeModifier) : "parser improperly defined:  modifier value not set for element type SPECIESMODE";
            ReactionRateSpeciesMode reactionRateSpeciesMode = getReactionRateSpeciesMode(speciesModeModifier);
            model.setReactionRateSpeciesMode(reactionRateSpeciesMode);
        }
        Element subModelsElement = getOptionalElement(pStatement, Element.Type.SUBMODELS);
        if(null != subModelsElement)
        {
            Iterator subModelsIter = subModelsElement.getDataListIter();
            assert (subModelsIter.hasNext()) : "parser improperly defined:  element type SUBMODELS has no elements in its symbol list";
            while(subModelsIter.hasNext())
            {
                String subModelName = (String) subModelsIter.next();
                Model subModel = getModel(subModelName);
                model.addSubModel(subModel);
            }
        }
        Element reactions = getOptionalElement(pStatement, Element.Type.REACTIONS);
        if(null != reactions)
        {
            Iterator reactionsIter = reactions.getDataListIter();
            assert (reactionsIter.hasNext()) : "parser improperly defined:  element type REACTIONS has no elements in its symbol list";
            while(reactionsIter.hasNext())
            {
                String reactionName = (String) reactionsIter.next();
                Reaction reaction = getReaction(reactionName);
                model.addReaction(reaction);
            }
        }
        Element parameters = getOptionalElement(pStatement, Element.Type.PARAMETERS);
        if(null != parameters)
        {
            Iterator parametersIter = parameters.getDataListIter();
            assert (parametersIter.hasNext()) : "parser improperly defined:  element type PARAMETERS has no elements in its symbol list";
            while(parametersIter.hasNext())
            {
                String parameterName = (String) parametersIter.next();
                Parameter parameter = getGlobalParameter(parameterName);
                model.addParameter(parameter);
            }
        }
        addModel(model);
    }

    PrintWriter handleOutputFile(Statement pStatement) throws IOException, DataNotFoundException
    {
        PrintWriter outputWriter = null;
        Element outputFileElement = getOptionalElement(pStatement, Element.Type.OUTPUTFILE);
        if(null != outputFileElement)
        {
            String outputFileName = outputFileElement.getSymbolName();
            if(null != outputFileName)
            {
                File outputFile = new File(outputFileName);
                FileWriter outputFileWriter = new FileWriter(outputFile);
                boolean autoFlush = false;
                outputWriter = new PrintWriter(outputFileWriter, autoFlush);
            }
            else
            {
                assert false: "parser improperly defined:  element type OUTPUTFILE has no symbol value defined";
            }
        }
        else
        {
            outputWriter = getOutputWriter();
        }
        return(outputWriter);
    }

    private void executeStatementSimulate(Statement pStatement) throws DataNotFoundException, IllegalArgumentException, IllegalStateException, IOException, SimulationFailedException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.SIMULATE == statementType) : "invalid statement type";
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";

        String modelName = statementName;
        Model model = getModel(modelName);

        double startTime = DEFAULT_START_TIME;

        Element startTimeElement = getOptionalElement(pStatement, Element.Type.STARTTIME);
        if(null != startTimeElement)
        {
            Double startTimeObj = startTimeElement.getNumberValue();
            assert (null != startTimeObj) : "parser improperly defined:  element type STARTTIME has no numeric value defined";
            startTime = startTimeObj.doubleValue();
        }

        PrintWriter outputPrintWriter = handleOutputFile(pStatement);

        String simulatorAlias = DEFAULT_SIMULATOR_ALIAS;
        Element simulatorElement = getOptionalElement(pStatement, Element.Type.SIMULATOR);
        if(null != simulatorElement)
        {
            simulatorAlias = simulatorElement.getSymbolName();
            assert (null != simulatorAlias) : "parser improperly defined:  element type SIMULATOR has no symbol value defined";
        }

        ISimulator simulator = (ISimulator) getSimulatorRegistry().getInstance(simulatorAlias);

        if(null == simulator)
        {
            throw new IllegalStateException("a simulator is not currently defined for the Runtime, so the SIMULATE statement cannot be processed");
        }

        PrintWriter debugOutput = simulator.getDebugOutput();

        Element debugElement = getOptionalElement(pStatement, Element.Type.DEBUG);
        PrintWriter newDebugOutput = null;
        if(null != debugElement)
        {
            Double debugValue = debugElement.getNumberValue();
            assert (null != debugValue) : "parser improperly defined:  element type DEBUG has no integer value defined";
            int debugValueInt = debugValue.intValue();
            DebugOutputVerbosityLevel debugLevel = DebugOutputVerbosityLevel.get(debugValueInt);
            if(null == debugLevel)
            {
                throw new IllegalArgumentException("invalid debug level specified: " + debugValueInt);
            }

            if(debugLevel.greaterThan(DebugOutputVerbosityLevel.NONE))
            {
                newDebugOutput = getDebugOutputWriter();
            }
            else
            {
                newDebugOutput = null;
            }

            simulator.setDebugOutput(newDebugOutput);
            simulator.setDebugLevel(debugLevel);
        }

        Element stopTimeElement = getRequiredElement(pStatement, Element.Type.STOPTIME);
        Double stopTimeObj = stopTimeElement.getNumberValue();
        assert (null != stopTimeObj) : "parser improperly defined:  element type STOPTIME has no numeric value defined";
        double stopTime = stopTimeObj.doubleValue();
        Element speciesPopulationsElement = getRequiredElement(pStatement, Element.Type.SPECIESPOPULATIONS);
        String speciesPopulationsName = speciesPopulationsElement.getSymbolName();
        assert (null != speciesPopulationsName) : "parser improperly defined:  element type SPECIESPOPULATIONS has no symbol value defind";
        SpeciesPopulations speciesPopulations = getSpeciesPopulations(speciesPopulationsName);
        Element numberTimePointsElement = getOptionalElement(pStatement, Element.Type.NUMTIMEPOINTS);
        int numberTimePoints = DEFAULT_NUMBER_TIME_POINTS;
        if(null != numberTimePointsElement)
        {
            Double numberTimePointsObj = numberTimePointsElement.getNumberValue();
            assert (null != numberTimePointsObj) : "parser improperly defined:  element type NUMBERTIMEPOINTS has no numeric value defined";
            numberTimePoints = numberTimePointsObj.intValue();
        }
        if(numberTimePoints <= 0)
        {
            throw new IllegalArgumentException("invalid number of time points requested for simulation: " + numberTimePoints);
        }

        Element viewSpeciesElement = getRequiredElement(pStatement, Element.Type.VIEWSPECIES);
        Iterator viewSpeciesIter = viewSpeciesElement.getDataListIter();
        Vector viewSpecies = new Vector();
        while(viewSpeciesIter.hasNext())
        {
            String speciesName = (String) viewSpeciesIter.next();
            Species species = getSpecies(speciesName);
            viewSpecies.add(species);
        }
        

        Element ensembleSizeElement = getOptionalElement(pStatement, Element.Type.ENSEMBLESIZE);
        boolean doEnsembleAverage = false;
        int ensembleSize = MINIMUM_ENSEMBLE_SIZE;
        if(null != ensembleSizeElement)
        {
            Double ensembleSizeDouble = ensembleSizeElement.getNumberValue();
            assert (null != ensembleSizeDouble) : "parser improperly defined:  element type ENSEMBLESIZE has no numeric value defined";
            ensembleSize = ensembleSizeDouble.intValue();
            if(ensembleSize < MINIMUM_ENSEMBLE_SIZE)
            {
                throw new IllegalArgumentException("invalid ensemble size specified: " + ensembleSize);
            }
            doEnsembleAverage = true;
        }

        Element.ModifierCode output = Element.ModifierCode.PRINT;
        Element outputElement = getOptionalElement(pStatement, Element.Type.OUTPUT);
        if(null != outputElement)
        {
            output = outputElement.getModifier();
            assert (null != output) : "parser improperly defined:  element type OUTPUT has a null modifier value";
            assert (Element.ModifierCode.PRINT == output ||
                    Element.ModifierCode.STORE == output) : new String("parser improperly defined:  element type OUTPUT has an invalid modifier value" + output);
        }

        String resultSpeciesPopulationsStoreName = null;
        Element storeNameElement = getOptionalElement(pStatement, Element.Type.STORENAME);
        if(null != storeNameElement)
        {
            if(doEnsembleAverage)
            {
                throw new IllegalArgumentException("specifying the storeName element and the ensembleSize element in the same simulation is not permitted; the capability to store the output of an ensemble-averaged simulation is not yet supported; sorry");
            }

            if(! output.equals(Element.ModifierCode.STORE))
            {
                throw new IllegalArgumentException("in order to use the storeName element, you must also specify the element \"output store\", which specifies that you want the output stored (as opposed to printed)");
            }

            resultSpeciesPopulationsStoreName = storeNameElement.getSymbolName();
            assert (null != resultSpeciesPopulationsStoreName): "parser improperly defined:  element STORENAME has no symbol value defined";
        }

        SpeciesPopulations []speciesPopulationSnapshots = new SpeciesPopulations[numberTimePoints];
        SimulationController simulationController = getSimulationController();

        double ensembleMultiplier = 1.0;
        double deltaTime = (stopTime - startTime)/((double) numberTimePoints);

        if(! doEnsembleAverage)
        {
            simulator.evolve(model,
                             speciesPopulations,
                             startTime,
                             stopTime,
                             speciesPopulationSnapshots,
                             simulationController);        
        }
        else
        {
            for(int timePointsCtr = 0; timePointsCtr < numberTimePoints; ++timePointsCtr)
            {
                double time = startTime + ((double) timePointsCtr)*deltaTime;
                speciesPopulationSnapshots[timePointsCtr] = new SpeciesPopulations("time=" + time);
            }

            for(int ensembleCtr = 0; ensembleCtr < ensembleSize; ++ensembleCtr)
            {
                SpeciesPopulations []ensembleSpeciesPopulationSnapshots = new SpeciesPopulations[numberTimePoints];
                if(null != newDebugOutput)
                {
                    newDebugOutput.println("starting simulation number:       " + ensembleCtr);
                }
                simulator.evolve(model,
                                 speciesPopulations,
                                 startTime,
                                 stopTime,
                                 ensembleSpeciesPopulationSnapshots,
                                 simulationController);                 
                for(int timePointCtr = 0; timePointCtr < numberTimePoints; ++timePointCtr)
                {
                    speciesPopulationSnapshots[timePointCtr].vectorAdd(ensembleSpeciesPopulationSnapshots[timePointCtr]);
                }
            }
            ensembleMultiplier = 1.0/((double) ensembleSize);
        }
        

        if(null != simulationController &&
           simulationController.getCancelled())
        {
            return;
        }

        if(null != debugElement)
        {
            simulator.setDebugOutput(debugOutput);
        }

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(6);
        nf.setGroupingUsed(false);

        if(Element.ModifierCode.PRINT == output)
        {
            int numViewSpecies = viewSpecies.size();
            
            {
                StringBuffer sb = new StringBuffer("#");
                sb.append("time, ");
                for(int speciesIndex = 0; speciesIndex < numViewSpecies; ++speciesIndex)
                {
                    Species species = (Species) viewSpecies.elementAt(speciesIndex);
                    sb.append(species.getName());
                    if(speciesIndex < numViewSpecies - 1)
                    {
                        sb.append(", ");
                    }
                }
                outputPrintWriter.println(sb.toString());
            }

            for(int timeIndex = 0; timeIndex < numberTimePoints; ++timeIndex)
            {
                StringBuffer sb = new StringBuffer(); 
                double time = startTime + ((double) timeIndex)*deltaTime;
                sb.append(nf.format(time) + ", ");
                SpeciesPopulations speciesPopulationSnapshot = speciesPopulationSnapshots[timeIndex];
                for(int speciesIndex = 0; speciesIndex < numViewSpecies; ++speciesIndex)
                {
                    Species species = (Species) viewSpecies.elementAt(speciesIndex);
                    String speciesName = species.getName();
                    double floatingPointSpeciesPopulation = 0.0;
                    if(! speciesPopulationSnapshot.speciesPopulationIsExpression(speciesName))
                    {
                        double speciesPopulation = speciesPopulationSnapshot.getSpeciesPopulation(species);
                        floatingPointSpeciesPopulation = speciesPopulation * ensembleMultiplier;
                    }
                    else
                    {
                        floatingPointSpeciesPopulation = speciesPopulationSnapshot.getSpeciesPopulation(species, model, time);
                    }
                    sb.append(nf.format(floatingPointSpeciesPopulation));
                    if(speciesIndex < numViewSpecies - 1)
                    {
                        sb.append(", ");
                    }
                }
                outputPrintWriter.println(sb.toString());
            }

            outputPrintWriter.flush();
        }
        else
        {
            // handle store output
            setSpeciesPopulationsFromLastSimulation(speciesPopulationSnapshots);
            if(null != resultSpeciesPopulationsStoreName)
            {
                SpeciesPopulations finalSpeciesPopulations = (SpeciesPopulations) speciesPopulationSnapshots[numberTimePoints - 1].clone();
                finalSpeciesPopulations.setName(resultSpeciesPopulationsStoreName);
                addSpeciesPopulations(finalSpeciesPopulations);
            }
        }
    }

    private void executeStatementParameter(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.PARAMETER == statementType) : "invalid statement type";
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";

        Element valueElement = getRequiredElement(pStatement, Element.Type.VALUE);
        Double value = valueElement.getNumberValue();
        Element reactionsElement = getOptionalElement(pStatement, Element.Type.REACTIONS);
        Parameter parameter = new Parameter(statementName, value.doubleValue());
        if(null != reactionsElement)
        {
            // this is a reaction-only parameter
            Iterator symbolIter = reactionsElement.getDataListIter();
            while(symbolIter.hasNext())
            {
                String reactionName = (String) symbolIter.next();
                Reaction reaction = getReaction(reactionName);
                reaction.addParameter(parameter);
            }
        }
        else
        {
            addGlobalParameter(parameter);
        }
    }

    private void executeStatementReaction(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.REACTION == statementType) : "invalid statement type";
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";

        Element reactantsElement = getRequiredElement(pStatement, Element.Type.REACTANTS);
        Iterator reactantsIter = reactantsElement.getDataListIter();
        Reaction reaction = new Reaction(statementName);
        HashMap reactants = new HashMap();
        while(reactantsIter.hasNext())
        {
            String speciesName = (String) reactantsIter.next();
            Species species = getSpecies(speciesName);
            MutableInteger stoichiometry = (MutableInteger) reactants.get(species);
            if(null == stoichiometry)
            {
                stoichiometry = new MutableInteger(1);
                reactants.put(species, stoichiometry);
            }
            else
            {
                stoichiometry.setValue(stoichiometry.getValue()+1);
            }
        }
        reactantsIter = reactants.keySet().iterator();
        while(reactantsIter.hasNext())
        {
            Species species = (Species) reactantsIter.next();
            MutableInteger stoichiometry = (MutableInteger) reactants.get(species);
            reaction.addReactant(species, stoichiometry.getValue());
        }
        Element productsElement = getRequiredElement(pStatement, Element.Type.PRODUCTS);
        Iterator productsIter = productsElement.getDataListIter();
        HashMap products = new HashMap();
        while(productsIter.hasNext())
        {
            String speciesName = (String) productsIter.next();
            Species species = getSpecies(speciesName);
            MutableInteger stoichiometry = (MutableInteger) products.get(species);
            if(null == stoichiometry)
            {
                stoichiometry = new MutableInteger(1);
                products.put(species, stoichiometry);
            }
            else
            {
                stoichiometry.setValue(stoichiometry.getValue()+1);
            }
        }        
        productsIter = products.keySet().iterator();
        while(productsIter.hasNext())
        {
            Species species = (Species) productsIter.next();
            MutableInteger stoichiometry = (MutableInteger) products.get(species);
            reaction.addProduct(species, stoichiometry.getValue());
        }

        Element rateElement = getRequiredElement(pStatement, Element.Type.RATE);
        String rateSymbol = rateElement.getSymbolName();
        Double rateValue = rateElement.getNumberValue();
        assert (null != rateSymbol || null != rateValue) : "parser improperly defined: element type RATE has both its symbol and numeric value set to null";
        assert (null == rateSymbol || null == rateValue) : "parser improperly defined: element type RATE has neither its symbol and numeric value set to null";
        if(rateSymbol != null)
        {
            String reactionRateExpressionStr = rateSymbol;
            MathExpression reactionRateExpression = new MathExpression(reactionRateExpressionStr);
            reaction.setRate(reactionRateExpression);
        }
        else
        {
            reaction.setRate(rateValue.doubleValue());
        }
        addReaction(reaction);        
    }

    private void executeStatementSpecies(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.SPECIES == statementType) : "invalid statement type";
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";

        Element compartmentElement = getRequiredElement(pStatement, Element.Type.COMPARTMENT);
        String compartmentName = compartmentElement.getSymbolName();
        Compartment compartment = getCompartment(compartmentName);
        Species species = new Species(statementName, compartment);
        Element speciesTypeElement = getOptionalElement(pStatement, Element.Type.SPECIESTYPE);
        if(null != speciesTypeElement)
        {
            Element.ModifierCode speciesTypeModifier = speciesTypeElement.getModifier();
            boolean floating = true;
            if(Element.ModifierCode.BOUNDARY == speciesTypeModifier)
            {
                floating = false;
            }
            else if(Element.ModifierCode.FLOATING == speciesTypeModifier)
            {
                floating = true;
            }
            else
            {
                assert false : new String("parser improperly defined:  invalid modifier for element type SPECIESTYPE: " + speciesTypeModifier);
            }
            species.setFloating(floating);
        }
        addSpecies(species);
    }

    private void executeStatementCompartment(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.COMPARTMENT == statementType) : "invalid statement type";
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";

        Compartment compartment = new Compartment(statementName);
        Element volumeElement = getOptionalElement(pStatement, Element.Type.VOLUME);            
        if(null != volumeElement)
        {
            Double volume = volumeElement.getNumberValue();
            assert (null != volume) : "parser improperly defined:  element type VOLUME has no numeric value defined";
            compartment.setVolumeLiters(volume.doubleValue());
        }
        addCompartment(compartment);
    }

    private void executeStatementAddToSpeciesPopulations(Statement pStatement) throws DataNotFoundException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.ADDTOSPECIESPOPULATIONS == statementType) : "invalid statement type";
        String speciesPopulationsName = pStatement.getName();
        assert (null != speciesPopulationsName) : "parser improperly defined: empty statement name encountered";

        SpeciesPopulations speciesPopulations = getSpeciesPopulations(speciesPopulationsName);

        Element speciesElement = getRequiredElement(pStatement, Element.Type.SPECIES);
        String speciesName = speciesElement.getSymbolName();
        assert (null != speciesName) : "parser improperly defined:  empty symbol name for element type SPECIES";
        Species species = getSpecies(speciesName);

        Element valueElement = getRequiredElement(pStatement, Element.Type.POPULATION);
        Double value = valueElement.getNumberValue();
        String valueExpression = valueElement.getSymbolName();

        setSpeciesPopulation(speciesPopulations, species, value, valueExpression);
    }

    private void setSpeciesPopulation(SpeciesPopulations pSpeciesPopulations, Species pSpecies, Double pValue, String pValueExpression)
    {
        assert (null != pValue || null != pValueExpression) : "parser improperly defined:  empty number value and empty symbol name for element type POPULATION";
        assert (null == pValue || null == pValueExpression) : "parser improperly defined:  nonempty number value and nonempty symbol name for element type POPULATION";

        if(null != pValue)
        {
            pSpeciesPopulations.setSpeciesPopulation(pSpecies, pValue.doubleValue());
        }
        else
        {
            MathExpression speciesPopulationExpression = new MathExpression(pValueExpression);
            pSpeciesPopulations.setSpeciesPopulation(pSpecies, speciesPopulationExpression);
        }
    }


    private void executeStatementExportModelInstance(Statement pStatement) throws DataNotFoundException, IllegalArgumentException, DataNotFoundException, ModelInstanceExporterException, IOException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.EXPORTMODELINSTANCE == statementType) : "invalid statement type";
        String modelName = pStatement.getName();
        assert (null != modelName) : "parser improperly defined: empty statement name encountered";

        Model model = getModel(modelName);

        Element speciesPopulationsElement = getRequiredElement(pStatement, Element.Type.SPECIESPOPULATIONS);
        String speciesPopulationsName = speciesPopulationsElement.getSymbolName();
        assert (null != speciesPopulationsName) : "parser improperly defined:  element type SPECIESPOPULATIONS has no symbol value defind";
        SpeciesPopulations speciesPopulations = getSpeciesPopulations(speciesPopulationsName);
        
        PrintWriter outputWriter = handleOutputFile(pStatement);

        String exporterAlias = DEFAULT_EXPORTER_ALIAS;
        Element exporterElement = getOptionalElement(pStatement, Element.Type.EXPORTER);
        if(null != exporterElement)
        {
            exporterAlias = exporterElement.getSymbolName();
            assert (null != exporterAlias) : "parser improperly defined:  element type EXPORTER has no symbol value defined";
        }
        
        IModelInstanceExporter modelInstanceExporter = (IModelInstanceExporter) getExporterRegistry().getInstance(exporterAlias);

        modelInstanceExporter.exportModelInstance(model, speciesPopulations, outputWriter);
        outputWriter.flush();
    }

    private void executeStatementSpeciesPopulations(Statement pStatement) throws DataNotFoundException, IllegalArgumentException
    {
        Statement.Type statementType = pStatement.getType();
        assert (Statement.Type.SPECIESPOPULATIONS == statementType) : "invalid statement type";
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";

        Element speciesElement = getOptionalElement(pStatement, Element.Type.SPECIESLIST);
        Element valuesElement = getOptionalElement(pStatement, Element.Type.POPULATIONS);
        if((null == valuesElement && null != speciesElement) ||
           (null != valuesElement && null == speciesElement))
        {
            throw new IllegalArgumentException("for statement of type SPECIESPOPULATIONS, elements SPECIESLIST and POPULATIONS must be specified together, or both omitted");
        }
        SpeciesPopulations speciesPopulations = new SpeciesPopulations(statementName);
        addSpeciesPopulations(speciesPopulations);

        if(null != speciesElement)
        {
            Iterator speciesIter = speciesElement.getDataListIter();
            Iterator valuesIter = valuesElement.getDataListIter();
            while(speciesIter.hasNext() && valuesIter.hasNext())
            {
                String speciesName = (String) speciesIter.next();
                Species species = getSpecies(speciesName);
                Double value = null;
                String valueExpression = null;
                Object valueData = valuesIter.next();
                if(valueData instanceof String)
                {
                    valueExpression = (String) valueData;
                }
                else if(valueData instanceof Double)
                {
                    value = (Double) valueData;
                }
                else
                {
                    assert false : "element type POPULATIONS contained an object of unknown class in its data list";
                }

                setSpeciesPopulation(speciesPopulations, species, value, valueExpression);
            }
            if(speciesIter.hasNext() || valuesIter.hasNext())
            {
                throw new IllegalArgumentException("unequal numbers of species and values list elements");
            }
        }
    }


    private void executeStatement(Statement pStatement) throws DataNotFoundException, IllegalStateException, IllegalArgumentException, ModelInstanceExporterException, IOException, SimulationFailedException
    {
        Statement.Type statementType = pStatement.getType();
        String statementName = pStatement.getName();
        assert (null != statementName) : "parser improperly defined: empty statement name encountered";
        if(Statement.Type.COMPARTMENT == statementType)
        {
            executeStatementCompartment(pStatement);
        }
        else if(Statement.Type.SPECIES == statementType)
        {
            executeStatementSpecies(pStatement);
        }
        else if(Statement.Type.PARAMETER == statementType)
        {
            executeStatementParameter(pStatement);
        }
        else if(Statement.Type.REACTION == statementType)
        {
            executeStatementReaction(pStatement);
        }
        else if(Statement.Type.ADDPARAMETERTOMODEL == statementType)
        {
            executeStatementAddParameterToModel(pStatement);
        }
        else if(Statement.Type.ADDREACTIONTOMODEL == statementType)
        {
            executeStatementAddReactionToModel(pStatement);
        }
        else if(Statement.Type.MODEL == statementType)
        {
            executeStatementModel(pStatement);
        }
        else if(Statement.Type.SPECIESPOPULATIONS == statementType)
        {
            executeStatementSpeciesPopulations(pStatement);
        }
        else if(Statement.Type.ADDTOSPECIESPOPULATIONS == statementType)
        {
            executeStatementAddToSpeciesPopulations(pStatement);
        }
        else if(Statement.Type.SIMULATE == statementType)
        {
            executeStatementSimulate(pStatement);
        }
        else if(Statement.Type.PRINTMODEL == statementType)
        {
            executeStatementPrintModel(pStatement);
        }
        else if(Statement.Type.PRINTSPECIESPOPULATIONS == statementType)
        {
            executeStatementPrintSpeciesPopulations(pStatement);
        }
        else if(Statement.Type.VALIDATEMODEL == statementType)
        {
            executeStatementValidateModel(pStatement);
        }
        else if(Statement.Type.EXPORTMODELINSTANCE == statementType)
        {
            executeStatementExportModelInstance(pStatement);
        }
        else
        {
            assert false : "parser improperly defined:  unknown statement type found: " + statementType;
        }
    }

    /*========================================*
     * protected methods
     *========================================*/

    Set getModelNames()
    {
        return(getModels().keySet());
    }

    Set getSpeciesPopulationsNames()
    {
        return(getSpeciesPopulations().keySet());
    }

    /*========================================*
     * public methods
     *========================================*/


    /**
     * Returns a Set containing the names of all {@link isb.chem.Model}
     * objects in the internal map of models.
     *
     * 
     * @return a Set containing the names of all {@link isb.chem.Model}
     * objects in the internal map of models. 
     */
    public Set getModelNamesCopy()
    {
        Set modelNames = getModelNames();
        HashSet newModelNames = new HashSet();
        Iterator modelNamesIter = modelNames.iterator();
        while(modelNamesIter.hasNext())
        {
            String modelName = (String) modelNamesIter.next();
            newModelNames.add(modelName);
        }
        return(newModelNames);
    }

    /**
     * Returns a Set containing the names of all {@link isb.chem.SpeciesPopulations}
     * objects in the internal map of species populations data structures.
     *
     * 
     * @return a Set containing the names of all {@link isb.chem.SpeciesPopulations}
     * objects in the internal map of species populations data structures. 
     */
    public Set getSpeciesPopulationsNamesCopy()
    {
        Set speciesPopulationsNames = getSpeciesPopulationsNames();
        HashSet newSpeciesPopulationsNames = new HashSet();
        Iterator speciesPopulationsNamesIter = speciesPopulationsNames.iterator();
        while(speciesPopulationsNamesIter.hasNext())
        {
            String speciesPopulationsName = (String) speciesPopulationsNamesIter.next();
            newSpeciesPopulationsNames.add(speciesPopulationsName);
        }
        return(newSpeciesPopulationsNames);
    }

    /**
     * Clear all data structures; sets the internal state of the runtime back
     * to its initial state, i.e., the state before any {@link Script} was
     * executed.
     */
    public void clear()
    {
        getSpecies().clear();
        getCompartments().clear();
        getGlobalParameters().clear();
        getReactions().clear();
        getModels().clear();
        getSpeciesPopulations().clear();
        setSpeciesPopulationsFromLastSimulation(null);
    }


    /**
     * Execute all statements in the the <code>pScript</code> script object.
     *
     * @param pScript the {@link Script} to execute
     */
    public void execute(Script pScript) throws ScriptRuntimeException
    {
        Iterator statementsIter = pScript.getStatementsIter();
        int statementCtr = 0;
        while(statementsIter.hasNext())
        {
            ++statementCtr;
            Statement statement = (Statement) statementsIter.next();
            try
            {
                executeStatement(statement);
            }
            catch(Exception e)
            {
                throw new ScriptRuntimeException("an error occurred processing statement number " + statementCtr + "; statement is: [" + statement.toString() + "]; error message is: " + e.toString(), e);
            }
        }
    }

    public Set getExporterAliasesCopy()
    {
        return(getExporterRegistry().getRegistryAliasesCopy());
    }

    public IModelInstanceExporter getExporter(String pExporterAlias) throws DataNotFoundException
    {
        return((IModelInstanceExporter) getExporterRegistry().getInstance(pExporterAlias));
    }

    public Set getSimulatorAliasesCopy()
    {
        return(getSimulatorRegistry().getRegistryAliasesCopy());
    }

    public ISimulator getSimulator(String pSimulatorAlias) throws DataNotFoundException
    {
        return((ISimulator) getSimulatorRegistry().getInstance(pSimulatorAlias));
    }
}
