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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
public class ObservationsData
{
    private ObjectMatrix2D mObservations;
    private String []mElementNames;
    private String []mEvidenceNames;
    private MatrixString mMatrixString;
    private double mMissingDataRate;
    private int []mNumObservations;
    
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
    
    public void loadFromFile(File pFile, DataFileDelimiter pDelimiter) throws FileNotFoundException, IOException, InvalidInputException
    {
        MatrixString matrixString = new MatrixString();
        FileReader fileReader = new FileReader(pFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        matrixString.buildFromLineBasedStringDelimitedInput(bufferedReader, pDelimiter);
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
        int missingCtr = 0;
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
                    ++missingCtr;
                }
                mObservations.set(i, j, elemValObj);
            }
        }
        mMissingDataRate = ((double) missingCtr)/((double) numEvidences*numElements);
        mNumObservations = new int[numEvidences];
        for(int j = 0; j < numEvidences; ++j)
        {
        	int obsCtr = 0;
        	for(int i = 0; i < numElements; ++i)
        	{
        		if(null != mObservations.get(i, j))
        		{
        			++obsCtr;
        		}
        	}
        	mNumObservations[j] = obsCtr;
        }
    }    

    public int getNumObservations(int pEvidenceNum)
    {
    	return mNumObservations[pEvidenceNum];
    }
}
