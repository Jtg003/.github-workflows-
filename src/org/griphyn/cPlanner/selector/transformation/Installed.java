/**
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.griphyn.cPlanner.selector.transformation;

import org.griphyn.cPlanner.selector.TransformationSelector;

import org.griphyn.common.catalog.TransformationCatalogEntry;
import org.griphyn.common.classes.TCType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This implementation of the Selector returns a list of  TransformationCatalogEntry objects of type INSTALLED y on the submit site.
 *
 * @author Gaurang Mehta
 *
 * @version $Revision$
 */

public class Installed
    extends TransformationSelector {

    /**
     * Returns a list of TransformationCatalogEntry objects of type installed
     * from a List of valid TCEntries
     * @param tcentries List The original list containing TransformationCatalogEntries.
     * @return List returns a List of TransformationCatalogEntry objects of type INSTALLED
     *
     */
    public List getTCEntry( List tcentries ) {
        List results = null;
        for ( Iterator i = tcentries.iterator(); i.hasNext(); ) {
            TransformationCatalogEntry tc = ( TransformationCatalogEntry ) i.
                next();
            if ( tc.getType().equals( TCType.INSTALLED ) ) {
                if ( results == null ) {
                    results = new ArrayList( 5 );
                }
                results.add( tc );
            }
        }
        return results;
    }
}
