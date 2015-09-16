package odeToJava;

import odeToJava.modules.*;
import odeToJava.ssCtrlModules.*;
import java.io.*;

/*
   class is an explicit Runge-Kutta triple solver for any ODE.  It
   features stiffness detection and also event location provided that
   an intepolant has been defined for the Runge-Kutta scheme
*
* Originally written by Murray Patterson
*
* Modified by Stephen Ramsey in order to allow for a "record()" function
* to be called on a ODERecorder object, after each successful iteration of
* the ODE solver.  This provides a "hook" for user-defined code to be
* invoked to update state in the ODE system, or to check for constraint
* satisfaction.  (2004/02/06)
*/
public class ErkTriple
{
    // static methods (used as the interface to the solver)

    public static double[] erk_triple(ODE function, Span tspan, double[] x0, double h0, Btableau butcher, double highOrder, double[] atol, double[] rtol, String fileName, String stiffnessDet, String eventLoc, String stats)
    {
	ErkTriple erkTr = new ErkTriple(function, tspan, x0, h0, butcher, highOrder, atol, rtol, fileName, stiffnessDet, eventLoc, stats);   // initialize the ERK Triple object

	erkTr.setAppend(false);   // whether to append to a non-empty file defaults to false
	erkTr.setNPoints(1000);   // amount of points to write to file defaults to 1000

	erkTr.routine();   // run the routine

	return(erkTr.getProfile());   // return profile (for event location purposes)
    }

    public static double[] erk_triple(ODE function, Span tspan, double[] x0, double h0, Btableau butcher, double highOrder, double[] atol, double[] rtol, String fileName, String stiffnessDet, String eventLoc, String stats, String append)
    {
	ErkTriple erkTr = new ErkTriple(function, tspan, x0, h0, butcher, highOrder, atol, rtol, fileName, stiffnessDet, eventLoc, stats);   // initialize the ERK Triple object

	erkTr.setAppend(true);   // append feature set to true, given a String
	erkTr.setNPoints(1000);   // amount of points to write to file defaults to 1000

	erkTr.routine();   // run the routine

	return(erkTr.getProfile());   // return profile (for event location purposes)
    }

    public static double[] erk_triple(ODE function, Span tspan, double[] x0, double h0, Btableau butcher, double highOrder, double[] atol, double[] rtol, String fileName, String stiffnessDet, String eventLoc, String stats, int nPoints)
    {
	ErkTriple erkTr = new ErkTriple(function, tspan, x0, h0, butcher, highOrder, atol, rtol, fileName, stiffnessDet, eventLoc, stats);   // initialize the ERK Triple object

	erkTr.setAppend(false);   // whether to append to a non-empty file defaults to false
	erkTr.setNPoints(nPoints);   // amount of points to write to file set to value specified by user

	erkTr.routine();   // run the routine

	return(erkTr.getProfile());   // return profile (for event location purposes)
    }

    public static double[] erk_triple(ODE function, Span tspan, double[] x0, double h0, Btableau butcher, double highOrder, double[] atol, double[] rtol, String fileName, String stiffnessDet, String eventLoc, String stats, String append, int nPoints)
    {
	ErkTriple erkTr = new ErkTriple(function, tspan, x0, h0, butcher, highOrder, atol, rtol, fileName, stiffnessDet, eventLoc, stats);   // initialize the ERK Triple object

	erkTr.setAppend(true);   // append feature set to true, given a String
	erkTr.setNPoints(nPoints);   // amount of points to write to file set to value specified by user

	erkTr.routine();   // run the routine

	return(erkTr.getProfile());   // return profile (for event location purposes)
    }

    public static double[] erk_triple(ODE function, Span tspan, double[] x0, double h0, Btableau butcher, double highOrder, double[] atol, double[] rtol, String fileName, String stiffnessDet, String eventLoc, String stats, int nPoints, String append)
    {
	ErkTriple erkTr = new ErkTriple(function, tspan, x0, h0, butcher, highOrder, atol, rtol, fileName, stiffnessDet, eventLoc, stats);   // initialize the ERK Triple object

	erkTr.setAppend(true);   // append feature set to true, given a String
	erkTr.setNPoints(nPoints);   // amount of point to write to file set to value specified by user

	erkTr.routine();   // run the routine

	return(erkTr.getProfile());   // return profile (for event location purposes)
    }

    // helper methods (for the static methods)

    public void setAppend(boolean append)
    {
	this.append = append;
    }

    public void setNPoints(int nPoints)
    {
	this.nPoints = nPoints;
    }

     // constructors

