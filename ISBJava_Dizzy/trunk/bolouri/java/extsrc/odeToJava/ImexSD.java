package odeToJava;

import odeToJava.modules.*;
import odeToJava.ssCtrlModules.*;
import java.io.*;

/*
   class contains an algorithm for integrating an ODE using an Imex
   Runge-Kutta routine with Step Doubling
*/
public class ImexSD
{
    // static methods (used as the interface for solver)

    public static void imex_sd(ODE function, Span tspan, double[] u0, double h0, Btableau butcher, double atol, double rtol, String fileName, String stats)
    {
	ImexSD imexsd = new ImexSD(function, tspan, u0, h0, butcher, atol, rtol, fileName, stats);   // initialize the IMEX with Step Doubling object

	imexsd.routine();   // run the routine
    }

     // constructors

     /*
        constructor sets up the class to do a Runge-Kutta scheme given the ODE,
        an interval of (temporal) integration, an initial value, an intial stepsize,
        a Butcher tableau, two tolerances, and a few strings for special features
     */
     public ImexSD(ODE function, Span tspan, double[] u0, double h0, Btableau butcher, double atol, double rtol, String fileName, String stats)
     {
          // span check

          if(!tspan.get_property())   // if the span is out of order halt immediately
          {   // with message
               System.out.println("Improper span: times are out of order");
               System.exit(0);   // halt
          }

          // general initializations / calculations

          f = function;   // get the ODE function
          this.butcher = butcher;   // get the Butcher tableau

          this.t0 = tspan.get_t0();   // get initial and final times
          this.tf = tspan.get_tf();

          this.atol = atol;   // get absolute tolerance
          this.rtol = rtol;   // get relative tolerance
          this.h = h0;   // get initial step size

          n = u0.length;   // get dimension of problem

          eta1 = new double[n];   // initialize error evaluation arrays
          eta2 = new double[n];

          if(h0 <= 0)   // error testing for h0 (must be greater than zero)
          {
               System.out.println("Initial stepsize must be greater than zero");
               System.exit(0);
          }

          if(atol <= 0)   // error testing for atol (must be greater than zero)
          {
               System.out.println("Abolute tolerance must be greater than zero");
               System.exit(0);
          }

          if(rtol <= 0)   // error testing for rtol (must be greater than zero)
          {
               System.out.println("Relative tolerance must be greater than zero");
               System.exit(0);
          }
          
          // parameter independent initializations

          p = 3;   // p = 3 is a good choice for most cases (IMEX443 and ones like it)

          nreject = 0;   // no step rejections yet
          naccept = 0;   // nor step acceptions

          told = t0;   // start told off with t0

          uold_e1 = new double[n];   // set up the initial function
          StdMet.arraycpy(uold_e1, u0);   // values for error estimation
          uold_e2 = new double[n];
          StdMet.arraycpy(uold_e2, u0);

          lastStep = false;   // we are not done nor at our last step
          done = false;

          // initializations for miscellaneous special features

          if(stats.equals("Stats_On"))   // the statistics feature
          {
               this.stats_on = true;
               this.stats_intermediate = false;
          }
          else
               if(stats.equals("Stats_Intermediate"))
               {
                    this.stats_on = true;
                    this.stats_intermediate = true;
               }
               else
                    if(stats.equals("Stats_Off"))
                    {
                         this.stats_on = false;
                         this.stats_intermediate = false;
                    }
                    else
                    {
                         System.out.println("String parameter must be either: 1) \"Stats_On\" 2) \"Stats_Intermediate\" or 3) \"Stats_Off\"");
                         System.exit(0);
                    }

	  this.fileName = fileName;   // the file writing feature

          // output warnings for cases that are not worth stopping the program for

          if(tspan.get_timesLength() > 0)   // if user has entered in a time span that
          {   // suggests interpolation, notify user that interpolation will not be done
               System.out.println();
               System.out.println("note that this solver does not do interpolation . . .");
               System.out.println();
          }
     }

     // methods

