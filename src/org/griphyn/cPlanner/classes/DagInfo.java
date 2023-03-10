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


import edu.isi.pegasus.common.logging.LogManager;

import org.griphyn.common.util.Currently;
import org.griphyn.common.util.Version;

import java.io.File;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Vector;
/**
 * Holds the information needed to make one dag file corresponding to a Abstract
 * Dag. It holds information to generate the .dax file which is submitted to
 * Condor.
 *
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 * @version $Revision$
 */

public class DagInfo extends Data {

    /**
     * The default name for the ADag object, if not supplied in the DAX.
     */
    private static final String DEFAULT_NAME = "PegasusRun";

    /**
     * Vector of String objects containing the jobname_id of jobs making
     * the abstract dag.
     */
    public Vector dagJobs;

    /**
     * Captures the parent child relations making up the DAG. It is a Vector of
     * <code>PCRelation</code> objects.
     */
    public Vector relations;

    /**
     * The name of the Abstract Dag taken from the adag element of the DAX
     * generated by the Abstract Planner.
     */
    public String nameOfADag;

    /**
     * Refers to the number of the Abstract Dags which are being sent to the
     * Concrete Planner in response to the user's request.
     */
    public String count;

    /**
     * Refers to the number of the Dag. Index can vary from 0 to count - 1.
     */
    public String index;

    /**
     * It is a unique identifier identifying the concrete DAG generated by Pegasus.
     * It consists of the dag name and the timestamp.
     *
     * @see #flowIDName
     * @see #mFlowTimestamp
     */
    public String flowID;


    /**
     * It is the name of the dag as generated by Chimera in the dax. If none is
     * specified then a default name of PegasusRun is assigned.
     */
    public String flowIDName;

    /**
     * The ISO timestamp corresponding to the time when Pegasus is invoked for a
     * dax. It is used to generate the random directory  names also if required.
     */
    private String mFlowTimestamp;

    /**
     * Keeps the last modified time of the DAX.
     */
    private String mDAXMTime;

    /**
     * Identifies the release version of the VDS software that was
     * used to generate the workflow. It is populated from Version.java.
     *
     * @see org.griphyn.common.util.Version
     */
    public String releaseVersion;

    /**
     * The workflow metric objects that contains metrics about the workflow being
     * planned.
     */
    private WorkflowMetrics mWFMetrics;


    /**
     * Contains a unique ordered listing of the logical names referred
     * to by the dag. The TreeMap implementation guarentees us a log(n) execution
     * time for the basic operations. Hence should scale well. The key for the
     * map is the lfn name. The value is a String flag denoting whether this
     * file is an input(i) or output(o) or both (b) or none(n). A value of
     * none(n) would denote an error condition.
     */
    public TreeMap lfnMap;


    //for scripts later

    /**
     * The default constructor.
     */
    public DagInfo() {
        dagJobs        = new Vector();
        relations      = new Vector();
        nameOfADag     = new String();
        count          = new String();
        index          = new String();
        flowID         = new String();
        flowIDName     = new String();
        mFlowTimestamp = new String();
        mDAXMTime      = new String();
        releaseVersion = new String();
        lfnMap         = new TreeMap();
        mWFMetrics     = new WorkflowMetrics();
    }


    /**
     * Adds a new job to the dag.
     *
     * @param job  the job to be added
     */
    public void addNewJob( SubInfo job ) {
        dagJobs.add( job.getID() );
        //increment the various metrics
        mWFMetrics.increment( job );
    }

    /**
     * Adds a new PCRelation pair to the Vector of <code>PCRelation</code>
     * pairs. Since we are adding a new relation the isDeleted parameter should
     * be false.
     *
     * @param parent    The parent in the relation pair
     * @param child     The child in the relation pair
     *
     * @see #relations
     */
    public void addNewRelation(String parent, String child) {
        PCRelation newRelation = new PCRelation(parent, child);
        relations.addElement(newRelation);
    }

