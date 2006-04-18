/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.awt.Frame;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.event.*;
import org.systemsbiology.data.DataFileDelimiter;
import org.systemsbiology.data.MatrixString;
import org.systemsbiology.util.InvalidInputException;

/**
 * @author sramsey
 *
 */
public class MatrixStringSelectorDialog extends JDialog
{
    private MatrixStringTableModel mMatrixStringTableModel;
    private MatrixString mMatrixString;
    private static final DataFileDelimiter DEFAULT_DELIMITER = DataFileDelimiter.TAB;
    private File mDirectory;
    private DataFileDelimiter mDelimiter;
    private boolean mMultipleSelectionsAllowed;
    private static final boolean DEFAULT_MULTIPLE_SELECTIONS_ALLOWED = true;
    private JButton mApproveButton;
    
    public void setDelimiter(DataFileDelimiter pDelimiter)
    {
        mDelimiter = pDelimiter;
    }
    
    public void setMultipleSelectionsAllowed(boolean pMultipleSelectionsAllowed)
    {
        mMultipleSelectionsAllowed = pMultipleSelectionsAllowed;
    }
    
    static class MatrixStringTableCellRenderer implements TableCellRenderer
    {
        private TableCellRenderer mDefaultTableCellRenderer;
        private MatrixStringTableModel mTableModel;
        
        public MatrixStringTableCellRenderer(TableCellRenderer pDefaultTableCellRenderer,
                                             MatrixStringTableModel pTableModel )
        {
            mDefaultTableCellRenderer = pDefaultTableCellRenderer;
            mTableModel = pTableModel;
        }

        public Component getTableCellRendererComponent(JTable pTable, 
                                                       Object pValue,
                                                       boolean pIsSelected,
                                                       boolean pHasFocus,
                                                       int pRow,
                                                       int pColumn)
        {
            Component retVal = null;

            if(pValue instanceof Boolean)
            {
                retVal = mTableModel.getComponent(pColumn);
            }
            else
            {
                retVal = mDefaultTableCellRenderer.getTableCellRendererComponent(pTable,
                                                                                 pValue,
                                                                                 pIsSelected,
                                                                                 pHasFocus,
                                                                                 pRow,
                                                                                 pColumn);
            }

            return(retVal);
        }
    }

    class MatrixStringTableModel extends AbstractTableModel
    {
        private MatrixString mMatrixString;
        private Boolean []mSelectedColumns;
        private JCheckBox []mSelectedColumnComponents;
        private boolean mHasSelectedSomething;
        private boolean mFirstRowIsTitles;
        
        public Boolean []getColumnSelections()
        {
            return(mSelectedColumns);
        }

        public boolean getHasSelectedSomething()
        {
            return mHasSelectedSomething;
        }
        
        private void setHasSelectedSomething(boolean pHasSelectedSomething)
        {
            mHasSelectedSomething = pHasSelectedSomething;
        }
        
        public MatrixStringTableModel(MatrixString pMatrixString, boolean pFirstRowIsTitles)
        {
            mSelectedColumns = null;
            mSelectedColumnComponents = null;
            mMatrixString = pMatrixString;
            setHasSelectedSomething(false);
            mFirstRowIsTitles = pFirstRowIsTitles;
            initializeSelectedColumns();
        }

        public String getColumnName(int pColumn)
        {
            if(! mFirstRowIsTitles)
            {
                return null;
            }
            else
            {
                return mMatrixString.getValueAt(0, pColumn);
            }
        }
        
        JCheckBox getComponent(int pColumn)
        {
            if(pColumn < 0)
            {
                throw new IllegalStateException("column value must be at least zero");
            }
            return mSelectedColumnComponents[pColumn];
        }

        private void initializeSelectedColumns()
        {
            int numColumns = mMatrixString.getColumnCount();
            if(numColumns == 0)
            {
                throw new IllegalStateException("invalid number of columns");
            }
            mSelectedColumns = new Boolean[numColumns];
            mSelectedColumnComponents = new JCheckBox[numColumns];
            for(int ctr = 0; ctr < numColumns; ++ctr)
            {
                mSelectedColumns[ctr] = new Boolean(false);
                mSelectedColumnComponents[ctr] = new JCheckBox();
                mSelectedColumnComponents[ctr].setSelected(false);
            }
        }

        public int getColumnCount()
        {
            return mMatrixString.getColumnCount();
        }

        public boolean isCellEditable(int pRow, int pColumn)
        {
            boolean retVal = false;
            if(pRow == 0)
            {
                if(mMultipleSelectionsAllowed || 
                   !mHasSelectedSomething ||
                   mSelectedColumns[pColumn].booleanValue())
                {
                    retVal = true;
                }
            }
            return(retVal);
        }

