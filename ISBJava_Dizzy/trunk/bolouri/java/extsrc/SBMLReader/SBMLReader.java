package edu.caltech.sbml;

/*
** Filename    : NOMService.java
** Description : service implementation thats exported by the SBMLValidate module application
**               provides a SBML parsing service
** Author(s)   : SBW Development Group <sysbio-team@caltech.edu>
** Organization: Caltech ERATO Kitano Systems Biology Project
** Created     : 2001-07-07
** Revision    : $Id$
** $Source$
**
** Copyright 2001 California Institute of Technology and
** Japan Science and Technology Corporation.
**
** This library is free software; you can redistribute it and/or modify it
** under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 2.1 of the License, or
** any later version.
**
** This library is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
** documentation provided hereunder is on an "as is" basis, and the
** California Institute of Technology and Japan Science and Technology
** Corporation have no obligations to provide maintenance, support,
** updates, enhancements or modifications.  In no event shall the
** California Institute of Technology or the Japan Science and Technology
** Corporation be liable to any party for direct, indirect, special,
** incidental or consequential damages, including lost profits, arising
** out of the use of this software and its documentation, even if the
** California Institute of Technology and/or Japan Science and Technology
** Corporation have been advised of the possibility of such damage.  See
** the GNU Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public License
** along with this library; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
**
** The original code contained here was initially developed by:
**
**     Andrew Finney, Herbert Sauro, Michael Hucka, Hamid Bolouri
**     The Systems Biology Workbench Development Group
**     ERATO Kitano Systems Biology Project
**     Control and Dynamical Systems, MC 107-81
**     California Institute of Technology
**     Pasadena, CA, 91125, USA
**
**     http://www.cds.caltech.edu/erato
**     mailto:sysbio-team@caltech.edu
**
** Contributor(s):
**
** sramsey  2004/02/13  Changed to "edu.caltech.sbml" package; unified
**                      all the source code in this directory into a single
**                      package namespace.
*/

import edu.caltech.sbw.*;
import java.util.*;

/**
 * Title:        SBML Validate
 * Description:  SBML Validation Application
 * Copyright:    Copyright (c) 2001
 * Company:      Caltech
 * @author Herbert Sauro
 * @version 1.0
 */

public class SBMLReader
{
    public static final String RULE_TYPE_SPECIES = "species";
    public static final String RULE_TYPE_COMPARTMENT = "compartment";
    public static final String RULE_TYPE_PARAMETER = "parameter";

