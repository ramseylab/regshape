package isb.chem.scripting.tp;

import isb.chem.scripting.*;
import java.io.*;

public class PreprocessCondensedCommandLanguageScript
{
    public static void main(String []pArgs)
    {
        try
        {
            if(pArgs.length < 1)
            {
                System.err.println("required argument: filename");
                System.exit(1);
            }
            String inputFileName = pArgs[0];
            FileReader inputFileReader = new FileReader(new File(inputFileName));
            BufferedReader inputFileBufferedReader = new BufferedReader(inputFileReader);
            CondensedCommandLanguagePreprocessor preprocessor = new CondensedCommandLanguagePreprocessor("myModel", "univ", "sb");
            PrintWriter pw = new PrintWriter(System.out);
            preprocessor.translate(inputFileBufferedReader, pw);
            System.out.flush();
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
