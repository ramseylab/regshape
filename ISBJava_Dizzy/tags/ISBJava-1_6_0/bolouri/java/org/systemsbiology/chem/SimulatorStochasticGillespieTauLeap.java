package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

import java.util.*;
import edu.cornell.lassp.houle.RngPack.*;
import cern.jet.random.*;

/**
 * Simulates the dynamics of a set of coupled chemical reactions
 * described by {@link Reaction} objects using the Gillespie stochastic
 * algorithm, "tau-leap method".
 *
 * @author Stephen Ramsey
 */
public final class SimulatorStochasticGillespieTauLeap extends SimulatorStochasticBase implements IAliasableClass, ISimulator
{
    private static final long NUM_FIRINGS_GILLESPIE = 1;
    private static final double DEFAULT_MAX_ALLOWED_RELATIVE_ERROR = 0.005;
    private static final long DEFAULT_MIN_RATIO_OF_LEAP_TIME_TO_REACTION_TIME_SCALE = 10;
    private static final long MULTIPLIER_FOR_MIN_NUM_GILLESPIE_STEPS = 4;
    private static final int MAX_LEAP_ATTEMPTS = 10;

    private Expression []mMu;
    private Expression []mSigma;
    private Object []mF;
    private double []mEstimatedSpeciesChange;
//    private double []mMaxNumReactionFirings;

    private double mAllowedError;
    private Poisson mPoisson;
    private long mNumNonLeapIterationsSinceLastLeapCheck;
    private boolean mLastIterationWasLeap;
    private long mMinRatioOfLeapTimeToReactionTimeScale;

    public static final String CLASS_ALIAS = "gillespie-tauleap"; 

    protected void prepareForStochasticSimulation(SymbolEvaluatorChem pSymbolEvaluator,
                                                  double pStartTime,
                                                  RandomElement pRandomNumberGenerator,
                                                  Reaction []pReactions,
                                                  double []pReactionProbabilities,
                                                  SimulatorParameters pSimulatorParameters) throws IllegalArgumentException
    {
        Double maxAllowedError = pSimulatorParameters.getMaxAllowedRelativeError();
        if(null == maxAllowedError)
        {
            throw new IllegalArgumentException("required simulator parameter maxAllowedRelativeError was not specified");
        }
        mAllowedError = maxAllowedError.doubleValue();

        mPoisson = new Poisson(1.0, pRandomNumberGenerator);

        Long minNumSteps = pSimulatorParameters.getMinNumSteps();
        if(null == minNumSteps)
        {
            throw new IllegalArgumentException("required simulator parameter minNumSteps was not specified");
        }

        mMinRatioOfLeapTimeToReactionTimeScale = minNumSteps.longValue();
        mNumNonLeapIterationsSinceLastLeapCheck = 0;
        mLastIterationWasLeap = true;
    }

