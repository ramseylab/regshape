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

import cern.colt.list.*;
import cern.jet.stat.Descriptive;
import cern.colt.matrix.*;
import org.systemsbiology.math.probability.*;
import org.systemsbiology.math.*;

/**
 * @author sramsey
 *
 */
public class SignificanceCalculator
{
    public static final double DEFAULT_MISSING_DATA_SIGNIFICANCE = -1.0;
    private double mMissingDataSignificance;
    private DoubleArrayList mDataList;
    private DoubleMatrix1D mHistogram;
    private int mNumBins;
    
    public SignificanceCalculator()
    {
        setMissingDataSignificance(DEFAULT_MISSING_DATA_SIGNIFICANCE);
        mDataList = new DoubleArrayList();
        mNumBins = 0;
    }
    
    private void initialize(int pNumBins)
    {
        if(mNumBins != pNumBins)
        {
            mNumBins = pNumBins;
            mHistogram = DoubleFactory1D.dense.make(mNumBins);
        }
    }
    
    public void setMissingDataSignificance(double pMissingDataSignificance)
    {
        mMissingDataSignificance = pMissingDataSignificance;
    }
    
    private static void makeHistogram(int pNumBins, DoubleArrayList pObservations, double pMin, double pMax, DoubleMatrix1D pHistogram)
    {
        if(pNumBins <= 0)
        {
            throw new IllegalArgumentException("invalid number of bins");
        }
        DoubleMatrix1D dist = pHistogram;
        dist.assign(0.0);
        int numObs = pObservations.size();
        double obs = 0.0;
        int k = 0;
        if(numObs == 0)
        {
            throw new IllegalArgumentException("no observations");
        }
        double binSize = (pMax - pMin)/((double) pNumBins);
        double frac = 1.0 / (((double) numObs)*binSize);
        
        for(int i = numObs; --i >- 0; )
        {
            obs = pObservations.get(i);
            k = (int) ( (obs - pMin)/binSize );
            if(k == pNumBins)
            {
                --k;
            }
            dist.set(k, dist.get(k) + frac);
        }    
    }
    
    private static double reducedChiSquare(DoubleMatrix1D pDist1, IContinuousDistribution pDist2, double pMin, double pMax)
    {
        int numBins = pDist1.size();
        int k = 0;
        double val1 = 0.0;
        double val2 = 0.0;
        double chi = 0.0;
        double numBinsDouble = (double) numBins;
        
        if(pMin >= pMax)
        {
            throw new IllegalStateException("max must exceed min");
        }
        
        double binSize = (pMax - pMin) / numBinsDouble;
        
        double expec = 0.0;
        
        double sumWeights = 0.0;
        double weight = 0.0;
        double x = 0.0;
        
        for(k = numBins; --k >= 0; )
        {
            // get probability density per unit x, for first distribution
            val1 = pDist1.get(k);
            
            x = pMin + binSize*(((double) k) + 0.5);
            // get probability density per unit x, for second distribution
            val2 = pDist2.pdf(x);
            
            expec = 0.5*(val1 + val2);
            if(expec < 0.0)
            {
                throw new IllegalStateException("invalid average probability: " + expec);
            }
            if(expec != 0.0)
            {
                weight = Math.sqrt(expec);
                sumWeights += weight;
                chi += weight*(val1 - val2)*(val1 - val2)/(expec*expec);
            }
//            System.out.println("val1[" + k + "] = " + val1 + "; val2[" + k + "] = " + val2 + "; chicum: " + chi + "; x: " + x + "; min: " + pMin);
        }
        if(0.0 == sumWeights)
        {
            throw new IllegalStateException("zero weight sum");
        }
        chi /= sumWeights;
        return chi;
    }
    
    static class DistributionFitResults
    {
        public IContinuousDistribution mBestProbDist;
        public double mBestChiSquare; 
    }
    
