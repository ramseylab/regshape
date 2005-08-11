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
package org.systemsbiology.math;

/**
 * @author sramsey
 *
 */
public class DoubleVector
{

    public static void zeroNegativeElements(double []vec)
    {
        int size = vec.length;
    
        for(int ctr = size; --ctr >= 0; )
        {
            if(vec[ctr] < 0.0)
            {
                vec[ctr] = 0.0;
            }
        }
    }

    public static void zeroElements(double []vec)
    {
        int size = vec.length;
    
        for(int ctr = size; --ctr >= 0; )
        {
            vec[ctr] = 0.0;
        }
    }

    public static double max(double []vec)
    {
        assert (vec.length > 0) : "computing the maximum of a vector requires non-zero length";
        double retVal = -1.0 * Double.MAX_VALUE;
    
        int size = vec.length;
    
        for(int ctr = size; --ctr >= 0; )
        {
            if(vec[ctr] > retVal)
            {
                retVal = vec[ctr];
            }
        }
    
        return(retVal);
    }

    public static double sumElements(double []vec)
    {
        double retVal = 0.0;
    
        int size = vec.length;
    
        for(int ctr = size; --ctr >= 0; )
        {
            retVal += vec[ctr];
        }
    
        return(retVal);
    }

    public static void add(double []addendX, double []addendY, double []sum)
    {
        int size1 = addendX.length;
        int size2 = addendY.length;
        int size3 = sum.length;
    
        assert (size1 == size2 && size2 == size3) : "inconsistent vector size";
    
        for(int ctr = size1; --ctr >= 0; )
        {
            sum[ctr] = addendX[ctr] + addendY[ctr];
        }
    }

    public static void subtract(double []addendX, double []addendY, double []sum)
    {
        int size1 = addendX.length;
        int size2 = addendY.length;
        int size3 = sum.length;
    
        assert (size1 == size2 && size2 == size3) : "inconsistent vector size";
    
        for(int ctr = size1; --ctr >= 0; )
        {
            sum[ctr] = addendX[ctr] - addendY[ctr];
        }
    }

    public static void scalarMultiply(double []vector, double scalar, double []product)
    {
        int size1 = vector.length;
        int size2 = product.length;
        assert (size1 == size2) : "inconsistent vector size";
    
        for(int ctr = size1; --ctr >= 0; )
        {
            product[ctr] = scalar * vector[ctr];
        }
    }

    public static void scalarMultiply(double []vector, double scalar)
    {
        int size1 = vector.length;
    
        for(int ctr = size1; --ctr >= 0; )
        {
            vector[ctr] = scalar * vector[ctr];
        }
    }
}
