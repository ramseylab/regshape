package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import isb.chem.*;
import isb.chem.scripting.*;
import isb.util.*;
import java.util.*;

public class ModelNamesList extends NamesList
{
    Component mContainingComponent;

    public ModelNamesList(Container pPanel, int pVisibleRowCount, int pDefaultCellWidth)
    {
        super(pPanel, "model names: ", pVisibleRowCount, pDefaultCellWidth);
    }

    protected void handleDoubleClick(int index)
    {
        String modelName = (String) getModel().getElementAt(index);
        Model model = null;
        MainApp app = MainApp.getApp();
        try
        {
            model = app.getScriptRuntime().getModel(modelName);
        }
        catch(DataNotFoundException e)
        {
            UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(mContainingComponent, "unable to find information about model: " + modelName);
            dialog.show();
            return;
        }
        String modelPrintout = model.toString();
        app.appendToOutputLog(modelPrintout);
    }

    String getSelectedModelName()
    {
        int index = getSelectedIndex();
        String modelName = null;
        if(-1 != index)
        {
            modelName = (String) getModel().getElementAt(index);
        }
        return(modelName);
    }

    int updateModelNames()
    {
        int numNames = 0;
        MainApp app = MainApp.getApp();
        ScriptRuntime scriptRuntime = app.getScriptRuntime();
        Set modelNames = scriptRuntime.getModelNamesCopy();
        java.util.List modelNamesList = new LinkedList(modelNames);
        Collections.sort(modelNamesList);
        Iterator modelIter = modelNamesList.iterator();
        Object []modelNamesArray = modelNamesList.toArray();
        setListData(modelNamesArray);
        numNames = modelNamesArray.length;
        return(numNames);
    }


}
