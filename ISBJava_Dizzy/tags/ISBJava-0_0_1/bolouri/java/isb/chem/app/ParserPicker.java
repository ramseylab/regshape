package isb.chem.app;

import java.io.File;
import isb.chem.scripting.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import isb.util.*;

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

    public static String processFileName(String pFileName)
    {
        String retParserAlias = null;
        MainApp app = MainApp.getApp();
        ScriptBuilder scriptBuilder = app.getScriptBuilder();
        Set parserAliases = scriptBuilder.getParserAliasesCopy();
        assert (parserAliases.size() > 0) : "no parser aliases found";
        java.util.List parserAliasesList = new LinkedList(parserAliases);
        Collections.sort(parserAliasesList);
        Iterator parserAliasesIter = parserAliasesList.iterator();
        while(parserAliasesIter.hasNext())
        {
            String parserAlias = (String) parserAliasesIter.next();
            IScriptBuildingParser parser = null;
            try
            {
                parser = scriptBuilder.getParser(parserAlias);
            }
            catch(DataNotFoundException e)
            {
                e.printStackTrace(System.err);
            }
            assert (null != parser): "unexpected exception thrown in scriptBuilder.getParser()";
            String fileRegex = parser.getFileRegex();
            if(pFileName.matches(fileRegex))
            {
                retParserAlias = parserAlias;
                break;
            }
        }

        return(retParserAlias);
    }

    public String selectParserAliasFromFileName(String pFileName)
    {
        String parserAlias = processFileName(pFileName);

        if(null == parserAlias)
        {
            MainApp app = MainApp.getApp();
            ScriptBuilder scriptBuilder = app.getScriptBuilder();
            Set parserAliases = scriptBuilder.getParserAliasesCopy();
            java.util.List parserAliasesList = new LinkedList(parserAliases);
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

        if(null != parserAlias)
        {
            File inputFile = new File(pFileName);
            String shortFileName = inputFile.getName();
            
            if(parserAlias.equals(ALIAS_MARKUP_LANGUAGE))
            {
                ReactionRateSpeciesModeSelectorDialog dialog = new ReactionRateSpeciesModeSelectorDialog(mMainFrame, shortFileName);
                // create dialog box to ask for type of import
                String selectedOption = (String) dialog.getValue();
                if(null == selectedOption)
                {
                    handleCancel();
                    parserAlias = null;
                }
                else
                {
                    if(selectedOption == ReactionRateSpeciesModeSelectorDialog.OPTION_CONCENTRATION)
                    {
                        parserAlias = ALIAS_MARKUP_LANGUAGE;
                    }
                    else if(selectedOption == ReactionRateSpeciesModeSelectorDialog.OPTION_MOLECULES)
                    {
                        parserAlias = ALIAS_MARKUP_LANGUAGE_MOLECULES;
                    }
                }
            }
        }

        return(parserAlias);
    }
}
