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

package org.griphyn.cPlanner.transfer.refiner;


import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.FileTransfer;
import org.griphyn.cPlanner.classes.PlannerOptions;
import org.griphyn.cPlanner.classes.NameValue;

import org.griphyn.cPlanner.common.PegasusProperties;
import edu.isi.pegasus.common.logging.LogManager;

import org.griphyn.cPlanner.engine.ReplicaCatalogBridge;

import org.griphyn.cPlanner.transfer.MultipleFTPerXFERJobRefiner;

import org.griphyn.cPlanner.provenance.pasoa.XMLProducer;
import org.griphyn.cPlanner.provenance.pasoa.producer.XMLProducerFactory;

import org.griphyn.cPlanner.provenance.pasoa.PPS;
import org.griphyn.cPlanner.provenance.pasoa.pps.PPSFactory;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import org.griphyn.cPlanner.classes.PegasusBag;
import org.griphyn.cPlanner.transfer.Implementation;
import org.griphyn.cPlanner.transfer.Refiner;

/**
 * The default transfer refiner, that implements the multiple refiner.
 * For each compute job if required it creates the following
 *          - a single stagein transfer job
 *          - a single stageout transfer job
 *          - a single interpool transfer job
 *
 * In addition this implementation prevents file clobbering while staging in data
 * to a remote site, that is shared amongst jobs.
 *
 * @author Karan Vahi
 * @version $Revision$
 */

public class Default extends MultipleFTPerXFERJobRefiner {

    /**
     * A short description of the transfer refinement.
     */
    public static final String DESCRIPTION = "Default Multiple Refinement ";

    /**
     * The string holding  the logging messages
     */
    protected String mLogMsg;


    /**
     * A Map containing information about which logical file has been
     * transferred to which site and the name of the stagein transfer node
     * that is transferring the file from the location returned from
     * the replica catalog.
     * The key for the hashmap is logicalfilename:sitehandle and the value would be
     * the name of the transfer node.
     *
     */
    protected Map mFileTable;

    /**
     * The handle to the provenance store implementation.
     */
    protected PPS mPPS;

    /**
     * The overloaded constructor.
     *
     * @param dag        the workflow to which transfer nodes need to be added.
     * @param bag   the bag of initialization objects.
     *
     */
    public Default( ADag dag,
                    PegasusBag bag ){
        super( dag, bag );
        mLogMsg = null;
        mFileTable = new TreeMap();

        //load the PPS implementation
        mPPS = PPSFactory.loadPPS( this.mProps );

        mXMLStore.add( "<workflow url=\"" + mPOptions.getDAX() + "\">" );

        //call the begin workflow method
        try{
            mPPS.beginWorkflowRefinementStep( this, PPS.REFINEMENT_STAGE, false );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception", e );
        }

        //clear the XML store
        mXMLStore.clear();

    }


    /**
     * Adds the stage in transfer nodes which transfer the input files for a job,
     * from the location returned from the replica catalog to the job's execution
     * pool.
     *
     * @param job   <code>SubInfo</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param symlinkFiles Collection of <code>FileTransfer</code> objects containing
     *                     source and destination file url's for symbolic linking
     *                     on compute site.
     */
    public  void addStageInXFERNodes( SubInfo job,
                                      Collection<FileTransfer> files,
                                      Collection<FileTransfer> symlinkFiles ){
        
        addStageInXFERNodes( job, files, Refiner.STAGE_IN_PREFIX , this.mTXStageInImplementation);
        
        addStageInXFERNodes( job, symlinkFiles, Refiner.SYMBOLIC_LINK_PREFIX, this.mTXSymbolicLinkImplementation );
        
    }
    
