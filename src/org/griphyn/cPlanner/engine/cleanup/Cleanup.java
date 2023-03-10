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


package org.griphyn.cPlanner.engine.cleanup;

import edu.isi.pegasus.planner.catalog.SiteCatalog;

import edu.isi.pegasus.planner.catalog.site.SiteFactory;
import edu.isi.pegasus.planner.catalog.site.SiteFactoryException;

import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;
import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;

import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PlannerOptions;
import org.griphyn.cPlanner.classes.PegasusBag;
import org.griphyn.cPlanner.classes.PegasusFile;

import org.griphyn.cPlanner.common.PegasusProperties;
import edu.isi.pegasus.common.logging.LogManager;


import org.griphyn.cPlanner.namespace.Condor;

import org.griphyn.common.catalog.TransformationCatalog;
import org.griphyn.common.catalog.TransformationCatalogEntry;

import org.griphyn.common.catalog.transformation.TransformationFactory;
import org.griphyn.common.catalog.transformation.TransformationFactoryException;

import org.griphyn.common.classes.TCType;

import org.griphyn.common.util.Separator;

import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

/**
 * Use's RM to do removal of the files on the remote sites.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class Cleanup implements CleanupImplementation{

    /**
     * The transformation namespace for the  job.
     */
    public static final String TRANSFORMATION_NAMESPACE = "pegasus";

    /**
     * The default priority key associated with the cleanup jobs.
     */
    public static final String DEFAULT_PRIORITY_KEY = "1000";

    /**
     * The name of the underlying transformation that is queried for in the
     * Transformation Catalog.
     */
    public static final String TRANSFORMATION_NAME = "cleanup";

    /**
     * The version number for the job.
     */
    public static final String TRANSFORMATION_VERSION = null;

    /**
     * The derivation namespace for the job.
     */
    public static final String DERIVATION_NAMESPACE = "pegasus";

    /**
     * The name of the underlying derivation.
     */
    public static final String DERIVATION_NAME = "cleanup";

    /**
     * The derivation version number for the job.
     */
    public static final String DERIVATION_VERSION = null;

    /**
     * A short description of the transfer implementation.
     */
    public static final String DESCRIPTION =
                    "A cleanup script that reads from the stdin the list of files" +
                    " to be cleaned, with one file per line";



    /**
     * The handle to the transformation catalog.
     */
    protected TransformationCatalog mTCHandle;

    /**
     * Handle to the site catalog.
     */
