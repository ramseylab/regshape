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
package org.systemsbiology.math.probability;


/**
 * Creates a string summary of a probability distribution function.
 * 
 * @author sramsey
 *
 */
public class DistributionPrinter
{
    private static final double NUM_STDEVS = 6.0;
    
    public static String print(IContinuousDistribution pDist, int pNumSamples)
    {
        StringBuffer sb = new StringBuffer();
        String name = pDist.name();
        double min = pDist.domainMin();
        double max = pDist.domainMax();
        double mean = pDist.mean();
        double variance = pDist.variance();
        sb.append("name: " + name + "\n");
        sb.append("min: " + min + "\n");
        sb.append("max: " + max + "\n");
        sb.append("mean: " + mean + "\n");
        sb.append("variance: " + variance + "\n");
        double xstart = 0.0;
        double xstop = 0.0;
        double stdev = Math.sqrt(variance);
        if(Double.isInfinite(min))
        {
            xstart = mean - NUM_STDEVS*stdev;
            xstop = mean + NUM_STDEVS*stdev;
        }
        else
        {
            xstart = min;
            if(! Double.isInfinite(mean))
            {
                xstop = mean + NUM_STDEVS*stdev;
            }
            else
            {
                xstop = min + (NUM_STDEVS + 2)*stdev;
            }
        }
        if(xstop > max)
        {
            xstop = max;
        }
        if(pNumSamples < 2)
        {
            throw new IllegalArgumentException("minimum number of samples is 2");
        }
        double deltax = (xstop - xstart)/((double) (pNumSamples-1));
        double x = xstart;
        double pdfx = 0.0;
        for(int i = 0; i < pNumSamples; ++i)
        {
            pdfx = pDist.pdf(x);
            sb.append("pdf(" + x + ") = " + pdfx + "\n");
            x += deltax;
            if(x > max)
            {
                x = max;
            }
        }
        return sb.toString();
    }
}
