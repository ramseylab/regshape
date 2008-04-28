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

import cern.jet.random.*;
import edu.cornell.lassp.houle.RngPack.*;
import cern.colt.matrix.*;
import org.systemsbiology.inference.*;
import org.systemsbiology.math.*;

/**
 * @author sramsey
 *
 */
public class TestSignificanceCalculator
{
    public static final int NUM_BINS = 100;
    public static final int NUM_ELEMENTS = 600;
    public static final double MAX_SMOOTHING_LENGTH = 1.0;
        public static final double MAX_CHI_SQUARE = 1.0;
    
    public static void testMultipleEvidences()
    {
        try
        {
            SignificanceCalculator sigCalc = new SignificanceCalculator();
            
            RandomElement random = new Ranmar(System.currentTimeMillis());
            
            Gamma gamma = new Gamma(4.0, 1.0, random);
            Normal normal = new Normal(0.0, 2.0, random);
            BreitWigner wigner = new BreitWigner(0.0, 2.0, 30.0, random);

            ObjectMatrix2D obsMatrix = ObjectFactory2D.dense.make(NUM_ELEMENTS, 3);
            
            boolean []singleTailed = new boolean[3];
            singleTailed[0] = true;
            singleTailed[1] = false;
            singleTailed[2] = false;
            
            DoubleMatrix2D sigsCDF = DoubleFactory2D.dense.make(NUM_ELEMENTS, 3);
            DoubleMatrix2D sigsPDF = DoubleFactory2D.dense.make(NUM_ELEMENTS, 3);
            
            double []means = new double[3];
            double []stdevs = new double[3];
            
            double obs = 0.0;
            for(int i = 0; i < NUM_ELEMENTS; ++i)
            {
                
                obs = gamma.nextDouble();
                obsMatrix.set(i, 0, new Double(obs));
                means[0] += obs;
                obs = normal.nextDouble();
                obsMatrix.set(i, 1, new Double(obs));
                means[1] += obs;
                obs = wigner.nextDouble();
                obsMatrix.set(i, 2, new Double(obs));
                means[2] += obs;
            }
            DoubleVector.scalarMultiply(means, 1.0/((double) NUM_ELEMENTS));

            
            for(int j = 0; j < 3; ++j)
            {
                for(int i = 0; i < NUM_ELEMENTS; ++i)
                {
                    double val = ((Double) obsMatrix.get(i, j)).doubleValue();
                    stdevs[j] += (val - means[j])*(val - means[j]);
                }   
                stdevs[j] = Math.sqrt(stdevs[j]/((double) (NUM_ELEMENTS - 1)));
            }
            
//            System.out.println("observations: ");
//            System.out.println(obsMatrix.toString());

            boolean allowEmpirical = true;
            
            ObjectMatrix1D obsCol = null;
            
            SignificanceCalculationResults results = new SignificanceCalculationResults(NUM_ELEMENTS);
            
            Double []obsColDbl = new Double[NUM_ELEMENTS];
            for(int j = 0; j < 3; ++j)
            {
                obsCol = obsMatrix.viewColumn(j);
                obsCol.toArray(obsColDbl);
                sigCalc.calculateSignificancesNonParametric(obsColDbl, 
                                                  obsColDbl,
                                                  NUM_BINS, 
                                                  singleTailed[j],
                                                  MAX_SMOOTHING_LENGTH,
                                                  SignificanceCalculationMethod.CODE_CDF_NONPARAMETRIC,
                                                  results);
                
                sigsCDF.viewColumn(j).assign(results.mSignificances);
            
                allowEmpirical = false;
                
                sigCalc.calculateSignificancesNonParametric(obsColDbl, 
                                                  obsColDbl,
                                                  NUM_BINS, 
                                                  singleTailed[j],
                                                  MAX_CHI_SQUARE,
                                                  SignificanceCalculationMethod.CODE_PDF_NONPARAMETRIC,
                                                  results);
                
                sigsPDF.viewColumn(j).assign(results.mSignificances);
            }
            
            for(int j = 0; j < 3; ++j)
            {
                System.out.println("mean[" + j + "] = " + means[j] + "; stdevs[" + j + "] = " + stdevs[j]);
                
                for(int i = 0; i < NUM_ELEMENTS; ++i)
                {
                    obs = ((Double) obsMatrix.get(i, j)).doubleValue();
                    double sigCDF = sigsCDF.get(i, j);
                    double sigPDF = sigsPDF.get(i, j);
                    System.out.println("obs[" + i + "," + j + "] = " + obs + "; sigCDF: " + sigCDF + "; sigPDF: " + sigPDF);
                }
            }
//            System.out.println("significances: ");
//            System.out.println(sigsCDF.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
        
    }
    
    public static final void main(String []pArgs)
    {
        testMultipleEvidences();
    }
}