    /**
     * Adds the stage in transfer nodes which transfer the input files for a job,
     * from the location returned from the replica catalog to the job's execution
     * pool.
     *
     * @param job   <code>SubInfo</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param prefix the prefix to be used while constructing the transfer jobname.
     * @param implementation  the transfer implementation to use
     * 
     */
    public  void addStageInXFERNodes( SubInfo job,
                                      Collection<FileTransfer> files,
                                      String prefix,
                                      Implementation implementation ){
        String jobName = job.getName();
        String pool = job.getSiteHandle();
        int counter = 0;
        String newJobName = prefix + jobName + "_" + counter;
        String key = null;
        String msg = "Adding stagein transfer nodes for job " + jobName;
        String par = null;
        Collection stagedFiles = new ArrayList(1);
        
        //the job class is always stage in , as we dont want 
        //later modules to treat symlink jobs different from stagein jobs
        int jobClass = SubInfo.STAGE_IN_JOB;

        //to prevent duplicate dependencies
        java.util.HashSet tempSet = new java.util.HashSet();
        int staged = 0;
        for (Iterator it = files.iterator();it.hasNext();) {
            FileTransfer ft = (FileTransfer) it.next();
            String lfn = ft.getLFN();

            //get the key for this lfn and pool
            //if the key already in the table
            //then remove the entry from
            //the Vector and add a dependency
            //in the graph
            key = this.constructFileKey(lfn, pool);
            par = (String) mFileTable.get(key);
            //System.out.println("lfn " + lfn + " par " + par);
            if (par != null) {
                it.remove();

                //check if tempSet does not contain the parent
                //fix for sonal's bug
                if (tempSet.contains(par)) {
                    mLogMsg =
                        "IGNORING TO ADD rc pull relation from rc tx node: " +
                        par + " -> " + jobName +
                        " for transferring file " + lfn + " to pool " + pool;

                    mLogger.log(mLogMsg,LogManager.DEBUG_MESSAGE_LEVEL);

                } else {
                    mLogMsg = /*"Adding relation " + par + " -> " + jobName +*/
                        " For transferring file " + lfn;
                    mLogger.log(mLogMsg, LogManager.DEBUG_MESSAGE_LEVEL);
                    addRelation(par,jobName,pool,false);
                    tempSet.add(par);
                }
            } else {
                if(ft.isTransferringExecutableFile()){
                    //add to staged files for adding of
                    //set up job.
                    stagedFiles.add(ft);
                    //the staged execution file should be having the setup
                    //job as parent if it does not preserve x bit
                    if( implementation.doesPreserveXBit() ){
                        mFileTable.put(key,newJobName);
                    }
                    else{
                        mFileTable.put(key,
                                       implementation.getSetXBitJobName(jobName,staged++));
                    }
                }
                else{
                    //make a new entry into the table
                    mFileTable.put(key, newJobName);
                }
                //add the newJobName to the tempSet so that even
                //if the job has duplicate input files only one instance
                //of transfer is scheduled. This came up during collapsing
                //June 15th, 2004
                tempSet.add(newJobName);
            }
        }

        if (!files.isEmpty()) {
            mLogger.log(msg,LogManager.DEBUG_MESSAGE_LEVEL);
            msg = "Adding new stagein transfer node named " + newJobName;
            mLogger.log(msg,LogManager.DEBUG_MESSAGE_LEVEL);

            //add a direct dependency between compute job
            //and stagein job only if there is no
            //executables being staged
            if(stagedFiles.isEmpty()){
                //add the direct relation
                addRelation(newJobName, jobName, pool, true);
                SubInfo siJob = implementation.createTransferJob( job, files,null,
                                                                  newJobName,
                                                                  jobClass );
                addJob( siJob );

                //record the action in the provenance store.
                logRefinerAction( job, siJob, files , "stage-in" );
            }
            else{
                //the dependency to stage in job is added via the
                //the setup job that does the chmod
                SubInfo siJob = implementation.createTransferJob( job,files,stagedFiles,
                                                                  newJobName,
                                                                  jobClass );

                addJob( siJob );
                //record the action in the provenance store.
                logRefinerAction( job, siJob, files , "stage-in" );
            }

        }


    }


