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


package org.griphyn.cPlanner.namespace;


import org.griphyn.cPlanner.classes.Profile;
import org.griphyn.cPlanner.common.PegasusProperties;

import org.griphyn.cPlanner.namespace.aggregator.Aggregator;
import org.griphyn.cPlanner.namespace.aggregator.MIN;
import org.griphyn.cPlanner.namespace.aggregator.MAX;
import org.griphyn.cPlanner.namespace.aggregator.Sum;
import org.griphyn.cPlanner.namespace.aggregator.Update;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;


/**
 * This helper class helps in handling the globus rsl key value pairs that
 * come through profile information for namespace Globus.
 * The information can either come in through transformation catalog, site catalog
 * or through profile tags in DAX.
 *
 * @author Karan Vahi
 * @version $Revision$
 */

public class Globus extends Namespace {

    /**
     * The name of the namespace that this class implements.
     */
    public static final String NAMESPACE_NAME = Profile.GLOBUS;

    /**
     * The table that maps the various globus profile keys to their aggregator
     * functions.
     *
     * 
     */
    public static Map mAggregatorTable;

    /**
     * The default aggregator to be used for profile aggregation, if none specified
     * in the aggregator table;
     */
    public static Aggregator mDefaultAggregator = new Update();

    /**
     * Initializer block that populates the Aggregator table just once.
     */
    static{
        mAggregatorTable = new HashMap( 5 );
        Aggregator max = new MAX();
        Aggregator sum = new Sum();

        //all the times need to be added
        mAggregatorTable.put( "maxtime", sum );
        mAggregatorTable.put( "maxcputime", sum );
        mAggregatorTable.put( "maxwalltime", sum );

        //for the memory rsl params we take max
        mAggregatorTable.put( "maxmemory", max );
        mAggregatorTable.put( "minmemory", max );
    }

    /**
     * The name of the implementing namespace. It should be one of the valid
     * namespaces always.
     *
     * @see Namespace#isNamespaceValid(String)
     */
    protected String mNamespace;


    /**
     * The default constructor.
     */
    public Globus(){
        mProfileMap = new TreeMap();
        mNamespace = NAMESPACE_NAME;
    }


    /**
     * The overloaded constructor
     *
     * @param map a possibly empty map.
     */
    public Globus(Map map){
        mProfileMap = new TreeMap(map);
        mNamespace = NAMESPACE_NAME;
    }

    /**
     * Returns the name of the namespace associated with the profile
     * implementations.
     *
     * @return the namespace name.
     * @see #NAMESPACE_NAME
     */
    public String namespaceName(){
        return mNamespace;
    }


    /**
     * Constructs a new element of the format (key=value). All the keys
     * are converted to lower case before storing.
     *
     * @param key is the left-hand-side
     * @param value is the right hand side
     */
    public void construct(String key, String value) {
	mProfileMap.put(key.toLowerCase(), value);
    }


