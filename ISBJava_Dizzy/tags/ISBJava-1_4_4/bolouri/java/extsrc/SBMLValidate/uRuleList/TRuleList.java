package uRuleList;

import uRule.*;
import java.util.*;

public class TRuleList 
{
    ArrayList mList;
    public TRuleList()
    {
        mList = new ArrayList();
    }

    public TRule get(int pIndex)
    {
        return((TRule) mList.get(pIndex));
    }

    public void set(int pIndex, TRule pItem)
    {
        mList.set(pIndex, pItem);
    }

    public int add(String pName, String pFormula, TRule.Type pType)
    {
        return(add(new TRule(pName, pFormula, pType)));
    }

    public int add(TRule pRule)
    {
        mList.add(pRule);
        return(size() - 1);
    }

    public int size()
    {
        return(mList.size());
    }
}