    static private HashMap functionMap = new HashMap();
    static private String[][] functionData = {
{ "massi1", "Irreversible Mass Action Kinetics for 1 substrate", "S", "k", "k * S" },
{ "massi2", "Irreversible Mass Action Kinetics for 2 substrates", "S1", "S2", "k", "k * S1 * S2" },
{ "massi3", "Irreversible Mass Action Kinetics for 3 substrates", "S1", "S2", "S3", "k", "k * S1 * S2 * S3" },
{ "massr11", "Reversible Mass Action Kinetics for 1 substrate and 1 product", "S", "P", "k_1", "k_2",
    "k_1 * S - k_2 * P"},
{ "massr12", "Reversible Mass Action Kinetics for 1 substrate and 2 products", "S", "P1", "P2", "k_1", "k_2",
    "k_1 * S - k_2 * P1 * P2"},
{ "massr13", "Reversible Mass Action Kinetics for 1 substrate and 3 products", "S", "P1", "P2", "P3", "k_1", "k_2",
    "k_1 * S - k_2 * P1 * P2 * P3"},
{ "massr21", "Reversible Mass Action Kinetics for 2 substrates and 1 product", "S1", "S2", "P", "k_1", "k_2",
    "k_1 * S1 * S2 - k_2 * P"},
{ "massr22", "Reversible Mass Action Kinetics for 2 substrates and 2 products", "S1", "S2", "P1", "P2", "k_1", "k_2",
    "k_1 * S1 * S2 - k_2 * P1 * P2"},
{ "massr23", "Reversible Mass Action Kinetics for 2 substrates and 3 products", "S1", "S2", "P1", "P2", "P3", "k_1", "k_2",
    "k_1 * S1 * S2 - k_2 * P1 * P2 * P3"},
{ "massr31", "Reversible Mass Action Kinetics for 3 substrates and 1 product", "S1", "S2", "S3", "P", "k_1", "k_2",
    "k_1 * S1 * S2 * S3 - k_2 * P"},
{ "massr32", "Reversible Mass Action Kinetics for 3 substrates and 2 products", "S1", "S2", "S3", "P1", "P2", "k_1", "k_2",
    "k_1 * S1 * S2 * S3 - k_2 * P1 * P2"},
{ "massr33", "Reversible Mass Action Kinetics for 3 substrates and 3 products", "S1", "S2", "S3", "P1", "P2", "P3", "k_1", "k_2",
    "k_1 * S1 * S2 * S3 - k_2 * P1 * P2 * P3"},
{ "uui", "Irreversible Simple Michaelis-Menten ", "S", "V_m", "K_m", "(V_m * S)/(K_m + S)" },
{ "uur", "Uni-Uni Reversible Simple Michaelis-Menten", "S", "P", "V_f", "V_r", "K_ms", "K_mp",
    "(V_f * S / K_ms - V_r * P / K_mp)/(1 + S / K_ms +  P / K_mp)"},
{ "uuhr", "Uni-Uni Reversible Simple Michaelis-Menten with Haldane adjustment", "S", "P", "V_f", "K_m1", "K_m2", "K_eq",
    "( V_f / K_m1 * (S - P / K_eq ))/(1 + S / K_m1 + P / K_m2)"},
{ "isouur", "Iso Uni-Uni", "S", "P", "V_f", "K_ms", "K_mp", "K_ii", "K_eq",
    "(V_f * (S - P / K_eq ))/(S * (1 + P / K_ii ) + K_ms * (1 + P / K_mp))"},
{ "hilli", "Hill Kinetics", "S", "V", "S_0_5", "h", "(V * pow(S,h))/(pow(S_0_5,h) + pow(S,h))"},
{ "hillr", "Reversible Hill Kinetics", "S", "P", "V_f", "S_0_5", "P_0_5", "h", "K_eq",
"(V_f * (S / S_0_5) * (1 - P / (S * K_eq) ) * pow(S / S_0_5 + P / P_0_5, h-1))/(1 + pow(S / S_0_5 + P / P_0_5, h))"},
{ "hillmr", "Reversible Hill Kinetics with One Modifier", "S", "M", "P", "V_f", "K_eq", "k", "h", "alpha",
"(V_f * (S / S_0_5) * (1 - P / (S * K_eq) ) * pow(S / S_0_5 + P / P_0_5, h-1))/( pow(S / S_0_5 + P / P_0_5, h) + (1 + pow(M / M_0_5, h))/(1 + alpha * pow(M/M_0_5,h)))"},
{ "hillmmr", "Reversible Hill Kinetics with Two Modifiers", "S", "P", "M", "V_f", "K_eq", "k", "h", "a", "b", "alpha_1", "alpha_2", "alpha_12",
"(V_f * (S / S_0_5) * (1 - P / (S * K_eq) ) * pow(S / S_0_5 + P / P_0_5, h-1)) / (pow(S / S_0_5 + P / P_0_5, h) + ((1 + pow(Ma/Ma_0_5,h) + pow(Mb/Mb_0_5,h))/( 1 + alpha_1 * pow(Ma/Ma_0_5,h) + alpha_2 * pow(Mb/Mb_0_5,h) + alpha_1 * alpha_2 * alpha_12 * pow(Ma/Ma_0_5,h) * pow(Mb/Mb_0_5,h))))"},
{ "usii", "Substrate Inhibition Kinetics (Irreversible)", "S", "V", "K_m", "K_i", "V*(S/K_m)/(1 + S/K_m + sqr(S)/K_i)"},
{ "usir", "Substrate Inhibition Kinetics (Reversible)", "S", "P", "V_f", "V_r", "K_ms", "K_mp", "K_i",
  "(V_f*S/K_ms + V_r*P/K_mp)/(1 + S/K_ms + P/K_mp + sqr(S)/K_i)"},
{ "usai", "Substrate Activation", "S", "V", "K_sa", "K_sc", "V * sqr(S/K_sa)/(1 + S/K_sc + sqr(S/K_sa) + S/K_sa)"},
{ "ucii", "Competitive Inhibition (Irreversible)", "S", "V", "K_m", "K_i", "(V * S/K_m)/(1 + S/K_m + I/K_i)"},
{ "ucir", "Competitive Inhibition (Reversible)", "S", "P", "V_f", "V_r", "K_ms", "K_mp", "K_i",
  "(V_f*S/K_ms - V_r*P/K_mp)/(1 + S/K_ms + P/K_mp + I/K_i)"},
{ "unii", "Noncompetitive Inhibition (Irreversible)", "S", "I", "V", "K_m", "K_i", "(V*S/K_m)/(1 + I/K_i + (S/K_m)*(1 + I/K_i))"},
{ "unir", "Noncompetitive Inhibition (Reversible)", "S", "P", "I", "V_f", "K_ms", "K_mp", "K_i",
  "(V_f*S/K_ms - V_r*P/K_mp)/(1 + I/K_i + (S/K_ms + P/K_mp )*(1 + I/K_i))"},
{ "uuci", "Uncompetitive Inhibition (Irreversible)", "S", "I", "V", "K_m", "K_i", "(V*S/K_m)/(1 + (S/K_m)*(1 + I/K_i))"},
{ "uucr", "Uncompetitive Inhibition (Reversible)", "S", "P", "I", "V_f", "V_r", "K_ms", "K_mp", "K_i",
  "(V_f*S/K_ms - V_r*P/K_mp)/(1 + ( S/K_ms + P/K_mp )*( 1 + I/K_i))"},
{ "umi", "Mixed Inhibition Kinetics (Irreversible)", "S", "I", "V", "K_m", "K_is", "K_ic",
  "(V*S/K_m)/(1 + I/K_is + (S/K_m)*(1 + I/K_ic))"},
{ "umr", "Mixed Inhibition Kinetics (Reversible)", "S", "P", "I", "V_f", "V_r", "K_ms", "K_mp", "K_is", "K_ic",
  "(V_f*S/K_ms - V_r*P/K_mp)/(1 + I/K_is + ( S/K_ms + P/K_mp )*( 1 + I/K_ic ))"},
{ "uai", "Specific Activation Kinetics - irreversible", "S", "A_c", "V", "K_m", "K_a", "(V*S/K_m)/(1 + S/K_m + K_a/A_c)"},
{ "uar", "Specific Activation Kinetics (Reversible)", "S", "P", "A_c", "V_f", "V_r", "K_ms", "K_mp", "K_a",
  "(V_f*S/K_ms - V_r*P/K_mp)/(1 + S/K_ms + P/K_mp + K_a/A_c)"},
{ "ucti", "Catalytic Activation (Irreversible)", "S", "A_c", "V", "K_m", "K_a", "(V*S/K_m)/(1 + K_a/A_c + (S/K_m)*(1 + K_a/A_c))"},
{ "uctr", "Catalytic Activation (Reversible)", "S", "P", "A_c", "V_f", "V_r", "K_ms", "K_mp", "K_a",
  "(V_f*S/K_ms - V_r*P/K_mp)/(1 + K_a/A_c + (S/K_ms + P/K_mp)*(1 + K_a/A_c))"},
{ "umai", "Mixed Activation Kinetics (Irreversible)", "S", "A_c", "V", "K_m", "Kas", "Kac",
  "(V*S/K_m)/(1 + Kas/A_c + (S/K_m)*(1 + Kac/A_c))"},
{ "umar", "Mixed Activation Kinetics (Reversible)", "S", "P", "A_c", "V_f", "V_r", "K_ms", "K_mp", "K_as", "K_ac",
  "(V_f*S/K_ms - V_r*P/K_mp)/(1 + K_as/A_c + (S/K_ms + P/K_mp)*(1 + K_ac/A_c))"},
{ "uhmi", "General Hyperbolic Modifier Kinetics (Irreversible)", "S", "M", "V", "K_m", "K_d", "a", "b",
  "(V*(S/K_m)*(1 + b * M / (a*K_d)))/(1 + M/K_d + (S/K_m)*(1 + M/(a*K_d)))"},
{ "uhmr", "General Hyperbolic Modifier Kinetics (Reversible)", "S", "P", "M", "V_f", "V_r", "K_ms", "K_mp", "K_d", "a", "b",
  "((V_f*S/K_ms - V_r*P/K_mp)*(1 + b*M/(a*K_d)))/(1 + M/K_d + (S/K_ms + P/K_mp)*(1 + M/(a*K_d)))"},
{ "ualii", "Allosteric inhibition (Irreversible)", "S", "I", "V", "K_s", "K_ii", "n", "L",
  "(V*pow(1 + S/K_s, n-1))/(L*pow(1 + I/K_ii,n) + pow(1 + S/K_s,n))"},
{ "ordubr", "Ordered Uni Bi Kinetics", "A", "P", "Q", "V_f", "V_r", "K_ma", "K_mq", "K_mp", "K_ip", "K_eq",
  "(V_f*( A - P*Q/K_eq))/(K_ma + A*(1 + P/K_ip) + (V_f/(V_r*K_eq))*(K_mq*P + K_mp*Q + P*Q))"},
{ "ordbur", "Ordered Bi Uni Kinetics", "A", "B", "P", "V_f", "V_r", "K_ma", "Kmb", "K_mp", "K_ia", "K_eq",
  "(V_f*(A*B - P/K_eq))/(A*B + K_ma*B + Kmb*A + (V_f/(V_r*K_eq))*(K_mp + P*(1 + A/K_ia)))"},
{ "ordbbr", "Ordered Bi Bi Kinetics", "A", "B", "P", "Q", "V_f", "K_ma", "K_mb", "K_mp", "K_ia", "K_ib", "K_ip", "K_eq",
  "(V_f*(A*B - P*Q/K_eq))/(A*B*(1 + P/K_ip) + K_mb*(A + K_ia) + K_ma*B + ((V_f / (V_r*K_eq)) * (K_mq*P*( 1 + A/K_ia) + Q*(K_mp*( 1 + (K_ma*B)/(K_ia*K_mb) + P*(1 + B/K_ib))))))"},
{ "ppbr", "Ping Pong Bi Bi Kinetics", "A", "B", "P", "Q", "V_f", "V_r", "K_ma", "K_mb", "K_mp", "K_mq", "K_ia", "K_iq", "K_eq",
  "(V_f*(A*B - P*Q/K_eq))/(A*B + K_mb*A + K_ma*B*(1 + Q/K_iq) + ((V_f/(V_r*K_eq))*(K_mq*P*(1 + A/K_ia) + Q*(K_mp + P))))"}};

