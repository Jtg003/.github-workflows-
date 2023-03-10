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


package org.griphyn.cPlanner.selector.site.heft;


import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PegasusBag;

import org.griphyn.cPlanner.common.PegasusProperties;
import edu.isi.pegasus.common.logging.LogManager;

import org.griphyn.cPlanner.partitioner.graph.Graph;
import org.griphyn.cPlanner.partitioner.graph.GraphNode;
import org.griphyn.cPlanner.partitioner.graph.Adapter;
import org.griphyn.cPlanner.partitioner.graph.Bag;

import org.griphyn.common.catalog.TransformationCatalog;
import org.griphyn.common.catalog.TransformationCatalogEntry;

import org.griphyn.common.catalog.transformation.Mapper;
import org.griphyn.common.catalog.transformation.Windward;


import org.griphyn.cPlanner.namespace.VDS;

import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;
import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;
import edu.isi.pegasus.planner.catalog.site.classes.GridGateway;

import edu.isi.ikcap.workflows.ac.ProcessCatalog;
import edu.isi.ikcap.workflows.ac.classes.TransformationCharacteristics;

import edu.isi.ikcap.workflows.sr.util.PropertiesHelper;

import edu.isi.ikcap.workflows.sr.util.WorkflowGenerationProvenanceCatalog;


import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;
import java.util.Properties;
import org.griphyn.cPlanner.classes.Profile;


/**
 * The HEFT based site selector. The runtime for the job in seconds is picked
 * from the pegasus profile key runtime in the transformation catalog for a
 * transformation.
 *
 * The data communication costs between jobs if scheduled on different sites
 * is assumed to be fixed. Later on if required, the ability to specify this
 * value will be exposed via properties.
 *
 * The number of processors in a site is picked by the attribute idle-nodes
 * associated with the vanilla jobmanager for a site in the site catalog.
 *
 * There are two important differences with the algorithm cited in the
 * HEFT paper.
 * <pre>
 *    - Our implementation uses downward ranks instead of the upward ranks as
 *      mentioned in the paper. The formulas have been updated accordingly.
 *
 *    - During the processor selection phase, we do the simple selection and
 *      not follow the insertion based approach.
 * </pre>
 *
 * @author Karan Vahi
 * @version $Revision$
 *
 * @see #AVERAGE_BANDWIDTH
 * @see #RUNTIME_PROFILE_KEY
 * @see #DEFAULT_NUMBER_OF_FREE_NODES
 * @see #AVERAGE_DATA_SIZE_BETWEEN_JOBS
 * @see org.griphyn.cPlanner.classes.JobManager#IDLE_NODES
 *
 */
public class Algorithm {


    /**
     * The pegasus profile key that gives us the expected runtime.
     */
    public static final String RUNTIME_PROFILE_KEY = VDS.RUNTIME_KEY;

    /**
     * The property that designates which Process catalog impl to pick up.
     */
    public static final String PROCESS_CATALOG_IMPL_PROPERTY = "pegasus.catalog.transformation.windward";


    /**
     * The average bandwidth between the sites. In mega bytes/per second.
     */
    public static final float AVERAGE_BANDWIDTH = 5;

    /**
     * The average data that is transferred in between 2 jobs in the workflow.
     * In megabytes.
     */
    public static final float AVERAGE_DATA_SIZE_BETWEEN_JOBS = 2;

    /**
     * The default number of nodes that are associated with a site if not found
     * in the site catalog.
     */
    public static final int DEFAULT_NUMBER_OF_FREE_NODES = 10;


    /**
     * The maximum finish time possible for a job.
     */
    public static final long MAXIMUM_FINISH_TIME = Long.MAX_VALUE;

    /**
     * The average communication cost between nodes.
     */
    private float mAverageCommunicationCost;

    /**
     * The workflow in the graph format, that needs to be scheduled.
     */
    private Graph mWorkflow;

    /**
     * Handle to the site catalog.
     */
//    private PoolInfoProvider mSiteHandle;
    private SiteStore mSiteStore;

    /**
     * The list of sites where the workflow can run.
     */
    private List mSites;

    /**
     * Map containing the number of free nodes for each site. The key is the site
     * name, and value is a <code>Site</code> object.
     */
    private Map mSiteMap;

    /**
     * Handle to the TCMapper.
     */
    protected Mapper mTCMapper;

    /**
     * The handle to the LogManager
     */
    private LogManager mLogger;

    /**
     * The handle to the properties.
     */
    private PegasusProperties mProps;

