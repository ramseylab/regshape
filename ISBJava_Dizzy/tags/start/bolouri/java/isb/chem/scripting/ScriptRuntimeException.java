package isb.chem.scripting;

public class ScriptRuntimeException extends Exception
{
    public ScriptRuntimeException(String pMessage)
    {
        super(pMessage);
    }

    public ScriptRuntimeException(String pMessage, Throwable pCause)
    {
        super(pMessage, pCause);
    }
}
