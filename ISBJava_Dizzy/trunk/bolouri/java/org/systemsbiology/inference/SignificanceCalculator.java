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
 * Based on a set of "negative control" observations, computes the
 * significance of each of a set of observations.  The set of
 * control ovservations may or may not be identically the same as
 * the observations for which the significances are to be computed,
 * which allows for computing the significances of a set of observations
 * in the absence of separate "negative control" measurements.  The
 * method used for computing the significance depends on the
 * {@link SignificanceCalculationFormula} passed as a parameter.
 * The algorithms used in this class are based on the ideas and designs of
 * Daehee Hwang at Institute for Systems Biology.
 * 
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
    
    private double []mPDFs;
    private double []mCDFs;
    private double []mXs;
    
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
            
            mPDFs = new double[pNumBins];
            mCDFs = new double[pNumBins];
            mXs = new double[pNumBins];
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
                throw new IllegalStateException("failed to fit any probability distribution; please try making the maximum reduced chi-square parameter smaller");
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

    
    public void calculateSignificancesCDF(Double []pObservations, 
                                          Double []pControlData,
                                          int pNumBins, 
                                          boolean pSingleTailed,
                                          double pSmoothingLength,
                                          SignificanceCalculationResults pRetResults) 
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
        
        double []significances = pRetResults.mSignificances;
        if(null == significances)
        {
            throw new IllegalArgumentException("missing return array for significances");
        }
        if(significances.length != numObservations)
        {
            throw new IllegalArgumentException("improper array length");
        }
                
        if(pSmoothingLength <= 0.0)
        {
            throw new IllegalArgumentException("improper smoothing length");
        }
        
        initialize(pNumBins);
        
        DoubleArrayList controlsList = mDataList;
        controlsList.clear();
        Double controlValObj = null;
        for(int i = 0; i < numControlValues; ++i)
        {
            controlValObj = pControlData[i];
            if(null != controlValObj)
            {
                controlsList.add(controlValObj.doubleValue());
            }
        }
        controlsList.sort();
        double controlMax = Descriptive.max(controlsList);
        double controlMin = Descriptive.min(controlsList);
