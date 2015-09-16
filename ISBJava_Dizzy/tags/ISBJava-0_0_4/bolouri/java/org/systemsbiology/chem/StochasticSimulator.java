package org.systemsbiology.chem;

import org.systemsbiology.util.*;
import org.systemsbiology.math.*;
import java.util.Random;

public abstract class StochasticSimulator extends Simulator
{
    protected Random mRandomNumberGenerator;

    protected void setRandomNumberGenerator(Random pRandomNumberGenerator)
    {
        mRandomNumberGenerator = pRandomNumberGenerator;
    }

    protected Random getRandomNumberGenerator()
    {
        return(mRandomNumberGenerator);
    }

    protected void initializeMultistepReactionSolvers()
    {
        MultistepReactionSolver []multistepSolvers = mMultistepReactionSolvers;
        int numMultistepSolvers = multistepSolvers.length;
        for(int ctr = 0; ctr < numMultistepSolvers; ++ctr)
        {
            MultistepReactionSolver solver = multistepSolvers[ctr];
            boolean isStochasticSimulator = true;
            solver.initialize(isStochasticSimulator);
        }
    }

    protected static final double getRandomNumberUniformInterval(Random pRandomNumberGenerator)
    {
        return( 1.0 - pRandomNumberGenerator.nextDouble() );
    }

    protected void initializeRandomNumberGenerator()
    {
        setRandomNumberGenerator(new Random(System.currentTimeMillis()));
    }

    protected void checkDynamicalSymbolsInitialValues() throws InvalidInputException
    {
        int numDynamicalSymbols = mInitialDynamicSymbolValues.length;
        for(int ctr = 0; ctr < numDynamicalSymbols; ++ctr)
        {
            double initialValue = mInitialDynamicSymbolValues[ctr];
            if(initialValue > 1.0 && (initialValue - 1.0 == initialValue))
            {
                throw new InvalidInputException("initial species population value for species is too large for the stochastic Simulator");
            }
        }
    }

//     private static final void updateMultistepReactionSolvers(MultistepReactionSolver []pMultistepReactionSolvers,
//                                                              SymbolEvaluator pSymbolEvaluator,
//                                                              double pCurrentTime) throws DataNotFoundException
//     {
//         int numMultistepReactions = pMultistepReactionSolvers.length;
//         for(int ctr = numMultistepReactions; --ctr >= 0; )
//         {
//             MultistepReactionSolver solver = pMultistepReactionSolvers[ctr];
//             solver.update(pSymbolEvaluator, pCurrentTime);
//         }
//     }

    protected static final int getNextMultistepReactionIndex(MultistepReactionSolver []pMultistepReactionSolvers)
    {
        int numMultistepReactions = pMultistepReactionSolvers.length;
        int nextReactionSolver = -1;
        double nextReactionTime = Double.POSITIVE_INFINITY;
        for(int ctr = numMultistepReactions; --ctr >= 0; )
        {
            MultistepReactionSolver solver = pMultistepReactionSolvers[ctr];
            if(solver.canHaveReaction())
            {
                double specReactionTime = solver.peekNextReactionTime();
                if(specReactionTime < nextReactionTime)
                {
                    nextReactionTime = specReactionTime;
                    nextReactionSolver = ctr;
                }
            }
        }
        return(nextReactionSolver);
    }

    protected static final void updateSymbolValuesForReaction(SymbolEvaluatorChemSimulation pSymbolEvaluator,
                                                              Reaction pReaction,
                                                              double []pSymbolValues,
                                                              double pCurrentTime,
                                                              MultistepReactionSolver []pMultistepReactionSolvers) throws DataNotFoundException
    {
        int numMultistepReactions = pMultistepReactionSolvers.length;

        Species []reactantsSpecies = pReaction.getReactantsSpeciesArray();
        boolean []reactantsDynamic = pReaction.getReactantsDynamicArray();

        int []reactantsStoichiometry = pReaction.getReactantsStoichiometryArray();
        int numReactants = reactantsSpecies.length;
        for(int ctr = numReactants; --ctr >= 0; )
        {
            if(reactantsDynamic[ctr])
            {
                Species reactant = reactantsSpecies[ctr];
                Symbol reactantSymbol = reactant.getSymbol();
                int reactantIndex = reactantSymbol.getArrayIndex();
                assert (reactantIndex != Symbol.NULL_ARRAY_INDEX) : "null array index";
                pSymbolValues[reactantIndex] -= ((double) reactantsStoichiometry[ctr]);
            }
        }

        Species []productsSpecies = pReaction.getProductsSpeciesArray();
        boolean []productsDynamic = pReaction.getProductsDynamicArray();
        int []productsStoichiometry = pReaction.getProductsStoichiometryArray();
        int numProducts = productsSpecies.length;
        for(int ctr = numProducts; --ctr >= 0; )
        {
            if(productsDynamic[ctr])
            {
                Species product = productsSpecies[ctr];
                Symbol productSymbol = product.getSymbol();
                int productIndex = productSymbol.getArrayIndex();
                assert (productIndex != Symbol.NULL_ARRAY_INDEX) : "null array index";
                pSymbolValues[productIndex] += ((double) productsStoichiometry[ctr]);
                if(numMultistepReactions > 0)
                {
                    for(int mCtr = numMultistepReactions; --mCtr >= 0; )
                    {
                        MultistepReactionSolver solver = pMultistepReactionSolvers[mCtr];
                        if(solver.hasIntermedSpecies(product))
                        {
                            assert (productsStoichiometry[ctr] == 1) : "invalid stoichiometry for multistep reaction";
                            solver.addReactant(pSymbolEvaluator);
                        }
                    }
                }
            }
        }
    }

    protected static final double chooseDeltaTimeToNextReaction(Random pRandomNumberGenerator,
                                                                double pReactionProbability)
    {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(pRandomNumberGenerator);
        double inverseRandomNumberUniformInterval = 1.0 / randomNumberUniformInterval;
        assert (randomNumberUniformInterval >= 0.0) : ("randomNumberUniformInterval: " + randomNumberUniformInterval);
        double logInverseRandomNumberUniformInterval = Math.log(inverseRandomNumberUniformInterval);
        double timeConstant = 1.0 / pReactionProbability;

        double deltaTime = timeConstant * logInverseRandomNumberUniformInterval;
        return(deltaTime);
    }

}
