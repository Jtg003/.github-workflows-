/**
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
package org.griphyn.cPlanner.code.generator;


import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PlannerOptions;

import org.griphyn.cPlanner.code.CodeGenerator;
import org.griphyn.cPlanner.code.CodeGeneratorException;

import org.griphyn.cPlanner.common.PegasusProperties;

import org.griphyn.common.util.DynamicLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * An Abstract Base class implementing the CodeGenerator interface. Introduces
 * helper methods for determining basenames of files, that contain concrete
 * job descriptions.
 *
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 *
 * @version $Revision: 1.1 $
 */
public abstract class Abstract implements CodeGenerator{

    /**
     * The version number associated with this API of Code Generator.
     */
    public static final String VERSION = "1.3";

    /**
     * The directory where all the submit files are to be generated.
     */
    protected String mSubmitFileDir;

    /**
     * The object holding all the properties pertaining to Pegasus.
     */
    protected PegasusProperties mProps;

    /**
     * The object containing the command line options specified to the planner
     * at runtime.
     */
    protected PlannerOptions mPOptions;


    /**
     * Initializes the Code Generator implementation.
     *
     * @param properties the <code>PegasusProperties</code> object containing all
     *                   the properties required by Pegasus.
     * @param directory  the base directory where the generated code should reside.
     * @param options    the options passed to the planner at runtime.
     *
     * @throws CodeGeneratorException in case of any error occuring code generation.
     */
    public void initialize( PegasusProperties properties,
                            String directory,
                            PlannerOptions options) throws CodeGeneratorException{

        mSubmitFileDir = directory;
        mProps         = properties;
        mPOptions      = options;
    }



    /**
     * Starts monitoring of the workflow by invoking a workflow monitor daemon.
     * The monitoring should start only after the output files have been generated.
     * FIXME: It should actually happen after the workflow has been submitted.
     *        Eventually should be a separate monitor interface, and submit writers
     *        should be loaded by an AbstractFactory.
     *
     * @return boolean indicating whether could successfully start the monitor
     *         daemon or not.
     */
    public boolean startMonitoring(){
        //by default not all code generators support monitoring.
        return false;
    }


    /**
     * Resets the Code Generator implementation.
     *
     * @throws CodeGeneratorException in case of any error occuring code generation.
     */
    public void reset( )throws CodeGeneratorException{
        mSubmitFileDir = null;
        mProps         = null;
        mPOptions      = null;
    }


    /**
     * Returns an open stream to the file that is used for writing out the
     * job information for the job.
     *
     * @param job  the job whose job information needs to be written.
     *
     * @return  the writer to the open file.
     * @exception IOException if unable to open a write handle to the file.
     */
    public PrintWriter getWriter( SubInfo job ) throws IOException{
//        String jobDir = job.getSubmitDirectory();
        StringBuffer sb = new StringBuffer();

        //determine the absolute submit directory for the job
//        sb.append( GridStart.getSubmitDirectory( mSubmitFileDir, job ));
        sb.append( mSubmitFileDir );

        //append the base name of the job
        sb.append( File.separatorChar ).append( getFileBaseName(job) );

        // intialize the print stream to the file
        return new PrintWriter(new BufferedWriter(new FileWriter(sb.toString())));
    }

    /**
     * Returns the basename of the file to which the job is written to.
     *
     * @param job  the job whose job information needs to be written.
     *
     * @return  the basename of the file.
     */
    public String getFileBaseName(SubInfo job){
        StringBuffer sb = new StringBuffer();
        sb.append(job.jobName).append(".sub");
        return sb.toString();
    }


}
