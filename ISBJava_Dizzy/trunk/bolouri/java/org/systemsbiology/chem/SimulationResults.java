package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.Date;

/**
 * Data structure that contains the results of a simulation.
 * This includes the time range for the simulation, the name
 * of the simulator used, the name of the chemical species
 * to be viewed, etc.
 */
public final class SimulationResults
{
    private String mSimulatorAlias;
    private double mStartTime;
    private double mEndTime;
    private SimulatorParameters mSimulatorParameters;
    private String []mResultsSymbolNames;
    private double []mResultsTimeValues;
    private Object []mResultsSymbolValues;
    private double []mResultsFinalSymbolFluctuations;
    private Date mResultsDateTime;

    public SimulationResults()
    {
        mResultsDateTime = new Date(System.currentTimeMillis());
        mSimulatorAlias = null;
        mSimulatorParameters = null;
        mResultsSymbolNames = null;
        mResultsTimeValues = null;
        mResultsSymbolValues = null;
        mResultsFinalSymbolFluctuations = null;
    }

    public double []getResultsFinalSymbolFluctuations()
    {
        return(mResultsFinalSymbolFluctuations);
    }

    public void setResultsFinalSymbolFluctuations(double []pResultsFinalSymbolFluctuations)
    {
        mResultsFinalSymbolFluctuations = pResultsFinalSymbolFluctuations;
    }

    public String getSimulatorAlias()
    {
        return(mSimulatorAlias);
    }    

    public double getStartTime()
    {
        return(mStartTime);
    }

    public double getEndTime()
    {
        return(mEndTime);
    }

    public SimulatorParameters getSimulatorParameters()
    {
        return(mSimulatorParameters);
    }

    /**
     * Returns an array containing the names of the symbols
     * for which the user requested to view the time-series
     * data results for the simulation.
     */
    public String []getResultsSymbolNames()
    {
        return(mResultsSymbolNames);
    }

    /**
     * Returns an array containing the time values of the
     * time points at which the symbols (requested by the
     * user) were evaluated.
     */
    public double []getResultsTimeValues()
    {
        return(mResultsTimeValues);
    }

    /**
     * A two-dimensional array of doubles containing the
     * actual values of the symbols requested by the user.
     * The first index identifies the time point, and returns
     * an array of doubles.  That array of doubles is of the
     * same length as the number of symbols requested by the
     * user; it contains the values of the corresponding
     * symbols at the time point identified by the first array
     * index; schematically, access would look like this:
     * <code>
     * value = ((double [])resultsSymbolValues[timeIndex])[symbolIndex]
     * </code>
     */
    public Object []getResultsSymbolValues()
    {
        return(mResultsSymbolValues);
    }

    public void setSimulatorAlias(String pSimulatorAlias)
    {
        mSimulatorAlias = pSimulatorAlias;
    }

    public void setStartTime(double pStartTime)
    {
        mStartTime = pStartTime;
    }

    public void setEndTime(double pEndTime)
    {
        mEndTime = pEndTime;
    }
    
    public void setSimulatorParameters(SimulatorParameters pSimulatorParameters)
    {
        mSimulatorParameters = pSimulatorParameters;
    }

    public void setResultsSymbolNames(String []pResultsSymbolNames)
    {
        mResultsSymbolNames = pResultsSymbolNames;
    }

    public void setResultsTimeValues(double []pResultsTimeValues)
    {
        mResultsTimeValues = pResultsTimeValues;
    }

    public void setResultsSymbolValues(Object []pResultsSymbolValues)
    {
        mResultsSymbolValues = pResultsSymbolValues;
    }

    public Date getResultsDateTime()
    {
        return(mResultsDateTime);
    }
}