        public int getRowCount()
        {
            if(! mFirstRowIsTitles)
            {
                return mMatrixString.getRowCount() + 1;
            }
            else
            {
                return mMatrixString.getRowCount();
            }
        }

        public void setValueAt(Object pValue, int pRow, int pColumn)
        {
            if(pRow == 0)
            {
                mSelectedColumnComponents[pColumn].setSelected(((Boolean) pValue).booleanValue());
                Boolean boolVal = (Boolean) pValue;
                if(!mMultipleSelectionsAllowed)
                {
                    setHasSelectedSomething(boolVal.booleanValue());
                }
                mSelectedColumns[pColumn] = (Boolean) pValue;
            }
        }

        public Object getValueAt(int pRow, int pColumn)
        {
            if(pRow == 0)
            {
                return mSelectedColumns[pColumn];
            }
            else
            {
                if(! mFirstRowIsTitles)
                {
                    return mMatrixString.getValueAt(pRow - 1, pColumn);
                }
                else
                {
                    return mMatrixString.getValueAt(pRow, pColumn);
                }
            }
        }
    }

    
    public MatrixStringSelectorDialog(Frame pParent, String pTitle, boolean pModal, MatrixString pMatrixString, boolean pFirstRowIsTitles) throws InvalidInputException
    {
        super(pParent, pTitle,  pModal);
        mDirectory = null;
        mMatrixString = pMatrixString;
        if(pMatrixString.getRowCount() == 0)
        {
            throw new InvalidInputException("a minimum of one row in the data matrix is required");
        }
        mMatrixStringTableModel = new MatrixStringTableModel(pMatrixString, pFirstRowIsTitles);
        mMultipleSelectionsAllowed = DEFAULT_MULTIPLE_SELECTIONS_ALLOWED;
        initialize();
        setDelimiter(DEFAULT_DELIMITER);
    }
    
    private void initialize()
    {
        JPanel panel = new JPanel();   
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        panel.setLayout(layout);

        JTable table = new JTable(mMatrixStringTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableCellRenderer defaultRenderer = table.getDefaultRenderer(Object.class);
        MatrixStringTableCellRenderer newRenderer = new MatrixStringTableCellRenderer(defaultRenderer, mMatrixStringTableModel);
        table.setDefaultRenderer(Object.class, newRenderer);
        DefaultCellEditor cellEditor = new DefaultCellEditor(new JCheckBox());
        table.setDefaultEditor(Object.class, cellEditor);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane);
        layout.setConstraints(scrollPane, constraints);
        setContentPane(panel);

        cellEditor.addCellEditorListener(new CellEditorListener()
                {
            public void editingCanceled(ChangeEvent e)
            {
            }
            public void editingStopped(ChangeEvent e)
            {
                mApproveButton.setEnabled(mMatrixStringTableModel.getHasSelectedSomething());
            }
                });
        mMatrixStringTableModel.addTableModelListener(new TableModelListener()
        {
            public void tableChanged(TableModelEvent e)
            {
                int row = e.getFirstRow();
                int column = e.getColumn();
                TableModel model = (TableModel) e.getSource();
                Object data = model.getValueAt(row, column);
            }
        });

        JButton approveButton = new JButton("approve");
        mApproveButton = approveButton;
        approveButton.setEnabled(false);
        mMatrixStringTableModel.addTableModelListener(new TableModelListener()
                {
            public void tableChanged(TableModelEvent e)
            {
                System.out.println("table changed");
            }
                });
        approveButton.addActionListener(new ActionListener()
                {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }});
        constraints.weightx = 1.0;
        constraints.weighty = 0.1;
        constraints.fill = GridBagConstraints.NONE;
        layout.setConstraints(approveButton, constraints);
        panel.add(approveButton);
        
        JButton cancelButton = new JButton("cancel");
        panel.add(cancelButton);
        cancelButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handleCancel();
                    }
                });
        pack();
    }

    public void handleCancel()
    {
        dispose();
    }
    
    public void addApproveListener(ActionListener pActionListener)
    {
        mApproveButton.addActionListener(pActionListener);
    }

    private static File queryInputFile()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int retVal = fileChooser.showOpenDialog(null);
        File selectedFile = null;
        if(retVal == JFileChooser.APPROVE_OPTION)
        {
            selectedFile = fileChooser.getSelectedFile();
        }

        return(selectedFile);
    }

    public Boolean []getSelectedColumns()
    {
        return mMatrixStringTableModel.getColumnSelections();
    }
    


    public static final void main(String []pArgs)
    {
        try
        {

        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
