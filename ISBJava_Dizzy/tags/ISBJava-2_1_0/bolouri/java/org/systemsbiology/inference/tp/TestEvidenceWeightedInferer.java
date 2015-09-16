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
package org.systemsbiology.inference.tp;

import org.systemsbiology.inference.*;
import org.systemsbiology.math.MathFunctions;
import org.systemsbiology.math.MutableDouble;

import cern.colt.matrix.*;
import cern.jet.random.Gamma;
import cern.jet.random.Normal;
import edu.cornell.lassp.houle.RngPack.RandomElement;
import edu.cornell.lassp.houle.RngPack.Ranmar;

/**
 * @author sramsey
 *
 */
public class TestEvidenceWeightedInferer
{
    public static final SignificanceCalculationFormula USE_CDF = SignificanceCalculationFormula.CDF;
    public static final double SMOOTHING_LENGTH = 0.05;
    public static final int NUM_BINS = 100;
    public static final double MIN_BIN_SIZE_SIG = 1.0e-8;
    public static final double AVERAGE_STD_DEV_FOLD_CHANGE_UNAFFECTED = 0.5;
    public static final double STD_DEV_STD_DEV_FOLD_CHANGE_UNAFFECTED = 0.1;
    public static final double AVERAGE_STD_DEV_FOLD_CHANGE_AFFECTED = 0.5;
    public static final double STD_DEV_STD_DEV_FOLD_CHANGE_AFFECTED = 0.1;
    public static final double STD_DEV_MEAN_FOLD_CHANGE_AFFECTED = 0.1;
    public static final double ASYMMETRY_MEAN = 0.0;
    public static final double ASYMMETRY_STDEV = 0.01;
    public static final double FRACTION_TO_REMOVE = 0.05;
    public static final double MIN_FRACTIONAL_COST_CHANGE = 1.0e-6;
    
    public static final int NUM_TRIALS = 1;
    
    static class TestResults
    {
        public double mFalsePositiveRate;
        public double mFalseNegativeRate;
        public double mCostFunctionValue;
        public int mNumTrueAffected;
        public int mNumPutativelyAffected;
        public double mAlphaParameter;
        public int mNumIterations;
        public double mSecondsPerIteration;
    }
        
