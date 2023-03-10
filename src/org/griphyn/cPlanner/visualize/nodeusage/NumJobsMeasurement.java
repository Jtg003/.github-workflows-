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
package org.griphyn.cPlanner.visualize.nodeusage;

import org.griphyn.cPlanner.visualize.Measurement;

import java.util.Date;

/**
 * Stores the number of jobs as an integer.
 *
 * @author Karan Vahi vahi@isi.edu
 * @version $Revision$
 */

public class NumJobsMeasurement implements Measurement {

    /**
     * Holds the timestamp when usage was taken.
     */
    private Date mDate;

    /**
     * The number of jobs.
     */
    private Integer mNum;


    /**
     * The jobname for which the reading was taken.
     */
    private String mAssociatedJob;

    /**
     * The overloaded constructor.
     *
     * @param d    the date.
     * @param num  the number
     * @param name the jobname.
     */
    public NumJobsMeasurement( Date d, Integer num , String name){
        mDate = d;
        mNum = num;
        mAssociatedJob = name;
    }

    /**
     * Returns the job for which the measurement was taken.
     *
     * @return the name of the job.
     */
    public String getJobName(){
        return this.mAssociatedJob;
    }

    /**
     * Returns the time at which the measurement was taken.
     *
     * @return  the Date object representing the time.
     */
    public Date getTime(){
        return mDate;
    }

    /**
     * Returns the value of the measurement.
     *
     * @return the value.
     */
    public Object getValue(){
        return mNum;
    }


    /**
     * Sets the job for which the measurement was taken.
     *
     * @param name sets the name of the job.
     */
    public void setJobName( String name ){
        this.mAssociatedJob = name;
    }

    /**
     * Sets the time at which the measurement was taken.
     *
     * @param time  the Date object representing the time.
     */
    public void setTime( Date time ){
        this.mDate = time;
    }

    /**
     * Sets the value of the measurement.
     *
     * @param value the value to be associated with measurement.
     */
    public void setValue( Object value ){
        this.mNum = (Integer)value;
    }

    /**
     * Returns textual description.
     *
     * @return String
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append( mDate ).append( " " ).append( getValue() );
        sb.append( " " ).append( getJobName() );
        return sb.toString();

    }
}
