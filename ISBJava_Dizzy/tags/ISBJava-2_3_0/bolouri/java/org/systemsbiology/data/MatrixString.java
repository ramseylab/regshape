package org.systemsbiology.data;

/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import java.util.*;
import java.io.*;

/**
 * Reprsents a 2-dimensional matrix of String objects
 */
public class MatrixString
{
    private ArrayList mRows;
    
    public MatrixString()
    {
        mRows = new ArrayList();
    }

    public void addRow(ArrayList pColumnValues)
    {
        mRows.add(pColumnValues);
    }

    public void clear()
    {
        mRows.clear();
    }

    public String []getRow(int pRow)
    {
        ArrayList row = (ArrayList) mRows.get(pRow);
        return (String []) row.toArray(new String[0]);
    }
       
    public String []getColumn(int pColumn)
    {
        int numRows = getRowCount();
        String []retArr = new String[numRows];
        for(int i = 0; i < numRows; ++i)
        {
            ArrayList row = (ArrayList) mRows.get(i);
            retArr[i] = (String) row.get(pColumn);
        }
        
        return retArr;
    }
    
    public String getValueAt(int pRow, int pColumn)
    {
        return (String) ((ArrayList) mRows.get(pRow)).get(pColumn);
    }

    public int getRowCount()
    {
        return mRows.size();
    }

    public int getColumnCount()
    {
        if(mRows.size() == 0)
        {
            return 0;
        }
        ArrayList firstRow = (ArrayList) mRows.get(0);
        return(firstRow.size());
    }

    public String toString()
    {
        return(toString(","));
    }

    public String toString(String pDelimiter)
    {
        Iterator rowIter = mRows.iterator();
        Iterator colIter = null;
        StringBuffer retBuf = new StringBuffer();
        while(rowIter.hasNext())
        {
            ArrayList row = (ArrayList) rowIter.next();
            colIter = row.iterator();
            boolean firstCol = true;
            while(colIter.hasNext())
            {
                String element = (String) colIter.next();
                if(firstCol)
                {
                    firstCol = false;
                }
                else
                {
                    retBuf.append(pDelimiter);
                }
                retBuf.append(element);
            }
            retBuf.append("\n");
        }
        return(retBuf.toString());
    }
    
    public void buildFromLineBasedStringDelimitedInput(BufferedReader pInputReader,
                                                       DataFileDelimiter pDelimiter) throws IOException, InvalidInputException
    {
        String inputLine = null;
        StringTokenizer lineTokenizer = null;
        ArrayList row = null;
        clear();
        Integer numCols = null;
        int rowCount = 0;
        boolean returnDelims = pDelimiter.getSingle();
        String delimiter = pDelimiter.getDelimiter();
        while(null != (inputLine = pInputReader.readLine()))
        {
            row = new ArrayList();
            lineTokenizer = new StringTokenizer(inputLine, delimiter, returnDelims);
            if(inputLine.startsWith(delimiter))
            {
                row.add("");
            }
            String element = null;
            boolean lastWasDelim = false;
            while(lineTokenizer.hasMoreTokens())
            {
                element = lineTokenizer.nextToken();
                if(element.startsWith(delimiter))
                {
                    if(lastWasDelim)
                    {
                        row.add("");
                    }
                    if(! lineTokenizer.hasMoreTokens())
                    {
                        row.add("");
                    }
                    else
                    {
                        lastWasDelim = true;
                    }
                    continue;
                }
                else
                {
                    lastWasDelim = false;
                    row.add(element);
                }
            }
            if(null == numCols)
            {
                numCols = new Integer(row.size());
            }
            else
            {
                if(row.size() != numCols.intValue())
                {
                    String lineNum = Integer.toString(rowCount + 1);
                    throw new InvalidInputException("inconsistent row size, at line number " + lineNum);
                }
            }
            rowCount++;
            mRows.add(row);
        }
    }

    public static final void main(String []pArgs)
    {
        try
        {
            String fileName = pArgs[0];
            if(null == fileName)
            {
                throw new IllegalArgumentException("no filename was supplied");
            }

            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufReader = new BufferedReader(fileReader);
            MatrixString matString = new MatrixString();
            matString.buildFromLineBasedStringDelimitedInput(bufReader, DataFileDelimiter.TAB);
            System.out.println(matString.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
    
}
