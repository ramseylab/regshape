/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.gui;

import javax.swing.table.AbstractTableModel;

/**
 * @author sramsey
 *
 */
public class EmptyTableModel extends AbstractTableModel
{
    public int getColumnCount()
    {
        return 0;
    }
    
    public int getRowCount()
    {
        return 0;
    }
    
    public Object getValueAt(int pRow, int pColumn)
    {
        return null;
    }        

}