      private TJNetwork network ;
    private String sbml ;

    public void loadSBML(String xml) throws SBWException
    {
        try
        {
            sbml = xml ;
            TJXML Parser = new TJXML();

            Parser.LoadFromString(xml);
            network = Parser.Network ;
        }
        catch (Exception e)
        {
            throw SBWException.translateException(e);
        }
    }

    public String getSBML()
    {
        return sbml;
    }

    public String getModelName()
    {
        return network.Name;
    }

    public int getNumCompartments()
    {
        return network.VolumeList.size();
    }

    public int getNumReactions()
    {
        return network.ReactionList.size();
    }

    public int getNumFloatingSpecies()
    {
        return network.MetaboliteList.size();
    }

    public int getNumBoundarySpecies()
    {
        return network.BoundaryList.size();
    }

    public int getNumGlobalParameters()
    {
        return network.GlobalParameterList.size();
    }

    public String getNthCompartmentName(int compartment)
    {
        return network.VolumeList.get(compartment).Name;
    }

    public String getNthFloatingSpeciesName(int floatingSpecies)
    {
        return network.MetaboliteList.get(floatingSpecies).Name;
    }

    public String getNthBoundarySpeciesName(int boundarySpecies)
    {
        return network.BoundaryList.get(boundarySpecies).Name;
    }