    /**
     * Adds a new PCRelation pair to the Vector of <code>PCRelation</code> pairs.
     *
     * @param parent    The parent in the relation pair
     * @param child     The child in the relation pair
     * @param isDeleted Whether the relation has been deleted due to the
     *                  reduction algorithm or not
     *
     * @see #relations
     */
    public void addNewRelation(String parent, String child, boolean isDeleted) {
        PCRelation newRelation = new PCRelation(parent, child, isDeleted);
        relations.addElement(newRelation);
    }

    /**
     * Removes a job from the dag/graph structure. It however does not
     * delete the relations the edges that refer to the job.
     *
     * @param job the job to be removed
     *
     * @return boolean indicating whether removal was successful or not.
     */
    public boolean remove( SubInfo  job ){
        mWFMetrics.decrement( job );
        return dagJobs.remove( job.getID() );
    }


    /**
     * It returns the list of lfns referred to by the DAG. The list is unique
     * as it is gotten from iterating through the lfnMap.
     *
     * @return a Set of <code>String<code> objects corresponding to the
     *         logical filenames
     */
    public Set getLFNs(){
        return this.getLFNs( false );
    }

    /**
     * Returns the list of lfns referred to by the DAG. The list is unique
     * as it is gotten from iterating through the lfnMap. The contents of the list
     * are determined on the basis of the command line options passed by the user
     * at runtime. For e.g. if the user has specified force, then one needs to
     * search only for the input files.
     *
     * @param onlyInput  a boolean flag indicating that you need only the input
     *                   files to the whole workflow
     *
     * @return a set of logical filenames.
     */
    public Set getLFNs( boolean onlyInput ) {

        Set lfns = onlyInput ? new HashSet( lfnMap.size()/3 ):
                                   new HashSet( lfnMap.size() );
        String key = null;
        String val = null;

        //if the force option is set we
        //need to search only for the
        //input files in the dag i.e
        //whose link is set to input in
        //the dag.
        if ( onlyInput ){
            for (Iterator it = lfnMap.keySet().iterator(); it.hasNext(); ) {
                key = (String) it.next();
                val = (String) lfnMap.get(key);

                if ( val.equals( "i" ) ) {
                    lfns.add( key );
                }
            }
        }
        else {
            lfns=new HashSet( lfnMap.keySet() );
        }

        return lfns;
    }



    /**
     * Returns the label of the workflow, that was specified in the DAX.
     *
     * @return the label of the workflow.
     */
    public String getLabel(){
        return (nameOfADag == null)?
                this.DEFAULT_NAME:
                nameOfADag;
    }

    /**
     * Returns the last modified time for the file containing the workflow
     * description.
     *
     * @return the MTime
     */
    public String getMTime(){
        return mDAXMTime;
    }



    /**
     * Returns the flow timestamp for the workflow.
     *
     * @return the flowtimestamp
     */
    public String getFlowTimestamp(){
        return mFlowTimestamp;
    }

    /**
     * Sets the flow timestamp for the workflow.
     *
     * @param timestamp the flowtimestamp
     */
    public void setFlowTimestamp( String timestamp ){
        mFlowTimestamp = timestamp;
    }



    /**
     * Returns the number of jobs in the dag on the basis of number of elements
     * in the <code>dagJobs</code> Vector.
     *
     * @return the number of the jobs.
     */
    public int getNoOfJobs() {
        return dagJobs.size();
    }

    /**
     * Gets all the parents of a particular node.
     *
     * @param node the name of the job whose parents are to be found.
     *
     * @return    Vector corresponding to the parents of the node.
     */
    public Vector getParents(String node) {
        //getting the parents of that node
        Enumeration ePcRel = this.relations.elements();
        Vector vParents = new Vector();
        PCRelation currentRelPair;
        while (ePcRel.hasMoreElements()) {
            currentRelPair = (PCRelation) ePcRel.nextElement();
            if (currentRelPair.child.trim().equalsIgnoreCase(node)) {
                vParents.addElement(new String(currentRelPair.parent));
            }
        }

        return vParents;
    }

