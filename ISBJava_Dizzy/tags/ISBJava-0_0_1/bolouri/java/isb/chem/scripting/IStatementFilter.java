package isb.chem.scripting;

/**
 * Defines an interface for a filter class
 * that can permit or reject a {@link Statement}
 *
 */
public interface IStatementFilter
{
    boolean allowStatement(Statement pStatement);
}