//        System.out.println("control min: " + controlMin + "; control max: " + controlMax);
        
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
        
        double []controls = controlsList.elements();
        double []pdfs = mPDFs;

        double numBinsDouble = (double) pNumBins;
        double binSize = (obsMax - obsMin)/numBinsDouble;
        double x = 0.0;
        double pdfx = 0.0;
        
        double variance = pSmoothingLength*pSmoothingLength;
        double numControlsDouble = (double) numControlValues;
        double pdfInt = 0.0;
        
        double []xs = mXs;
        
        double pdfLeft = 0.0;
        double pdfRight = 0.0;
        double controlVal = 0.0;
        double lastControlVal = 0.0;
        double lastPDFVal = 0.0;
        
        // build the probability distribution
        for(int k = 0; k < pNumBins; ++k)
        {
            x = obsMin + binSize*(((double) k) + 0.5);
            xs[k] = x;
            pdfx = 0.0;
            
            lastControlVal = controls[0];
            lastPDFVal = Normal.pdf(lastControlVal, variance, x);
            
            for(int i = 0; i < numControlValues; ++i)
            {
                controlVal = controls[i];
                // only recompute the Normal.pdf() if it is necessary (i.e., we are at a new "control" value)
                if((lastControlVal - controlVal) != 0.0)
                {
                    lastPDFVal = Normal.pdf(controlVal, variance, x);
                    lastControlVal = controlVal;
                }
                pdfx += lastPDFVal;
            }
            
            pdfx /= numControlsDouble;
            
            pdfs[k] = pdfx;
            pdfInt += pdfx*binSize;
        }
        if(! pSingleTailed)
        {
            x = obsMin - 0.5*binSize;
            for(int i = 0; i < numControlValues; ++i)
            {
                pdfLeft += Normal.pdf(controls[i], variance, x);
            }
            pdfLeft /= numControlsDouble;
        }
        x = obsMax + 0.5*binSize;
        for(int i = 0; i < numControlValues; ++i)
        {
            pdfRight += Normal.pdf(controls[i], variance, x);
        }
        pdfRight /= numControlsDouble;
        pdfInt += pdfLeft*binSize;
        pdfInt += pdfRight*binSize;

        // need to rescale the probability distribution function to ensure it has unit integral
        for(int k = 0; k < pNumBins; ++k)
        {
            pdfs[k] /= pdfInt;
//            System.out.println("pdf[" + k + "] = " + pdfs[k]);
        }
        pdfLeft /= pdfInt;
        pdfRight /= pdfInt;
        
        pdfInt = pdfLeft*binSize;
        double []cdfs = mCDFs;
        for(int k = 0; k < pNumBins; ++k)
        {
            x = obsMin + binSize*(((double) k) + 0.5);
            pdfInt += pdfs[k]*binSize;
            cdfs[k] = Math.min(pdfInt, 1.0);
        }            
        
        Double obsValObj = null;
        int k = 0;
        double kdouble = 0.0;
        double yLeft = 0.0;
        double yRight = 0.0;
        double xLeft = 0.0;
        double slope = 0.0;
        double cdf = 0.0;
        double sig = 0.0;
        
        for(int i = 0; i < numObservations; ++i)
        {
            obsValObj = pObservations[i];
            if(null != obsValObj)
            {
                obsVal = obsValObj.doubleValue();
                kdouble = (obsVal - obsMin)/binSize;
                
                if(kdouble > 0.5 && kdouble < numBinsDouble - 0.5)
                {
                    k = (int) (kdouble - 0.5);
                    yLeft = cdfs[k];
                    yRight = cdfs[k+1];
                    xLeft = xs[k];
                }
                else if(kdouble <= 0.5)
                {
                    yLeft = pdfLeft*binSize;
                    yRight = cdfs[0];
                    //xLeft = xs[0] - binSize;
                    xLeft = obsMin - 0.5*binSize;
                }
                else
                {
                    yLeft = cdfs[pNumBins - 1];
                    yRight = yLeft + pdfRight*binSize;
                    xLeft = xs[pNumBins - 1];
                }
                
                slope = (yRight - yLeft)/binSize;
                
                cdf = Math.min(1.0, yLeft + slope*(obsVal - xLeft));                
                sig = 0.0;
                
                if(! pSingleTailed)
                {
                    sig = 2.0 * Math.min(cdf, 1.0 - cdf);
                }
                else
                {
                    sig = 1.0 - cdf;
                }

            }
            else
            {
                sig = mMissingDataSignificance;
            }
            significances[i] = sig;
        }
    }
    
    public void calculateSignificancesPDF(Double []pObservations, 
                                          Double []pControlData,
                                          int pNumBins, 
                                          boolean pSingleTailed,
                                          double pMaxChiSquare, 
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
        
        if(pSingleTailed && (obsMin < 0.0 || controlMin < 0.0))
        {
            throw new IllegalArgumentException("cannot perform single-tailed test on an evidence that contains negative observations");
        }
        		
        DistributionFitResults distributionFitResults = new DistributionFitResults();

        boolean allowEmpirical = false;
        fitDistribution(controlList,
                        pSingleTailed,
                        allowEmpirical,
                        pNumBins,
                        pMaxChiSquare,
                        distributionFitResults);
        
        double bestChiSquare = distributionFitResults.mBestChiSquare;
        IContinuousDistribution bestProbDist = distributionFitResults.mBestProbDist;
        if(null == bestProbDist)
        {
            throw new IllegalArgumentException("no probability distribution returned from fitDistribution");
        }
        
        double sig = 0.0;
        double obs = 0.0;
      
        double max = bestProbDist.domainMax();
        double min = bestProbDist.domainMin();
      
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
                
                sig = bestProbDist.pdf(obs);
            }
            else
            {
                sig = mMissingDataSignificance;
            }
            significances[i] = sig;
        }       
        
        pRetResults.mReducedChiSquare = bestChiSquare;
        pRetResults.mBestFitDistribution = bestProbDist;
    }
                          
    public void calculateSignificances(Double []pObservations,
                                       Double []pControlData,
                                       SignificanceCalculatorParams pParams,
                                       SignificanceCalculationResults pResults) throws AccuracyException
    {
        SignificanceCalculationFormula formula = pParams.getSignificanceCalculationFormula();
        if(null == formula)
        {
            throw new IllegalArgumentException("missing required parameter:  significance calculation formula");
        }
           
        Integer numBinsObj = pParams.getNumBins();
        if(null == numBinsObj)
        {
            throw new IllegalArgumentException("missing required parameter:  num bins");
        }
        int numBins = numBinsObj.intValue();
        if(numBins <= 1)
        {
            throw new IllegalArgumentException("illegal number of bins: " + numBins);
        }
        
        Boolean singleTailedObj = pParams.getSingleTailed();
        if(null == singleTailedObj)
        {
            throw new IllegalArgumentException("missing required parameter: single tailed");
        }
        boolean singleTailed = singleTailedObj.booleanValue();
        
        if(formula.equals(SignificanceCalculationFormula.CDF))
        {
            Double smoothingLengthObj = pParams.getSmoothingLength();
            if(null == smoothingLengthObj)
            {
                throw new IllegalArgumentException("missing required parameter: smoothing length");
            }
            double smoothingLength = smoothingLengthObj.doubleValue();
            if(smoothingLength <= 0.0)
            {
                throw new IllegalArgumentException("illegal smoothing length: " + smoothingLength);
            }
            
            calculateSignificancesCDF(pObservations,
                                      pControlData,
                                      numBins,
                                      singleTailed,
                                      smoothingLength,
                                      pResults);
        }
        else if(formula.equals(SignificanceCalculationFormula.PDF))
        {
            Double maxReducedChiSquareObj = pParams.getMaxReducedChiSquare();
            if(null == maxReducedChiSquareObj)
            {
                throw new IllegalArgumentException("missing required parameter: max reduced chi square");
            }
            double maxReducedChiSquare = maxReducedChiSquareObj.doubleValue();
            if(maxReducedChiSquare <= 0.0)
            {
                throw new IllegalArgumentException("illegal max reduced chi square: " + maxReducedChiSquare);
            }
            
            calculateSignificancesPDF(pObservations,
                                      pControlData,
                                      numBins,
                                      singleTailed,
                                      maxReducedChiSquare,
                                      pResults);
        }
        else
        {
            throw new IllegalArgumentException("unknown significance calculation formula: " + formula.getName());
        }
    }
    
    public SignificanceCalculationResults calculateSignificances(Double []pObservations, 
                                                                 Double []pControlData,
                                                                 SignificanceCalculatorParams pParams) throws AccuracyException
    {
        int numObservations = pObservations.length;
        
        if(numObservations == 0)
        {
            throw new IllegalArgumentException("no observations");
        }

        SignificanceCalculationResults results = new SignificanceCalculationResults(numObservations);
        
        calculateSignificances(pObservations, 
                               pControlData,
                               pParams,
                               results);
        
        return results;
    }    
}