    protected double iterate(SymbolEvaluatorChem pSymbolEvaluator,
                             double pEndTime,
                             Reaction []pReactions,
                             double []pReactionProbabilities,
                             RandomElement pRandomNumberGenerator,
                             double []pDynamicSymbolValues,
                             MutableInteger pLastReactionIndex,
                             DelayedReactionSolver []pDelayedReactionSolvers) throws DataNotFoundException, IllegalStateException, SimulationAccuracyException
    {
        double time = pSymbolEvaluator.getTime();
//        System.out.println("time at start of iteration: " + time);

        int lastReactionIndex = pLastReactionIndex.getValue();
        if(NULL_REACTION != lastReactionIndex)
        {
            Reaction lastReaction = pReactions[lastReactionIndex];

            updateSymbolValuesForReaction(pSymbolEvaluator,
                                          lastReaction,
                                          pDynamicSymbolValues,
                                          pDelayedReactionSolvers,
                                          NUM_FIRINGS_GILLESPIE);
            // we have changed the species populations; must clear the expression value caches
            clearExpressionValueCaches();
        }

        int numReactions = pReactions.length;

        computeReactionProbabilities(pSymbolEvaluator,
                                     pReactionProbabilities,
                                     pReactions);

        double aggregateReactionProbability = MathFunctions.vectorSumElements(pReactionProbabilities);

        double leapTime = 0.0;
        boolean doLeap = false;
        if(mLastIterationWasLeap || 
           mNumNonLeapIterationsSinceLastLeapCheck >= MULTIPLIER_FOR_MIN_NUM_GILLESPIE_STEPS * mMinRatioOfLeapTimeToReactionTimeScale)
        {
//              getMaxNumberFiringsForReactions(pSymbolEvaluator,
//                                              pReactions,
//                                              pDynamicSymbolValues,
//                                              mMaxNumReactionFirings);

             leapTime = getLargestJumpConsistentWithAllowedError(pSymbolEvaluator,
                                                                           mAllowedError,
                                                                           pReactionProbabilities,
                                                                           aggregateReactionProbability,
                                                                           mF,
                                                                           pReactions,
                                                                           pDynamicSymbolValues,
                                                                           mEstimatedSpeciesChange);

            if(leapTime >= (mMinRatioOfLeapTimeToReactionTimeScale / aggregateReactionProbability))
            {
                doLeap = true;
            }

            mNumNonLeapIterationsSinceLastLeapCheck = 0;
        }

//        System.out.println("max allowed jump time: " + leapTime + "; avg reaction time: " + Double.toString(1.0/aggregateReactionProbability));

//        System.out.println("time: " + time + "; tau: " + leapTime);

        mLastIterationWasLeap = doLeap;

        if(! doLeap)
        {
            // it is not worth it to leap
            double deltaTimeToNextReaction = Double.POSITIVE_INFINITY;
            ++mNumNonLeapIterationsSinceLastLeapCheck;

            if(aggregateReactionProbability > 0.0)
            {
                deltaTimeToNextReaction = chooseDeltaTimeToNextReaction(pRandomNumberGenerator, 
                                                                        aggregateReactionProbability);
            }            

            int reactionIndex = -1;

            if(pDelayedReactionSolvers.length == 0)
            {
                // do nothing
            }
            else
            {
                int nextDelayedReactionIndex = getNextDelayedReactionIndex(pDelayedReactionSolvers);
                if(nextDelayedReactionIndex >= 0)
                {
                    DelayedReactionSolver solver = pDelayedReactionSolvers[nextDelayedReactionIndex];
                    double nextDelayedReactionTime = solver.peekNextReactionTime();
//                System.out.println("next delayed reaction will occur at: " + nextDelayedReactionTime);
                    if(nextDelayedReactionTime < time + deltaTimeToNextReaction)
                    {
                        // execute delayed reaction
                        deltaTimeToNextReaction = nextDelayedReactionTime - time;
                        reactionIndex = solver.getReactionIndex();
//                    System.out.println("delayed reaction selected: " + pReactions[reactionIndex]);
                        solver.pollNextReactionTime();
                    }
                }
            }

            if(-1 == reactionIndex && aggregateReactionProbability > 0.0)
            {
                reactionIndex = chooseIndexOfNextReaction(pRandomNumberGenerator,
                                                          aggregateReactionProbability,
                                                          pReactions,
                                                          pReactionProbabilities);
            }


            if(-1 != reactionIndex)
            {
//                System.out.println("normal gillespie step, delta time: " + deltaTimeToNextReaction + "; reaction: " + pReactions[reactionIndex].toString());

                // choose type of next reaction
                Reaction reaction = pReactions[reactionIndex];
                
                pLastReactionIndex.setValue(reactionIndex);
                
                time += deltaTimeToNextReaction;
                
//            System.out.println("time: " + time + "; reaction occurred: " + reaction);
            }
            else
            {
                time = pEndTime;
            }
            pSymbolEvaluator.setTime(time);
        }
        else
        {
//            System.out.println("LEAP TIME: " + leapTime);
            // leaping will help us
            double allowedError = mAllowedError;

            if(time + leapTime > pEndTime)
            {
                leapTime = pEndTime - time;
                // don't leap past the end time
                time = pEndTime;
            }
            else
            {
                time += leapTime;
            }

            pLastReactionIndex.setValue(NULL_REACTION);

            // update symbol values right away, since we are no longer assuming that
            // no reactions occur during the time jump

//            System.out.println("tau-leap");

//             Reaction reaction = null;
//             double reactionProbability = 0.0;
//             double maxNumFirings = 0.0;

//             for(int j = numReactions; --j >= 0; )
//             {
//                 reaction = pReactions[j];
//                 reactionProbability = pReactionProbabilities[j];

//                 if(reactionProbability > 0.0)
//                 {
//                     long numFirings = 0;
//                     double lambda = reactionProbability * leapTime;

//                     maxNumFirings = mMaxNumReactionFirings[j];

//                     if(lambda <= 0.8 * maxNumFirings)
//                     {
//                         boolean gotSuccessfulNumFirings = false;

//                         do
//                         {
//                             try
//                             {
//                                 numFirings = (long) mPoisson.nextInt(lambda);
//                                 gotSuccessfulNumFirings = true;
//                             }
//                             catch(ArrayIndexOutOfBoundsException e)
//                             {
//                                 System.err.println("internal bug in cern.jet.random.Poisson tripped; this is being handled");
//                             }
//                         }
//                         while(! gotSuccessfulNumFirings);
//                     }
//                     else
//                     {
//                         numFirings = (long) lambda;
//                     }
                        
//                     updateSymbolValuesForReaction(pSymbolEvaluator,
//                                                   pReactions[j],
//                                                   pDynamicSymbolValues,
//                                                   pDelayedReactionSolvers,
//                                                   numFirings);
//                 }
//             }

            boolean successfulLeap = false;
            for(int failedLeaps = 0; failedLeaps < MAX_LEAP_ATTEMPTS; ++failedLeaps)
            {
                successfulLeap = attemptLeap(pSymbolEvaluator,
                                             leapTime,
                                             pReactionProbabilities,
                                             pReactions,
                                             pDynamicSymbolValues,
                                             mEstimatedSpeciesChange,
                                             mPoisson,
                                             mAllowedError);
                if(successfulLeap)
                {
                    break;
                }
            }

            if(! successfulLeap)
            {
                throw new SimulationAccuracyException("simulation became unstable; please re-run with a smaller value for the error control parameter");
            }

            MathFunctions.vectorAdd(mEstimatedSpeciesChange, pDynamicSymbolValues, pDynamicSymbolValues);

            if(pDelayedReactionSolvers.length > 0)
            {
                double nextDelayedReactionTime = 0.0;

                DelayedReactionSolver solver = null;
                int nextDelayedReactionIndex = NULL_REACTION;

                while(nextDelayedReactionTime <= time)
                {
                    nextDelayedReactionIndex = getNextDelayedReactionIndex(pDelayedReactionSolvers);
                    if(nextDelayedReactionIndex > NULL_REACTION)
                    {
                        solver = pDelayedReactionSolvers[nextDelayedReactionIndex];
                        nextDelayedReactionTime = solver.peekNextReactionTime();
                        if(nextDelayedReactionTime <= time)
                        {
                            updateSymbolValuesForReaction(pSymbolEvaluator,
                                                          pReactions[solver.getReactionIndex()],
                                                          pDynamicSymbolValues,
                                                          pDelayedReactionSolvers,
                                                          NUM_FIRINGS_GILLESPIE);
                            solver.pollNextReactionTime();
                        }
                    }      
                    else
                    {
                        break;
                    }
                }
            }            

            pSymbolEvaluator.setTime(time);
            clearExpressionValueCaches();
        }

//        System.out.println("time at end of iteration: " + time + "\n");

        return(time);
    }