    public String getNthFloatingSpeciesCompartmentName(int floatingSpecies)
    {
        return network.MetaboliteList.get(floatingSpecies).Volume.Name;
    }

    public String getNthBoundarySpeciesCompartmentName(int boundarySpecies)
    {
        return network.BoundaryList.get(boundarySpecies).Volume.Name;
    }

    public boolean isReactionReversible(int reaction)
    {
        return network.ReactionList.get(reaction).Reversible;
    }

    public String getNthReactionName(int reaction)
    {
        return network.ReactionList.get(reaction).Name;
    }

    public int getNumReactants(int reaction)
    {
        return network.ReactionList.get(reaction).ReactantList.size();
    }

    public int getNumProducts(int reaction)
    {
        return network.ReactionList.get(reaction).ProductList.size();
    }

    public String getNthReactantName(int reaction, int reactant)
    {
        return network.ReactionList.get(reaction).ReactantList.get(reactant).Species.Name;
    }

    public String getNthProductName(int reaction, int product)
    {
        return network.ReactionList.get(reaction).ProductList.get(product).Species.Name;
    }

    public String getKineticLaw(int reaction)
    {
        return network.ReactionList.get(reaction).RateLaw.expression;
    }

    public int getNthReactantStoichiometry(int reaction, int reactant)
    {
        return network.ReactionList.get(reaction).ReactantList.get(reactant).Stoichiometry;
    }