    /**
     * Additional  method to handle the globus namespace with
     * convenience mappings. Currently supported keys are:
     *
     * <pre>
     * arguments	- not supported, clashes with Condor
     * count		- OK
     * directory	- not supported, clashes with Pegasus
     * dryRun		- OK, beware the consequences!
     * environment	- not supported, use env namespace
     * executable	- not supported, clashes with Condor
     * gramMyjob	- OK
     * hostCount	- OK
     * jobType		- OK to handle MPI jobs
     * maxCpuTime	- OK
     * maxMemory	- OK
     * maxTime		- OK
     * maxWallTime	- OK
     * minMemory	- OK
     * project		- OK
     * queue		- OK
     * stdin		- not supported, clashes with Pegasus
     * stdout		- not supported, clashes with Pegasus
     * stderr		- not supported, clashes with Pegasus
     *
     * rls		- OK: Chimera's generic extension (AOB)
     * </pre>
     *
     * @param key is the key within the globus namespace, must be lowercase!
     * @param value is the value for the given key.
     *
     * @return   MALFORMED_KEY
     *            VALID_KEY
     *            UNKNOWN_KEY
     *            NOT_PERMITTED_KEY
     */
    public int checkKey(String key, String value) {
        // sanity checks first
        int res = 0;
        
        if (key == null || key.length() < 2 ) {
            res = MALFORMED_KEY ;
            return res;
        }
        
        if( value == null || value.length() < 1 ){
            res = EMPTY_KEY;
            return res;
        }


        //before checking convert the key to lower case
        key = key.toLowerCase();

        switch (key.charAt(0)) {
            case 'a':
                if( ( key.compareTo( "arch" ) == 0 ) ){
                    res = VALID_KEY;
                }
                else if ( (key.compareTo("arguments") == 0)  ){
                    res = NOT_PERMITTED_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'c':
                if (key.compareTo("count") == 0) {
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'd':
                if (key.compareTo("directory") == 0) {
                    res = NOT_PERMITTED_KEY;
                }
                else if (key.compareTo("dryrun") == 0) {
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'e':
                if (key.compareTo("environment") == 0 ||
                    key.compareTo("executable") == 0) {
                    res = NOT_PERMITTED_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'g':
                if (key.compareTo("grammyjob") == 0) {
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'h':
                if (key.compareTo("hostcount") == 0) {
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'j':
                if (key.compareTo("jobtype") == 0) {

                    // FIXME: Gaurang?
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'm':
                if (key.compareTo("maxcputime") == 0 ||
                    key.compareTo("maxmemory") == 0 ||
                    key.compareTo("maxtime") == 0 ||
                    key.compareTo("maxwalltime") == 0 ||
                    key.compareTo("minmemory") == 0) {
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'p':
                if (key.compareTo("project") == 0) {
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'q':
                if (key.compareTo("queue") == 0) {
                    res = VALID_KEY;
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 'r':
                if (key.compareTo("rsl") == 0) {

                    // our own extension mechanism, no warnings here
                    // Note: The value IS the RSL!!!
                    new String(value);
                }
                else {
                    res = UNKNOWN_KEY;
                }
                break;

            case 's':
                if (key.compareTo("stdin") == 0 ||
                    key.compareTo("stdout") == 0 ||
                    key.compareTo("stderr") == 0) {
                    res = NOT_PERMITTED_KEY;
                }
                else {
                    res = UNKNOWN_KEY;

                }
            default:
                res = UNKNOWN_KEY;
        }

        return res;
    }


    /**
     * Merge the profiles in the namespace in a controlled manner.
     * In case of intersection, the new profile value overrides, the existing
     * profile value.
     *
     * @param profiles  the <code>Namespace</code> object containing the profiles.
     */
    public void merge( Namespace profiles ){
        //check if we are merging profiles of same type
        if (!(profiles instanceof Globus )){
            //throw an error
            throw new IllegalArgumentException( "Profiles mismatch while merging" );
        }
        String key;
        Aggregator agg;
        for ( Iterator it = profiles.getProfileKeyIterator(); it.hasNext(); ){
            key = (String)it.next();
            agg = this.aggregator( key );
            //load the appropriate aggregator to merge the profiles
            this.construct( key,
                            agg.compute( (String)get( key ), (String)profiles.get( key ), "0" ) );
        }
    }


    /**
     * It puts in the namespace specific information specified in the properties
     * file into the namespace. The name of the pool is also passed, as many of
     * the properties specified in the properties file are on a per pool basis.
     * An empty implementation for the timebeing. It is handled in the submit
     * writer.
     *
     * @param properties  the <code>PegasusProperties</code> object containing
     *                    all the properties that the user specified at various
     *                    places (like .chimerarc, properties file, command line).
     * @param pool        the pool name where the job is scheduled to run.
     */
    public void checkKeyInNS(PegasusProperties properties, String pool){

        //the time rsl's are correctly handled here.
        //the other RSL's are handled in the CodeGenerator.
        enforceMinTime( properties, "maxwalltime" );
        enforceMinTime( properties, "maxtime" );
        enforceMinTime( properties, "maxcputime" );


    }

    /**
     * Converts the contents of the map into the string that can be put in the
     * Condor file for printing.
     *
     * @return the textual description.
     */
    public String toString(){
        return convert(mProfileMap);
    }

    /**
     * Returns a copy of the current namespace object
     *
     * @return the Cloned object
     */
    public Object clone(){
       return new Globus(this.mProfileMap);
    }

    /**
     * Enforces a minimum time if specified in the properties.
     *
     * @param properties   the properties object holding the properties.
     * @param key          the RSL time key .
     */
    protected void enforceMinTime( PegasusProperties properties, String key ){
        //try to get the existing value if any
        String val = (String)this.get( key );
        int existing = ( val == null ) ? -1 : Integer.parseInt( val );

        int value = properties.getMinimumRemoteSchedulerTime( key );
        //we enforce the min value specified in properties only if it is
        //greater than the existing value
        if ( value > existing ){
            //user asked for enforcement
            this.construct( key, Integer.toString( value ) );
        }
    }


    /**
     * Returns the aggregator to be used for the profile key while merging.
     * If no aggregator is found, the then default Aggregator (Update) is used.
     *
     * @param key  the key for which the aggregator is found.
     *
     * @return the aggregator for the profile key.
     */
    protected Aggregator aggregator( String key ){
        Object aggregator = this.mAggregatorTable.get( key );
        return ( aggregator == null )? mDefaultAggregator : (Aggregator)aggregator;
    }

    /**
     * Converts a map with RSL kv-pairs into an RSL string.
     *
     * @param rsl is the RSL map to convert
     * @return the new string to use in globusrsl of Condor.
     */
    private String convert(java.util.Map rsl) {
        StringBuffer result = new StringBuffer();
        for (Iterator i = rsl.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            String value = (String) rsl.get(key);
            if (value != null && value.length() > 0) {
                if (key.compareTo("rsl") == 0) {
                    result.append(value);
                }
                else {
                    result.append('(').append(key).append('=').append(value).
                        append(')');
                }
            }
        }
        return result.toString();
    }

}


