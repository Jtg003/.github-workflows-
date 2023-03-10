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


package org.griphyn.cPlanner.engine.createdir;


import org.griphyn.cPlanner.engine.*;
import edu.isi.pegasus.planner.catalog.site.classes.GridGateway;
import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.PegasusBag;
import org.griphyn.cPlanner.classes.SubInfo;

import org.griphyn.cPlanner.code.gridstart.GridStartFactory;

import edu.isi.pegasus.common.logging.LogManager;

import org.griphyn.cPlanner.namespace.VDS;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import org.griphyn.common.util.Separator;


/**
 * This class inserts the nodes for creating the random directories on the remote
 * execution pools. This is done when the resources have already been selected
 * to execute the jobs in the Dag. It adds a make directory node at the top level
 * of the graph, and all these concat to a single dummy job before branching
 * out to the root nodes of the original/ concrete dag so far. So we end up
 * introducing a  classic X shape at the top of the graph. Hence the name
 * HourGlass.
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 *
 * @version $Revision$
 */

public class HourGlass extends AbstractStrategy{

    
    /**
     * The name concatenating dummy job that ensures that Condor does not start
     * staging in before the directories are created.
     */
    public static final String DUMMY_CONCAT_JOB = "pegasus_concat";
    
    /**
     * The prefix assigned to the concatenating dummy job that ensures that Condor does not start
     * staging in before the directories are created.
     */
    public static final String DUMMY_CONCAT_JOB_PREFIX = "pegasus_concat_";
    
    /**
     * The transformation namespace for the create dir jobs.
     */
    public static final String TRANSFORMATION_NAMESPACE = "pegasus";

    /**
     * The logical name of the transformation that creates directories on the
     * remote execution pools.
     */
    public static final String TRANSFORMATION_NAME = "dirmanager";

    /**
     * The version number for the derivations for create dir  jobs.
     */
    public static final String TRANSFORMATION_VERSION = null;


    /**
     * The complete TC name for dirmanager.
     */
    public static final String COMPLETE_TRANSFORMATION_NAME = Separator.combine(
                                                                 TRANSFORMATION_NAMESPACE,
                                                                 TRANSFORMATION_NAME,
                                                                 TRANSFORMATION_VERSION  );


    /**
     * The derivation namespace for the create dir  jobs.
     */
    public static final String DERIVATION_NAMESPACE = "pegasus";

    /**
     * The logical name of the transformation that creates directories on the
     * remote execution pools.
     */
    public static final String DERIVATION_NAME = "dirmanager";


    /**
     * The version number for the derivations for create dir  jobs.
     */
    public static final String DERIVATION_VERSION = "1.0";
    
    /**
     * Intializes the class.
     *
     * @param bag    bag of initialization objects
     * @param impl    the implementation instance that creates create dir job
     */
    public void initialize( PegasusBag bag, Implementation impl ){
        super.initialize( bag, impl );
    }


    

    /**
     * It modifies the concrete dag passed in the constructor and adds the create
     * random directory nodes to it at the root level. These directory nodes have
     * a common child that acts as a concatenating job and ensures that Condor
     * does not start staging in the data before the directories have been added.
     * The root nodes in the unmodified dag are now chidren of this concatenating
     * dummy job.
     * @param dag   the workflow to which the nodes have to be added.
     * 
     * @return the added workflow
     */
    public ADag addCreateDirectoryNodes( ADag dag ){
        Set set = this.getCreateDirSites( dag );

        //remove the entry for the local pool
        //set.remove("local");

        String pool = null;
        String jobName = null;
        SubInfo newJob = null;
        SubInfo concatJob = null;

        //add the concat job
        if (!set.isEmpty()) {
            concatJob = makeDummyConcatJob( dag );
            introduceRootDependencies( dag, concatJob.jobName);
            dag.add(concatJob);
        }

        //for each execution pool add
        //a create directory node.
        for (Iterator it = set.iterator();it.hasNext();){
            pool = (String) it.next();
            jobName = getCreateDirJobName( dag, pool);
            newJob = mImpl.makeCreateDirJob( pool, 
                                             jobName,
                                             mSiteStore.getWorkDirectory( pool ) );
            dag.add(newJob);

            //add the relation to the concat job
            String msg = "Adding relation " + jobName + " -> " + concatJob.jobName;
            mLogger.log( msg, LogManager.DEBUG_MESSAGE_LEVEL );
            dag.addNewRelation( jobName, concatJob.jobName );
        }
        return dag;
    }

