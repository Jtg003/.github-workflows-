/*
 * 
 *   Copyright 2007-2008 University Of Southern California
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package edu.isi.pegasus.planner.parser;

import edu.isi.pegasus.common.logging.LogManagerFactory;
import edu.isi.pegasus.planner.catalog.classes.Architecture;
import edu.isi.pegasus.planner.catalog.classes.OS;

import edu.isi.pegasus.planner.catalog.site.classes.SiteData;
import edu.isi.pegasus.planner.catalog.site.classes.Connection;
import edu.isi.pegasus.planner.catalog.site.classes.FileServer;
import edu.isi.pegasus.planner.catalog.site.classes.GridGateway;
import edu.isi.pegasus.planner.catalog.site.classes.HeadNodeFS;
import edu.isi.pegasus.planner.catalog.site.classes.HeadNodeScratch;
import edu.isi.pegasus.planner.catalog.site.classes.HeadNodeStorage;
import edu.isi.pegasus.planner.catalog.site.classes.InternalMountPoint;
import edu.isi.pegasus.planner.catalog.site.classes.LocalDirectory;
import edu.isi.pegasus.planner.catalog.site.classes.ReplicaCatalog;
import edu.isi.pegasus.planner.catalog.site.classes.StorageType;
import edu.isi.pegasus.planner.catalog.site.classes.SharedDirectory;
import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;
import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;
import edu.isi.pegasus.planner.catalog.site.classes.WorkerNodeFS;
import edu.isi.pegasus.planner.catalog.site.classes.WorkerSharedDirectory;
import edu.isi.pegasus.planner.catalog.site.classes.WorkerNodeStorage;
import edu.isi.pegasus.planner.catalog.site.classes.WorkerNodeScratch;

import org.griphyn.cPlanner.classes.Profile;
import org.griphyn.cPlanner.namespace.Namespace;

import org.griphyn.cPlanner.parser.Parser;


import edu.isi.pegasus.common.logging.LogManager;
import org.griphyn.cPlanner.common.PegasusProperties;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;

import java.util.Iterator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This class uses the Xerces SAX2 parser to validate and parse an XML
 * document conforming to the Site Catalog schema v3.0
 * 
 * @author Karan Vahi vahi@isi.edu
 * @version $Revision$
 */
public class SiteCatalogParser extends Parser {

    /**
     * The "not-so-official" location URL of the Site Catalog Schema.
     */
    public static final String SCHEMA_LOCATION =
                                        "http://pegasus.isi.edu/schema/sc-3.0.xsd";

    /**
     * uri namespace
     */
    public static final String SCHEMA_NAMESPACE =
                                        "http://pegasus.isi.edu/schema/sitecatalog";

    /**
    * Count the depths of elements in the document
    */
    private int mDepth;
    
    /**
     * The stack of objects kept around.
     */
    private Stack mStack;
    
    /**
     * The final result constructed.
     */
    private SiteStore mResult;

    /**
     * The set of sites that need to be parsed.
     */
    private Set<String> mSites;
    
    /**
     * A boolean indicating whether to load all sites.
     */
    private boolean mLoadAll;
    
    /**
     * A boolean indicating that parsing is done.
     */
    private boolean mParsingDone;
    
    /**
     * The default Constructor.
     * 
     * @param sites   the list of sites to be parsed. * means all.
     *
     */
    public SiteCatalogParser( List<String> sites ) {
        this( PegasusProperties.nonSingletonInstance(), sites );
    }
    
    
    /**
     * The overloaded constructor.
     *
     * @param properties the <code>PegasusProperties</code> to be used.
     * @param sites the list of sites that need to be parsed. * means all.
     */
    public SiteCatalogParser( PegasusProperties properties, List<String> sites ) {
        super( properties );
        mStack = new Stack();
        mDepth = 0;
        
        mSites = new HashSet<String>();
        for( Iterator<String> it = sites.iterator(); it.hasNext(); ){
            mSites.add( it.next() );
        }
        mLoadAll = mSites.contains( "*" );                
        
        //setting the schema Locations
        String schemaLoc = getSchemaLocation();
        mLogger.log( "Picking schema for site catalog" + schemaLoc,
                     LogManager.CONFIG_MESSAGE_LEVEL);
        String list = SiteCatalogParser.SCHEMA_NAMESPACE + " " + schemaLoc;
        setSchemaLocations( list );
    }

