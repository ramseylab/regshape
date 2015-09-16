package isb.chem.sbw;

import isb.chem.*;

/**
 * SBML parser that assumes initial species populations are given in
 * molecules, rather than moles.  This is a common misuse of the
 * SBML standard, so we have a specific parser for this case.
 *
 * @author Stephen Ramsey
 */
public class MarkupLanguageParserMolecules extends MarkupLanguageParser
{
    public static final String CLASS_ALIAS = "markup-language-molecules";

    public MarkupLanguageParserMolecules()
    {
        super(ReactionRateSpeciesMode.MOLECULES);
    }
}
