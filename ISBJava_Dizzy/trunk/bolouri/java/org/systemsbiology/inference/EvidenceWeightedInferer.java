/*
 * Copyright (C) 2004 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.inference;

import cern.jet.math.*;
import cern.colt.function.*;
import cern.colt.matrix.*;
import cern.colt.matrix.doublealgo.*;
import org.systemsbiology.math.AccuracyException;

/**
 * An implementation of the Pointillist algorithm for
 * inferring the set elements affected by a perturbation of
 * a biological system, based on multiple types of evidence.  The
 * algorithm computes the combined probability that an element would
 * have a given set of significance values, by chance, if it were
 * not in the set of true affected elements.  This algorithm was
 * designed by Daehee Hwang at Institute for Systems Biology.
 * 
 * @author sramsey
 *
 */
public final class EvidenceWeightedInferer
{
    public static final int MIN_NUM_BINS = 2;
    
    private DoubleMatrix2D mDistributionSignificancesAffected;
    private DoubleMatrix2D mDistributionSignificancesUnaffected;
    private DoubleMatrix2D mDistributionSignificancesDifference;
    private DoubleMatrix1D mNegLogCombinedEffectiveSignificances;
    
    private DoubleMatrix1D mMinimumNonzeroEvidenceSpecificSignificances;
    private DoubleMatrix1D mDistributionCombinedSignificances;
    private DoubleMatrix1D mDistributionCombinedSignificancesAffected;
    private DoubleMatrix1D mDistributionCombinedSignificancesUnaffected;
    private DoubleMatrix1D mDistributionCombinedSignificancesDifference;
    private DoubleMatrix1D mEffectiveSignificanceNormalizations;
    private DoubleMatrix1D mAffectedSignificances;
    private DoubleMatrix1D mUnaffectedSignificances;
    
    private DoubleMatrix2D mEffectiveSignificances;
    private DoubleMatrix2D mNonzeroSignificances;
    private DoubleMatrix2D mSignificancesSorted;
    
    private DoubleMatrix1D mDistNormAffected;
    private DoubleMatrix1D mDistNormUnaffected;
    
    private ColumnwiseDivider mColumnwiseDividerAffected;
    private ColumnwiseDivider mColumnwiseDividerUnaffected;
    
    private int mNumBins;
    private int mNumEvidences;
    private int mNumElements;
    private DoubleMatrix1D mWeights;
    private DoubleMatrix1D mBiases;
    
    private SignificanceCalculator mSignificanceCalculator;
    private SignificanceCalculationResults mSignificanceCalculationResults;
    
    static class ColumnwiseDivider implements IntIntDoubleFunction
    {
        DoubleMatrix1D mDivisors;
        int mNumColumns;
        public ColumnwiseDivider(DoubleMatrix1D pDivisors)
        {
            mDivisors = pDivisors;
            mNumColumns = pDivisors.size();
        }
        public double apply(int row, int col, double x)
        {
            if(col >= mNumColumns)
            {
                throw new IllegalArgumentException("matrix size is inconsistent with number of columns");
            }
           
            return x/mDivisors.get(col);
        }
    }
    
    static class IterationResults 
    {
        public double mSignificanceDistributionSeparation;
        public double mCriticalSignificance;
        public double mLowQuantileSignificanceAffected;
        public double mHighQuantileSignificanceUnaffected;
        public double mAlphaParameter;
    }
    
    public EvidenceWeightedInferer()
    {
        mNumEvidences = 0;
        mNumBins = 0;
        mNumElements = 0;
        mSignificanceCalculator = new SignificanceCalculator();
    }
    
