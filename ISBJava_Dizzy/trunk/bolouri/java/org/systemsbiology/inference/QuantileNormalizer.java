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

import cern.colt.matrix.*;
import cern.colt.list.*;
import cern.jet.stat.Descriptive;
import cern.colt.Sorting;
import cern.colt.function.IntComparator;

// these are for debugging only:
import java.io.*;
import org.systemsbiology.data.*;

/**
 * Performs a quantile normalization of raw observations.  The
 * normalization is performed after a rescaling of the raw observations
 * in accordance of the scale parameter {@link QuantileNormalizationScale}.
 * All parameters for the normalization are contained in the 
 * {@link QuantileNormalizerParams} class.   Results are stored in the
 * {@link QuantileNormalizationResults} object, which must have a pre-allocated
 * <code>cern.colt.matrix.ObjectMatrix2D</code> object <code>mNormalizedObservations</code>
 * to hold the normalized observation values.  The quantile normalization algorithm
 * implemented here is based on a prototype written by Daehee Hwang at 
 * Institute for Systems Biology, and it is similar to the quantile normalization 
 * algorithm proposed by Bolstat <em>et al.</em> in their paper
 * <blockquote>
 * Bolstad, B.M., Irizarry R. A., Astrand M., and Speed, T.P. (2003), 
 * "A Comparison of Normalization Methods for High Density Oligonucleotide Array Data 
 * Based on Bias and Variance." <em>Bioinformatics</em> <b>19</b>(2):185-193
 * </blockquote>
 * Note that only the quantile normalization step of the RMA (Robust Multi-Chi Average)
 * procedure is implemented in this class; background adjustment is not implemented here,
 * and is assumed to have been applied to the raw observations before this class's 
 * normalization function is to be executed.
 * 
 * @author sramsey
 *
 */
public class QuantileNormalizer
{
    private static final double LOG2 = Math.log(2.0);
    private ObjectMatrix2D mRawObservations;
    private ObjectMatrix2D mRawObservationsFixedForScaling;
    private DoubleMatrix2D mRawObservationsFilledIn;
    private DoubleMatrix2D mRescaledRawObservations;
    private DoubleArrayList mColumnObservations;
    private IntArrayList mColumnMissingObservationsIndices;
    private DoubleMatrix1D mAverageObservations;
    private int mNumColumns;
    private int mNumRows;
    private Object []mColumnIndices;
    private ElementComparator mElementComparator;
    private ObjectMatrix2D mNormalizedObservations;
    private DoubleMatrix1D mRawColumnMediansNonNegative;
    
    static class ElementComparator implements IntComparator
    {
        private DoubleMatrix1D mColumnValues;
        
        public ElementComparator()
        {
            mColumnValues = null;
        }
        
        public void setColumnValues(DoubleMatrix1D pColumnValues)
        {
            mColumnValues = pColumnValues;
        }
        
        public int compare(int pElement1, int pElement2)
        {
            DoubleMatrix1D columnValues = mColumnValues;
            if(null == columnValues)
            {
                throw new IllegalStateException("no column values defined for ElementComparator");
            }
            return Double.compare(columnValues.get(pElement1), columnValues.get(pElement2));
        }
    }
    
    public QuantileNormalizer()
    {
        mNumColumns = 0;
        mNumRows = 0;
        mRawObservationsFilledIn = null;
        mColumnObservations = null;
        mColumnMissingObservationsIndices = null;
        mRescaledRawObservations = null;
        mAverageObservations = null;
        mColumnIndices = null;
        mNormalizedObservations = null;
        mRawObservations = null;
        mRawObservationsFixedForScaling = null;
        mRawColumnMediansNonNegative = null;
        mElementComparator = new ElementComparator();
    }

    private void initializeIfNecessary(ObjectMatrix2D pRawObservations, ObjectMatrix2D pNormalizedObservations)
    {
        int numColumns = pRawObservations.columns();
        int numRows = pRawObservations.rows();
        if(pNormalizedObservations.rows() != numRows)
        {
            throw new IllegalArgumentException("normalized observations data structure has an incorrect number of rows");
        }
        if(pNormalizedObservations.columns() != numColumns)
        {
            throw new IllegalArgumentException("normalized observations data structure has an incorrect number of columns");
        }
        if(numRows == 0)
        {
            throw new IllegalArgumentException("invalid number of rows");
        }
        if(numColumns == 0)
        {
            throw new IllegalArgumentException("invalid number of columns");
        }
        if(mNumColumns != numColumns || mNumRows != numRows)
        {
            initialize(numRows, numColumns);
        } 
        mRawObservations = pRawObservations;
        mNormalizedObservations = pNormalizedObservations;
    }
        

