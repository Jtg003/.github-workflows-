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


package org.griphyn.cPlanner.engine;

import edu.isi.pegasus.planner.catalog.site.classes.GridGateway;
import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;

import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.FileTransfer;
import org.griphyn.cPlanner.classes.PegasusFile;
import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PegasusBag;


import org.griphyn.common.classes.Arch;
import org.griphyn.common.classes.Os;
import org.griphyn.common.classes.SysInfo;


import edu.isi.pegasus.common.logging.LogManager;

import org.griphyn.cPlanner.selector.SiteSelector;

import org.griphyn.cPlanner.selector.site.SiteSelectorFactory;

import org.griphyn.cPlanner.selector.TransformationSelector;

import org.griphyn.cPlanner.namespace.Hints;

import org.griphyn.cPlanner.provenance.pasoa.XMLProducer;
import org.griphyn.cPlanner.provenance.pasoa.producer.XMLProducerFactory;

import org.griphyn.cPlanner.provenance.pasoa.PPS;
import org.griphyn.cPlanner.provenance.pasoa.pps.PPSFactory;


import org.griphyn.common.catalog.TransformationCatalogEntry;


import org.griphyn.common.classes.TCType;

import org.griphyn.common.catalog.transformation.Mapper;

import org.griphyn.common.util.Separator;

import org.griphyn.cPlanner.transfer.SLS;

import org.griphyn.cPlanner.transfer.sls.SLSFactory;

import java.io.File;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
/**
 * This engine calls out to the Site Selector selected by the user and maps the
 * jobs in the workflow to the execution pools.
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 * @version $Revision$
 *
 */
public class InterPoolEngine extends Engine implements Refiner {

    /**
     * ADag object corresponding to the Dag whose jobs we want to schedule.
     *
     */
    private ADag mDag;

    /**
     * Set of the execution pools which the user has specified.
     */
    private Set mExecPools;

    /**
     * Handle to the site selector.
     */
    private SiteSelector mSiteSelector;

    /**
     * The handle to the transformation selector, that ends up selecting
     * what transformations to pick up.
     */
    private TransformationSelector mTXSelector;


    /**
     * The handle to the transformation catalog mapper object that caches the
     * queries to the transformation catalog, and indexes them according to
     * lfn's. There is no purge policy in the TC Mapper, so per se it is not a
     * classic cache.
     */
    private Mapper mTCMapper;

    /**
     * The XML Producer object that records the actions.
     */
    private XMLProducer mXMLStore;

    /**
     * The handle to the SLS implementor
     */
    private SLS mSLS;

    /**
     * A boolean indicating whether to have worker node execution or not.
     */
    private boolean mWorkerNodeExecution;

    /**
     * Default constructor.
     *
     *
     * @param bag  the bag of initialization objects.
     */
    public InterPoolEngine( PegasusBag bag ) {
        super( bag  );
        mDag        = new ADag();
        mExecPools  = new java.util.HashSet();

        //initialize the transformation mapper
        mTCMapper = Mapper.loadTCMapper( mProps.getTCMapperMode(), mBag );
        mBag.add( PegasusBag.TRANSFORMATION_MAPPER, mTCMapper );
/*        
        mTCMapper   = Mapper.loadTCMapper( mProps.getTCMapperMode(), mBag );

        //intialize the bag of objects and load the site selector
        mBag = new PegasusBag();
        mBag.add( PegasusBag.PEGASUS_PROPERTIES, mProps );
        mBag.add( PegasusBag.TRANSFORMATION_CATALOG, mTCHandle );
        mBag.add( PegasusBag.TRANSFORMATION_MAPPER, mTCMapper );
        mBag.add( PegasusBag.PEGASUS_LOGMANAGER, mLogger );
*/
        mTXSelector = null;
        mXMLStore        = XMLProducerFactory.loadXMLProducer( mProps );

        mWorkerNodeExecution = mProps.executeOnWorkerNode();
        if( mWorkerNodeExecution ){
            //load SLS
            mSLS = SLSFactory.loadInstance( mBag );
        }

    }

