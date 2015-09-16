package isb.util;

import org.w3c.dom.*;
import org.xml.sax.*;
import java.util.jar.*;
import java.io.*;
import javax.xml.parsers.*;
import java.net.*;

/**
 * Utility class for reading an XML configuration file
 * for an application.  Provides standard methods for
 * obtaining the application name, version, release date,
 * and home page.  This class can be subclassed to
 * add new properties.
 * 
 * @author Stephen Ramsey
 */
public class AppConfig
{
    private Document mConfigFileDocument;
    private String mSourceURI;

    private static final String PROPERTY_NAME_APP_NAME = "appName";
    private static final String PROPERTY_NAME_APP_DATE = "appDate";
    private static final String PROPERTY_NAME_APP_VERSION = "appVersion";
    private static final String PROPERTY_NAME_APP_HOME_PAGE = "appHomePage";
    public static final String CONFIG_FILE_NAME="AppConfig.xml";

    /**
     * Creates an AppConfig using the config file <code>pConfigFile</code>.
     */
    public AppConfig(File pConfigFile) throws InvalidInputException, DataNotFoundException, FileNotFoundException
    {
        this(getConfigSource(pConfigFile));
    }

    /**
     * Creates an AppConfig using class <code>pAppClass</code> to 
     * search for the config file resource.  That class must have
     * a resource file <code>AppConfig.xml</code>.
     */
    private AppConfig(InputSource pInputSource) throws InvalidInputException, DataNotFoundException
    {
        try
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(pInputSource);
            mConfigFileDocument = document;
        }
        catch(Exception e)
        {
            throw new InvalidInputException("unable to load XML configuration file", e);
        }
    }

    /**
     * Creates an AppConfig using class <code>pAppClass</code> to 
     * search for the config file resource.  That class must have
     * a resource file <code>AppConfig.xml</code>.
     */
    public AppConfig(Class pAppClass) throws InvalidInputException, DataNotFoundException
    {
        this(getConfigSource(pAppClass));
    }

    private static InputSource getConfigSource(File pAppFile) throws FileNotFoundException
    {
        InputSource retSource = null;
        String fileDirName = "file://" + pAppFile.getAbsolutePath();
        FileInputStream fileInputStream = new FileInputStream(pAppFile);
        retSource = new InputSource(fileInputStream);
        retSource.setSystemId(fileDirName);
        return(retSource);
    }

    private static InputSource getConfigSource(Class pAppClass) throws DataNotFoundException
    {
        URL resource = pAppClass.getResource(CONFIG_FILE_NAME);
        String resourceFileName = resource.toExternalForm();
        File resourceFile = new File(resourceFileName);
        InputStream configResourceStream = pAppClass.getResourceAsStream(CONFIG_FILE_NAME);
        if(null == configResourceStream)
        {
            throw new DataNotFoundException("unable to find XML configuration file resource: " + CONFIG_FILE_NAME + " for class: " + pAppClass.getName());
        }
        InputSource inputSource = new InputSource(configResourceStream);
        if(! resourceFile.exists())
        {
            inputSource.setSystemId(resourceFileName);
        }
        return(inputSource);
    }

    protected String getProperty(String pPropertyName)
    {
        String propertyValue = null;
        NodeList nodeList = mConfigFileDocument.getElementsByTagName(pPropertyName);
        int nodeListLength = nodeList.getLength();
        if(nodeListLength > 0)
        {
            Node firstChildNode = nodeList.item(nodeListLength - 1).getFirstChild();
            if(null != firstChildNode)
            {
                propertyValue = firstChildNode.getNodeValue();
            }
        }
        return(propertyValue);
    }

    public String getAppHomePage()
    {
        return(getProperty(PROPERTY_NAME_APP_HOME_PAGE));
    }

    public String getAppName()
    {
        return(getProperty(PROPERTY_NAME_APP_NAME));
    }

    public String getAppVersion()
    {
        return(getProperty(PROPERTY_NAME_APP_VERSION));
    }

    public String getAppDate()
    {
        return(getProperty(PROPERTY_NAME_APP_DATE));
    }

    public static void main(String []pArgs)
    {
        try
        {
            AppConfig appConfig = new AppConfig(new File(pArgs[0]));
            System.out.println("version: " + appConfig.getAppVersion());
            System.out.println("appname: " + appConfig.getAppName());
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
