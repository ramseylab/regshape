package isb.chem.scripting;

/**
 * Statement filter class that allows only statements of
 * the &quot;definition&quot; category to pass the filter.
 * This means that only model definition statements are allowed.
 * Other types of statements (&quot;simulate&quot;, &quot;export&quot;,
 * etc.) are not permitted by this filter.
 *
 * @author Stephen Ramsey
 */
public class StatementFilterDefinitionCategory implements IStatementFilter
{
    public boolean allowStatement(Statement pStatement)
    {
        return(pStatement.getType().getCategory().equals(Statement.Category.DEFINITION));
    }
}
