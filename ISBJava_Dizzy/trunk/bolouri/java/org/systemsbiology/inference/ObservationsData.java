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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cern.colt.list.*;
import cern.colt.matrix.*;

import org.systemsbiology.data.*;
import org.systemsbiology.util.InvalidInputException;

/**
 * A data structure containing a set of
 * observations for M elements, and N types of
 * evidence.  Missing obesrvations are stored as
 * a <code>null</code> in the data matrix.  Non-missing
 * observations are stored as a {@link Double} object.
 * This class is used by the {@link SignificanceCalculatorDriver}
 * class.
 * 
 * @author sramsey
 *
 */
public class ObservationsData implements Cloneable
{
    private ObjectMatrix2D mObservations;
    private String []mElementNames;
    private String []mEvidenceNames;
    private double mMissingDataRate;
    private int []mNumObservations;
    
    public ObservationsData()
    {
        mElementNames = null;
        mEvidenceNames = null;
        mMissingDataRate = 0.0;
        mNumObservations = null;
        mObservations = null;
    }
    
    public ObservationsData(ObjectMatrix2D pObservations,
                            String []pElementNames,
                            String []pEvidenceNames)
    {
        mObservations = pObservations;
        mElementNames = pElementNames;
        mEvidenceNames = pEvidenceNames;
        calculateStatistics();
    }
    
    public void setElementName(int pElementNumber, String pElementName)
    {
        mElementNames[pElementNumber] = pElementName;
    }

    public void setEvidenceName(int pEvidenceNumber, String pEvidenceName)
    {
        mEvidenceNames[pEvidenceNumber] = pEvidenceName;
    }
    
    public void mergeDataArray(ObservationsData []pObservationsDataArray, boolean pAllowDuplicates)
    {
        int numFiles = pObservationsDataArray.length;
        ObservationsData obsData = null;
        HashMap elementsMap = new HashMap();
        HashMap evidencesMap = new HashMap();
        String evidenceName = null;
        String elementName = null;
        int numEvidences = 0;
        int numElements = 0;
        String []evidences = null;
        String []elements = null;
        ArrayList elementsList = new ArrayList();
        ArrayList evidencesList = new ArrayList();
        ArrayList tempElements = new ArrayList();
        ArrayList tempEvidences = new ArrayList();
        Integer index = null;
        int i = 0; 
        int j = 0;
        int totalEvidences = 0;
        int totalElements = 0;
        Double obsObj = null;
        ArrayList tempElementsList = new ArrayList();
        ArrayList tempEvidencesList = new ArrayList();
        
        int k = 0;
        
        for(k = 0; k < numFiles; ++k)
        {
            obsData = pObservationsDataArray[k];
            elements = obsData.getElementNames();
            numElements = elements.length;
            tempElements = new ArrayList();
            for(i = 0; i < numElements; ++i)
            {
                elementName = elements[i];
                index = (Integer) elementsMap.get(elementName);
                if(null == index)
                {
                    elementsList.add(elementName);
                    index = new Integer(elementsList.size() - 1);
                    elementsMap.put(elementName, index);
                }
                tempElements.add(index);
            }
            totalElements = elementsList.size();
            
            evidences = obsData.getEvidenceNames();
            numEvidences = evidences.length;
            tempEvidences = new ArrayList();
            for(j = 0; j < numEvidences; ++j)
            {
                evidenceName = evidences[j];
                index = (Integer) evidencesMap.get(evidenceName);
                if(null == index)
                {
                    evidencesList.add(evidenceName);
                    index = new Integer(evidencesList.size() - 1);
                    evidencesMap.put(evidenceName, index);
                }
                tempEvidences.add(index);
            }
            
            totalEvidences = evidencesList.size();
            
            tempElementsList.add(tempElements);
            tempEvidencesList.add(tempEvidences);
        }
        
        String []elementNames = (String []) elementsList.toArray(new String[0]);
        String []evidenceNames = (String []) evidencesList.toArray(new String[0]);
        
        int totalNumElements = elementsList.size();
        int totalNumEvidences = evidencesList.size();
        ObjectMatrix2D masterMat = ObjectFactory2D.dense.make(totalNumElements, totalNumEvidences);
        masterMat.assign((Object) null);
        
        DoubleMatrix2D divisorMat = DoubleFactory2D.dense.make(totalNumElements, totalNumEvidences);
        divisorMat.assign(1.0);
        
        int ip = 0;
        int jp = 0;
        
        Double insertVal = null;
        Double existingObs = null;
        
        for(k = 0; k < numFiles; ++k)
        {
            obsData = pObservationsDataArray[k];
            tempElements = (ArrayList) tempElementsList.get(k);
            tempEvidences = (ArrayList) tempEvidencesList.get(k);
            numElements = tempElements.size();
            numEvidences = tempEvidences.size();
            ObjectMatrix2D obsMat = obsData.getObservations();
            for(j = 0; j < numEvidences; ++j)
            {
                jp = ((Integer) tempEvidences.get(j)).intValue();
                for(i = 0; i < numElements; ++i)
                {
                    obsObj = (Double) obsMat.get(i, j);
                    if(null != obsObj)
                    {
                        ip = ((Integer) tempElements.get(i)).intValue();
                        existingObs = (Double) masterMat.get(ip, jp);
                        if(null != existingObs)
                        {
                            if(pAllowDuplicates)
                            {
                                insertVal = new Double(existingObs.doubleValue() + obsObj.doubleValue());
                                divisorMat.set(ip, jp, divisorMat.get(ip, jp) + 1.0);
                            }
                            else
                            {
                                throw new IllegalArgumentException("element \"" + elementNames[ip] + "\" has a duplicate observation, in evidence \"" + evidenceNames[jp] + "\"");
                            }
                        }
                        else
                        {
                            insertVal = obsObj;
                        }
                        masterMat.set(ip, jp, insertVal);
                    }
                }
            }
        }
        
        double divisor = 0.0;
        for(j = 0; j < totalNumEvidences; ++j)
        {
            for(i = 0; i < totalNumElements; ++i)
            {
                obsObj = (Double) masterMat.get(i, j);
                if(null != obsObj)
                {
                    divisor = divisorMat.get(i, j);
                    if(divisor > 1.0)
                    {
                        obsObj = new Double(obsObj.doubleValue() / divisor);
                        masterMat.set(i, j, obsObj);
                    }
                }
            }
        }
        
        mObservations = masterMat;
        mElementNames = elementNames;
        mEvidenceNames = evidenceNames;
        calculateStatistics();
    }
    
