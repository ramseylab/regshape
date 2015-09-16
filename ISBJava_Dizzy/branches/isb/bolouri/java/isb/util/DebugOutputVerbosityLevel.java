package isb.util;

import java.util.*;

/**
 * Enumeration class for debugging verbosity levels.
 * Used by the {@link isb.chem.ISimulator} framework.
 *
 * @see isb.chem.ISimulator
 *
 * @author Stephen Ramsey
 */
public class DebugOutputVerbosityLevel
{
    private final String mName;
    private final int mLevel;
    private static HashMap mMap;

    static
    {
        mMap = new HashMap();
    }

    private DebugOutputVerbosityLevel(String pName, int pLevel)
    {
        mName = pName;
        mLevel = pLevel;
        if(null != get(pLevel))
        {
            throw new IllegalArgumentException("debug level defined more than once; level code is: " + pLevel);
        }
        mMap.put(new Integer(pLevel), this);
    }

    public String toString()
    {
        return(mName);
    }

    public static DebugOutputVerbosityLevel get(int pLevel)
    {
        return((DebugOutputVerbosityLevel) mMap.get(new Integer(pLevel)));
    }

    public boolean greaterThan(DebugOutputVerbosityLevel pDebugOutputVerbosityLevel)
    {
        return(mLevel > pDebugOutputVerbosityLevel.mLevel);
    }

    public static final DebugOutputVerbosityLevel NONE = new DebugOutputVerbosityLevel("none", 0);
    public static final DebugOutputVerbosityLevel LOW = new DebugOutputVerbosityLevel("low", 1);
    public static final DebugOutputVerbosityLevel MEDIUM = new DebugOutputVerbosityLevel("medium", 2);
    public static final DebugOutputVerbosityLevel HIGH = new DebugOutputVerbosityLevel("high", 3);
    public static final DebugOutputVerbosityLevel EXTREME = new DebugOutputVerbosityLevel("extreme", 4);


}
