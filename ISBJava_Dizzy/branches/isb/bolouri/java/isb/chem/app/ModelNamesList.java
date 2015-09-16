package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import isb.chem.*;
import isb.chem.scripting.*;
import isb.util.*;
import java.util.*;

public class ModelNamesList extends JList
{
    Component mContainingComponent;

    public ModelNamesList(Container pPanel)
    {
        super();
        mContainingComponent = pPanel;
        JPanel listPane = new JPanel();
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        listPane.setBorder(BorderFactory.createEtchedBorder());

        MouseListener mouseListener = new MouseAdapter()
        {
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2)
                {
                    int index = locationToIndex(event.getPoint());
                    if(-1 != index)
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
                        app.getOutputTextArea().append(modelPrintout);
                    }
                }
            }
        };
        addMouseListener(mouseListener);
        setVisibleRowCount(4);

        JScrollPane listBoxPanel = new JScrollPane(this);

        JLabel label = new JLabel("models:");
        listPane.add(label);
        listPane.add(listBoxPanel);
        pPanel.add(listPane);
        
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
