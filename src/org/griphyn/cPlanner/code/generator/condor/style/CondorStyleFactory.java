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

package org.griphyn.cPlanner.code.generator.condor.style;


import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;

import org.griphyn.cPlanner.code.generator.condor.CondorStyle;

import org.griphyn.cPlanner.classes.SubInfo;

import org.griphyn.cPlanner.common.PegasusProperties;


import org.griphyn.cPlanner.namespace.VDS;

import org.griphyn.common.util.DynamicLoader;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.io.IOException;

/**
 * A factory class to load the appropriate type of Condor Style impelementations.
 * This factory class is different from other factories, in the sense that it
 * must be instantiated first and intialized first before calling out to any
 * of the Factory methods.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class CondorStyleFactory {

    /**
     * The default package where the all the implementing classes are supposed to
     * reside.
     */
    public static final String DEFAULT_PACKAGE_NAME =
                                          "org.griphyn.cPlanner.code.generator.condor.style";
    //

    /**
     * The name of the class implementing the Condor Style.
     */
    private static final String CONDOR_STYLE_IMPLEMENTING_CLASS = "Condor";

    /**
     * The name of the class implementing the Condor GlideIN Style.
     */
    private static final String GLIDEIN_STYLE_IMPLEMENTING_CLASS = "CondorGlideIN";

    /**
     * The name of the class implementing the CondorG Style.
     */
    private static final String GLOBUS_STYLE_IMPLEMENTING_CLASS = "CondorG";
    
    /**
     * The name of the class implementing the CondorC Style.
     */
    private static final String CONDORC_STYLE_IMPLEMENTING_CLASS = "CondorC";

    /**
     * The name of the class implementing the CondorG Style.
     */
    private static final String GLITE_STYLE_IMPLEMENTING_CLASS = "GLite";

    
    /**
     * A table that maps, VDS style keys to the names of the corresponding classes
     * implementing the CondorStyle interface.
     */
    private static Map mImplementingClassNameTable;


    /**
     * A table that maps, VDS style keys to appropriate classes implementing the
     * CondorStyle interface
     */
    private  Map mImplementingClassTable ;

    /**
     * A boolean indicating that the factory has been initialized.
     */
    private boolean mInitialized;


    /**
     * The default constructor.
     */
    public CondorStyleFactory(){
        mImplementingClassTable = new HashMap(3);
        mInitialized = false;
    }

    /**
     * Initializes the Factory. Loads all the implementations just once.
     *
     * @param properties  the <code>PegasusProperties</code> object containing all
     *                    the properties required by Pegasus.
     * @param siteStore   the handle to the SiteCatalog Store being used.
     *
     * @throws CondorStyleFactoryException that nests any error that
     *            might occur during the instantiation of the implementation.
     */
    public void initialize( PegasusProperties properties,
                            SiteStore siteStore ) throws CondorStyleFactoryException{

        //load all the implementations that correspond to the VDS style keys
        for( Iterator it = this.implementingClassNameTable().entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Map.Entry) it.next();
            String style    = (String)entry.getKey();
            String className= (String)entry.getValue();

            //load via reflection. not required in this case though
            put( style, this.loadInstance( properties, siteStore, className ));
        }

        //we have successfully loaded all implementations
        mInitialized = true;
    }


    /**
     * This method loads the appropriate implementing CondorStyle as specified
     * by the user at runtime. The CondorStyle is initialized and returned.
     *
     * @param job         the job for which the corresponding style is required.
     *
     * @throws CondorStyleFactoryException that nests any error that
     *            might occur during the instantiation of the implementation.
     */
    public CondorStyle loadInstance( SubInfo job )
                                            throws CondorStyleFactoryException{

        //sanity checks first
        if( !mInitialized ){
            throw new CondorStyleFactoryException(
                "CondorStyleFactory needs to be initialized first before using" );
        }
        String defaultStyle = job.getSiteHandle().equalsIgnoreCase( "local" )?
                              //jobs scheduled on local site have
                              //default style as condor
                              VDS.CONDOR_STYLE:
                              VDS.GLOBUS_STYLE;

        String style = job.vdsNS.containsKey( VDS.STYLE_KEY )?
                       (String)job.vdsNS.get( VDS.STYLE_KEY ):
                       defaultStyle;

        //need to check if the style isvalid or not
        //missing for now.

        //update the job with style determined
        job.vdsNS.construct( VDS.STYLE_KEY, style );

        //now just load from the implementing classes
        Object cs = this.get( style );
        if ( cs == null ) {
            throw new CondorStyleFactoryException( "Unsupported style " + style);
        }

        return (CondorStyle)cs;
    }

    /**
     * This method loads the appropriate Condor Style using reflection.
     *
     *
     * @param properties  the <code>PegasusProperties</code> object containing all
     *                    the properties required by Pegasus.
     * @param siteStore   the handle to the SiteCatalog Store being used.
     * @param className  the name of the implementing class.
     *
     * @return the instance of the class implementing this interface.
     *
     * @throws CondorStyleFactoryException that nests any error that
     *            might occur during the instantiation of the implementation.
     *
     * @see #DEFAULT_PACKAGE_NAME
     */
    private  CondorStyle loadInstance( PegasusProperties properties,
                                       SiteStore siteStore,
                                       String className )
                                              throws CondorStyleFactoryException{

        //sanity check
        if (properties == null) {
            throw new RuntimeException( "Invalid properties passed" );
        }
        if (className == null) {
            throw new RuntimeException( "Invalid className specified" );
        }

        //prepend the package name if classname is actually just a basename
        className = (className.indexOf('.') == -1) ?
            //pick up from the default package
            DEFAULT_PACKAGE_NAME + "." + className :
            //load directly
            className;

        //try loading the class dynamically
        CondorStyle cs = null;
        try {
            DynamicLoader dl = new DynamicLoader( className );
            cs = (CondorStyle) dl.instantiate( new Object[0] );
            //initialize the loaded condor style
            cs.initialize( properties, siteStore );
        }
        catch (Exception e) {
            throw new CondorStyleFactoryException( "Instantiating Condor Style ",
                                                   className,
                                                   e);
        }

        return cs;
    }

    /**
     * Returns the implementation from the implementing class table.
     *
     * @param style           the VDS style
     *
     * @return implementation  the class implementing that style, else null
     */
    private Object get( String style ){
        return mImplementingClassTable.get( style);
    }


    /**
     * Inserts an entry into the implementing class table.
     *
     * @param style           the VDS style
     * @param implementation  the class implementing that style.
     */
    private void put( String style, Object implementation){
        mImplementingClassTable.put( style, implementation );
    }


    /**
     * Returns a table that maps, the VDS style keys to the names of implementing
     * classes.
     *
     * @return a Map indexed by VDS styles, and values as names of implementing
     *         classes.
     */
    private static Map implementingClassNameTable(){
        if( mImplementingClassNameTable == null ){
            mImplementingClassNameTable = new HashMap(3);
            mImplementingClassNameTable.put( VDS.CONDOR_STYLE, CONDOR_STYLE_IMPLEMENTING_CLASS);
            mImplementingClassNameTable.put( VDS.GLIDEIN_STYLE, GLIDEIN_STYLE_IMPLEMENTING_CLASS);
            mImplementingClassNameTable.put( VDS.GLOBUS_STYLE, GLOBUS_STYLE_IMPLEMENTING_CLASS);
            mImplementingClassNameTable.put( VDS.GLITE_STYLE, GLITE_STYLE_IMPLEMENTING_CLASS);
            mImplementingClassNameTable.put( VDS.CONDORC_STYLE, CONDORC_STYLE_IMPLEMENTING_CLASS );
        }
        return mImplementingClassNameTable;
    }

}