    public Object clone()
    {
        ObservationsData newObj = new ObservationsData();
        
        int numElements = mElementNames.length;
        String []elements = new String[numElements];
        System.arraycopy(mElementNames, 0, elements, 0, numElements);
        newObj.mElementNames = elements;
        
        int numEvidences = mEvidenceNames.length;
        String []evidences = new String[numEvidences];
        System.arraycopy(mEvidenceNames, 0, evidences, 0, numEvidences);
        newObj.mEvidenceNames = evidences;
        
        newObj.mObservations = mObservations.copy();
        newObj.mMissingDataRate = mMissingDataRate;
        
        int []numObservations = new int[numEvidences];
        System.arraycopy(mNumObservations, 0, numObservations, 0, numEvidences);
        newObj.mNumObservations = numObservations;
        
        return newObj;
    }
    
    ObjectMatrix2D getObservations()
    {
        return mObservations;
    }
    
    void setObservations(ObjectMatrix2D pObservations)
    {
        mObservations = pObservations;
        calculateStatistics();
    }
    
    private void calculateStatistics()
    {
        int numElements = mObservations.rows();
        int numEvidences = mObservations.columns();

        int missingCtr = 0;
        
        mNumObservations = new int[numEvidences];
       
        for(int j = 0; j < numEvidences; ++j)
        {
            int obsCtr = 0;
            for(int i = 0; i < numElements; ++i)
            {
                if(null == mObservations.get(i, j))
                {
                    ++missingCtr;
                }
                else
                {
                    ++obsCtr;
                }
            }
            mNumObservations[j] = obsCtr;
        }
        
        mMissingDataRate = ((double) missingCtr)/((double) (numElements * numEvidences));
    }
    
    public int getNumElements()
    {
        return mElementNames.length;
    }
    
    public int getNumEvidences()
    {
        return mEvidenceNames.length;
    }
    