    /**
     * Get all the children of a particular node.
     *
     * @param node the name of the node whose children we want to find.
     *
     * @return  Vector containing the children of the node.
     */
    public Vector getChildren(String node) {
        Enumeration ePcRel = this.relations.elements();
        Vector vChildren = new Vector();
        PCRelation currentRelPair;

        while (ePcRel.hasMoreElements()) {
            currentRelPair = (PCRelation) ePcRel.nextElement();
            if (currentRelPair.parent.trim().equalsIgnoreCase(node)) {
                vChildren.addElement(new String(currentRelPair.child));
            }
        }

        return vChildren;
    }

    /**
     * This returns all the leaf nodes of the dag. The way the structure of Dag
     * is specified in terms of the parent child relationship pairs, the
     * determination of the leaf nodes can be computationally intensive. The
     * complexity if of order n^2.
     *
     * @return Vector of <code>String</code> corresponding to the job names of
     *         the leaf nodes.
     *
     * @see org.griphyn.cPlanner.classes.PCRelation
     * @see org.griphyn.cPlanner.classes.DagInfo#relations
     */
    public Vector getLeafNodes() {
        Vector leafJobs = new Vector();
        Vector vJobs = this.dagJobs;
        Vector vRelations = this.relations;
        Enumeration eRel;
        String job;
        PCRelation pcRel;
        boolean isLeaf = false;

        //search for all the jobs which are Roots i.e are not child in relation
        Enumeration e = vJobs.elements();

        while (e.hasMoreElements()) {
            //traverse through all the relations
            job = (String) e.nextElement();
            eRel = vRelations.elements();

            isLeaf = true;
            while (eRel.hasMoreElements()) {
                pcRel = (PCRelation) eRel.nextElement();

                if (pcRel.parent.equalsIgnoreCase(job)) { //means not a Child job
                    isLeaf = false;
                    break;
                }
            }

            //adding if leaf to vector
            if (isLeaf) {
                mLogger.log("Leaf job is " + job, LogManager.DEBUG_MESSAGE_LEVEL);
                leafJobs.addElement(new String(job));
            }
        }

        return leafJobs;
    }

    /**
     * It determines the root Nodes for the ADag looking at the relation pairs
     * of the adag. The way the structure of Dag is specified in terms
     * of the parent child relationship pairs, the determination of the leaf
     * nodes can be computationally intensive. The complexity if of
     * order n^2.
     *
     *
     * @return the root jobs of the Adag
     *
     * @see org.griphyn.cPlanner.classes.PCRelation
     * @see org.griphyn.cPlanner.classes.DagInfo#relations
     *
     */
    public Vector getRootNodes() {
        Vector rootJobs = new Vector();
        Vector vJobs = this.dagJobs;
        Vector vRelations = this.relations;
        Enumeration eRel;
        String job;
        PCRelation pcRel;
        boolean isRoot = false;

        //search for all the jobs which are Roots
        //i.e are not child in relation
        Enumeration e = vJobs.elements();

        while (e.hasMoreElements()) {
            //traverse through all the relations
            job = (String) e.nextElement();
            eRel = vRelations.elements();

            isRoot = true;
            while (eRel.hasMoreElements()) {
                pcRel = (PCRelation) eRel.nextElement();

                if (pcRel.child.equalsIgnoreCase(job)) { //means not a Root job
                    isRoot = false;
                    break;
                }
            }
            //adding if Root to vector
            if (isRoot) {
                mLogger.log("Root job is " + job, LogManager.DEBUG_MESSAGE_LEVEL);
                rootJobs.addElement(new String(job));
            }
        }

        return rootJobs;
    }


    /**
     * Returns the workflow metrics so far.
     *
     * @return the workflow metrics
     */
    public WorkflowMetrics getWorkflowMetrics(){
        return this.mWFMetrics;
    }


    /**
     * Generates the flow id for this current run. It is made of the name of the
     * dag and a timestamp. This is a simple concat of the mFlowTimestamp and the
     * flowName. For it work correctly the function needs to be called after the
     * flow name and timestamp have been generated.
     */
    public void generateFlowID() {
        StringBuffer sb = new StringBuffer(40);

        sb.append(flowIDName).append("-").append(mFlowTimestamp);

        flowID = sb.toString();
    }


