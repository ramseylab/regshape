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
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import java.util.*;

public final class SteadyStateAnalyzer
{
    private Model mModel;

    public SteadyStateAnalyzer(Model pModel)
    {
        mModel = pModel;
    }

    public static Object []computeJacobian(Expression []pReactionRateExpressions,
                                           Reaction []pReactions,
                                           Species []pSpecies,
                                           Object []pReactionSpeciesAdjustmentVectors,
                                           SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        int numReactions = pReactions.length;
        Object []v = pReactionSpeciesAdjustmentVectors;

        int numSpecies = pSpecies.length;

        Object []jac = new Object[numSpecies];
        Object []partials = new Object[numSpecies];
        
        for(int i = 0; i < numSpecies; ++i)
        {
            Species species = pSpecies[i];
            Symbol speciesSymbol = species.getSymbol();
            double []partialsi = new double[numReactions];
            partials[i] = partialsi;
            for(int j = 0; j < numReactions; ++j)
            {
                Reaction reaction = pReactions[j];
                Expression reactionRateExpression = pReactionRateExpressions[j];
                partialsi[j] = reaction.computeRatePartialDerivative(reactionRateExpression,
                                                                     species,
                                                                     pSymbolEvaluator);
            }
        }

        for(int i = 0; i < numSpecies; ++i)
        {
            double []jaci = new double[numSpecies];
            jac[i] = jaci;
            for(int ip = 0; ip < numSpecies; ++ip)
            {
                double []partialsip = (double []) partials[ip];
                Expression jacexp = Expression.ZERO;
                double sum = 0.0;
                for(int j = 0; j < numReactions; ++j)
                {
                    double []vj = (double []) v[j];
                    sum += vj[i] * partialsip[j];
                }
                jaci[ip] = sum;
            }
        }

        return(jac);
    }

    public static double []estimateSpeciesFluctuations(Reaction []pReactions,
                                                       Species []pSpecies,
                                                       Object []pReactionSpeciesAdjustmentVectors,
                                                       SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        int numReactions = pReactions.length;
        int numSpecies = pSpecies.length;
        
        Expression []a = Reaction.getReactionRateExpressions(pReactions);
        Object []Jdbl = computeJacobian(a, pReactions, pSpecies, pReactionSpeciesAdjustmentVectors, pSymbolEvaluator);

        // allocate storage for the matrix J
        DoubleFactory2D df = DoubleFactory2D.sparse;
        DoubleMatrix2D J = df.make(numSpecies, numSpecies);

        for(int i = 0; i < numSpecies; ++i)
        {
            double []Jdbl_i = (double [])Jdbl[i];
            for(int ip = 0; ip < numSpecies; ++ip)
            {
                J.set(i, ip, Jdbl_i[ip]);
            }
        }

        EigenvalueDecomposition eigenvalueDecomposition = new EigenvalueDecomposition(J);
        DoubleMatrix2D P = eigenvalueDecomposition.getV();
        DoubleMatrix1D u = eigenvalueDecomposition.getRealEigenvalues();
        DoubleMatrix2D T = df.make(numSpecies, numSpecies);

        for(int i = 0; i < numSpecies; ++i)
        {
            double eigenvalueRealPart = u.get(i);
            if(eigenvalueRealPart > 0.0)
            {
                return(null);
            }
            T.set(i, i, 1.0/Math.sqrt(Math.abs(eigenvalueRealPart)));
        }

        Algebra algebra = new Algebra();

        DoubleMatrix2D Pinv = algebra.inverse(P);

        DoubleMatrix2D PT = algebra.mult(P, T);
        
        DoubleMatrix2D Q = algebra.mult(PT, Pinv);

        Object []v = pReactionSpeciesAdjustmentVectors;
        
        DoubleMatrix2D r = df.make(numSpecies, numReactions);

        for(int j = 0; j < numReactions; ++j)
        {
            double []vj = (double []) v[j];
            double sum = 0.0;
            for(int i = 0; i < numSpecies; ++i)
            {
                sum += Math.abs(vj[i]);
            }
            double norm = 1.0 / Math.sqrt(sum);
            for(int i = 0; i < numSpecies; ++i)
            {
                r.set(i, j, vj[i] * norm);
            }
        }
        
        DoubleMatrix2D Qr = algebra.mult(Q, r);

        DoubleMatrix2D w = df.make(numSpecies, numReactions);

        for(int i = 0; i < numSpecies; ++i)
        {
            for(int j = 0; j < numReactions; ++j)
            {
                w.set(i, j, 0.5 * Math.pow(Qr.get(i, j), 2.0));
            }
        }

        DoubleFactory1D df1 = DoubleFactory1D.sparse;

        DoubleMatrix1D s = df1.make(numReactions);

        for(int j = 0; j < numReactions; ++j)
        {
            Expression reactionRateExpression = a[j];
            Reaction reaction = pReactions[j];
            double rate = reaction.computeRate(pSymbolEvaluator);
            s.set(j, rate);
        }

        DoubleMatrix1D var = algebra.mult(w, s);

        double []varArray = (double []) var.toArray();

        for(int i = 0; i < numSpecies; ++i)
        {
            varArray[i] = Math.sqrt(varArray[i]);
        }
        return(varArray);
    }

    public HashMap estimateSteadyStateVariance() throws IllegalStateException, DataNotFoundException
    {
        Model model = mModel;

        Species []species = model.constructDynamicSymbolsArray();
        Reaction []reactions = model.constructReactionsArray();
        
        int numReactions = reactions.length;
        SymbolValue []nonDynamicSymbols = model.constructGlobalNonDynamicSymbolsArray();

        for(int j = 0; j < numReactions; ++j)
        {
            Reaction reaction = reactions[j];
            reaction.prepareSymbolVectorsForSimulation(species, nonDynamicSymbols, null);
        }

        SymbolEvaluatorChem symbolEvaluator = model.getSymbolEvaluator();
        if(null == symbolEvaluator.getSymbolsMap())
        {
            throw new IllegalStateException("a simulation has not yet been conducted on this model");
        }

        HashMap steadyStateValuesMap = null;

        Object []reactionSpeciesAdjustmentVectors = new Object[numReactions];
        for(int ctr = 0; ctr < numReactions; ++ctr)
        {
            Reaction reaction = reactions[ctr];
            reactionSpeciesAdjustmentVectors[ctr] = reaction.constructDynamicSymbolAdjustmentVector(species);
        }

        double []steadyStateValues = estimateSpeciesFluctuations(reactions,
                                                                 species,
                                                                 reactionSpeciesAdjustmentVectors,
                                                                 symbolEvaluator);

        if(null != steadyStateValues)
        {
            steadyStateValuesMap = new HashMap();
            int numSpecies = species.length;
            
            for(int i = 0; i < numSpecies; ++i)
            {
                String speciesName = species[i].getName();
                Double value = new Double(steadyStateValues[i]);
                steadyStateValuesMap.put(speciesName, value);
            }
        }

        return(steadyStateValuesMap);
    }
}
