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

import java.io.*;
import org.systemsbiology.data.*;
import cern.colt.matrix.*;
import org.systemsbiology.util.*;

/**
 * Contains a matrix of significance values of a set of
 * observations for M elements and N evidence types.  A
 * significance of -1 means a missing observation, so no
 * significance value exists for that (element,evidenc)
 * ordererd pair.  Non-missing significance values in 
 * this matrix are always nonnegative.
 * 
 * @author sramsey
 *
 */
public class SignificancesData 
{
    public static final double DEFAULT_SIGNIFICANCE_MISSING_DATA = -1.0;
    
    private DoubleMatrix2D mSignificances;
    private MatrixString mMatrixString;
    private String []mElementNames;
    private String []mEvidenceNames;
    private double mMissingDataRate;
    
    public SignificancesData()
    {
        mMissingDataRate = 0.0;
        mElementNames = null;
        mEvidenceNames = null;
        mMatrixString = null;
        mSignificances = null;
    }
    
    public String []getEvidenceNames()
    {
        int numEvidences = mEvidenceNames.length;
        String []newEvidenceNames = new String[numEvidences];
        for(int j = 0; j < numEvidences; ++j)
        {
            newEvidenceNames[j] = mEvidenceNames[j];
        }
        return newEvidenceNames;
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
    
    public double getValueAt(int pRow, int pColumn)
    {
        return mSignificances.get(pRow, pColumn);
    }

    public DoubleMatrix2D getSignificancesMatrix()
    {
        return mSignificances;
    }
    
    public double getMissingDataRate()
    {
        return mMissingDataRate;
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
        double elemVal = 0.0;
        mSignificances = DoubleFactory2D.dense.make(numElements, numEvidences);
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
                        elemVal = Double.parseDouble(elemStr);
                        if(elemVal < 0.0)
                        {
                            ++missingCtr;
                        }
                    }
                    catch(NumberFormatException e)
                    {
                        throw new InvalidInputException("invalid numeric data at row " + i + ", column " + j);
                    }
                }
                else
                {
                    elemVal = DEFAULT_SIGNIFICANCE_MISSING_DATA;
                    ++missingCtr;
                }
                mSignificances.set(i, j, elemVal);
            }
        }
        mMissingDataRate = ((double) missingCtr)/((double) numEvidences*numElements);
    }
    
    
}