    public String getEvidenceName(int pColumn)
    {
        return mEvidenceNames[pColumn];
    }
    
    public String getElementName(int pRow)
    {
        return mElementNames[pRow];
    }
    
    public String []getElementNames()
    {
        int numElements = mElementNames.length;
        String []elementNames = new String[numElements];
        for(int i = 0; i < numElements; ++i)
        {
            elementNames[i] = mElementNames[i];
        }
        return elementNames;
    }
    
    public double getMissingDataRate()
    {
        return mMissingDataRate;
    }
        
    public String []getEvidenceNames()
    {
        int numEvidences = mEvidenceNames.length;
        String []evidenceNames = new String[numEvidences];
        for(int j = 0; j < numEvidences; ++j)
        {
            evidenceNames[j] = mEvidenceNames[j];
        }
        return evidenceNames;
    }
    
    public void setValueAt(int pRow, int pColumn, Double pValue)
    {
        mObservations.set(pRow, pColumn, pValue);
    }
    
    public Double getValueAt(int pRow, int pColumn)
    {
        return (Double) mObservations.get(pRow, pColumn);
    }    
    
    public Double []getColumn(int pColumn)
    {
        int numElements = mElementNames.length;
        Double []obsCol = new Double[numElements];
        for(int i = 0; i < numElements; ++i)
        {
            obsCol[i] = getValueAt(i, pColumn);
        }
        return obsCol;
    }
    
    public DoubleArrayList getNonMissingColumnVals(int pColumn)
    {
        DoubleArrayList retList = new DoubleArrayList();
        
        int numEvidences = mEvidenceNames.length;
        if(pColumn < 0 || pColumn >= numEvidences)
        {
            throw new IllegalArgumentException("invalid column number: " + pColumn);
        }
        
        int numElements = mElementNames.length;
        Double obsObj = null;
        double obs = 0.0;
        for(int i = 0; i < numElements; ++i)
        {
            obsObj = (Double) mObservations.get(i, pColumn);
            if(null != obsObj)
            {
                obs = obsObj.doubleValue();
                retList.add(obs);
            }
        }        
        
        return retList;
    }
    
    public void loadFromFile(BufferedReader pBufferedReader, DataFileDelimiter pDelimiter) throws IOException, InvalidInputException
    {
        MatrixString matrixString = new MatrixString();
//        FileReader fileReader = new FileReader(pFile);
//        BufferedReader bufferedReader = new BufferedReader(fileReader);
        matrixString.buildFromLineBasedStringDelimitedInput(pBufferedReader, pDelimiter);
        int numElements = matrixString.getRowCount() - 1;
        if(numElements < 1)
        {
            throw new InvalidInputException("The minimum number of elements is 1");
        }
        int numEvidences = matrixString.getColumnCount() - 1;
        if(numEvidences < 1)
        {
            throw new InvalidInputException("The minimum number of evidences is 1");
        }
        mElementNames = new String[numElements];
        String elemStr = null;
        for(int i = 0; i < numElements; ++i)
        {
            elemStr = matrixString.getValueAt(i + 1, 0).trim();
            mElementNames[i] = elemStr;
        }
        mEvidenceNames = new String[numEvidences];
        for(int j = 0; j < numEvidences; ++j)
        {
            elemStr = matrixString.getValueAt(0, j + 1).trim();
            mEvidenceNames[j] = elemStr;
        }
        Double elemValObj = null;
        mObservations = ObjectFactory2D.dense.make(numElements, numEvidences);
        for(int i = 0; i < numElements; ++i)
        {
            for(int j = 0; j < numEvidences; ++j)
            {
                elemStr = matrixString.getValueAt(i + 1, j + 1).trim();
                if(elemStr.length() > 0)
                {
                    try
                    {
                        elemValObj = new Double(elemStr);
                    }
                    catch(NumberFormatException e)
                    {
                        throw new InvalidInputException("invalid numeric data at row " + i + ", column " + j);
                    }
                }
                else
                {
                    elemValObj = null;
                }
                mObservations.set(i, j, elemValObj);
            }
        }

        calculateStatistics();
    }    

    public int getNumObservations(int pEvidenceNum)
    {
    	return mNumObservations[pEvidenceNum];
    }
}
