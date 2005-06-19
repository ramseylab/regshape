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
 * Represents an object that can display a {@link Model} in graphical
 * or textual format, on the screen.  All subclasses of this
 * interface should implement {@link org.systemsbiology.util.IAliasableClass},
 * and have the public static string field <code>CLASS_ALIAS</code>.
 *
 * @author sramsey
 *
 */
public interface IModelViewer
{
    public void viewModel(Model pModel, String pAppName) throws ModelViewerException;
}
