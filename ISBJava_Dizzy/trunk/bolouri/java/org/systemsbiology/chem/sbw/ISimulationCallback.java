package org.systemsbiology.chem.sbw;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import edu.caltech.sbw.*;

interface ISimulationCallback
{
    void onError(String pErrorMessage) throws SBWException;
    void onRowData(double []pData) throws SBWException;
}