    private static boolean attemptLeap(SymbolEvaluatorChem pSymbolEvaluator,
                                       double pLeapTime,
                                       double []pReactionProbabilities,
                                       Reaction []pReactions,
                                       double []pSpeciesValues,
                                       double []pEstimatedSpeciesChange,
                                       Poisson pPoissonEventGenerator,
                                       double pAllowedError) throws DataNotFoundException
    {
        MathFunctions.vectorZeroElements(pEstimatedSpeciesChange);

        int numReactions = pReactions.length;

        Reaction reaction = null;
        double lambda = 0.0;
        long numFirings = 0;
        boolean gotSuccessfulNumFirings = false;
        for(int j = numReactions; --j >= 0; )
        {
            reaction = pReactions[j];
            lambda = pLeapTime * pReactionProbabilities[j];
            if(lambda > 0.0)
            {
                if(1.0/Math.sqrt(lambda) > pAllowedError)
                {
                    gotSuccessfulNumFirings = false;
                    do
                    {
                        try
                        {
                            numFirings = (long) pPoissonEventGenerator.nextInt(lambda);
                            gotSuccessfulNumFirings = true;
                        }
                        catch(ArrayIndexOutOfBoundsException e)
                        {
                            System.err.println("internal bug in cern.jet.random.Poisson tripped; this is being handled");
                        }
                    }
                    while(! gotSuccessfulNumFirings);
                }
                else
                {
                    numFirings = Math.round(lambda);
                }
                updateSymbolValuesForReaction(pSymbolEvaluator,
                                              reaction,
                                              pEstimatedSpeciesChange,
                                              null,
                                              numFirings);
            }
        }

        boolean succeeded = true;
        int numSpecies = pSpeciesValues.length;
        for(int i = numSpecies; --i >= 0; )
        {
            if(pEstimatedSpeciesChange[i] + pSpeciesValues[i] < 0.0)
            {
                succeeded = false;
            }
        }

        return(succeeded);
    }