    private void fitDistribution(DoubleArrayList pObservationsList,
                                 boolean pSingleTailed,
                                 boolean pAllowEmpirical,
                                 int pNumBins, 
                                 double pMaxChiSquare,
                                 DistributionFitResults pRetResults) throws AccuracyException
    {
        double max = Descriptive.max(pObservationsList);
        
        double min = 0.0;
        
        if(! pSingleTailed)
        { 
            min = Descriptive.min(pObservationsList);
        }
        
        DoubleMatrix1D dist = mHistogram;

        makeHistogram(pNumBins, pObservationsList, min, max, dist);
        
        double mean = Descriptive.mean(pObservationsList);
//        System.out.println("mean: " + mean);
        
        double variance = Descriptive.sampleVariance(pObservationsList, mean);
        if(variance == 0.0)
        {
            throw new IllegalArgumentException("all data values are the same; cannot fit a distribution");
        }   
        
        double bestChiSquare = Double.MAX_VALUE;
        IContinuousDistribution bestProbDist = null;
        
        if(pSingleTailed)
        {
            double zeroVariance = Descriptive.sampleVariance(pObservationsList, 0.0);
            
            HalfNormal normal = new HalfNormal(mean);
            double chiNormal = reducedChiSquare(dist, normal, min, max);
            
            Gamma gamma = new Gamma(mean, variance);
            double chiGamma = reducedChiSquare(dist, gamma, min, max);
            
            double zeroStdev = Math.sqrt(zeroVariance);
            
            HalfLorentz lorentz = new HalfLorentz(zeroStdev);
            double chiLorentz = reducedChiSquare(dist, lorentz, min, max);
            
            Rayleigh rayleigh = new Rayleigh(mean);
            double chiRayleigh = reducedChiSquare(dist, rayleigh, min, max);
            
            Maxwell maxwell = new Maxwell(mean);
            double chiMaxwell = reducedChiSquare(dist, maxwell, min, max);
            
            Empirical empirical = null;
            double chiEmpirical = 0.0;
            if(pAllowEmpirical)
            {
                empirical = new Empirical(dist, min, max);
                chiEmpirical = reducedChiSquare(dist, empirical, min, max);
            }
            
            //System.out.println("chi-square for normal: " + chiNormal);
            //System.out.println(DistributionPrinter.print(normal, 50));
            //System.out.println("chi-square for gamma: " + chiGamma);
            //System.out.println(DistributionPrinter.print(gamma, 50));
            //System.out.println("chi-square for lorentz: " + chiLorentz);
            //System.out.println(DistributionPrinter.print(lorentz, 50));
            //System.out.println("chi-square for rayleigh: " + chiRayleigh);
            //System.out.println(DistributionPrinter.print(rayleigh, 50));
            //System.out.println("chi-square for maxwell: " + chiMaxwell);
            //System.out.println(DistributionPrinter.print(maxwell, 50));
            //System.out.println("chi-square for empirical: " + chiEmpirical);    
            //System.out.println(DistributionPrinter.print(empirical, 50));
            
            if(chiNormal < bestChiSquare)
            {
                bestProbDist = normal;
                bestChiSquare = chiNormal;
            }
            if(chiGamma < bestChiSquare)
            {
                bestProbDist = gamma;
                bestChiSquare = chiGamma;
            }
            if(chiLorentz < bestChiSquare)
            {
                bestProbDist = lorentz;
                bestChiSquare = chiLorentz;
            }
            if(chiRayleigh < bestChiSquare)
            {
                bestProbDist = rayleigh;
                bestChiSquare = chiRayleigh;
            }
            if(chiMaxwell < bestChiSquare)
            {
                bestProbDist = maxwell;
                bestChiSquare = chiMaxwell;
            }
            if(pAllowEmpirical && bestChiSquare > pMaxChiSquare)
            {
                bestProbDist = empirical;
                bestChiSquare = chiEmpirical;
            }
            if(bestChiSquare > pMaxChiSquare)
            {
                throw new IllegalStateException("failed to fit any probability distribution");
            }
        }
        else
        {
            Normal normal = new Normal(mean, variance);
            double chiNormal = reducedChiSquare(dist, normal, min, max);
            double stdev = Math.sqrt(variance);
            
            Lorentz lorentz = new Lorentz(mean, stdev);
            double chiLorentz = reducedChiSquare(dist, lorentz, min, max);
            
            Laplace laplace = new Laplace(mean, variance);
            double chiLaplace = reducedChiSquare(dist, laplace, min, max);
            
            Logistic logistic = new Logistic(mean, variance);
            double chiLogistic = reducedChiSquare(dist, logistic, min, max);
            
            Empirical empirical = null;
            double chiEmpirical = 0.0;
            if(pAllowEmpirical)
            {
                empirical = new Empirical(dist, min, max);
                chiEmpirical = reducedChiSquare(dist, empirical, min, max);
            }
            
//              System.out.println("chi-square for normal: " + chiNormal);
//            System.out.println(DistributionPrinter.print(normal, 50));
//              System.out.println("chi-square for lorentz: " + chiLorentz);
            //System.out.println(DistributionPrinter.print(lorentz, 50));
//              System.out.println("chi-square for laplace: " + chiLaplace);
//              System.out.println(DistributionPrinter.print(laplace, 50));
//              System.out.println("chi-square for logistic: " + chiLogistic);
  //          System.out.println(DistributionPrinter.print(logistic, 50));
//              System.out.println("chi-square for empirical: " + chiEmpirical);
    //        System.out.println(DistributionPrinter.print(empirical, 50));
            
                
            if(chiNormal < bestChiSquare)
            {
                bestProbDist = normal;
                bestChiSquare = chiNormal;
            }
            if(chiLorentz < bestChiSquare)
            {
                bestProbDist = lorentz;
                bestChiSquare = chiLorentz;
            }
            if(chiLaplace < bestChiSquare)
            {
                bestProbDist = laplace;
                bestChiSquare = chiLaplace;
            }
            if(chiLogistic < bestChiSquare)
            {
                bestProbDist = logistic;
                bestChiSquare = chiLogistic;
            }
            if(pAllowEmpirical && bestChiSquare > pMaxChiSquare)
            {
                bestProbDist = empirical;
                bestChiSquare = chiEmpirical;
            }
            if(bestChiSquare > pMaxChiSquare)
            {
                throw new IllegalStateException("failed to fit any probability distribution");
            }
        }
        
        pRetResults.mBestChiSquare = bestChiSquare;
        pRetResults.mBestProbDist = bestProbDist;
    }
    