    /**
     * Adds the inter pool transfer nodes that are required for  transferring
     * the output files of the parents to the jobs execution site.
     *
     * @param job   <code>SubInfo</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     */
    public void addInterSiteTXNodes(SubInfo job,
                                    Collection files){
        String jobName = job.getName();
        int counter = 0;
        String newJobName = this.INTER_POOL_PREFIX + jobName + "_" + counter;

        String msg = "Adding inter pool nodes for job " + jobName;
        String prevParent = null;

        String lfn = null;
        String key = null;
        String par = null;
        String pool = job.getSiteHandle();

        boolean toAdd = true;

        //to prevent duplicate dependencies
        java.util.HashSet tempSet = new java.util.HashSet();

        //node construction only if there is
        //a file to transfer
        if (!files.isEmpty()) {
            mLogger.log(msg,LogManager.DEBUG_MESSAGE_LEVEL);

            for(Iterator it = files.iterator();it.hasNext();) {
                FileTransfer ft = (FileTransfer) it.next();
                lfn = ft.getLFN();
                //System.out.println("Trying to figure out for lfn " + lfn);


                //to ensure that duplicate edges
                //are not added in the graph
                //between the parent of a node and the
                //inter tx node that transfers the file
                //to the node site.

                //get the key for this lfn and pool
                //if the key already in the table
                //then remove the entry from
                //the Vector and add a dependency
                //in the graph
                key = this.constructFileKey(lfn, pool);
                par = (String) mFileTable.get(key);
                //System.out.println("\nGot Key :" + key + " Value :" + par );
                if (par != null) {
                    //transfer of this file
                    //has already been scheduled
                    //onto the pool
                    it.remove();

                    //check if tempSet does not contain the parent
                    if (tempSet.contains(par)) {
                        mLogMsg =
                            "IGNORING TO ADD interpool relation 1 from inter tx node: " +
                            par + " -> " + jobName +
                            " for transferring file " + lfn + " to pool " +
                            pool;

                        mLogger.log(mLogMsg,LogManager.DEBUG_MESSAGE_LEVEL);

                    } else {
                        mLogMsg =
                            "Adding interpool relation 1 from inter tx node: " +
                            par + " -> " + jobName +
                            " for transferring file " + lfn + " to pool " +
                            pool;
                        mLogger.log(mLogMsg, LogManager.DEBUG_MESSAGE_LEVEL);
                        addRelation(par, jobName);
                        tempSet.add(par);
                    }
                } else {
                    //make a new entry into the table
                    mFileTable.put(key, newJobName);
                    //System.out.println("\nPut Key :" + key + " Value :" + newJobName );

                    //to ensure that duplicate edges
                    //are not added in the graph
                    //between the parent of a node and the
                    //inter tx node that transfers the file
                    //to the node site.
                    if (prevParent == null ||
                        !prevParent.equalsIgnoreCase(ft.getJobName())) {

                        mLogMsg = "Adding interpool relation 2" + ft.getJobName() +
                            " -> " + newJobName +
                            " for transferring file " + lfn + " to pool " +
                            pool;
                        mLogger.log(mLogMsg,LogManager.DEBUG_MESSAGE_LEVEL);
                        addRelation(ft.getJobName(), newJobName);
                    }
                    else{
                    	mLogger.log( "NOT ADDED relation "+ ft.getJobName() +
                            " -> " + newJobName, LogManager.DEBUG_MESSAGE_LEVEL );
                    	mLogger.log( "Previous parent " + prevParent  + " " + ft.getLFN(),
                    			     LogManager.DEBUG_MESSAGE_LEVEL );
                    }

                    //we only need to add the relation between a
                    //inter tx node and a node once.
                    if (toAdd) {
                        mLogMsg = "Adding interpool relation 3" + newJobName +
                            " -> " + jobName + " for transferring file " + lfn +
                            " to pool " +
                            pool;
                        mLogger.log(mLogMsg,LogManager.DEBUG_MESSAGE_LEVEL);
                        addRelation(newJobName, jobName);
                        tempSet.add(newJobName);
                        toAdd = false;
                    }

                    //moved to the inner loop Karan Aug 26, 2009
                    //else in some cases relations between compute job
                    //and inter pool job are not added even though they shoud be
                    prevParent = ft.getJobName();
                }
            }

            //add the new job and construct it's
            //subinfo only if the vector is not
            //empty
            if (!files.isEmpty()) {
                msg = "Adding new inter pool node named " + newJobName;
                mLogger.log(msg,LogManager.DEBUG_MESSAGE_LEVEL);

                //added in make transfer node
                SubInfo interJob = mTXInterImplementation.createTransferJob(job, files,null,
                                                           newJobName,
                                                           SubInfo.INTER_POOL_JOB);

                addJob( interJob );

                this.logRefinerAction( job, interJob, files, "inter-site" );
            }

        }
        tempSet = null;

    }