    //TANGRAM related variables
    /**
     * The handle to the workflow provenance catalog
     */
    private WorkflowGenerationProvenanceCatalog mWGPC;

    /**
     * The request id associated with the DAX.
     */
    private String mRequestID;

    /**
     * The label of the workflow.
     */
    private String mLabel;

    /**
     * The handle to the Process Catalog.
     */
    private ProcessCatalog mProcessCatalog;

    /**
     * The handle to the transformation catalog.
     */
    private TransformationCatalog mTCHandle;

    /**
     * The default constructor.
     *
     * @param bag  the bag of Pegasus related objects.
     */
    public Algorithm(  PegasusBag bag ) {
        mProps      = ( PegasusProperties ) bag.get( PegasusBag.PEGASUS_PROPERTIES );
        mRequestID  = mProps.getWingsRequestID();
        mTCHandle   = ( TransformationCatalog )bag.get( PegasusBag.TRANSFORMATION_CATALOG );
        mTCMapper   = ( Mapper )bag.get( PegasusBag.TRANSFORMATION_MAPPER );
        mLogger     = ( LogManager )bag.get( PegasusBag.PEGASUS_LOGMANAGER );
//        mSiteHandle = ( PoolInfoProvider )bag.get( PegasusBag.SITE_CATALOG );
        mSiteStore = bag.getHandleToSiteStore();
        mAverageCommunicationCost = (this.AVERAGE_BANDWIDTH / this.AVERAGE_DATA_SIZE_BETWEEN_JOBS);

        mProcessCatalog = this.loadProcessCatalog( mProps.getProperty( this.PROCESS_CATALOG_IMPL_PROPERTY ),
                                                   mProps.matchingSubset( this.PROCESS_CATALOG_IMPL_PROPERTY, false )
                                                   );

        //to figure out a way to insantiate SWF
        //Varun needs to write out a factory
        mWGPC = new WorkflowGenerationProvenanceCatalog( true );
    }


    /**
     * Schedules the workflow using the heft.
     *
     * @param dag   the <code>ADag</code> object containing the abstract workflow
     *              that needs to be mapped.
     * @param sites the list of candidate sites where the workflow can potentially
     *              execute.
     */
    public void schedule( ADag dag , List sites ){
        //metadata about the DAG needs to go to Graph object
        //mLabel     = dag.getLabel();

        //convert the dag into a graph representation
        schedule( Adapter.convert( dag ), sites, dag.getLabel() );
    }



    /**
     * Load the process catalog, only if it is determined that the Transformation
     * Catalog description is the windward one.
     *
     * @param type  the type of process catalog
     * @param props contains all necessary data to establish the link.
     *
     * @return true if connected now, or false to indicate a failure.
     */
    protected ProcessCatalog loadProcessCatalog( String type, Properties props ) {
        ProcessCatalog result = null;

        //only load process catalog if TC implementation loaded is of type windward.
        if( ! (mTCHandle instanceof Windward ) ) {
            return result;
        }

        //figure out how to specify via properties
        try{
            result = PropertiesHelper.getPCFactory().getPC(
				PropertiesHelper.getDCDomain(), 
				PropertiesHelper.getPCDomain(), null);
            
            //String requestID = mProps.getWingsRequestID();
            if( mRequestID == null ){
                throw new RuntimeException( "Specify the request id by specifying pegasus.wings.request.id property" );
            }
            result.setRequestId( mRequestID );
		
        }catch( Exception e ){
            mLogger.log( "Unable to connect to process catalog " + e,
                         LogManager.DEBUG_MESSAGE_LEVEL );
        }
        return result;
    }


