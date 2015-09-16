package org.systemsbiology.chem;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import java.util.*;

/**
 * Implementation of Gillespie's "Tau-Leap" simulator that
 * is optimized for complex models (models with complex rate
 * law expressions that will have very complicated partial
 * derivatives).
 *
 * @author Stephen Ramsey
 */
public class SimulatorStochasticTauLeapComplex extends SimulatorStochasticTauLeapBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "tauleap-complex";

    private Object []mF;
    private Object []mFinv;

    private int []mJexpCols;
    private int []mJexpRows;
    private Expression []mJexp;
    
    private double []mAdjVecVals;
    private int []mAdjVecRows;
    private int []mAdjVecCols;

    protected double computeLeapTime(double pSumReactionProbabilities) throws DataNotFoundException
    {
        int numReactions = mReactionProbabilities.length;
        int numSpecies = mDynamicSymbols.length;

        int numAdjVecElements = mAdjVecVals.length;
        int numJexpElements = mJexp.length;
        
        int j;
        int jp;
        int k;
        int i;
        int kp;

        Object []F = mF;
        Object []Finv = mFinv;

        // clear the F matrix
        for(j = numReactions; --j >= 0; )
        {
            System.arraycopy(Finv[j], 0, F[j], 0, numReactions);
        }

        SymbolEvaluatorChem symbolEvaluator = mSymbolEvaluator;

        Expression derivExp = null;
        double derivVal = 0.0;
        double vijp = 0.0;

        int []jexpRows = mJexpRows;
        int []jexpCols = mJexpCols;
        Expression []jexp = mJexp;

        int []adjVecCols = mAdjVecCols;
        int []adjVecRows = mAdjVecRows;
        double []adjVecVals = mAdjVecVals;

        Reaction reaction = null;
        double []Fj;
        Reaction []reactions = mReactions;
        boolean []reactionHasLocalSymbolsFlags = mReactionHasLocalSymbolsFlags;

        // Compute the values of all of the partial derivatives that are non-invariant
        // (have expressions instead of static floating-point values); this method of calculation is
        // (at worst) of order 8*M^2, where M is the number of reactions.  The normal matrix multiplication
        // way of doing the calculation would be order N*M^2 (where N is the number of species).
        for(k = numJexpElements; --k >= 0; )
        {
            j = jexpRows[k];
            i = jexpCols[k];
            derivExp = jexp[k];
            if(! reactionHasLocalSymbolsFlags[j])
            {
                derivVal = derivExp.computeValue(symbolEvaluator);
            }
            else
            {
                symbolEvaluator.setLocalSymbolsMap(mReactionsLocalParamSymbolsMaps[j]);
                derivVal = derivExp.computeValue(symbolEvaluator);
                symbolEvaluator.setLocalSymbolsMap(null);
            }

            Fj = (double []) F[j];
            
            for(kp = numAdjVecElements; --kp >= 0; )
            {
                if(i == adjVecRows[kp])
                {
                    Fj[adjVecCols[kp]] += adjVecVals[kp]*derivVal;
                }
            }
        }


        double muj = 0.0;
        double sigmaj = 0.0;
        double ftimesrate = 0.0;

        double []reactionProbabilities = mReactionProbabilities;

        double jumpTime = Double.MAX_VALUE;
        double muVal = 0.0;
        double sigmaVal = 0.0;
        double muFac = mAllowedError * pSumReactionProbabilities;
        double sigmaFac = muFac*muFac;

        for(j = numReactions; --j >= 0; )
        {
            muj = 0.0;
            sigmaj = 0.0;
            Fj = (double []) F[j];
            for(jp = numReactions; --jp >= 0; )
            {
                ftimesrate = Fj[jp]*reactionProbabilities[jp];
                muj += ftimesrate;
                sigmaj += Fj[jp]*ftimesrate;
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

        return(jumpTime);
    }


    protected void initializeTauLeap(SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        int numReactions = mReactions.length;
        int numSpecies = mDynamicSymbols.length;

        Object []v = mDynamicSymbolAdjustmentVectors;

        ArrayList adjVecValues = new ArrayList();
        ArrayList adjVecRows = new ArrayList();
        ArrayList adjVecCols = new ArrayList();
        
        for(int j = 0; j < numReactions; ++j)
        {
            double []vj = (double []) v[j];
            for(int i = 0; i < numSpecies; ++i)
            {
                double vji = vj[i];
                if(vji != 0.0)
                {
                    adjVecValues.add(new Double(vji));
                    adjVecRows.add(new Integer(i));
                    adjVecCols.add(new Integer(j));
                }
            }
        }

        int numAdjVecElements = adjVecValues.size();
        mAdjVecVals = new double[numAdjVecElements];
        mAdjVecRows = new int[numAdjVecElements];
        mAdjVecCols = new int[numAdjVecElements];
        for(int k = 0; k < numAdjVecElements; ++k)
        {
            mAdjVecVals[k] = ((Double) adjVecValues.get(k)).doubleValue();
            mAdjVecRows[k] = ((Integer) adjVecRows.get(k)).intValue();
            mAdjVecCols[k] = ((Integer) adjVecCols.get(k)).intValue();
        }

        ArrayList partialDerivExpressions = new ArrayList();
        ArrayList partialDerivRows = new ArrayList();
        ArrayList partialDerivCols = new ArrayList();
        Expression []a = Simulator.getReactionRateExpressions(mReactions);
        Species species = null;
        Reaction reaction = null;
        Expression reactionRate = null;
        Expression deriv = null;
        double derivValue = 0.0;

        mFinv = new Object[numReactions];
        for(int j = 0; j < numReactions; ++j)
        {
            mFinv[j] = new double[numReactions];
            MathFunctions.vectorZeroElements((double []) mFinv[j]);
        }

        double []v_jp = null;

        for(int j = 0; j < numReactions; ++j)
        {
            reaction = mReactions[j];
            reactionRate = a[j];

            double []Finv_j = (double []) mFinv[j];

            for(int i = 0; i < numSpecies; ++i)
            {
                species = mDynamicSymbols[i];
                deriv = reaction.computeRatePartialDerivativeExpression(reactionRate, species, pSymbolEvaluator);
                if(! deriv.isSimpleNumber())
                {
                    partialDerivExpressions.add(deriv);
                    partialDerivRows.add(new Integer(j));
                    partialDerivCols.add(new Integer(i));
                }
                else
                {
                    derivValue = deriv.getSimpleNumberValue();
                    if(derivValue > 0.0)
                    {
                        for(int jp = 0; jp < numReactions; ++jp)
                        {
                            v_jp = (double []) v[jp];
                            Finv_j[jp] += v_jp[i] * derivValue;
                        }
                    }
                }
            }
        }

        int numPartials = partialDerivExpressions.size();
        mJexp = new Expression[numPartials];
        mJexpRows = new int[numPartials];
        mJexpCols = new int[numPartials];
        for(int k = 0; k < numPartials; ++k)
        {
            mJexp[k] = (Expression) partialDerivExpressions.get(k);
            mJexpRows[k] = ((Integer) partialDerivRows.get(k)).intValue();
            mJexpCols[k] = ((Integer) partialDerivCols.get(k)).intValue();
        }
        
        mF = new Object[numReactions];
        for(int j = 0; j < numReactions; ++j)
        {
            mF[j] = new double[numReactions];
        }
    }
    
}