    private void initialize(int pNumRows, int pNumColumns)
    {
        mNumRows = pNumRows;
        mNumColumns = pNumColumns;
        mRawObservationsFilledIn = DoubleFactory2D.dense.make(pNumRows, pNumColumns);
        mRescaledRawObservations = DoubleFactory2D.dense.make(pNumRows, pNumColumns);
        mColumnObservations = new DoubleArrayList();
        mColumnMissingObservationsIndices = new IntArrayList();
        mAverageObservations = DoubleFactory1D.dense.make(pNumRows);
        mRawObservationsFixedForScaling = ObjectFactory2D.dense.make(pNumRows, pNumColumns);
        mRawColumnMediansNonNegative = DoubleFactory1D.dense.make(pNumColumns);
        mColumnIndices = new Object[pNumColumns];
        for(int j = pNumColumns; --j >= 0; )
        {
            mColumnIndices[j] = new int[pNumRows];
        }
    }
    
    
    private void unscaleRawColumnMedians(QuantileNormalizationScale pScale)
    {
        DoubleMatrix1D rawColumns = mRawColumnMediansNonNegative;
        int numColumns = rawColumns.size();
        double value = 0.0;
        boolean doExp = false;
        if(pScale.equals(QuantileNormalizationScale.LOGARITHM))
        {
            doExp = true;
        }
        else if(pScale.equals(QuantileNormalizationScale.NORM_ONLY))
        {
            // do nothing
        }
        else
        {
            throw new IllegalArgumentException("unknown quantile normalization scale: " + pScale.getName());
        }
        double unscaledVal = 0.0;
        for(int j = numColumns; --j >= 0; )
        {
            value = rawColumns.get(j);
            if(doExp)
            {
                unscaledVal = Math.pow(2.0, value);
            }
            else
            {
                unscaledVal = value;
            }
            rawColumns.set(j, unscaledVal);
        }
    }

    // mAverageObservations => mNormalizedObservations
    private void normalizeObservations()
    {
        int numColumns = mRescaledRawObservations.columns();
        int numRows = mRescaledRawObservations.rows();
        int j = 0;
        int i = 0;
        int index = 0;
        int []columnIndices = null;
        double value = 0.0;
        Object []columnIndicesArray = mColumnIndices;
        DoubleMatrix1D rowAverages = mAverageObservations;
        ObjectMatrix2D normalizedObservations = mNormalizedObservations;
        ObjectMatrix2D rawObservations = mRawObservations;
        double rawObs = 0.0;
        for(j = numColumns; --j >= 0; )
        {
            columnIndices = (int []) columnIndicesArray[j];
            for(i = numRows; --i >= 0; )
            {
                index = columnIndices[i];
                value = rowAverages.get(i);
                if(null != mRawObservations.get(index, j))
                {
                    normalizedObservations.set(index, j, new Double(value));
                }
                else
                {
                    normalizedObservations.set(index, j, null);
                }
            }
        }
    }
    
    // mLogRawObservations -> mAverageObservations
    private void getRowAverages()
    {
        int numColumns = mRescaledRawObservations.columns();
        int numRows = mRescaledRawObservations.rows();
        int i = 0;
        double numColumnsDouble = (double) numColumns;
        double average = 0.0;
        DoubleMatrix1D columnMatrix = null;
        for(i = numRows; --i >= 0; )
        {
            columnMatrix = mRescaledRawObservations.viewRow(i);
            average = columnMatrix.zSum() / numColumnsDouble;
            mAverageObservations.set(i, average);
        }
    }
    
    // mLogRawObservations => mLogRawObservations
    private void sortRawObservations()
    {
        int numColumns = mRescaledRawObservations.columns();
        int numRows = mRescaledRawObservations.rows();
        int j = 0;
        int i = 0;
        double obsVal = 0.0;
        DoubleMatrix1D columnMatrix = null;
        int []columnIndices = null;
        for(j = 0; j < numColumns; ++j)
        {
            columnIndices = (int []) mColumnIndices[j];
            for(i = 0; i < numRows; ++i)
            {
                columnIndices[i] = i;
            }
        }
        ElementComparator elementComparator = mElementComparator;
        Object []columnIndicesArray = mColumnIndices;
        DoubleMatrix2D logRawObservations = mRescaledRawObservations;
        for(j = numColumns; --j >= 0; )
        {        
            columnIndices = (int []) columnIndicesArray[j];
            columnMatrix = logRawObservations.viewColumn(j);
            elementComparator.setColumnValues(columnMatrix);
            Sorting.mergeSort(columnIndices, 0, numRows, elementComparator);
            columnMatrix.assign(cern.colt.matrix.doublealgo.Sorting.mergeSort.sort(columnMatrix));
        }
    }
        
