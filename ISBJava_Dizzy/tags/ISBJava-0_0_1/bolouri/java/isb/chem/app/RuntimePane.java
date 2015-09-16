package isb.chem.app;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class RuntimePane
{
    private Component mMainFrame;
    private ModelNamesList mModelNamesListBox;
    private SpeciesPopulationNamesList mSpeciesPopulationNamesListBox;
    private JButton mClearRuntimeButton;

    private static final int DEFAULT_LIST_BOX_CELL_WIDTH = 200;
    private static final int DEFAULT_LIST_BOX_NUM_ROWS = 4;

    public RuntimePane(Container pPane)
    {
        initialize(pPane);
        mMainFrame = MainApp.getApp().getMainFrame();
    }


    private void initializeSpeciesPopulationNamesList(Container pPane)
    {
        SpeciesPopulationNamesList speciesPopulationNamesList = new SpeciesPopulationNamesList(pPane,
                                                                                               DEFAULT_LIST_BOX_NUM_ROWS,
                                                                                               DEFAULT_LIST_BOX_CELL_WIDTH);
        mSpeciesPopulationNamesListBox = speciesPopulationNamesList;
    }

    private void initializeModelNamesList(Container pPane)
    {
        ModelNamesList modelNamesList = new ModelNamesList(pPane,
                                                           DEFAULT_LIST_BOX_NUM_ROWS,
                                                           DEFAULT_LIST_BOX_CELL_WIDTH);
        mModelNamesListBox = modelNamesList;
    }

    private void initializeClearRuntimeButton(Container pPane)
    {
        JPanel buttonPane = new JPanel();
        JButton clearButton = new JButton("clear runtime variables");
        buttonPane.add(clearButton);
        buttonPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        mClearRuntimeButton = clearButton;

        clearButton.addActionListener( new ActionListener()
                                      {
                                          public void actionPerformed(ActionEvent e)
                                          {
                                              MainApp.getApp().clearRuntime();
                                          }
                                      });
        pPane.add(buttonPane);
    }

    String getSelectedModelName()
    {
        return(mModelNamesListBox.getSelectedModelName());
    }

    String getSelectedSpeciesPopulationName()
    {
        return(mSpeciesPopulationNamesListBox.getSelectedSpeciesPopulationName());
    }

    void updateMainPanelFromRuntime()
    {
        MainApp theApp = MainApp.getApp();

        int numModels = mModelNamesListBox.updateModelNames();
        int numSpeciesPops = mSpeciesPopulationNamesListBox.updateSpeciesPopulationNames();
        if(numModels > 0 && numSpeciesPops > 0)
        {
            theApp.enableExportMenuItem(true);
            if(null == theApp.getSimulator())
            {
                theApp.enableSimulateMenuItem(true);
            }
            else
            {
                theApp.enableSimulateMenuItem(false);
            }
        }
        else
        {
            theApp.enableExportMenuItem(false);
            theApp.enableSimulateMenuItem(false);
        }
    }

    public void initialize(Container pPane)
    {
        JPanel runtimePanel = new JPanel();
        runtimePanel.setBorder(BorderFactory.createEtchedBorder());
        LayoutManager layoutManager = new BoxLayout(runtimePanel, BoxLayout.Y_AXIS);
        runtimePanel.setLayout(layoutManager);

        JPanel listBoxesPanel = new JPanel();
        listBoxesPanel.setLayout(new FlowLayout());
        runtimePanel.add(listBoxesPanel);

        initializeModelNamesList(listBoxesPanel);
        initializeSpeciesPopulationNamesList(listBoxesPanel);

        initializeClearRuntimeButton(runtimePanel);

        pPane.add(runtimePanel);
    }

    void setEnableClearRuntime(boolean pEnable)
    {
        mClearRuntimeButton.setEnabled(pEnable);
    }
}
