package isb.chem.scripting;

/**
 * Proxy for the {@link isb.chem.sbw.MarkupLanguageModelInstanceImporter} class,
 * used by the {@link MarkupLanguageScriptBuildingUtility} class.
 *
 * @see isb.chem.sbw.MarkupLanguageModelInstanceImporter
 * @see MarkupLanguageScriptBuildingUtility
 *
 * @author Stephen Ramsey (cut-and-pasted from NOMService.java, by Andrew Finney)
 */
public interface IModelInstanceImporter
{
    public void readModelDescription(String xml) throws ModelInstanceImporterException;
    public String getModelName() throws ModelInstanceImporterException;
    public int getNumCompartments() throws ModelInstanceImporterException;
    public int getNumReactions() throws ModelInstanceImporterException;
    public int getNumFloatingSpecies() throws ModelInstanceImporterException;
    public int getNumBoundarySpecies() throws ModelInstanceImporterException;
    public int getNumGlobalParameters() throws ModelInstanceImporterException;
    public String getNthCompartmentName(int compartment) throws ModelInstanceImporterException;
    public String getNthFloatingSpeciesName(int floatingSpecies) throws ModelInstanceImporterException;
    public String getNthBoundarySpeciesName(int boundarySpecies) throws ModelInstanceImporterException;
    public String getNthFloatingSpeciesCompartmentName(int floatingSpecies) throws ModelInstanceImporterException;
    public String getNthBoundarySpeciesCompartmentName(int boundarySpecies) throws ModelInstanceImporterException;
    public String getNthReactionName(int reaction) throws ModelInstanceImporterException;
    public int getNumReactants(int reaction) throws ModelInstanceImporterException;
    public int getNumProducts(int reaction) throws ModelInstanceImporterException;
    public String getNthReactantName(int reaction, int reactant) throws ModelInstanceImporterException;
    public String getNthProductName(int reaction, int product) throws ModelInstanceImporterException;
    public String getKineticLaw(int reaction) throws ModelInstanceImporterException;
    public int getNthReactantStoichiometry(int reaction, int reactant) throws ModelInstanceImporterException;
    public int getNthProductStoichiometry(int reaction, int product) throws ModelInstanceImporterException;
    public int getNumParameters(int reaction) throws ModelInstanceImporterException;
    public String getNthParameterName(int reaction, int parameter) throws ModelInstanceImporterException;
    public double getNthParameterValue(int reaction, int parameter) throws ModelInstanceImporterException;
    public boolean getNthParameterHasValue(int reaction, int parameter) throws ModelInstanceImporterException;
    public String getNthGlobalParameterName(int globalParameter) throws ModelInstanceImporterException;
    public boolean hasValue(String name) throws ModelInstanceImporterException;
    public double getValue(String name) throws ModelInstanceImporterException;
    public String[] getBuiltinFunctionInfo(String name) throws ModelInstanceImporterException;
    public String[] getBuiltinFunctions() throws ModelInstanceImporterException;
}
