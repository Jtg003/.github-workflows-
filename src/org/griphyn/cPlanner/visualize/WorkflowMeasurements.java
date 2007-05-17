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
package org.griphyn.cPlanner.visualize ;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;

/**
 * A container object that stores the measurements for each site on which
 * the workflow was executed.
 *
 * @author Karan Vahi
 * @version $Revision: 1.1 $
 */
public class WorkflowMeasurements {

    /**
     * The map that stores the list of <code>Measurement</code> indexed by site name.
     */
    private Map mStore;

    /**
     * The default constructor.
     */
    public WorkflowMeasurements() {
        mStore = new HashMap();
    }


    /**
     * Returns an iterator to list of <code>String</code> site identifiers
     * for which data is available.
     *
     * @return Iterator
     */
    public Iterator siteIterator(){
        return mStore.keySet().iterator();
    }

    /**
     * Returns the list of <code>Measurement</code> objects corresponding to a
     * particular site.
     *
     * @param site  the site for which Measurements are required.
     *
     * @return List
     */
    public List getMeasurements( String site ) {
        return (mStore.containsKey( site ) ?  (List) mStore.get( site ) : new LinkedList());
    }
    /**
     * Add a Measurement to the store.
     *
     * @param site     the site for which the record is logged.
     * @param record   the <code>Measurement</code> record.
     */
    public void addMeasurement( String site, Measurement record ){
        List l =  ( mStore.containsKey( site ) ) ?
                                                (List) mStore.get( site ):
                                                new LinkedList();
        l.add( record );
        mStore.put( site, l );
    }


    /**
     * Sorts the records for each site.
     */
    public void sort(){
        MeasurementComparator s = new MeasurementComparator();
        for( Iterator it = mStore.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Map.Entry) it.next();
            List l = (List)entry.getValue();
            Collections.sort( l, s );
        }
    }

    /**
     * Returns textual description of the object.
     *
     * @return the textual description
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append( "{\n ");
        for( Iterator it = mStore.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Map.Entry) it.next();
            List l = (List)entry.getValue();
            sb.append( entry.getKey() ).append( " -> " );
            for( Iterator lIT = l.iterator(); lIT.hasNext(); ){
                sb.append( "\n\t");
                sb.append( lIT.next() );
                sb.append( " , ");
            }
        }
        sb.append( "\n}" );
        return sb.toString();
    }
}


/**
 * Comparator for Measurement objects that allows us to sort on time.
 *
 */
class MeasurementComparator implements Comparator{

    /**
     * Implementation of the {@link java.lang.Comparable} interface.
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object. The
     * definitions are compared by their type, and by their short ids.
     *
     * @param o1 is the object to be compared
     * @param o2 is the object to be compared with o1.
     *
     * @return a negative number, zero, or a positive number, if the
     * object compared against is less than, equals or greater than
     * this object.
     * @exception ClassCastException if the specified object's type
     * prevents it from being compared to this Object.
     */
    public int compare( Object o1, Object o2 ) {
        if ( o1 instanceof Measurement && o2 instanceof Measurement ) {
            Measurement s1 = (Measurement) o1;
            Measurement s2 = (Measurement) o2;

            return s1.getTime().compareTo( s2.getTime() );
        } else {
            throw new ClassCastException( "object is not a Space" );
        }
    }
}
