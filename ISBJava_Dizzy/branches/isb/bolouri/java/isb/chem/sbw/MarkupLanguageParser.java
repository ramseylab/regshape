package isb.chem.sbw;

import isb.util.*;
import isb.chem.scripting.*;
import isb.chem.*;
import java.io.*;

/**
 * This class builds a {@link isb.chem.scripting.Script} from a text input
 * stream containing an SBML description of a {@link isb.chem.Model}. 
 * This class is used by the {@link SimulationService} class.
 *
 * @see isb.chem.Model
 * @see isb.chem.scripting.Script
 *
 * @author Stephen Ramsey
 */
public class MarkupLanguageParser implements IScriptBuildingParser, IAliasableClass
{
    /*========================================*
     * constants
     *========================================*/
    public static final String CLASS_ALIAS = "markup-language";
    private static final double DEFAULT_SPECIES_POPULATIONS_CORRECTION_FACTOR = 1.0;

    /*========================================*
     * inner classes
     *========================================*/
    
    /*========================================*
     * member data
     *========================================*/
    private IModelInstanceImporter mMarkupLanguageModelInstanceImporter;
    private ReactionRateSpeciesMode mReactionRateSpeciesMode;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private IModelInstanceImporter getMarkupLanguageModelInstanceImporter()
    {
        return(mMarkupLanguageModelInstanceImporter);
    }

    private void setMarkupLanguageModelInstanceImporter(IModelInstanceImporter pMarkupLanguageModelInstanceImporter)
    {
        mMarkupLanguageModelInstanceImporter = pMarkupLanguageModelInstanceImporter;
    }

    public void setReactionRateSpeciesMode(ReactionRateSpeciesMode pReactionRateSpeciesMode) throws IllegalArgumentException
    {
        mReactionRateSpeciesMode = pReactionRateSpeciesMode;
    } 

    public ReactionRateSpeciesMode getReactionRateSpeciesMode()
    {
        return(mReactionRateSpeciesMode);
    }

    /*========================================*
     * initialization methods
     *========================================*/
    /*========================================*
     * constructors
     *========================================*/
    public MarkupLanguageParser()
    {
        setReactionRateSpeciesMode(ReactionRateSpeciesMode.CONCENTRATION);
        setMarkupLanguageModelInstanceImporter(new MarkupLanguageModelInstanceImporter());
    }

    MarkupLanguageParser(ReactionRateSpeciesMode pReactionRateSpeciesMode)
    {
        setReactionRateSpeciesMode(pReactionRateSpeciesMode);
        setMarkupLanguageModelInstanceImporter(new MarkupLanguageModelInstanceImporter());
    }
    
    /*========================================*
     * private methods
     *========================================*/

    /*========================================*
     * protected methods
     *========================================*/
    String processMarkupLanguage(BufferedReader pInputReader, Script pScript) throws ModelInstanceImporterException, IOException, IllegalArgumentException
    {
        return(MarkupLanguageScriptBuildingUtility.processMarkupLanguage(pInputReader,
                                                                         getMarkupLanguageModelInstanceImporter(),
                                                                         getReactionRateSpeciesMode(),
                                                                         pScript));
    }

    /*========================================*
     * public methods
     *========================================*/
    public void appendFromInputStream(BufferedReader pBufferedReader, 
                                      IFileIncludeHandler pFileIncludeHandler, 
                                      Script pScript) throws InvalidInputException, IOException
    {
        try
        {
            processMarkupLanguage(pBufferedReader, pScript);
        }
        catch(IllegalArgumentException e)
        {
            throw new InvalidInputException(e.toString(), e);
        }
        catch(ModelInstanceImporterException e)
        {
            throw new InvalidInputException(e.toString(), e);
        }        
    }

    public String getFileRegex()
    {
        return(".*\\.xml$");
    }
}
