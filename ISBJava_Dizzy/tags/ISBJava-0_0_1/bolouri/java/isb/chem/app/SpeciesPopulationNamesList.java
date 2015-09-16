package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import isb.chem.*;
import isb.chem.scripting.*;
import isb.util.*;

public class SpeciesPopulationNamesList extends NamesList
{
    Component mContainingComponent;

    public SpeciesPopulationNamesList(Container pPanel, int pVisibleRowCount, int pDefaultCellWidth)
    {
        super(pPanel, "species populations: ", pVisibleRowCount, pDefaultCellWidth);
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

    protected void handleDoubleClick(int index)
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
        app.appendToOutputLog(speciesPopulationPrintout);
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
