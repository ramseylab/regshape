package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.io.File;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import org.systemsbiology.util.*;
import org.systemsbiology.chem.*;
import org.systemsbiology.gui.*;

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
        JOptionPane.showMessageDialog(mMainFrame,
                                      "Your model processing has been cancelled",
                                      "Model processing cancelled",
                                      JOptionPane.INFORMATION_MESSAGE);
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
                String fileRegex = modelBuilder.getFileRegex();
                if(pFileName.matches(fileRegex))
                {
                    retModelBuilderAlias = modelBuilderAlias;
                    break;
                }
            }
            catch(DataNotFoundException e)
            {
                e.printStackTrace(System.err);
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
