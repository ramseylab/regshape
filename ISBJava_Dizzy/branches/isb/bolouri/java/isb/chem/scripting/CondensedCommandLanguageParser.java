package isb.chem.scripting;

/**
 * Can parse input based on a simplified command language.  Uses
 * the {@link CondensedCommandLanguagePreprocessor} to preprocess
 * the condensed command language notation into &quot;normal&quot; 
 * command language statements.
 * For a description of this simplified command language, please
 * refer to the {@link CondensedCommandLanguagePreprocessor}.
 *
 * @see CondensedCommandLanguagePreprocessor
 * @see Script
 *
 * @author Stephen Ramsey
 */
public class CondensedCommandLanguageParser extends CommandLanguageParser
{
    /*========================================*
     * constants
     *========================================*/
    public static final String CLASS_ALIAS = "condensed-command-language";
    private static final String DEFAULT_MODEL_NAME = "myModel";
    private static final String DEFAULT_SPECIES_POPULATIONS_NAME = "sp";
    private static final String DEFAULT_COMPARTMENT_NAME = "univ";

    public CondensedCommandLanguageParser()
    {
        super();
        ITranslator preprocessor = new CondensedCommandLanguagePreprocessor(DEFAULT_MODEL_NAME,
                                                                            DEFAULT_COMPARTMENT_NAME,
                                                                            DEFAULT_SPECIES_POPULATIONS_NAME);
        setPreprocessor(preprocessor);
    }

    public String getFileRegex()
    {
        return(".*\\.dizzy$");
    }
}