    /**
     * Schedules the workflow according to the HEFT algorithm.
     *
     * @param workflow  the workflow that has to be scheduled.
     * @param sites the list of candidate sites where the workflow can potentially
     *              execute.
     * @param label  the label of the workflow
     *
     */
    public void schedule( Graph workflow , List sites, String label ){
        mLabel = label;
        mWorkflow = workflow;
        populateSiteMap( sites );

        //compute weighted execution times for each job
        for( Iterator it = workflow.nodeIterator(); it.hasNext(); ){
            GraphNode node = ( GraphNode )it.next();
            SubInfo job    = (SubInfo)node.getContent();

            //add the heft bag to a node
            Float averageComputeTime = new Float( calculateAverageComputeTime( job ) );
            HeftBag b = new HeftBag();
            b.add( HeftBag.AVG_COMPUTE_TIME, averageComputeTime );
            node.setBag( b );

            mLogger.log( "Average Compute Time " + node.getID() + " is " + averageComputeTime,
                         LogManager.DEBUG_MESSAGE_LEVEL );

        }

        //add a dummy root
        Bag bag;
        GraphNode dummyRoot = new GraphNode( "dummy", "dummy" );
        workflow.addRoot( dummyRoot );
        bag = new HeftBag();
        //downward rank for the root is set to 0
        bag.add( HeftBag.DOWNWARD_RANK, new Float( 0 ) );
        dummyRoot.setBag( bag );

        //do a breadth first traversal and compute the downward ranks
        Iterator it = workflow.iterator();
        dummyRoot = ( GraphNode )it.next(); //we have the dummy root
        Float drank;
        //the dummy root has a downward rank of 0
        dummyRoot.getBag().add( HeftBag.DOWNWARD_RANK, new Float( 0 ) );
        //stores the nodes in sorted ascending order
        List sortedNodes = new LinkedList();
        while ( it.hasNext() ){
            GraphNode node = ( GraphNode ) it.next();
            drank = new Float( computeDownwardRank( node ) );
            bag = node.getBag();
            bag.add( HeftBag.DOWNWARD_RANK , drank );
            sortedNodes.add( node );
            mLogger.log( "Downward rank for node " + node.getID() + " is " + drank,
                         LogManager.DEBUG_MESSAGE_LEVEL );
        }

        //sort the node
        Collections.sort( sortedNodes, new HeftGraphNodeComparator() );


        //the start time and end time for the dummy root is 0
        dummyRoot.getBag().add( HeftBag.ACTUAL_START_TIME, new Long( 0 ) );
        dummyRoot.getBag().add( HeftBag.ACTUAL_FINISH_TIME, new Long( 0 ) );

        //schedule out the sorted order of the nodes
        for( it = sortedNodes.iterator(); it.hasNext(); ){
            GraphNode current = (GraphNode) it.next();
            bag           = current.getBag();
            mLogger.log("Scheduling node " + current.getID(),
                        LogManager.DEBUG_MESSAGE_LEVEL);

            //figure out the sites where a job can run
            SubInfo job = (SubInfo) current.getContent();
            List runnableSites = mTCMapper.getSiteList( job.getTXNamespace(),
                                                        job.getTXName(),
                                                        job.getTXVersion(),
                                                        mSites);

            //for each runnable site get the estimated finish time
            //and schedule job on site that minimizes the finish time
            String site;
            long est_result[ ];
            long result[] = new long[ 2 ];
            result [ 1 ] = this.MAXIMUM_FINISH_TIME;
            for( Iterator rit = runnableSites.iterator(); rit.hasNext(); ){
                site = (String) rit.next();
                est_result = calculateEstimatedStartAndFinishTime( current, site );

                //if existing EFT is greater than the returned EFT
                //set existing EFT to the returned EFT
                if( result[ 1 ] > est_result[ 1 ] ){
                    result[ 0 ] = est_result[ 0 ];
                    result[ 1 ] = est_result[ 1 ];
                    //tentatively schedule the job for that site
                    bag.add( HeftBag.SCHEDULED_SITE , site );
                }
            }

            //update the site selected with the job
            bag.add( HeftBag.ACTUAL_START_TIME, new Long( result[ 0 ] ));
            bag.add( HeftBag.ACTUAL_FINISH_TIME, new Long( result[ 1 ] ) );
            site = (String)bag.get( HeftBag.SCHEDULED_SITE );
            scheduleJob( site,
                         result[ 0 ],
                         result[ 1 ]  );

            //log the information
            StringBuffer sb = new StringBuffer();
            sb.append( "Scheduled job " ).append( current.getID() ).
               append( " to site " ).append( site ).
               append( " with from  ").append( result[ 0 ] ).
               append( " till " ).append( result[ 1 ] );

            mLogger.log( sb.toString(), LogManager.DEBUG_MESSAGE_LEVEL );
        }//end of going through all the sorted nodes

        //remove the dummy root
        mWorkflow.remove( dummyRoot.getID() );
    }


    /**
     * Returns the makespan of the scheduled workflow. It is maximum of the
     * actual finish times for the leaves of the scheduled workflow.
     *
     * @return long  the makespan of the workflow.
     */
    public long getMakespan( ){
        long result = -1;

        //compute the maximum of the actual end times of leaves
        for( Iterator it = mWorkflow.getLeaves().iterator(); it.hasNext() ; ){
            GraphNode node = ( GraphNode )it.next();
            Long endTime   = ( Long ) node.getBag().get( HeftBag.ACTUAL_FINISH_TIME );
            //sanity check
            if( endTime == null ){
                throw new RuntimeException( "Looks like the leave node is unscheduled " +  node.getID());
            }
            if( endTime > result ){
                result = endTime;
            }
        }

        return result;
    }


