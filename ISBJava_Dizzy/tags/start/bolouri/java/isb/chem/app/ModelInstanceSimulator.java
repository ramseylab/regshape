package isb.chem.app;

import isb.chem.*;
import isb.chem.scripting.*;
import isb.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ModelInstanceSimulator
{
    private Component mMainFrame;

    public ModelInstanceSimulator(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    // returns true if simulation launcher frame loaded, false if it did not successfully load
    public boolean simulateModelInstance(String pModelName, 
                                         String pSpeciesPopulationName)
    {
        boolean success = false;

        MainApp app = MainApp.getApp();
        if(null != pModelName && null != pSpeciesPopulationName)
        {
            ScriptRuntime scriptRuntime = app.getScriptRuntime();
            Model model = null;
            SpeciesPopulations speciesPopulations = null;
            try
            {
                model = scriptRuntime.getModel(pModelName);
                speciesPopulations = scriptRuntime.getSpeciesPopulations(pSpeciesPopulationName);
            }
            catch(DataNotFoundException e)
            {
                String message = null;
                if(null != model)
                {
                    message = "unable to find information about species populations set: " + pSpeciesPopulationName;
                }
                else
                {
                    message = "unable to find information about model: " + pModelName;
                }
                UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(mMainFrame, message + pSpeciesPopulationName);
                dialog.show();
                return(success);
            }

            if(model.getSpeciesSetCopy().size() > 0)
            {
                SimulationLauncher simController = new SimulationLauncher(mMainFrame,
                                                                          model,
                                                                          speciesPopulations);
                success = true;
            }
            else
            {
                SimpleDialog dialog = new SimpleDialog(mMainFrame, "Insufficient number of species", "The model you selected, " + pModelName + ", has no species.  This operation is cancelled.");
                dialog.show();
            }
        }
        else
        {
            SimpleTextArea textArea = new SimpleTextArea("Both a model and a species populations set must be selected, in order to simulate a model instance.  Please select a model and a species populations set, and try again.  This operation is cancelled.");
            
            SimpleDialog messageDialog = new SimpleDialog(mMainFrame, 
                                                          "Simulation operation cancelled", 
                                                          textArea);
            messageDialog.setMessageType(JOptionPane.WARNING_MESSAGE);
            messageDialog.show();
        }
        return(success);
    }
    
}
