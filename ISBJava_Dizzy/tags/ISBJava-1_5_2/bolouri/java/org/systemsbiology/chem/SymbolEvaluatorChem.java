package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Abstract Symbol Evaluator class used for chemical simulations.
 * 
 * @author Stephen Ramsey
 */
public abstract class SymbolEvaluatorChem extends SymbolEvaluator
{
    protected HashMap mSymbolsMap;
    protected double mTime;
    protected HashMap mLocalSymbolsMap;

    public SymbolEvaluatorChem()
    {
        // do nothing
    }

    public void setSymbolsMap(HashMap pSymbolsMap) throws DataNotFoundException
    {
        mSymbolsMap = pSymbolsMap;
    }

    public void setLocalSymbolsMap(HashMap pLocalSymbolsMap)
    {
        mLocalSymbolsMap = pLocalSymbolsMap;
    }

    public final void setTime(double pTime)
    {
        mTime = pTime;
    }

    public final double getTime()
    {
        return(mTime);
    }

    public abstract boolean hasValue(Symbol pSymbol);


    public abstract double getUnindexedValue(Symbol pSymbol) throws DataNotFoundException, IllegalStateException;

}
