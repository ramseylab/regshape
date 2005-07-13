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
package org.systemsbiology.chem.sbw;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.systemsbiology.gui.*;
import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.IncludeHandler;
import org.systemsbiology.util.InvalidInputException;
import org.systemsbiology.chem.*;
import org.systemsbiology.chem.app.*;
import org.systemsbiology.chem.sbml.*;

/**
 * @author sramsey
 *
 */
public class SimulationDriverService implements ISimulationDriverService
{
    public static final String SERVICE_NAME = "asim";
    public static final String SERVICE_DESCRIPTION = "Simulation Service";
    private Model mModel;
    private SimulationLauncher mSimulationLauncher;
    
    public SimulationDriverService()
    {
        mModel = null;
        mSimulationLauncher = null;
    }
    
    private Model loadModel(String pModelDescriptionText,
                           IModelBuilder pModelBuilder) throws IOException, InvalidInputException, DataNotFoundException
    {
        byte []modelDescriptionBytes = pModelDescriptionText.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(modelDescriptionBytes);
        IncludeHandler fileIncludeHandler = null;  // file inclusion not allowed, for security reasons
        Model model = pModelBuilder.buildModel(inputStream, fileIncludeHandler);
        return model;
    }
    
    private void createSimulationLauncher(Model pModel) throws InstantiationException, IOException, ClassNotFoundException
    {
        boolean handleOutputInternally = true;
        String appName = SimulationDriverModule.getConfig().getAppName();
        SimulationLauncher launcher = new SimulationLauncher(appName,
                                                             pModel,
                                                             handleOutputInternally);  
        launcher.setModel(pModel);
        mSimulationLauncher = launcher;        
    }
    
    public void doAnalysis(String pModelDescriptionText)
    {
        try
        {
            ModelBuilderMarkupLanguage modelBuilder = new ModelBuilderMarkupLanguage();
            Model model = loadModel(pModelDescriptionText, modelBuilder);    
            createSimulationLauncher(model);
        }
        
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e, e.getMessage());
            optionPane.createDialog(null, "Error in simulation driver").show();            
        }
    }
    
    public void doAnalysisCMDL(String pModelDescriptionText)
    {
        try
        {
            ModelBuilderCommandLanguage modelBuilder = new ModelBuilderCommandLanguage();
            Model model = loadModel(pModelDescriptionText, modelBuilder);    
            createSimulationLauncher(model);
        }
        
        catch(Exception e)
        {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e, e.getMessage());
            optionPane.createDialog(null, "Error in simulation driver").show();            
        }        
    }    
}
