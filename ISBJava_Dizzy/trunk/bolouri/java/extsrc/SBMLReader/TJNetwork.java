package edu.caltech.sbml;
/*
** Filename    : TJNetwork.java
** Description : NOM (Network Object Model)
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
** sramsey  2004/02/13  Changed to "edu.caltech.sbml" package
*/

import java.util.*;

/**
 * Title:        SBML Validate
 * Description:  SBML Validation Application
 * Copyright:    Copyright (c) 2001
 * Company:      Caltech
 * @author Herbert Sauro
 * @version 1.0
 */

 // NOM (Network Object Model)

 // Model details are stored in NOM objects. Parsers of particular
 // information types, eg XML, Jarnac, will load a model into
 // a NOM object. The NOM acts as a common interchange object
 // between different export representations.


public class TJNetwork {
  public TVolumeList VolumeList;
  public TSpeciesList MetaboliteList;
  public TSpeciesList BoundaryList;
  public TParameterList GlobalParameterList;
  public TReactionList ReactionList;
  public String Name;
  public TRuleList RuleList;

  public TJNetwork() {
     VolumeList = new TVolumeList();
     MetaboliteList = new TSpeciesList();
     BoundaryList = new TSpeciesList();
     GlobalParameterList = new TParameterList();
     ReactionList = new TReactionList();
     RuleList = new TRuleList();
  }

  public TSpecies FindMetabolite (String Name) {
       for (int i = 0; i<MetaboliteList.size(); i++) {
           if (MetaboliteList.get(i).Name.equals(Name)) {
              return MetaboliteList.get(i);
           }
       }
       for (int i = 0; i<BoundaryList.size(); i++) {
           if (BoundaryList.get(i).Name.equals(Name)) {
              return BoundaryList.get(i);
           }
       }
       return null;
  }

}