    /**
     * It traverses through the root jobs of the dag and introduces a new super
     * root node to it.
     *
     * @param dag       the DAG
     * @param newRoot   the name of the job that is the new root of the graph.
     */
    private void introduceRootDependencies( ADag dag, String newRoot) {
        Vector vRootNodes = dag.getRootNodes();
        Iterator it = vRootNodes.iterator();
        String job = null;

        while (it.hasNext()) {
            job = (String) it.next();
            dag.addNewRelation(newRoot, job);
            mLogger.log( "Adding relation " + newRoot + " -> " + job,LogManager.DEBUG_MESSAGE_LEVEL);

        }
    }

    /**
     * It creates a dummy concat job that is run at the local submit host.
     * This job should run always provided the directories were created
     * successfully.
     *
     * @param dag  the workflow
     * 
     * @return  the dummy concat job.
     */
    public SubInfo makeDummyConcatJob( ADag dag ) {

        SubInfo newJob = new SubInfo();
        List entries = null;
        String execPath =  null;

        //jobname has the dagname and index to indicate different
        //jobs for deferred planning
        newJob.jobName = getConcatJobname( dag );

        newJob.setTransformation( HourGlass.TRANSFORMATION_NAMESPACE,
                                  HourGlass.TRANSFORMATION_NAME,
                                  HourGlass.TRANSFORMATION_VERSION );
        newJob.setDerivation( HourGlass.DERIVATION_NAMESPACE,
                              HourGlass.DERIVATION_NAME,
                              HourGlass.DERIVATION_VERSION );

//        newJob.condorUniverse = Engine.REGISTRATION_UNIVERSE;
        newJob.setUniverse( GridGateway.JOB_TYPE.auxillary.toString());
        //the noop job does not get run by condor
        //even if it does, giving it the maximum
        //possible chance
        newJob.executable = "/bin/true";

        //construct noop keys
        newJob.executionPool = "local";
        newJob.jobClass = SubInfo.CREATE_DIR_JOB;
        construct(newJob,"noop_job","true");
        construct(newJob,"noop_job_exit_code","0");

        //we do not want the job to be launched
        //by kickstart, as the job is not run actually
        newJob.vdsNS.checkKeyInNS( VDS.GRIDSTART_KEY,
                                   GridStartFactory.GRIDSTART_SHORT_NAMES[GridStartFactory.NO_GRIDSTART_INDEX] );

        return newJob;

    }

    /**
     * Returns the name of the concat job
     *
     * @return name
     */
    protected String getConcatJobname( ADag dag ){
        StringBuffer sb = new StringBuffer();

        sb.append( HourGlass.DUMMY_CONCAT_JOB_PREFIX );

        //append the job prefix if specified in options at runtime
        if ( mJobPrefix != null ) { sb.append( mJobPrefix ) ;} 
        
        sb.append( dag.dagInfo.nameOfADag ).append( "_" ).
           append( dag.dagInfo.index )/*.append( "_" )*/;

        //append the job prefix if specified in options at runtime
        //if ( mJobPrefix != null ) { sb.append( mJobPrefix ); }

        //sb.append( this.DUMMY_CONCAT_JOB );

        return sb.toString();
    }

    /**
     * Constructs a condor variable in the condor profile namespace
     * associated with the job. Overrides any preexisting key values.
     *
     * @param job   contains the job description.
     * @param key   the key of the profile.
     * @param value the associated value.
     */
    private void construct(SubInfo job, String key, String value){
        job.condorVariables.checkKeyInNS(key,value);
    }

}
