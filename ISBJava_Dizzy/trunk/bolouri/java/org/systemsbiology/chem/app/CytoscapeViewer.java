package org.systemsbiology.chem.app;

import org.systemsbiology.chem.*;
import org.systemsbiology.chem.scripting.*;
import org.systemsbiology.util.*;
import java.io.*;
import cytoscape.CytoscapeWindow;
import csplugins.util.SBMLCytoscapeWindow;

public class CytoscapeViewer
{
    public void viewModelInCytoscape(Model pModel) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException, IOException
    {
        ModelExporterMarkupLanguage exporterMarkupLanguage = new ModelExporterMarkupLanguage();
        File tempFile = File.createTempFile("sbml", ".xml");
        FileWriter fileWriter = new FileWriter(tempFile);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        exporterMarkupLanguage.export(pModel, printWriter);
        try
        {
            CytoscapeWindow cw =  SBMLCytoscapeWindow.create(tempFile.getAbsolutePath());
        }
        catch(Exception e)
        {
            throw new ModelExporterException("unable to export model to Cytoscape: " + e.toString(), e);
        }
    }
}