    private double computeMuTime(int j, double pAllowedError, double pAggregateReactionProbability) throws DataNotFoundException
    {
        Reaction []reactions = mReactions;
        Reaction reaction = reactions[j];
        Object []F = mF;
        Expression []Fj = (Expression []) F[j];
        int numReactions = reactions.length;
        double muj = 0.0;
        for(int jp = 0; jp < numReactions; ++jp)
        {
            double  Fjjp = reaction.evaluateExpressionWithReactionRateLocalSymbolTranslation(Fj[jp], mSymbolEvaluator);
            muj += mReactionProbabilities[jp] * Fjjp;
        }
        return(pAllowedError * pAggregateReactionProbability / Math.abs(muj));
    }

    private static void getMaxNumberFiringsForReactions(SymbolEvaluatorChem pSymbolEvaluator,
                                                        Reaction []pReactions,
                                                        double []pSymbolValues,
                                                        double []pMaxNumReactionFiringsSpeciesLimited) throws DataNotFoundException
    {
        int numReactions = pReactions.length;
        for(int j = numReactions; --j >= 0; )
        {
            pMaxNumReactionFiringsSpeciesLimited[j] = getMaxNumberFiringsForReaction(pSymbolEvaluator,
                                                                                     pReactions[j],
                                                                                     pSymbolValues);
        }
    }


