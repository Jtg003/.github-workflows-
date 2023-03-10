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


package org.griphyn.cPlanner.cluster.aggregator;

import org.griphyn.cPlanner.cluster.JobAggregator;

import org.griphyn.cPlanner.common.PegasusProperties;

import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.PegasusBag;

import org.griphyn.common.util.DynamicLoader;


/**
 * A factory class to load the appropriate JobAggregator implementations while
 * clustering jobs.
 *
 * @author Karan Vahi
 * @version $Revision$
 */

public class JobAggregatorFactory {

    /**
     * Package to prefix "just" class names with.
     */
    public static final String DEFAULT_PACKAGE_NAME =
                                             "org.griphyn.cPlanner.cluster.aggregator";


    /**
     * The name of the class in this package, that corresponds to seqexec.
     * This is required to load the correct class, even though the user
     * specifyies a class that matches on ignoring case, but not directly.
     */
    public static final String SEQ_EXEC_CLASS = "SeqExec";


    /**
     * The name of the class in this package, that corresponds to mpiexec.
     * This is required to load the correct class, even though the user
     * specifyies a class that matches on ignoring case, but not directly.
     */
    public static final String MPI_EXEC_CLASS = "MPIExec";

    /**
     * Loads the implementing class corresponding to the mode specified by the user
     * at runtime in the properties file. The properties object passed should not
     * be null.
     *
     * @param dag        the workflow that is being clustered.
     * @param bag        the bag of objects that is useful for initialization.
     *
     * @return the instance of the class implementing this interface.
     *
     * @throws JobAggregatorFactoryException that nests any error that
     *            might occur during the instantiation
     *
     * @see #DEFAULT_PACKAGE_NAME
     */
    public static JobAggregator loadInstance( ADag dag,
                                              PegasusBag bag ) {

        PegasusProperties properties = bag.getPegasusProperties();
        //sanity check
        if( properties  == null){
            throw new RuntimeException("Invalid properties passed");
        }


        return loadInstance( properties.getJobAggregator(), dag, bag );
    }


    /**
     * Loads the implementing class corresponding to the class passed.
     *
     * @param className  the name of the class that implements the mode. It is the
     *                   name of the class, not the complete name with package. That
     *                   is added by itself.
     * @param dag        the workflow that is being clustered.
     * @param bag        the bag of objects that is useful for initialization.
     *
     * @return the instance of the class implementing this interface.
     *
     * @throws JobAggregatorFactoryException that nests any error that
     *            might occur during the instantiation
     *
     * @see #DEFAULT_PACKAGE_NAME
     */
    public static JobAggregator loadInstance( String className,
                                              ADag dag,
                                              PegasusBag bag
                                               ) {

        //sanity check
        if( bag.getPegasusProperties() == null){
            throw new RuntimeException("Invalid properties passed");
        }
        if(className == null){
            throw new RuntimeException("Invalid class specified to load");
        }

        JobAggregator ja = null;
        try{
            //ensure that correct class is picked up in case
            //of mpiexec and seqexec
            if(className.equalsIgnoreCase(MPI_EXEC_CLASS)){
                className = MPI_EXEC_CLASS;
            }
            else if(className.equalsIgnoreCase(SEQ_EXEC_CLASS)){
                className = SEQ_EXEC_CLASS;
            }

            //prepend the package name if required
            className = (className.indexOf('.') == -1)?
                        //pick up from the default package
                        DEFAULT_PACKAGE_NAME + "." + className:
                        //load directly
                        className;

            //try loading the class dynamically
            DynamicLoader dl = new DynamicLoader( className);
            Object argList[] = new Object[0];
            ja = (JobAggregator) dl.instantiate(argList);

            ja.initialize( dag, bag );
        }
        catch ( Exception e ) {
            throw new JobAggregatorFactoryException("Instantiating JobAggregator ",
                                                     className, e);
        }

        return ja;
    }



}
