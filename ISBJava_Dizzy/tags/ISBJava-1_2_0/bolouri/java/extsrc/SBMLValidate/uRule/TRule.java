package uRule;

import SBMLValidate.*;
import uBaseSymbol.*;

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
        public static final Type PARAMETER = new Type(NOMService.RULE_TYPE_PARAMETER);
        public static final Type SPECIES = new Type(NOMService.RULE_TYPE_SPECIES);
        public static final Type COMPARTMENT = new Type(NOMService.RULE_TYPE_COMPARTMENT);
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
