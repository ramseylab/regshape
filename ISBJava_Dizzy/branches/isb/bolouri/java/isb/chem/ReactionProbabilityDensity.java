package isb.chem;

import java.util.*;
import isb.util.*;

/**
 * This class contains a {@link Reaction} object and a floating-point 
 * probability densities (per unit time) for that particular reaction.
 * The dimensions of this probability density is inverse time, in the
 * same units as the reaction parameter defined for a <code>Reaction</code> 
 * object using the {@link Reaction#setReactionParameter(double)} method.
 * <p />
 * This class is normally used only by the {@link Gillespie} simulator,
 * and does not normally need to be accessed by the application program.
 * For more information on the <code>Gillespie</code> simulator, refer to 
 * the {@link Gillespie} class documentation.
 * 
 * @see Gillespie
 * @see Reaction
 *
 * @author Stephen Ramsey
 */
class ReactionProbabilityDensity
{
    /*========================================*
     * constants
     *========================================*/
    private static final double EMPTY_REACTION_PROBABILITY_DENSITY = -1.0;

    /*========================================*
     * member data
     *========================================*/
    private Reaction mReaction;
    private double mProbabilityDensity;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setReaction(Reaction pReaction)
    {
        mReaction = pReaction;
    }

    Reaction getReaction()
    {
        return(mReaction);
    }

    void setProbabilityDensity(double pProbabilityDensity) throws IllegalArgumentException
    {
        if(pProbabilityDensity < 0.0)
        {
            throw new IllegalArgumentException("invalid probability density");
        }
        mProbabilityDensity = pProbabilityDensity;
    }

    double getProbabilityDensity()
    {
        return(mProbabilityDensity);
    }
    
    /*========================================*
     * initialization methods
     *========================================*/
    
    /*========================================*
     * constructors
     *========================================*/
    ReactionProbabilityDensity(Reaction pReaction)
    {
        setReaction(pReaction);
        mProbabilityDensity = EMPTY_REACTION_PROBABILITY_DENSITY;
    }

    /*========================================*
     * private methods
     *========================================*/

    /*========================================*
     * protected methods
     *========================================*/

    /*========================================*
     * public methods
     *========================================*/
}
