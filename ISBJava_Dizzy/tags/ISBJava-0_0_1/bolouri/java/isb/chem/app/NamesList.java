package isb.chem.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public abstract class NamesList extends JList
{
    Component mContainingComponent;

    public NamesList(Container pPanel, String pListName, int pVisibleRowCount, int pDefaultCellWidth)
    {
        super();
        setFixedCellWidth(pDefaultCellWidth);
        JPanel listPane = new JPanel();
        listPane.setBorder(BorderFactory.createEtchedBorder());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        MouseListener mouseListener = new MouseAdapter()
        {
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2)
                {
                    int index = locationToIndex(event.getPoint());
                    if(-1 != index)
                    {
                        handleDoubleClick(index);
                    }
                }
            }
        };
        addMouseListener(mouseListener);
        setVisibleRowCount(pVisibleRowCount);

        JScrollPane listBoxPanel = new JScrollPane(this);

        JLabel label = new JLabel(pListName);
        listPane.add(label);
        listPane.add(listBoxPanel);
        pPanel.add(listPane);
    }

    protected abstract void handleDoubleClick(int index);

}