    /**
     * Estimates the start and finish time of a job on a site.
     *
     * @param node   the node that is being scheduled
     * @param site  the site for which the finish time is reqd.
     *
     * @return  long[0] the estimated start time.
     *          long[1] the estimated finish time.
     */
    protected long[] calculateEstimatedStartAndFinishTime( GraphNode node, String site ){

        SubInfo job = ( SubInfo )node.getContent();
        long[] result = new long[2];

        //calculate the ready time for the job
        //that is time by which all the data needed
        //by the job has reached the site.
        long readyTime = 0;
        for( Iterator it = node.getParents().iterator(); it.hasNext(); ){
            GraphNode parent = ( GraphNode )it.next();
            long current = 0;
            //add the parent finish time to current
            current += (Long)parent.getBag().get( HeftBag.ACTUAL_FINISH_TIME );

            //if the parent was scheduled on another site
            //add the average data transfer time.
            if( !parent.getBag().get( HeftBag.SCHEDULED_SITE ).equals( site ) ){
                current += this.mAverageCommunicationCost;
            }

            if ( current > readyTime ){
                //ready time is maximum of all currents
                readyTime = current;
            }
        }

        //the estimated start time is the maximum
        //of the ready time and available time of the site
        //using non insertion based policy for time being
        result[ 0 ] = getAvailableTime( site , readyTime );

// do not need it, as available time is always >= ready time
//        if ( result[ 0 ] < readyTime ){
//            result[ 0 ] = readyTime;
//       }


        //the estimated finish time is est + compute time on site
        List entries = mTCMapper.getTCList( job.getTXNamespace(),
                                            job.getTXName(),
                                            job.getTXVersion(),
                                            site );
        //pick the first one for time being
        TransformationCatalogEntry entry = ( TransformationCatalogEntry ) entries.get( 0 );
        result[ 1 ] = result[ 0 ] + getExpectedRuntime( job, entry );

        //est now stores the estimated finish time
        return result;
    }

    /**
     * Computes the downward rank of a node.
     *
     * The downward rank of node i is
     *                                   _    ___
     *           max {       rank( n ) + w  + c    }
     *         j E pred( i )     d  j     j    ji
     *
     *
     *
     * @param node   the <code>GraphNode</code> whose rank needs to be computed.
     *
     * @return computed rank.
     */
    protected float computeDownwardRank( GraphNode node ){
        float result = 0;
	//value needs to be computed for each parent separately
        //float value = 0;

        for( Iterator it = node.getParents().iterator(); it.hasNext(); ){
            GraphNode p = (GraphNode)it.next();
            Bag pbag    = p.getBag();
	    float value = 0;
            value += ( getFloatValue ( pbag.get( HeftBag.DOWNWARD_RANK ) )+
                       getFloatValue ( pbag.get( HeftBag.AVG_COMPUTE_TIME ) ) +
                       mAverageCommunicationCost
                     );

            if( value > result ){
                 result = value;
            }
        }

        return result;
    }

    /**
     * Returns the average compute time in seconds for a job.
     *
     * @param job the job whose average compute time is to be computed.
     *
     * @return the weighted compute time in seconds.
     */
    protected float calculateAverageComputeTime( SubInfo job ){
        //get all the TC entries for the sites where a job can run
        List runnableSites = mTCMapper.getSiteList( job.getTXNamespace(),
                                                    job.getTXName(),
                                                    job.getTXVersion(),
                                                    mSites );

        //sanity check
        if( runnableSites == null || runnableSites.isEmpty() ){
            throw new RuntimeException( "No runnable site for job " + job.getName() );
        }

        mLogger.log( "Runnables sites for job " + job.getName() + " " + runnableSites , LogManager.DEBUG_MESSAGE_LEVEL );

        //for each runnable site get the expected runtime
        String site;
        int total_nodes = 0;
        int total = 0;
        for( Iterator it = runnableSites.iterator(); it.hasNext(); ){
            site = ( String ) it.next();
            int nodes = getFreeNodesForSite( site );
            List entries = mTCMapper.getTCList( job.getTXNamespace(),
                                                job.getTXName(),
                                                job.getTXVersion(),
                                                site );

            //pick the first one for time being
            TransformationCatalogEntry entry = ( TransformationCatalogEntry ) entries.get( 0 );
            int jobRuntime = getExpectedRuntime( job, entry );
            total_nodes += nodes;
            total += jobRuntime * nodes;

        }

        return total/total_nodes;
    }


