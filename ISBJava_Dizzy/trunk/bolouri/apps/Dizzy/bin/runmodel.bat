set ISBCHEMAPPDIR=c:\Program Files\@APPNAME@
set ISBCHEMAPPCLASSPATH=%ISBCHEMAPPDIR%\lib\ISBJava.jar;%ISBCHEMAPPDIR%\lib\SBWCore.jar;%ISBCHEMAPPDIR%\lib\jfreechart.jar;%ISBCHEMAPPDIR%\lib\jcommon.jar;%ISBCHEMAPPDIR%\lib\SBMLValidate.jar;%ISBCHEMAPPDIR%\lib\Jama.jar;%ISBCHEMAPPDIR%\lib\odeToJava.jar
java @APPJAVARUNTIMEFLAGS@ -cp "%CLASSPATH%;%ISBCHEMAPPCLASSPATH%" org.systemsbiology.chem.app.SimulationLauncherCommandLine %1 %2 %3 %4 %5 %6 %7 %8 %9 %10 %11 %12 %13 %14 %15 %16 %17 %18 %19 %20 %21 %22 %23 %24 %25 %26 %27 %28 %29 %30
