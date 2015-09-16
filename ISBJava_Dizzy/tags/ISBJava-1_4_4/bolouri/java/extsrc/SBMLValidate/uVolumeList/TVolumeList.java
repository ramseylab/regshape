/*
** Filename    : TVolumeList.java
** Description : a list of volume objects, suitably wrapped to avoid casting from Object to Volume
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
package uVolumeList;

import java.util.*;

/**
 * Title:        SBML Validate
 * Description:  SBML Validation Applciation
 * Copyright:    Copyright (c) 2001
 * Company:      Caltech
 * @author Herbert Sauro
 * @version 1.0
 */

// Define a list of volume objects, suitably wrapped to avoid casting
// from Object to Volume

import uVolume.*;
import uIntObject.*;


public class TVolumeList {
     ArrayList FList;

     public TVolumeList() {
       FList = new ArrayList();
     }

     public TVolume get (int Index) {
        return (TVolume) FList.get(Index);
     }

     public void set (int Index, TVolume Item) {
        FList.set (Index, Item);
     }

     public int add (String Name, double Value) {
        FList.add(new TVolume (Name, Value));
        return FList.size() - 1;
     }

     public boolean find (String Name, IntObj Index) {
         boolean result = false; Index.i = -1; int i = 0;
         for (i=0; i<FList.size(); i++) {
             if (((TVolume) FList.get(i)).Name.equals(Name)) {
                result = true;
                Index.i = i;
                break;
             }
         }
         return result;
     }

     public int size() {
        return FList.size();
     }
}