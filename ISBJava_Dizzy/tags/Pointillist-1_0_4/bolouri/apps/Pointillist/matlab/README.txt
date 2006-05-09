Supplementary codes for paper: 
"A data integration methodology for systems biology" part I (theory).
by D. Hwang et al., Institute for Systems Biology (2005)

Enclosed are the MATLAB codes for Pointillist version 2.0.  These MATLAB
codes were developed on MATLAB R14 and require the MATLAB Statistics Toolbox.

The function of each MATLAB file is as follows:

  basisexp.m: select a dataset to be used for p-value normalization

  pscalef.m: normalization of p-values (correct non-ideality of p-value distributions or correlation issues if any).

	     When applying "pv2.m" to the p-values obtained from real high-throughput datasets, 
	     we recommend to run the two functions above to normalize p-values before running "pv2.m"

  pv2.m: a wrapper to call esa.m and genwfunf.m depending upon the methods mentioned in the main text of the paper

  esa.m: enhanced simulated annealing that searches for the optimal weight and parameters.

  genwfunf.m: select elements given a weight vector and alpha

  mcmc.m: Monte Carlo simulation to generate random numbers for each integration method (for example, Fisher's method)

  nprampv2.m: non-parametric method for integrating datasets

  paretospace.m: perform a multi-objective optimization on the Pareto space

  supsmu.m: non-parametric smoothing algorithm.

  nwpv2.m: non-weighted integration methods.

Copyright (c) Institute for Systems Biology 2005

For questions and suggestions, please email "pointillist at systemsbiology.org"

