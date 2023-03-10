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


import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.PegasusBag;
import org.griphyn.cPlanner.classes.PCRelation;
import org.griphyn.cPlanner.classes.PlannerOptions;
import org.griphyn.cPlanner.classes.SubInfo;

import org.griphyn.cPlanner.partitioner.Partitioner;
import org.griphyn.cPlanner.partitioner.ClustererCallback;

import org.griphyn.cPlanner.partitioner.graph.GraphNode;

import org.griphyn.cPlanner.cluster.ClustererFactory;
import org.griphyn.cPlanner.cluster.Clusterer;
import org.griphyn.cPlanner.cluster.ClustererException;


import edu.isi.pegasus.common.logging.LogManager;
import org.griphyn.cPlanner.common.PegasusProperties;

import org.griphyn.cPlanner.parser.dax.DAX2LabelGraph;


import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This collapses the nodes of the same logical name scheduled on the same
 * pool into fewer fat nodes. The idea behind this is to collapse jobs that
 * take a few seconds to run into a larger job, and hence reducing time because
 * of lesser delays due to lesser number of Condor Globus interactions.
 * Note that the merging of the edges for the jobs being collapsed at present, is
 * not the best implementation. Once the graph structure is correct , it would
 * be modified.
 *
 * @author Karan Vahi vahi@isi.edu
 * @author Mei-Hui Su mei@isi.edu
 *
 * @version $Revision$
 */
public class NodeCollapser extends Engine {


    /**
     * The handle to the logger object.
     */
    protected LogManager mLogger;


    /**
     * The directory, where the stdin file of the fat jobs are created.
     * It should be the submit file directory that the user mentions at
     * runtime.
     */
    private String mDirectory;


    /**
     * The internal map that contains the adjacency list representation of the
     * Graph referred to by the workflow. This is temporary till the main ADag
     * data structure is corrected.
     */
    private Map mGraph;

    /**
     * The bag of initialization objects.
     */
    private PegasusBag mBag;

    /**
     * The overloaded constructor.
     *
     * @param bag  the bag of initialization objects.
     *
     */
    public NodeCollapser( PegasusBag bag ) {
        super( bag );
        mBag = bag;
        mLogger = bag.getLogger();
        mGraph  = new HashMap();
        mPOptions = bag.getPlannerOptions();
        setDirectory( mPOptions.getSubmitDirectory() );
    }


    /**
     * Sets the directory where the stdin files are to be generated.
     *
     * @param directory  the path to the directory to which it needs to be set.
     */
    public void setDirectory(String directory){
        mDirectory = (directory == null)?
                      //user did not specify a submit file dir
                      //use the default i.e current directory
                      ".":
                      //user specified directory picked up
                      directory;

    }


    /**
     * Clusters the jobs in the workflow. It applies a series of clustering
     * actions on the graph, as specified by the user at runtime.
     *
     * For each clustering action, the graph is first partitioned,
     * and then sent to the appropriate clustering module for clustering.
     *
     * @param dag the scheduled dag that has to be clustered.
     *
     * @return ADag containing the collapsed scheduled workflow.
     *
     * @throws ClustererException in case of error while clustering
     */
    public ADag cluster( ADag dag ) throws ClustererException{
        //load the appropriate partitioner and clusterer
        String types = mPOptions.getClusteringTechnique();

        //sanity check
        if( types == null){
            //return the orginal DAG only
            mLogger.log( "No clustering actions specified. Returning orginal DAG",
                         LogManager.DEBUG_MESSAGE_LEVEL);
            return dag;
        }

        //tokenize and get the types
        ADag clusteredDAG = dag;
        for( StringTokenizer st = new StringTokenizer( types, ","); st.hasMoreTokens(); ){
            clusteredDAG = this.cluster( clusteredDAG, st.nextToken() );
        }

        return clusteredDAG;
    }

    /**
     * Clusters the jobs in the workflow. The graph is first partitioned,
     * and then sent to the appropriate clustering module for clustering.
     *
     * @param dag the scheduled dag that has to be clustered.
     * @param type the type of clustering to do.
     *
     * @return ADag containing the collapsed scheduled workflow.
     *
     * @throws ClustererException in case of error while clustering
     */
    public ADag cluster( ADag dag, String type ) throws ClustererException{
        //convert the graph representation to a
        //more manageable and traversal data structure that is sent
        //to the partitioning stuff
        Map nameIDMap = new HashMap();
        SubInfo job;
        for( Iterator it = dag.vJobSubInfos.iterator(); it.hasNext(); ){
            //pass the jobs to the callback
            job = (SubInfo)it.next();
            nameIDMap.put( job.getName(), job.getLogicalID() );
        }
        mGraph = edgeList2Graph( dag.dagInfo.relations, nameIDMap );

        //we need to build up a partitioner graph structure to do
        //the partitioning on the graph. Use the callback mechanism
        //developed for the partiotioner stuff and populate it
        //from the exisiting graph structure
        DAX2LabelGraph d2g = new DAX2LabelGraph( mProps, mPOptions.getDAX() );

        //set the appropriate key that is to be used for picking up the labels
        d2g.setLabelKey( mProps.getClustererLabelKey() );


        //no need to pass any attributes
        d2g.cbDocument( null );
        for( Iterator it = dag.vJobSubInfos.iterator(); it.hasNext(); ){
            //pass the jobs to the callback
            d2g.cbJob( (SubInfo)it.next() );
        }
        //pass the relations
        for( Iterator it = mGraph.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Map.Entry)it.next();
            d2g.cbParents( (String)entry.getKey(), (List)entry.getValue() );
        }
        //finished populating
        d2g.cbDone();
        //get the graph map
        mGraph = (Map)d2g.getConstructedObject();

        //get the fake dummy root node
        GraphNode root = (GraphNode)mGraph.get( DAX2LabelGraph.DUMMY_NODE_ID );

        Partitioner p = ClustererFactory.loadPartitioner( mProps, type, root, mGraph );
        mLogger.log( "Partitioner loaded is " + p.description(),
                     LogManager.CONFIG_MESSAGE_LEVEL );

        Clusterer c = ClustererFactory.loadClusterer( dag, mBag, type );

        mLogger.log( "Clusterer loaded is "+ c.description(),
                     LogManager.CONFIG_MESSAGE_LEVEL );
        ClustererCallback  cb  = new ClustererCallback();
        cb.initialize( mProps, c);

        //start the partitioner and let the fun begin!
        p.determinePartitions( cb );

        return c.getClusteredDAG();
    }


    /**
     * Returns an adjacency list representation of the graph referred to by
     * the list of edges. The map contains adjacency list with key as a child
     * and value as the list of parents.
     *
     * @param relations  vector of <code>PCRelation</code> objects that does
     *                   the conversion.
     * @param nameIDMap        map with the key as the jobname and value as the
     *                   logical id
     *
     * @return Map.
     */
    protected Map edgeList2Graph(Vector relations, Map nameIDMap){
        Map map = new HashMap();
        List l = null;

        for( Iterator it = relations.iterator(); it.hasNext(); ){
            PCRelation rel = (PCRelation)it.next();
            if(map.containsKey(nameIDMap.get(rel.child))){
                l = (List)map.get(nameIDMap.get(rel.child));
                l.add(nameIDMap.get(rel.parent));
            }
            else{
                l = new java.util.LinkedList();
                l.add( nameIDMap.get(rel.parent));
                map.put(nameIDMap.get(rel.child),l);
            }
        }

        return map;
    }



}//end of NodeCollapser
