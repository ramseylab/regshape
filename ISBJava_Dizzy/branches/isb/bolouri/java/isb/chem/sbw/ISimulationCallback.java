package isb.chem.sbw;

import edu.caltech.sbw.*;

interface ISimulationCallback
{
    void onError(String pErrorMessage) throws SBWException;
    void onRowData(double []pData) throws SBWException;
}
