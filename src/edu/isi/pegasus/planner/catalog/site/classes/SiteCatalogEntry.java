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

package edu.isi.pegasus.planner.catalog.site.classes;

import edu.isi.pegasus.planner.catalog.classes.Architecture;
import edu.isi.pegasus.planner.catalog.classes.OS;
import edu.isi.pegasus.planner.catalog.classes.Profiles;

import edu.isi.pegasus.planner.catalog.classes.Profiles.NAMESPACES;
import edu.isi.pegasus.planner.catalog.site.classes.GridGateway.JOB_TYPE;

import org.griphyn.common.classes.Arch;
import org.griphyn.common.classes.Os;
import org.griphyn.common.classes.SysInfo;

import org.griphyn.cPlanner.classes.Profile;

import org.griphyn.cPlanner.common.PegRandom;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import java.io.File;
import java.io.Writer;
import java.io.IOException;
import org.griphyn.cPlanner.namespace.Namespace;
import org.griphyn.cPlanner.namespace.VDS;
        
/**
 * This data class describes a site in the site catalog.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class SiteCatalogEntry extends AbstractSiteData{

    /**
     * The name of the environment variable PEGASUS_HOME.
     */
    public static final String PEGASUS_HOME = "PEGASUS_HOME";

    /**
     * The name of the environment variable VDS_HOME.
     */
    public static final String VDS_HOME = "VDS_HOME";
    
    /**
     * The map storing architecture to corresponding NMI architecture platforms.
     */
    private static Map< Architecture,Arch > mNMIArchToOldArch = null;
    
    /**
     * Singleton access to the  NMI arch to old arch map.
     * @return map
     */
    public static Map NMIArchToOldArch(){
        //singleton access
        if( mNMIArchToOldArch == null ){
            mNMIArchToOldArch = new HashMap< Architecture,Arch >();
            mNMIArchToOldArch.put( Architecture.x86, Arch.INTEL32  );
            mNMIArchToOldArch.put( Architecture.x86_64, Arch.INTEL64  );
            //mNMIArchToOldArch.put( Architecture.x86_64, Arch.AMD64 );
            
        }
        return mNMIArchToOldArch;
    }

    
    /**
     * The map storing OS to corresponding NMI OS platforms.
     */
    private static Map<OS,Os> mNMIOSToOldOS = null;
    
    /**
     * Singleton access to the os to NMI os map.
     * @return map
     */
    public static Map NMIOSToOldOS(){
        //singleton access
        if( mNMIOSToOldOS == null ){
            mNMIOSToOldOS = new HashMap<OS,Os>();
            //mNMIOSToOldOS.put( "rhas_3", Os.LINUX );
            mNMIOSToOldOS.put( OS.LINUX, Os.LINUX );
            mNMIOSToOldOS.put( OS.WINDOWS, Os.WINDOWS );
            mNMIOSToOldOS.put( OS.AIX, Os.AIX );
            mNMIOSToOldOS.put( OS.SUNOS, Os.SUNOS );

        }
        return mNMIOSToOldOS;
    }
    
     /**
     * The map storing architecture to corresponding NMI architecture platforms.
     */
    private static Map mArchToNMIArch = null;
    
    /**
     * Singleton access to the architecture to NMI arch map.
     * @return map
     */
    public static Map oldArchToNMIArch(){
        //singleton access
        if( mArchToNMIArch == null ){
            mArchToNMIArch = new HashMap();
            mArchToNMIArch.put( Arch.INTEL32, Architecture.x86 );
            mArchToNMIArch.put( Arch.INTEL64, Architecture.x86_64 );
            mArchToNMIArch.put( Arch.AMD64, Architecture.x86_64 );
            
        }
        return mArchToNMIArch;
    }

    
    /**
     * The map storing OS to corresponding NMI OS platforms.
     */
    private static Map mOSToNMIOS = null;
    
    /**
     * Singleton access to the os to NMI os map.
     * @return map
     */
    public static Map oldOSToNMIOS(){
        //singleton access
        if( mOSToNMIOS == null ){
            mOSToNMIOS = new HashMap();
            mOSToNMIOS.put( Os.LINUX, OS.LINUX );
            mOSToNMIOS.put( Os.AIX, OS.AIX );
            mOSToNMIOS.put( Os.SUNOS, OS.SUNOS );
            mOSToNMIOS.put( Os.WINDOWS, OS.WINDOWS );

        }
        return mOSToNMIOS;
    }
    
    /**
     * The site identifier. 
     */
    private String mID;
    
    /**
     * The OS of the site. 
     */
    private OS mOS;
    
    /**
     * The architecture of the site.
     */
    private Architecture mArch;
    
    /**
     * Optional information about the os release.
     */
    private String mOSRelease;
    
    /**
     * Optional information about the version.
     */
    private String mOSVersion;
    
    /**
     * Optional information about the glibc.
     */
    private String mGlibc;
    
    /**
     * The profiles asscociated with the site.
     */
    private Profiles mProfiles;
    
    /**
     * The handle to the head node filesystem.
     */
    private HeadNodeFS mHeadFS;
    
    /**
     * The handle to the worker node filesystem.
     */
    private WorkerNodeFS mWorkerFS;
    
    
    /**
     * Map of grid gateways at the site for submitting different job types.
     */
    private Map<GridGateway.JOB_TYPE, GridGateway> mGridGateways;
    
    /**
     * The list of replica catalog associated with the site.
     */
    private List<ReplicaCatalog> mReplicaCatalogs;

    /**
     * The default constructor.
     */
    public SiteCatalogEntry() {
        this( "" );
    }
    
    /**
     * The overloaded constructor.
     * 
     * @param id   the site identifier.
     */
    public SiteCatalogEntry( String id ) {
        initialize( id );
    }

    /**
     * Not implmented as yet.
     *
     * @return UnsupportedOperationException
     */
    public Iterator getFileServerIterator() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Not implemented as yet.
     *
     * @return UnsupportedOperationException
     */
    public List getFileServers() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Not implemented as yet
     *
     * @return UnsupportedOperationException
     */
    public List getGridGateways() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    /**
     * Initializes the object.
     * 
     * @param id   the site identifier.
     */
    public void initialize( String id ){        
        mID       = id;
        mArch     = Architecture.x86;
        mOS       = OS.LINUX;      
        mProfiles        = new Profiles();
        mGridGateways    = new HashMap();
        mReplicaCatalogs = new LinkedList();
    }
    
    /**
     * Sets the site handle for the site
     * 
     * @param id  the site identifier.
     */
    public void setSiteHandle( String id ){
        mID = id;
    }
    
    
    /**
     * Returns the site handle for the site
     * 
     * @return  the site identifier.
     */
    public String getSiteHandle( ){
        return mID;
    }
    
    /**
     * Sets the architecture of the site.
     * 
     * @param arch  the architecture.
     */
    public void setArchitecture( Architecture arch ){
        mArch = arch;
    }
    
    
    /**
     * Returns the architecture of the site.
     * 
     * @return  the architecture.
     */
    public Architecture getArchitecture( ){
        return mArch;
    }
    
    
    /**
     * Sets the OS of the site.
     * 
     * @param os the os of the site.
     */
    public void setOS( OS os ){
        mOS = os;
    }
    
    
    /**
     * Returns the OS of the site.
     * 
     * @return  the OS
     */
    public OS getOS( ){
        return mOS;
    }
    
    /**
     * Sets the sysinfo for the site.
     * 
     * @param  sysinfo
     */
    public void setSysInfo( SysInfo sysinfo ){
        this.setOSVersion( sysinfo.getOsversion() );
        this.setGlibc( sysinfo.getGlibc() );
        this.setOS( (OS)oldOSToNMIOS().get( sysinfo.getOs() ) );
        this.setArchitecture( (Architecture)oldArchToNMIArch().get( sysinfo.getArch() ) );
                            
    }
    /**
     * Returns the sysinfo for the site.
     * 
     * @return getSysInfo
     */
    public SysInfo getSysInfo(){
        return new SysInfo( (Arch)NMIArchToOldArch().get( this.getArchitecture() ),
                            (Os)NMIOSToOldOS().get( this.getOS() ),
                            this.getOSVersion(),
                            this.getGlibc() );
                            
    }
    
    /**
     * Sets the OS release of the site.
     * 
     * @param release the os releaseof the site.
     */
    public void setOSRelease( String release ){
        mOSRelease = release;
    }
    
    
    /**
     * Returns the OS release of the site.
     * 
     * @return  the OS
     */
    public String getOSRelease( ){
        return mOSRelease;
    }
    
    /**
     * Sets the OS version of the site.
     * 
     * @param version  the os versionof the site.
     */
    public void setOSVersion( String version ){
        mOSVersion = version;
    }
    
    
    /**
     * Returns the OS version of the site.
     * 
     * @return  the OS
     */
    public String getOSVersion( ){
        return mOSVersion;
    }
    
    /**
     * Sets the glibc version on the site.
     * 
     * @param version  the glibc version of the site.
     */
    public void setGlibc( String version ){
        mGlibc = version;
    }
    
    
    /**
     * Returns the glibc version of the site.
     * 
     * @return  the OS
     */
    public String getGlibc( ){
        return mGlibc;
    }
    
    /**
     * Sets the headnode filesystem.
     * 
     * @param system   the head node filesystem.
     */
    public void setHeadNodeFS( HeadNodeFS system ){
        mHeadFS = system;
    }
    
    
    /**
     * Returns the headnode filesystem.
     * 
     * @return   the head node filesystem.
     */
    public HeadNodeFS getHeadNodeFS(  ){
        return mHeadFS;
    }
    
    /**
     * Sets the worker node filesystem.
     * 
     * @param system   the head node filesystem.
     */
    public void setWorkerNodeFS( WorkerNodeFS system ){
        mWorkerFS = system;
    }
    
    
    /**
     * Returns the worker node filesystem.
     * 
     * @return   the worker node filesystem.
     */
    public WorkerNodeFS getWorkerNodeFS(  ){
        return mWorkerFS;
    }
    
    /**
     * Returns the work directory for the compute jobs on a site. 
     * 
     * Currently, the work directory is picked up from the head node shared filesystem.
     * 
     * @return the internal mount point.
     */
    public String getInternalMountPointOfWorkDirectory() {
        return this.getHeadNodeFS().getScratch().getSharedDirectory().getInternalMountPoint().getMountPoint();
    }
    
    /**
     * Adds a profile.
     * 
     * @param p  the profile to be added
     */
    public void addProfile( Profile p ){
        //retrieve the appropriate namespace and then add
       mProfiles.addProfile(  p );
    }
    
    /**
     * Sets the profiles associated with the file server.
     * 
     * @param profiles   the profiles.
     */
    public void setProfiles( Profiles profiles ){
        mProfiles = profiles;
    }
    
    /**
     * Returns the profiles associated with the site.
     * 
     * @return profiles.
     */
    public Profiles getProfiles( ){
        return mProfiles;
    }
    
    /**
     * Returns the value of VDS_HOME for a site.
     *
     * 
     * @return value if set else null.
     */
    public String getVDSHome( ){
        return this.getEnvironmentVariable( VDS_HOME );
    }


    /**
     * Returns the value of PEGASUS_HOME for a site.
     *
     * 
     * @return value if set else null.
     */
    public String getPegasusHome( ){
        return this.getEnvironmentVariable(  PEGASUS_HOME );
    }
    
    /**
     * Returns the default path to kickstart as constructed from the
     * environment variable.
     * 
     * @return value if set else null
     */
    public String getKickstartPath() {
        
        //check to see if user specified gridstart.path profile 
        //in the site catalog.
        String profile = (String) this.getProfiles().get( NAMESPACES.pegasus ).get( VDS.GRIDSTART_PATH_KEY );
        if( profile != null ){
            //return the path specified in profile
            return profile;
        }
        
        //try to construct the default path on basis of
        //PEGASUS_HOME environment variable.
        String home = this.getPegasusHome();
        if( home == null ){
            return null;
        }
        
        StringBuffer ks = new StringBuffer();
        ks.append( home ).append( File.separator ).
           append( "bin").append( File.separator ).
           append( "kickstart" );
        return ks.toString();
    }
    
    
    /**
     * Returns an environment variable associated with the site.
     *
     * @param variable  the environment variable whose value is required.
     *
     * @return value of the environment variable if found, else null
     */
    public String getEnvironmentVariable( String variable ){
        Namespace n = this.mProfiles.get( Profiles.NAMESPACES.env );
        return ( n == null ) ? null : (String)n.get( variable );
    }

    
    /**
     * Returns a grid gateway object corresponding to a job type.
     * 
     * @param type the job type
     * 
     * @return GridGateway
     */
    public GridGateway getGridGateway( GridGateway.JOB_TYPE type ){
        return mGridGateways.get( type );
    }
    
    /**
     * Selects a grid gateway object corresponding to a job type.
     * It also defaults to other GridGateways if grid gateway not found for
     * that job type.
     *
     * @param type the job type
     * 
     * @return GridGateway
     */
    public GridGateway selectGridGateway( GridGateway.JOB_TYPE type ){
        GridGateway g = this.getGridGateway( type );
        if( g == null ){
            if( type == JOB_TYPE.transfer || type == JOB_TYPE.cleanup || type == JOB_TYPE.register ){
                return this.selectGridGateway( JOB_TYPE.auxillary );
            }
            else if ( type == JOB_TYPE.auxillary ){
                return this.selectGridGateway( JOB_TYPE.compute );
            }
        }
        return g;
    }
    
    /**
     * A convenience method that selects a file server for staging the data out to 
     * a site. It returns the file server to which the generated data is staged
     * out / published.
     * 
     * The <code>FileServer</code> selected is associated with the HeadNode Filesystem.
     * 
     * @return the <code>FileServer</code> else null.
     */
    public FileServer selectStorageFileServerForStageout(){
        return ( this.getHeadNodeFS() == null )?
               null:
               this.getHeadNodeFS().selectStorageFileServerForStageout();
    }
    
    /**
     * Return an iterator to value set of the Map.
     * 
     * @return Iterator<GridGateway>
     */
    public Iterator<GridGateway> getGridGatewayIterator(){        
        return mGridGateways.values().iterator();
    }
    
    /**
     * Add a GridGateway to the site.
     * 
     * @param g   the grid gateway to be added.
     */
    public void addGridGateway( GridGateway g ){
        mGridGateways.put( g.getJobType(), g );
    }
    
    /**
     * This is a soft state remove, that removes a GridGateway from a particular
     * site. 
     * 
     * @param contact the contact string for the grid gateway.
     *
     * @return true if was able to remove the jobmanager from the cache
     *         false if unable to remove, or the matching entry is not found
     *         or if the implementing class does not maintain a soft state.
     */
    public boolean removeGridGateway( String contact ) {
        //iterate through the entry set
        for( Iterator it = this.mGridGateways.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Entry) it.next();
            GridGateway g = ( GridGateway )entry.getValue();
            if( g.getContact().equals( contact ) ) {
                it.remove();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Return an iterator to the replica catalog associated with the site.
     * 
     * @return Iterator<ReplicaCatalog>
     */
    public Iterator<ReplicaCatalog> getReplicaCatalogIterator(){        
        return mReplicaCatalogs.iterator();
    }
    
    /**
     * Add a Replica Catalog to the site.
     * 
     * @param catalog   the replica catalog to be added.
     */
    public void addReplicaCatalog( ReplicaCatalog catalog ){
        mReplicaCatalogs.add( catalog );
    }
    
    /**
     * Selects a Random ReplicaCatalog.
     *
     * @return <code>ReplicaCatalog</object> if more than one associates else
     *         returns null.
     */
    public ReplicaCatalog selectReplicaCatalog( ) {
        
        return ( this.mReplicaCatalogs == null || this.mReplicaCatalogs.size() == 0 )?
                 null :
                 this.mReplicaCatalogs.get(  PegRandom.getInteger( this.mReplicaCatalogs.size() - 1) );
    }

    
    /**
     * Writes out the xml description of the object. 
     *
     * @param writer is a Writer opened and ready for writing. This can also
     *               be a StringWriter for efficient output.
     * @param indent the indent to be used.
     *
     * @exception IOException if something fishy happens to the stream.
     */
    public void toXML( Writer writer, String indent ) throws IOException {
        String newLine = System.getProperty( "line.separator", "\r\n" );
        String newIndent = indent + "\t";
        
        //write out the  xml element
        writer.write( indent );
        writer.write( "<site " );        
        writeAttribute( writer, "handle", getSiteHandle() );
        writeAttribute( writer, "arch", getArchitecture().toString() );        
        writeAttribute( writer, "os", getOS().toString() );
       
        String val = null;
        if ( ( val = this.getOSRelease() ) != null ){
            writeAttribute( writer, "osrelease", val );
        }
        
        if ( ( val = this.getOSVersion() ) != null ){
            writeAttribute( writer, "osversion", val );
        }
         
        if ( ( val = this.getGlibc() ) != null ){
            writeAttribute( writer, "glibc", val );
        }
        
        writer.write( ">");
        writer.write( newLine );
        
        //list all the gridgateways
        for( Iterator<GridGateway> it = this.getGridGatewayIterator(); it.hasNext(); ){
            it.next().toXML( writer, newIndent );
        }
        
        HeadNodeFS fs = null;
        if( (fs = this.getHeadNodeFS()) != null ){
            fs.toXML( writer, newIndent );
        }
        
        
        WorkerNodeFS wfs = null;
        if( ( wfs = this.getWorkerNodeFS() ) != null ){
            wfs.toXML( writer, newIndent );
        }
        
        //list all the replica catalogs associate
        for( Iterator<ReplicaCatalog> it = this.getReplicaCatalogIterator(); it.hasNext(); ){
            it.next().toXML( writer, newIndent );
        }
        
        this.getProfiles().toXML( writer, newIndent );
        
        writer.write( indent );
        writer.write( "</site>" );
        writer.write( newLine );
    }

    /**
     * Returns the clone of the object.
     *
     * @return the clone
     */
    public Object clone(){
        SiteCatalogEntry obj;
        try{
            obj = ( SiteCatalogEntry ) super.clone();
            obj.initialize( this.getSiteHandle() );
            obj.setArchitecture( this.getArchitecture() );
            obj.setOS( this.getOS() );
        
            obj.setOSRelease( this.getOSRelease() );
            obj.setOSVersion( this.getOSVersion() );
            obj.setGlibc( this.getGlibc() );
        
            //list all the gridgateways
            for( Iterator<GridGateway> it = this.getGridGatewayIterator(); it.hasNext(); ){
                obj.addGridGateway( (GridGateway)it.next().clone() );
            }   
        
            HeadNodeFS fs = null;
            if( (fs = this.getHeadNodeFS()) != null ){
                obj.setHeadNodeFS( (HeadNodeFS)fs.clone() );
            }
        
            WorkerNodeFS wfs = null;
            if( ( wfs = this.getWorkerNodeFS() ) != null ){
                obj.setWorkerNodeFS( (WorkerNodeFS)wfs.clone() );
            }
        
            //list all the replica catalogs associate
            for( Iterator<ReplicaCatalog> it = this.getReplicaCatalogIterator(); it.hasNext(); ){
                obj.addReplicaCatalog( (ReplicaCatalog)it.next().clone( ) );
            }
        
            obj.setProfiles( (Profiles)this.mProfiles.clone() );
        
            
        }
        catch( CloneNotSupportedException e ){
            //somewhere in the hierarch chain clone is not implemented
            throw new RuntimeException("Clone not implemented in the base class of " + this.getClass().getName(),
                                       e );
        }
        return obj;
    }

   
    
    
    
}