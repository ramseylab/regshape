package isb.chem.scripting;

import java.io.*;
import isb.util.*;

/**
 * Represents a general interface for a scripting translator,
 * which is a class that can convert an input text stream into
 * an output text stream.  An example implementation of this
 * interface is the {@link CondensedCommandLanguagePreprocessor} class.
 *
 * @see CondensedCommandLanguagePreprocessor
 *
 * @author Stephen Ramsey
 */
public interface ITranslator
{
    void translate(BufferedReader pInput,
                   PrintWriter pOutput) throws InvalidInputException, IOException;
    void generatePreamble(PrintWriter pOutput) throws IOException;
}