    /**
     * Returns the constructed site store object
     * 
     * @return <code>SiteStore<code> if parsing completed 
     */
    public SiteStore getSiteStore() {
        if( mParsingDone ){
            return mResult;
        }
        else{
            throw new RuntimeException( "Parsing of file needs to complete before function can be called" );
        }
    }



    /**
     * The main method that starts the parsing.
     * 
     * @param file   the XML file to be parsed.
     */
    public void startParser( String file ) {
        try {
            this.testForFile( file );
            mParser.parse( file );
            
            //sanity check
            if ( mDepth != 0 ){
                throw new RuntimeException( "Invalid stack depth at end of parsing " + mDepth );
            }
            mLogger.log( "Object constructed is " + mResult.toXML(), 
                         LogManager.DEBUG_MESSAGE_LEVEL );
        } catch ( IOException ioe ) {
            mLogger.log( "IO Error :" + ioe.getMessage(),
                        LogManager.ERROR_MESSAGE_LEVEL );
        } catch ( SAXException se ) {

            if ( mLocator != null ) {
                mLogger.log( "Error in " + mLocator.getSystemId() +
                    " at line " + mLocator.getLineNumber() +
                    "at column " + mLocator.getColumnNumber() + " :" +
                    se.getMessage() , LogManager.ERROR_MESSAGE_LEVEL);
            }
        }
    }

    /**
     * 
     */
    public void endDocument() {
        mParsingDone = true;
    }

    

    
     /**
      * This method defines the action to take when the parser begins to parse
      * an element.
      *
      * @param namespaceURI is the URI of the namespace for the element
      * @param localName is the element name without namespace
      * @param qName is the element name as it appears in the docment
      * @param atts has the names and values of all the attributes
      */
    public void startElement( String namespaceURI,
                              String localName,
                              String qName,
                              Attributes atts ) throws SAXException{
        
       /* to be added later when logging is fixed.
        mLogger.log( "parser", 3,
	       "<" + map(namespaceURI) + localName + "> at " +
	       m_location.getLineNumber() + ":" +
	       m_location.getColumnNumber() );
         */

        //one more element level
        mDepth++;

        List names = new java.util.ArrayList();
        List values = new java.util.ArrayList();
        for ( int i=0; i < atts.getLength(); ++i ) {
            String name = new String( atts.getLocalName(i) );
            String value = new String( atts.getValue(i) );            
            names.add(name);
            values.add(value);
        }

        //System.out.println( "QNAME " + qName + " NAME " + names + "\t Values" + values );

        Object object = createObject( qName, names, values );
        if ( object != null ){
            mStack.push( new ParserStackElement( qName, object ) );
        }
        else{
            mLogger.log(
                    "Unknown element in xml :" + namespaceURI + ":" +
                    localName + ":" + qName, LogManager.ERROR_MESSAGE_LEVEL );
            
            throw new SAXException( "Unknown or Empty element while parsing" );
        }
    }

    /**
     * The parser is at the end of an element. Triggers the association of
     * the child elements with the appropriate parent elements.
     *
     * @param namespaceURI is the URI of the namespace for the element
     * @param localName is the element name without namespace
     * @param qName is the element name as it appears in the docment
     */  
    public void endElement( String namespaceURI,
                            String localName,
                            String qName )   throws SAXException{

        // that's it for this level
        mDepth--;
        mLogger.log( "</" +  localName + "> at " +
                     this.mLocator.getLineNumber() + ":" +
                     mLocator.getColumnNumber() , LogManager.DEBUG_MESSAGE_LEVEL );

        ParserStackElement tos = ( ParserStackElement ) mStack.pop();
        if ( ! qName.equals( tos.getElementName() ) ) {
            String error = "Top of Stack " + tos.getElementName() + " does not mactch " + qName;
            mLogger.log( error,
                         LogManager.FATAL_MESSAGE_LEVEL );
            throw new SAXException( error );
        }

        if ( ! mStack.empty() ) {
            // add pieces to lower levels
            ParserStackElement peek = ( ParserStackElement ) mStack.peek();
            
            if ( !setElementRelation( tos.getElementName(), peek.getElementObject(), tos.getElementObject() )){
                    mLogger.log( "Element " + tos.getElementName() +
                     		  " does not fit into element " + peek.getElementName(),
                                  LogManager.DEBUG_MESSAGE_LEVEL );
            }
            
        } else {
          // run finalizer, if available
          mLogger.log( "End of last element reached ",
                        LogManager.DEBUG_MESSAGE_LEVEL );
        }
        //reinitialize our cdata handler at end of each element
        mTextContent.setLength( 0 );    
  }
   
    
    /**
     * Composes the  <code>SiteData</code> object corresponding to the element
     * name in the XML document.
     * 
     * @param element the element name encountered while parsing.
     * @param names   is a list of attribute names, as strings.
     * @param values  is a list of attribute values, to match the key list.
     * 
     * @return the relevant SiteData object, else null if unable to construct.
     * 
     * @exception IllegalArgumentException if the element name is too short.
     */
    private Object createObject(String element, List names, List values) {
        if ( element == null || element.length() < 1 ){
            throw new IllegalArgumentException("illegal element length");
        }
        
        SiteData object = null;
        
        switch ( element.charAt(0) ) {
            // a alias
            case 'a':
                if ( element.equals( "alias" ) ) {
                    String alias = null;
                    for ( int i=0; i < names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "name" ) ) {
                            alias = value;
                	    this.log( element, name, value );                              
                        } else {
                	    this.complain( element, name, value );
                        }
                    } 
                    return alias;
                }
                else{
                    return null;
                }
                