     /*
        constructor sets up the class to do the ERK triple scheme given an ODE,
        an interval of temporal integration, an initial value, an initial stepsize,
        a Butcher tableau, a number denoting the order of the higher order scheme
        of the embedded Runge-Kutta scheme of the Butcher tableau, absolute and
        relative tolerance arrays, and a few strings for special features
     */
     public ErkTriple(ODE function, Span tspan, double[] x0, double h0, Btableau butcher, double highOrder, double[] atol, double[] rtol, String fileName, String stiffnessDet, String eventLoc, String stats)
     {
          /*
             we handle the case that the user has set the highOrder argument <= 0, and
             has thus specified an ERK constant step routine (and therefore solver will 
             now solve the ODE with a constant step)
          */
          if(highOrder <= 0)   // if highOrder <= 0
               this.constStep = true;   // toggle constStep (routine is now constant step)
          else   // otherwise, it is not
               this.constStep = false;

          // general initializations / calculations

          if(!tspan.get_property())   // if the span is out of order halt program
          {   // immediately with message
               System.out.println("Improper span: times are out of order");
               System.exit(0);   // halt
          }
          
          this.f = function;   // store the function
          this.t0 = tspan.get_t0();   // store initial and final
          this.tf = tspan.get_tf();   // points of time span
          this.butcher = butcher;   // default to Dormand-Prince Butcher tableau
          this.p = highOrder;   // store the higher order of the embedded scheme
             // i.e., for Dormand-Prince it is 5.0
          this.s = butcher.getbl();   // store how many stages this Runge-Kutta
             // scheme will execute in

          // initial value and tolerances

          this.x0 = new double[x0.length];
          StdMet.arraycpy(this.x0, x0);   // store the initial value

          if(!constStep)   // if routine is constant step: no automatic stepsize control,
          {   // therefore ignore the tolerances
               this.atol = new double[atol.length];
               StdMet.arraycpy(this.atol, atol);   // get the array of absolute tolerances
               this.rtol = new double[rtol.length];
               StdMet.arraycpy(this.rtol, rtol);   // get the array of relative tolerances
          }

          // Butcher tableau calculations / initializations

          this.a = new double[butcher.getal()][butcher.getal()];   // initialize
          this.b = new double[butcher.getbl()];   // a,b, bhat and c arrays of the

          if(!constStep)   // if routine is constant step: do not need embedded part of Butcher tableau
               this.bhat = new double[butcher.getbEmbl()];   // Butcher tableau of this

          this.c = new double[butcher.getcl()];   // class

          StdMet.matrixcpy(this.a, butcher.get_a());   // fill these a,b, bhat and c
          StdMet.arraycpy(this.b, butcher.get_b());   // arrays using Butcher tableau

          if(!constStep)   // if routine is constant step: do not need embedded part of Butcher tableau
               StdMet.arraycpy(this.bhat, butcher.get_bEmb());   // passed to this constructor
     
          StdMet.arraycpy(this.c, butcher.get_c());

          this.FSALenabled = butcher.get_FSALenabled();   // get from the Butcher
             // tableau whether the scheme is first same as last or not

          // general calculations

          this.n = x0.length;   // store dimension of ODE

          if(!constStep)   // if routine is constant step: no need for tolerances, so
          {   //  there is no need for checking them
               if(atol.length != n)   // test the length of atol (must be equal to the
               {   // the length of the initial value array)
                    System.out.println("Improper absolute tolerance array size");
                    System.exit(0);
               }

               if(rtol.length != n)   // test the length of rtol (must be equal to the
               {   // the length of the initial value array)
                    System.out.println("Improper relative tolerance array size");
                    System.exit(0);
               }
          
               for(int i= 0; i< n; i++)   // test the values in atol and rtol arrays
               {   // (all values must be greater than zero)
                    if(atol[i] <= 0.0)
                    {
                         System.out.println("All abolute tolerances in array must be greater than zero");
                         System.exit(0);
                    }

                    if(rtol[i] <= 0.0)
                    {
                         System.out.println("All relative tolerances in array must be greater than zero");
                         System.exit(0);
                    }
               }

               if(h0 <= 0.0)   // h calculation depends on the value of h0:
               {   // if h0 is less than or equal to 0 there is not a useful value of
                   // h so we call the initial stepsize selection routine and get h
                   // from it.  The idea here is that if the user wishes to use the
                   // initial stepsize selection routine, he or she just enters an
                   // h0 <= 0 (this also helps fool proof the system)
                    Initsss init = new Initsss(function, tspan, x0, atol, rtol);   // call
                    this.h = init.get_h();   // initial step size selection routine and
                       // get intitial step from it
               }
               else   // else assume the user wants to define his or her own intial
                    this.h = h0;   // step size, so we assign h to this value
          }

          if(constStep)   // if it is a constant step routine, the constant step
          {   // must be greater than zero and smaller than the integration interval
               if(h0 <= 0)   // error testing for h0 (must be greater than zero)
               {
                    System.out.println("Stepsize must be greater than zero");
                    System.exit(0);
               }

               if((tf - t0) < h0)   // test to see if stepsize is smaller than time span
               {
                    System.out.println("Stepsize is larger than tspan");
                    System.exit(0);   
               }

               this.h = h0;
          }

          /*
             section dealing with the more special features of the solver
          */

          // initializations for interpolation

          this.timesLength = tspan.get_timesLength();

          if(timesLength == 0 || butcher.get_btheta() == null)
               interpolant_on = false;
          else
          {
               interpolant_on = true;

               this.times = new double[timesLength];   // fill times array
               for(int i= 0; i< timesLength; i++)
                   times[i] = tspan.get_times()[i];
          }

          // initializations for stiffness detection

          if(!constStep)   // stiffness detection is not done in a constant step routine
          {
               if(stiffnessDet.equals("StiffDetect_Halt"))
               {
                    this.sdetect_on = true;   // assume it is true to begin with

                    /*
                       test to see if the Butcher tableau is the Dormand-Prince Butcher
                       tableau, otherwise stiffness detection is not done as the
                       Dormand-Prince scheme is the only scheme in this package that
                       offers stiffness detection
                    */

                    Btableau doprButcher = new Btableau("dopr54");   // Dormand-Prince Butcher tableau
                       // to refer to, to see if present scheme is Dormand-Prince

                    // test matrix a

                    for(int i= 0; i< s; i++)
                         for(int j= 0; j< s; j++)
                              if(butcher.get_a()[i][j] != doprButcher.get_a()[i][j])
                                   this.sdetect_on = false;
    
                    // test array b

                    for(int i= 0; i< s; i++)
                         if(butcher.get_b()[i] != doprButcher.get_b()[i])
                              this.sdetect_on = false;
                         
                    // test array bEmb
               
                    for(int i= 0; i< s; i++)
                         if(butcher.get_bEmb()[i] != doprButcher.get_bEmb()[i])
                              this.sdetect_on = false;
                      
                    // test array c
               
                    for(int i= 0; i< s; i++)
                         if(butcher.get_c()[i] != doprButcher.get_c()[i])
                              this.sdetect_on = false;

                    // handle the case that any of the above Butcher tableu tests failed

                    if(this.sdetect_on == false)
                    {
                         System.out.println("cannot do stiffness detection: Butcher scheme is not the Dormand-Prince scheme");
                         System.exit(0);
                    }
               }
               else
                    if(stiffnessDet.equals("StiffDetect_Off"))
                         this.sdetect_on = false;
                    else
                    {
                         System.out.println("String parameter must be either: 1) \"StiffDetect_Halt\" or 2) \"StiffDetect_Off\"");
                         System.exit(0);
                    }

               this.fevalTotal = 0;   // so far no function evaluations have been done
          }

          // initializations for event location

          if(!constStep)   // no event location done in a constant step routine
          {
               if(eventLoc.equals("EventLoc_Halt"))
                    this.eventLoc_on = true;
               else
                    if(eventLoc.equals("EventLoc_Off"))
                         this.eventLoc_on = false;
                    else
                    {
                         System.out.println("String parameter must be either: 1) \"EventLoc_Halt\" or 2) \"EventLoc_Off\"");
                         System.exit(0);
                    }
          }

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

          if(constStep)   // if routine is constant step: no stiffness detection or event
          {   // location, and user is notified of this and more
               sdetect_on = false;   // none of these offered in constant step routine
               eventLoc_on = false;
          
               System.out.println();
               System.out.println("Note that a constant step Runge Kutta scheme has been");
               System.out.println("specified for the Runge Kutta Triple Routine, so there");
               System.out.println("will be no automatic stepsize control, no stiffness");
               System.out.println("detection, and no event location");
               System.out.println();

               double temp = (tf - t0)/ h;   // if the integration interval is not an integer
               if(temp != Math.floor(temp))   // multiple of the stepsize, solver cannot possibly
               {   // step exactly to tf, so user is notified of this
                    System.out.println();
                    System.out.println("integration interval is not integer multiple of stepsize:");
                    System.out.println("problem will only be integrated to " + Math.floor(temp));
                    System.out.println();
               }
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
        System.out.println("Begin Runge Kutta Triple Routine . . .");
        System.out.println();   // leave a space at start (for screen output)

        // general initializations

        told = t0;   // initialize told to t0 (the starting time)

        xold = new double[n];   // initialize the arrays xold, and xnew, two
        xnew = new double[n];   // arrays that will represent each of the
        // arrays of the solution as it integrates
        StdMet.arraycpy(xold, x0);   // pass x0 to xold (initial value)
        xe = new double[n];   // initialize error estimate arrays

        K = new double[s][n];   // a matrix of K values

        nreject = 0;   // no step rejections yet
        naccept = 0;   // nor step acceptions

        firstStep = true;   // starting first step soon
        lastStep = false;   // not done nor at last step
        done = false;

        steps = (long)(Math.floor((tf - t0)/h));   // get approximation on number of steps
        // this routine will exectute in (used if routine is constant step)
        count = 0;   // counts number of steps done

        // interpolation initializations

        int times_inc = 0;   // starting at the first time
        int times_limit = timesLength;   // the final time

        double theta;   // where a specified time lies between 2 calculated time points
        double[] sigmaInterp = new double[n];   // stores an interpolated sum
        double[] x_interp = new double[n];   // stores an interpolated solution

        // stiffness detection initializations

        tFirst = told;   // start tFirst off at t0

        // event location initializations

        double thetaStarMin;   // this stores the smallest thetaStar found in event interval
        double toldWhen;   // stores the time when the last thetaStar was computed
        double signDet;   // determines if there is an event or not

        double thetaA;   // theta on the left (as we converge to thetaStar)
        double thetaB;   // theta on the right
        double ga;   // g function evaluation on one side
        double gb;   // g function evaluation on the other side
                                             
        double thetaStar;   // the value of thetaStar (as we converge to true answer)
                                                 
        // temporary variable inititializations

        double[] sigma = new double[n];   // the sum loop for each row of K
        double[] sigma2 = new double[n];   // the sum for lower order solution
        double[] sigma3 = new double[n];   // the sum for higher order solution
        double[] as1 = new double[n];   // temporary variable for an array sum
        double[] ad1 = new double[n];   // temporary variable for an array difference
        double[] dq1 = new double[n];   // temporary variable for a piecewise array division
        double[] stam1 = new double[n];   // temporary variable for a scalar*array
        double[] fp = new double[n];   // temporary variable to store function evaluation

        double[] g1 = new double[n];   // for calculating rho for stiffness
        double[] g2 = new double[n];   // detection

        double norm;   // used to test to see if solver has gone unstable (in constStep mode)

        // open a writer for the solution at each step

        ODEFileWriter writer = new ODEFileWriter();
  	  writer.openFile(fileName);

        /* begin integraion loop */

        while(!done)   // outer loop integrates the ODE
        {

            if(lastStep)   // if at the last step then next the integrator be
                done = true;   // done

            // double loop to calculate K matrix

            /*
              this loop calculates each row i in the K matrix using the
              Butcher tableau, its inner loop (the sum), and function
              evaluations
            */
            for(int i= 0; i< s; i++)   // loop for the K matrix
            {
                /*
                  this loop calculates the ith row of the K matrix
                  using the ith row of the a array of the given Butcher tableau
                  and all of the rows of K before it
                */
                for(int j= 0; j< i; j++)   // the loop for the row sum
                {
                    StdMet.stam(stam1, a[i][j], K[j]);   // a[i][j]*K[j]
                    StdMet.arraysum(sigma, sigma, stam1);  // sigma = sigma + a[i][j]*K[j]
                }

                if(!((i == 0) && !firstStep && FSALenabled))
                {
                    StdMet.arraycpy(g1, as1);   // get the second last
                    // g of the loop calculations

                    StdMet.stam(stam1, h, sigma);  // sigma = sigma*n
                    StdMet.arraysum(as1, xold, stam1);   // as1 = xold + h*stam1
                    fp = f.f(told + h*c[i], as1);   // fp = f(told + h*c[i], as1)
                    StdMet.arraycpy(K[i], fp);   // set ith row of the K matrix to function evaluation
                    StdMet.zero_out(sigma);   // set sigma array to array of zeros

                    StdMet.arraycpy(g2, as1);   // get the the last g of the
                    // loop calculations
                }
                else
                    if(justAccepted)   // do this only if previous step was accepted
                        StdMet.arraycpy(K[0], K[s - 1]);   // else we copy the last row of K
                // from previous step into first row of K of present step
            }

            // get xnew and xe

            /*
              this loop takes the weighted average of all of the rows in the
              K matrix using the b array of the Butcher tableau
              -> this loop is the weighted average for the higher order ERK method and
              is used to compute xnew
            */
            for(int i= 0; i< s; i++)   // loop for xnew
            {
                StdMet.stam(stam1, h*b[i], K[i]);   // h*b[i]*K[i]
                StdMet.arraysum(sigma2, sigma2, stam1);   // sigma2 = sigma2 + h*b[i]*K[i]
            }

            /*
              this loop computes the same weighted average as above except
              it is done using bhat instead of b, it is for the lower order ERK method
              and it is used to compare to the higher order method for error estimation
            */
            if(!constStep)   // compute embedded part if method is not constant step
            {
                for(int i= 0; i< s; i++)   // loop for error estimation
                {
                    StdMet.stam(stam1, h*bhat[i], K[i]);   // h*bhat[i]*K[i]
                    StdMet.arraysum(sigma3, sigma3, stam1);   // sigma3 = sigma3 + h*bhat[i]*K[i]
                }

                StdMet.arraysum(xe, xold, sigma3);   // xe = xold + sigma3
            }

            // xnew is obtained from the b array of the Butcher tableau

            StdMet.arraysum(xnew, xold, sigma2);   // xnew = xold + sigma2

            StdMet.zero_out(sigma2);   // set sigma2 array to array of zeros
            StdMet.zero_out(sigma3);   // set sigma3 array to array of zeros

            firstStep = false;   // whether accepted, rejected or constant step, first step is over
            justAccepted = false;   // default false (will toggle on if step is accepted)

            // event location routine

            if(eventLoc_on)
            {                              
                RootFinder rootFinder = new RootFinder(f, butcher, h, told, xold, K);

                thetaStarMin = 1.1;   // this stores the smallest thetaStar found
                // (make sure it is beyond the value that any theta can be)
                thetaStar = 1.1;   // assign thetaStar to this as well
                toldWhen = -1.0;   // assign this to -1 so that no times can be
                // mistaken for event times

                /*
                  we do a loop here to find the thetaStar for every component of g that an event occured,
                  since this is just simple event location, if several components had events we
                  take the smallest thetaStar to be the event (i.e., a maximum of one event per step)
                */
                for(int i= 0; i< f.g(told, xold).length; i++)
                {
                    signDet = f.g(told, xold)[i] * f.g(told + h, xnew)[i];

                    if(signDet < 0)
                    {
                        toldWhen = told;   // if there is an event, get the time that this event
                        // occured at so that we can interpolate between the right points

                        // g function evaluations and thetas on either side of event

                        thetaA = 0.0;   // theta on the left
                        thetaB = 1.0;   // theta on the right
                        ga = f.g(told, xold)[i];   // g function evaluation on one side
                        gb = f.g(told + h, xnew)[i];   // g function evaluation on the other side
                                             
                        rootFinder.setup(i);   // let the root finder know what iteration the event location routine is on

                        thetaStar = rootFinder.safeguarded_secant(thetaA, thetaB, ga, gb);   // find thetaStar with the safeguarded secant method

                    }
                                        
                    if(thetaStar <= thetaStarMin)   // if we find a smaller thetaStar
                        thetaStarMin = thetaStar;   // assign tMin to that smaller thetaStar                                                                           
                }
                         
                /*
                  here is where we actually act upon the event: if the time when
                  the last thetaStar calculation was this time, then we do something with
                  this time
                */
                if(toldWhen == told)
                {
                    h = thetaStarMin * h;   // we adjust h back to event

                    /*
                      now that we know that an event has occured, we adjust h back to
                      exactly where the event occured, then we go through another ERK
                      loop to recompute xnew and xe with the intention that they will
                      fall right on the event . . . we then halt the integration and
                      output a message and final solution
                    */

                    System.out.println("an event occured at t = " + (told + h));

                    // double loop to calculate K matrix

                    /*
                      this loop calculates each row i in the K matrix using the
                      Butcher tableau, its inner loop (the sum), and function
                      evaluations
                    */
                    for(int i= 0; i< s; i++)   // loop for K matrix
                    {
                        /*
                          this loop calculates the ith row of the K matrix
                          using the ith row of the a array of the given Butcher tableau
                          and all of the rows of K before it
                        */
                        for(int j= 0; j< i; j++)   // loop for each row
                        {
                            StdMet.stam(stam1, a[i][j], K[j]);   // a[i][j]*K[j]
                            StdMet.arraysum(sigma, sigma, stam1);  // sigma = sigma + a[i][j]*K[j]
                        }

                        if(!((i == 0) && !firstStep && FSALenabled))
                        {
                            StdMet.arraycpy(g1, as1);   // get the second last
                            // g of the loop calculations

                            StdMet.stam(stam1, h, sigma);  // sigma = sigma*h
                            StdMet.arraysum(as1, xold, stam1);   // as1 = xold + stam1
                            fp = f.f(told + h*c[i], as1);   // fp = f(told + h*c[i], as1)
                            StdMet.arraycpy(K[i], fp);   // set ith row of the K matrix to function evaluation
                            StdMet.zero_out(sigma);   // set sigma array to array of zeros

                            StdMet.arraycpy(g2, as1);   // get the last g of the
                            // loop calculations
                        }
                        else
                            if(justAccepted)   // do this only if previous step was accepted
                                StdMet.arraycpy(K[0], K[s - 1]);   // else we copy the last row
                        // from previous step into first row of present step
                    }

                    // get xnew and xe

                    /*
                      this loop takes the weighted average of all of the rows in the
                      K matrix using the b array of the Butcher tableau
                      -> this loop is the weighted average for the higer order ERK method and
                      is used to compute xnew
                    */
                    for(int i= 0; i< s; i++)   // loop for xnew
                    {
                        StdMet.stam(stam1, h*b[i], K[i]);   // h*b[i]*K[i]
                        StdMet.arraysum(sigma2, sigma2, stam1);   // sigma2 = sigma2 + h*b[i]*K[i]
                    }

                    /*
                      this loop computes a similar weighted average as above except
                      it is done using bhat instead of b, it is for the higher order ERK method
                      and it is used to compare to the higher order method for error estimation
                    */
                    for(int i= 0; i< s; i++)   // loop for error estimation
                    {
                        StdMet.stam(stam1, h*bhat[i], K[i]);   // h*bhat[i]*K[i]
                        StdMet.arraysum(sigma3, sigma3, stam1);   // sigma3 = sigma3 + h*bhat[i]*K[i]
                    }

                    StdMet.arraysum(xe, xold, sigma3);   // xe = xold + sigma3
                    StdMet.arraysum(xnew, xold, sigma2);   // xnew = xold + sigma2

                    StdMet.zero_out(sigma2);   // set sigma2 array to array of zeros
                    StdMet.zero_out(sigma3);   // set sigma3 array to array of zeros

                    done = true;   // we stop integration upon an event, so we treat
                    tf = told + h;   // tStar as we would tf and output the solution at tStar
                }
            } // if(eventLoc_on)

            if(constStep)   // if routine is constant step, step constantly
            {
                justAccepted = true;   // if routine is constant step, step is always accepted
                         
                norm = StdMet.rmsNorm(xnew);   // take norm of xnew

                if(norm != norm)   // check to see if norm is NaN, if
                {   // so, something has gone wrong, solver is unstable
                    System.out.println("unstable . . . aborting");

                    writer.closeFile();   // close the writer before halting

                    return;   // halt routine
                }

                if(done)
                {
                    System.out.println("done"); 
                    System.out.println("final t = " + (told + h));   // output final t
                    System.out.println("final solution =");   // and solution
                    StdMet.arrayprt(xnew);

                    handleRecord(told + h, xnew);

                    if(!interpolant_on)
                    {
                        writer.writeToFile(told + h, xnew);   // write final soln to file
                    }
                    else
                    {
                        while((times_inc < times_limit) && (told <= times[times_inc]) && ((told + h) >= times[times_inc]))
                        {   // while there are points left, and a point in array falls between point in solution
                                   
                            theta = (times[times_inc] - told) / h;   // generate a theta for this point
 
                            /*
                              this loop takes the weighted average of all of the arrays in the
                              K matrix using the functions of theta of the Butcher tableau
                              -> this loop is the weighted average for an ERK method and it
                              is used to interpolate two solution points
                            */
                            for(int i= 0; i< s; i++)   // loop for interpolant
                            {
                                StdMet.stam(stam1, h*butcher.get_btheta().f(theta)[i], K[i]);   // h*f(theta)[i]*K[i]
                                StdMet.arraysum(sigmaInterp, sigmaInterp, stam1);   // sigmaInterp = sigmaInterp + h*f(theta)[i]*K[i]
                            }

                            StdMet.arraysum(x_interp, xold, sigmaInterp);   // x_interp = xold + sigmaInterp

                            writer.writeToFile(times[times_inc], x_interp);   // output solution thus far (into file)

                            times_inc++;   // go to next time in the times array
                            StdMet.zero_out(sigmaInterp);   // clear out x_interp for the next interpolated solution
                        }
                    }
                }
                else
                {
                    if(stats_on)   // output statistics (if user has chosen such)
                    {
                        System.out.println("stepping: " + told + " -> " + (told + h));

                        if(!stats_intermediate)   // do not output if only on
                        {   // intermediate statistics mode
                            System.out.println("solution = ");
                            StdMet.arrayprt(xnew);
                        }

                        System.out.println();
                    }

                    count++;   // increment step counter

                    handleRecord(told + h, xnew);

                    if(!interpolant_on)   // if the interpolant is not on, put an upper bound
                    {   // on the number of points that go in file

                        if(steps <= nPoints)   // if there are less that nPoints points, write
                        {   // every time
                            writer.writeToFile(told + h, xnew);
                        }
                        else
                        {
                            if(count % (steps/nPoints) == 0)   // output solution (thus far) into file
                            {   // (but only allow ~nPoints of these to go as time is a factor)
                                writer.writeToFile(told + h, xnew);
                            }
                        }
                    }
                    else
                    {
                        while((times_inc < times_limit) && (told <= times[times_inc]) && ((told + h) >= times[times_inc]))
                        {   // while there are points left, and a point in array falls between point in solution

                            theta = (times[times_inc] - told) / h;   // generate a theta for this point
 
                            /*
                              this loop takes the weighted average of all of the rows in the
                              K matrix using the functions of theta of the Butcher tableau
                              -> this loop is the weighted average for an ERK method and it
                              is used to interpolate two solution points
                            */
                            for(int i= 0; i< s; i++)   // loop for interpolant
                            {
                                StdMet.stam(stam1, h*butcher.get_btheta().f(theta)[i], K[i]);   // h*f(theta)[i]*K[i]
                                StdMet.arraysum(sigmaInterp, sigmaInterp, stam1);   // sigmaInterp = sigmaInterp + h*f(theta)[i]*K[i]
                            }

                            StdMet.arraysum(x_interp, xold, sigmaInterp);   // x_interp = xold + sigma
                            writer.writeToFile(times[times_inc], x_interp);   // output solution thus far (into file)

                            times_inc++;   // go to next time in the times array
                            StdMet.zero_out(sigmaInterp);   // clear out x_interp for the next interpolated solution
                        }
                    }

                    StdMet.arraycpy(xold, xnew);  // set xold to xnew, preparing to put the next
                    // value of the next step in the integration into xnew
	
                    told += h;  // update told for the next step of the integration
                         
                    if((told + 2.0*h) > tf)
                        lastStep = true;
                }
            } //(end if(constStep))
            else   // else handle it as an embedded method
            {
                // do the embedded error estimation
		       
                double[] estimation = ErrorEstimator.embedded_estimate(h, xold, xnew, xe, atol, rtol, p, aMax, AMIN, ALPHA);

                epsilon = estimation[0];   // get required information from this estimation
                hNew = estimation[1];

                if(((1.1*hNew) >= (tf - (told + h))) && (epsilon <= 1.0))
                {   // stretch the last step if it is within 10% of tf - (told + h)
                    hNew = tf - (told + h);
                    lastStep = true;
                }

                if(epsilon != epsilon)   // check to see if error is NaN, if
                {   // so, something has gone wrong, solver is unstable
                    System.out.println("unstable . . . aborting");
                    System.out.println("accepted: " + naccept);
                    System.out.println("rejected: " + nreject);

                    writer.closeFile();   // close the writer before halting

                    return;   // halt routine
                }

                // based on the error estimation, make the decision to finish,
                // accept or reject a step

                if(done)   // when done output all of the statistics involved in
                {   // the embedded method and it is done the program                    
                    if(epsilon > 1.0)   // in the very odd case that the stretched step
                    {   // is rejected:
                        done = false;   // set done and last step to false, because it is
                        lastStep = false;   // doing at least two more steps
                        nreject++;   // we reject a step and up the counter to
                        nfailed++;   // keep track for stiffness
                        h = h/2;   // we cut h in half, seeing as either half will be too small

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
                    else
                    {
                        naccept++;   // last step is obviously accepted
                        avgStepSize = (tf - t0)/naccept;
                        System.out.println("done");
                        System.out.println("final t = " + (told + h));   // output final t
                        System.out.println("final solution =");   // and solution
                        StdMet.arrayprt(xnew);
                        System.out.println();

                        if(sdetect_on)
                        {
                            System.out.println("# of checks due to MAXFCN: " + checks1);
                            System.out.println("# of checks due to ratio: " + checks2);
                        }

                        System.out.println("# of rejections = " + nreject);
                        System.out.println("# of accepted steps = " + naccept);
                        System.out.println("average step size = " + avgStepSize);

                        // profile filling

                        this.profile = new double[n + 2];   // profile contians h, t and xnew
                         
                        profile[0] = avgStepSize;   // give it h (avg)
                        profile[1] = (told + h);   // give it t (final)
                         
                        for(int i= 0; i< n; i++)   // fill the rest with x
                            profile[i + 2] = xnew[i];

                        // file writing/interpolation stuff

                        handleRecord(told + h, xnew);

                        if(!interpolant_on)
                        {
                            writer.writeToFile(told + h, xnew);   // output this final solution into the file
                        }
                        else
                        {
                            while((times_inc < times_limit) && (told <= times[times_inc]) && ((told + h) >= times[times_inc]))
                            {   // while there are points left, and a point in array falls b/w point in solution

                                theta = (times[times_inc] - told) / h;   // generate a theta for this point
 
                                /*
                                  this loop takes the weighted average of all of the rows in the
                                  K matrix using the functions of theta of the Butcher tableau
                                  -> this loop is the weighted average for an ERK method and it
                                  is used to interpolate two solution points
                                */
                                for(int i= 0; i< s; i++)   // loop for interpolant
                                {
                                    StdMet.stam(stam1, h*butcher.get_btheta().f(theta)[i], K[i]);   // h*f(theta)[i]*K[i]
                                    StdMet.arraysum(sigmaInterp, sigmaInterp, stam1);   // sigmaInterp = sigmaInterp + h*f(theta)[i]*K[i]
                                }

                                StdMet.arraysum(x_interp, xold, sigmaInterp);   // x_interp = xold + sigma

                                writer.writeToFile(times[times_inc], x_interp);   // output solution array thus far (into file)

                                times_inc++;   // go to next time in the times array
                                StdMet.zero_out(sigmaInterp);   // clear out x_interp for the next interpolated solution
                            }
                        }
                    }
                }
                else // (not done)
                {
                    fevalTotal += s;   // another s function evaluations are done
                    if((fevalTotal > MAXFCN) && sdetect_on)
                    {
                        checks1++;

                        // check for stiffness

                        double hRho = StiffnessDetector.calc_hRho(h, K[K2], K[K1], g2, g1);

                        if(stats_on)   // output some statistics (if user has chosen
                        {   // such)
                            if(!stats_intermediate)   // do not output if only on
                            {   // intermediate statistics mode
                                System.out.println("a check due to MAXFCN was done");
                                System.out.println("h*rho = " + hRho);
                                System.out.println();
                            }
                        }

                        if(hRho > BOUND)
                        {
                            System.out.println("problem is stiff due to MAXFCN at t = " + told);
                            System.out.println("solution at this time:");
                            StdMet.arrayprt(xold);
                            System.out.println("# of checks due to MAXFCN: " + checks1);
                            System.out.println("# of checks due to ratio: " + checks2);
                            System.out.println("accepted: " + naccept);
                            System.out.println("rejected: " + nreject);

                            writer.closeFile();   // close the writer before halting

                            return;   // halt routine
                        }

                        fevalTotal = 0;   // reset counter
                    }
                    if(lastStep || (epsilon <= 1.0))   // if on our last step
                    {   // (where step is usually very small):

                        handleRecord(told, xnew);

                        if(interpolant_on)
                        {
                            while((times_inc < times_limit) && (told <= times[times_inc]) && ((told + h) >= times[times_inc]))
                            {   // while there are points left, and a point in array falls b/w point in solution

                                theta = (times[times_inc] - told) / h;   // generate a theta for this point

                                /*
                                  this loop takes the weighted average of all of the rows in the
                                  K matrix using the functions of theta of the Butcher tableau
                                  -> this loop is the weighted average for an ERK method and it
                                  is used to interpolate two solution points
                                */
                                for(int i= 0; i< s; i++)   // loop for interpolant
                                {
                                    StdMet.stam(stam1, h*butcher.get_btheta().f(theta)[i], K[i]);   // h*f(theta)[i]*K[i]
                                    StdMet.arraysum(sigmaInterp, sigmaInterp, stam1);   // sigmaInterp = sigmaInterp + h*f(theta)[i]*K[i]
                                }

                                StdMet.arraysum(x_interp, xold, sigmaInterp);   // x_interp = xold + sigmaInterp
					     
                                writer.writeToFile(times[times_inc], x_interp);   // output solution array thus far (into file)

                                times_inc++;   // go to next time in the times array
                                StdMet.zero_out(sigmaInterp);   // clear out sigmaInterp for the next interpolated solution
                            }
                        }

                        StdMet.arraycpy(xold, xnew);   // accept step

                        if(nsuccess == 0)   // if check was just done, update
                            tFirst = told;   // the time

                        told += h;   // increment related counters
                        h = hNew;
                        aMax = 5.0;   // restore amax to 5 after step acceptance
                        naccept++;
                        nsuccess++;   // keep track for stiffness detection
                        justAccepted = true;   // toggle for FSAL functionality

                        if(!interpolant_on)
                        {
                            writer.writeToFile(told, xnew);   // output new solution array thus far (into file)
                        }

                        if(stats_on)   // output statistics (if user has chosen
                        {   // such)
                            if(!stats_intermediate)   // do not output if only on
                            {   // intermediate statistics mode
                                System.out.println("accepted");
                                System.out.println("new h =" + h);
                            }

                            System.out.println("t =" + told);

                            if(!stats_intermediate)   // do not output if only on
                            {   // intermediate statistics mode
                                System.out.println("solution =");
                                StdMet.arrayprt(xold);
                            }

                            System.out.println();
                        }
                    }
                    else   // else reject the step and increment related counters
                    {
                        h = hNew;
                        aMax = 1.0;   // set amax to 1 after a step rejection
                        nreject++;
                        nfailed++;   // keep track for stiffness

                        if((nfailed >= NFAILED) && sdetect_on)
                        {
                            if(nsuccess <= NSUCCESS)
                            {
                                avgStepSize = (told - tFirst)/nsuccess;

                                if(avgStepSize == 0)   // in the event that there were only rejected steps from
                                    avgStepSize = h;   // the start, make it so a check can be done

                                if(((h <= AMAX*avgStepSize) && (h >= avgStepSize/AMAX)) && (fevalTotal > MAXFCN*(told - t0)/(tf - t0)))
                                {
                                    checks2++;

                                    // check for stiffness

                                    double hRho = StiffnessDetector.calc_hRho(h, K[K2], K[K1], g2, g1);

                                    if(stats_on)   // output some statistics (if user has chosen
                                    {   // such)
                                        if(!stats_intermediate)   // do not output if only on
                                        {   // intermediate statistics mode
                                            System.out.println("a check due to ratio was done");
                                            System.out.println("h*rho = " + hRho);
                                            System.out.println();
                                        }
                                    }

                                    if(hRho > BOUND)
                                    {
                                        System.out.println("problem is stiff due to ratio at t = " + told);
                                        System.out.println("solution at this time:");
                                        StdMet.arrayprt(xold);
                                        System.out.println("# of checks due to MAXFCN: " + checks1);
                                        System.out.println("# of checks due to ratio: " + checks2);
                                        System.out.println("accepted: " + naccept);
                                        System.out.println("rejected: " + nreject);                                             System.exit(0);

                                        writer.closeFile();   // close the writer before halting

                                        return;   // halt
                                    }
                                }
                            }

                            nsuccess = 0;     // reset the counters
                            nfailed = 0;
                        }

                        if(stats_on)   // output statitics (if user has chosen so), note that
                        {   // time and solution do not change so do not output such
                            if(!stats_intermediate)   // do not output if only on
                            {   // intermediate statistics mode
                                System.out.println("rejected");
                                System.out.println("new h =" + h);
                                System.out.println();
                            }
                        }
                    } // end if/else(lastStep)
                } // end if/else(done)
            } // end if/else(constStep)


        } // end while(! done)
    
        /* end integration loop */

        writer.closeFile();   // now that we are done, close the writer
    }

     /*
        gets profile of statistics from an integration
     */
     public double[] getProfile()
     {
          return(profile);   // return profile array
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

     // instance variables

     // variables dealing with general parameters (section 1 of constructor)

     private ODE f;   // the function of the differential equation
    private ODERecorder recorder;

     private double t0;   // the starting time
     private double tf;   // the stopping time
     private double[] x0;   // the initial value array
     private Btableau butcher;   // the Butcher tableau for the scheme
     private double p;   // order of the scheme
     private int s;   // the number of stages of this Runge-Kutta scheme
     private boolean constStep;   // whether the scheme is constant step or not
     private long steps;   // the actual number of steps of integration needed (if scheme is constant step)
     private long count;   // counter to count the steps of the integration (if scheme is constant step)


     private double[][] a;   // the matrix a of the given Butcher tableau
     private double[] b;   // the array b of the given Butcher tableau
     private double[] bhat;   // the array bhat of the given Butcher tableau
     private double[] c;   // the array c of the given Butcher tableau

     private boolean FSALenabled;   // whether first same as last functionality of the
        // scheme (if this scheme has the property to begin with) is enabled

     private double[] atol;   // abolute tolerances for each solution array entry
     private double[] rtol;   // relative tolerances for each solution array entry

     private double h;   // the step size of the integration
     private int n;   // dimension of the ODE

     // variables that are used in the integration loop

     private double told;   // stores the current t value
     private double[] xold;   // stores the current x value (xold)
     private double[] xnew;   // stores the next x value (xnew)
     private double[] xe;   // the error estimation for embedded method
     private double[][] K;   // matrix of K values (s rows of size n)

     // error control variables

     private double epsilon;   // determines whether we accept or reject next
        // step

     private double hNew;   // the step size to take for the next step
     private double aMax = 5.0;   // maximum growth limit for h

     private int nreject;   // number of rejected steps counter
     private int naccept;   // number of accepted steps counter
     private double avgStepSize;   // the average stepsise throughout integration

     // iteration affected variables

     private boolean done;   // termination switch
     private boolean firstStep;   // switch verifies if loop of routine is on first step
     private boolean lastStep;   // switch verifies if loop of routine is on last step
     private boolean justAccepted;   // switch verifies whether previous step was an
        // accepted step or not (for the purpose of FSAL functionality)

     // variables for interpolation

     private double[] times;   // array of times user can use to interpolate solution to
     private int timesLength;   // length of the times array (length = 0 if times = null)
     private boolean interpolant_on;   // whether to do interpolation or not

     // variables for stiffness detection

     private boolean sdetect_on;   // whether to detect stiffness or not

     private int fevalTotal;   // number of function evaluations thus far
     private double tFirst;   // the first time after a check is done
     private int nsuccess;   // number of successful steps thus far (from start or last check)
     private int nfailed;   // number of failed steps thus far (from start or last check)
     private int checks1;   // number of stiffness checks due to function evaluations > MAXFCN
     private int checks2;   // number of stiffness checks due to 10 fail b/f 50 succeed

     // variables for event location

     private boolean eventLoc_on;   // whether to locate events or not
     private double[] profile;   // a capsule of a few statistics from this routine to use in others

     // variables for miscellaneous special features

     private String fileName;   // name of file solution is written to (each step)
     private boolean append;   // whether to append solution to a non-empty file, or to overwrite it
     private boolean stats_on;   // whether to report status at each step
     private boolean stats_intermediate;   // whether to report just a few statistics

     // finals (constants)

     private final double P = 5.0;   // the higher order method of a Dormand-Prince scheme
     private final double AMIN = 1.0/5.0;   // minimum growth limit for h
     private final double AMAX = 5.0;   // maximum growth limit for h
     private final double ALPHA = 0.9;   // safety factor

     private final int MAXFCN = 14000;   // default amount of function evaluations before we say problem is stiff
     private final int NSUCCESS = 50;   // after 50 succesful steps check ratio
     private final int NFAILED = 10;   // if 10 failed after 50 successful, check for stiffness
     private final int K1 = 5;   // last 2 row of K of Dormand-Prince scheme (as Dormand-Prince is the only scheme that
     private final int K2 = 6;   // offers stiffness detection)
     private final double BOUND = 3.25;   // stability region boundary for Dormand-Prince scheme

     private int nPoints;   // number of points to write to file
}