    private void initialize(int pNumEvidences, int pNumBins, int pNumElements)
    {
        mNumEvidences = pNumEvidences;
        mNumBins = pNumBins;
        mNumElements = pNumElements;
        
        mSignificanceCalculationResults = new SignificanceCalculationResults(mNumElements);
        
        DoubleFactory2D fac2d = DoubleFactory2D.dense;
        
        mDistributionSignificancesAffected = fac2d.make(mNumBins, mNumEvidences);
        mDistributionSignificancesUnaffected = fac2d.make(mNumBins, mNumEvidences);
        mDistributionSignificancesDifference = fac2d.make(mNumBins, mNumEvidences);
        mNonzeroSignificances = fac2d.make(mNumElements, mNumEvidences);
        
        mEffectiveSignificances = fac2d.make(mNumElements, mNumEvidences);
        mSignificancesSorted = fac2d.make(mNumElements, mNumEvidences);
        
        DoubleFactory1D fac1d = DoubleFactory1D.dense;

        mWeights = fac1d.make(mNumEvidences);
        mBiases = fac1d.make(mNumEvidences);

        mDistributionCombinedSignificances = fac1d.make(mNumBins);
        mDistributionCombinedSignificancesAffected = fac1d.make(mNumBins);
        mDistributionCombinedSignificancesUnaffected = fac1d.make(mNumBins);
        mDistributionCombinedSignificancesDifference = fac1d.make(mNumBins);
        mAffectedSignificances = fac1d.make(mNumElements);
        mUnaffectedSignificances = fac1d.make(mNumElements);
        
        mDistNormAffected = fac1d.make(mNumEvidences);
        mDistNormUnaffected = fac1d.make(mNumEvidences);
        mEffectiveSignificanceNormalizations = fac1d.make(mNumEvidences);
        mMinimumNonzeroEvidenceSpecificSignificances = fac1d.make(mNumEvidences);
        mNegLogCombinedEffectiveSignificances = fac1d.make(mNumElements);
        mColumnwiseDividerAffected = new ColumnwiseDivider(mDistNormAffected);
        mColumnwiseDividerUnaffected = new ColumnwiseDivider(mDistNormUnaffected);
    }
    

    
    // a significance value of "-1" denotes missing data; this is more storage friendly than using null
    // Double object references
    private void calculateCostFunction(DoubleMatrix2D pSignificances,
                                       boolean []pAffectedElements,
                                       double pQuantileFraction,
                                       EvidenceWeightType pWeightType,
                                       IterationResults pIterationResults)
    {
        int i = 0;  // index for elements
        int j = 0;  // index for evidences
        int k = 0;  // index for histogram bin
        
        if(pQuantileFraction <= 0.0 || pQuantileFraction >= 1.0)
        {
            throw new IllegalStateException("invalid quantile value: " + pQuantileFraction);
        }
        
        int numElements = pAffectedElements.length;
        if(mNumElements != numElements)
        {
            throw new IllegalArgumentException("number of elements in boolean array passed to iterate() does not match stored number of elements");
        }

        int numEvidences = pSignificances.columns();
        if(mNumEvidences != numEvidences)
        {
            throw new IllegalArgumentException("number of columns in the significances matrix does not match the stored number of evidence types");
        }
        
        // clear significances distributions for the affected & unaffected elements
        mDistributionSignificancesAffected.assign(0.0);
        mDistributionSignificancesUnaffected.assign(0.0);
        mDistNormAffected.assign(0.0);
        mDistNormUnaffected.assign(0.0);
        mEffectiveSignificanceNormalizations.assign(0.0);
        
        double sigVal = 0.0;
        int numBins = mNumBins;
        double numBinsDouble = (double) mNumBins;
        double count = 0.0;
        boolean affected = false;
        DoubleMatrix2D dist = null;
        int numAffected = 0;
        
        DoubleMatrix1D distNorm = null;
        
        // count the number of putative affected elements, and calculate the
        // normalization factors for the distributions of affected and unaffected
        // element significances for each type of evidence; calculate the histograms
        // of affected and unaffected element significances for each evidence type
        for(i = numElements; --i >= 0; )
        {
            affected = pAffectedElements[i];
            if(affected)
            {
                ++numAffected;
            }
            
            for(j = numEvidences; --j >= 0; )
            {
                sigVal = pSignificances.get(i, j);
                if(sigVal >= 0.0)
                {
                    k = (int) (numBinsDouble * sigVal);
                    if(k == mNumBins)
                    {
                        k--;
                    }
                    if(affected)
                    {
                        dist = mDistributionSignificancesAffected;
                        distNorm = mDistNormAffected;
                    }
                    else
                    {
                        dist = mDistributionSignificancesUnaffected;
                        distNorm = mDistNormUnaffected;
                    }
                    count = dist.get(k, j) + 1.0;
                    dist.set(k, j, count);   
                    
                    count = distNorm.get(j) + 1.0;
                    distNorm.set(j, count);
                }
                else
                {
                    // do nothing
                }
            }
        }
        
        // make sure that each evidence has a nonzero number of affected and unaffected elements
        for(j = numEvidences; --j >= 0; )
        {
            if(mDistNormAffected.get(j) <= 0.0)
            {
                throw new IllegalArgumentException("evidence type has no affected elements: " + j);
            }
            if(mDistNormUnaffected.get(j) <= 0.0)
            {
                throw new IllegalArgumentException("evidence type has no unaffected elements: " + j);
            }
        }
        
        if(0 == numAffected)
        {
            throw new IllegalArgumentException("there are no affected elements");
        }

        int numUnaffected = numElements - numAffected;
        if(0 == numUnaffected)
        {
            throw new IllegalArgumentException("zero unaffected elements");
        }

        // normalize distributions based on number of elements in each distribution
        mDistributionSignificancesAffected.forEachNonZero(mColumnwiseDividerAffected);
        mDistributionSignificancesUnaffected.forEachNonZero(mColumnwiseDividerUnaffected);

        mDistributionSignificancesDifference.assign(mDistributionSignificancesAffected);
        mDistributionSignificancesDifference.assign(mDistributionSignificancesUnaffected,
                                                    Functions.minus);

        // for each evidence type, calculate the weight
        double cumulativeSum = 0.0;
        double cumulativeMax = 0.0;
        for(j = numEvidences; --j >= 0; )
        {
            cumulativeSum = 0.0;
            cumulativeMax = 0.0;
            for(k = 0; k < numBins; ++k)
            {
                cumulativeSum += mDistributionSignificancesDifference.get(k, j);
                if(cumulativeSum > cumulativeMax)
                {
                    cumulativeMax = cumulativeSum;
                }
            }
            mWeights.set(j, cumulativeMax);
        }

        double biasesSum = 0.0;
        double weightsSum = 0.0;
        
        // if necessary, calculate the biases
        if(pWeightType.equals(EvidenceWeightType.LINEAR))
        {
            // compute un-normalized biases
            mBiases.assign(mWeights);
            mBiases.assign(Functions.minus(1.0));
            mBiases.assign(Functions.mult(-1.0));
            // normalize the biases
            biasesSum = mBiases.zSum();
            mBiases.assign(Functions.mult(1.0/biasesSum));

        }
        else if(pWeightType.equals(EvidenceWeightType.POWER))
        {
            mBiases.assign(0.0);
        }
        else
        {
            throw new IllegalArgumentException("unknown weight type: " + pWeightType.toString());
        }
      
        // normalize the weights
        weightsSum = mWeights.zSum();
        mWeights.assign(Functions.mult(1.0/weightsSum));
        
        double bias = 0.0;
        double weight = 0.0;
        double effSig = 0.0;
        double sigNorm = 0.0;

        int numUnaffectedForEvidence = 0;

        // calculate all effective significances
        for(j = numEvidences; --j >= 0; )
        {
            bias = mBiases.get(j);
            weight = mWeights.get(j);
            sigNorm = 0.0;
            numUnaffectedForEvidence = 0;
            
            for(i = numElements; --i >= 0; )
            {
                affected = pAffectedElements[i];
                
                sigVal = pSignificances.get(i, j);
                if(sigVal > 0.0)
                {
                    if(pWeightType.equals(EvidenceWeightType.LINEAR))
                    {
                        // calculate the effective significance for element i and evidence type j
                        effSig = bias + weight*sigVal;
                    }
                    else if(pWeightType.equals(EvidenceWeightType.POWER))
                    {
                        effSig = Math.pow(sigVal, weight);
                    }
                    else
                    {
                        throw new IllegalArgumentException("invalid weight type: " + pWeightType);
                    }
                    mEffectiveSignificances.set(i, j, effSig); 
                    if(! affected)
                    {
                        sigNorm += effSig;
                        ++numUnaffectedForEvidence;
                    }
                }
                else if(sigVal < 0.0)
                {
                    mEffectiveSignificances.set(i, j, sigVal);
                }
                else
                {
                    throw new IllegalStateException("zero value of significance for element " + i + " and evidence " + j);
                }
            }

            if(0 == numUnaffectedForEvidence)
            {
                throw new IllegalArgumentException("for evidence number " + j + ", there were no putatively unaffected network elements");
            }
            
            // calculate the evidence-specific normalization for the effective significance
            sigNorm /= (double) numUnaffectedForEvidence;
            
            mEffectiveSignificanceNormalizations.set(j, sigNorm);
        }
            
//        System.out.println("weights: " + mWeights.toString());
        
        double avgProd = 0.0;
        double prod = 1.0;
        
        // calculate the average of the product of effective significances, over the putative unaffected set
        // (this is ONLY used in calculating the alpha parameter)
        for(i = numElements; --i >= 0; )
        {
            affected = pAffectedElements[i];
            prod = 1.0;
            if(! affected)
            {
                for(j = numEvidences; --j >= 0; )
                {
                    sigVal = mEffectiveSignificances.get(i, j);
                    if(sigVal >= 0.0)
                    {
                        prod *= sigVal;
                    }
                    else
                    {
                        prod *= mEffectiveSignificanceNormalizations.get(j); 
                    }
                }
                avgProd += prod;
            }
        }
        avgProd /= numUnaffected;        
        
        double prodAvg = 1.0;
        
        // normalize the effective significances;
        // compute the product of averages of effective significances of putatively unaffected elements
        for(j = numEvidences; --j >= 0; )
        {
            sigNorm = mEffectiveSignificanceNormalizations.get(j);
            prodAvg *= sigNorm;
            
            for(i = numElements; --i >= 0; )
            {
                effSig = mEffectiveSignificances.get(i, j);
                if(effSig > 0.0)
                {
                    effSig /= sigNorm;
                    mEffectiveSignificances.set(i, j, effSig);
                }
                else
                {
                    // leave the effective significance as -1.0 to indicate missing data
                }
            }
        }
           
        double alphaParam = avgProd/prodAvg;

        boolean atLeastOneSignificanceIsDefined = false;
        double combinedEffectiveSignificance = 0.0;
        double negLogCombinedEffectiveSignificance = 0.0;
        
        // compute the combined effective significance for each element, and the
        // max combined significance of affected elements
        for(i = numElements; --i >= 0; )
        {
            atLeastOneSignificanceIsDefined = false;
            combinedEffectiveSignificance = 1.0;
            
            // combine the significance values of all evidences, for this element
            for(j = numEvidences; --j >= 0; )
            {
                effSig = mEffectiveSignificances.get(i, j);
                if(effSig >= 0.0)
                {
                    combinedEffectiveSignificance *= effSig;
                    atLeastOneSignificanceIsDefined = true;
                }
                else
                {
                    // Just assume the un-normalized effective significance is equal to the
                    // average un-normalized effective significance for unaffected elements of
                    // this evidence type, which would mean a value of 1.0 for the normalized effective
                    // significance.  So, we have no need to mutiply the combinedSig by anything here. 
                }
            }
            if(! atLeastOneSignificanceIsDefined)
            {
                throw new IllegalArgumentException("no evidences were available for element number: " + i);
            }

            if(combinedEffectiveSignificance <= 0.0)
            {
                throw new IllegalStateException("non-positive combined significance");
            }
            
            negLogCombinedEffectiveSignificance = -2.0 * Math.log(combinedEffectiveSignificance);
            
            // save the combined effective significance
            mNegLogCombinedEffectiveSignificances.set(i, negLogCombinedEffectiveSignificance);
        }
        
        double maxCombinedSigAffected = 0.0;
        double minCombinedSigAffected = Double.MAX_VALUE;
        mAffectedSignificances.assign(Double.MIN_VALUE);
        mUnaffectedSignificances.assign(Double.MIN_VALUE);
        int affectedIndex = numUnaffected;
        int unaffectedIndex = numAffected;

        for(i = numElements; --i >= 0; )
        {
            combinedEffectiveSignificance = mNegLogCombinedEffectiveSignificances.get(i);
            
            // calculate the maximum combined effective significance of affected elements 
            if(pAffectedElements[i] && combinedEffectiveSignificance > maxCombinedSigAffected)
            {
                maxCombinedSigAffected = combinedEffectiveSignificance;
            }
            
            // calculate the minimum combined effective significance of affected elements
            if(pAffectedElements[i] && combinedEffectiveSignificance < minCombinedSigAffected)
            {
                minCombinedSigAffected = combinedEffectiveSignificance;
            }
            
            affected = pAffectedElements[i];
            if(affected)
            {
                mAffectedSignificances.set(affectedIndex, combinedEffectiveSignificance);
                ++affectedIndex;
            }
            else
            {
                mUnaffectedSignificances.set(unaffectedIndex, combinedEffectiveSignificance);
                ++unaffectedIndex;
            }
        }
        
        Sorting sorting = Sorting.quickSort;
        
        // obtain a sorted view of the combined significances (ascending order)
        DoubleMatrix1D sortedAffectedSignificances = sorting.sort(mAffectedSignificances); 
        DoubleMatrix1D sortedUnaffectedSignificances = sorting.sort(mUnaffectedSignificances);
        
        int affectedQuantile = numUnaffected + ((int) (pQuantileFraction * ((double) numAffected)));
        int unaffectedQuantile = numAffected + ((int) ((1.0 - pQuantileFraction) * ((double) numUnaffected)));
        
        double affectedQuantileValue = sortedAffectedSignificances.get(affectedQuantile);
        double unaffectedQuantileValue = sortedUnaffectedSignificances.get(unaffectedQuantile);

        mDistributionCombinedSignificancesAffected.assign(0.0);
        mDistributionCombinedSignificancesUnaffected.assign(0.0);
        
        DoubleMatrix1D distCombined = null;

        if(maxCombinedSigAffected <= minCombinedSigAffected)
        {
            throw new IllegalStateException("max combined sig affected must exceed min combined sig affected");
        }
        
        double binSize = (maxCombinedSigAffected - minCombinedSigAffected)/((double) mNumBins);
        
        // construct the distributions of combined significances of affected & unaffected elements
        for(i = 0; i < numElements; ++i)
        {
            affected = pAffectedElements[i];
            
            combinedEffectiveSignificance = mNegLogCombinedEffectiveSignificances.get(i);
            
            if(combinedEffectiveSignificance >= minCombinedSigAffected && 
               combinedEffectiveSignificance <= maxCombinedSigAffected)
            {
                k = (int) ((combinedEffectiveSignificance - minCombinedSigAffected)/binSize);
                
                if(k == mNumBins)
                {
                    k--;
                }
                
                if(affected)
                {
                    distCombined = mDistributionCombinedSignificancesAffected;

                }
                else
                {
                    distCombined = mDistributionCombinedSignificancesUnaffected;

                }

                distCombined.set(k, distCombined.get(k) + 1.0);                
            }
        }

//        for(i = 0; i < numElements; ++i)
//        {
//            System.out.println("combinedSig[" + i + "] = " + pCombinedEffectiveSignificances[i] + "; aff: " + pAffectedElements[i]);
//        }
        
        double normAffected = 1.0 / ((double) numAffected);
        mDistributionCombinedSignificancesAffected.assign(Functions.mult(normAffected));
//        System.out.println("dist of affected significances: " + mDistributionCombinedSignificancesAffected);

        double normUnaffected = 1.0 / ((double) numUnaffected);
        mDistributionCombinedSignificancesUnaffected.assign(Functions.mult(normUnaffected));      
        //System.out.println("dist of unaffected significances: " + mDistributionCombinedSignificancesUnaffected);
        
        mDistributionCombinedSignificancesDifference.assign(mDistributionCombinedSignificancesAffected);
        mDistributionCombinedSignificancesDifference.assign(mDistributionCombinedSignificancesUnaffected,
                                                         Functions.minus);       
        
        //System.out.println("difference of distributions of combined significances: " + mDistributionCombinedSignificancesDifference);
        
        cumulativeSum = 0.0;
        double maxCumulativeSum = 0.0;
        double countDouble = 0.0;
        double criticalSignificance = 0.0;

        // determine the "critical significance" and separation between the distributions
        for(k = numBins; --k >= 0; )
        {
            countDouble = mDistributionCombinedSignificancesDifference.get(k);
            cumulativeSum += countDouble;
            
            if(cumulativeSum > maxCumulativeSum)
            {
                maxCumulativeSum = cumulativeSum;
                criticalSignificance = minCombinedSigAffected + (((double) k) + 0.5) * binSize;
            }
        }
        
        // guard against roundoff error (objective function can never exceed 1.0)
        double objFunction = Math.min(1.0, maxCumulativeSum);
        
        pIterationResults.mSignificanceDistributionSeparation = objFunction;
        pIterationResults.mAlphaParameter = alphaParam;
        pIterationResults.mCriticalSignificance = criticalSignificance;
        //System.out.println("critical significance: " + criticalSignificance);
        pIterationResults.mLowQuantileSignificanceAffected = affectedQuantileValue;
        //System.out.println("lower quantile of affected significances: " + affectedQuantileValue);
        pIterationResults.mHighQuantileSignificanceUnaffected = unaffectedQuantileValue;
        //System.out.println("upper quantile of unaffected significances: " + unaffectedQuantileValue);
        //System.out.println("cost function: " + objFunction);
    }
    

    
    
