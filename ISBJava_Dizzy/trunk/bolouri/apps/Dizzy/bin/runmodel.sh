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
${INSTALL_DIR}/lib/Jama.jar:\
${INSTALL_DIR}/lib/jh.jar:\
${INSTALL_DIR}/lib/colt.jar

${JAVA_BIN} -Xmx@APP_JAVA_MAX_HEAP_SIZE_BYTES@ -cp "${CLASSPATH}" 'org.systemsbiology.chem.app.SimulationLauncherCommandLine' $@