    private void calculateSignificances(Double []pObservations,
                                        IContinuousDistribution pDistribution,
                                        boolean pSingleTailed,
                                        SignificanceCalculationFormula pSignificanceType,
                                        double []pRetSignificances)
    {    
        if(null == pObservations)
        {
            throw new IllegalArgumentException("no observations");
        }
        if(null == pDistribution)
        {
            throw new IllegalArgumentException("no distribution");
        }
        if(null == pSignificanceType)
        {
            throw new IllegalArgumentException("no significance formula");
        }
        if(null == pRetSignificances)
        {
            throw new IllegalArgumentException("no ret significances");
        }
        int numObservations = pObservations.length;
        if(numObservations == 0)
        {
            throw new IllegalArgumentException("no observations");
        }
        
        if(pRetSignificances.length != numObservations)
        {
            throw new IllegalArgumentException("significances vector has the wrong size");
        }
                
        double sig = 0.0;
        double obs = 0.0;
        Double obsObj = null;
        
        double max = pDistribution.domainMax();
        double min = pDistribution.domainMin();
        
        double cdfx = 0.0;
        Double sigObj = null;
        for(int i = 0; i < numObservations; ++i)
        {
            obsObj = (Double) pObservations[i];
            if(null != obsObj)
            {
                obs = obsObj.doubleValue();
                if(obs < min || obs > max)
                {     
                    throw new IllegalArgumentException("observation out of range, for element number: " + i + "; value is: " + obs);
                }
                if(pSignificanceType.equals(SignificanceCalculationFormula.CDF))
                {
                    cdfx = pDistribution.cdf(obs);
                    if(pSingleTailed)
                    {
                        sig = 1.0 - cdfx;
                        if(sig < 0.0)
                        {
                            throw new IllegalStateException("negative significance value");
                        }
                    }
                    else
                    {
                        sig = Math.min( 2.0 * cdfx, 2.0 * (1.0 - cdfx) ); 
                    }
                }
                else if(pSignificanceType.equals(SignificanceCalculationFormula.PDF))
                {
                    sig = pDistribution.pdf(obs);
                }
                else
                {
                    throw new IllegalArgumentException("unknown significance calculation formula: " + pSignificanceType.toString());
                }
            }
            else
            {
                sig = mMissingDataSignificance;
            }
            pRetSignificances[i] = sig;
        }              
    }

