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

/*
 * Abstract class defining the interface for a class that
 * can modify the result of the evaluation of a symbol.
 *
 * @author Stephen Ramsey
 */
public abstract class SymbolEvaluationPostProcessor implements Cloneable
{
    public abstract double modifyResult(Symbol pSymbol,
                                        SymbolEvaluator pSymbolEvaluator,
                                        double pSymbolValue) throws DataNotFoundException;

    public abstract Object clone();
}