    static class NetworkElement implements Comparable
    {
        public double mSignificance;
        public int mIndex;
        public int compareTo(Object pObject)
        {
            return Double.compare(mSignificance, ((NetworkElement) pObject).mSignificance);
        }
    }
    
    
    private static TestResults runTestTrial(int pNumElements,
                                            double pFractionAffected, 
                                            int pNumEvidences,
                                            double pMeanFoldChangeAffected,
                                            double pFractionDataMissing,
                                            double pInitialCutoff,
                                            double pJointSignificanceCutoff,
                                            double pConfusion,
                                            double pSmoothingLength,
                                            EvidenceWeightedInferer pAffectedElementFinder,
                                            SignificanceCalculator pSignificancesCalculator)
    {
        TestResults results = null;

        try
        {
            double []stdDevFoldChangeUnaffected = new double[pNumEvidences];
            double []stdDevFoldChangeAffected = new double[pNumEvidences];
            double []meanFoldChangeAffected = new double[pNumEvidences];

            // create a random number generator
            RandomElement random = new Ranmar(System.currentTimeMillis());

            double alpha = Math.pow(AVERAGE_STD_DEV_FOLD_CHANGE_UNAFFECTED/
                                    STD_DEV_STD_DEV_FOLD_CHANGE_UNAFFECTED, 2.0);

            double lambda = AVERAGE_STD_DEV_FOLD_CHANGE_UNAFFECTED / 
                            Math.pow(STD_DEV_STD_DEV_FOLD_CHANGE_UNAFFECTED, 2.0);

            // assume the standard deviation for fold-changes is normally distributed
            Gamma stdDevFoldChangeUnaffectedDist = new Gamma(alpha, lambda, random);

            alpha = Math.pow(AVERAGE_STD_DEV_FOLD_CHANGE_AFFECTED/
                             STD_DEV_STD_DEV_FOLD_CHANGE_AFFECTED, 2.0);

            lambda = AVERAGE_STD_DEV_FOLD_CHANGE_AFFECTED / 
                     Math.pow(STD_DEV_STD_DEV_FOLD_CHANGE_AFFECTED, 2.0);

            Gamma stdDevFoldChangeAffectedDist = new Gamma(alpha, lambda, random);

            double alpha2 = Math.pow(pMeanFoldChangeAffected/
                             STD_DEV_MEAN_FOLD_CHANGE_AFFECTED, 2.0);

            double lambda2 = pMeanFoldChangeAffected / 
                              Math.pow(STD_DEV_MEAN_FOLD_CHANGE_AFFECTED, 2.0);


            Gamma meanFoldChangeAffectedDist = new Gamma(alpha2, lambda2, random);

            for(int j = 0; j < pNumEvidences; ++j)
            {
                // get the standard deviation for the evidence
 //               stdDevFoldChangeUnaffected[j] = stdDevFoldChangeUnaffectedDist.nextDouble();
 //               stdDevFoldChangeAffected[j] = stdDevFoldChangeAffectedDist.nextDouble();
 //              meanFoldChangeAffected[j] = meanFoldChangeAffectedDist.nextDouble();

                stdDevFoldChangeUnaffected[j] = 0.5;
                stdDevFoldChangeAffected[j] = 0.5;
                meanFoldChangeAffected[j] = pMeanFoldChangeAffected;
            }

            Normal asymmetryDist = new Normal(ASYMMETRY_MEAN, ASYMMETRY_STDEV, random);

            DoubleFactory2D fac2d = DoubleFactory2D.dense;
            ObjectMatrix2D obs = ObjectFactory2D.dense.make(pNumElements, pNumEvidences);
            Normal []distUnaffected = new Normal[pNumEvidences];
            Normal []distAffectedPos = new Normal[pNumEvidences];
            Normal []distAffectedNeg = new Normal[pNumEvidences];

            boolean []trueAffected = new boolean[pNumElements];

            for(int i = 0; i < pNumElements; ++i)
            {
                if(random.raw() < pFractionAffected)
                {
                    trueAffected[i] = true;
                }
                else
                {
                    trueAffected[i] = false;
                }
            }

            double []means = new double[pNumEvidences];
            double []stdevs = new double[pNumEvidences];
            boolean []hasAtLeastOneObservation = new boolean[pNumElements];
            for(int i = 0; i < pNumElements; ++i)
            {
                hasAtLeastOneObservation[i] = false;
            }
            
            for(int j = 0; j < pNumEvidences; ++j)
            {
                distUnaffected[j] = new Normal(0.0, stdDevFoldChangeUnaffected[j], random);
                double asymmetryForMean = asymmetryDist.nextDouble();
                distAffectedPos[j] = new Normal(meanFoldChangeAffected[j] + asymmetryForMean, 
                                                stdDevFoldChangeAffected[j], random);
                asymmetryForMean = asymmetryDist.nextDouble();
                distAffectedNeg[j] = new Normal(-1.0 * meanFoldChangeAffected[j] + asymmetryForMean, 
                                                stdDevFoldChangeAffected[j], random);


                for(int i = 0; i < pNumElements; ++i)
                {
                    double value = 0.0;
                    if(trueAffected[i] && random.raw() > pConfusion)
                    {
//                      :TODO: for debugging only                        
                        value = distUnaffected[j].nextDouble();
                        if(value < 0.0)
                        {
                            value -= meanFoldChangeAffected[j];
                        }
                        else
                        {
                            value += meanFoldChangeAffected[j];
                        }
//                      :TODO: for debugging only                        
//                        if(random.raw() > 0.5)
//                        {
//                            value = distAffectedPos[j].nextDouble();
//                        }
//                        else
//                        {
//                            value = distAffectedNeg[j].nextDouble();
//                        }
                    }
                    else
                    {
                        value = distUnaffected[j].nextDouble();
                    }
                    
                    if(random.raw() >= pFractionDataMissing || (j == pNumEvidences - 1 &&
                            ! hasAtLeastOneObservation[i]))
                    {
                        obs.set(i, j, new Double(value));
                        hasAtLeastOneObservation[i] = true;
                    }
                    else
                    {
                        obs.set(i, j, null);
                    }
                    
//                    System.out.println("i: " + i + "; j: " + j + "; value: " + value);
                }
            }

            DoubleMatrix2D sig = fac2d.make(pNumElements, pNumEvidences);

            DoubleMatrix1D sigCol = null;
            ObjectMatrix1D obsCol = null;
            Double []obsColDbl = new Double[pNumElements];
            
            // ------------------ used for creating file of observations ----------------------
//            File file = new File("/users/sramsey/observations.csv");
//            FileWriter fw = new FileWriter(file);
//            PrintWriter pw = new PrintWriter(fw);
//            StringBuffer sb = new StringBuffer();
//            Object obsObj = null;
//            String obsStr = null;
//            for(int i = 0; i < pNumElements; ++i)
//            {
//                sb.append("gene" + i + ",");
//                for(int j = 0; j < pNumEvidences; ++j)
//                {
//                    obsObj = obs.get(i, j);
//                    if(obsObj != null)
//                    {
//                        obsStr = ((Double) obsObj).toString();
//                    }
//                    else
//                    {
//                        obsStr = "";
//                    }
//                    sb.append(obsStr);
//                    if(j < pNumEvidences - 1)
//                    {
//                        sb.append(",");
//                    }
//                }
//                sb.append("\n");
//            }
//            pw.print(sb.toString());
//            pw.flush();
            
            SignificanceCalculationResults sigResults = null;
            SignificanceCalculatorParams sigParams = new SignificanceCalculatorParams();
            
            for(int j = 0; j < pNumEvidences; ++j)
            {
                sigParams.setNumBins(new Integer(NUM_BINS));
                sigParams.setSignificanceCalculationFormula(SignificanceCalculationFormula.CDF);
                sigParams.setSingleTailed(new Boolean(false));
                sigParams.setSmoothingLength(new Double(pSmoothingLength));
                
                obsCol = obs.viewColumn(j);
                obsCol.toArray(obsColDbl);
                
                sigResults = pSignificancesCalculator.calculateSignificances(obsColDbl,
                                                                             obsColDbl,
                                                                             sigParams);
                sigCol = sig.viewColumn(j);
                sigCol.assign(sigResults.mSignificances);
            }
            
            int numAffected = 0;

            double []sigEffProd = new double[pNumElements];

            double funcValue = 0.0;
            int pCutoffIndex = 0;
            int numAffectedGenes = 0;

            int numChanged = 0;
            
            boolean affected = false;
            double jointSig = 0.0;
            int i = 0;

     //       System.out.println("significances: " + sig);
            
            EvidenceWeightedInfererResults affectedElementsResults = null;

            long startTime = System.currentTimeMillis();

            EvidenceWeightedInfererParams params = new EvidenceWeightedInfererParams();
            params.setNumBins(new Integer(NUM_BINS));
            params.setInitialSignificanceCutoff(new Double(pInitialCutoff));
            params.setCombinedSignificanceCutoff(new Double(pJointSignificanceCutoff));
            params.setFractionToRemove(new Double(FRACTION_TO_REMOVE));
            params.setMinFractionalCostChange(new Double(MIN_FRACTIONAL_COST_CHANGE));
            params.setSmoothingLength(new Double(SMOOTHING_LENGTH));
            params.setEvidenceWeightType(EvidenceWeightType.POWER);
            
            affectedElementsResults = pAffectedElementFinder.findAffectedElements(sig, params);
                                
            long stopTime = System.currentTimeMillis();
            double elapsedTimeSeconds = ((double) (stopTime - startTime))/1000.0;

            int falsePos = 0;
            int falseNeg = 0;
            int numTrueAff = 0;
            int numAff = 0;

            double avgTrueAffectedSignificance = 0.0;
            
            double []jointEffectiveSignificances = affectedElementsResults.mCombinedEffectiveSignificances;
            boolean []affectedElements = affectedElementsResults.mAffectedElements;
            
            for(i = 0; i < pNumElements; ++i)
            {
                boolean trueAff = trueAffected[i];
                if(trueAff)
                {
                    avgTrueAffectedSignificance += jointEffectiveSignificances[i];
                }
                boolean aff = affectedElements[i];
                if(trueAff && (! aff))
                {
//                    System.out.println("false negative!; significance: " + sigEffProd[i]);
                    falseNeg++;
                }
                if((! trueAff) && aff)
                {
//                    System.out.println("false positive!; significance: " + sigEffProd[i]);
                    falsePos++;
                }
                if(trueAff)
                {
                    numTrueAff++;
                }
                if(aff)
                {
                    numAff++;
                }
            }

            avgTrueAffectedSignificance /= ((double) numTrueAff); 
                
            double falsePosDouble = (double) falsePos;
            double numTruePositives = (double) (numAff - falsePos);
            double ppv = numTruePositives / (numTruePositives + falsePosDouble);
            
            double falseNegDouble = (double) falseNeg;
            double numTrueUnaffDouble = (double) (pNumElements - numTrueAff);
            double numUnaff = pNumElements - numAff;
            double numTrueNegatives = (double) (numUnaff - falseNeg);
            double npv = numTrueNegatives / (numTrueNegatives + falseNegDouble);
            
            int numIterations = affectedElementsResults.mNumIterations;
            funcValue = affectedElementsResults.mSignificanceDistributionSeparation;
            
            double falsePosRate = 1.0 - ppv;
            double falseNegRate = 1.0 - npv;
            
            System.out.println("num true affected: " + numTrueAff);
             System.out.println("number of false positives: " + falsePos);
             System.out.println("false pos rate: " + falsePosRate);
             System.out.println("number of false negatives: " + falseNeg);
             System.out.println("false neg rate: " + falseNegRate);
             System.out.println("number of true affected: " + numTrueAff);
             System.out.println("number of putatively affected: " + numAff);
             System.out.println("final obj function: " + funcValue);
             System.out.println("num iterations: " + numIterations);
             System.out.println("\n");
             
            results = new TestResults();
            results.mCostFunctionValue = funcValue;
            results.mNumIterations = numIterations;
            results.mFalsePositiveRate = falsePosRate;
            results.mFalseNegativeRate = falseNegRate;
            results.mNumTrueAffected = numTrueAff;
            results.mNumPutativelyAffected = numAff;
            results.mSecondsPerIteration = elapsedTimeSeconds/((double) numIterations);
            results.mAlphaParameter = affectedElementsResults.mAlphaParameter;
       }


        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }

