package org.systemsbiology.chem;

import org.systemsbiology.util.DataNotFoundException;
import org.systemsbiology.util.InvalidInputException;

public interface ISimulator
{
    public boolean isInitialized();

    public void initialize(Model pModel, 
                           SimulationController pSimulationController) throws DataNotFoundException, InvalidInputException;

    public void simulate(double pStartTime, 
                         double pEndTime,
                         int pNumTimePoints,
                         int pNumSteps,
                         String []pRequestedSymbolNames,
                         double []pRetTimeValues,
                         Object []pRetSymbolValues) throws DataNotFoundException, IllegalStateException, IllegalArgumentException;
}
