package org.systemsbiology.chem.app;

import java.io.File;
import org.systemsbiology.chem.scripting.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.systemsbiology.util.*;

public class ParserPicker
{
    private static final String ALIAS_MARKUP_LANGUAGE = "markup-language";
    private static final String ALIAS_MARKUP_LANGUAGE_MOLECULES = "markup-language-molecules";

    private Component mMainFrame;

    public ParserPicker(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    private void handleCancel()
    {
        SimpleDialog messageDialog = new SimpleDialog(mMainFrame, "Model processing cancelled", 
                                                      "Your model processing has been cancelled");
        messageDialog.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        messageDialog.show();
    }

    private static java.util.List getModelBuilderAliasesList(ClassRegistry modelBuilderRegistry)
    {
        Set modelBuilderAliases = modelBuilderRegistry.getRegistryAliasesCopy();
        assert (modelBuilderAliases.size() > 0) : "no model builder aliases found";
        java.util.List modelBuilderAliasesList = new LinkedList(modelBuilderAliases);
        Collections.sort(modelBuilderAliasesList);

        return(modelBuilderAliasesList);
    }

    public static String processFileName(String pFileName)
    {
        String retModelBuilderAlias = null;

        MainApp app = MainApp.getApp();
        ClassRegistry modelBuilderRegistry = app.getModelBuilderRegistry();

        java.util.List modelBuilderAliasesList = getModelBuilderAliasesList(modelBuilderRegistry);
        Iterator modelBuilderAliasesIter = modelBuilderAliasesList.iterator();;

        while(modelBuilderAliasesIter.hasNext())
        {
            String modelBuilderAlias = (String) modelBuilderAliasesIter.next();
            IModelBuilder modelBuilder = null;
            try
            {
                modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(modelBuilderAlias);
            }
            catch(DataNotFoundException e)
            {
                e.printStackTrace(System.err);
            }
            assert (null != modelBuilder): "unexpected exception thrown in scriptBuilder.getModelBuilder()";
            String fileRegex = modelBuilder.getFileRegex();
            if(pFileName.matches(fileRegex))
            {
                retModelBuilderAlias = modelBuilderAlias;
                break;
            }
        }

        return(retModelBuilderAlias);
    }

    public String selectParserAliasFromFileName(String pFileName)
    {
        String parserAlias = processFileName(pFileName);

        if(null == parserAlias)
        {
            MainApp app = MainApp.getApp();
            ClassRegistry modelBuilderRegistry = app.getModelBuilderRegistry();
            
            java.util.List parserAliasesList = getModelBuilderAliasesList(modelBuilderRegistry);
            Collections.sort(parserAliasesList);
            Object []parserOptions = parserAliasesList.toArray();
            File inputFile = new File(pFileName);
            String shortName = inputFile.getName();
            SimpleTextArea textArea = new SimpleTextArea("In order to process this model definition, you will need to specify which parser to use.  Which parser plugin do you wish to use?\n[To cancel, just click the close box in the upper-right corner.]");
            JOptionPane parserOptionPane = new JOptionPane(textArea,
                                                           JOptionPane.WARNING_MESSAGE,
                                                           JOptionPane.DEFAULT_OPTION,
                                                           null,
                                                           parserOptions);
            JDialog parserDialog = parserOptionPane.createDialog(mMainFrame, "Please select a parser");
            parserDialog.show();
            parserAlias = (String) parserOptionPane.getValue();
            
            if(null == parserAlias)
            {
                handleCancel();
            }
        }

        return(parserAlias);
    }
}