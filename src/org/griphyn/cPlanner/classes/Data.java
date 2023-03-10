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

package org.griphyn.cPlanner.classes;

import edu.isi.pegasus.common.logging.LogManagerFactory;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import edu.isi.pegasus.common.logging.LogManager;

/**
 * This is the container for all the Data classes.
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 * @version $Revision$
 */
public abstract class Data implements Cloneable {

    /**
     * The LogManager object which is used to log all the messages.
     *
     */
    public LogManager mLogger = LogManagerFactory.loadSingletonInstance( );

    /**
     * The String which stores the message to be stored.
     */
    public String mLogMsg;


    /**
     * The default constructor.
     */
    public Data(){
        mLogMsg = new String();
    }


    /**
     * Returns the String version of the data object, which is in human readable
     * form.
     */
    public abstract String toString();


    /**
     * It converts the contents of the Vector to a String and returns it.
     * For this to work , all the objects making up the vector should be having
     * a valid toString() method.
     *
     * @param heading   The heading you want to give
     *                  to the text which is printed
     *
     * @param vector    The <code>Vector</code> whose
     *                  elements you want to print
     */
    public String vectorToString(String heading,Vector vector){
        Enumeration e = vector.elements();

        String st = "\n" + heading;
        while(e.hasMoreElements()){
            st += " " + e.nextElement().toString();
        }

        return st;
    }

    /**
     * A small helper method that displays the contents of a Set in a String.
     *
     * @param delim  The delimited between the members of the set.
     * @return  String
     */
    public String setToString(Set s, String delim){
        Iterator it = s.iterator();
        String st = new String();
        while(it.hasNext()){
            st += (String)it.next() + delim;
        }
        st = (st.length() > 0)?
             st.substring(0,st.lastIndexOf(delim)):
             st;
        return st;
    }


}