    // mRawObservationsFilledIn => mLogRawObservations
    public void rescaleRawObservations(QuantileNormalizationScale pScale)
    {
        int numColumns = mRawObservationsFilledIn.columns();
        int numRows = mRawObservationsFilledIn.rows();
        int j = 0;
        int i = 0;
        double obsVal = 0.0;
        
        boolean doLog = false;
        if(pScale.equals(QuantileNormalizationScale.LOGARITHM))
        {
            doLog = true;
        }
        else if(pScale.equals(QuantileNormalizationScale.NORM_ONLY))
        {
            // do nothing
        }
        else
        {
            throw new IllegalArgumentException("unknown quantile normalization scale \"" + pScale.getName() + "\"");
            
        }
        double rescaledObs = 0.0;
        for(j = numColumns; --j >= 0; )
        {
            for(i = numRows; --i >= 0; )
            {
                obsVal = mRawObservationsFilledIn.get(i, j);
                if(doLog)
                {
                    if(obsVal <= 0.0)
                    {
                        throw new IllegalArgumentException("non-positive raw observation (" + obsVal + ") at row " + i + " and column " + j);
                    }
                    rescaledObs = Math.log(obsVal/LOG2);
                }
                else
                {
                    rescaledObs = obsVal;
                }
                mRescaledRawObservations.set(i, j, rescaledObs);
            }
        }
    }
    
    // mRawColumnMediansNonNegative, mRAwObservationsNonNegative => mRawObservationsFilledIn
    private double fillInMissingObservations(boolean pComputeError)
    {
        int numColumns = mRawObservationsFixedForScaling.columns();
        int numRows = mRawObservationsFixedForScaling.rows();
        int j = 0;
        int i = 0;
        Double obsObj = null;
        double obsVal = 0.0;
        double median = 0.0;
        int numMissing = 0;
        int index = 0;
        double errFrac = 0.0;
        double errSum = 0.0;
        int errDiv = 0;
        double oldVal = 0.0;
        double avgVal = 0.0;
        for(j = numColumns; --j >= 0; )
        {
            median = mRawColumnMediansNonNegative.get(j);
            for(i = numRows; --i >= 0; )
            {
                obsObj = (Double) mRawObservationsFixedForScaling.get(i, j);
                if(null != obsObj)
                {
                    obsVal = obsObj.doubleValue();
                    mRawObservationsFilledIn.set(i, j, obsVal);
                }
                else
                {
                    if(pComputeError)
                    {
                        oldVal = mRawObservationsFilledIn.get(i, j);
                        avgVal = Math.abs(0.5 * (oldVal + median));
                        if(avgVal > 0.0)
                        {
                            ++errDiv;
                            errFrac = Math.abs(oldVal - median)/avgVal;
                            errSum += errFrac;
                        }
                    }
                    mRawObservationsFilledIn.set(i, j, median);
                }
            }
        }

        if(pComputeError)
        {
            if(errDiv > 0)
            {
                errSum /= ((double) errDiv);
            }
            else
            {
                errSum = 0.0;
            }
            return errSum;
        }
        else
        {
            return 0.0;
        }
    }
    
    // [mNormalizedObservations|mRawObservationsNonNegative] -> mRawColumnMediansNonNegative
    private void computeRawColumnMediansNonNegative(ObjectMatrix2D pObs)
    {
        int numColumns = pObs.columns();
        int numRows = pObs.rows();
        DoubleArrayList columnObs = mColumnObservations;
        double median = 0.0;
        Double obsObj = null;
        int i = 0;
        for(int j = numColumns; --j >= 0; )
        {
            columnObs.clear();
            for(i = numRows; --i >= 0; )
            {
                obsObj = (Double) pObs.get(i, j);
                if(null != obsObj)
                {
                    columnObs.add(obsObj.doubleValue());
                }
            }
            median = Descriptive.median(columnObs);
            mRawColumnMediansNonNegative.set(j, median);
        }
    }
    
    // mRawObservations => mRawObservationsNonNegative
    private void fixNonpositiveValues(boolean pFixNonpositiveValues, QuantileNormalizationScale pScale)
    {
        ObjectMatrix2D obsNonNeg = mRawObservationsFixedForScaling;
        obsNonNeg.assign(mRawObservations);
        boolean usingLogScale = pScale.equals(QuantileNormalizationScale.LOGARITHM);
        if(pFixNonpositiveValues)
        {
            int numColumns = mRawObservations.columns();
            int numRows = mRawObservations.rows();
            double minValue = Double.MAX_VALUE;
            int j = 0;
            int i = 0;
            double value = 0.0;
            Double obsObj = null;
            for(j = numColumns; --j >= 0; )
            {
                for(i = numRows; --i >= 0; ) 
                {
                    obsObj = (Double) mRawObservations.get(i, j);
                    if(null != obsObj)
                    {
                        value = obsObj.doubleValue();
                        if(value < minValue)
                        {
                            minValue = value;
                        }                    
                    }
                }
            }
            if(minValue <= 0.0)
            {
                double addTo = 0.0;
                if(usingLogScale)
                {
                    addTo = 1.0 - minValue;
                }
                else
                {
                    addTo = -minValue;
                }
                for(j = numColumns; --j >= 0; )
                {
                    for(i = numRows; --i >= 0; )
                    {
                        obsObj = (Double) obsNonNeg.get(i, j);
                        if(null != obsObj)
                        {
                            value = obsObj.doubleValue() + addTo;
                            obsNonNeg.set(i, j, new Double(value));
                        }
                    }
                }
            }
        }
    }
    
