package org.systemsbiology.gui;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.data.*;
import org.systemsbiology.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Displays a data file in tabular format, and allows the
 * user to select columns of data to save to a new file.
 * 
 * @author sramsey
 */
public class DataColumnSelector extends JFrame
{
    private MatrixStringTableModel mMatrixStringTableModel;
    private MatrixString mMatrixString;
    private static final String DEFAULT_DELIMITER = "\t";
    private File mDirectory;
    private String mDelimiter;
    
    public void setDelimiter(String pDelimiter)
    {
        mDelimiter = pDelimiter;
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


    static class MatrixStringTableModel extends AbstractTableModel
    {
        private MatrixString mMatrixString;
        private Boolean []mSelectedColumns;
        private JCheckBox []mSelectedColumnComponents;

        public Boolean []getColumnSelections()
        {
            return(mSelectedColumns);
        }

        public MatrixStringTableModel(MatrixString pMatrixString)
        {
            mMatrixString = pMatrixString;
            initializeSelectedColumns();
        }

        JCheckBox getComponent(int pColumn)
        {
            if(pColumn < 1)
            {
                throw new IllegalStateException("column value must be at least one");
            }
            return mSelectedColumnComponents[pColumn - 1];
        }

        private void initializeSelectedColumns()
        {
            int numColumns = mMatrixString.getColumnCount() - 1;
            if(numColumns < 0)
            {
                throw new IllegalStateException("invalid number of columns");
            }
            mSelectedColumns = new Boolean[numColumns];
            mSelectedColumnComponents = new JCheckBox[numColumns];
            for(int ctr = 0; ctr < numColumns; ++ctr)
            {
                mSelectedColumns[ctr] = new Boolean(true);
                mSelectedColumnComponents[ctr] = new JCheckBox();
                mSelectedColumnComponents[ctr].setSelected(true);
            }
        }

        public String getColumnName(int pColumn)
        {
            return(mMatrixString.getValueAt(0, pColumn));
        }

        public int getColumnCount()
        {
            return mMatrixString.getColumnCount();
        }

        public boolean isCellEditable(int pRow, int pColumn)
        {
            boolean retVal = false;
            if(pRow == 0 && pColumn > 0)
            {
                retVal = true;
            }
            return(retVal);
        }

        public int getRowCount()
        {
            return mMatrixString.getRowCount();
        }

        public void setValueAt(Object pValue, int pRow, int pColumn)
        {
            if(pRow == 0 && pColumn > 0)
            {
                mSelectedColumnComponents[pColumn - 1].setSelected(((Boolean) pValue).booleanValue());
                mSelectedColumns[pColumn - 1] = (Boolean) pValue;
            }
        }

        public Object getValueAt(int pRow, int pColumn)
        {
            if(pRow == 0)
            {
                if(pColumn == 0)
                {
                    return( mMatrixString.getValueAt(0, 0) );
                }
                else
                {
                    return mSelectedColumns[pColumn - 1];
                }
            }
            else
            {
                return mMatrixString.getValueAt(pRow, pColumn);
            }
        }
    }

    public DataColumnSelector(String pTitle, MatrixString pMatrixString) throws InvalidInputException
    {
        super(pTitle);
        mDirectory = null;
        mMatrixString = pMatrixString;
        if(pMatrixString.getRowCount() == 0)
        {
            throw new InvalidInputException("a minimum of one row in the data matrix is required");
        }
        mMatrixStringTableModel = new MatrixStringTableModel(pMatrixString);
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
        table.setDefaultEditor(Object.class, new DefaultCellEditor(new JCheckBox()));
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane);
        layout.setConstraints(scrollPane, constraints);
        setContentPane(panel);

        mMatrixStringTableModel.addTableModelListener(new TableModelListener()
        {
            public void tableChanged(TableModelEvent e)
            {
                int row = e.getFirstRow();
                int column = e.getColumn();
                TableModel model = (TableModel) e.getSource();
                Object data = model.getValueAt(row, column);
                System.out.println("changed: " + data);
            }
        });

        JButton button = new JButton("save selected columns");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleSaveButton();
            }
        });
        constraints.weightx = 1.0;
        constraints.weighty = 0.1;
        constraints.fill = GridBagConstraints.NONE;
        layout.setConstraints(button, constraints);
        panel.add(button);

        pack();
    }

    public static DataColumnSelector constructDataSelector(File pFile, String pDelimiter) throws IOException, InvalidInputException
    {
        FileReader fileReader = new FileReader(pFile);
        BufferedReader bufReader = new BufferedReader(fileReader);
        MatrixString matString = new MatrixString();
        matString.buildFromLineBasedStringDelimitedInput(bufReader, pDelimiter);
        DataColumnSelector dataSelector = new DataColumnSelector("Please select the data columns you wish to retain", matString);
        dataSelector.mDirectory = pFile.getParentFile();
        dataSelector.mMatrixString = matString;
        dataSelector.setVisible(true);
        return(dataSelector);
    }
 
    private File queryOutputFile(Component pParent)
    {
        FileChooser fileChooser = new FileChooser(mDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int value = fileChooser.showSaveDialog(pParent);
        File selectedFile = null;
        if(value == JFileChooser.APPROVE_OPTION)
        {
            selectedFile = fileChooser.getSelectedFile();
            if(null != selectedFile)
            {
                if(selectedFile.exists())
                {
                    boolean proceed = FileChooser.handleOutputFileAlreadyExists(this, selectedFile.getName());
                    if(! proceed)
                    {
                        selectedFile = null;
                    }
                }
            }
        }
        if(null != selectedFile)
        {
            mDirectory = selectedFile.getParentFile();
        }
        return(selectedFile);
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

    private void handleSaveFile(File pOutputFile, String pDelimiter) throws IOException
    {
        Boolean []selectedColumns = mMatrixStringTableModel.getColumnSelections();
        int numRows = mMatrixString.getRowCount();
        int numColumns = mMatrixString.getColumnCount();
        
        StringBuffer rowBuf = new StringBuffer();

        FileWriter fileWriter = new FileWriter(pOutputFile);
        PrintWriter printWriter = new PrintWriter(fileWriter);

        for(int rowCtr = 0; rowCtr < numRows; ++rowCtr)
        {
            String rowName = mMatrixString.getValueAt(rowCtr, 0);
            rowBuf.append(rowName);
            for(int colCtr = 0; colCtr < numColumns - 1; ++colCtr)
            {
                if(selectedColumns[colCtr].booleanValue())
                {
                    rowBuf.append(pDelimiter);
                    String value = mMatrixString.getValueAt(rowCtr, colCtr + 1);
                    rowBuf.append(value);
                }
            }

            printWriter.println(rowBuf.toString());
            rowBuf.delete(0, rowBuf.length());
        }
        
        printWriter.flush(); 

    }

    private void handleSaveButton()
    {
        File outputFile = queryOutputFile(this);
        if(null != outputFile)
        {
            try
            {
                handleSaveFile(outputFile, mDelimiter);
            }
            catch(Exception e)
            {
                ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
                optionPane.createDialog(this, "unable to save file").show();
            }
        }
    }

    public static final void main(String []pArgs)
    {
        try
        {
            File file = queryInputFile();
            if(null != file)
            {
                DataColumnSelector dataSelector = constructDataSelector(file, DEFAULT_DELIMITER);
                dataSelector.addWindowListener(
                    new WindowAdapter()
                    {
                        public void windowClosing(WindowEvent e)
                        {
                            System.exit(0);
                        }
                    });
            }
            else
            {
                System.exit(0);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
