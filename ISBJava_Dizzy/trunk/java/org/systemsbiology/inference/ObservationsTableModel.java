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
package org.systemsbiology.inference;

import java.text.*;
import javax.swing.table.AbstractTableModel;
import org.systemsbiology.math.*;
import cern.colt.function.IntComparator;
import cern.colt.Sorting;

import org.systemsbiology.gui.SortStatus;

/**
 * A graphical user interface table model for  
 * {@link ObservationsData}.  Allows sorting of rows by
 * element name, and sorting of columns by evidence name.
 * 
 * @author sramsey
 *
 */
public class ObservationsTableModel extends AbstractTableModel
{
    public static final String COLUMN_NAME_ELEMENT = "element";
    private ObservationsData mObservationsData;
    private NumberFormat mNumberFormat;
    private int []mEvidenceSortMap; 
    private int []mElementSortMap;
    private StringSortingComparator mEvidenceSortingComparator;
    private StringSortingComparator mElementsSortingComparator;
    private boolean mElementNamesEditable;
    private boolean mEvidenceNamesEditable;
    private boolean mCellValuesEditable;
    
    public void setElementNamesEditable(boolean pElementNamesEditable)
    {
        mElementNamesEditable = pElementNamesEditable;
    }
    
    public void setEvidenceNamesEditable(boolean pEvidenceNamesEditable)
    {
        mEvidenceNamesEditable = pEvidenceNamesEditable;
    }
    
    public void setCellValuesEditable(boolean pCellValuesEditable)
    {
        mCellValuesEditable = pCellValuesEditable;
    }
    
    public void setObservationsData(ObservationsData pObservationsData)
    {
        mObservationsData = pObservationsData;
        
        updateEvidenceArrayChanged();
        updateElementArrayChanged();  
        
        fireTableStructureChanged();
    }
    
    class StringSortingComparator implements IntComparator
    {
        private SortStatus mSortStatus;
        private String []mStrings;
        public StringSortingComparator(SortStatus pSortStatus, String []pStrings)
        {
            setSortStatus(pSortStatus);
            setStrings(pStrings);
        }
        public SortStatus getSortStatus()
        {
            return mSortStatus;
        }
        public void setStrings(String []pStrings)
        {
            mStrings = pStrings;
        }
        public void setSortStatus(SortStatus pSortStatus)
        {
            mSortStatus = pSortStatus;
        }
        public int compare(int a, int b)
        {
            int result = 0;
            
            switch(mSortStatus.getCode())
            {
            case SortStatus.CODE_NONE:
                result = a-b;
                break;
            
            case SortStatus.CODE_ASCENDING:
                result = mStrings[a].compareTo(mStrings[b]);
                break;
                
            case SortStatus.CODE_DESCENDING:
                result = mStrings[b].compareTo(mStrings[a]);
                break;
            }
            
            return result;
        }
    }
    
    public ObservationsTableModel(ObservationsData pObservationsData)
    {
        mObservationsData = pObservationsData;
        mNumberFormat = new ScientificNumberFormat(new SignificantDigitsCalculator());
        
        mElementsSortingComparator = new StringSortingComparator(SortStatus.NONE, mObservationsData.getElementNames());
        mEvidenceSortingComparator = new StringSortingComparator(SortStatus.NONE, mObservationsData.getEvidenceNames());
        
        updateEvidenceArrayChanged();
        updateElementArrayChanged();
        
        mElementNamesEditable = false;
        mEvidenceNamesEditable = false;
        mCellValuesEditable = false;
    }
    
    private void updateEvidenceArrayChanged()
    {
        String []evidences = mObservationsData.getEvidenceNames();
        int numEvidences = evidences.length;
        
        mEvidenceSortingComparator.setStrings(evidences);
        mEvidenceSortMap = new int[numEvidences];
        for(int j = 0; j < numEvidences; ++j)
        {
            mEvidenceSortMap[j] = j;
        }
        sortEvidences();        
    }
    
    private void updateElementArrayChanged()
    {
        String []elements = mObservationsData.getElementNames();
        int numElements = elements.length;
        
        mElementsSortingComparator.setStrings(elements);
        mElementSortMap = new int[numElements];
        for(int i = 0; i < numElements; ++i)
        {
            mElementSortMap[i] = i;
        }
        sortElements();
    }
    
    private void sortEvidences()
    {
        Sorting.mergeSort(mEvidenceSortMap, 0, mEvidenceSortMap.length, mEvidenceSortingComparator);
    }
    
    private void sortElements()
    {
        Sorting.mergeSort(mElementSortMap, 0, mElementSortMap.length, mElementsSortingComparator);
    }
    
    public void setElementSortStatus(SortStatus pSortStatus)
    {
        if(! mElementsSortingComparator.getSortStatus().equals(pSortStatus))
        {
            mElementsSortingComparator.setSortStatus(pSortStatus);
            sortElements();
            fireTableStructureChanged();
        }
    }
        