    private static double getMaxNumberFiringsForReaction(SymbolEvaluatorChem pSymbolEvaluator,
                                                         Reaction pReaction,
                                                         double []pSymbolValues) throws DataNotFoundException
    {
        double retVal = Double.MAX_VALUE;
        
        Species []reactantsSpecies = pReaction.getReactantsSpeciesArray();
        boolean []reactantsDynamic = pReaction.getReactantsDynamicArray();

        int []reactantsStoichiometry = pReaction.getReactantsStoichiometryArray();
        int numReactants = reactantsSpecies.length;

        double reactantLimit = 0.0;

        Species reactant = null;
        int reactantIndex = -1;

        for(int ctr = numReactants; --ctr >= 0; )
        {
            if(reactantsDynamic[ctr])
            {
                reactant = reactantsSpecies[ctr];
                reactantIndex = reactant.getSymbol().getArrayIndex();
                reactantLimit = Math.floor(pSymbolValues[reactantIndex] / ((double) reactantsStoichiometry[ctr]));
                if(reactantLimit < retVal)
                {
                    retVal = reactantLimit;
                }
            }
        }

        return(retVal);
    }


    private static double getLargestJumpConsistentWithAllowedError(SymbolEvaluatorChem pSymbolEvaluator,
                                                                   double pAllowedError,
                                                                   double []pReactionProbabilities,
                                                                   double pSumReactionProbabilities,
                                                                   Object []pF,
                                                                   Reaction []pReactions,
                                                                   double []pDynamicSpeciesValues,
                                                                   double []pEstimatedSpeciesChange) throws DataNotFoundException
    {
        int numReactions = pReactionProbabilities.length;
        double jumpTime = Double.MAX_VALUE;
        double muVal = 0.0;
        double sigmaVal = 0.0;
        double muFac = pAllowedError * pSumReactionProbabilities;
        double sigmaFac = muFac*muFac;
        double muj = 0.0;
        double fjjp = 0.0;
        double sigmaj = 0.0;
        Reaction reaction = null;
        double fjjpTimesRate = 0.0;

        int numSpecies = pEstimatedSpeciesChange.length;
        double estimatedSpeciesChange = 0.0;
        double estimatedNumFirings = 0.0;

        for(int j = numReactions; --j >= 0; )
        {
            muj = 0.0;
            sigmaj = 0.0;
            Expression []Fj = (Expression []) pF[j];
            reaction = pReactions[j];
            for(int jp = numReactions; --jp >= 0; )
            {
                fjjp = reaction.evaluateExpressionWithReactionRateLocalSymbolTranslation(Fj[jp], pSymbolEvaluator);
                fjjpTimesRate = fjjp * pReactionProbabilities[jp];
                muj += fjjpTimesRate;
                sigmaj += fjjp * fjjpTimesRate;
            }

            muVal = muFac / Math.abs(muj);
            if(muVal < jumpTime)
            {
                jumpTime = muVal;
            }

            sigmaVal = sigmaFac / sigmaj;
            if(sigmaVal < jumpTime)
            {
                jumpTime = sigmaVal;
            }

        }

        double rate = 0.0;
        double numFirings = 0.0;
        DelayedReactionSolver []delayedSolvers = null;

        MathFunctions.vectorZeroElements(pEstimatedSpeciesChange);

        for(int j = numReactions; --j >= 0; )
        {
            rate = pReactionProbabilities[j];
            numFirings = rate * jumpTime;

            if(numFirings > 0.0)
            {
                updateSymbolValuesForReaction(pSymbolEvaluator,
                                              pReactions[j],
                                              pEstimatedSpeciesChange,
                                              delayedSolvers,
                                              (long) numFirings);
            }
        }

        numSpecies = pEstimatedSpeciesChange.length;
        double newSpeciesValue = 0.0;
        double frac = 0.0;
        double minFrac = 1.0;
        double oldSpeciesValue = 0.0;
        for(int i = numSpecies; --i >= 0; )
        {
            oldSpeciesValue = pDynamicSpeciesValues[i];
            if(oldSpeciesValue > 0.0)
            {
                newSpeciesValue = oldSpeciesValue + pEstimatedSpeciesChange[i];
                if(newSpeciesValue < 0.0)
                {
                    frac = 0.5 * oldSpeciesValue/(-1.0 * pEstimatedSpeciesChange[i]);
                    if(frac < minFrac)
                    {
                        minFrac = frac;
                    }
                }
            }
        }

        if(minFrac < 1.0)
        {
//            System.out.println("modifying jump time from : " + jumpTime + " by fraction: " + minFrac);
            jumpTime *= minFrac;
        }

        return(jumpTime);
    }