    /**
     * Adds the stageout transfer nodes, that stage data to an output site
     * specified by the user.
     *
     * @param job   <code>SubInfo</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param rcb   bridge to the Replica Catalog. Used for creating registration
     *              nodes in the workflow.
     *
     */
    public void addStageOutXFERNodes(SubInfo job,
                                     Collection files,
                                     ReplicaCatalogBridge rcb ) {

        this.addStageOutXFERNodes( job, files, rcb, false);
    }

    /**
     * Adds the stageout transfer nodes, that stage data to an output site
     * specified by the user.
     *
     * @param job   <code>SubInfo</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param rcb   bridge to the Replica Catalog. Used for creating registration
     *              nodes in the workflow.
     * @param deletedLeaf to specify whether the node is being added for
     *                      a deleted node by the reduction engine or not.
     *                      default: false
     */
    public  void addStageOutXFERNodes(SubInfo job,
                                      Collection files,
                                      ReplicaCatalogBridge rcb,
                                      boolean deletedLeaf){
        String jobName = job.getName();
        int counter = 0;
        String newJobName = this.STAGE_OUT_PREFIX + jobName + "_" + counter;
        String regJob = this.REGISTER_PREFIX + jobName;

        mLogMsg = "Adding output pool nodes for job " + jobName;

        //separate the files for transfer
        //and for registration
        List txFiles = new ArrayList();
        List regFiles   = new ArrayList();
        for(Iterator it = files.iterator();it.hasNext();){
            FileTransfer ft = (FileTransfer) it.next();
            if (!ft.getTransientTransferFlag()) {
                txFiles.add(ft);
            }
            if (!ft.getTransientRegFlag()) {
                regFiles.add(ft);
            }
        }

        boolean makeTNode = !txFiles.isEmpty();
        boolean makeRNode = !regFiles.isEmpty();

        if (!files.isEmpty()) {
            mLogger.log(mLogMsg,LogManager.DEBUG_MESSAGE_LEVEL);
            mLogMsg = "Adding new output pool node named " + newJobName;
            mLogger.log(mLogMsg,LogManager.DEBUG_MESSAGE_LEVEL);

            if (makeTNode) {
                //added in make transfer node
                //mDag.addNewJob(newJobName);
                SubInfo soJob = mTXStageOutImplementation.createTransferJob(job, txFiles,null,
                                                                   newJobName,
                                                                   SubInfo.STAGE_OUT_JOB);
                addJob( soJob );
                if (!deletedLeaf) {
                    addRelation(jobName, newJobName);
                }
                if (makeRNode) {
                    addRelation(newJobName, regJob);
                }

                //log the refiner action
                this.logRefinerAction( job, soJob, txFiles, "stage-out" );
            }
            else if (!makeTNode && makeRNode) {
                addRelation(jobName, regJob);

            }
            if (makeRNode) {
                //call to make the reg subinfo
                //added in make registration node
                addJob(createRegistrationJob( regJob, job, regFiles, rcb ));
            }

        }


    }