    /**
     * Return expected runtime.
     *
     * @param job    the job in the workflow.
     * @param entry  the <code>TransformationCatalogEntry</code> object.
     *
     * @return the runtime in seconds.
     */
    protected int getExpectedRuntime( SubInfo job, TransformationCatalogEntry entry ){
        int result = -1;

        //try and fetch the expected runtime from the Windward AC
        double pcresult = getExpectedRuntimeFromAC( job , entry );
        
        if( pcresult == 0.0 ){
            mLogger.log( "PC returned a value of 0 for job" + job.getID(), 
                         LogManager.WARNING_MESSAGE_LEVEL );
            result = 1;
        }
        else if( pcresult > 0.0 && pcresult < 1.0 ){
            mLogger.log( "PC returned a value between 0 and 1" + pcresult + " for job " + job.getID(), 
                         LogManager.WARNING_MESSAGE_LEVEL );
            result = 1;
        }
        else{
            result = (int)pcresult;
        }
       
//        if(result == 0){
//            mLogger.log("PC returned 0 as runtime. Returning 1", LogManager.ERROR_MESSAGE_LEVEL);
//            return result=1;
//        }
        if( result >= 1 ){
            return result;
        }

        //else try and get the runtime from the profiles
        List profiles = entry.getProfiles( Profile.VDS );
        mLogger.log( "Fetching runtime information from profiles for job " + job.getName(),
                     LogManager.DEBUG_MESSAGE_LEVEL  );
        mLogger.log( "Profiles are " + profiles, LogManager.DEBUG_MESSAGE_LEVEL);
        if( profiles != null ){
            for (Iterator it = profiles.iterator(); it.hasNext(); ) {
                Profile p = (Profile) it.next();

                if (p.getProfileKey().equals(this.RUNTIME_PROFILE_KEY)) {
                    result = Integer.parseInt(p.getProfileValue());
                    break;
                }
            }
        }
        //if no information . try from profiles in dax
        if( result < 1 ){
            mLogger.log( "Fetching runtime information from profiles for job " + job.getName(),
                          LogManager.DEBUG_MESSAGE_LEVEL  );

            for (Iterator it = job.vdsNS.getProfileKeyIterator(); it.hasNext(); ) {
                String key = (String) it.next();

                if ( key.equals(this.RUNTIME_PROFILE_KEY)) {
                    result = Integer.parseInt( job.vdsNS.getStringValue( key ) );
                    break;
                }

            }

        }

        //sanity check for time being
        if( result < 1 ){
            throw new RuntimeException( "Invalid or no runtime specified for job " + job.getID() );
        }

        return result;
    }

    /**
     * Return expected runtime from the AC only if the process catalog is
     * initialized.
     *
     * @param job    the job in the workflow.
     * @param entry  the TC entry
     *
     * @return the runtime in seconds.
     */
    protected double getExpectedRuntimeFromAC( SubInfo job, TransformationCatalogEntry entry  ){
        double result = -1;
        if( mProcessCatalog == null ){
            return result;
        }
        //fetch the job information first
        List tcs =  mProcessCatalog.getPredictedPerformance(
                          mWGPC.getJobInformation( mRequestID, mLabel, job.getLogicalID() ),
                          entry.getResourceId(),
                          entry.getSysInfo().getArch().toString()
                              );


        mLogger.log( "Predicted performance for job " + job.getID() + " is " + tcs,
                     LogManager.DEBUG_MESSAGE_LEVEL );
        return tcs == null || tcs.isEmpty()?
               result:
               (Double)((( TransformationCharacteristics )tcs.get(0)).getCharacteristic( TransformationCharacteristics.EXPECTED_RUNTIME ));
    }

