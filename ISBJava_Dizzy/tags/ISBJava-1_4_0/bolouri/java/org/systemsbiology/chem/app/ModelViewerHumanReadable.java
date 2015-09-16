package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.chem.*;
import java.awt.*;
import javax.swing.*;

public class ModelViewerHumanReadable
{
    private Component mMainFrame;

    public ModelViewerHumanReadable(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    void handleViewModelHumanReadable(Model pModel)
    {
        String modelName = pModel.getName();
        try
        {
            JTextArea modelTextArea = new JTextArea(40, 80);
            modelTextArea.setEditable(false);
            modelTextArea.append(pModel.toString());
            modelTextArea.setCaretPosition(0);
            JScrollPane scrollPane = new JScrollPane(modelTextArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            JPanel modelTextAreaPane = new JPanel();
            modelTextAreaPane.add(scrollPane);
            JOptionPane optionPane = new JOptionPane(modelTextAreaPane);
            JDialog dialog = optionPane.createDialog(mMainFrame, "model description for: " + modelName);
            dialog.show();
        }

        catch(Exception e)
        {
            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(mMainFrame, "View-model operation failed: " + modelName, e);
            dialog.show();
            
        }
    }
}
