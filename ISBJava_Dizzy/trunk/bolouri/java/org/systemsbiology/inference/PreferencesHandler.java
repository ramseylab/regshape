/*
 * Copyright (C) 2005 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser
 * General Public License, the text of which should have
 * been distributed with this source code in the file 
 * License.html.  The license can also be obtained at:
 *   http://www.gnu.org/copyleft/lesser.html
 */
package org.systemsbiology.inference;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * @author sramsey
 *
 */
public class PreferencesHandler
{
    private Preferences mPreferences;
    public static final String KEY_DELIMITER = "delimiter";
    
    public PreferencesHandler()
    {
        mPreferences = Preferences.userNodeForPackage(this.getClass());
    }
    
    public void setPreference(String pKey, String pValue)
    {
        mPreferences.put(pKey, pValue);
    }
    
    public String getPreference(String pKey, String pDefault)
    {
        return mPreferences.get(pKey, pDefault);
    }
    
    public void flush() throws BackingStoreException
    {
        mPreferences.flush();
    }
}
