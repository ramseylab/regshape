set DIZZYDIR=c:\Program Files\Dizzy
set DIZZYPATH=%DIZZYDIR%\lib\ISBJava.jar;%DIZZYDIR%\lib\SBWCore.jar;%DIZZYDIR%\lib\jfreechart.jar;%DIZZYDIR%\lib\jcommon.jar
java -Xmx500mb -cp "%CLASSPATH%;%DIZZYPATH%" isb.chem.scripting.MainScriptRunner %1 %2 %3 %4 %5 %6