    public void setEvidenceSortStatus(SortStatus pSortStatus)
    {
        if(! mEvidenceSortingComparator.getSortStatus().equals(pSortStatus))
        {
            mEvidenceSortingComparator.setSortStatus(pSortStatus);
            sortEvidences();
            fireTableStructureChanged();
        }
    }
   
    public SortStatus getEvidenceSortStatus()
    {
        return mEvidenceSortingComparator.getSortStatus();
    }
    
    public SortStatus getElementSortStatus()
    {
        return mElementsSortingComparator.getSortStatus();
    }
    
    public int getRowCount()
    {
        if(! mEvidenceNamesEditable)
        {
            return mObservationsData.getNumElements();
        }
        else
        {
            return mObservationsData.getNumElements() + 1;
        }
    }
    
    public int getColumnCount()
    {
        return mObservationsData.getNumEvidences() + 1;
    }
    
    public String getColumnName(int pColumn)
    {
        if(! mEvidenceNamesEditable)
        {
            if(pColumn == 0)
            {
                return COLUMN_NAME_ELEMENT;
            }
            else
            {
                return mObservationsData.getEvidenceName(mEvidenceSortMap[pColumn - 1]);
            }
        }
        else
        {
            return null;
        }
    }
    
    public Object getValueAtNoFormatting(int pRow, int pColumn)
    {
        Object retObj = null;
        
        if(mEvidenceNamesEditable)
        {
            if(pRow == 0)
            {
                if(pColumn == 0)
                {
                    return COLUMN_NAME_ELEMENT;
                }
                else
                {
                    return mObservationsData.getEvidenceName(mEvidenceSortMap[pColumn - 1]);
                }
            }
            --pRow;
        }
        
        if(pColumn == 0)
        {
            retObj = mObservationsData.getElementName(mElementSortMap[pRow]);
        }
        else
        {
            retObj = mObservationsData.getValueAt(mElementSortMap[pRow], mEvidenceSortMap[pColumn - 1]);
        }

        return retObj;        
    }
    
    public boolean isCellEditable(int pRow, int pColumn)
    {
        if(mEvidenceNamesEditable)
        {
            if(pRow == 0)
            {
                return (pColumn > 0);
            }
            else
            {
                if(pColumn == 0)
                {
                    return mElementNamesEditable;
                }
                else
                {
                    return mCellValuesEditable;
                }
            }
        }
        else
        {
            if(pColumn == 0)
            {
                return mElementNamesEditable;
            }
            else
            {
                return mCellValuesEditable;
            }
        }
    }
    
//    public Class getColumnClass(int pColumn)
//    {
//        return getValueAt(0, pColumn).getClass();
//    }    
    
    public void setValueAt(Object pValue, int pRow, int pColumn)
    {
        if(mEvidenceNamesEditable)
        {
            if(pRow == 0)
            {
                if(pColumn == 0)
                {
                    throw new IllegalStateException("cannot edit column 0, row 0 of an observations table");
                }
                int evidenceNumber = mEvidenceSortMap[pColumn-1];
                mObservationsData.setEvidenceName(evidenceNumber, (String) pValue);
                updateEvidenceArrayChanged();
                return;
            }
            else
            {
                --pRow;
            }
        }
        
        int elementNumber = mElementSortMap[pRow];
        if(pColumn == 0)
        {
            // user is changing the element name
            mObservationsData.setElementName(elementNumber, (String) pValue);
            updateElementArrayChanged();
        }
        else
        {
            int evidenceNumber = mEvidenceSortMap[pColumn - 1];
            Double doubleValue = null;
            try
            {
                doubleValue = new Double((String) pValue);
            }
            catch(NumberFormatException e)
            {
                return;
            }
            mObservationsData.setValueAt(elementNumber, evidenceNumber, doubleValue);
        }
    }
    
    public Object getValueAt(int pRow, int pColumn)
    {
        if(mEvidenceNamesEditable)
        {
            if(pRow == 0)
            {
                if(pColumn == 0)
                {
                    return COLUMN_NAME_ELEMENT;
                }
                else
                {
                    return mObservationsData.getEvidenceName(mEvidenceSortMap[pColumn - 1]);
                }
            }
            else
            {
                pRow--;
            }
        }
        
        Object retObj = null;
        
        if(pColumn == 0)
        {
            retObj = mObservationsData.getElementName(mElementSortMap[pRow]);
        }
        else
        {
            Double val = mObservationsData.getValueAt(mElementSortMap[pRow], mEvidenceSortMap[pColumn - 1]);
            if(null != val)
            {
                retObj = mNumberFormat.format(val.doubleValue());
            }
            else
            {
                retObj = null;
            }
        }
        return retObj;
    }


}
