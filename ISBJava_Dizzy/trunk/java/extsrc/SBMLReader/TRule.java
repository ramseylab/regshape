package edu.caltech.sbml;

/*
** Copyright 2004 Institute for Systems Biology, Seattle Washington
**
** This library is free software; you can redistribute it and/or modify it
** under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 2.1 of the License, or
** any later version.
**
** This library is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
** documentation provided hereunder is on an "as is" basis, and the
** California Institute of Technology and Japan Science and Technology
** Corporation have no obligations to provide maintenance, support,
** updates, enhancements or modifications.  In no event shall the
** California Institute of Technology or the Japan Science and Technology
** Corporation be liable to any party for direct, indirect, special,
** incidental or consequential damages, including lost profits, arising
** out of the use of this software and its documentation, even if the
** California Institute of Technology and/or Japan Science and Technology
** Corporation have been advised of the possibility of such damage.  See
** the GNU Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public License
** along with this library; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
**
** The original code contained here was initially developed by:
**
**     Stephen RAmsey
**     Institute for Systems Biology
**     1441 N 34th St
**     Seattle, WA 98103
**
** Contributor(s):
**
*/

public class TRule extends TBaseSymbol 
{
    public static class Type
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
        public static final Type PARAMETER = new Type(SBMLReader.RULE_TYPE_PARAMETER);
        public static final Type SPECIES = new Type(SBMLReader.RULE_TYPE_SPECIES);
        public static final Type COMPARTMENT = new Type(SBMLReader.RULE_TYPE_COMPARTMENT);
    }

    private Type mType;
    private String mFormula;

    public String getFormula()
    {
        return(mFormula);
    }
    
    public Type getType()
    {
        return(mType);
    }

    public TRule(String pName, String pFormula, Type pType)
    {
        this.Name = pName;
        mFormula = pFormula;
        mType = pType;
    }
}
