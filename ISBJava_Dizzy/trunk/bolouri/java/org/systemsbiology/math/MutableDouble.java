package org.systemsbiology.math;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * A container class for a <code>double</code> native data type.
 * This class allows you to alter the <code>double</code> variable
 * that it contains; it is a fully mutable object.  The purpose
 * of this class is to provide a mechanism to use <code>double</code>
 * values as values of a <code>HashMap</code>, while allowing those
 * values to be mutable as well; this cannot be done with the standard
 * Java class <code>Double</code>, which is immutable.
 *
 * @see MutableLong
 * @see MutableInteger
 *
 * @author Stephen Ramsey
 */
public final class MutableDouble
{
    double mDouble;

    public double getValue()
    {
        return(mDouble);
    }

    public void setValue(double pDouble)
    {
        mDouble = pDouble;
    }

    public double doubleValue()
    {
        return(mDouble);
    }
    
    public MutableDouble(double pDouble)
    {
        setValue(pDouble);
    }

    public static int compare(MutableDouble p1, MutableDouble p2)
    {
        return(Double.compare(p1.mDouble, p2.mDouble));
    }

    public Object clone()
    {
        MutableDouble md = new MutableDouble(mDouble);
        return(md);
    }

    public String toString()
    {
        return(String.valueOf(mDouble));
    }
}
