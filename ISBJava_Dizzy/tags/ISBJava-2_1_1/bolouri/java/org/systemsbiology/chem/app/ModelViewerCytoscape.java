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
import org.systemsbiology.chem.sbml.*;
import org.systemsbiology.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

import netx.jnlp.*;
import netx.jnlp.runtime.*;

public class ModelViewerCytoscape implements IModelViewer, IAliasableClass
{
    public static final String CLASS_ALIAS = "cytoscape";
    private static final String JNLP_TEMPLATE_FILE_NAME = "CytoscapeViewer.xml";
    private static final String TAG_NAME_APPLICATION_DESC = "application-desc";

    public ModelViewerCytoscape()
    {
        // do nothing
    }

    static
    {
        JNLPRuntime.setSecurityEnabled(false);
        JNLPRuntime.initialize();
    }

    public void viewModel(Model pModel, String pAppName) throws ModelViewerException
    {
        try
        {
            ModelExporterMarkupLanguage exporterMarkupLanguage = new ModelExporterMarkupLanguage();
            File tempFile = File.createTempFile("sbml", ".xml");
            String tempFileName = tempFile.getAbsolutePath();
            FileWriter fileWriter = new FileWriter(tempFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            // convert the model to SBML level 1, version 1 (eventually we will modify Cytoscape's SBML reader
            // so that it can read SBML level 1, version 2-- then we will be able to export L1V2 to Cytoscape).
            exporterMarkupLanguage.export(pModel, printWriter, ModelExporterMarkupLanguage.Specification.LEVEL1_VERSION1);
            URL jnlpFileResource = this.getClass().getResource(JNLP_TEMPLATE_FILE_NAME);
            if(null == jnlpFileResource)
            {
                throw new DataNotFoundException("could not find resource file: " + JNLP_TEMPLATE_FILE_NAME);
            }
            InputStream jnlpFileInputStream = jnlpFileResource.openStream();
            
            File tempJNLPFile = File.createTempFile("cytoscpe", ".jnlp");
            FileWriter tempJNLPFileWriter = new FileWriter(tempJNLPFile);
            PrintWriter tempJNLPPrintWriter = new PrintWriter(tempJNLPFileWriter);
            String tempJNLPFileURLString = FileUtils.createFileURL(tempJNLPFile);
            URL tempJNLPFileURL = new URL(tempJNLPFileURLString);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(jnlpFileInputStream);

            Element jnlpElement = document.getDocumentElement();
            jnlpElement.setAttribute("href", tempJNLPFileURLString);

            NodeList applicationDescElements = document.getElementsByTagName(TAG_NAME_APPLICATION_DESC);
            Element applicationDescElement = (Element) applicationDescElements.item(0);
            Element argumentElement = document.createElement("argument");
            applicationDescElement.appendChild(argumentElement);
            Text jnlpFileNameTextNode = document.createTextNode(tempFileName);
            argumentElement.appendChild(jnlpFileNameTextNode);
            
            // write out the XML 
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();   
            transformer.setOutputProperty("indent", "yes");
            Properties properties = transformer.getOutputProperties();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(tempJNLPPrintWriter);
            transformer.transform(source, result);
            tempJNLPPrintWriter.flush();

//=========================================================================
// This code is used in conjunction with Sun's Java Web Start:
//            BrowserLauncher.openURL(tempJNLPFileURLString);
//=========================================================================

//=========================================================================
// This code is used in conjunction with the OpenJNLP API (which seems to be
// incompatible with our JNLP file):
//            FileCache fileCache = FileCache.defaultCache();
//            JNLPParser.launchJNLP(fileCache, tempJNLPFileURL, true);
//=========================================================================

//=========================================================================
// This code is used in conjunction with the NetX JNLP API:
            Launcher launcher = new Launcher();
            launcher.launch(tempJNLPFileURL);
//=========================================================================
        }
        catch(Exception e)
        {
            throw new ModelViewerException("View-in-Cytoscape operation failed", e);
        }
    }
}
