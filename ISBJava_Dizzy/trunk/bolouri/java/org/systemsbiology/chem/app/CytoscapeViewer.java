package org.systemsbiology.chem.app;

import org.systemsbiology.chem.*;
import org.systemsbiology.chem.scripting.*;
import org.systemsbiology.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

//import org.nanode.jnlp.*;
//import org.nanode.launcher.cache.*;

import netx.jnlp.*;
import netx.jnlp.runtime.*;

public class CytoscapeViewer
{
    private static final String JNLP_TEMPLATE_FILE_NAME = "CytoscapeViewer.xml";
    private static final String TAG_NAME_APPLICATION_DESC = "application-desc";

    static
    {
        JNLPRuntime.setSecurityEnabled(false);
        JNLPRuntime.initialize();
    }

    public void viewModelInCytoscape(Model pModel) throws IllegalArgumentException, DataNotFoundException, IllegalStateException, UnsupportedOperationException, ModelExporterException, IOException
    {
        ModelExporterMarkupLanguage exporterMarkupLanguage = new ModelExporterMarkupLanguage();
        File tempFile = File.createTempFile("sbml", ".xml");
        String tempFileName = tempFile.getAbsolutePath();
        FileWriter fileWriter = new FileWriter(tempFile);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        exporterMarkupLanguage.export(pModel, printWriter);
        URL jnlpFileResource = this.getClass().getResource(JNLP_TEMPLATE_FILE_NAME);
        if(null == jnlpFileResource)
        {
            throw new DataNotFoundException("could not find resource file: " + JNLP_TEMPLATE_FILE_NAME);
        }
        InputStream jnlpFileInputStream = jnlpFileResource.openStream();

        File tempJNLPFile = File.createTempFile("cytoscpe", ".jnlp");
        FileWriter tempJNLPFileWriter = new FileWriter(tempJNLPFile);
        PrintWriter tempJNLPPrintWriter = new PrintWriter(tempJNLPFileWriter);
        String tempJNLPFileURLString = URLUtils.createFileURL(tempJNLPFile);
        URL tempJNLPFileURL = new URL(tempJNLPFileURLString);

        try
        {
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
            throw new ModelExporterException("unable to export model to Cytoscape: " + e.toString(), e);
        }
    }
}
