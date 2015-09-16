/*
 * Copyright (C) 2004 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.chem;

/**
 * @author sramsey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IModelViewer
{
    public void viewModel(Model pModel, String pAppName) throws ModelViewerException;
}