    /**
     * Creates the registration jobs, which registers the materialized files on
     * the output site in the Replica Catalog.
     *
     * @param regJobName  The name of the job which registers the files in the
     *                    Replica Mechanism.
     * @param job         The job whose output files are to be registered in the
     *                    Replica Mechanism.
     * @param files       Collection of <code>FileTransfer</code> objects containing
     *                    the information about source and destURL's.
     * @param rcb   bridge to the Replica Catalog. Used for creating registration
     *              nodes in the workflow.
     *
     *
     * @return the registration job.
     */
    protected SubInfo createRegistrationJob(String regJobName,
                                            SubInfo job,
                                            Collection files,
                                            ReplicaCatalogBridge rcb ) {

        SubInfo regJob = rcb.makeRCRegNode( regJobName, job, files );

        //log the registration action for provenance purposes
        StringBuffer sb = new StringBuffer();
        String indent = "\t";
        sb.append( indent );
        sb.append( "<register job=\"" ).append( regJobName ).append( "\"> ");
        sb.append( "\n" );

        //traverse through all the files
        NameValue dest;
        String newIndent = indent + "\t";
        for( Iterator it = files.iterator(); it.hasNext(); ){
            FileTransfer ft = (FileTransfer)it.next();
            dest   = ft.getDestURL();
            sb.append( newIndent );
            sb.append( "<file " );
            appendAttribute( sb, "lfn", ft.getLFN() );
            appendAttribute( sb, "site", dest.getKey() );
            sb.append( ">" );
            sb.append( "\n" );
            sb.append( newIndent ).append( indent );
            sb.append( dest.getValue() );
            sb.append( "\n" );
            sb.append( newIndent );
            sb.append( "</file>" ).append( "\n" );
        }
        sb.append( indent );
        sb.append( "</register>" ).append( "\n" );

        //log the graph relationship
        String parent = job.getName ();
        String child = regJob.getName();

        sb.append( indent );
        sb.append( "<child " );
        appendAttribute( sb, "ref", child );
        sb.append( ">" ).append( "\n" );

        sb.append( newIndent );
        sb.append( "<parent " );
        appendAttribute( sb, "ref", parent );
        sb.append( "/>" ).append( "\n" );

        sb.append( indent );
        sb.append( "</child>" ).append( "\n" );

        mXMLStore.add( sb.toString() );

        //log the action for creating the relationship assertions
        try{
            mPPS.registrationIntroducedFor( regJob.getName(),job.getName() );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception while logging relationship assertion for registration",
                                         e );
        }



