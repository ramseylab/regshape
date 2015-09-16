package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

public final class ReactionParticipant implements Comparable
{
    final Species mSpecies;
    final int mStoichiometry;
    final boolean mDynamic;

    public final static class Type 
    {
        private final String mName;
        private Type(String pName)
        {
            mName = pName;
        }
        
        public String toString()
        {
            return(mName);
        }

        public static final Type REACTANT = new Type("reactant");
        public static final Type PRODUCT = new Type("product");
    }

    public ReactionParticipant(Species pSpecies, int pStoichiometry, boolean pDynamic) throws IllegalArgumentException
    {
        if(pSpecies.getValue().isExpression() && pDynamic == true)
        {
            throw new IllegalArgumentException("attempt to use a species with a population expression, as a dynamic participant of a reaction");
        }
        if(pStoichiometry < 1)
        {
            throw new IllegalArgumentException("illegal stoichiometry value: " + pStoichiometry);
        }

        mStoichiometry = pStoichiometry;
        mSpecies = pSpecies;
        mDynamic = pDynamic;
    }

    public int getStoichiometry()
    {
        return(mStoichiometry);
    }

    public boolean getDynamic()
    {
        return(mDynamic);
    }

    public Species getSpecies()
    {
        return(mSpecies);
    }

    public boolean equals(ReactionParticipant pReactionParticipant)
    {
        return( mSpecies.equals(pReactionParticipant.mSpecies) &&
                mStoichiometry == pReactionParticipant.mStoichiometry &&
                mDynamic == pReactionParticipant.mDynamic );
    }

    public int compareTo(Object pReactionParticipant)
    {
        return(mSpecies.getName().compareTo(((ReactionParticipant) pReactionParticipant).mSpecies.getName()));
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("ReactionParticipant[");
        sb.append(mSpecies.toString());
        sb.append(", Stoichiometry: ");
        sb.append(mStoichiometry);
        sb.append(", Dynamic: ");
        sb.append( mDynamic );
        sb.append("]");
        return(sb.toString());
    }
}


