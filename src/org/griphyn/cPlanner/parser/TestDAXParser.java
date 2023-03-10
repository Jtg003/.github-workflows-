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
package org.griphyn.cPlanner.parser;

import edu.isi.pegasus.common.logging.LogManager;
import edu.isi.pegasus.common.logging.LogManagerFactory;

import org.griphyn.cPlanner.classes.PegasusBag;

import org.griphyn.cPlanner.parser.dax.DAXCallbackFactory;
import org.griphyn.cPlanner.parser.dax.Callback;

import org.griphyn.cPlanner.common.PegasusProperties;

import org.griphyn.common.util.Version;

/**
 * A Test Class to demonstrate use of DAXParser and illustrates how to use
 * the Callbacks for the parser.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class TestDAXParser {

    /**
     * The main program to TestDAXParser.
     *
     * @param args
     */
    public static void main( String[] args ){
       if( args.length != 1 ){
           System.err.println( "The class takes in one argument - the path to the DAX file" );
       }

       String daxFile = args[0];

       /* get handle to the Pegasus Properties File*/
       PegasusProperties properties = PegasusProperties.nonSingletonInstance();

       /* instantiate the internal Pegasus Logger */
       //setup the logger for the default streams.
       LogManager logger = LogManagerFactory.loadSingletonInstance( properties );
       logger.logEventStart( "example.dax.parser", "planner.version", Version.instance().toString() );

       /* pass the logger and properties to Pegasus Bag*/
       PegasusBag bag = new PegasusBag();
       bag.add( PegasusBag.PEGASUS_LOGMANAGER, logger );
       bag.add( PegasusBag.PEGASUS_PROPERTIES, properties );

       
       /* instantiate the Callback via the callback Factory */
       Callback mycallback = DAXCallbackFactory.loadInstance( properties, daxFile, "org.griphyn.cPlanner.parser.dax.ExampleDAXCallback" );

       /* instantiate the DAX Parser and start parsing */
       try{
           DaxParser parser = new DaxParser( daxFile, bag, mycallback );
       }
       catch( Exception e ){
           e.printStackTrace();
       }

    }
}


