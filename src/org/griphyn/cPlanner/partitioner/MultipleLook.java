/*
 * This file or a portion of this file is licensed under the terms of
 * the Globus Toolkit Public License, found in file GTPL, or at
 * http://www.globus.org/toolkit/download/license.html. This notice must
 * appear in redistributions of this file, with or without modification.
 *
 * Redistributions of this Software, with or without modification, must
 * reproduce the GTPL in: (1) the Software, or (2) the Documentation or
 * some other similar material which is provided with the Software (if
 * any).
 *
 * Copyright 1999-2004 University of Chicago and The University of
 * Southern California. All rights reserved.
 */
package org.griphyn.cPlanner.partitioner;

import org.griphyn.vdl.classes.LFN;

import org.griphyn.cPlanner.common.LogManager;

import org.griphyn.vdl.dax.ADAG;
import org.griphyn.vdl.dax.Filename;
import org.griphyn.vdl.dax.Job;

import org.griphyn.vdl.euryale.Callback;

import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * This class ends up writing a partitioned dax, that corresponds to one
 * partition as defined by the Partitioner. Whenever it is called to write
 * out a dax corresponding to a partition it looks up the dax i.e parses the
 * dax and gets the information about the jobs making up the partition.
 *
 * @author Karan Vahi
 * @version $Revision: 1.8 $
 */
public class MultipleLook extends DAXWriter{

    /**
     * The set of job id's in the partition.
     */
    private Set mNodeSet;

    /**
     * A map containing the relations between the jobs making up the partition.
     */
    private Map mRelationsMap;

    /**
     * The ADAG object containing the partitioned dax.
     */
    private ADAG mPartADAG;

    /**
     * The number of jobs that are in the partition.
     */
    private int mNumOfJobs;

    /**
     * The number of jobs about which the callback interface has knowledge.
     */
    private int mCurrentNum;

    /**
     * The index of the partition that is being written out.
     */
    private int mIndex;

    /**
     * The overloaded constructor.
     *
     * @param daxFile   the path to the dax file that is being partitioned.
     * @param directory the directory in which the partitioned daxes are to be
     *                  generated.
     */
    public MultipleLook(String daxFile, String directory){
        super(daxFile,directory);
        mIndex = -1;
    }


    /**
     * It writes out a dax consisting of the jobs as specified in the partition.
     *
     * @param partition  the partition object containing the relations and id's
     *                   of the jobs making up the partition.
     *
     * @return boolean  true if dax successfully generated and written.
     *                  false in case of error.
     */
    public boolean writePartitionDax(Partition partition, int index){

        //do the cleanup
        mPartADAG = null;
        mNodeSet  = null;
        mRelationsMap = null;
        mIndex = index;

        //get from the partition object the set of jobs
        //and relations between them
        mNodeSet      = partition.getNodeIDs();
        mRelationsMap = partition.getRelations();
        mNumOfJobs    = mNodeSet.size();

        //set the current number of jobs whose information we have
        mCurrentNum = 0;

        mPartADAG      = new ADAG(0,index,mPartitionName);

        Callback callback = new MyCallBackHandler();
        org.griphyn.vdl.euryale.DAXParser d =
            new org.griphyn.vdl.euryale.DAXParser(null);
        d.setCallback(callback);
        d.parse(mDaxFile);

        //do the actual writing to the file
        this.initializeWriteHandle(mIndex);
        try{
            mPartADAG.toXML(mWriteHandle, new String());
        }
        catch(IOException e){
            mLogger.log("Error while writing out a partition dax :" +
                        e.getMessage(),LogManager.ERROR_MESSAGE_LEVEL);
            return false;
        }
        this.close();


        return true;
    }




    /**
     * The internal callback handler for the DAXParser in Euryale. It only
     * stores the jobs that are part of the dax, that are then populated into
     * the internal ADAG object that is used to write out the dax file
     * corresponding to the partition.
     */
    private class MyCallBackHandler implements Callback {

        /**
         * The empty constructor.
         */
        public MyCallBackHandler(){

        }

        /**
         * Callback when the opening tag was parsed. The attribute maps each
         * attribute to its raw value. The callback initializes the DAG
         * writer.
         *
         * @param attributes is a map of attribute key to attribute value
         */
        public void cb_document(Map attributes) {
            //do nothing at the moment
        }


        public void cb_filename(Filename filename) {

        }

        /**
         *
         */
        public void cb_job(Job job) {
            List fileList = null;
            Iterator it;

            if(mNodeSet.contains(job.getID())){
                mCurrentNum++;
                mPartADAG.addJob(job);
                fileList = job.getUsesList();

                //iterate through the file list
                //populate it in the ADAG object
                it = fileList.iterator();
                while(it.hasNext()){
                    Filename file = (Filename)it.next();
                    mPartADAG.addFilename(file.getFilename(),
                                          (file.getLink() == LFN.INPUT)?true:false,
                                          file.getTemporary(),
                                          file.getDontRegister(),file.getDontTransfer());
                }
            }

        }


        public void cb_parents(String child, List parents) {

        }


        public void cb_done() {
            List parentIDs;
            //print the xml generated so far


            if(mCurrentNum != mNumOfJobs){
                //throw an error and exit.
                throw new RuntimeException( "Could not find information about all the jobs" +
                                            " in the dax for partition " + mNodeSet);
            }


            //add the relations between the jobs in the partition to the ADAG
            Iterator it = mRelationsMap.keySet().iterator();
            while(it.hasNext()){
                String childID = (String)it.next();
                parentIDs =  (List)mRelationsMap.get(childID);

                //get all the parents of the children and populate them in the
                //ADAG object
                Iterator it1 = parentIDs.iterator();
                while(it1.hasNext()){
                    mPartADAG.addChild(childID,(String)it1.next());
                }

            }


        }

    }
}
