#!/bin/bash
ISBCHEMAPPDIR=${HOME}/@APPNAME@
ISBCHEMAPPCLASSPATH=${ISBCHEMAPPDIR}/lib/ISBJava.jar:${ISBCHEMAPPDIR}/lib/SBWCore.jar:${ISBCHEMAPPDIR}/lib/jfreechart.jar:${ISBCHEMAPPDIR}/lib/jcommon.jar:${ISBCHEMAPPDIR}/lib/SBMLValidate.jar
java @APPJAVARUNTIMEFLAGS@ -cp "${CLASSPATH}:${ISBCHEMAPPCLASSPATH}" 'org.systemsbiology.chem.app.ModelLoaderApp' $@