    private void initializeF(SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        int numReactions = mReactions.length;
        int numSpecies = mDynamicSymbols.length;

        Expression []a = Reaction.getReactionRateExpressions(mReactions);
        Object []v = mDynamicSymbolAdjustmentVectors;

        for(int j = 0; j < numReactions; ++j)
        {
            Reaction reaction = mReactions[j];
//            System.out.println("a[" + j + "] = " + a[j].toString());
            double []vj = (double []) v[j];
            for(int i = 0; i < numSpecies; ++i)
            {
                double vji = vj[i];
//                System.out.println("v[" + i + "][" + j + "] = " + vji);
            }
        }

        for(int i = 0; i < numSpecies; ++i)
        {
            Species species = mDynamicSymbols[i];
//            System.out.println("x[" + i + "] is: " + species.getName());
        }

        // generate the Jji matrix, which is the partial derivative of the jth propensity function
        // with respect to the ith species
        Object []J = new Object[numReactions];
        for(int j = 0; j < numReactions; ++j)
        {
            Reaction reaction = mReactions[j];
            Expression []Jj = new Expression[numSpecies];
            Expression aj = a[j];
            for(int i = 0; i < numSpecies; ++i)
            {
                Species species = mDynamicSymbols[i];
                Expression Jji = reaction.computeRatePartialDerivativeExpression(aj, species, pSymbolEvaluator);
                Jj[i] = Jji;
                if(! Jji.isSimpleNumber())
                {
//                    System.out.println("d(a[" + j + "])/d(x[" + i + "]) = " + Jji.toString());
                }
            }
            J[j] = Jj;
        }

        Object []f = new Object[numReactions];

        for(int j = 0; j < numReactions; ++j)
        {
            Expression []fj = new Expression[numReactions];
            Expression []Jj = (Expression []) J[j];
            for(int jp = 0; jp < numReactions; ++jp)
            {
                double []vjp = (double []) v[jp];
                Expression fjjp = new Expression(0.0);
                for(int i = 0; i < numSpecies; ++i)
                {
                    double vijpVal = vjp[i];
                    Expression Jji = Jj[i];
                    Expression vijp = new Expression(vijpVal);
                    Expression mult = Expression.multiply(Jji, vijp);
                    fjjp = Expression.add(fjjp, mult);
                }
                fj[jp] = fjjp;
//                System.out.println("f[" + j + "][" + jp + "] = " + fjjp.toString());
            }
            f[j] = fj;
        }

        mF = f;
    }



    public void initialize(Model pModel) throws DataNotFoundException, InvalidInputException
    {
        initializeSimulator(pModel);
        initializeSimulatorStochastic(pModel);
        initializeDynamicSymbolAdjustmentVectors(mDynamicSymbols);
        initializeF(mSymbolEvaluator);
        mEstimatedSpeciesChange = new double[mDynamicSymbolValues.length];
//        mMaxNumReactionFirings = new double[mReactions.length];
        mMinNumMillisecondsForUpdate = 4000;
    }

    public String getAlias()
    {
        return(CLASS_ALIAS);
    }

    protected void modifyDefaultSimulatorParameters(SimulatorParameters pSimulatorParameters)
    {
        SimulatorParameters sp = pSimulatorParameters;
        sp.setMaxAllowedRelativeError(DEFAULT_MAX_ALLOWED_RELATIVE_ERROR);
        sp.setMinNumSteps(DEFAULT_MIN_RATIO_OF_LEAP_TIME_TO_REACTION_TIME_SCALE);
    }


}
