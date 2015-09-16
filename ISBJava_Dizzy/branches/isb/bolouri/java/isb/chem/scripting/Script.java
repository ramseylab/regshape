package isb.chem.scripting;

import java.util.*;
import java.io.*;
import isb.chem.*;

/**
 * Represents an abstract notion of a &quot;script&quot;.
 * A script is an ordered collection of <code>Statement</code>
 * objects that can be executed by the {@link ScriptRuntime}.
 * The <code>ScriptRuntime</code> contains all of the state
 * information and data structures built based on the
 * instructions in the <code>Script</code>; the 
 * <code>Script</code> just contains instructions
 * that are encoded in the <code>Statement</code> objects.
 *
 * @see IScriptBuildingParser
 * @see ScriptBuilder
 * @see ScriptRuntime
 *
 * @author Stephen Ramsey
 */
public class Script
{
    /*========================================*
     * constants
     *========================================*/
    /*========================================*
     * inner classes
     *========================================*/



    
    /*========================================*
     * member data
     *========================================*/
    private Vector mStatements;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private Vector getStatements()
    {
        return(mStatements);
    }

    private void setStatements(Vector pStatements)
    {
        mStatements = pStatements;
    }

    /*========================================*
     * initialization methods
     *========================================*/

    /*========================================*
     * constructors
     *========================================*/
    public Script()
    {
        setStatements(new Vector());
    }

    /*========================================*
     * private methods
     *========================================*/


    /*========================================*
     * protected methods
     *========================================*/


    Iterator getStatementsIter()
    {
        return(getStatements().iterator());
    }

    /*========================================*
     * public methods
     *========================================*/
    /**
     * Clear (remove) all statements from this script object.
     */
    public void clear()
    {
        getStatements().clear();
    }

    /**
     * Add the specified <code>pStatement</code> to this
     * script object, at the end of the script.
     */
    void addStatement(Statement pStatement)
    {
        getStatements().add(pStatement);
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        Iterator statementsIter = getStatementsIter();
        while(statementsIter.hasNext())
        {
            Statement statement = (Statement) statementsIter.next();
            sb.append(statement.toString() + "\n");
        }
        return(sb.toString());
    }


}
