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
import org.systemsbiology.math.DoubleVector;

/**
 * An implementation of the Pointillist algorithm for
 * inferring the set of probable affected elements.  Uses a Bayesian
 * method to compute the joint probability that an element would
 * have a given set of significance values, by chance, if it were
 * not in the set of true affected elements.  This algorithm was
 * designed by Daehee Hwang.
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

    private DoubleMatrix1D mDistributionJointSignificances;
    private DoubleMatrix1D mDistributionJointSignificancesAffected;
    private DoubleMatrix1D mDistributionJointSignificancesUnaffected;
    private DoubleMatrix1D mDistributionJointSignificancesDifference;
    private DoubleMatrix1D mEffectiveSignificanceNormalizations;
    private DoubleMatrix1D mAffectedSignificances;
    private DoubleMatrix1D mUnaffectedSignificances;
    
    private DoubleMatrix2D mEffectiveSignificances;
    private DoubleMatrix1D mDistNormAffected;
    private DoubleMatrix1D mDistNormUnaffected;
    
    private ColumnwiseDivider mColumnwiseDividerAffected;
    private ColumnwiseDivider mColumnwiseDividerUnaffected;
    
    private int mNumBins;
    private int mNumEvidences;
    private int mNumElements;
    private DoubleMatrix1D mWeights;
    private DoubleMatrix1D mBiases;
    
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
        public double mHighQuantileSignificanceAffected;
        public double mLowQuantileSignificanceUnaffected;
        public double mAlphaParameter;
    }
    
    public EvidenceWeightedInferer()
    {
        mNumEvidences = 0;
        mNumBins = 0;
        mNumElements = 0;
    }
    
    private void initialize(int pNumEvidences, int pNumBins, int pNumElements)
    {
        mNumEvidences = pNumEvidences;
        mNumBins = pNumBins;
        mNumElements = pNumElements;
        
        DoubleFactory2D fac2d = DoubleFactory2D.dense;
        
        mDistributionSignificancesAffected = fac2d.make(mNumBins, mNumEvidences);
        mDistributionSignificancesUnaffected = fac2d.make(mNumBins, mNumEvidences);
        mDistributionSignificancesDifference = fac2d.make(mNumBins, mNumEvidences);
        
        mEffectiveSignificances = fac2d.make(mNumElements, mNumEvidences);
        
        DoubleFactory1D fac1d = DoubleFactory1D.dense;

        mWeights = fac1d.make(mNumEvidences);
        mBiases = fac1d.make(mNumEvidences);

        mDistributionJointSignificances = fac1d.make(mNumBins);
        mDistributionJointSignificancesAffected = fac1d.make(mNumBins);
        mDistributionJointSignificancesUnaffected = fac1d.make(mNumBins);
        mDistributionJointSignificancesDifference = fac1d.make(mNumBins);
        mAffectedSignificances = fac1d.make(mNumElements);
        mUnaffectedSignificances = fac1d.make(mNumElements);
        
        mDistNormAffected = fac1d.make(mNumEvidences);
        mDistNormUnaffected = fac1d.make(mNumEvidences);
        mEffectiveSignificanceNormalizations = fac1d.make(mNumEvidences);
        mColumnwiseDividerAffected = new ColumnwiseDivider(mDistNormAffected);
        mColumnwiseDividerUnaffected = new ColumnwiseDivider(mDistNormUnaffected);
}
    

    
    // a significance value of "-1" denotes missing data; this is more storage friendly than using null
    // Double object references
    private void calculateCostFunction(DoubleMatrix2D pSignificances,
                                       boolean []pAffectedElements,
                                       double pQuantileFraction,
                                       EvidenceWeightType pWeightType,
                                       double []pJointEffectiveSignificances,
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
        DoubleVector.zeroElements(pJointEffectiveSignificances);
        mEffectiveSignificanceNormalizations.assign(0.0);
        
        double sigVal = 0.0;
        int numBins = mNumBins;
        double numBinsDouble = (double) mNumBins;
        double count = 0.0;
        boolean affected = false;
        DoubleMatrix2D dist = null;
        int numAffected = 0;
        
        DoubleMatrix1D distNorm = null;
        
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
                if(sigVal >= 0.0)
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
                else
                {
                    mEffectiveSignificances.set(i, j, sigVal);
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
        double maxJointSigAffected = 0.0;
        double minJointSigAffected = Double.MAX_VALUE;
        double jointSig = 0.0;
        mAffectedSignificances.assign(-1.0);
        mUnaffectedSignificances.assign(-1.0);
        int affectedIndex = numUnaffected;
        int unaffectedIndex = numAffected;
        
        // compute the joint effective significance for each element, and the
        // max joint significance of affected elements
        for(i = numElements; --i >= 0; )
        {
            atLeastOneSignificanceIsDefined = false;
            jointSig = 1.0;
            for(j = numEvidences; --j >= 0; )
            {
                effSig = mEffectiveSignificances.get(i, j);
                if(effSig >= 0.0)
                {
                    jointSig *= effSig;
                    atLeastOneSignificanceIsDefined = true;
                }
                else
                {
                    // Just assume the un-normalized effective significance is equal to the
                    // average un-normalized effective significance for unaffected elements of
                    // this evidence type, which would mean a value of 1.0 for the normalized effective
                    // significance.  So, we have no need to mutiply the jointSig by anything here. 
                }
            }
            if(! atLeastOneSignificanceIsDefined)
            {
                throw new IllegalArgumentException("no evidences were available for element number: " + i);
            }
            
            if(jointSig < 0.0)
            {
                throw new IllegalStateException("negative joint significance");
            }

            // save the joint effective significance
            pJointEffectiveSignificances[i] = jointSig;
            
            // calculate the maximum joint effective significance of affected elements
            if(pAffectedElements[i] && jointSig > maxJointSigAffected)
            {
                maxJointSigAffected = jointSig;
            }
            
            // calculate the minimum joint effective significance of affected elements
            if(pAffectedElements[i] && jointSig < minJointSigAffected)
            {
                minJointSigAffected = jointSig;
            }
            
            affected = pAffectedElements[i];
            if(affected)
            {
                mAffectedSignificances.set(affectedIndex, jointSig);
                ++affectedIndex;
            }
            else
            {
                mUnaffectedSignificances.set(unaffectedIndex, jointSig);
                ++unaffectedIndex;
            }
        }
        
        Sorting sorting = Sorting.quickSort;
        
        // obtain a sorted view of the joint significances (ascending order)
        DoubleMatrix1D sortedAffectedSignificances = sorting.sort(mAffectedSignificances); 
        DoubleMatrix1D sortedUnaffectedSignificances = sorting.sort(mUnaffectedSignificances);
        
        int affectedQuantile = numUnaffected + ((int) ((1.0 - pQuantileFraction) * ((double) numAffected)));
        int unaffectedQuantile = numAffected + ((int) (pQuantileFraction * ((double) numUnaffected)));
        
        double affectedQuantileValue = sortedAffectedSignificances.get(affectedQuantile);
        double unaffectedQuantileValue = sortedUnaffectedSignificances.get(unaffectedQuantile);

        mDistributionJointSignificancesAffected.assign(0.0);
        mDistributionJointSignificancesUnaffected.assign(0.0);
        
        maxJointSigAffected *= 2.0;
        
        DoubleMatrix1D distJoint = null;
        
        double binSize = maxJointSigAffected / ((double) mNumBins);
        
        // construct the distributions of joint significances of affected & unaffected elements
        for(i = 0; i < numElements; ++i)
        {
            affected = pAffectedElements[i];
            
            jointSig = pJointEffectiveSignificances[i];
            
            // Each element has a non-negative joint significance defined, so we
            // don't have to worry about missing data, at this point.  If an element
            // had no observations, an exception would have been thrown earlier in the code
            k = (int) (jointSig / binSize);
            if(k < mNumBins)
            {
                if(affected)
                {
                    distJoint = mDistributionJointSignificancesAffected;

                }
                else
                {
                    distJoint = mDistributionJointSignificancesUnaffected;

                }

                distJoint.set(k, distJoint.get(k) + 1.0);
            }
        }
        
//        System.out.println("joint effective significances: " + pJointEffectiveSignificances.toString());
        
        double normAffected = 1.0 / ((double) numAffected);
        mDistributionJointSignificancesAffected.assign(Functions.mult(normAffected));
//        System.out.println("dist of affected significances: " + mDistributionJointSignificancesAffected);

        double normUnaffected = 1.0 / ((double) numUnaffected);
        mDistributionJointSignificancesUnaffected.assign(Functions.mult(normUnaffected));      
        //System.out.println("dist of unaffected significances: " + mDistributionJointSignificancesUnaffected);
        
        mDistributionJointSignificancesDifference.assign(mDistributionJointSignificancesAffected);
        mDistributionJointSignificancesDifference.assign(mDistributionJointSignificancesUnaffected,
                                                         Functions.minus);       
        
        //System.out.println("difference of distributions of joint significances: " + mDistributionJointSignificancesDifference);
        
        cumulativeSum = 0.0;
        double maxCumulativeSum = 0.0;
        double countDouble = 0.0;
        double criticalSignificance = 0.0;

        for(k = 0; k < numBins; ++k )
        {
            countDouble = mDistributionJointSignificancesDifference.get(k);
            cumulativeSum += countDouble;
            
            if(cumulativeSum > maxCumulativeSum)
            {
                maxCumulativeSum = cumulativeSum;
                criticalSignificance = (((double) k) + 0.5) * binSize;
            }
        }
        
        double objFunction = Math.min(1.0, maxCumulativeSum);
        
        pIterationResults.mSignificanceDistributionSeparation = objFunction;
        pIterationResults.mAlphaParameter = alphaParam;
        pIterationResults.mCriticalSignificance = criticalSignificance;
//        System.out.println("critical significance: " + criticalSignificance);
        pIterationResults.mHighQuantileSignificanceAffected = affectedQuantileValue;
//        System.out.println("upper quantile of affected significances: " + affectedQuantileValue);
        pIterationResults.mLowQuantileSignificanceUnaffected = unaffectedQuantileValue;
//        System.out.println("lower quantile of unaffected significances: " + unaffectedQuantileValue);
    }
    
    public void findAffectedElements(DoubleMatrix2D pSignificances,
                                     int pNumBins,
                                     double pInitialCutoff,
                                     double pJointSignificanceCutoff,
                                     double pFractionToRemove,
                                     double pMinFractionalCostChange,
                                     EvidenceWeightType pWeightType,
                                     EvidenceWeightedInfererResults pRetResults)
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
        double []jointEffectiveSignificances = pRetResults.mJointEffectiveSignificances;
        if(null == jointEffectiveSignificances)
        {
            throw new IllegalArgumentException("missing return array for joint effective significances");
        }
        if(jointEffectiveSignificances.length != numElements)
        {
            throw new IllegalArgumentException("illegal array size for joint effective significances");
        }
        if(pRetResults.mWeights.length != numEvidences)
        {
            throw new IllegalArgumentException("illegal array size for weights array");
        }
                
        boolean affected = false;
        double jointSig = 0.0;
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
        
        if(pJointSignificanceCutoff <= 0.0 || pJointSignificanceCutoff >= 1.0)
        {
            throw new IllegalArgumentException("illegal affected cutoff value: " + pJointSignificanceCutoff);
        }
        if(numElements != pSignificances.rows())
        {
            throw new IllegalArgumentException("number of rows of significances matrix not consistent with return array for affected elements");
        }
        
        double sigValue = 0.0;
        int j = 0;
        int numAffected = 0;
        
        // determine the initial putative set of affected elements, using the "greedy" method
        for(i = numElements; --i >= 0; )
        {
            affected = false;
            for(j = numEvidences; (--j >= 0) && (! affected); )
            {
                sigValue = pSignificances.get(i, j);
                if(sigValue >= 0.0 && sigValue < pInitialCutoff)
                {
                    affected = true;
                    ++numAffected;
                }
            }
            affectedElements[i] = affected;
        }
//        System.out.println("initial number putatively affected: " + numAffected);
        
        int numChanged = 0;
        
        double lastFuncValue = 0.0;
        boolean firstIteration = true;
        
        IterationResults iterationResults = new IterationResults();
        
        double affectedCutoff = 0.0;
        double unaffectedCutoff = 0.0;
        double affectedQuantile = 0.0;
        double unaffectedQuantile = 0.0;
        double criticalSignificance = 0.0;
        double funcValueDeriv = 0.0;
        double fractionToRemove = pFractionToRemove;
        double lastFractionRemoved = 0.0;
        double deriv = 0.0;
        
        while(! iterationsComplete)
        {
            ++iterationCtr;

//            System.out.println("calling cost function with frac to remove: " + fractionToRemove);
             // calculate the joint effective significances for all elements
             calculateCostFunction(pSignificances, 
                                   affectedElements, 
                                   fractionToRemove, 
                                   pWeightType,
                                   jointEffectiveSignificances, 
                                   iterationResults);

             criticalSignificance = iterationResults.mCriticalSignificance;
             affectedQuantile = iterationResults.mHighQuantileSignificanceAffected;
             unaffectedQuantile = iterationResults.mLowQuantileSignificanceUnaffected;
             affectedCutoff = Math.max(pJointSignificanceCutoff, affectedQuantile);
             unaffectedCutoff = Math.min(affectedCutoff, criticalSignificance);
             
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
//                     System.out.println("quitting because L(H) changed by a really small amount, number of iterations: " + iterationCtr);
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
                     jointSig = jointEffectiveSignificances[i];
                     if(affected && jointSig >= affectedCutoff)
                     {
                         affectedElements[i] = false;
                         ++numChanged;
                     }
                 }
                 numAffected -= numChanged;
                 if(numChanged == 0 || numAffected <= 0)
                 {
//                     System.out.println("quitting because number changed is zero, number of iterations: " + iterationCtr);
                     iterationsComplete = true;
                 }                 
//                 System.out.println("iteration: " + iterationCtr + "; L(H): " + funcValue + "; numChanged: " + numChanged + "; num aff: " + numAffected);

                 lastFractionRemoved = ((double) numChanged)/((double) (numAffected + numChanged));
                 lastFuncValue = funcValue;
             }
        }
        
        criticalSignificance = iterationResults.mCriticalSignificance;
        affectedQuantile = iterationResults.mHighQuantileSignificanceAffected;
        unaffectedQuantile = iterationResults.mLowQuantileSignificanceUnaffected;
        affectedCutoff = Math.max(pJointSignificanceCutoff, affectedQuantile);
        unaffectedCutoff = Math.min(affectedCutoff, criticalSignificance);
        
        int numProbableFalseNegatives = 0;
        
        for(i = numElements; --i >= 0; )
        {
            affected = affectedElements[i];
            if(! affected)
            {
                jointSig = jointEffectiveSignificances[i];
                if(jointSig < unaffectedCutoff)
                {
                    affectedElements[i] = true;
                    ++numProbableFalseNegatives;
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
                                  jointEffectiveSignificances, 
                                  iterationResults);  
        }
        
        pRetResults.mSignificanceDistributionSeparation = iterationResults.mSignificanceDistributionSeparation;
        pRetResults.mAlphaParameter = iterationResults.mAlphaParameter;
        pRetResults.mNumIterations = iterationCtr;
        mWeights.toArray(pRetResults.mWeights);
    }
    
    public EvidenceWeightedInfererResults findAffectedElements(DoubleMatrix2D pSignificances,
                                                               int pNumBins,
                                                               double pInitialCutoff,
                                                               double pJointSignificanceCutoff,
                                                               double pFractionToRemove,
                                                               double pMinFractionalCostChange,
                                                               EvidenceWeightType pWeightType)
    {
        EvidenceWeightedInfererResults results = new EvidenceWeightedInfererResults();
        int numElements = pSignificances.rows();
        boolean []affectedElements = new boolean[numElements];
        double []jointEffectiveSignificances = new double[numElements];
        results.mJointEffectiveSignificances = jointEffectiveSignificances;
        results.mAffectedElements = affectedElements;
        int numEvidences = pSignificances.columns();
        results.mWeights = new double[numEvidences];
        
        findAffectedElements(pSignificances,
                             pNumBins,
                             pInitialCutoff,
                             pJointSignificanceCutoff,
                             pFractionToRemove,
                             pMinFractionalCostChange,
                             pWeightType,
                             results);
        
        return results;
    }
}
