package isb.chem;

import java.util.HashMap;

/**
 * Represents an enumeration of possible modes for
 * interpreting a {@link Species} symbol that appears
 * in a custom rate expression for a {@link Reaction}.
 *
 * @author Stephen Ramsey
 */
public class ReactionRateSpeciesMode
{
    private final String mName;
    private static HashMap mMap;

    static
    {
        mMap = new HashMap();
    }

    private ReactionRateSpeciesMode(String pName) 
    {
        mName = pName;
        mMap.put(mName, this);
    }

    public static ReactionRateSpeciesMode get(String pName)
    {
        return((ReactionRateSpeciesMode) mMap.get(pName));
    }

    public String toString()
    {
        return(mName);
    }

    /**
     * Represents a mode of operation in which a species symbol occuring
     * in a custom rate expression is interpreted as a concentration,
     * in moles/liter.  This is the mode to be used with a rate expression
     * received from SBML.  This mode identifier should be passed to 
     * {@link Model#setReactionRateSpeciesMode(ReactionRateSpeciesMode)} 
     * in order to select this mode.
     */
    public static final ReactionRateSpeciesMode CONCENTRATION = new ReactionRateSpeciesMode("concentration");

    /**
     * Represents a mode of operation in which a species symbol occuring
     * in a custom rate expression is interpreted as the number of molecules
     * for that species.  This is the default mode of operation.  This mode 
     * identifier should be passed to 
     * {@link Model#setReactionRateSpeciesMode(ReactionRateSpeciesMode)} 
     * in order to select this mode.
     */
    public static final ReactionRateSpeciesMode MOLECULES = new ReactionRateSpeciesMode("molecules");
    
}

