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

/**
 * Class providing functions for analyzing the
 * steady-state behavior of a {@link Model}.  To use
 * this class, you must have previously run a
 * simulation using a {@link ISimulator}, and have
 * references to the {@link SymbolEvaluatorChem} object
 * that will return the final values for species concentrations
 * in the model, resulting from the simulation. 
 *
 * @author Stephen Ramsey
 */
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

        HashMap []localSymbolsMaps = new HashMap[numReactions];

        for(int j = 0; j < numReactions; ++j)
        {           
            localSymbolsMaps[j] = Simulator.createLocalSymbolsMap(pReactions[j]);
        }

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
                partialsi[j] = Simulator.computeRatePartialDerivativeExpression(reactionRateExpression,
                                                                                species,
                                                                                pSymbolEvaluator,
                                                                                localSymbolsMaps[j]).computeValue(pSymbolEvaluator);
            }
        }

        for(int i = 0; i < numSpecies; ++i)
        {
            double []jaci = new double[numSpecies];
            jac[i] = jaci;
            for(int ip = 0; ip < numSpecies; ++ip)
            {
                double []partialsip = (double []) partials[ip];
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

    /**
     * For the supplied reactions and species, estimates the
     * steady-state species fluctuations in the model.  If the
     * estimate is unsuccessful (e.g., because of the absence of 
     * an eigenvalue with negative real part), null is returned.
     */
    public static double []estimateSpeciesFluctuations(Reaction []pReactions,
                                                       Species []pSpecies,
                                                       Object []pReactionSpeciesAdjustmentVectors,
                                                       double []pReactionProbabilities,
                                                       SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        int numReactions = pReactions.length;
        int numSpecies = pSpecies.length;
        
        Expression []a = Simulator.getReactionRateExpressions(pReactions);
        Object []Jdbl = computeJacobian(a, pReactions, pSpecies, pReactionSpeciesAdjustmentVectors, pSymbolEvaluator);
        Algebra algebra = new Algebra();

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

        double eigenvalueRealPart = 0.0;
        double matElem = 0.0;
        boolean gotNegEigenvalue = false;
        for(int i = 0; i < numSpecies; ++i)
        {
            eigenvalueRealPart = u.get(i);
            if(eigenvalueRealPart < 0.0)
            {
                matElem = 1.0/Math.sqrt(Math.abs(eigenvalueRealPart));
                gotNegEigenvalue = true;
            }
            else
            {
                matElem = 0.0;
            }
            T.set(i, i, matElem);
        }

        if(! gotNegEigenvalue)
        {
            return(null);
        }

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

        DoubleFactory1D df1 = DoubleFactory1D.dense;

        DoubleMatrix1D s = df1.make(numReactions);

        for(int j = 0; j < numReactions; ++j)
        {
            s.set(j, pReactionProbabilities[j]);
        }

//---------------------------------------------------------
// added for David Orrell's testing purposes: (need to find a way to
// make this a debugging option)
//         for(int j = 0; j < numReactions; ++j)
//         {
//             System.out.println("reaction[" + j + "] = \"" + pReactions[j].toString() + "\"");
//         }
//         for(int i = 0; i < numSpecies; ++i)
//         {
//             System.out.println("species[" + i + "] = \"" + pSpecies[i].toString() + "\"");
//         }
//         StringBuffer sb = new StringBuffer("");
//         for(int i = 0; i < numSpecies; ++i)
//         {
//             for(int j = 0; j < numReactions; ++j)
//             {
//                 double val = w.get(i, j) * s.get(j);
//                 sb.append(val);
//                 if(j < numReactions - 1)
//                 {
//                     sb.append(", ");
//                 }
//             }
//             sb.append("\n");
//         }
//         System.out.println(sb.toString());
//---------------------------------------------------------

        DoubleMatrix1D var = algebra.mult(w, s);

        double []varArray = (double []) var.toArray();

        for(int i = 0; i < numSpecies; ++i)
        {
            varArray[i] = Math.sqrt(varArray[i]);
        }
        return(varArray);
    }
}
