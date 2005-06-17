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
package org.systemsbiology.math.probability.tp;

import org.systemsbiology.math.probability.*;

/**
 * @author sramsey
 *
 */
public class TestDistributions
{
    public static final void main(String []pArgs)
    {
        try
        {
            Rayleigh dist = new Rayleigh(1.0);
            System.out.println(DistributionPrinter.print(dist, 100));
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