    public void normalize(ObjectMatrix2D pRawObservations,
                          QuantileNormalizerParams pParams,
                          QuantileNormalizationResults pResults)
    {
        Double errorTolerance = pParams.mErrorTolerance;
        ObjectMatrix2D normalizedObservations = pResults.mNormalizedObservations;
        if(normalizedObservations.columns() != pRawObservations.columns())
        {
            throw new IllegalArgumentException("normalized observations matrix size is not consistent with raw observations matrix column count"); 
        }
        if(normalizedObservations.rows() != pRawObservations.rows())
        {
            throw new IllegalArgumentException("normalized observations matrix size is not consistent with raw observations matrix row count"); 
        }
        QuantileNormalizationScale scale = pParams.mScale;
        
        
        if(null != errorTolerance && errorTolerance.doubleValue() <= 0.0)
        {
            throw new IllegalArgumentException("non-negative error tolerance provide: " + errorTolerance);
        }
        
        initializeIfNecessary(pRawObservations, normalizedObservations);

        boolean fixNonpositiveValues = pParams.mFixNonpositiveValues;

        fixNonpositiveValues(fixNonpositiveValues, scale);
        
//        System.out.println("raw obs: " + mRawObservationsNonNegative.toString());
        computeRawColumnMediansNonNegative(mRawObservationsFixedForScaling);
//        System.out.println("raw medians non-negative: " + mRawColumnMediansNonNegative.toString());
        fillInMissingObservations(false);
        
        boolean computeErrors = false;
        double errorToleranceVal = 0.0;
        if(errorTolerance != null)
        {
            errorToleranceVal = errorTolerance.doubleValue();
            computeErrors = true;
        }

        double errSum = 0.0;
        
        int iterationCtr = 0;
        
        do
        {
            rescaleRawObservations(scale);
                
            //            System.out.println("log obs:\n" + mLogRawObservations.toString());
            sortRawObservations();
//            System.out.println("sorted log obs:\n" + mLogRawObservations.toString());
            getRowAverages();
            normalizeObservations();
            computeRawColumnMediansNonNegative(mNormalizedObservations);
            unscaleRawColumnMedians(scale);
            errSum = fillInMissingObservations(computeErrors);
            ++iterationCtr;
//            System.out.println("errSum: " + errSum);
        }
        while(computeErrors && errSum > errorToleranceVal);
        
//        System.out.println("iterations: " + iterationCtr);        
        
        pResults.mNumIterations = iterationCtr;
        
        if(null != errorTolerance)
        {
            pResults.mFinalError = new Double(errSum);
        }
        else
        {
            pResults.mFinalError = null;
        }
    }
    
    public QuantileNormalizationResults normalize(ObjectMatrix2D pRawObservations,
                                                  QuantileNormalizerParams pParams)
    {
        QuantileNormalizationResults results = new QuantileNormalizationResults();
        results.mNormalizedObservations = pRawObservations.like();
        normalize(pRawObservations, pParams, results);
        return results;
    }
    
    public static final void main(String []pArgs)
    {
        try
        {
            if(pArgs.length < 1)
            {
                System.err.println("please re-run with the filename supplied as the command-line argument to the program");
                System.exit(1);
            }
            String fileName = pArgs[0];
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            ObservationsData obsData = new ObservationsData();
            obsData.loadFromFile(bufferedReader, DataFileDelimiter.TAB);
            ObjectMatrix2D obs = obsData.getObservations();
            QuantileNormalizer normalizer = new QuantileNormalizer();
            int numColumns = obs.columns();
            int numRows = obs.rows();
            ObjectMatrix2D normObs = ObjectFactory2D.dense.make(numRows, numColumns);
            QuantileNormalizerParams params = new QuantileNormalizerParams();
            params.mErrorTolerance = new Double(1.0e-4);
            params.mScale = QuantileNormalizationScale.LOGARITHM;
            QuantileNormalizationResults results = new QuantileNormalizationResults();
            results.mNormalizedObservations = normObs;
            normalizer.normalize(obs, params, results);
            String fileOutName = fileName + "_norm.txt";
            File outFile = new File(fileOutName);
            FileWriter fileWriter = new FileWriter(outFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(normObs.toString());
            printWriter.flush();
        }
        
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
        
    }
        
}
