package org.systemsbiology.math;

import org.systemsbiology.util.*;
import java.util.*;

public abstract class ReservedSymbolMapper
{
    public abstract boolean isReservedSymbol(Symbol pSymbol);
    public abstract double getReservedSymbolValue(Symbol pSymbol, SymbolEvaluator pSymbolEvaluator) throws DataNotFoundException;
    public abstract Collection getReservedSymbolNames();
}
