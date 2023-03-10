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


package org.griphyn.cPlanner.visualize.spaceusage;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * A data class that associates at most three space reading with the job
 * corresponding to the GRIDSTART_PREJOB, GRIDSTART_MAINJOB and GRIDSTART_POSTJOB.
 *
 * @author Karan Vahi vahi@isi.edu
 * @version $Revision$
 */

public class JobSpace {

    /**
     * The PREJOB data index.
     */
    public static final int GRIDSTART_PREJOB_EVENT_TYPE = 0;

    /**
     * The MAINJOB data index.
     */
    public static final int GRIDSTART_MAINJOB_EVENT_TYPE = 1;

    /**
     * The POSTJOB data index.
     */
    public static final int GRIDSTART_POSTJOB_EVENT_TYPE = 2;

    /**
     * The name of the job.
     */
    private String mName;

    /**
     * The list of Space reading objects.
     */
    private List mSpaceList;

    /**
     * The default constructor.
     */
    public JobSpace(){
        mSpaceList = new ArrayList( 3 );
        for( int i = 0; i < 3; i++ ){ mSpaceList.add( null ); }
    }

    /**
     * The overloaded constructor.
     *
     * @param name  the name of the job
     */
    public JobSpace( String name ){
        this();
        mName = name;
    }

    /**
     * Adds a space record for a particular event type.
     *
     * @param space  the space record.
     * @param type   the type of job
     */
    public void addSpaceReading( Space space, int type ){
        if ( !typeInRange( type ) ){
            throw new NumberFormatException( "Event type specified is not in range " + type );
        }

        mSpaceList.set( type, space );
    }

    /**
     * Returns the space reading for a particular type of job of event.
     *
     * @param type event type.
     *
     * @return <code>Space</code> object if data exists else null
     */
    public Space getSpaceReading( int type ){
        if ( !typeInRange( type ) ){
            throw new NumberFormatException( "Event type specified is not in range " + type );
        }
        Object obj = mSpaceList.get( type );
        return ( obj == null ) ? null : (Space)obj;

    }

    /**
     * Returns the readings iterator. Values can be null.
     *
     * @return iterator to space readings.
     */
    public Iterator spaceReadingsIterator(){
        return mSpaceList.iterator();
    }

    /**
     * Returns a boolean indicating whether the event type is in range of not.
     *
     * @param type the type value
     */
    public boolean typeInRange( int type ){
        return ( type >= GRIDSTART_PREJOB_EVENT_TYPE &&
                 type <= GRIDSTART_POSTJOB_EVENT_TYPE );
    }

    /**
     * Returns a textual description of the object.
     *
     * @return description
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append( mName ).append( " ").append( mSpaceList );
        return sb.toString();
    }
}
