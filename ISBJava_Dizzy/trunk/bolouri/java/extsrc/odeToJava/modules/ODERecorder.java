package odeToJava.modules;

/**
 * User-defined code that is called after each
 * successful iteration of the ODE solver.  Useful
 * for updating state in the ODE system, or checking
 * constraints.  To be used with the ODE solvers 
 * ImexSD and ErkTriple.  This is a new interface
 * that was introduced into a modified version of
 * the odeToJava package by Stephen Ramsey on 2004/02/06.
 *
 * @author Stephen Ramsey
 */
public interface ODERecorder
{
    public void record(double t, double []x);
}