    /**
     * Generates the name of the flow. It is same as the nameOfADag if specified
     * in dax generated by Chimera.
     */
    public void generateFlowName(){
        StringBuffer sb = new StringBuffer();

        if (nameOfADag != null)
            sb.append(nameOfADag);
        else
            sb.append(this.DEFAULT_NAME);

        //append the count. that is important for deffered planning
        sb.append("-").append(index);

        flowIDName = sb.toString();

    }

    /**
     * Sets the label for the workflow.
     *
     * @param label the label to be assigned to the workflow
     */
    public void setLabel(String label){
        this.nameOfADag = label;
        mWFMetrics.setLabel( label );
    }

    /**
     * Sets the mtime (last modified time) for the DAX. It is the time, when
     * the DAX file was last modified. If the DAX file does not exist or an
     * IO error occurs, the MTime is set to OL i.e . The DAX mTime is always
     * generated in an extended format. Generating not in extended format, leads
     * to the XML parser tripping while parsing the invocation record generated
     * by Kickstart.
     *
     * @param f  the file descriptor to the DAX|PDAX file.
     */
    public void setDAXMTime( File f ){
        long time = f.lastModified();
        mDAXMTime = Currently.iso8601(false,true,false,
                                          new java.util.Date(time));
    }


    /**
     * Sets the mtime (last modified time) for the DAX. It is the time, when
     * the DAX file was last modified. If the DAX file does not exist or an
     * IO error occurs, the MTime is set to OL i.e . The DAX mTime is always
     * generated in an extended format. Generating not in extended format, leads
     * to the XML parser tripping while parsing the invocation record generated
     * by Kickstart.
     *
     * @param time  iso formatted time string indicating the last modified time
     *              of DAX
     */
    public void setDAXMTime( String time ){
        mDAXMTime = time;
    }

    /**
     * Grabs the release version from VDS.Properties file.
     *
     * @see org.griphyn.common.util.Version
     */
    public void setReleaseVersion() {
        this.releaseVersion = Version.instance().toString();
    }


    /**
     * Updates the lfn map, that contains the mapping of an lfn with the type.
     *
     * @param lfn  the logical file name.
     * @param type  type the type of lfn (i|o|b). usually a character.
     */
    public void updateLFNMap(String lfn,String type){
        Object entry = lfnMap.get(lfn);
        if(entry == null){
            lfnMap.put(lfn,type);
            return;
        }
        else{
            //there is a preexisting entry in the map, check if it needs to be
            //updated
            if(!(entry.equals("b") || entry.equals(type))){
                //types do not match. so upgrade the type to both
                lfnMap.put(lfn,"b");
            }
        }
    }

    /**
     * Returns a new copy of the Object.
     *
     * @return a copy of the object.
     */
    public Object clone() {
        DagInfo dag = new DagInfo();

        dag.dagJobs = (Vector)this.dagJobs.clone();
        dag.relations = (Vector)this.relations.clone();
        dag.nameOfADag = new String(this.nameOfADag);
        dag.count = new String(this.count);
        dag.index = new String(this.index);
        dag.flowID = new String(this.flowID);
        dag.flowIDName     = new String(this.flowIDName);
        dag.mFlowTimestamp  = new String(this.mFlowTimestamp);
        dag.mDAXMTime       = new String(this.mDAXMTime);
        dag.releaseVersion = new String(this.releaseVersion);
        dag.lfnMap = (TreeMap)this.lfnMap.clone();
        dag.mWFMetrics = ( WorkflowMetrics )this.mWFMetrics.clone();
        return dag;
    }


    /**
     * Returns the a textual description of the object.
     *
     * @return textual description.
     */
    public String toString() {
        String st = "\n " +
            "\n Name of ADag : " + this.nameOfADag +
            "\n Index        : " + this.index + " Count :" + this.count +
            //"\n FlowId       : " + this.flowID +
            "\n FlowName     : " + this.flowIDName +
            "\n FlowTimestamp: " + this.mFlowTimestamp +
            "\n Release Ver  : " + this.releaseVersion +
            vectorToString(" Relations making the Dag ", this.relations) +
            "\n LFN List is " + this.lfnMap;


        return st;
    }

}