    public void calculateSignificances(Double []pObservations, 
                                       Double []pControlData,
                                       int pNumBins, 
                                       double pMaxChiSquare, 
                                       boolean pSingleTailed,
                                       boolean pAllowEmpirical,
                                       SignificanceCalculationFormula pSignificanceType,
                                       SignificanceCalculationResults pRetResults) throws AccuracyException
    {
        int numObservations = pObservations.length;
        if(numObservations == 0)
        {
            throw new IllegalArgumentException("no observations");
        }
        
        int numControlValues = pControlData.length;
        if(numControlValues == 0)
        {
            throw new IllegalArgumentException("no control data");
        }

        if(pMaxChiSquare <= 0.0)
        {
            throw new IllegalArgumentException("invalid max chi square");
        }
        
        double []significances = pRetResults.mSignificances;
        if(null == significances)
        {
            throw new IllegalArgumentException("missing return array for significances");
        }
        if(significances.length != numObservations)
        {
            throw new IllegalArgumentException("improper array length");
        }
        
        initialize(pNumBins);
        
        int numNonMissingControlValues = 0;
        DoubleArrayList controlList = mDataList;
        controlList.clear();
        Double valueObj = null;
        for(int i = 0; i < numControlValues; ++i)
        {
            valueObj = pControlData[i];
            if(null != valueObj)
            {
                controlList.add(valueObj.doubleValue());
            }
        }
        
        if(pSignificanceType.equals(SignificanceCalculationFormula.PDF) &&
           pAllowEmpirical)
        {
        	double obsMax = 0.0;
        	double obsMin = Double.MAX_VALUE;
        	Double obsObj = null;
        	double obsVal = 0.0;
        	for(int i = 0; i < numObservations; ++i)
        	{
        		obsObj = pObservations[i];
        		if(null != obsObj)
        		{
        			obsVal = obsObj.doubleValue();
        			if(obsVal > obsMax)
        			{
        				obsMax = obsVal;
        			}
        			if(obsVal < obsMin)
        			{
        				obsMin = obsVal;
        			}
        		}
        	}
        	// check to make sure that the observations are bounded by the 
        	// negative 
        	double controlMax = Descriptive.max(controlList);
        	double controlMin = Descriptive.min(controlList);
        	if(obsMax > controlMax || obsMin < controlMin)
        	{
        		throw new IllegalArgumentException("when allowing the empirical distribution and using the PDF method for calculating significances, observations must be within the data range of the control data");
        	}
        }
        		
        DistributionFitResults distributionFitResults = new DistributionFitResults();
        fitDistribution(controlList,
                        pSingleTailed,
                        pAllowEmpirical,
                        pNumBins,
                        pMaxChiSquare,
                        distributionFitResults);
        
        double bestChiSquare = distributionFitResults.mBestChiSquare;
        IContinuousDistribution bestProbDist = distributionFitResults.mBestProbDist;
        if(null == bestProbDist)
        {
            throw new IllegalArgumentException("no probability distribution returned from fitDistribution");
        }
        
        calculateSignificances(pObservations,
                               bestProbDist,
                               pSingleTailed,
                               pSignificanceType,
                               significances);
        
        pRetResults.mReducedChiSquare = bestChiSquare;
        pRetResults.mBestFitDistribution = bestProbDist;
    }
                                           
    public SignificanceCalculationResults calculateSignificances(Double []pObservations, 
                                                                 Double []pControlData,
                                                                 int pNumBins, 
                                                                 double pMaxChiSquare, 
                                                                 boolean pSingleTailed,
                                                                 boolean pAllowEmpirical,
                                                                 SignificanceCalculationFormula pSignificanceType) throws AccuracyException
    {
        int numObservations = pObservations.length;
        
        if(numObservations == 0)
        {
            throw new IllegalArgumentException("no observations");
        }

        SignificanceCalculationResults results = new SignificanceCalculationResults(numObservations);
        
        calculateSignificances(pObservations, 
                               pControlData,
                               pNumBins,
                               pMaxChiSquare,
                               pSingleTailed,
                               pAllowEmpirical,
                               pSignificanceType,
                               results);
        
        return results;
    }    
}
