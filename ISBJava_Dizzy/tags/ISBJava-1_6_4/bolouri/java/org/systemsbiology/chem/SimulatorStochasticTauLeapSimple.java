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
 * is optimized for simple models (models with simple elementary
 * reactions, rather than complex rate law expressions).
 *
 * @author Stephen Ramsey
 */
public class SimulatorStochasticTauLeapSimple extends SimulatorStochasticTauLeapBase implements IAliasableClass, ISimulator
{
    public static final String CLASS_ALIAS = "tauleap-simple";

    private Expression []mMu;
    private Expression []mSigma;

    protected double computeLeapTime(double pSumReactionProbabilities) throws DataNotFoundException
    {
        int numReactions = mReactionProbabilities.length;
        int numSpecies = mDynamicSymbols.length;

        SymbolEvaluatorChem symbolEvaluator = mSymbolEvaluator;

        double muj = 0.0;
        double sigmaj = 0.0;

        double jumpTime = Double.MAX_VALUE;
        double muVal = 0.0;
        double sigmaVal = 0.0;
        double muFac = mAllowedError * pSumReactionProbabilities;
        double sigmaFac = muFac*muFac;

        for(int j = numReactions; --j >= 0; )
        {
            muj = mMu[j].computeValue(symbolEvaluator);

            muVal = muFac / Math.abs(muj);
            if(muVal < jumpTime)
            {
                jumpTime = muVal;
            }

            sigmaj = mSigma[j].computeValue(symbolEvaluator);

            sigmaVal = sigmaFac / sigmaj;
            if(sigmaVal < jumpTime)
            {
                jumpTime = sigmaVal;
            }
        }

        return(jumpTime);
    }


    protected void initializeTauLeap(SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException, InvalidInputException
    {
        int numReactions = mReactions.length;
        int numSpecies = mDynamicSymbols.length;

        Object []v = mDynamicSymbolAdjustmentVectors;

        Expression []a = Simulator.getReactionRateExpressions(mReactions);
        Species species = null;
        Reaction reaction = null;
        Expression reactionRate = null;
        Expression deriv = null;
        double derivValue = 0.0;

        Object []f = new Object[numReactions];
        Expression []fj = null;

        for(int j = 0; j < numReactions; ++j)
        {
            if(mReactionHasLocalSymbolsFlags[j])
            {
                throw new InvalidInputException("this model contains local parameter definitions, which are not supported by the \"" + CLASS_ALIAS + "\" simulator; please try using a different simulator");
            }

            fj = new Expression[numReactions];
            f[j] = fj;
            for(int jp = 0; jp < numReactions; ++jp)
            {
                fj[jp] = null;
            }
        }

        double []v_jp = null;
        double vjpi = 0.0;

        for(int j = 0; j < numReactions; ++j)
        {
            reaction = mReactions[j];
            reactionRate = a[j];

            fj = (Expression []) f[j];

            for(int i = 0; i < numSpecies; ++i)
            {
                species = mDynamicSymbols[i];
                deriv = reaction.computeRatePartialDerivativeExpression(reactionRate, species, pSymbolEvaluator);

                for(int jp = 0; jp < numReactions; ++jp)
                {
                    if(! deriv.isSimpleNumber() || 0.0 != deriv.getSimpleNumberValue())
                    {
                        vjpi = ((double []) v[jp])[i];
                        if(vjpi != 0.0)
                        {
                            if(null == fj[jp])
                            {
                                fj[jp] = new Expression(0.0);
                            }
                            fj[jp] = Expression.add(Expression.multiply(new Expression(vjpi), deriv), fj[jp]);
                        }
                    }
                }
            }
        }

        Expression []mu = new Expression[numReactions];
        Expression []sigma = new Expression[numReactions];
        Expression ajp = null;
        Expression fjjp = null;
        for(int j = 0; j < numReactions; ++j)
        {
            mu[j] = new Expression(0.0);
            sigma[j] = new Expression(0.0);
            fj = (Expression []) f[j];

            for(int jp = 0; jp < numReactions; ++jp)
            {
                ajp = a[jp];
                fjjp = fj[jp];
                if((! ajp.isSimpleNumber() || 0.0 != ajp.getSimpleNumberValue()) &&
                   (null != fjjp))
                {
                    mu[j] = Expression.add(mu[j], Expression.multiply(ajp, fjjp));
                    sigma[j] = Expression.add(sigma[j], Expression.multiply(ajp, Expression.square(fjjp)));
                }
            }

            f[j] = null;
        }

        f = null;

        mMu = mu;
        mSigma = sigma;
    }
}