//    protected PoolInfoProvider mSiteHandle;
    protected SiteStore mSiteStore;

    /**
     * The handle to the properties passed to Pegasus.
     */
    private PegasusProperties mProps;

    /**
     * The submit directory where the output files have to be written.
     */
    private String mSubmitDirectory;

    /**
     * The handle to the logger.
     */
    private LogManager mLogger;

    /**
     * A convenience method to return the complete transformation name being
     * used to construct jobs in this class.
     *
     * @return the complete transformation name
     */
    public static String getCompleteTranformationName(){
        return Separator.combine( TRANSFORMATION_NAMESPACE,
                                  TRANSFORMATION_NAME,
                                  TRANSFORMATION_VERSION );
    }
    
    /**
     * The default constructor.
     */
    public Cleanup(){
        
    }

    /**
     * Creates a new instance of InPlace
     *
     * @param bag  the bag of initialization objects.
     *
     */
    public void initialize( PegasusBag bag ) {
        mProps           = bag.getPegasusProperties();
        mSubmitDirectory = bag.getPlannerOptions().getSubmitDirectory();
        mSiteStore       = bag.getHandleToSiteStore();
        mTCHandle        = bag.getHandleToTransformationCatalog(); 
        mLogger          = bag.getLogger();
    }


    /**
     * Creates a cleanup job that removes the files from remote working directory.
     * This will eventually make way to it's own interface.
     *
     * @param id         the identifier to be assigned to the job.
     * @param files      the list of <code>PegasusFile</code> that need to be
     *                   cleaned up.
     * @param job        the primary compute job with which this cleanup job is associated.
     *
     * @return the cleanup job.
     */
    public SubInfo createCleanupJob( String id, List files, SubInfo job ){

        //we want to run the clnjob in the same directory
        //as the compute job. So we clone.
        SubInfo cJob = ( SubInfo )job.clone();
        cJob.setJobType( SubInfo.CLEANUP_JOB );
        cJob.setName( id );
        cJob.setArguments( "" );

        //inconsistency between job name and logical name for now
        cJob.setTransformation( Cleanup.TRANSFORMATION_NAMESPACE,
                                Cleanup.TRANSFORMATION_NAME,
                                Cleanup.TRANSFORMATION_VERSION );

        cJob.setDerivation( Cleanup.DERIVATION_NAMESPACE,
                            Cleanup.DERIVATION_NAME,
                            Cleanup.DERIVATION_VERSION );

        //cJob.setLogicalID( id );

        //set the list of files as input files
        //to change function signature to reflect a set only
        cJob.setInputFiles( new HashSet( files) );

        //the compute job of the VDS supernode is this job itself
        cJob.setVDSSuperNode( job.getID() );

        //set the path to the rm executable
        TransformationCatalogEntry entry = this.getTCEntry( job.getSiteHandle() );
        cJob.setRemoteExecutable( entry.getPhysicalTransformation() );


        //prepare the stdin for the cleanup job
        String stdIn = id + ".in";
        try{
            BufferedWriter writer;
            writer = new BufferedWriter( new FileWriter(
                                        new File( mSubmitDirectory, stdIn ) ));

            for( Iterator it = files.iterator(); it.hasNext(); ){
                PegasusFile file = (PegasusFile)it.next();
                writer.write( file.getLFN() );
                writer.write( "\n" );
            }



            //closing the handle to the writer
            writer.close();
        }
        catch(IOException e){
            mLogger.log( "While writing the stdIn file " + e.getMessage(),
                        LogManager.ERROR_MESSAGE_LEVEL);
            throw new RuntimeException( "While writing the stdIn file " + stdIn, e );
        }

        //we want to run the job on fork jobmanager
        //SiteInfo site = mSiteHandle.getTXPoolEntry( cJob.getSiteHandle() );
        //JobManager jobmanager = site.selectJobManager( Engine.TRANSFER_UNIVERSE, true );
        //cJob.globusScheduler = (jobmanager == null) ?
        //                        null :
        //                       jobmanager.getInfo(JobManager.URL);


        //set the stdin file for the job
        cJob.setStdIn( stdIn );

        //the cleanup job is a clone of compute
        //need to reset the profiles first
        cJob.resetProfiles();

        //the profile information from the pool catalog needs to be
        //assimilated into the job.
        cJob.updateProfiles( mSiteStore.lookup( job.getSiteHandle() ).getProfiles()  );

        //the profile information from the transformation
        //catalog needs to be assimilated into the job
        //overriding the one from pool catalog.
        cJob.updateProfiles( entry );

        //the profile information from the properties file
        //is assimilated overidding the one from transformation
        //catalog.
        cJob.updateProfiles( mProps );

        //let us put some priority for the cleaunup jobs
        cJob.condorVariables.construct( Condor.PRIORITY_KEY,
                                        DEFAULT_PRIORITY_KEY );

        //a remote hack that only works for condor pools
        //cJob.globusRSL.construct( "condorsubmit",
        //                                 "(priority " + DEFAULT_PRIORITY_KEY + ")");
        return cJob;
    }

    /**
     * Returns the TCEntry object for the rm executable on a grid site.
     *
     * @param site the site corresponding to which the entry is required.
     *
     * @return  the TransformationCatalogEntry corresponding to the site.
     */
    protected TransformationCatalogEntry getTCEntry( String site ){
        List tcentries = null;
        TransformationCatalogEntry entry  = null;
        try {
            tcentries = mTCHandle.getTCEntries( this.TRANSFORMATION_NAMESPACE,
                                                this.TRANSFORMATION_NAME,
                                                this.TRANSFORMATION_VERSION,
                                                site,
                                                TCType.INSTALLED );
        } catch (Exception e) { /* empty catch */ }


        entry = ( tcentries == null ) ?
                 this.defaultTCEntry( site ): //try using a default one
                 (TransformationCatalogEntry) tcentries.get(0);

        if( entry == null ){
            //NOW THROWN AN EXCEPTION

            //should throw a TC specific exception
            StringBuffer error = new StringBuffer();
            error.append("Could not find entry in tc for lfn ").
                  append( this.getCompleteTranformationName()).
                  append(" at site ").append(site);

              mLogger.log( error.toString(), LogManager.ERROR_MESSAGE_LEVEL);
              throw new RuntimeException( error.toString() );

          }


        return entry;

    }

    /**
     * Returns a default TC entry to be used in case entry is not found in the
     * transformation catalog.
     *
     * @param site   the site for which the default entry is required.
     *
     *
     * @return  the default entry.
     */
    private  TransformationCatalogEntry defaultTCEntry( String site ){
        TransformationCatalogEntry defaultTCEntry = null;
        //check if PEGASUS_HOME is set
        String home = mSiteStore.getPegasusHome( site );

        mLogger.log( "Creating a default TC entry for " +
                     this.getCompleteTranformationName() +
                     " at site " + site,
                     LogManager.DEBUG_MESSAGE_LEVEL );


        //if home is still null
        if ( home == null ){
            //cannot create default TC
            mLogger.log( "Unable to create a default entry for " +
                         Separator.combine( this.TRANSFORMATION_NAMESPACE,
                                            this.TRANSFORMATION_NAME,
                                            this.TRANSFORMATION_VERSION ),
                         LogManager.DEBUG_MESSAGE_LEVEL );
            //set the flag back to true
            return defaultTCEntry;
        }

        //remove trailing / if specified
        home = ( home.charAt( home.length() - 1 ) == File.separatorChar )?
            home.substring( 0, home.length() - 1 ):
            home;

        //construct the path to it
        StringBuffer path = new StringBuffer();
        path.append( home ).append( File.separator ).
            append( "bin" ).append( File.separator ).
            append( Cleanup.TRANSFORMATION_NAME );


        defaultTCEntry = new TransformationCatalogEntry( Cleanup.TRANSFORMATION_NAMESPACE,
                                                           Cleanup.TRANSFORMATION_NAME,
                                                           Cleanup.TRANSFORMATION_VERSION );

        defaultTCEntry.setPhysicalTransformation( path.toString() );
        defaultTCEntry.setResourceId( site );
        defaultTCEntry.setType( TCType.INSTALLED );

        //register back into the transformation catalog
        //so that we do not need to worry about creating it again
        try{
            mTCHandle.addTCEntry( defaultTCEntry , false );
        }
        catch( Exception e ){
            //just log as debug. as this is more of a performance improvement
            //than anything else
            mLogger.log( "Unable to register in the TC the default entry " +
                          defaultTCEntry.getLogicalTransformation() +
                          " for site " + site, e,
                          LogManager.DEBUG_MESSAGE_LEVEL );
        }

        return defaultTCEntry;
    }

}