    /**
     * Overloaded constructor.
     *
     * @param aDag      the <code>ADag</code> object corresponding to the Dag
     *                  for which we want to determine on which pools to run
     *                  the nodes of the Dag.
     * @param bag       the bag of initialization objects
     *
     */
    public InterPoolEngine( ADag aDag, PegasusBag bag ) {
        super( bag );
        mDag = aDag;
        mExecPools = (Set)mPOptions.getExecutionSites();
        mLogger.log( "List of executions sites is " + mExecPools,
                     LogManager.DEBUG_MESSAGE_LEVEL );
        
        mTCMapper = Mapper.loadTCMapper( mProps.getTCMapperMode(), mBag );
        mBag.add( PegasusBag.TRANSFORMATION_MAPPER, mTCMapper );
/*        
        //initialize the transformation mapper
        mTCMapper   = Mapper.loadTCMapper( mProps.getTCMapperMode() );

        //intialize the bag of objects and load the site selector
        mBag = new PegasusBag();
        mBag.add( PegasusBag.PEGASUS_PROPERTIES, mProps );
        mBag.add( PegasusBag.PLANNER_OPTIONS, mPOptions );
        mBag.add( PegasusBag.TRANSFORMATION_CATALOG, mTCHandle );
        mBag.add( PegasusBag.TRANSFORMATION_MAPPER, mTCMapper );
        mBag.add( PegasusBag.PEGASUS_LOGMANAGER, mLogger );
        mBag.add( PegasusBag.SITE_CATALOG, mSiteStore );
*/
        mTXSelector = null;
        mXMLStore        = XMLProducerFactory.loadXMLProducer( mProps );

        mWorkerNodeExecution = mProps.executeOnWorkerNode();
        if( mWorkerNodeExecution ){
            //load SLS
            mSLS = SLSFactory.loadInstance( mBag );
        }


    }

    /**
     * Returns the bag of intialization objects.
     *
     * @return PegasusBag
     */
    public PegasusBag getPegasusBag(){
        return mBag;
    }



    /**
     * Returns a reference to the workflow that is being refined by the refiner.
     *
     *
     * @return ADAG object.
     */
    public ADag getWorkflow(){
        return this.mDag;
    }

    /**
     * Returns a reference to the XMLProducer, that generates the XML fragment
     * capturing the actions of the refiner. This is used for provenace
     * purposes.
     *
     * @return XMLProducer
     */
    public XMLProducer getXMLProducer(){
        return this.mXMLStore;
    }


    /**
     * This is where the callout to the Partitioner should take place, that
     * partitions the workflow into clusters and sends to the site selector only
     * those list of jobs that are ready to be scheduled.
     *
     */
    public void determineSites() {
        SubInfo job;

        //at present we schedule the whole workflow at once
        List jobs = convertToList( mDag.vJobSubInfos );
        List pools = convertToList( mExecPools );

        //going through all the jobs making up the Adag, to do the physical mapping
        scheduleJobs( mDag, pools );
    }

