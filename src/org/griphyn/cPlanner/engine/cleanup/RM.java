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

import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;
import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PegasusFile;

import org.griphyn.cPlanner.common.PegasusProperties;

import org.griphyn.cPlanner.namespace.Condor;


import org.griphyn.common.catalog.TransformationCatalog;
import org.griphyn.common.catalog.TransformationCatalogEntry;


import org.griphyn.common.classes.TCType;

import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import org.griphyn.cPlanner.classes.PegasusBag;

/**
 * Use's RM to do removal of the files on the remote sites.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class RM implements CleanupImplementation{


    /**
     * The default logical name to rm executable.
     */
    public static final String DEFAULT_RM_LOGICAL_NAME = "rm";


    /**
     * The default path to rm executable.
     */
    public static final String DEFAULT_RM_LOCATION = "/bin/rm";

    /**
     * The default priority key associated with the cleanup jobs.
     */
    public static final String DEFAULT_PRIORITY_KEY = "1000";


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
     * The default constructor.
     */
    public RM( ){
    }
    
    /**
     * Intializes the class.
     *
     * @param bag      bag of initialization objects
     */
    public void initialize( PegasusBag bag ) {
        mSiteStore       = bag.getHandleToSiteStore();
        mTCHandle        = bag.getHandleToTransformationCatalog();  
        mProps           = bag.getPegasusProperties();

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
        cJob.setSiteHandle( job.getSiteHandle() );

        //inconsistency between job name and logical name for now
        cJob.setTXVersion( null );
        cJob.setTXName( "rm" );
        cJob.setTXNamespace( null );
        cJob.setLogicalID( id );

        //the compute job of the VDS supernode is this job itself
        cJob.setVDSSuperNode( job.getID() );

        //set the list of files as input files
        //to change function signature to reflect a set only.
        cJob.setInputFiles( new HashSet( files) );


        //set the path to the rm executable
        TransformationCatalogEntry entry = this.getTCEntry( job.getSiteHandle() );
        cJob.setRemoteExecutable( entry.getPhysicalTransformation() );

        //set the arguments for the cleanup job
        StringBuffer arguments = new StringBuffer();
        for( Iterator it = files.iterator(); it.hasNext(); ){
            PegasusFile file = (PegasusFile)it.next();
            arguments.append( " " ).append( file.getLFN() );
        }
        cJob.setArguments( arguments.toString() );

        //the cleanup job is a clone of compute
        //need to reset the profiles first
        cJob.resetProfiles();

        //the profile information from the pool catalog needs to be
        //assimilated into the job.
        cJob.updateProfiles( mSiteStore.lookup( job.getSiteHandle() ).getProfiles() );

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
            tcentries = mTCHandle.getTCEntries( null,
                                                DEFAULT_RM_LOGICAL_NAME,
                                                null,
                                                site,
                                                TCType.INSTALLED );
        } catch (Exception e) { /* empty catch */ }

        //see if any record is returned or not
        entry = (tcentries == null)?
                 defaultTCEntry() :
                 (TransformationCatalogEntry) tcentries.get(0);

        return entry;

    }

    /**
     * Returns a default TransformationCatalogEntry object for the rm executable.
     *
     * @return default <code>TransformationCatalogEntry</code>
     */
    private static TransformationCatalogEntry defaultTCEntry(){
        TransformationCatalogEntry entry = new TransformationCatalogEntry( null, DEFAULT_RM_LOGICAL_NAME, null);
        entry.setPhysicalTransformation( DEFAULT_RM_LOCATION );

        return entry;
    }
}
