package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.DataNotFoundException;

/**
 * Defines an interface for a class that can look up a value
 * (which is of type double) from a symbol name (which is of type string).
 *
 * @author Stephen Ramsey
 */
public interface ISymbolValueMap
{
    /**
     * Returns the {@link Value} associated with the specified
     * symbol name.
     * 
     * @param pSymbol the {@link Symbol} object for which the value is to be 
     * returned
     *
     * @return the {@link Value} associated with the specified
     * symbol.
     */
    public double getValue(Symbol pSymbol) throws DataNotFoundException;
}