    /**
     * Populates the number of free nodes for each site, by querying the
     * Site Catalog.
     *
     * @param sites   list of sites.
     */
    @SuppressWarnings({"unchecked", "unchecked"})
    protected void populateSiteMap( List sites ){
        mSiteMap = new HashMap();

        //for testing purposes
        mSites = sites;

        String value = null;
        int nodes = 0;
        for( Iterator it = mSites.iterator(); it.hasNext(); ){
//            SiteInfo s = mSiteHandle.getPoolEntry( site, "vanilla" );
//            JobManager manager = s.selectJobManager( "vanilla", true );
//            value = (String)manager.getInfo( JobManager.IDLE_NODES );
            String site = (String)it.next();
            SiteCatalogEntry eSite = mSiteStore.lookup( site );
            GridGateway jobManager = eSite.selectGridGateway( GridGateway.JOB_TYPE.compute );   
            
            try {
                nodes = jobManager.getIdleNodes();
                if( nodes == -1 ){
                    mLogger.log( "Picking up total nodes for site " + site, 
                                 LogManager.DEBUG_MESSAGE_LEVEL );
                    nodes = jobManager.getTotalNodes();
                
                    if( nodes == -1 ){
                        mLogger.log( "Picking up default free nodes for site " + site, 
                                     LogManager.DEBUG_MESSAGE_LEVEL );
                        nodes = Algorithm.DEFAULT_NUMBER_OF_FREE_NODES;
                    }
                }
            }catch( Exception e ){
                nodes = this.DEFAULT_NUMBER_OF_FREE_NODES;
            }
            mLogger.log( "Available nodes set for site " + site + " " + nodes, 
                                 LogManager.DEBUG_MESSAGE_LEVEL );
            mSiteMap.put( site, new Site( site,  nodes ) );
        }

    }


    /**
     * Returns the freenodes for a site.
     *
     * @param site   the site identifier.
     *
     * @return number of nodes
     */
    protected int getFreeNodesForSite( String site ){
        if( mSiteMap.containsKey( site ) ){
            return ( ( Site )mSiteMap.get( site )).getAvailableProcessors();
        }
        else{
            throw new RuntimeException( "The number of free nodes not available for site " + site );
        }
    }


    /**
     * Schedules a job to a site.
     *
     * @param site  the site at which to schedule
     * @param start the start time for job
     * @param end   the end time of job
     */
    protected void scheduleJob( String site, long start , long end ){
        Site s = ( Site )mSiteMap.get( site );
        s.scheduleJob( start, end );

    }

    /**
     * Returns the available time for a site.
     *
     * @param site       the site at which you want to schedule the job.
     * @param readyTime  the time at which all the data reqd by the job will arrive at site.
     *
     * @return the available time of the site.
     */
    protected long getAvailableTime( String site , long readyTime ){
        if( mSiteMap.containsKey( site ) ){
            return ( ( Site )mSiteMap.get( site )).getAvailableTime( readyTime );
        }
        else{
            throw new RuntimeException( "Site information unavailable for site " + site );
        }


    }

    /**
     * This method returns a String describing the site selection technique
     * that is being implemented by the implementing class.
     *
     * @return String
     */
    public String description() {
        return "Heft based Site Selector";
    }

    /**
     * The call out to the site selector to determine on what pool the job
     * should be scheduled.
     *
     * @param job SubInfo the <code>SubInfo</code> object corresponding to
     *   the job whose execution pool we want to determine.
     * @param pools the list of <code>String</code> objects representing the
     *   execution pools that can be used.
     * @return if the pool is found to which the job can be mapped, a string
     *   of the form <code>executionpool:jobmanager</code> where the
     *   jobmanager can be null. If the pool is not found, then set
     *   poolhandle to NONE. null - if some error occured .
     */
    public String mapJob2ExecPool(SubInfo job, List pools) {
        return "";
    }

    /**
     * A convenience method to get the intValue for the object passed.
     *
     * @param key   the key to be converted
     *
     * @return the floatt value if object an integer, else -1
     */
    private float getFloatValue( Object key ){

        float k = -1;
        //try{
            k = ( (Float) key).floatValue();
        //}
        //catch( Exception e ){}

        return k;

    }
}

/**
 * Comparator for GraphNode objects that allow us to sort on basis of
 * the downward rank computed.
 */
class HeftGraphNodeComparator implements Comparator{

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
  public int compare( Object o1, Object o2 )
  {
    if ( o1 instanceof GraphNode && o2 instanceof GraphNode ) {
      GraphNode g1 = ( GraphNode ) o1;
      GraphNode g2 = ( GraphNode ) o2;

      float drank1 = (( Float )g1.getBag().get( HeftBag.DOWNWARD_RANK ));//.floatValue();
      float drank2 = (( Float )g2.getBag().get( HeftBag.DOWNWARD_RANK ));//.floatValue();

      return (int)(drank1 - drank2);
    } else {
      throw new ClassCastException( "object is not a GraphNode" );
    }
  }

}
