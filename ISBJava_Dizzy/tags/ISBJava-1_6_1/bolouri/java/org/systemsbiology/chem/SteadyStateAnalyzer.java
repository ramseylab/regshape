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
//                System.out.println("a[" + j + "] = " + reactionRateExpression);
//                System.out.println("x[" + i + "] = " + species.getName());
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

    public static double []estimateSpeciesFluctuations(Reaction []pReactions,
                                                       Species []pSpecies,
                                                       Object []pReactionSpeciesAdjustmentVectors,
                                                       SymbolEvaluatorChem pSymbolEvaluator) throws DataNotFoundException
    {
        int numReactions = pReactions.length;
        int numSpecies = pSpecies.length;
        
        Expression []a = Reaction.getReactionRateExpressions(pReactions);
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

        for(int i = 0; i < numSpecies; ++i)
        {
            double eigenvalueRealPart = u.get(i);
            if(eigenvalueRealPart > 0.0)
            {
                return(null);
            }
            T.set(i, i, 1.0/Math.sqrt(Math.abs(eigenvalueRealPart)));
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
        
//         DoubleMatrix2D UTreal = df.make(numSpecies, numSpecies, 0.0);
//         DoubleMatrix2D UTimag = df.make(numSpecies, numSpecies, 0.0);
//         DoubleMatrix2D Ureal = df.make(numSpecies, numSpecies, 0.0);
//         DoubleMatrix2D Uimag = df.make(numSpecies, numSpecies, 0.0);
        
//         DoubleMatrix1D u_imag = eigenvalueDecomposition.getImagEigenvalues();

//         double norm = 1.0/Math.sqrt(2.0);

//         for(int i = 0; i < numSpecies; ++i)
//         {
//             double imaginary_part_of_ith_eigenvalue = u_imag.get(i);
//             if(imaginary_part_of_ith_eigenvalue > 0.0)
//             {
//                 UTreal.set(i, i, norm);
//                 UTreal.set(i + 1, i, norm);
//                 UTreal.set(i, i + 1, 0.0);
//                 UTreal.set(i + 1, i + 1, 0.0);
                
//                 UTimag.set(i, i, 0.0);
//                 UTimag.set(i + 1, i, 0.0);
//                 UTimag.set(i, i + 1, -1.0*norm);
//                 UTimag.set(i + 1, i + 1, norm);

//                 Ureal.set(i, i, norm);
//                 Ureal.set(i + 1, i, 0.0);
//                 Ureal.set(i, i + 1, norm);
//                 Ureal.set(i + 1, i + 1, 0.0);
                
//                 Uimag.set(i, i, 0.0);
//                 Uimag.set(i + 1, i, norm);
//                 Uimag.set(i, i + 1, 0.0);
//                 Uimag.set(i + 1, i + 1, -1.0*norm);
                
//                 ++i;
//             }
//             else
//             {
//                 UTreal.set(i, i, 1.0);
//                 UTimag.set(i, i, 0.0);
//                 Ureal.set(i, i, 1.0);
//                 Uimag.set(i, i, 0.0);
//             }
//         }
        
//         System.out.println("Ureal: " + Ureal.toString());
//         System.out.println("Uimag: " + Uimag.toString());
//         System.out.println("UTreal: " + UTreal.toString());
//         System.out.println("UTimag: " + UTimag.toString());

        
        DoubleMatrix2D w = df.make(numSpecies, numReactions);
        DoubleMatrix2D Qr = algebra.mult(Q, r);

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
}