        return results;
    }
    
    private static void runTest(int pNumElements, 
                                double pFractionAffected, 
                                int pNumEvidences,
                                double pMeanFoldChangeAffected,
                                double pFractionDataMissing,
                                double pInitialCutoff,
                                double pJointSignificanceCutoff,
                                double pConfusion)
    {
        TestResults results = null;

        MutableDouble mean = new MutableDouble(0.0);
        MutableDouble stdev = new MutableDouble(0.0);
        double []alpha = new double[NUM_TRIALS];
        double []falsePos = new double[NUM_TRIALS];
        double []falseNeg = new double[NUM_TRIALS];
        double []costFunc = new double[NUM_TRIALS];
        double []numIterations = new double[NUM_TRIALS];
        double []numTrueAffected = new double[NUM_TRIALS];
        double []numPutativelyAffected = new double[NUM_TRIALS];
        double []secondsPerIteration = new double[NUM_TRIALS];

        EvidenceWeightedInferer finder = new EvidenceWeightedInferer();
        SignificanceCalculator sigCalc = new SignificanceCalculator();

        for(int i = 0; i < NUM_TRIALS; ++i)
        {
//            System.out.println("iteration: " + (i+1));
            results = runTestTrial(pNumElements, pFractionAffected, pNumEvidences, pMeanFoldChangeAffected,
                                   pFractionDataMissing, pInitialCutoff, pJointSignificanceCutoff, pConfusion, 
                                   SMOOTHING_LENGTH, 
                                   finder, sigCalc);
            
            alpha[i] = results.mAlphaParameter;
            falsePos[i] = results.mFalsePositiveRate;
            falseNeg[i] = results.mFalseNegativeRate;
            costFunc[i] = results.mCostFunctionValue;
            numIterations[i] = (double) results.mNumIterations;
            numTrueAffected[i] = results.mNumTrueAffected;
            numPutativelyAffected[i] = results.mNumPutativelyAffected;
            secondsPerIteration[i] = results.mSecondsPerIteration;
        }

        System.exit(0);  // :TODO: for debugging only
        
        StringBuffer sb = new StringBuffer();

        sb.append(pNumElements + ", ");
        sb.append(pFractionAffected + ", ");
        sb.append(pNumEvidences + ", ");
        sb.append(pMeanFoldChangeAffected + ", ");
        sb.append(pInitialCutoff + ", ");
        sb.append(pConfusion + ", ");
        sb.append(pFractionDataMissing + ", ");
        sb.append(pJointSignificanceCutoff + ", ");
                
        sb.append("");

        MathFunctions.stats(alpha, mean, stdev);
        sb.append(mean + ", " + stdev + ", ");

        MathFunctions.stats(falsePos, mean, stdev);
        sb.append(mean + ", " + stdev + ", ");

        MathFunctions.stats(falseNeg, mean, stdev);
        sb.append(mean + ", " + stdev + ", ");

        MathFunctions.stats(costFunc, mean, stdev);
        sb.append(mean + ", " + stdev + ", ");

        MathFunctions.stats(numIterations, mean, stdev);
        sb.append(mean + ", " + stdev + ", ");

        MathFunctions.stats(numTrueAffected, mean, stdev);
        sb.append(mean + ", " + stdev + ", ");
        
        MathFunctions.stats(numPutativelyAffected, mean, stdev);
        sb.append(mean + ", " + stdev + ", ");

        MathFunctions.stats(secondsPerIteration, mean, stdev);
        sb.append(mean + ", " + stdev);

        System.out.println(sb.toString());
    }
    
    public static final void main(String []pArgs)
    {
        try
        {
            int defaultNumElements = 6000; // 6000
            double defaultConfusion = 0.1; // 0.1
            double defaultFracDataMissing = 0.05; // 0.05
            double defaultInitialCutoff = 0.05; // 0.05
            double defaultFoldChangeAffected = 1.5; // 1.5
            int defaultNumEvidences = 6; // 6
            double defaultJointSignificanceCutoff = 1.0e-6;
            double defaultPriorFracAffected = 0.05;//0.05
            
            // baseline run, all defaults
            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);

//            // number of elements
//            runTest(325,   defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(750,   defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(1500,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(3000,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(4500,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(6000,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(9000,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(12000, defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//
//            // prior fraction affected
//            runTest(defaultNumElements,  0.01,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.02,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.03,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.04,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.05,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.06,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.07,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.08,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.09,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.10,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.12,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.14,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.16,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  0.18,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//
//            // number of evidence types
//            runTest(defaultNumElements,  defaultPriorFracAffected,  2, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  3, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  4, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  5, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  6, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  7, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  8, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  9, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected, 10, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected, 11, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected, 12, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected, 13, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected, 14, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected, 15, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            
//            // average fold change of affected elements
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 0.5, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 0.6, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 0.7, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 0.8, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 0.9, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.0, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.1, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.2, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.3, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.4, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.5, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.6, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.7, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.8, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 1.9, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, 2.0, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//
//            // fraction of data missing
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.01, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.02, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.03, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.04, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.05, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.06, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.07, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.08, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.09, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.10, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, 0.15, defaultInitialCutoff, defaultJointSignificanceCutoff, defaultConfusion);
//            
//            // initial p-value cutoff
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.005, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.0075, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.01, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.02, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.03, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.04, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.05, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.055, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.06, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.07, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.08, defaultJointSignificanceCutoff, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.09, defaultJointSignificanceCutoff, defaultConfusion);
////            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, 0.1, defaultJointSignificanceCutoff, defaultConfusion);
//            
//            // confusion parameter
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.05);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.06);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.07);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.08);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.09);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.10);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.11);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.12);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.13);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.15);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, defaultJointSignificanceCutoff, 0.20);
//
//            // joint significance cutoff
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 1.0e-8, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 2.5e-8, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 5.0e-8, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 7.5e-8, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 1.0e-7, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 1.25e-7, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 1.5e-7, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 1.75e-7, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 3.0e-7, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 5.0e-7, defaultConfusion);
//            runTest(defaultNumElements,  defaultPriorFracAffected,  defaultNumEvidences, defaultFoldChangeAffected, defaultFracDataMissing, defaultInitialCutoff, 1.0e-6, defaultConfusion);
//            
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
