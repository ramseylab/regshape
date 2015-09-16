package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import isb.chem.*;
import isb.chem.scripting.*;
import isb.util.*;

public class SpeciesPopulationNamesList extends JList
{
    Component mContainingComponent;

    public SpeciesPopulationNamesList(Container pPanel)
    {
        super();
        mContainingComponent = pPanel;
        JPanel listPane = new JPanel();
        listPane.setBorder(BorderFactory.createEtchedBorder());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        MouseListener mouseListener = new MouseAdapter()
        {
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2)
                {
                    int index = locationToIndex(event.getPoint());
                    if(-1 != index)
                    {
                        String speciesPopulationName = (String) getModel().getElementAt(index);
                        SpeciesPopulations speciesPopulations = null;
                        MainApp app = MainApp.getApp();
                        try
                        {
                            speciesPopulations = app.getScriptRuntime().getSpeciesPopulations(speciesPopulationName);
                        }
                        catch(DataNotFoundException e)
                        {
                            UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(mContainingComponent, "unable to find information about species populations: " + speciesPopulationName);
                            dialog.show();
                            return;
                        }
                        String speciesPopulationPrintout = speciesPopulations.toString();
                        app.getOutputTextArea().append(speciesPopulationPrintout);
                    }
                }
            }
        };
        addMouseListener(mouseListener);
        setVisibleRowCount(4);

        JScrollPane listBoxPanel = new JScrollPane(this);

        JLabel label = new JLabel("species population sets:");
        listPane.add(label);
        listPane.add(listBoxPanel);
        pPanel.add(listPane);
        
    }

    int updateSpeciesPopulationNames()
    {
        int numNames = 0;
        MainApp app = MainApp.getApp();
        ScriptRuntime scriptRuntime = app.getScriptRuntime();
        Set speciesPopulationNames = scriptRuntime.getSpeciesPopulationsNamesCopy();
        java.util.List speciesPopulationNamesList = new LinkedList(speciesPopulationNames);
        Collections.sort(speciesPopulationNamesList);
        Object []speciesPopulationNamesArray = speciesPopulationNamesList.toArray();
        setListData(speciesPopulationNamesArray);
        numNames = speciesPopulationNamesArray.length;
        return(numNames);
    }


    String getSelectedSpeciesPopulationName()
    {
        int index = getSelectedIndex();
        String speciesPopulationName = null;
        if(-1 != index)
        {
            speciesPopulationName = (String) getModel().getElementAt(index);
        }
        return(speciesPopulationName);
    }
}
