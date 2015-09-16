package isb.chem.app;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ReactionRateSpeciesModeSelectorDialog extends JOptionPane
{
    static final String OPTION_CONCENTRATION = "concentration";
    static final String OPTION_MOLECULES = "molecules";
    private static final String []OPTIONS = {OPTION_CONCENTRATION, OPTION_MOLECULES};
    
    public ReactionRateSpeciesModeSelectorDialog(Component pParent, String pShortFileName)
    {

        super("How should species symbols appearing within rate expressions be interpreted?", 
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.DEFAULT_OPTION,
              null,
              OPTIONS);
        JDialog dialog = createDialog(pParent, "Select species mode for import of file: " + pShortFileName);
        dialog.show();
    }
}