    public int getNthProductStoichiometry(int reaction, int product)
    {
        return network.ReactionList.get(reaction).ProductList.get(product).Stoichiometry;
    }

    public int getNumParameters(int reaction)
    {
        return network.ReactionList.get(reaction).RateLaw.ParameterList.size();
    }

    public String getNthParameterName(int reaction, int parameter)
    {
        return network.ReactionList.get(reaction).RateLaw.ParameterList.get(parameter).Name;
    }

    public double getNthParameterValue(int reaction, int parameter)
    {
        return network.ReactionList.get(reaction).RateLaw.ParameterList.get(parameter).Value;
    }

    public boolean getNthParameterHasValue(int reaction, int parameter)
    {
        return network.ReactionList.get(reaction).RateLaw.ParameterList.get(parameter).HasValue != 0;
    }

    public String getNthGlobalParameterName(int globalParameter)
    {
        return network.GlobalParameterList.get(globalParameter).Name;
    }

    private TBaseSymbol getSymbol(String name) throws SBWException
    {
        TBaseSymbol symbol = getSymbolOrNull(name);

        if (symbol == null)
            throw new SBWApplicationException("unable to find symbol", "");

        return symbol ;
    }

    public boolean exists(String name)
    {
        TBaseSymbol symbol = getSymbolOrNull(name);

        return symbol != null;
    }

    private TBaseSymbol getSymbolOrNull(String name)
    {
        TBaseSymbol symbol = network.FindMetabolite(name);

        if (symbol != null)
            return symbol ;

        int i = 0 ;

        while (i != getNumBoundarySpecies())
        {
            if (name.equals(getNthBoundarySpeciesName(i)))
                return network.BoundaryList.get(i);

            i++ ;
        }

        i = 0 ;
        while (i != getNumCompartments())
        {
            if (name.equals(getNthCompartmentName(i)))
                return network.VolumeList.get(i);

            i++ ;
        }

        i = 0 ;
        while (i != getNumGlobalParameters())
        {
            if (name.equals(getNthGlobalParameterName(i)))
                return network.GlobalParameterList.get(i);

            i++ ;
        }

        return null ;
    }

    public boolean hasValue(String name) throws SBWException
    {
        return getSymbol(name).HasValue == TConstants.nsUnDefined ? false : true;
    }

    public double getValue(String name) throws SBWException
    {
        return getSymbol(name).Value;
    }

    public String[] getBuiltinFunctionInfo(String name)
    {
        String[] result = (String[])functionMap.get(name);

        if (result == null)
            return new String[0];
        else
            return result ;
    }

    public String[] getBuiltinFunctions()
    {
        String[] result = new String[functionData.length];
        int i = 0 ;

        while (i != functionData.length)
        {
            result[i] = functionData[i][0];
            i++ ;
        }

        return result ;
    }

    public int getNumRules()
    {
        return(network.RuleList.size());
    }

    public String getNthRuleName(int pIndex)
    {
        String retName = null;
        TRule rule = network.RuleList.get(pIndex);
        if(null != rule)
        {
            retName = rule.Name;
        }
        return(retName);
    }

    public String getNthRuleFormula(int pIndex) 
    {
        String retFormula = null;
        TRule rule = network.RuleList.get(pIndex);
        if(null != rule)
        {
            retFormula = rule.getFormula();
        }
        return(retFormula);
    }

    public String getNthRuleType(int pIndex)
    {
        String retType = null;
        TRule rule = network.RuleList.get(pIndex);
        if(null != rule)
        {
            retType = rule.getType().toString();
        }
        return(retType);
    }

    public String getSubstanceUnitsString()
    {
        return(network.SubstanceUnitsString);
    }

    static
    {
        int i = 0 ;

        while (i != functionData.length)
        {
            String[] function = functionData[i];
            String[] data = new String[function.length - 1];
            int j = 1 ;

            while (j != function.length)
            {
                data[j - 1] = function[j];
                j++ ;
            }

            functionMap.put(function[0], data);
            i++;
        }
    }
}
