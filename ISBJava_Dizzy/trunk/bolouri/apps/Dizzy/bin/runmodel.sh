#!/bin/bash
#----------------------------------------
# TO BE CONFIGURED BY THE USER:
JAVA_BIN=java
INSTALL_DIR=..
#----------------------------------------

CLASSPATH=${INSTALL_DIR}/lib/ISBJava.jar:\
${INSTALL_DIR}/lib/SBWCore.jar:\
${INSTALL_DIR}/lib/jfreechart.jar:\
${INSTALL_DIR}/lib/jcommon.jar:\
${INSTALL_DIR}/lib/SBMLReader.jar:\
${INSTALL_DIR}/lib/odeToJava.jar:\
${INSTALL_DIR}/lib/Jama.jar

${JAVA_BIN} -Xmx@APPJAVAMAXHEAPSIZEBYTES@ -cp "${CLASSPATH}" 'org.systemsbiology.chem.app.SimulationLauncherCommandLine' $@