    public void findAffectedElements(DoubleMatrix2D pSignificances,
                                     int pNumBins,
                                     double pInitialCutoff,
                                     double pCombinedSignificanceCutoff,
                                     double pFractionToRemove,
                                     double pMinFractionalCostChange,
                                     double pSmoothingLength,
                                     EvidenceWeightType pWeightType,
                                     EvidenceWeightedInfererResults pRetResults) throws AccuracyException
    {
        int numElements = pSignificances.rows();
        if(numElements == 0)
        {
            throw new IllegalArgumentException("no elements in significances array");
        }
        int numEvidences = pSignificances.columns();
        if(numEvidences == 0)
        {
            throw new IllegalArgumentException("no evidences in significances array");
        }
        if(pNumBins <= MIN_NUM_BINS)
        {
            throw new IllegalArgumentException("invalid number of bins");
        }
        if(mNumBins != pNumBins || mNumElements != numElements || mNumEvidences != numEvidences)
        {
            initialize(numEvidences, pNumBins, numElements);
        }
        if(pMinFractionalCostChange <= 0.0 || pMinFractionalCostChange >= 1.0)
        {
            throw new IllegalArgumentException("illegal min fractional cost change parameter: " + pMinFractionalCostChange);
        }
        boolean []affectedElements = pRetResults.mAffectedElements;
        if(null == affectedElements)
        {
            throw new IllegalArgumentException("missing return array for affected elements flags");
        }
        if(affectedElements.length != numElements)
        {
            throw new IllegalArgumentException("illegal array size for affected elements flags");
        }
        double []combinedEffectiveSignificances = pRetResults.mCombinedEffectiveSignificances;
        if(null == combinedEffectiveSignificances)
        {
            throw new IllegalArgumentException("missing return array for combined effective significances");
        }
        if(combinedEffectiveSignificances.length != numElements)
        {
            throw new IllegalArgumentException("illegal array size for combined effective significances");
        }
        if(pRetResults.mWeights.length != numEvidences)
        {
            throw new IllegalArgumentException("illegal array size for weights array");
        }
        if(pSmoothingLength <= 0.0)
        {
            throw new IllegalArgumentException("illegal smoothing length: " + pSmoothingLength);
        }
        if(pCombinedSignificanceCutoff < 0.0 || pCombinedSignificanceCutoff > 1.0)
        {
            throw new IllegalArgumentException("the combined significance cutoff must be between 0.0 and 1.0");
        }
        
        boolean affected = false;
        double combinedSig = 0.0;
        int i = 0;
        
        boolean iterationsComplete = false;
        int iterationCtr = 0;
        double funcValue = 0.0;
        
        if(pFractionToRemove<= 0.0 || pFractionToRemove >= 1.0)
        {
            throw new IllegalArgumentException("invalid fraction to remove: " + pFractionToRemove);
        }

        if(pInitialCutoff <= 0.0 || pInitialCutoff >= 1.0)
        {
            throw new IllegalArgumentException("illegal cutoff value: " + pInitialCutoff);
        }

        if(numElements != pSignificances.rows())
        {
            throw new IllegalArgumentException("number of rows of significances matrix not consistent with return array for affected elements");
        }
        
        double sigValue = 0.0;
        int j = 0;
        int numAffected = 0;
        
        // obtain sorted significances for each evidence type
        for(j = numEvidences; --j >= 0; )
        {
            mSignificancesSorted.viewColumn(j).assign(Sorting.mergeSort.sort(pSignificances.viewColumn(j)));
        }
        
        // calculate the minimum nonzero evidence-specific significances
        double minSig = 0.0;
        for(j = numEvidences; --j >= 0; )
        {
            minSig = Double.MAX_VALUE;
            for(i = numElements; --i >= 0; )
            {
                sigValue = pSignificances.get(i, j);
                if(sigValue > 0.0 && sigValue < minSig)
                {
                    minSig = sigValue;
                }
            }
            if(minSig == Double.MAX_VALUE)
            {
                throw new IllegalArgumentException("evidence type " + j + " has no non-zero significances; cannot proceed");
            }
            mMinimumNonzeroEvidenceSpecificSignificances.set(j, minSig);
        }
        
        // handle the special case of significances that are identically zero, by
        // setting them to the minimum nonzero evidence-specific significance value
        mNonzeroSignificances.assign(pSignificances);
        int quantileIndex = (int) (numElements * pInitialCutoff);
        double minSigQuantile = 0.0;
        for(j = numEvidences; --j >= 0; )
        {
            minSigQuantile = mSignificancesSorted.get(quantileIndex, j);
            minSig = mMinimumNonzeroEvidenceSpecificSignificances.get(j);
            
            if(minSig > minSigQuantile)
            {
                throw new IllegalArgumentException("the lowest nonzero significance for evidence number " + j + " is above your initial cutoff; please increase your initial cutoff, or change the zero significances to small positive values.");
            }
            for(i = numElements; --i >= 0; )
            {
                sigValue = mNonzeroSignificances.get(i, j);
                if(0.0 == sigValue)
                {
                    mNonzeroSignificances.set(i, j, minSig);
                }
            }
        }
        
        for(j = numEvidences; --j >= 0; )
        {
            mSignificancesSorted.viewColumn(j).assign(Sorting.mergeSort.sort(mNonzeroSignificances.viewColumn(j)));
        }
        
        // determine the initial putative set of affected elements, using the "greedy" method
        for(i = numElements; --i >= 0; )
        {
            affected = false;
            for(j = numEvidences; (--j >= 0) && (! affected); )
            {
                sigValue = mNonzeroSignificances.get(i, j);
                if(sigValue >= 0.0 && sigValue <= mSignificancesSorted.get(quantileIndex, j))
                {
                    affected = true;
                    ++numAffected;
                }
            }
            affectedElements[i] = affected;
        }
        
        int numChanged = 0;
        
        double lastFuncValue = 0.0;
        boolean firstIteration = true;
        
        IterationResults iterationResults = new IterationResults();
        
        double affectedCutoff = 0.0;
        double affectedQuantile = 0.0;
        double unaffectedQuantile = 0.0;
        double criticalSignificance = 0.0;
        double funcValueDeriv = 0.0;
        double fractionToRemove = pFractionToRemove;
        double lastFractionRemoved = 0.0;
        double deriv = 0.0;
        
        double negLogCombinedEffectiveSignificanceCutoff = -2.0 * Math.log(pCombinedSignificanceCutoff);
        
        while(! iterationsComplete)
        {
            ++iterationCtr;

//            System.out.println("calling cost function with frac to remove: " + fractionToRemove);
             // calculate the combined effective significances for all elements
             calculateCostFunction(mNonzeroSignificances, 
                                   affectedElements, 
                                   fractionToRemove, 
                                   pWeightType,
                                   iterationResults);

             criticalSignificance = iterationResults.mCriticalSignificance;
             affectedQuantile = iterationResults.mLowQuantileSignificanceAffected;
             unaffectedQuantile = iterationResults.mHighQuantileSignificanceUnaffected;
             
             // make sure we don't "scalp" too much of the data away, by ensuring that the
             // affected cutoff is never greater than pCombinedSignificanceCutoff
             affectedCutoff = Math.min(negLogCombinedEffectiveSignificanceCutoff, affectedQuantile);
                          
             funcValue = iterationResults.mSignificanceDistributionSeparation;
             if(funcValue <= 0.0 || funcValue > 1.0)
             {
                 throw new IllegalStateException("illegal value for objective function: " + funcValue); 
             }
             
             if(! firstIteration)
             {
                 if(Math.abs(funcValue - lastFuncValue)/funcValue < pMinFractionalCostChange  || 
                         1.0 - funcValue < pMinFractionalCostChange)
                 {
                     //System.out.println("quitting because L(H) changed by a really small amount, number of iterations: " + iterationCtr);
                     iterationsComplete = true;
                 }
                 else
                 {
                     deriv = (funcValue - lastFuncValue)/lastFractionRemoved;
                     if(deriv > 0.0)
                     {
                         fractionToRemove = 0.5*(1.0 - funcValue)/deriv;
//                         System.out.println("estimated fraction to remove next: " + fractionToRemove);
                         
                         // make sure that the fraction to remove is at least 1.0/((double) numAffected)
                         fractionToRemove = Math.max(1.0/((double) numAffected), fractionToRemove);

                         // make sure that the fraction to remove never exceeds last fraction to remove
                         fractionToRemove = Math.min(lastFractionRemoved, fractionToRemove);
                     }
                     else
                     {
                         // remove a smaller fraction than last time
                         fractionToRemove = 0.5 * lastFractionRemoved;
                     }
                 }
             }
             else
             {
                 firstIteration = false;
             }

             numChanged = 0;
             
             if(! iterationsComplete)
             {
                 for(i = numElements; --i >= 0; )
                 {
                     affected = affectedElements[i];
                     combinedSig = mNegLogCombinedEffectiveSignificances.get(i);
                     
                     // if the significance value is less than the cutoff, move it to the 
                     // "unaffected" set of elements
                     if(affected && combinedSig <= affectedCutoff)
                     {
                         affectedElements[i] = false;
                         ++numChanged;
                     }
                 }
                 numAffected -= numChanged;
                 if(numChanged == 0 || numAffected <= 0)
                 {
                     //System.out.println("quitting because number changed is zero, number of iterations: " + iterationCtr);
                     iterationsComplete = true;
                 }                 
                 //System.out.println("iteration: " + iterationCtr + "; L(H): " + funcValue + "; numChanged: " + numChanged + "; num aff: " + numAffected);

                 lastFractionRemoved = ((double) numChanged)/((double) (numAffected + numChanged));
                 lastFuncValue = funcValue;
             }
        }
        
        double unaffectedCutoff = Math.min(affectedCutoff, criticalSignificance);
        
        int numProbableFalseNegatives = 0;
        
        for(i = numElements; --i >= 0; )
        {
            affected = affectedElements[i];
            if(! affected)
            {
                combinedSig = mNegLogCombinedEffectiveSignificances.get(i);
                
                // if the significance value is greater than the cutoff, move the element
                // to the set of "affected" elements
                if(combinedSig > unaffectedCutoff)
                {
                    affectedElements[i] = true;
                    ++numProbableFalseNegatives;
                    ++numAffected;
                }
            }
        }
//        System.out.println("num false negatives re-added to affected set: " + numProbableFalseNegatives);
        
        
        if(numProbableFalseNegatives > 0)
        {
            // recompute the objective function and quantile values
            calculateCostFunction(pSignificances, 
                                  affectedElements, 
                                  pFractionToRemove,
                                  pWeightType,
                                  iterationResults);
        }

        Double []allSignificances = new Double[numElements];
        Double []unaffectedSignificances = new Double[numElements - numAffected];
        int unaffCtr = 0;
        double negLogCombinedEffectiveSignificance = 0.0;
        for(i = 0; i < numElements; ++i)
        {
            negLogCombinedEffectiveSignificance = mNegLogCombinedEffectiveSignificances.get(i);
            allSignificances[i] = new Double(negLogCombinedEffectiveSignificance);
            affected = affectedElements[i];
            if(! affected)
            {
                unaffectedSignificances[unaffCtr] = new Double(negLogCombinedEffectiveSignificance);
                ++unaffCtr;
            }
        }
        
        boolean singleTailed = true;
        mSignificanceCalculator.calculateSignificancesCDF(allSignificances, 
                                                       unaffectedSignificances,
                                                       mNumBins,
                                                       singleTailed,
                                                       pSmoothingLength,
                                                       mSignificanceCalculationResults);
        
        double []cumulativeSignificances = mSignificanceCalculationResults.mSignificances;
        for(i = numElements; --i >= 0; )
        {
         //   System.out.println("i: " + i + "; allSig: " + allSignificances[i] + "; affected: " + affectedElements[i] + "; finalSig: " + cumulativeSignificances[i]);
            pRetResults.mCombinedEffectiveSignificances[i] = cumulativeSignificances[i];
        }
        pRetResults.mSignificanceDistributionSeparation = iterationResults.mSignificanceDistributionSeparation;
        pRetResults.mAlphaParameter = iterationResults.mAlphaParameter;
        pRetResults.mNumIterations = iterationCtr;
        pRetResults.mNumAffected = numAffected;
        mWeights.toArray(pRetResults.mWeights);
    }
    