            //c connection
            case 'c':
                if ( element.equals( "connection" ) ) {
                    Connection c = new Connection();
                    for ( int i=0; i < names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "key" ) ) {
                            c.setKey( value );
                	    this.log( element, name, value );                              
                        } else {
                	    this.complain( element, name, value );
                        }
                    } 
                    return c;
                }
                else{
                    return null;
                }
                
            //f
            case 'f':
                if( element.equals( "file-server" ) ){
                    FileServer fs = new FileServer();
                    for ( int i=0; i < names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "protocol" ) ) {
                            fs.setProtocol( value );
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "url" ) ) {
                            fs.setURLPrefix( value );
                 	    this.log( element, name, value );                              
                        }                        
                        else if ( name.equals( "mount-point" ) ) {
                            fs.setMountPoint( value );
                 	    this.log( element, name, value );                              
                        }
                        else {
                	      this.complain( element, name, value );
                        }
                    }
                    return fs;
                }
                else{
                    return null;
                }
                
            //g  grid
            case 'g':
                if( element.equals( "grid" ) ){
                    GridGateway gw = new GridGateway();
                    for ( int i=0; i<names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "arch") ){
                            gw.setArchitecture( Architecture.valueOf( value ));
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "type" ) ) {
                            gw.setType( GridGateway.TYPE.valueOf( value ) );
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "contact" ) ) {
                            gw.setContact( value );
                 	    this.log( element, name, value );                              
                        }                        
                        else if ( name.equals( "scheduler" ) ) {
                            gw.setScheduler( value );
                 	    this.log( element, name, value );                              
                        }                                    
                        else if ( name.equals( "jobtype" ) ) {
                            gw.setJobType( GridGateway.JOB_TYPE.valueOf( value ));
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "os" ) ){
                            gw.setOS( OS.valueOf( value ) );
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "osrelease" ) ){
                            gw.setOSRelease( value );
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "osversion" ) ){                            
                            gw.setOSVersion( value  );
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "glibc" ) ){
                            gw.setGlibc( value );                            
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "idle-nodes") ){
                            gw.setIdleNodes(name);
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "total-nodes") ){
                            gw.setTotalNodes(name);
                            this.log( element, name, value );                              
                        }
                        else {
                	      this.complain( element, name, value );
                        }
                    } 
                    return gw;
                }
                else{
                    return null;
                }
                
            //h head-fs
            case 'h':
                if( element.equals( "head-fs" ) ){
                    return new HeadNodeFS();
                }
                else{
                    return null;
                }
            
            //i  internal-mount-point
            case 'i':
                if( element.equals( "internal-mount-point" ) ){
                    InternalMountPoint imt = new InternalMountPoint();
                    for ( int i=0; i < names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "mount-point" ) ) {
                            imt.setMountPoint( value );
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "free-size" ) ) {
                            imt.setFreeSize( value );
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "total-size" ) ) {
                            imt.setTotalSize( value );
                 	    this.log( element, name, value );                              
                        }                        
                        else {
                	      this.complain( element, name, value );
                        }
                    }
                    return imt;
                }
                else{
                    return null;
                }
                
            //l local                 
            case 'l':
                if( element.equals( "local" ) ){
                    return new LocalDirectory();
                }
                else{
                    return null;
                }
                
            //p profile                 
            case 'p':
                if( element.equals( "profile" ) ){
                    Profile p = new Profile();
                    for ( int i=0; i < names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "namespace" ) ) {
                            p.setProfileNamespace( value );
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "key" ) ) {
                            p.setProfileKey( value );
                 	    this.log( element, name, value );                              
                        }
                        else {
                	    this.complain( element, name, value );
                        }
                    }
                    return p;
                }
                else{
                    return null;
                }
                
            //r replica-catalog
            case 'r':
                if( element.equals( "replica-catalog" ) ){
                    ReplicaCatalog rc = new ReplicaCatalog();
                    for ( int i=0; i < names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "type" ) ) {
                            rc.setType( value );
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "url" ) ) {
                            rc.setURL( value );
                 	    this.log( element, name, value );                              
                        }
                        else {
                	    this.complain( element, name, value );
                        }
                    }
                    return rc;
                }
                else{
                    return null;
                }
                
            //s shared scratch storage site
            case 's':
                if( element.equals( "shared" ) ){
                    return new SharedDirectory();
                }
                else if( element.equals( "scratch" ) || element.equals( "storage" ) ){
                    return new StorageType();//typecast later
                }
                else if( element.equals( "site" ) ){
                    SiteCatalogEntry site = new SiteCatalogEntry();
                    
                    for ( int i=0; i<names.size(); ++i ) {
                        String name = (String) names.get( i );
                        String value = (String) values.get( i );

                        if ( name.equals( "arch") ){
                            site.setArchitecture( Architecture.valueOf( value ));
                 	    this.log( element, name, value );                              
                        }
                        else if ( name.equals( "os") ){
                            site.setOS( OS.valueOf( value ) );
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "handle" ) ){
                            site.setSiteHandle( value );                            
                            this.log( element, name, value ); 
                        }
                        else if ( name.equals( "osrelease") ){
                            site.setOSRelease( value );
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "osversion") ){                            
                            site.setOSVersion( value  );
                            this.log( element, name, value );                              
                        }
                        else if ( name.equals( "glibc") ){
                            site.setGlibc( value );                            
                            this.log( element, name, value );                              
                        }
                        else {
                	      this.complain( element, name, value );
                        }
                    }
                    return site;
                }
                else if( element.equals( "sitecatalog" ) ){
                    SiteStore catalog = new SiteStore();
                    mResult = catalog;
                    return catalog;
                }
                else{
                    return null;
                }
                
            //w worker-fs
            case 'w':
                if( element.equals( "worker-fs" ) ){
                    return new WorkerNodeFS();
                }
                else if ( element.equals( "wshared" ) ){
                    return new WorkerSharedDirectory();
                }
                else{
                    return null;
                }
                
           
        }
        
        return object;
    }

    /**
     * Whether to laod a site or not in the <code>SiteStore</code>
     * 
     * @param site   the <code>SiteCatalogEntry</code> object.
     * 
     * @return boolean
     */
    private boolean loadSite(SiteCatalogEntry site) {
        return ( mLoadAll || mSites.contains( site.getSiteHandle() ));
    }

    /**
     * This method sets the relations between the currently finished XML
     * element and its containing element in terms of Java objects.
     * Usually it involves adding the object to the parent's child object
     * list.
     *
     * @param childElement name  is the  the child element name
     * @param parent is a reference to the parent's Java object
     * @param child is the completed child object to connect to the parent
     * 
     * @return true if the element was added successfully, false, if the
     *              child does not match into the parent.
     */
    private boolean setElementRelation( String childElement, Object parent, Object child ) {
    
        switch ( childElement.charAt( 0 ) ) {
            // a alias
            case 'a':
                //alias only appears in replica-catalog
                if ( child instanceof String && parent instanceof ReplicaCatalog ) {
                    ReplicaCatalog replica = ( ReplicaCatalog )parent;
                    replica.addAlias( (String)child );
                    return true;
                }
                else{
                    return false;
                }
                
            //c connection
            case 'c':
                //connection only appears in replica-catalog
                if ( child instanceof Connection && parent instanceof ReplicaCatalog ) {
                    ReplicaCatalog replica = ( ReplicaCatalog )parent;
                    Connection c = ( Connection )child;
                    c.setValue( mTextContent.toString().trim() );
                    replica.addConnection( c );
                    return true;
                }
                else{
                    return false;
                }
                
            //f
            case 'f':
                //file-server appears in local , shared, wshared
                if ( child instanceof FileServer && parent instanceof LocalDirectory ) {
                    LocalDirectory directory = ( LocalDirectory )parent;
                    directory.addFileServer( (FileServer)child );
                    return true;
                }
                else if ( child instanceof FileServer && parent instanceof SharedDirectory ) {
                    SharedDirectory directory = ( SharedDirectory )parent;
                    directory.addFileServer( (FileServer)child );
                    return true;
                }
                else if ( child instanceof FileServer && parent instanceof WorkerSharedDirectory ) {
                    WorkerSharedDirectory directory = ( WorkerSharedDirectory )parent;
                    directory.addFileServer( (FileServer)child );
                    return true;
                }
                else{
                    return false;
                }
                
            //g  grid
            case 'g':
                //grid only appears in the site element
                if ( child instanceof GridGateway && parent instanceof SiteCatalogEntry ) {
                    SiteCatalogEntry site = ( SiteCatalogEntry )parent;
                    site.addGridGateway( (GridGateway)child );
                    return true;
                }
                else{
                    return false;
                }
                
            //h head-fs
            case 'h':
                //head-fs only appears in the site element
                if ( child instanceof HeadNodeFS && parent instanceof SiteCatalogEntry ) {
                    SiteCatalogEntry site = ( SiteCatalogEntry )parent;
                    site.setHeadNodeFS( (HeadNodeFS)child );
                    return true;
                }
                else{
                    return false;
                }
            
            //i  internal-mount-point
            case 'i':
                //internal-mount-point appears in local , shared, wshared
                if ( child instanceof InternalMountPoint && parent instanceof LocalDirectory ) {
                    LocalDirectory directory = ( LocalDirectory )parent;
                    directory.setInternalMountPoint( (InternalMountPoint)child );
                    return true;
                }
                else if ( child instanceof InternalMountPoint && parent instanceof SharedDirectory ) {
                    SharedDirectory directory = ( SharedDirectory )parent;
                    directory.setInternalMountPoint( (InternalMountPoint)child );
                    return true;
                }
                else if ( child instanceof InternalMountPoint && parent instanceof WorkerSharedDirectory ) {
                    WorkerSharedDirectory directory = ( WorkerSharedDirectory )parent;
                    directory.setInternalMountPoint( (InternalMountPoint)child );
                    return true;
                }
                else{
                    return false;
                }
                
            //l local                 
            case 'l':
                //local appears in scratch and storage
                if ( child instanceof LocalDirectory &&
                     parent instanceof StorageType ) {
                    StorageType st = ( StorageType )parent;
                    st.setLocalDirectory( (LocalDirectory)child );
                    return true;
                }
                else{
                    return false;
                }
                
            //p profile                 
            case 'p':
                //profile appear in file-server site head-fs worker-fs
                if ( child instanceof Profile ){
                    Profile p = ( Profile ) child;
                    p.setProfileValue( mTextContent.toString().trim() );
                    mLogger.log( "Set Profile Value to " + p.getProfileValue(), LogManager.DEBUG_MESSAGE_LEVEL );
                    if ( parent instanceof FileServer ) {
                        FileServer server = ( FileServer )parent;
                        server.addProfile( p );
                        return true;
                    }
                    else  if ( parent instanceof HeadNodeFS ) {
                        HeadNodeFS fs = ( HeadNodeFS )parent;
                        fs.addProfile( p );
                        return true;
                    }
                    else if ( parent instanceof WorkerNodeFS ) {
                        WorkerNodeFS fs = ( WorkerNodeFS )parent;
                        fs.addProfile( p );
                        return true;
                    }
                    else if ( parent instanceof SiteCatalogEntry ){
                        SiteCatalogEntry s = ( SiteCatalogEntry )parent;
                        s.addProfile( p );
                    }
                }
                else{
                    return false;
                }
                
                
            //r replica-catalog                 
            case 'r':
                //replica-catalog appear in site
                if ( child instanceof ReplicaCatalog && parent instanceof SiteCatalogEntry ){
                    SiteCatalogEntry s = ( SiteCatalogEntry )parent;
                    s.addReplicaCatalog( (ReplicaCatalog)child );
                    return true;
                    
                }
                else{
                   return false;
                }
                
            //s shared scratch storage site site-catalog
            case 's':
                if ( child instanceof SharedDirectory ){
                    //shared appears in scratch and storage
                    if ( parent instanceof StorageType ) {
                        StorageType st = ( StorageType )parent;
                        st.setSharedDirectory( (SharedDirectory)child );
                        return true;
                    }
                }
                else if ( child instanceof StorageType && childElement.equals( "scratch" ) ){
                    //scratch appears in HeadNodeFS and WorkerNodeFS
                    StorageType scratch = ( StorageType )child;
                    
                     if ( parent instanceof HeadNodeFS ) {
                        HeadNodeFS fs = ( HeadNodeFS )parent;
                        fs.setScratch( new HeadNodeScratch(scratch) );
                        return true;
                    }
                    else if ( parent instanceof WorkerNodeFS ) {
                        WorkerNodeFS fs = ( WorkerNodeFS )parent;
                        fs.setScratch( new WorkerNodeScratch(scratch) );
                        return true;
                    }
                }
                else if ( child instanceof StorageType && childElement.equals( "storage" ) ){
                    //storage appears in HeadNodeFS and WorkerNodeFS
                    StorageType storage = ( StorageType )child;
                    
                     if ( parent instanceof HeadNodeFS ) {
                        HeadNodeFS fs = ( HeadNodeFS )parent;
                        fs.setStorage( new HeadNodeStorage( storage ) );
                        return true;
                    }
                    else if ( parent instanceof WorkerNodeFS ) {
                        WorkerNodeFS fs = ( WorkerNodeFS )parent;
                        fs.setStorage( new WorkerNodeStorage( storage ) );
                        return true;
                    }
                }
                else if( child instanceof SiteCatalogEntry && parent instanceof SiteStore ){
                    SiteStore c = ( SiteStore )parent;
                    
                    //add only to store if required.
                    SiteCatalogEntry site = (SiteCatalogEntry)child ;
                    if( loadSite( site ) ){
                        mLogger.log( "Loading site in SiteStore " + site.getSiteHandle(),
                                     LogManager.DEBUG_MESSAGE_LEVEL );
                        c.addEntry( site );
                    }
                }
                else if ( child instanceof SiteStore ){
                    //should never happen.
                    //XML totally messed up
                    mLogger.log( "sitecatalog element appears as child to " + parent,
                                 LogManager.ERROR_MESSAGE_LEVEL );
                    return false;
                  
                }
                else{
                    return false;
                }
                
            //w worker-fs wshared
            case 'w':
                //worker-fs appears in site
                if ( child instanceof WorkerNodeFS &&
                     parent instanceof SiteCatalogEntry ) {
                    SiteCatalogEntry site = ( SiteCatalogEntry )parent;
                    site.setWorkerNodeFS((WorkerNodeFS)child );
                    return true;
                }
                //wshared appears in shared scratch of worker node
                else if ( child instanceof WorkerSharedDirectory && parent instanceof WorkerNodeScratch ){
                    WorkerNodeScratch scratch = ( WorkerNodeScratch )parent;
                    scratch.setWorkerSharedDirectory( (WorkerSharedDirectory)child );
                }
                else if ( child instanceof WorkerSharedDirectory && parent instanceof WorkerNodeStorage ){
                    WorkerNodeStorage storage = ( WorkerNodeStorage )parent;
                    storage.setWorkerSharedDirectory( (WorkerSharedDirectory)child );
                }
                else{
                    return false;
                }
                
            default:
                return false;
        }
        
    }
    
    /**
     * Returns the local path to the XML schema against which to validate.
     * 
     * @return path to the schema
     */
    public String getSchemaLocation() {
        // treat URI as File, yes, I know - I need the basename
        File uri = new File( SiteCatalogParser.SCHEMA_LOCATION );
        // create a pointer to the default local position
        File poolconfig = new File( this.mProps.getSysConfDir(),  uri.getName() );

        return this.mProps.getPoolSchemaLocation( poolconfig.getAbsolutePath() );

    }

    /**
     * 
     * @param element
     * @param attribute
     * @param value
     */
    private void log( String element, String attribute, String value) {
        //to be enabled when logging per queue.
        mLogger.log( "For element " + element + " found " + attribute + " -> " + value,
                     LogManager.DEBUG_MESSAGE_LEVEL );
    }
    
    /**
     * 
     * @param element
     * @param attribute
     * @param value
     */
    private void complain(String element, String attribute, String value) {
        mLogger.log( "For element " + element + " invalid attribute found " + attribute + " -> " + value,
                     LogManager.ERROR_MESSAGE_LEVEL );
    }
    
    /**
     * 
     * @param args
     */
    public static void main( String[] args ){
        LogManagerFactory.loadSingletonInstance().setLevel( 5 );
        List s = new ArrayList(1);
        s.add( "*" );
        SiteCatalogParser parser = new SiteCatalogParser( s );
        if (args.length == 1) {
            parser.startParser( args[0] );
 
        } else {
            System.out.println("Usage: SiteCatalogParser <input site catalog xml file>");
        }
        
    }

    
}

