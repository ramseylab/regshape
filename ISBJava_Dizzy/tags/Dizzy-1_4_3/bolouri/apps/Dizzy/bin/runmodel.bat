set ISBCHEMAPPDIR=c:\Program Files\@APPNAME@
set ISBCHEMAPPCLASSPATH=%ISBCHEMAPPDIR%\lib\ISBJava.jar;%ISBCHEMAPPDIR%\lib\SBWCore.jar;%ISBCHEMAPPDIR%\lib\jfreechart.jar;%ISBCHEMAPPDIR%\lib\jcommon.jar;%ISBCHEMAPPDIR%\lib\SBMLValidate.jar;%ISBCHEMAPPDIR%\lib\Jama.jar;%ISBCHEMAPPDIR%\lib\odeToJava.jar
set args=%1 %2 %3 %4 %5 %6 %7 %8 %9
shift 
shift
shift
shift
shift
shift
shift
shift
shift
set args2=%args% %1 %2 %3 %4 %5 %6 %7 %8 %9
shift
shift
shift
shift
shift
shift
shift
shift
shift
set args3=%args2% %1 %2 %3 %4 %5 %6 %7 %8 %9
java @APPJAVARUNTIMEFLAGS@ -cp "%CLASSPATH%;%ISBCHEMAPPCLASSPATH%" org.systemsbiology.chem.app.SimulationLauncherCommandLine %args3%
