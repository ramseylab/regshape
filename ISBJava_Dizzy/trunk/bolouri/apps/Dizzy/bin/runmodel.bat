set ISBCHEMAPPDIR=c:\Program Files\@APPNAME@
set ISBCHEMAPPCLASSPATH=%ISBCHEMAPPDIR%\lib\ISBJava.jar;%ISBCHEMAPPDIR%\lib\SBWCore.jar;%ISBCHEMAPPDIR%\lib\jfreechart.jar;%ISBCHEMAPPDIR%\lib\jcommon.jar;%ISBCHEMAPPDIR%\lib\SBMLValidate.jar;%ISBCHEMAPPDIR%\lib\Jama.jar;%ISBCHEMAPPDIR%\lib\odeToJava.jar
java @APPJAVARUNTIMEFLAGS@ -cp "%CLASSPATH%;%ISBCHEMAPPCLASSPATH%" org.systemsbiology.chem.app.ModelLoaderApp %1 %2 %3 %4 %5 %6

