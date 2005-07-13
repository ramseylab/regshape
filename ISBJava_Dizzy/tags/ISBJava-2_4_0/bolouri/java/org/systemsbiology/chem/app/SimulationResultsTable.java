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
import javax.swing.table.*;
import java.text.*;

/**
 * Displays simulation results in a JTable
 */
public class SimulationResultsTable extends JFrame
{
    private static final String COLUMN_NAME_TIME = "time";
    private String mAppName;
    private String mLabel;
    private SimulationResultsTableModel mSimulationResultsTableModel;

    static class SimulationResultsTableModel extends AbstractTableModel
    {
        private SimulationResults mSimulationResults;
        private NumberFormat mNumberFormat;

        public SimulationResultsTableModel(SimulationResults pSimulationResults,
                                           NumberFormat pNumberFormat)
        {
            mSimulationResults = pSimulationResults;
            mNumberFormat = pNumberFormat;
        }

        public int getColumnCount()
        {
            return((mSimulationResults.getResultsSymbolNames().length) + 1);
        }

        public int getRowCount()
        {
            return(mSimulationResults.getResultsTimeValues().length);
        }

        public String getColumnName(int pColNumber)
        {
            String name = null;
            if(pColNumber == 0)
            {
                name = COLUMN_NAME_TIME;
            }
            else
            {
                name = mSimulationResults.getResultsSymbolNames()[pColNumber - 1];
            }
            return(name);
        }

        public Object getValueAt(int pRowNumber, int pColNumber)
        {
            String stringValue = null;
            double value = 0.0;
            if(pColNumber == 0)
            {
                value = mSimulationResults.getResultsTimeValues()[pRowNumber];
            }
            else
            {
                Object []matrix = mSimulationResults.getResultsSymbolValues();
                double []row = (double []) matrix[pRowNumber];
                value = row[pColNumber - 1];
            }
            stringValue = mNumberFormat.format(value);
            return(stringValue);
        }        
    }

    public SimulationResultsTable(SimulationResults pSimulationResults, 
                                  String pAppName,
                                  String pLabel,
                                  NumberFormat pNumberFormat)
    {
        super(pAppName + ": results");

        mSimulationResultsTableModel = new SimulationResultsTableModel(pSimulationResults,
                                                                       pNumberFormat);

        JPanel panel = new JPanel(); 
        LayoutManager layoutManager = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layoutManager);

        JLabel tableLabel = new JLabel(pLabel);
        tableLabel.setAlignmentX(Container.CENTER_ALIGNMENT);
        panel.add(tableLabel);

        JTable table = new JTable(mSimulationResultsTableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane);
        setContentPane(panel);
        pack();
    }

}
