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

import javax.swing.*;
import org.systemsbiology.util.*;
import org.systemsbiology.gui.*;
import java.awt.*;

public class ModelViewerHumanReadable implements IModelViewer, IAliasableClass
{
    public static final String CLASS_ALIAS = "human-readable";
    public static final int WIDTH = 500;
    public static final int HEIGHT = 600;
    
    public ModelViewerHumanReadable()
    {
        // do nothing
    }

    public void viewModel(Model pModel, String pAppName) throws ModelViewerException
    {
        String modelName = pModel.getName();
        try
        {
            JLabel modelNameLabel = new JLabel("model: " + pModel.getName());
            modelNameLabel.setFont(modelNameLabel.getFont().deriveFont(Font.PLAIN));
            
            JTextArea modelTextArea = new JTextArea(40, 80);
            modelTextArea.setEditable(false);
            modelTextArea.append(pModel.toString());
            modelTextArea.setCaretPosition(0);
            JScrollPane scrollPane = new JScrollPane(modelTextArea);
            JPanel modelTextAreaPanel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints constraints = new GridBagConstraints();
            
            modelTextAreaPanel.add(modelNameLabel);
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.gridx = 0;
            constraints.gridy = 0;     
            layout.setConstraints(modelNameLabel, constraints);
            
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.gridheight = GridBagConstraints.REMAINDER;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.gridx = 0;
            constraints.gridy = 1;             
            modelTextAreaPanel.setLayout(layout);
            modelTextAreaPanel.add(scrollPane);
            layout.setConstraints(scrollPane, constraints);
            JDialog dialog = new JDialog();
            dialog.setSize(WIDTH, HEIGHT);
            Point location = FramePlacer.placeInCenterOfScreen(WIDTH, HEIGHT);
            dialog.setLocation(location);
            dialog.setTitle(pAppName + ": model description");
            dialog.setContentPane(modelTextAreaPanel);
            dialog.show();
        }

        catch(Exception e)
        {
            throw new ModelViewerException("unable to view model", e);
        }
    }
}
