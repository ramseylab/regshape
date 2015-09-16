package org.systemsbiology.util;

public class InvalidInputException extends Exception
{
    public InvalidInputException(String pMessage)
    {
        super(pMessage);
    }

    public InvalidInputException(String pMessage, Throwable pCause)
    {
        super(pMessage, pCause);
    }
}
