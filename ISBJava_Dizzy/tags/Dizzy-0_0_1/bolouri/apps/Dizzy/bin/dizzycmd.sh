#!/bin/bash
DIZZYDIR=/local/Dizzy
DIZZYPATH=${DIZZYDIR}/lib/ISBJava.jar:${DIZZYDIR}/lib/SBWCore.jar:${DIZZYDIR}/lib/jfreechart.jar:${DIZZYDIR}/lib/jcommon.jar:${DIZZYDIR}/lib/SBMLValidate.jar
java -Xmx500mb -cp "${CLASSPATH}:${DIZZYPATH}" 'org.systemsbiology.chem.app.ModelLoaderApp' $@

