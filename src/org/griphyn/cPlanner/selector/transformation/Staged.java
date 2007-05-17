/*
 * This file or a portion of this file is licensed under the terms of
 * the Globus Toolkit Public License, found in file GTPL, or at
 * http://www.globus.org/toolkit/download/license.html. This notice must
 * appear in redistributions of this file, with or without modification.
 *
 * Redistributions of this Software, with or without modification, must
 * reproduce the GTPL in: (1) the Software, or (2) the Documentation or
 * some other similar material which is provided with the Software (if
 * any).
 *
 * Copyright 1999-2004 University of Chicago and The University of
 * Southern California. All rights reserved.
 */
package org.griphyn.cPlanner.selector.transformation;

import org.griphyn.cPlanner.selector.TransformationSelector;

import org.griphyn.common.catalog.TransformationCatalogEntry;
import org.griphyn.common.classes.TCType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This implementation of the Selector select a transformation of type STATIC_BINARY on all sites.
 *
 * @author Gaurang Mehta
 * @version $Revision: 1.3 $
 */
public class Staged
    extends TransformationSelector {

    /**
     * Takes a list of TransformationCatalogEntry objects and returns 1 or
     * many TransformationCatalogEntry objects as a list  by selecting only Static stageable binary's
     *
     * @param tcentries List
     * @return List
     */
    public List getTCEntry( List tcentries ) {
        List results = null;
        for ( Iterator i = tcentries.iterator(); i.hasNext(); ) {
            TransformationCatalogEntry tc = ( TransformationCatalogEntry ) i.
                next();
            if ( tc.getType().equals( TCType.STATIC_BINARY ) ) {
                if ( results == null ) {
                    results = new ArrayList( 5 );
                }
                results.add( tc );
            }
        }
        return results;
    }
}