        return regJob;
    }


    /**
     * Signals that the traversal of the workflow is done. It signals to the
     * Provenace Store, that refinement is complete.
     */
    public void done(){

        try{
            mPPS.endWorkflowRefinementStep( this );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception", e );
        }


    }




    /**
     * Add a new job to the workflow being refined.
     *
     * @param job  the job to be added.
     */
    public void addJob(SubInfo job){
        mDAG.add(job);
    }

    /**
     * Adds a new relation to the workflow being refiner.
     *
     * @param parent    the jobname of the parent node of the edge.
     * @param child     the jobname of the child node of the edge.
     */
    public void addRelation(String parent,
                            String child){
        mLogger.log("Adding relation " + parent + " -> " + child,
                    LogManager.DEBUG_MESSAGE_LEVEL);
        
        mDAG.addNewRelation(parent,child);

    }


    /**
     * Adds a new relation to the workflow. In the case when the parent is a
     * transfer job that is added, the parentNew should be set only the first
     * time a relation is added. For subsequent compute jobs that maybe
     * dependant on this, it needs to be set to false.
     *
     * @param parent    the jobname of the parent node of the edge.
     * @param child     the jobname of the child node of the edge.
     * @param site      the execution pool where the transfer node is to be run.
     * @param parentNew the parent node being added, is the new transfer job
     *                  and is being called for the first time.
     */
    public void addRelation(String parent,
                            String child,
                            String site,
                            boolean parentNew){
        mLogger.log("Adding relation " + parent + " -> " + child,
                    LogManager.DEBUG_MESSAGE_LEVEL);
        mDAG.addNewRelation(parent,child);

    }


    /**
     * Returns a textual description of the transfer mode.
     *
     * @return a short textual description
     */
    public  String getDescription(){
        return this.DESCRIPTION;
    }

    /**
     * Records the refiner action into the Provenace Store as a XML fragment.
     *
     * @param computeJob   the compute job.
     * @param txJob        the associated transfer job.
     * @param files        list of <code>FileTransfer</code> objects containing file transfers.
     * @param type         the type of transfer job
     */
    protected void logRefinerAction( SubInfo computeJob, SubInfo txJob, Collection files , String type ){
        StringBuffer sb = new StringBuffer();
        String indent = "\t";
        sb.append( indent );
        sb.append( "<transfer job=\"" ).append( txJob.getName() ).append( "\" ").
           append( "type=\"" ).append( type ).append( "\">" );
        sb.append( "\n" );

        //traverse through all the files
        NameValue source;
        NameValue dest;
        String newIndent = indent + "\t";
        for( Iterator it = files.iterator(); it.hasNext(); ){
            FileTransfer ft = (FileTransfer)it.next();
            source = ft.getSourceURL();
            dest   = ft.getDestURL();
            sb.append( newIndent );
            sb.append( "<from " );
            appendAttribute( sb, "site", source.getKey() );
            appendAttribute( sb, "lfn", ft.getLFN() );
            appendAttribute( sb, "url", source.getValue() );
            sb.append( "/>" );
            sb.append( "\n" );

            sb.append( newIndent );
            sb.append( "<to " );
            appendAttribute( sb, "site", dest.getKey() );
            appendAttribute( sb, "lfn", ft.getLFN() );
            appendAttribute( sb, "url", dest.getValue() );
            sb.append( "/>" );
            sb.append( "\n" );
        }
        sb.append( indent );
        sb.append( "</transfer>" );
        sb.append( "\n" );

        //log the graph relationship
        String parent = ( txJob.getJobType() == SubInfo.STAGE_IN_JOB )?
                  txJob.getName():
                  computeJob.getName();

        String child = ( txJob.getJobType() == SubInfo.STAGE_IN_JOB )?
                        computeJob.getName():
                        txJob.getName();

        sb.append( indent );
        sb.append( "<child " );
        appendAttribute( sb, "ref", child );
        sb.append( ">" ).append( "\n" );

        sb.append( newIndent );
        sb.append( "<parent " );
        appendAttribute( sb, "ref", parent );
        sb.append( "/>" ).append( "\n" );

        sb.append( indent );
        sb.append( "</child>" ).append( "\n" );


        //log the action for creating the relationship assertions
        try{
            List stagingNodes = new java.util.ArrayList(1);
            stagingNodes.add(txJob.getName());
            mPPS.stagingIntroducedFor(stagingNodes, computeJob.getName());
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception while logging relationship assertion for staging ",
                                         e );
        }

        mXMLStore.add( sb.toString() );

    }

    /**
     * Appends an xml attribute to the xml feed.
     *
     * @param xmlFeed  the xmlFeed to which xml is being written
     * @param key   the attribute key
     * @param value the attribute value
     */
    protected void appendAttribute( StringBuffer xmlFeed, String key, String value ){
        xmlFeed.append( key ).append( "=" ).append( "\"" ).append( value ).
                append( "\" " );
    }



    /**
     * Constructs the key for an entry to the file table. The key returned
     * is lfn:siteHandle
     *
     * @param lfn         the logical filename of the file that has to be
     *                    transferred.
     * @param siteHandle  the name of the site to which the file is being
     *                    transferred.
     *
     * @return      the key for the entry to be  made in the filetable.
     */
    protected String constructFileKey(String lfn, String siteHandle) {
        StringBuffer sb = new StringBuffer();
        sb.append(lfn).append(":").append(siteHandle);

        return sb.toString();
    }


}
