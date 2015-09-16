package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.*;
import java.io.File;

public class FileOpenChooser extends FileChooser
{
    private String mParserAlias;

    String getParserAlias()
    {
        return(mParserAlias);
    }

    public FileOpenChooser(Component pMainFrame)
    {
        super(pMainFrame);

        mParserAlias = null;

        FileFilter fileFilter = new ChemFileFilter();
        JFileChooser fileChooser = getFileChooser();
        setDialogTitle("Please select a file to open");
        fileChooser.setFileFilter(fileFilter);
    }

    public void show()
    {
        super.show();
        String fileName = getFileName();
        if(null != fileName)
        {
            ParserPicker parserPicker = new ParserPicker(getMainFrame());
            String parserAlias = parserPicker.selectParserAliasFromFileName(fileName);
            if(null != parserAlias)
            {
                mParserAlias = parserAlias;
            }
            else
            {
                setFileName(null);
                // do nothing, as this case means that the user has cancelled the open operation
            }
        }
    }
}