     /*
        method computes the solution to the ODE depending on parameters and calculations
	given to and done by the constructor
     */
    public void routine()
    {
        // beginning message

        System.out.println();
        System.out.println("Begin Implicit-Explicit Runge-Kutta /w Step Doubling Routine . . .");
        System.out.println();   // leave a space at start (for screen output)

        // some temporary variables

        Imex1S imex1s = new Imex1S(butcher);   // initialize the IMEX stepper

        double[] utemp = new double[n];   // temporary array to store uold
        double[] eta2temp = new double[n];   // temporary array to store first
        // part of the error calculation
        double[] diff1 = new double[n];   // temporary array for difference of 2 arrays
        double hNew;   // temporary variable to hold tentative new step size
        double norm;   // used to test to see if solver has gone unstable

        // open a writer for the solution file (solution at each step)

        ODEFileWriter writer = new ODEFileWriter();
        writer.openFile(fileName);

        // the loop

        while(!done)
        {
            if(lastStep)   // if we are at the last step then next it will be
                done = true;   // done
      
            // the eta values
         
            // eta1

            eta1 = imex1s.doOneStep(f, told, uold_e1, h);  // do a step with h
         
            StdMet.arraycpy(utemp, uold_e1);   // record utemp
            StdMet.arraycpy(uold_e1, eta1);   // update with step

            // eta2
         
            eta2temp = imex1s.doOneStep(f, told, uold_e2, h/2);   // do a step
            eta2 = imex1s.doOneStep(f, told + h/2, eta2temp, h/2);   // with h/2
         
            StdMet.arraycpy(uold_e2, eta2);   // update with double step
            told += h;   // update h                  
  
            // error estimation

		    double[] estimation = ErrorEstimator.stepdoubling_estimate(h, eta1, eta2, utemp, atol, rtol, p, amax, alpha);

		    hNew = estimation[0];   // get required information from estimate
		    hopt = estimation[1];
            norm = estimation[2];

            if(norm != norm)   // check to see if norm is NaN, if
            {   // so, something has gone wrong, solver is unstable
                System.out.println("unstable . . . aborting");
                System.out.println("accepted: " + naccept);
                System.out.println("rejected: " + nreject);

                writer.closeFile();   // close the writer before halting

                return;   // halt routine
            }
         
            if((tf - told) <= hNew)   // when executing the last step, the step
            {   // that puts us right on tf may be smaller than suggested step
                hNew = tf - told;   // size, so we take the smaller to land
                lastStep = true;   // right on tf, at this point we know we have
            }   // taken the last step

            if(done)   // when done, output all of the statistics involved in
            {   // step doubling and program is done
                if((h/hopt) > 3)   // in the very odd case that the stretched step
                {   // is rejected we:
                    done = false;   // set done and last step to false, because it is
                    lastStep = false;   // doing at least two more steps
                    nreject++;   // we reject a step
                    told -= h;
                    StdMet.arraycpy(uold_e1, utemp);
                    StdMet.arraycpy(uold_e2, utemp);
                    h = h/2;   // we cut h in half, seeing as neither half will be too small

                    if(stats_on)   // output statistics (if user has chosen so), note that
                    {   // time and solution do not change so we do not output such
                        if(!stats_intermediate)   // do not output if only on
                        {   // intermediate statistics mode
                            System.out.println("rejected");
                            System.out.println("new h = " + h);
                            System.out.println();
                        }
                    }
                }
                else
                {
                    naccept++;   // last step is obviously accepted
                    System.out.println("done");
                    System.out.println("final t = " + told);
                    System.out.println("final u =");
                    StdMet.arrayprt(uold_e2);
                    System.out.println();
                    System.out.println("# of rejections = " + nreject);
                    System.out.println("# of accepted steps = " + naccept);

                    writer.writeToFile(told, uold_e2);   // output new solution array	thus far (into file)
                    handleRecord(told, uold_e2);
                }
            }
            else
                if(lastStep || ((h/hopt) <= 3))   // if on our last step
                {   // (where step is usually very small) or optimal h is close
                    StdMet.arraycpy(uold_e1, uold_e2);   // enough to h, accept step and
                    h = hNew;   // increment related counters
                    naccept++;

                    writer.writeToFile(told, uold_e2);   // output new solution thus far (into file)
                    handleRecord(told, uold_e2);

                    if(stats_on)   // output statistics (if user has chosen
                    {   // such)
                        if(!stats_intermediate)   // do not output if only on
                        {   // intermediate statistics mode
                            System.out.println("accepted");
                            System.out.println("new h = " + h);
                        }

                        System.out.println("t = " + told);

                        if(!stats_intermediate)   // do not output if only on
                        {   // intermediate statistics mode
                            System.out.println("solution = ");
                            StdMet.arrayprt(uold_e2);
                        }

                        System.out.println();
                    }
                }
                else   // else we reject the step and increment related counters
                {
                    told -= h;
                    StdMet.arraycpy(uold_e1, utemp);
                    StdMet.arraycpy(uold_e2, utemp);
                    h = hNew;
                    nreject++;

                    if(stats_on)   // output statistics (if user has chosen so), note that
                    {   // time and solution do not change so do not output such
                        if(!stats_intermediate)   // do not output if only on
                        {   // intermediate statistics mode
                            System.out.println("rejected");
                            System.out.println("new h = " + h);
                            System.out.println();
                        }
                    }
                }
        }      

        writer.closeFile();   // now that we are done, close the writer
    }

    public void setRecorder(ODERecorder pRecorder)
    {
        recorder = pRecorder;
    }

    public void handleRecord(double t, double []x)
    {
        if(null != recorder)
        {
            recorder.record(t, x);
        }
    }

    private ODERecorder recorder;

    // instance variables

    private ODE f;   // the function
    private Btableau butcher;   // the Butcher tableau

    private double t0;   // the initial and final times
    private double tf;

    private int n;   // dimension of ODE
    private double[] unew;   // final function value
   
    private double atol;   // absolute tolerance
    private double rtol;   // relative tolerance
    private double h;   // stepsize
    private double hopt;   // optimal stepsize

    private double told;   // time in each step
    private double[] uold_e1;   // function value for eta1
    private double[] uold_e2;   // function value for eta2

    private String fileName;   // name of file solution is written to (each step)
    private boolean stats_on;   // whether to report status at each step
    private boolean stats_intermediate;   // whether to report just a few statistics

    private final double alpha = 0.9;   // safety factor
    private final double amax = 5.0;   // step size growth limit
    private double p;   // order of IMEX routine
   
    private int nreject;   // number of rejected steps counter
    private int naccept;   // number of accepted steps counter

    private boolean done;   // termination switch
    private boolean lastStep;   // switch verifies if loop of routine is on last step

    // variables involved in the routine method
   
    private double[] eta1;   // result of stepping with h
    private double[] eta2;   // result of stepping with h/2
}