    /**
     * It schedules a list of jobs on the execution pools by calling out to the
     * site selector specified. It is upto to the site selector to determine if
     * the job can be run on the list of sites passed.
     *
     * @param dag   the abstract workflow.
     * @param sites the list of execution sites, specified by the user.
     *
     */
    public void scheduleJobs( ADag dag, List sites ) {
        
        //before loading the site selector we need to
        //update the transformation with the hints in the 
        //DAX for the job.
        for( Iterator it = dag.jobIterator(); it.hasNext(); ){
            SubInfo job = ( SubInfo ) it.next();
            //some sanity check for hints namespace
            if( job.hints.containsKey( Hints.PFN_HINT_KEY ) &&
                !job.hints.containsKey( Hints.EXECUTION_POOL_KEY )    ){
                try {
                    //insert an entry into the transformation catalog
                    //for the mapper to pick up later on
                    TransformationCatalogEntry tcEntry = constructTCEntryFromJobHints( job );
                    mLogger.log( "Addding entry into transformation catalog " + tcEntry, LogManager.DEBUG_MESSAGE_LEVEL);
            
                    if (!mTCHandle.addTCEntry( tcEntry, false )) {
                        mLogger.log("Unable to add entry to transformation catalog " + tcEntry, LogManager.WARNING_MESSAGE_LEVEL);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException( "Exception while inserting into TC in Interpool Engine ");
                }
            }
        }
        

        mSiteSelector = SiteSelectorFactory.loadInstance( mBag );
        mSiteSelector.mapWorkflow( dag, sites );

        int i = 0;
        StringBuffer error;

        //load the PPS implementation
        PPS pps = PPSFactory.loadPPS( this.mProps );

        mXMLStore.add( "<workflow url=\"" + mPOptions.getDAX() + "\">" );

        //call the begin workflow method
        try{
            pps.beginWorkflowRefinementStep( this, PPS.REFINEMENT_SITE_SELECT, false );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception", e );
        }

        //clear the XML store
        mXMLStore.clear();


        //Iterate through the jobs and hand them to
        //the site selector if required
        String site ;
        for( Iterator it = dag.jobIterator(); it.hasNext(); i++ ){

            SubInfo job = ( SubInfo ) it.next();
            site  = job.getSiteHandle();
            mLogger.log( "Mapping Job "  + job.getName(), 
                         LogManager.DEBUG_MESSAGE_LEVEL );
            //check if the user has specified any hints in the dax

//          replaced with jobmanager-type
//            incorporateHint(job, "pfnUniverse");
            incorporateHint(job, Hints.JOBMANAGER_UNIVERSE_KEY  );
            if (incorporateHint(job, "executionPool")) {
                //i++;
                incorporateProfiles(job);
                continue;
            }

            if ( site == null ) {
                error = new StringBuffer();
                error.append( "Site Selector could not map the job " ).
                      append( job.getCompleteTCName() ).
                      append( "\nMost likely an error occured in site selector." );
                mLogger.log( error.toString(),
                            LogManager.ERROR_MESSAGE_LEVEL );
                throw new RuntimeException( error.toString() );
            }
            String jm = job.getJobManager();
            jm = ( (jm == null) || jm.length() == 0 ) ?
                null : jm;

            if ( site.length() == 0 ||
                 site.equalsIgnoreCase( SiteSelector.SITE_NOT_FOUND ) ) {
                error = new StringBuffer();
                error.append( "Site Selector (" ).append( mSiteSelector.description() ).
                      append( ") could not map job " ).append( job.getCompleteTCName() ).
                      append( " to any site" );
                mLogger.log( error.toString(), LogManager.ERROR_MESSAGE_LEVEL );
                throw new RuntimeException( error.toString() );
            }
            job.setJobManager( jm == null ?
                                          getJobManager( site, job.getUniverse() ) :
                                          jm );


            mLogger.log("Mapped job " + job.jobName + " to pool " + site,
                        LogManager.DEBUG_MESSAGE_LEVEL);
            //incorporate the profiles and
            //do transformation selection
            if ( !incorporateProfiles(job) ){
                error = new StringBuffer();
                error.append( "Profiles incorrectly incorporated for ").
                      append( job.getCompleteTCName());

               mLogger.log( error.toString(), LogManager.ERROR_MESSAGE_LEVEL );
               throw new RuntimeException( error.toString() );

            }

            //modify the jobs if required for worker node execution
            if( mWorkerNodeExecution ){
                mSLS.modifyJobForFirstLevelStaging( job,
                                                    mPOptions.getSubmitDirectory(),
                                                    mSLS.getSLSInputLFN( job ),
                                                    mSLS.getSLSOutputLFN( job )   );
            }

            //log actions as XML fragment
            try{
                logRefinerAction(job);
                pps.siteSelectionFor( job.getName(), job.getName() );
            }
            catch( Exception e ){
                throw new RuntimeException( "PASOA Exception", e );
            }

        }//end of mapping all jobs

        try{
            pps.endWorkflowRefinementStep( this );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception", e );
        }

    }

    /**
     * Constructs a TC entry object from the contents of a job. 
     * The architecture assigned to this entry is default ( INTEL32::LINUX )
     * and resource id is set to unknown.
     * 
     * @param job  the job object
     * 
     * @return constructed TransformationCatalogEntry 
     */
    private TransformationCatalogEntry constructTCEntryFromJobHints( SubInfo job ){ 
        String executable = (String) job.hints.get( Hints.PFN_HINT_KEY );
        TransformationCatalogEntry entry = new TransformationCatalogEntry();
        entry.setLogicalTransformation(job.getTXNamespace(), job.getTXName(), job.getTXVersion());
        entry.setResourceId( "unknown" );
        entry.setSysInfo( new SysInfo( Arch.INTEL64, Os.LINUX, "", "" ) );
        entry.setPhysicalTransformation( executable );
        //hack to determine whether an executable is
        //installed or static binary
        entry.setType( executable.startsWith("/") ?
                            TCType.INSTALLED : 
                            TCType.STATIC_BINARY);
                    
        return entry;
    }






    /**
     * Incorporates the profiles from the various sources into the job.
     * The profiles are incorporated in the order pool, transformation catalog,
     * and properties file, with the profiles from the properties file having
     * the highest priority.
     * It is here where the transformation selector is called to select
     * amongst the various transformations returned by the TC Mapper.
     *
     * @param job  the job into which the profiles have been incorporated.
     *
     * @return true profiles were successfully incorporated.
     *         false otherwise
     */
    private boolean incorporateProfiles(SubInfo job){
        TransformationCatalogEntry tcEntry = null;
        List tcEntries = null;
        String siteHandle = job.getSiteHandle();

        //the profile information from the pool catalog needs to be
        //assimilated into the job.
        job.updateProfiles( mSiteStore.lookup( siteHandle ).getProfiles() );


        //we now query the TCMapper only if there is no hint available
        //by the user in the DAX 3.0 .  
        if( job.getRemoteExecutable() == null || job.getRemoteExecutable().length() == 0 ){ 
        
            //query the TCMapper and get hold of all the valid TC
            //entries for that site
            tcEntries = mTCMapper.getTCList(job.namespace,job.logicalName,
                                            job.version,siteHandle);

            StringBuffer error;
            if(tcEntries != null && tcEntries.size() > 0){
                //select a tc entry calling out to
                //the transformation selector
                tcEntry = selectTCEntry(tcEntries,job,mProps.getTXSelectorMode());
                if(tcEntry == null){
                    error = new StringBuffer();
                    error.append( "Transformation selection operation for job  ").
                          append( job.getCompleteTCName() ).append(" for site " ).
                          append( job.getSiteHandle() ).append( " unsuccessful." );
                    mLogger.log( error.toString(), LogManager.ERROR_MESSAGE_LEVEL );
                    throw new RuntimeException( error.toString() );
                }
            }
            else{
                //mismatch. should be unreachable code!!!
                //as error should have been thrown in the site selector
                mLogger.log(
                    "Site selector mapped job " +
                    job.getCompleteTCName() + " to pool " +
                    job.executionPool + " for which no mapping exists in " +
                    "transformation mapper.",LogManager.FATAL_MESSAGE_LEVEL);
                return false;
            }
        }
        else{
            //create a transformation catalog entry object
            //corresponding to the executable set
            String executable = job.getRemoteExecutable();
            tcEntry = new TransformationCatalogEntry();
            tcEntry.setLogicalTransformation( job.getTXNamespace(),
                                              job.getTXName(), 
                                              job.getTXVersion() );
            tcEntry.setResourceId( job.getSiteHandle() );
            tcEntry.setPhysicalTransformation( executable );
            //hack to determine whether an executable is 
            //installed or static binary
            tcEntry.setType( executable.startsWith( "/" ) ?
                             TCType.INSTALLED:
                             TCType.STATIC_BINARY );
                
        }

        FileTransfer fTx = null;
        //something seriously wrong in this code line below.
        //Need to verify further after more runs. (Gaurang 2-7-2006).
//            tcEntry = (TransformationCatalogEntry) tcEntries.get(0);
        if(tcEntry.getType().equals( TCType.STATIC_BINARY )){
            SiteCatalogEntry site = mSiteStore.lookup( siteHandle );
            //construct a file transfer object and add it
            //as an input file to the job in the dag
            fTx = new FileTransfer( job.getStagedExecutableBaseName(),
                                                 job.jobName);
            fTx.setType(FileTransfer.EXECUTABLE_FILE);
                
            //the physical transformation points to
            //guc or the user specified transfer mechanism
            //accessible url
            fTx.addSource( tcEntry.getResourceId(),
                           tcEntry.getPhysicalTransformation());
            
            //the destination url is the working directory for
            //pool where it needs to be staged to
            //always creating a third party transfer URL
            //for the destination.
            String stagedPath =  mSiteStore.getWorkDirectory(job)
                                + File.separator + job.getStagedExecutableBaseName();

            fTx.addDestination( siteHandle,
                                site.getHeadNodeFS().selectScratchSharedFileServer().getURLPrefix() + stagedPath);

            //added in the end now after dependant executables
            //have been handled Karan May 31 2007
            //job.addInputFile(fTx);

            if( mWorkerNodeExecution ){
                //do not specify the full path as we do not know worker
                //node directory

                if( mSLS.doesCondorModifications() ){
                    //we need to take the basename of the source url
                    //as condor file transfer mech does not allow to
                    //specify destination filenames
                    job.setRemoteExecutable( new File( tcEntry.getPhysicalTransformation() ).getName() );
                }
                else{
                    //do this only when kickstart executable existance check is fixed
                    //Karan Nov 30 2007
                    //job.setRemoteExecutable(job.getStagedExecutableBaseName());
                    job.setRemoteExecutable(  stagedPath );
                }
            }
            else{
                //the jobs executable is the path to where
                //the executable is going to be staged
                job.executable = stagedPath;
            }
            //setting the job type of the job to
            //denote the executable is being staged
            job.setJobType(SubInfo.STAGED_COMPUTE_JOB);
        }
        else{
            //the executable needs to point to the physical
            //path gotten from the selected transformantion
            //entry
            job.executable = tcEntry.getPhysicalTransformation();
        }
        
        

        //the profile information from the transformation
        //catalog needs to be assimilated into the job
        //overriding the one from pool catalog.
        job.updateProfiles(tcEntry);

        //the profile information from the properties file
        //is assimilated overidding the one from transformation
        //catalog.
        job.updateProfiles(mProps);

        //handle dependant executables
        handleDependantExecutables( job );
        if( fTx != null ){
            //add the main executable back as input
            job.addInputFile( fTx);
        }

        return true;
    }

    /**
     * Handles the dependant executables that need to be staged.
     *
     * @param job SubInfo
     *
     */
    private void handleDependantExecutables( SubInfo job ){
        String siteHandle = job.getSiteHandle();
        boolean installedTX =  !(job.getJobType() == SubInfo.STAGED_COMPUTE_JOB );

        List dependantExecutables = new ArrayList();
        for (Iterator it = job.getInputFiles().iterator(); it.hasNext(); ) {
            PegasusFile input = (PegasusFile) it.next();

            if (input.getType() == PegasusFile.EXECUTABLE_FILE) {

                //if the main executable is installed, just remove the executable
                //file requirement from the input files
                if( installedTX ){
                    it.remove();
                    continue;
                }

                //query the TCMapper and get hold of all the valid TC
                //entries for that site
                String lfn[] = Separator.split( input.getLFN() );
                List tcEntries = mTCMapper.getTCList( lfn[0], lfn[1], lfn[2],
                                                     siteHandle);

                StringBuffer error;
                if (tcEntries != null && tcEntries.size() > 0) {
                    //select a tc entry calling out to
                    //the transformation selector , we only should stage
                    //never pick any installed one.
                    TransformationCatalogEntry tcEntry = selectTCEntry(tcEntries, job,
                                            "Staged" );
                    if (tcEntry == null) {
                        error = new StringBuffer();
                        error.append("Transformation selection operation for job  ").
                            append(job.getCompleteTCName()).append(" for site ").
                            append(job.getSiteHandle()).append(" unsuccessful.");
                        mLogger.log(error.toString(),
                                    LogManager.ERROR_MESSAGE_LEVEL);
                        throw new RuntimeException(error.toString());
                    }

                    //            tcEntry = (TransformationCatalogEntry) tcEntries.get(0);
                    if (tcEntry.getType().equals(TCType.STATIC_BINARY)) {
//                        SiteInfo site = mPoolHandle.getPoolEntry(siteHandle,
//                            "vanilla");
                        
                        SiteCatalogEntry site = mSiteStore.lookup( siteHandle );
                        //construct a file transfer object and add it
                        //as an input file to the job in the dag

                        //a disconnect between the basename and the input lfn.
                        String basename = SubInfo.getStagedExecutableBaseName(  lfn[0], lfn[1], lfn[2] );

                        FileTransfer fTx = new FileTransfer( basename,
                                                             job.jobName );
                        fTx.setType(FileTransfer.EXECUTABLE_FILE);
                        //the physical transformation points to
                        //guc or the user specified transfer mechanism
                        //accessible url
                        fTx.addSource(tcEntry.getResourceId(),
                                      tcEntry.getPhysicalTransformation());
                        //the destination url is the working directory for
                        //pool where it needs to be staged to
                        //always creating a third party transfer URL
                        //for the destination.
//                        String stagedPath = mPoolHandle.getExecPoolWorkDir(job)
//                            + File.separator + basename;
//                        fTx.addDestination(siteHandle,
//                                           site.getURLPrefix(false) + stagedPath);
                        
                        String stagedPath = mSiteStore.getWorkDirectory(job)
                            + File.separator + basename;
                        fTx.addDestination(siteHandle,
                                           site.getHeadNodeFS().selectScratchSharedFileServer().getURLPrefix() + stagedPath);


                        dependantExecutables.add( fTx );

                        //the jobs executable is the path to where
                        //the executable is going to be staged
                        //job.executable = stagedPath;
                        mLogger.log( "Dependant Executable " + input.getLFN() + " being staged from " +
                                     fTx.getSourceURL(), LogManager.DEBUG_MESSAGE_LEVEL );

                    }

                }
                it.remove();
            } //end of if file is exectuable
        }

        //add all the dependant executable FileTransfers back as input files
        for( Iterator it = dependantExecutables.iterator(); it.hasNext(); ){
            FileTransfer file = (FileTransfer)it.next();
            job.addInputFile( file );
        }
    }

    /**
     * Recursively ends up calling the transformation selector accordxing to
     * a chain of selections that need to be performed on the list of valid
     * transformation catalog
     *
     * @param entries    list of <code>TransformationCatalogEntry</code> objects.
     * @param job        the job.
     * @param selectors  comma separated list of selectors.
     *
     * @return the selected <code>TransformationCatalogEntry</code> object
     *         null when transformation selector is unable to select any
     *         transformation
     */
    private TransformationCatalogEntry selectTCEntry(List entries, SubInfo job,
                                                     String selectors){
        //at present there is only one selector
        //operation performed on the selector
        String selector = selectors;

        //load the transformation selector. different
        //selectors may end up being loaded for different jobs.
        mTXSelector = TransformationSelector.loadTXSelector(selector);
        entries    = mTXSelector.getTCEntry(entries);
        return (entries == null || entries.size() == 0)?
                null:
                 entries.size() > 1?
                      //call the selector again
                      selectTCEntry(entries,job,selectors):
                      (TransformationCatalogEntry) entries.get(0);
    }

    /**
     * It returns a jobmanager for the given pool.
     *
     * @param site      the name of the pool.
     * @param universe  the universe for which you need the scheduler on that
     *                  particular pool.
     *
     * @return the jobmanager for that pool and universe.
     *         null if not found.
     */
    private String getJobManager( String site, String universe) {
//        SiteInfo p = mPoolHandle.getPoolEntry( site, universe );
        SiteCatalogEntry p = mSiteStore.lookup( site );
//        JobManager jm = ( p == null )? null : p.selectJobManager( universe, true );
//        String result =  ( jm == null ) ? null : jm.getInfo( JobManager.URL );
        GridGateway jm = ( p == null )? null : p.selectGridGateway( GridGateway.JOB_TYPE.valueOf( universe ) );
        String result = ( jm == null ) ? null : jm.getContact( );


        if ( result == null) {
            StringBuffer error = new StringBuffer();
            error = new StringBuffer();
            error.append( "Could not find a jobmanager at site (").
                  append( site ).append( ") for universe " ).
                  append( universe );
            mLogger.log( error.toString(), LogManager.ERROR_MESSAGE_LEVEL );
            throw new RuntimeException( error.toString() );

        }

        return result;
    }

    /**
     * It incorporates a hint in the namespace to the job. After the hint
     * is incorporated the key is deleted from the hint namespace for that
     * job.
     *
     * @param job  the job that needs the hint to be incorporated.
     * @param key  the key in the hint namespace.
     *
     * @return true  the hint was successfully incorporated.
     *         false the hint was not set in job or was not successfully
     *               incorporated.
     */
    private boolean incorporateHint(SubInfo job, String key) {
        //sanity check
        if (key.length() == 0) {
            return false;
        }

        switch (key.charAt(0)) {
            case 'e':
                if (key.equals("executionPool") && job.hints.containsKey(key)) {
                    //user has overridden in the dax which execution Pool to use
                    job.executionPool = (String) job.hints.removeKey(
                        "executionPool");

                    incorporateHint( job, "globusScheduler");
                    incorporateHint( job, Hints.PFN_HINT_KEY );
                    return true;
                }
                break;

            case 'g':
                if (key.equals("globusScheduler")) {
                    if( job.hints.containsKey( Hints.GLOBUS_SCHEDULER_KEY ) ){
                        //user specified the jobmanager on which they want 
                        //the job to execute on
                        job.globusScheduler = (String) job.hints.removeKey("globusScheduler");
                    }
                    else{
                        //try to lookup in the site catalog
                        SiteCatalogEntry s = mSiteStore.lookup( job.getSiteHandle() );
                        if( s == null ){
                            throw new RuntimeException( "Unable to find entry for site in site catalog " + job.getSiteHandle() );
                        }
                        GridGateway gw = s.selectGridGateway( 
                                                    GridGateway.JOB_TYPE.valueOf(job.condorUniverse));
                        if( gw == null ){
                            throw new RuntimeException( 
                                    "No GridGateway specified for compute jobs for site  " + job.getSiteHandle() );
                        }
                        job.globusScheduler =  gw.getContact();
                    }
                    return true;
                }
                break;

            
            case 'p':

                if (key.equals( Hints.PFN_HINT_KEY )) {
                    job.setRemoteExecutable( 
                            job.hints.containsKey( Hints.PFN_HINT_KEY ) ?
                                (String) job.hints.removeKey( Hints.PFN_HINT_KEY ) :
                                null
                            );

                    return true;

                }
                break;
                
            case 'j':
                if (key.equals( Hints.JOBMANAGER_UNIVERSE_KEY  )) {
                 job.condorUniverse = job.hints.containsKey( Hints.JOBMANAGER_UNIVERSE_KEY  ) ?
                     (String) job.hints.removeKey( Hints.JOBMANAGER_UNIVERSE_KEY  ) :
                     job.condorUniverse;

                 return true;

             }
             break;


            default:
                break;

        }
        return false;
    }

    /**
     * Converts a Vector to a List. It only copies by reference.
     * @param v Vector
     * @return a ArrayList
     */
    public List convertToList(Vector v) {
        return  new java.util.ArrayList(v);
    }

    /**
     * Converts a Set to a List. It only copies by reference.
     * @param s Set
     * @return a ArrayList
     */
    public List convertToList(Set s) {
        return new java.util.ArrayList(s);
    }


    /**
     * Logs the action taken by the refiner on a job as a XML fragment in
     * the XML Producer.
     *
     * @param job  the <code>SubInfo</code> containing the job that was mapped
     *             to a site.
     */
    protected void logRefinerAction( SubInfo job ){
        StringBuffer sb = new StringBuffer();
        sb.append( "\t<siteselection job=\"" ).append( job.getName() ).append( "\">" );
        sb.append( "\n" ).append( "\t\t" );
        sb.append( "<logicalsite>" ).append( job.getSiteHandle() ).append( "</logicalsite>" );
        sb.append( "\n" ).append( "\t\t" );
        sb.append( "<jobmanager>" ).append( job.getJobManager() ).append( "</jobmanager>" );
        sb.append( "\n" );
        sb.append( "\t</siteselection>" );
        sb.append( "\n" );
        mXMLStore.add( sb.toString() );
    }

}
