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
 * A container class for a <code>integer</code> native data type.
 * This class allows you to alter the <code>integer</code> variable
 * that it contains; it is a fully mutable object.  The purpose
 * of this class is to provide a mechanism to use <code>integer</code>
 * values as values of a <code>HashMap</code>, while allowing those
 * values to be mutable as well; this cannot be done with the standard
 * Java class <code>Integer</code>, which is immutable.
 *
 * @see MutableLong
 * @see MutableInteger
 *
 * @author Stephen Ramsey
 */
public final class MutableInteger
{
    private int mInteger;

    public int getValue()
    {
        return(mInteger);
    }

    public void setValue(int pInteger)
    {
        mInteger = pInteger;
    }

    public int integerValue()
    {
        return(mInteger);
    }
    
    public MutableInteger(int pInteger)
    {
        setValue(pInteger);
    }

    public Object clone()
    {
        MutableInteger md = new MutableInteger(mInteger);
        return(md);
    }

    public String toString()
    {
        return(String.valueOf(mInteger));
    }
}