    public void findAffectedElements(DoubleMatrix2D pSignificances,
                                     EvidenceWeightedInfererParams pParams,
                                     EvidenceWeightedInfererResults pResults) throws AccuracyException
    {
        Integer numBinsObj = pParams.getNumBins();
        if(null == numBinsObj)
        {
            throw new IllegalArgumentException("missing parameter: num bins");
        }
        int numBins = numBinsObj.intValue();
        
        Double initialSignificanceCutoffObj = pParams.getInitialSignificanceCutoff();
        if(null == initialSignificanceCutoffObj)
        {
            throw new IllegalArgumentException("missing parameter: initial significance cutoff");
        }
        double initialSignificanceCutoff = initialSignificanceCutoffObj.doubleValue();
        
        Double combinedSignificanceCutoffObj = pParams.getCombinedSignificanceCutoff();
        if(null == combinedSignificanceCutoffObj)
        {
            throw new IllegalArgumentException("missing parameter: combined significance cutoff");
        }
        double combinedSignificanceCutoff = combinedSignificanceCutoffObj.doubleValue();
        
        Double fractionToRemoveObj = pParams.getFractionToRemove();
        if(null == fractionToRemoveObj)
        {
            throw new IllegalArgumentException("missing parameter:  fraction to remove");
        }
        double fractionToRemove = fractionToRemoveObj.doubleValue();
        
        Double minFractionalCostChangeObj = pParams.getMinFractionalCostChange();
        if(null == minFractionalCostChangeObj)
        {
            throw new IllegalArgumentException("missing parameter:  min fractional cost change");
        }
        double minFractionalCostChange = minFractionalCostChangeObj.doubleValue();
        
        Double smoothingLengthObj = pParams.getSmoothingLength();
        if(null == smoothingLengthObj)
        {
            throw new IllegalArgumentException("missing parameter:  smoothing length");
        }
        double smoothingLength = smoothingLengthObj.doubleValue();
        
        EvidenceWeightType weightType = pParams.getEvidenceWeightType();
        if(null == weightType)
        {
            throw new IllegalArgumentException("missing parameter:  weight type");
        }
        
        findAffectedElements(pSignificances,
                numBins,
                initialSignificanceCutoff,
                combinedSignificanceCutoff,
                fractionToRemove,
                minFractionalCostChange,
                smoothingLength,
                weightType,
                pResults);
    }
    
    public EvidenceWeightedInfererResults findAffectedElements(DoubleMatrix2D pSignificances,
                                                               EvidenceWeightedInfererParams pParams) throws AccuracyException
    {
        EvidenceWeightedInfererResults results = new EvidenceWeightedInfererResults();
        int numElements = pSignificances.rows();
        boolean []affectedElements = new boolean[numElements];
        double []combinedEffectiveSignificances = new double[numElements];
        results.mCombinedEffectiveSignificances = combinedEffectiveSignificances;
        results.mAffectedElements = affectedElements;
        int numEvidences = pSignificances.columns();
        results.mWeights = new double[numEvidences];
        
        findAffectedElements(pSignificances,
                             pParams,
                             results);
        
        return results;
    }
}
