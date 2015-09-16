/*
** Filename    : TOutputNetwork.java
** Description : Write the Network object out to the console for inspection
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
*/
package uOutputNetwork;

/**
 * Title:        SBML Validate
 * Description:  SBML Validation Application
 * Copyright:    Copyright (c) 2001
 * Company:      Caltech
 * @author Herbert Sauro
 * @version 1.0
 */

// Wrtite the Network object out to the console for inspection

import uJNetwork.*;
import uReactantList.*;


public class TOutputNetwork {

  TJNetwork Network;

  public TOutputNetwork(TJNetwork Network) {
       this.Network = Network;
  }

  public void OutputNetwork() {
      System.out.println ("Network Dump Follows:\n");
      System.out.println ("Model Name: " + Network.Name + "\n");
      System.out.println ("Number of compartments: " + Network.VolumeList.size());
      System.out.println ("Number of floating species: " + Network.MetaboliteList.size());
      System.out.println ("Number of boundary species: " + Network.BoundaryList.size() + "\n");
      System.out.println ("Number of reactions: " + Network.ReactionList.size() + "\n");
      System.out.println ("Compartment List");
      System.out.println ("----------------");
      for (int i = 0; i<Network.VolumeList.size(); i++) {
          System.out.print (Network.VolumeList.get(i).Name);
          System.out.println (" : " + Network.VolumeList.get(i).Value);
          }

      if (Network.MetaboliteList.size() > 0) {
         System.out.println ("\nFloating Species List");
         System.out.println ("---------------------");
         for (int i = 0; i<Network.MetaboliteList.size(); i++) {
             System.out.print (Network.MetaboliteList.get(i).Name);
             System.out.println (" : " + Network.MetaboliteList.get(i).Value);
             }
         }

      if (Network.BoundaryList.size() > 0) {
         System.out.println ("\nBoundary Species List");
         System.out.println ("---------------------");
         for (int i = 0; i<Network.BoundaryList.size(); i++) {
             System.out.print (Network.BoundaryList.get(i).Name);
             System.out.println (" : " + Network.BoundaryList.get(i).Value);
             }
         }

      if (Network.GlobalParameterList.size() > 0) {
         System.out.println ("\nGlobal Parameter List");
         System.out.println ("----------------------");
         for (int i = 0; i<Network.GlobalParameterList.size(); i++) {
             System.out.print (Network.GlobalParameterList.get(i).Name);
             System.out.println (" : " + Network.GlobalParameterList.get(i).Value);
             }

      }

      TReactantList ReactantList;

      if (Network.ReactionList.size() > 0) {
         System.out.println ("\nReaction List");
         System.out.println ("---------------");
         for (int i = 0; i<Network.ReactionList.size(); i++) {

             System.out.print (Network.ReactionList.get(i).Name + ": ");
             ReactantList = Network.ReactionList.get(i).ReactantList;
             for (int j=0; j<ReactantList.size(); j++) {
                 if (Math.abs (ReactantList.get(j).Stoichiometry) > 1)
                    System.out.print (Math.abs(ReactantList.get(j).Stoichiometry));
                 System.out.print (ReactantList.get(j).Species.Name);
                 if (j < ReactantList.size() - 1)
                    System.out.print (" + ");
             }
             System.out.print (" -> ");

             ReactantList = Network.ReactionList.get(i).ProductList;
             for (int j=0; j<ReactantList.size(); j++) {
                 if (Math.abs (ReactantList.get(j).Stoichiometry) > 1)
                    System.out.print (Math.abs(ReactantList.get(j).Stoichiometry));
                 System.out.print (ReactantList.get(j).Species.Name);
                 if (j < ReactantList.size() - 1)
                    System.out.print (" + ");
             }
             System.out.print (" " + Network.ReactionList.get(i).RateLaw.expression);
             System.out.println();

         }
      }

  }
}