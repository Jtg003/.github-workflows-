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

package org.griphyn.common.catalog.transformation;

/**
 * This is the database implementation for the TC.
 *
 * @author Gaurang Mehta gmehta@isi.edu
 * @version $Revision$
 */

import edu.isi.pegasus.common.logging.LogManagerFactory;
import org.griphyn.cPlanner.classes.Profile;
import edu.isi.pegasus.common.logging.LogManager;
import org.griphyn.cPlanner.common.PegasusProperties;
import org.griphyn.common.catalog.TransformationCatalog;
import org.griphyn.common.catalog.TransformationCatalogEntry;
import org.griphyn.common.classes.SysInfo;
import org.griphyn.common.classes.TCType;
import org.griphyn.common.util.Separator;
import org.griphyn.vdl.dbschema.DatabaseSchema;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.griphyn.cPlanner.classes.PegasusBag;

public class Database
    extends DatabaseSchema implements TransformationCatalog {

    /**
     * The LogManager object which is
     * used to log all the messages.
     * It's values are set in the CPlanner class.
     */
    protected static LogManager mLogger =  LogManagerFactory.loadSingletonInstance();

    //   private static final String TC_MODE = "Database TC Mode";

    // private PegasusProperties mProps;

    private static Database mDatabaseTC = null;

    /**
     * Used for a singleton access to the implementation
     * 
     * @return instance to TransformationCatalog.
     * 
     * @deprecated
     *
     */
    public static TransformationCatalog getInstance() {
        try {
            if( mDatabaseTC == null ){
                PegasusBag bag = new PegasusBag();
                bag.add( PegasusBag.PEGASUS_LOGMANAGER,  LogManagerFactory.loadSingletonInstance() );
                mDatabaseTC = new Database();
                mDatabaseTC.initialize( bag );
            }
            return mDatabaseTC;
        }
        catch (Exception e) {
            mLogger.log(
                "Unable to create Database TC Instance", e,
                LogManager.ERROR_MESSAGE_LEVEL);
            return null;
        }
    }

    /**
     * Initialize the implementation, and return an instance of the implementation.
     * 
     * @param bag  the bag of Pegasus initialization objects.
     * 
     */
    public void initialize ( PegasusBag bag ){
        mLogger = bag.getLogger();
        
        try{
        /**
         * ADD SECTION
         */
        
        this.m_dbdriver.insertPreparedStatement("stmt.add.lfn",
                                                "INSERT INTO tc_logicaltx VALUES (?,?,?,?)");

        this.m_dbdriver.insertPreparedStatement("stmt.add.pfn",
                                                "INSERT INTO tc_physicaltx VALUES (?,?,?,?,?)");

        this.m_dbdriver.insertPreparedStatement("stmt.add.sysinfo",
                                                "INSERT INTO tc_sysinfo VALUES (?,?,?,?,?)");

        this.m_dbdriver.insertPreparedStatement("stmt.add.pfnprofile",
                                                "INSERT INTO tc_pfnprofile(namespace,name,value,pfnid) VALUES (?,?,?,?)");

        this.m_dbdriver.insertPreparedStatement("stmt.add.lfnprofile",
                                                "INSERT INTO tc_lfnprofile(namespace,name,value,lfnid) VALUES (?,?,?,?)");

        this.m_dbdriver.insertPreparedStatement("stmt.add.lfnpfnmap",
                                                "INSERT INTO tc_lfnpfnmap(lfnid,pfnid) VALUES (?,?)");

        /**
         * QUERY SECTION
         */

        this.m_dbdriver.insertPreparedStatement("stmt.query.lfns",
                                                "SELECT DISTINCT p.resourceid, l.namespace,l.name,l.version, p.type FROM " +
                                                "tc_logicaltx l, tc_physicaltx p, tc_lfnpfnmap m " +
                                                "WHERE l.id=m.lfnid and p.id=m.pfnid and p.resourceid like ? and p.type like ? " +
                                                "ORDER BY resourceid,name,namespace,version");

        this.m_dbdriver.insertPreparedStatement("stmt.query.pfns",
                                                "SELECT p.resourceid, p.pfn, p.type, s.architecture, s.os, " +
                                                "s.osversion, s.glibc " +
                                                "FROM tc_lfnpfnmap m, tc_logicaltx l, tc_physicaltx p, tc_sysinfo s " +
                                                "WHERE l.id=m.lfnid and p.id=m.pfnid and p.archid=s.id and " +
                                                "l.namespace=? and l.name=? and l.version=? and p.resourceid like ? and p.type like ? " +
                                                "ORDER by p.resourceid");

        this.m_dbdriver.insertPreparedStatement("stmt.query.resource",
                                                "SELECT DISTINCT p.resourceid FROM tc_logicaltx l, tc_physicaltx p, tc_lfnpfnmap m " +
                                                "WHERE l.id=m.lfnid and p.id=m.pfnid and l.namespace like ? and " +
                                                "l.name like ? and l.version like ? and p.type like ? ORDER by resourceid");

        this.m_dbdriver.insertPreparedStatement("stmt.query.lfnprofiles",
                                                "SELECT pr.namespace, pr.name, pr.value FROM " +
                                                "tc_logicaltx l, tc_lfnprofile pr WHERE " +
                                                "l.id=pr.lfnid and l.namespace=? and l.name=? and l.version=? ORDER BY namespace");

        this.m_dbdriver.insertPreparedStatement("stmt.query.pfnprofiles",
                                                "SELECT pr.namespace, pr.name, pr.value FROM tc_physicaltx p, tc_pfnprofile pr " +
                                                "WHERE p.id=pr.pfnid and p.pfn=? and p.resourceid like ? and " +
                                                "p.type like ? ORDER BY namespace");

        this.m_dbdriver.insertPreparedStatement("stmt.query.lfnid",
                                                "SELECT id FROM tc_logicaltx WHERE namespace=? and name=? and version=?");

        this.m_dbdriver.insertPreparedStatement("stmt.query.sysid",
                                                "SELECT id FROM tc_sysinfo WHERE " +
                                                "architecture=? and os=? and osversion=? and glibc=?");

        this.m_dbdriver.insertPreparedStatement("stmt.query.pfnid",
                                                "SELECT id FROM tc_physicaltx p WHERE p.pfn = ? and p.type like ? and p.resourceid like ?");

        this.m_dbdriver.insertPreparedStatement("stmt.query.lfnpfnmap",
                                                "SELECT * FROM tc_lfnpfnmap WHERE lfnid like ? and pfnid like ?");

        this.m_dbdriver.insertPreparedStatement("stmt.query.tc",
                                                "SELECT DISTINCT p.resourceid,l.namespace,l.name, l.version, p.pfn, p.type, " +
                                                "s.architecture, s.os, s.osversion, s.glibc " +
                                                "FROM tc_logicaltx l, tc_physicaltx p, tc_lfnpfnmap m, tc_sysinfo s " +
                                                "WHERE l.id=m.lfnid and p.id=m.pfnid and p.archid=s.id " +
                                                "ORDER by p.resourceid, l.name, l.namespace, l.version");

        this.m_dbdriver.insertPreparedStatement("stmt.query.lfnprofileid",
                                                "SELECT * FROM tc_lfnprofile WHERE " +
                                                "namespace=? and name = ? and value =? and lfnid=?");

        this.m_dbdriver.insertPreparedStatement("stmt.query.pfnprofileid",
                                                "SELECT * FROM tc_pfnprofile WHERE " +
                                                "namespace=? and name = ? and value =? and pfnid=?");

        this.m_dbdriver.insertPreparedStatement("stmt.query.pfnid2",
                                                "SELECT p.id FROM tc_physicaltx p,tc_lfnpfnmap m " +
                                                "WHERE m.pfnid=p.id and p.resourceid like ? and p.type like ? and m.lfnid=?");

        /**
         * DELETE SECTION
         */

        this.m_dbdriver.insertPreparedStatement("stmt.delete.alllfns",
                                                "DELETE FROM tc_logicaltx");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.alllfnpfnmap",
                                                "DELETE FROM tc_lfnpfnmap");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.allpfns",
                                                "DELETE FROM tc_physicaltx");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.allsysinfo",
                                                "DELETE FROM tc_sysinfo");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.allpfnprofile",
                                                "DELETE FROM tc_pfnprofile");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.alllfnprofile",
                                                "DELETE FROM tc_lfnprofile");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.byresourceid",
                                                "DELETE FROM tc_physicaltx WHERE resourceid=?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.lfnprofiles",
                                                "DELETE FROM tc_lfnprofile WHERE lfnid=?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.pfnprofiles",
                                                "DELETE FROM tc_pfnprofile WHERE pfnid=?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.lfnprofile",
                                                "DELETE FROM tc_lfnprofile WHERE namespace=? and name=? and value=? and lfnid=?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.pfnprofile",
                                                "DELETE FROM tc_pfnprofile WHERE namespace=? and name=? and value=? and pfnid=?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.bytype",
                                                "DELETE FROM tc_physicaltx WHERE type=? and resourceid like ?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.lfnpfnmap",
                                                "DELETE FROM tc_lfnpfnmap where lfnid like ? and pfnid like ?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.logicaltx",
                                                "DELETE FROM tc_logicaltx WHERE id=?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.physicaltx",
                                                "DELETE FROM tc_physicaltx WHERE id=?");

        this.m_dbdriver.insertPreparedStatement("stmt.delete.sysinfo",
                                                "DELETE FROM tc_sysinfo WHERE architecture=? and os=? and osversion=? and glibc=?");

        }
        catch ( SQLException sqe ){
            throw new RuntimeException( "SQL exception during initialization" + sqe );
        }
    }
    
    private Database() throws ClassNotFoundException,
        NoSuchMethodException, InstantiationException,
        IllegalAccessException, InvocationTargetException,
        SQLException, IOException {
        super( (String)null, "pegasus.catalog.transformation.db.schema");
        //mLogger = LogManager.getInstance();
        mLogger.log("TC Mode being used is " + this.getTCMode(),
                    LogManager.CONFIG_MESSAGE_LEVEL);

    }

    /**
     * Returns TC entries for a particular logical transformation and/or on a
     * particular resource and/or of a particular type.
     * @param namespace String The namespace of the logical transformation.
     * @param name String the name of the logical transformation.
     * @param version String The version of the logical transformation.
     * @param resourceid String The resourceid where the transformation is
     * located. If <B>NULL</B> it returns all resources.
     * @param type TCType The type of the transformation to search for.
     * If <B>NULL</b> it returns all types.
     * @throws Exception
     * @return List Returns a list of TCEntry objects containing the
     * corresponding entries from the TC. Returns null if no entry found.
     *  The profiles returned are lfn profiles, followed by pfn profiles.
     * The code does not check for similar keys between lfn and pfn profiles
     * and all profiles are kept
     * @see org.griphyn.common.classes.TCType
     * @see org.griphyn.common.catalog.TransformationCatalogEntry
     */

    public List getTCEntries(String namespace, String name, String version,
                             String resourceid, TCType type) throws Exception {
        List resultEntries = null;
        mLogger.log("Trying to get TCEntries for " +
                    Separator.combine(namespace, name, version) +
                    " on resource " +
                    ( (resourceid == null) ? "ALL" : resourceid) + " of type " +
                    ( (type == null) ? "ALL" : type.toString()),
                    LogManager.DEBUG_MESSAGE_LEVEL);

        List pfnentries = this.getTCPhysicalNames(namespace, name, version,
                                                  resourceid, type);
        if (pfnentries != null) {
            resultEntries = new ArrayList(pfnentries.size());
            List lfnprofiles = this.getTCLfnProfiles(namespace, name, version);
            for (int i = 0; i < pfnentries.size() - 1; i++) {
                String[] pfnresult = (String[]) pfnentries.get(i);
                String qresourceid = pfnresult[0];
                String qpfn = pfnresult[1];
                String qtype = pfnresult[2];
                SysInfo qsysinfo = new SysInfo(pfnresult[3]);
                List pfnprofiles = this.getTCPfnProfiles(qpfn, qresourceid,
                    TCType.fromString(qtype));
                TransformationCatalogEntry tc = new TransformationCatalogEntry(
                    namespace, name, version, pfnresult[0],
                    pfnresult[1],
                    TCType.fromString(pfnresult[2]), null, qsysinfo);

                try {
                    if (lfnprofiles != null) {
                        tc.setProfiles(lfnprofiles);
                    }
                    if (pfnprofiles != null) {
                        tc.setProfiles(pfnprofiles);
                    }
                }
                catch (RuntimeException e) {
                    mLogger.log(
                        "Ignoring errors while parsing profile in Transformation Catalog DB" +
                        " for " + Separator.combine(namespace, name, version),
                        e,
                        LogManager.WARNING_MESSAGE_LEVEL);

                }

                resultEntries.add(tc);
            }
        }
        return resultEntries;
    }

    /**
     * Returns TC entries for a particular logical transformation and/or on a
     * number of resources and/or of a particular type.
     * @param namespace String The namespace of the logical transformation.
     * @param name String the name of the logical transformation.
     * @param version String The version of the logical transformation.
     * @param resourceids List The List resourceid where the transformation is
     * located. If <b>NULL</b> it returns all resources.
     * @param type TCType The type of the transformation to search for.
     * If <b>NULL</b> it returns all types.
     * @throws Exception
     * @return List Returns a list of TCEntry objects containing the
     * corresponding entries from the TC. Returns null if no entry found.
     * @see org.griphyn.common.classes.TCType
     * @see org.griphyn.common.catalog.TransformationCatalogEntry
     */

    public List getTCEntries(String namespace, String name, String version,
                             List resourceids, TCType type) throws Exception {
        List results = null;
        if (resourceids != null) {
            for (Iterator i = resourceids.iterator(); i.hasNext(); ) {
                List tempresults = getTCEntries(namespace, name, version,
                                                (String) i.next(), type);
                if (tempresults != null) {
                    if (results == null) {
                        results = new ArrayList();
                    }
                    results.addAll(tempresults);
                }
            }
        }
        else {
            results = getTCEntries(namespace, name, version, (String)null,
                                   type);
        }
        return results;
    }

    /**
     * List all the contents of the TC in a column format.
     * @throws Exception
     * @return List Returns a List of String Arrays. Each string array contains
     * the resource, lfn, pfn, type, sysinfo and profiles.
     * The last entry in the list is an array of integers which contain the
     * column lengths for pretty print.
     */

    public List getTC() throws Exception {
        //get the statement.
        PreparedStatement ps = m_dbdriver.getPreparedStatement("stmt.query.tc");
        //execute the query
        ResultSet rs = ps.executeQuery();
        List result = new ArrayList();
        //       int[] count = {0, 0, 0, 0, 0};
        while (rs.next()) {
//            if ( result == null ) {
            //              result = new ArrayList();
            //        }
            //get the entries.
            String resourceid = rs.getString(1);
            String namespace = rs.getString(2);
            String name = rs.getString(3);
            String version = rs.getString(4);
            //          String lfn = Separator.combine( namespace, name, version );
            String pfn = rs.getString(5);
            String type = rs.getString(6);
            String sysinfo = new SysInfo(rs.getString(7), rs.getString(8),
                                         rs.getString(9), rs.getString(10)).
                toString();
            List pfnprofiles = this.getTCPfnProfiles(pfn, resourceid,
                TCType.fromString(type));
            List lfnprofiles = this.getTCLfnProfiles(namespace, name, version);
            //         String profiles = null;
            List allprofiles = null;
            if (lfnprofiles != null) {
                allprofiles = new ArrayList(lfnprofiles);
            }
            if (pfnprofiles != null) {
                if (allprofiles == null) {
                    allprofiles = new ArrayList(pfnprofiles);
                }
                else {
                    allprofiles.addAll(pfnprofiles);
                }
            }
            //get the profiles and construct a string out of them.
            //     if ( allprofiles != null ) {
            //            profiles = ProfileParser.combine( allprofiles );
            //      }

            //add them to the array.
            TransformationCatalogEntry tcentry = new TransformationCatalogEntry(
                namespace, name, version, resourceid, pfn,
                TCType.fromValue(type), allprofiles, new SysInfo(sysinfo));
            //caculate the max length of each column
//            columnLength( s, count );
            //add the array to the list to be returned.
            result.add(tcentry);
        }

        rs.close();
        //     if ( result != null ) {
        //set the column length info as the last element
        //       result.add( count );
        // }
        return result;
    }

    /**
     * Get the list of Resource ID's where a particular transformation may reside.
     * @param   namespace String The namespace of the transformation to search for.
     * @param   name      String The name of the transformation to search for.
     * @param   version   String The version of the transformation to search for.
     * @param   type      TCType The type of the transformation to search for.<BR>
     * (Enumerated type includes SOURCE, STATIC-BINARY, DYNAMIC-BINARY, PACMAN, INSTALLED, SCRIPT)<BR>
     * If <B>NULL</B> it returns all types.
     * @return  List      Returns a list of Resource Id's as strings.
     * Returns <B>NULL</B> if no results found.
     * @throws  Exception NotImplementedException if not implemented
     * @see org.griphyn.common.classes.TCType
     */

    public List getTCResourceIds(String namespace, String name,
                                 String version,
                                 TCType type) throws Exception {
        //get the statement
        PreparedStatement ps = m_dbdriver.getPreparedStatement(
            "stmt.query.resource");
        //set the parameters

        //if name is null then search for all lfn
        if (name == null) {
            ps.setString(1, "%");
            ps.setString(2, "%");
            ps.setString(3, "%");
        }
        else {
            ps.setString(1, makeNotNull(namespace));
            ps.setString(2, makeNotNull(name));
            ps.setString(3, makeNotNull(version));
        }
        //if type is null then search for all type
        String temp = (type != null) ? type.toString() : "%";
        ps.setString(4, temp);
        //execute the query.
        ResultSet rs = ps.executeQuery();

        List result = null;
        while (rs.next()) {
            if (result == null) {
                result = new ArrayList();
            }
            //add the results to the list.
            result.add(rs.getString(1));
        }

        rs.close();
        return result;
    }

    /**
     * Get the list of PhysicalNames for a particular transformation on a
     * site/sites for a particular type/types;
     * @param  namespace  String The namespace of the transformation to search for.
     * @param  name       String The name of the transformation to search for.
     * @param  version    String The version of the transformation to search for.
     * @param  resourceid String The id of the resource on which you want to
     * search. <BR>
     *                    If <B>NULL</B> then returns entries on all resources
     * @param  type       TCType The type of the transformation to search for. <BR>
     *                        (Enumerated type includes source, binary,
     * dynamic-binary, pacman, installed)<BR>
     *                        If <B>NULL</B> then returns entries of all types.
     * @throws Exception  NotImplementedException if not implemented.
     * @return List       Returns a list of String Arrays.
     *                    Each array contains the resourceid, the physical
     * transformation, the type of the tr and the systeminfo.
     *                    The last entry in the List is a int array containing t
     * he column lengths for pretty print.
     *                    Returns <B>NULL</B> if no results found.
     * @see org.griphyn.common.classes.TCType
     * @see org.griphyn.common.classes.SysInfo
     */

    public List getTCPhysicalNames(String namespace, String name,
                                   String version,
                                   String resourceid, TCType type) throws

        Exception {
        int[] count = {
            0, 0, 0};
        PreparedStatement ps = m_dbdriver.getPreparedStatement(
            "stmt.query.pfns");

        ps.setString(1, makeNotNull(namespace));
        ps.setString(2, makeNotNull(name));
        ps.setString(3, makeNotNull(version));

        String temp = (resourceid != null) ? resourceid : "%";
        ps.setString(4, temp);

        temp = (type != null) ? type.toString() : "%";
        ps.setString(5, temp);

        ResultSet rs = ps.executeQuery();

        List result = null;
        while (rs.next()) {
            if (result == null) {
                result = new ArrayList();
            }
            String[] s = {
                rs.getString(1), rs.getString(2), rs.getString(3),
                (new
                 SysInfo(rs.getString(4), rs.getString(5), rs.getString(6),
                         rs.getString(7))).toString()};
            result.add(s);
            columnLength(s, count);
        }
        rs.close();
        if (result != null) {
            result.add(count);
        }
        return result;
    }

    /**
     * Get the list of LogicalNames available on a particular resource.
     * @param resourceid String The id of the resource on which you want to search
     * @param type       TCType The type of the transformation to search for. <BR>
     *                  (Enumerated type includes source, binary, dynamic-binary,
     *  pacman, installed)<BR>
     *                   If <B>NULL</B> then return logical name for all types.
     * @throws Exception NotImpementedException if not implemented
     * @return List      Returns a list of String Arrays.
     *                   Each array contains the resourceid, logical transformation
     *                   in the format namespace::name:version and type.
     *                   The last entry in the list is an array of integers
     * specifying the column length for pretty print.
     *                   Returns <B>NULL</B> if no results found.
     */

    public List getTCLogicalNames(String resourceid, TCType type) throws
        Exception {
        int[] count = {
            0, 0};
        PreparedStatement ps = null;
        ps = this.m_dbdriver.getPreparedStatement("stmt.query.lfns");
        String temp = (resourceid == null) ? "%" : resourceid;
        ps.setString(1, temp);
        temp = (type == null) ? "%" : type.toString();
        ps.setString(2, temp);
        ResultSet rs = ps.executeQuery();

        List result = null;
        while (rs.next()) {
            if (result == null) {
                result = new ArrayList();
            }
            String[] st = {
                rs.getString(1),
                Separator.combine(rs.getString(2), rs.getString(3),
                                  rs.getString(4)), rs.getString(5)};
            columnLength(st, count);
            result.add(st);
        }
        rs.close();
        if (result != null) {
            result.add(count);
        }
        return result;
    }

    /**
     * Get the list of Profiles associated with a particular logical transformation.
     * @param namespace  String The namespace of the transformation to search for.
     * @param name       String The name of the transformation to search for.
     * @param version    String The version of the transformation to search for.
     * @throws Exception NotImplementedException if not implemented.
     * @return List      Returns a list of Profile Objects containing profiles
     * assocaited with the transformation.
     *                   Returns <B>NULL</B> if no profiles found.
     * @see org.griphyn.cPlanner.classes.Profile
     */

    public List getTCLfnProfiles(String namespace, String name, String version) throws
        Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.query.lfnprofiles");
        ps.setString(1, makeNotNull(namespace));
        ps.setString(2, makeNotNull(name));
        ps.setString(3, makeNotNull(version));
        ResultSet rs = ps.executeQuery();

        List result = null;
        while (rs.next()) {
            if (result == null) {
                result = new ArrayList();
            }

            Profile p = new Profile(rs.getString(1), rs.getString(2),
                                    rs.getString(3));
            result.add(p);
        }
        rs.close();

        return result;
    }

    /**
     * Get the list of Profiles associated with a particular physical transformation.
     * @param pfn        The physical file name to search the transformation by.
     * @param resourceid String The id of the resource on which you want to search.
     * @param type       TCType The type of the transformation to search for. <br>
     *                   (Enumerated type includes source, binary, dynamic-binary,
     *  pacman, installed)<br>
     * @throws Exception NotImplementedException if not implemented.
     * @return List      Returns a list of Profile Objects containing profiles
     * assocaited with the transformation.
     *                   Returns <B>NULL</B> if no profiless found.
     * @see org.griphyn.cPlanner.classes.Profile
     */

    public List getTCPfnProfiles(String pfn, String resourceid, TCType type) throws
        Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.query.pfnprofiles");
        ps.setString(1, pfn);
        String temp = (resourceid != null) ? resourceid : "%";
        ps.setString(2, temp);

        temp = (type != null) ? type.toString() : "%";
        ps.setString(3, temp);

        ResultSet rs = ps.executeQuery();

        List result = null;
        while (rs.next()) {
            if (result == null) {
                result = new ArrayList();
            }
            Profile p = new Profile(rs.getString(1), rs.getString(2),
                                    rs.getString(3));
            result.add(p);
        }
        rs.close();

        return result;
    }

    /**
     *  ADDITIONS
     */

    /**
     * Add multiple TCEntries to the Catalog.
     * @param tcentry List Takes a list of TCEntry objects as input
     * @throws Exception
     * @return boolean Return true if succesful, false if error. Exception is
     * thrown when error occurs.
     * @see org.griphyn.common.catalog.TransformationCatalogEntry
     */
    public boolean addTCEntry(List tcentry) throws
        Exception {
        for (int i = 0; i < tcentry.size(); i++) {
            TransformationCatalogEntry entry = ( (TransformationCatalogEntry)
                                                tcentry.get(i));
            this.addTCEntry(entry);
        }
        return true;
    }

    /**
     * Add a single TCEntry to the Catalog. Exception is thrown when error
     * occurs.
     *
     * @param entry a single {@link org.griphyn.common.catalog.TransformationCatalogEntry}
     * object as input.
     *
     * @return boolean Return true if succesful, false if error.
     *
     * @throws Exception
     * @see org.griphyn.common.catalog.TransformationCatalogEntry
     */
    public boolean addTCEntry(TransformationCatalogEntry entry) throws
        Exception {
        this.addTCEntry(entry.getLogicalNamespace(),
                        entry.getLogicalName(), entry.getLogicalVersion(),
                        entry.getPhysicalTransformation(),
                        entry.getType(), entry.getResourceId(), null,
                        entry.getProfiles(), entry.getSysInfo());
        return true;
    }

    /**
     * Add a single TCEntry to the Catalog. Exception is thrown when error
     * occurs. This method is a hack and wont commit the additions to the
     * backend catalog
     *
     * @param entry a single {@link org.griphyn.common.catalog.TransformationCatalogEntry}
     * object as input.
     * @param write boolean to commit additions to backend catalog.
     * @return boolean Return true if succesful, false if error.
     *
     * @throws Exception
     * @see org.griphyn.common.catalog.TransformationCatalogEntry
     */
    public boolean addTCEntry(TransformationCatalogEntry entry, boolean write) throws
        Exception {
        this.addTCEntry(entry.getLogicalNamespace(),
                        entry.getLogicalName(), entry.getLogicalVersion(),
                        entry.getPhysicalTransformation(),
                        entry.getType(), entry.getResourceId(), null,
                        entry.getProfiles(), entry.getSysInfo(), write);
        return true;
    }

    /**
     * Add an single entry into the transformation catalog.
     *
     * @param namespace    String The namespace of the transformation to be
     * added (Can be null)
     * @param name         String The name of the transformation to be added.
     *
     * @param version      String The version of the transformation to be added.
     * (Can be null)
     * @param physicalname String The physical name/location of the transformation
     *  to be added.
     *  @param type        TCType  The type of the physical transformation.
     * @param resourceid   String The resource location id where the
     * transformation is located.
     * @param lfnprofiles     List   The List of Profile objects associated with
     * a Logical Transformation. (can be null)
     * @param pfnprofiles     List   The List of Profile objects associated with
     * a Physical Transformation. (can be null)
     * @param system    SysInfo  The System information associated with a
     * physical transformation.
     * @throws Exception
     * @return boolean   Returns true if succesfully added, returns false if
     * error and throws exception.
     * @see org.griphyn.common.catalog.TransformationCatalogEntry
     * @see org.griphyn.common.classes.SysInfo
     * @see org.griphyn.cPlanner.classes.Profile
     */
    public boolean addTCEntry(String namespace, String name,
                              String version,
                              String physicalname, TCType type,
                              String resourceid,
                              List lfnprofiles, List pfnprofiles,
                              SysInfo system) throws
        Exception {
        return this.addTCEntry(namespace, name, version, physicalname, type,
                               resourceid, lfnprofiles, pfnprofiles, system, true);

    }

    /**
     * Add an single entry into the transformation catalog.
     *
     * @param namespace    String The namespace of the transformation to be
     * added (Can be null)
     * @param name         String The name of the transformation to be added.
     *
     * @param version      String The version of the transformation to be added.
     * (Can be null)
     * @param physicalname String The physical name/location of the transformation
     *  to be added.
     *  @param type        TCType  The type of the physical transformation.
     * @param resourceid   String The resource location id where the
     * transformation is located.
     * @param lfnprofiles     List   The List of Profile objects associated with
     * a Logical Transformation. (can be null)
     * @param pfnprofiles     List   The List of Profile objects associated with
     * a Physical Transformation. (can be null)
     * @param system    SysInfo  The System information associated with a
     * physical transformation.
     * @param write boolean to commit changes to the backend catalog
     * @throws Exception
     * @return boolean   Returns true if succesfully added, returns false if
     * error and throws exception.
     * @see org.griphyn.common.catalog.TransformationCatalogEntry
     * @see org.griphyn.common.classes.SysInfo
     * @see org.griphyn.cPlanner.classes.Profile
     */
    public boolean addTCEntry(String namespace, String name,
                              String version,
                              String physicalname, TCType type,
                              String resourceid,
                              List lfnprofiles, List pfnprofiles,
                              SysInfo system, boolean write) throws
        Exception {
if(!write) return false;
        //ADD LFN
        //try to add the logical name of the transformation
        long lfnid = -1;
        if ( (lfnid = this.getLogicalId(namespace, name, version)) == -1) {
            //the lfn does not exist so we can go ahead
            if ( (lfnid = this.addLogicalTr(namespace, name,
                                            version)) == -1) {
                return false;
            }
        }
        else {
            mLogger.log("Logical tr entry already exists",
                        LogManager.DEBUG_MESSAGE_LEVEL);
        }

        //ADD SYSINFO
        //now since the lfn is in lets check if the architecture info is there.
        long archid = -1;
        if ( (archid = this.getSysInfoId(system)) == -1) {
            //this means sytem information does not exist and we have to add it.
            if ( (archid = this.addSysInfo(system)) == -1) {
                return false;
            }
            // archid = this.getSysInfoId( system );
        }
        else {
            mLogger.log("Sysinfo entry already exists",
                        LogManager.DEBUG_MESSAGE_LEVEL);
        }

        //ADD PFN
        //now since the sysinfo is in the map lets add the pfn to the table.
        long pfnid = -1;
        if ( (pfnid = this.getPhysicalId(physicalname, type,
                                         resourceid)) == -1) {
            //since pfn is not in the database
            if ( (pfnid = this.addPhysicalTr(physicalname, resourceid, type,
                                             archid)) == -1) {
                return false;
            }
        }
        else {
            mLogger.log("PFN entry already exists",
                        LogManager.DEBUG_MESSAGE_LEVEL);
        }

        //ADD LFN-PFN MAP
        //now since the pfn is in lets check the lfnpfn map if it is correct
        if (this.checkLfnPfnMap(lfnid, pfnid) == -1) {
            //entry does not exist and we need to add it
            PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
                "stmt.add.lfnpfnmap");
            ps.setLong(1, lfnid);
            ps.setLong(2, pfnid);
            try {
                ps.executeUpdate();
            }
            catch (SQLException e) {
                mLogger.log(
                    "Unable to add lfn-pfn mapping entry to the TC :" +
                    e.getMessage(),
                    LogManager.ERROR_MESSAGE_LEVEL);
                m_dbdriver.cancelPreparedStatement("stmt.add.lfnpfnmap");
                m_dbdriver.rollback();
                return false;
            }
        }
        else {
            mLogger.log("LFN - PFN mapping entry already exists",
                        LogManager.DEBUG_MESSAGE_LEVEL);
        }

        //lfn pfn mapping is done now lets move to profiles.

        //ADD PFN PROFILES
        if (pfnprofiles != null) {
            for (Iterator i = pfnprofiles.iterator(); i.hasNext(); ) {
                Profile p = (Profile) i.next();
                this.addProfile(p, pfnid, true);
            }
        }

        //ADD LFN PROFILES
        if (lfnprofiles != null) {
            for (Iterator i = lfnprofiles.iterator(); i.hasNext(); ) {
                Profile p = (Profile) i.next();
                this.addProfile(p, lfnid, false);
            }
        }
        //everything seems to have gone ok.
        //so lets commit

            m_dbdriver.commit();

        mLogger.log("Added TC entry", LogManager.DEBUG_MESSAGE_LEVEL);
        return true;
    }

    /**
     *Add additional profile to a logical transformation .
     * @param namespace String The nsamespace of the transformation to be added. (can be null)
     * @param name      String The name of the transformation to be added.
     * @param version   String The version of the transformation to be added. (can be null)
     * @param profiles  List   The List of Profile objects that are to be added to the transformation.
     * @return boolean Returns true if success, false if error.
     * @throws Exception
     * @see org.griphyn.cPlanner.classes.Profile
     */
    public boolean addTCLfnProfile(String namespace, String name,
                                   String version, List profiles) throws
        Exception {
        long lfnid = -1;
        if ( (lfnid = getLogicalId(namespace, name, version)) != -1) {
            if (profiles != null) {
                for (Iterator i = profiles.iterator(); i.hasNext(); ) {
                    Profile p = (Profile) i.next();
                    this.addProfile(p, lfnid, false);
                }
            }
        }
        else {
            mLogger.log(
                "The lfn does not exist. Cannot add profiles to non existent lfn",
                LogManager.ERROR_MESSAGE_LEVEL);
        }
        m_dbdriver.commit();
        return true;
    }

    /**
     * Add additional profile to a physical transformation.
     * @param pfn  String       The physical name of the transformation
     * @param type      TCType The type of transformation that the profile is associated with.
     * @param resourceid String The resource on which the physical transformation exists
     * @param profiles  List   The List of Profile objects that are to be added to the transformation.
     * @return boolean Returns true for success, false for error.
     * @throws Exception
     * @see org.griphyn.cPlanner.classes.Profile
     */
    public boolean addTCPfnProfile(String pfn, TCType type, String resourceid,
                                   List profiles) throws Exception {
        long pfnid = -1;
        if ( (pfnid = getPhysicalId(pfn, type, resourceid)) != -1) {
            long profileid = -1;
            if (profiles != null) {
                for (Iterator i = profiles.iterator(); i.hasNext(); ) {
                    Profile p = (Profile) i.next();
                    this.addProfile(p, pfnid, true);
                }
            }
            m_dbdriver.commit();
            return true;
        }
        else {
            mLogger.log(
                "The pfn does not exist. Cannot add profiles to a non existent pfn",
                LogManager.ERROR_MESSAGE_LEVEL);
            return false;
        }
    }

    /**
     * DELETIONS
     */

    /**
     * Delete all entries in the transformation catalog for a give logical tranformation and/or on a resource and/or of a particular type
     * @param namespace   String The namespace of the transformation to be added. (can be null)
     * @param name        String The name of the transformation to be added.
     * @param version     String The version of the transformation to be added. ( can be null)
     * @param resourceid String The resource id for which the transformation is to be deleted.
     *                          If <B>NULL</B> then transformation on all resource are deleted
     * @param type TCType The type of the transformation. If <B>NULL</B> then all types are deleted for the transformation.
     * @throws Exception
     * @return boolean Returns true if success , false if there is any error.
     * @see org.griphyn.common.classes.TCType
     */

    public boolean deleteTCbyLogicalName(String namespace, String name,
                                         String version, String resourceid,
                                         TCType type) throws Exception {

        long lfnid;
        long[] pfnids;
        if (name != null) {
            //humm this is where we start.
            //get the logical transformation id first.
            if ( (lfnid = this.getLogicalId(namespace, name,
                                            version)) != -1) {
                //this means lfn is there now we try to get the list of pfnids.
                PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
                    "stmt.query.pfnid2");
                String temp = (resourceid == null) ? "%" : resourceid;
                ps.setString(1, temp);
                temp = (type == null) ? "%" : type.toString();
                ps.setString(2, temp);
                ps.setLong(3, lfnid);
                ResultSet rst = ps.executeQuery();
                List results = null;
                while (rst.next()) {
                    if (results == null) {
                        results = new ArrayList();
                    }
                    results.add(new Long(rst.getLong(1)));
                }
                if (results != null) {
                    pfnids = new long[results.size()];
                    for (int i = 0; i < results.size(); i++) {
                        pfnids[i] = ( (Long) results.get(i)).longValue();
                    }
                }
                else {
                    long[] t = {
                         -1};
                    pfnids = t;
                }
                if (pfnids[0] != -1) {
                    //this means there are pfns.
                    ps = this.m_dbdriver.getPreparedStatement(
                        "stmt.delete.lfnpfnmap");
                    ps.setLong(1, lfnid);
                    for (int i = 0; i < pfnids.length; i++) {
                        ps.setLong(2, pfnids[i]);
                        int rs = ps.executeUpdate();
                        if (rs < 1) {
                            mLogger.log(
                                "No entries found in the lfnpfnmap that could be deleted.",
                                LogManager.ERROR_MESSAGE_LEVEL);
                        }
                        else {
                            mLogger.log("Deleted " + rs +
                                        " mappings  from the lfnpfnmap",
                                        LogManager.DEBUG_MESSAGE_LEVEL);
                        }
                    }
                    //now since the map is deleted we check if the any lfn pfn mappings exist for that lfn
                    ps = this.m_dbdriver.getPreparedStatement(
                        "stmt.query.lfnpfnmap");
                    ps.setLong(1, lfnid);
                    ps.setString(2, "%");
                    rst = ps.executeQuery();
                    int count = 0;
                    while (rst.next()) {
                        count++;
                    }
                    if (count == 0) {
                        //no more pfns are mapped to the same lfn.
                        //It is safe to delete the lfn;
                        PreparedStatement pst = this.m_dbdriver.
                            getPreparedStatement("stmt.delete.logicaltx");
                        pst.setLong(1, lfnid);
                        int rs = pst.executeUpdate();
                        if (rs < 1) {
                            //this should not happen cause we checked that it existed before we started the work here.
                            mLogger.log(
                                "No entry for the lfn exists",
                                LogManager.ERROR_MESSAGE_LEVEL);
                        }
                        else {
                            mLogger.log(
                                "Deleted the logical transformation " +
                                Separator.combine(namespace, name, version),
                                LogManager.DEBUG_MESSAGE_LEVEL);
                        }
                    }
                    else {
                        mLogger.log("Logical transformation " +
                                    Separator.combine(namespace, name, version) +
                                    " is mapped to " + count +
                                    " other pfns.",
                                    LogManager.DEBUG_MESSAGE_LEVEL);
                        mLogger.log(
                            "***Wont delete logical transformation***",
                            LogManager.DEBUG_MESSAGE_LEVEL);
                    }
                    //now that we have handled the lfns lets handle the pfns.
                    for (int i = 0; i < pfnids.length; i++) {
                        ps.clearParameters();
                        ps.setString(1, "%");
                        ps.setLong(2, pfnids[i]);
                        rst = ps.executeQuery();
                        count = 0;
                        while (rst.next()) {
                            count++;
                        }
                        if (count == 0) {
                            //this means this pfn is not mapped to any other lfn and is safe to be deleted
                            PreparedStatement pst = this.m_dbdriver.
                                getPreparedStatement(
                                    "stmt.delete.physicaltx");
                            pst.setLong(1, pfnids[i]);
                            int rs = pst.executeUpdate();
                            if (rs < 1) {
                                //this should not happen cause we checked that pfn existed before we started the work here.
                                mLogger.log(
                                    "No entry for the pfn exists",
                                    LogManager.ERROR_MESSAGE_LEVEL);
                            }
                            else {
                                mLogger.log(
                                    "Deleted the physical transformation with id " +
                                    pfnids[i], LogManager.DEBUG_MESSAGE_LEVEL);
                            }
                        }
                        else {
                            mLogger.log("Pfn with id " + pfnids[i] +
                                        " is mapped with " + count +
                                        " other lfns.",
                                        LogManager.DEBUG_MESSAGE_LEVEL);
                            mLogger.log(
                                "***Wont delete physical transformation***",
                                LogManager.DEBUG_MESSAGE_LEVEL);
                        }
                    }
                    //hopefully everything went ok so lets commit
                    this.m_dbdriver.commit();
                    return true;
                }
                else {
                    mLogger.log(
                        "No pfns associated with lfn " +
                        Separator.combine(namespace, name,
                                          version) + " of type " +
                        ( (type == null) ? "ALL" : type.toString()) +
                        " on resource " +
                        ( (resourceid == null) ? "ALL" :
                         resourceid) + " exist", LogManager.ERROR_MESSAGE_LEVEL);
                    return false;
                }
            }
            else {
                //logical transformation does not exist
                mLogger.log(
                    "The logical transformation " +
                    Separator.combine(namespace, name, version) +
                    " does not exist.", LogManager.ERROR_MESSAGE_LEVEL);
                return false;
            }
        }
        else {
            mLogger.log(
                "The logical transformation name cannot be null.",
                LogManager.ERROR_MESSAGE_LEVEL);
            return false;
        }

    }

    /**
     * Delete all entries in the transformation catalog for pair of logical and physical transformation.
     * @param physicalname String The physical name of the transformation
     * @param namespace String The namespace assocaited in the logical name of the transformation.
     * @param name String The name of the logical transformation.
     * @param version String The version number of the logical transformation.
     * @param resourceid String The resource on which the transformation is to be deleted.
     *                          If <B>NULL</B> then it searches all the resource id.
     * @param type TCType The type of transformation. If <B>NULL</B> then it search and deletes entries for all types.
     * @throws Exception
     * @return boolean Returns true if sucess, false if any error occurs.
     * @see org.griphyn.common.classes.TCType
     */

    public boolean deleteTCbyPhysicalName(String physicalname,
                                          String namespace,
                                          String name, String version,
                                          String resourceid, TCType type) throws
        Exception {
        long lfnid;
        long[] pfnids;
        if (name != null) {
            if (physicalname != null) {
                //humm this is where we start.
                //get the logical transformation id first.
                if ( (lfnid = this.getLogicalId(namespace, name, version)) !=
                    -1) {
                    //this means lfn is there now we try to get the list of pfnids.
                    pfnids = this.getPhysicalIds(physicalname, type,
                                                 resourceid);
                    if (pfnids[0] != -1) {
                        //this means there are pfns.
                        PreparedStatement ps = this.m_dbdriver.
                            getPreparedStatement("stmt.delete.lfnpfnmap");
                        ps.setLong(1, lfnid);
                        for (int i = 0; i < pfnids.length; i++) {
                            ps.setLong(2, pfnids[i]);
                            int rs = ps.executeUpdate();
                            if (rs < 1) {
                                mLogger.log(
                                    "No entries found in the lfnpfnmap that could be deleted.",
                                    LogManager.ERROR_MESSAGE_LEVEL);
                            }
                            else {
                                mLogger.log("Deleted " + rs +
                                            " mappings  from the lfnpfnmap",
                                            LogManager.DEBUG_MESSAGE_LEVEL);
                            }
                        }
                        //now since the map is deleted we check if the any lfn pfn mappings exist for that lfn
                        ps = this.m_dbdriver.getPreparedStatement(
                            "stmt.query.lfnpfnmap");
                        ps.setLong(1, lfnid);
                        ps.setString(2, "%");
                        ResultSet rst = ps.executeQuery();
                        int count = 0;
                        while (rst.next()) {
                            count++;
                        }
                        if (count == 0) {
                            //no more pfns are mapped to the same lfn.
                            //It is safe to delete the lfn;
                            PreparedStatement pst = this.m_dbdriver.
                                getPreparedStatement("stmt.delete.logicaltx");
                            pst.setLong(1, lfnid);
                            int rs = pst.executeUpdate();
                            if (rs < 1) {
                                //this should not happen cause we checked that it existed before we started the work here.
                                mLogger.log(
                                    "No entry for the lfn exists",
                                    LogManager.DEBUG_MESSAGE_LEVEL);
                            }
                            else {
                                mLogger.log(
                                    "Deleted the logical transformation " +
                                    Separator.combine(namespace, name, version),
                                    LogManager.DEBUG_MESSAGE_LEVEL);
                            }
                        }
                        else {
                            mLogger.log("Logical transformation " +
                                        Separator.combine(namespace, name,
                                version) +
                                        " is mapped to " + count +
                                        " other pfns.",
                                        LogManager.DEBUG_MESSAGE_LEVEL);
                            mLogger.log(
                                "***Wont delete logical transformation***",
                                LogManager.DEBUG_MESSAGE_LEVEL);
                        }
                        //now that we have handled the lfns lets handle the pfns.
                        for (int i = 0; i < pfnids.length; i++) {
                            ps.clearParameters();
                            ps.setString(1, "%");
                            ps.setLong(2, pfnids[i]);
                            rst = ps.executeQuery();
                            count = 0;
                            while (rst.next()) {
                                count++;
                            }
                            if (count == 0) {
                                //this means this pfn is not mapped to any other lfn and is safe to be deleted
                                PreparedStatement pst = this.m_dbdriver.
                                    getPreparedStatement(
                                        "stmt.delete.physicaltx");
                                pst.setLong(1, pfnids[i]);
                                int rs = pst.executeUpdate();
                                if (rs < 1) {
                                    //this should not happen cause we checked that pfn existed before we started the work here.
                                    mLogger.log(
                                        "No entry for the pfn exists",
                                        LogManager.DEBUG_MESSAGE_LEVEL);
                                }
                                else {
                                    mLogger.log(
                                        "Deleted the physical transformation " +
                                        physicalname,
                                        LogManager.DEBUG_MESSAGE_LEVEL);
                                }
                            }
                            else {
                                mLogger.log("Pfn " + physicalname +
                                            " with id " + pfnids[i] +
                                            " is mapped with " + count +
                                            " other lfns.",
                                            LogManager.DEBUG_MESSAGE_LEVEL);
                                mLogger.log(
                                    "***Wont delete physical transformation***",
                                    LogManager.DEBUG_MESSAGE_LEVEL);
                            }
                        }
                        //hopefully everything went ok so lets commit
                        this.m_dbdriver.commit();
                        return true;
                    }
                    else {
                        mLogger.log(
                            "The physical transformation " +
                            physicalname + " of type " +
                            ( (type == null) ? "ALL" : type.toString()) +
                            " on resource " +
                            ( (resourceid == null) ? "ALL" : resourceid),
                            LogManager.ERROR_MESSAGE_LEVEL);
                        mLogger.log("does not exist",
                                    LogManager.ERROR_MESSAGE_LEVEL);
                        return false;
                    }
                }
                else {
                    //logical transformation does not exist
                    mLogger.log(
                        "The logical transformation " +
                        Separator.combine(namespace, name, version) +
                        " does not exist.", LogManager.ERROR_MESSAGE_LEVEL);
                    return false;
                }
            }
            else {
                mLogger.log(
                    "The physical transformation cannot be null.",
                    LogManager.ERROR_MESSAGE_LEVEL);
                return false;
            }
        }
        else {
            mLogger.log(
                "The logical transformation name cannot be null.",
                LogManager.ERROR_MESSAGE_LEVEL);
            return false;
        }
    }

    /**
     * Deletes entries from the catalog which have a particular system information.
     * @param sysinfo SysInfo The System Information by which you want to delete
     * @throws Exception
     * @return boolean Returns true for success, false if any error occurs.
     * @see org.griphyn.common.classes.SysInfo
     */
    public boolean deleteTCbySysInfo(SysInfo sysinfo) throws Exception {
        if (sysinfo == null) {
            mLogger.log(
                "The system information cannot be null",
                LogManager.ERROR_MESSAGE_LEVEL);
            return false;
        }
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.delete.sysinfo");
        ps.setString(1, this.makeNotNull(sysinfo.getArch().toString()));
        ps.setString(2, this.makeNotNull(sysinfo.getOs().toString()));
        ps.setString(3, this.makeNotNull(sysinfo.getOsversion()));
        ps.setString(4, this.makeNotNull(sysinfo.getGlibc()));
        try {
            int i = ps.executeUpdate();
            if (i < 1) {
                mLogger.log(
                    "No entries found that could be deleted",
                    LogManager.ERROR_MESSAGE_LEVEL);
                return false;
            }
            else {
                mLogger.log("Deleted " + i + "entry with system info " +
                            sysinfo.toString(), LogManager.DEBUG_MESSAGE_LEVEL);
            }
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to delete entries by SysInfo from TC :" + e.getMessage(),
                LogManager.ERROR_MESSAGE_LEVEL);
            this.m_dbdriver.rollback();
            return false;
        }
        this.m_dbdriver.commit();
        return true;

    }

    /**
     * Delete a paricular type of transformation, and/or on a particular resource
     * @param type TCType The type of the transformation
     * @param resourceid String The resource on which the transformation exists.
     *                           If <B>NULL</B> then that type of transformation is deleted from all the resources.
     * @throws Exception
     * @return boolean Returns true if success, false if any error occurs.
     * @see org.griphyn.common.classes.TCType
     */

    public boolean deleteTCbyType(TCType type, String resourceid) throws
        Exception {
        if (type == null) {
            mLogger.log(
                "The type of transformation cannot be null",
                LogManager.ERROR_MESSAGE_LEVEL);
            return false;
        }
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.delete.bytype");
        ps.setString(1, this.makeNotNull(type.toString()));
        String temp = (resourceid != null) ? resourceid : "%";
        ps.setString(2, temp);
        try {
            int i = ps.executeUpdate();
            if (i < 1) {
                mLogger.log(
                    "No entries found that could be deleted",
                    LogManager.ERROR_MESSAGE_LEVEL);
                return false;
            }
            else {
                mLogger.log("Deleted " + i +
                            " transformations with Type " + type +
                            " and on Resource " +
                            ( (resourceid == null) ? "ALL" : resourceid),
                            LogManager.DEBUG_MESSAGE_LEVEL);
            }
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to delete entries by type from TC :" + e.getMessage(),
                LogManager.ERROR_MESSAGE_LEVEL);
            this.m_dbdriver.rollback();
            return false;
        }
        this.m_dbdriver.commit();
        return true;
    }

    /**
     * Delete all entries on a particular resource from the transformation catalog.
     * @param resourceid String The resource which you want to remove.
     * @throws Exception
     * @return boolean  Returns true if successm false if any error occurs.
     */

    public boolean deleteTCbyResourceId(String resourceid) throws Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.delete.byresourceid");
        ps.setString(1, resourceid);
        try {
            int i = ps.executeUpdate();
            if (i < 1) {
                mLogger.log(
                    "No entries found that could be deleted",
                    LogManager.ERROR_MESSAGE_LEVEL);
                return false;
            }
            else {
                mLogger.log("Deleted " + i + " resources",
                            LogManager.INFO_MESSAGE_LEVEL);
            }
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to delete Resource from TC :" + e.getMessage(),
                LogManager.ERROR_MESSAGE_LEVEL);
            this.m_dbdriver.rollback();
            return false;
        }
        this.m_dbdriver.commit();
        return true;
    }

    /**
     * Deletes the entire transformation catalog. CLEAN............. USE WITH CAUTION.
     * @throws Exception
     * @return boolean Returns true if delete succeeds, false if any error occurs.
     */

    public boolean deleteTC() throws Exception {
        PreparedStatement[] ps = {
            this.m_dbdriver.getPreparedStatement("stmt.delete.alllfnpfnmap"),
            this.m_dbdriver.getPreparedStatement("stmt.delete.alllfnprofile"),
            this.m_dbdriver.getPreparedStatement("stmt.delete.allpfnprofile"),
            this.m_dbdriver.getPreparedStatement("stmt.delete.alllfns"),
            this.m_dbdriver.getPreparedStatement("stmt.delete.allpfns"),
            this.m_dbdriver.getPreparedStatement("stmt.delete.allsysinfo")
        };
        try {
            for (int i = 0; i < ps.length; i++) {
                ps[i].executeUpdate();
            }
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to delete the entire TC ", e,
                LogManager.ERROR_MESSAGE_LEVEL);
            this.m_dbdriver.rollback();
            return false;
        }
        //if we are here all succeeded and we should commit;
        this.m_dbdriver.commit();
        return true;
    }

    /**
     * Delete a list of profiles or all the profiles associated with a pfn on a
     *  resource and of a type.
     * @param physicalname String The physical name of the transformation.
     * @param type TCType The type of the transformation.
     * @param resourceid String The resource of the transformation.
     * @param profiles List The list of profiles to be deleted. If <B>NULL</B>
     * then all profiles for that pfn+resource+type are deleted.
     * @return boolean Returns true if success, false if any error occurs.
     * @throws Exception
     * @see org.griphyn.cPlanner.classes.Profile
     */
    public boolean deleteTCPfnProfile(String physicalname, TCType type,
                                      String resourceid, List profiles) throws
        Exception {
        long pfnid;
        if ( (pfnid = this.getPhysicalId(physicalname,
                                         type, resourceid)) !=
            -1) {
            if (profiles == null) {
                PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
                    "stmt.delete.pfnprofiles");
                ps.setLong(1, pfnid);
                try {
                    int i = ps.executeUpdate();
                    if (i < 1) {
                        mLogger.log(
                            "No entries found that could be deleted",
                            LogManager.ERROR_MESSAGE_LEVEL);
                        return false;
                    }
                    else {
                        mLogger.log("Deleted " + i + " pfn profiles",
                                    LogManager.INFO_MESSAGE_LEVEL);
                    }
                }
                catch (SQLException e) {
                    mLogger.log(
                        "Unable to delete pfnprofiles from TC", e,
                        LogManager.ERROR_MESSAGE_LEVEL);
                    this.m_dbdriver.rollback();
                    return false;
                }
            }
            else {
                for (Iterator i = profiles.iterator(); i.hasNext(); ) {
                    this.deleteProfile( (Profile) i.next(), pfnid, true);
                }
            }
            this.m_dbdriver.commit();
            return true;
        }
        else {
            mLogger.log("The pfn " + physicalname + " of type " +
                        type + " on resource " + resourceid +
                        " does not exist", LogManager.ERROR_MESSAGE_LEVEL);
        }
        return false;
    }

    /**
     * Delete a list of profiles or all the profiles associated with a logical
     *  transformation.
     * @param namespace String  The namespace of the logical transformation.
     * @param name String The name of the logical transformation.
     * @param version String The version of the logical transformation.
     * @param profiles List The List of profiles to be deleted. If <B>NULL</B>
     * then all profiles for the logical transformation are deleted.
     * @return boolean Returns true if success, false if any error occurs.
     * @throws Exception
     * @see org.griphyn.cPlanner.classes.Profile
     */
    public boolean deleteTCLfnProfile(String namespace, String name,
                                      String version, List profiles) throws
        Exception {
        long lfnid;
        if ( (lfnid = getLogicalId(namespace, name, version)) != -1) {
            if (profiles == null) {
                PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
                    "stmt.delete.lfnprofiles");
                ps.setLong(1, lfnid);
                try {
                    int i = ps.executeUpdate();
                    if (i < 1) {
                        mLogger.log(
                            "No entries found that could be deleted",
                            LogManager.ERROR_MESSAGE_LEVEL);
                        return false;
                    }
                    else {
                        mLogger.log("Deleted " + i + " lfn profiles",
                                    LogManager.INFO_MESSAGE_LEVEL);
                    }
                }
                catch (SQLException e) {
                    mLogger.log(
                        "Unable to delete lfnprofiles from TC", e,
                        LogManager.ERROR_MESSAGE_LEVEL);
                    this.m_dbdriver.rollback();
                    return false;
                }
            }
            else {
                for (Iterator i = profiles.iterator(); i.hasNext(); ) {
                    this.deleteProfile( (Profile) i.next(), lfnid, false);
                }
            }
            this.m_dbdriver.commit();
            return true;
        }
        else {
            mLogger.log("Logical transformation " +
                        Separator.combine(namespace, name, version) +
                        " does not exist", LogManager.ERROR_MESSAGE_LEVEL);
        }
        return false;
    }

    
    /**
     * Returns the TC implementation being used
     * @return String
     */
    public String getTCMode() {
        return new String(PegasusProperties.nonSingletonInstance().getTCMode());
    }

    public void close() {
        try {
            super.close();
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to call Close on the Database", e,
                LogManager.ERROR_MESSAGE_LEVEL);
        }
    }

    public boolean connect(java.util.Properties props) {
        //not implemented
        return true;
    }

    public boolean isClosed() {
        //not impelemented
        return true;
    }

    /**
     * Returns the id  associated with the logical transformation.
     * @param namespace String
     * @param name String
     * @param version String
     * @throws Exception
     * @return long Returns -1 if entry does not exist
     */
    private long getLogicalId(String namespace, String name, String version) throws
        Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.query.lfnid");
        ps.setString(1, this.makeNotNull(namespace));
        ps.setString(2, this.makeNotNull(name));
        ps.setString(3, this.makeNotNull(version));
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            long l = rs.getLong(1);
            rs.close();

            return l;
        }
        else {
            return -1;
        }
    }

    /**
     * Checks if a lfn,pfn exist in the map.
     * @param lfnid long
     * @param pfnid long
     * @throws Exception
     * @return long Returns 1 if exists, -1 if not exists
     */
    private long checkLfnPfnMap(long lfnid, long pfnid) throws Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.query.lfnpfnmap");
        ps.setLong(1, lfnid);
        ps.setLong(2, pfnid);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {

            rs.close();

            return 1;
        }
        else {
            return -1;
        }
    }

    /**
     * Checks if a given profile exists
     * @param namespace String
     * @param name String
     * @param value String
     * @param id long
     * @param pfn boolean
     * @throws Exception
     * @return long Returns 1 if exists , -1 if does not exist.
     */
    private long checkProfile(String namespace, String name, String value,
                              long id, boolean pfn) throws Exception {
        PreparedStatement ps = null;

        if (pfn) {
            ps = this.m_dbdriver.getPreparedStatement(
                "stmt.query.pfnprofileid");
        }
        else {
            ps = this.m_dbdriver.getPreparedStatement(
                "stmt.query.lfnprofileid");
        }
        ps.setString(1, makeNotNull(namespace));
        ps.setString(2, makeNotNull(name));
        ps.setString(3, makeNotNull(value));
        ps.setLong(4, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            rs.close();

            return 1;
        }
        else {
            return -1;
        }
    }

    /**
     * Gets the id for the system information entry.
     * @param system SysInfo
     * @throws Exception
     * @return long Returns -1 if it does not exist
     * @see org.griphyn.common.classes.SysInfo
     */
    private long getSysInfoId(SysInfo system) throws
        Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.query.sysid");
        ps.setString(1, system.getArch().getValue());
        ps.setString(2, system.getOs().getValue());
        ps.setString(3, this.makeNotNull(system.getOsversion()));
        ps.setString(4, this.makeNotNull(system.getGlibc()));
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            long l = rs.getLong(1);
            rs.close();
            return l;
        }
        else {
            return -1;
        }
    }

    /**
     * Adds a system information entry into the TC.
     * @param system SysInfo
     * @throws Exception
     * @return boolean Returns true if success, false if error occurs.
     */
    private long addSysInfo(SysInfo system) throws Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.add.sysinfo");
        long id = -1;
        try {
            id = m_dbdriver.sequence1("tc_sysinfo_id_seq");
        }
        catch (SQLException e) {
            mLogger.log("Error while getting sysinfoid : ", e,
                        LogManager.ERROR_MESSAGE_LEVEL);
            mLogger.log("Starting Rollback", LogManager.ERROR_MESSAGE_LEVEL);
            m_dbdriver.cancelPreparedStatement("stmt.add.sysinfo");
            m_dbdriver.rollback();
            mLogger.log("Finished Rollback", LogManager.ERROR_MESSAGE_LEVEL);
            return -1;
        }
        longOrNull(ps, 1, id);
        ps.setString(2, this.makeNotNull(system.getArch().getValue()));
        ps.setString(3, this.makeNotNull(system.getOs().getValue()));
        ps.setString(4, this.makeNotNull(system.getOsversion()));
        ps.setString(5, this.makeNotNull(system.getGlibc()));
        try {
            ps.executeUpdate();
            mLogger.log("Added the system info " + system.toString(),
                        LogManager.DEBUG_MESSAGE_LEVEL);
            if (id == -1) {
                id = m_dbdriver.sequence2(ps, "tc_sysinfo_id_seq", 1);
            }
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to add system info to the TC", e,
                LogManager.ERROR_MESSAGE_LEVEL);
            m_dbdriver.cancelPreparedStatement("stmt.add.sysinfo");
            m_dbdriver.rollback();
            return -1;
        }
        return id;
    }

    /**
     * Adds a logical entry to the logicaltx table
     * @param namespace String The namespace of the transformation
     * @param name String The name of the transformation
     * @param version String The version of the transformation
     * @return long   The position in table at which the entry is added.
     * If there is an error -1 is returned.
     * @throws Exception
     */
    private long addLogicalTr(String namespace, String name,
                              String version) throws Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.add.lfn");
        long id = -1;
        try {
            id = m_dbdriver.sequence1("tc_logicaltx_id_seq");
        }
        catch (SQLException e) {
            mLogger.log("Unable to get lfnid : ", e,
                        LogManager.ERROR_MESSAGE_LEVEL);
            mLogger.log("Starting Rollback", LogManager.ERROR_MESSAGE_LEVEL);
            m_dbdriver.cancelPreparedStatement("stmt.add.lfn");
            m_dbdriver.rollback();
            mLogger.log("Finished Rollback", LogManager.ERROR_MESSAGE_LEVEL);
            return -1;
        }
        longOrNull(ps, 1, id);
        //fill the data
        ps.setString(2, this.makeNotNull(namespace));
        ps.setString(3, this.makeNotNull(name));
        ps.setString(4, this.makeNotNull(version));
        try {
            //run the command
            ps.executeUpdate();
            mLogger.log("Added the lfn " + Separator.combine(namespace,
                name, version), LogManager.DEBUG_MESSAGE_LEVEL);
            if (id == -1) {
                id = m_dbdriver.sequence2(ps, "tc_logicaltx_id_seq", 1);
            }
            // lfnid = this.getLogicalId( namespace, name, version );
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to add logical transformation into the TC",
                e, LogManager.ERROR_MESSAGE_LEVEL);
            m_dbdriver.cancelPreparedStatement("stmt.add.lfn");
            m_dbdriver.rollback();
            return -1;
        }
        return id;
    }

    /**
     * Adds a physical entry to the physicaltxtable
     * @param physicalname String The physical name of the transformation
     * @param resourceid String The resource on which the transformation exists
     * @param type TCType  The type of the transformation
     * @param archid long  The architecture id from the sysinfo table for the tr.
     * @return long  The position in the physicaltx table at which the entry is
     * stored. If there is an error -1 is returned.
     * @throws Exception
     */
    private long addPhysicalTr(String physicalname, String resourceid,
                               TCType type, long archid) throws Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.add.pfn");
        long id = -1;
        try {
            id = m_dbdriver.sequence1("tc_physicaltx_id_seq");
        }
        catch (SQLException e) {
            mLogger.log("Error while getting pfnid : ", e,
                        LogManager.ERROR_MESSAGE_LEVEL);
            mLogger.log("Starting Rollback", LogManager.ERROR_MESSAGE_LEVEL);
            m_dbdriver.cancelPreparedStatement("stmt.add.pfn");
            m_dbdriver.rollback();
            mLogger.log("Finished Rollback", LogManager.ERROR_MESSAGE_LEVEL);
            return -1;
        }
        longOrNull(ps, 1, id);

        //this means the pfn entry does not exist and we have to add it.
        ps.setString(2, makeNotNull(resourceid));
        ps.setString(3, makeNotNull(physicalname));
        ps.setString(4, makeNotNull(type.toString()));
        ps.setLong(5, archid);
        try {
            ps.executeUpdate();
            mLogger.log("Added the pfn " + physicalname,
                        LogManager.DEBUG_MESSAGE_LEVEL);
            mLogger.log("Added the type " + type.toString(),
                        LogManager.DEBUG_MESSAGE_LEVEL);
            mLogger.log("Added the resource " + resourceid,
                        LogManager.DEBUG_MESSAGE_LEVEL);
            mLogger.log("Added the archid " + archid,
                        LogManager.DEBUG_MESSAGE_LEVEL);
            if (id == -1) {
                id = m_dbdriver.sequence2(ps, "tc_physicaltx_id_seq", 1);
            }
            // pfnid = this.getPhysicalId( physicalname, type, resourceid );
        }
        catch (SQLException e) {
            mLogger.log(
                "Unable to add pfn entry to the TC", e,
                LogManager.ERROR_MESSAGE_LEVEL);
            m_dbdriver.cancelPreparedStatement("stmt.add.pfn");
            m_dbdriver.rollback();
            return -1;
        }
        return id;
    }

    /**
     * Add a lfn or pfn profile to the TC
     * @param p Profile The profile to be added
     * @param id long The lfn or pfn id to which the profile is associated.
     * @param pfn boolean if true entry is added to the pfn, false to the lfn.
     * @throws Exception
     * @return boolean Returns true if success, false if error occurs.
     */
    private boolean addProfile(Profile p, long id, boolean pfn) throws
        Exception {
        String namespace = p.getProfileNamespace();
        String key = p.getProfileKey();
        String value = p.getProfileValue();
        long profileid = -1;
        PreparedStatement ps = null;
        if ( (profileid = this.checkProfile(namespace, key, value, id, pfn)) ==
            -1) {
            if (pfn) {
                //add pfn profile
                //the profile doesnt exist so lets add it in there.
                ps = m_dbdriver.getPreparedStatement("stmt.add.pfnprofile");
            }
            else {
                //add lfn profile
                ps = m_dbdriver.getPreparedStatement("stmt.add.lfnprofile");
            }
            ps.setString(1, makeNotNull(namespace));
            ps.setString(2, makeNotNull(key));
            ps.setString(3, makeNotNull(value));
            ps.setLong(4, id);
            try {
                ps.executeUpdate();

            }
            catch (SQLException e) {
                mLogger.log(
                    "Unable to add Profile to the TC", e,
                    LogManager.ERROR_MESSAGE_LEVEL);
                m_dbdriver.rollback();
                return false;
            }
            return true;
        }
        else {
            String temp = (pfn) ? "PFN" : "LFN";
            mLogger.log("The " + temp + "profile exists.",
                        LogManager.DEBUG_MESSAGE_LEVEL);
            return false;
        }

    }

    /**
     * Delete a given lfn or pfn profile
     * @param p Profile The profile to be deleted
     * @param id long The lfn or pfnid with which the profile is associated
     * @param pfn boolean If true the pfn profile is deleted, if false lfn profile is deleted
     * @throws Exception
     * @return boolean Returns true if success, false if any error occurs.
     */
    private boolean deleteProfile(Profile p, long id, boolean pfn) throws
        Exception {
        String namespace = p.getProfileNamespace();
        String key = p.getProfileKey();
        String value = p.getProfileValue();
        long profileid = -1;
        PreparedStatement ps = null;
        if ( (profileid = this.checkProfile(namespace, key, value, id, pfn)) !=
            -1) {
            if (pfn) {
                //delete pfn profile
                //the profile exists so lets delete it.
                ps = m_dbdriver.getPreparedStatement("stmt.delete.pfnprofile");
            }
            else {
                //delete lfn profile
                ps = m_dbdriver.getPreparedStatement("stmt.delete.lfnprofile");
            }
            ps.setString(1, makeNotNull(namespace));
            ps.setString(2, makeNotNull(key));
            ps.setString(3, makeNotNull(value));
            ps.setLong(4, id);
            try {
                ps.executeUpdate();

            }
            catch (SQLException e) {
                mLogger.log(
                    "Unable to delete Profile to the TC", e,
                    LogManager.ERROR_MESSAGE_LEVEL);
                m_dbdriver.rollback();
                return false;
            }
            return true;
        }
        else {
            String temp = (pfn) ? "PFN" : "LFN";
            mLogger.log("The " + pfn + " profile does not exist.",
                        LogManager.DEBUG_MESSAGE_LEVEL);
            return false;
        }

    }

    /**
     * Returns the id of the physical transformation
     * @param pfn String the physical transformation
     * @param type TCType The type of the transformation
     * @param resourceid String The resource on which the transformation exists.
     * @throws Exception
     * @return long Returns -1 if entry does not exist.
     */
    private long getPhysicalId(String pfn, TCType type, String resourceid) throws
        Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.query.pfnid");
        ps.setString(1, this.makeNotNull(pfn));
        ps.setString(2, this.makeNotNull(type.toString()));
        ps.setString(3, this.makeNotNull(resourceid));
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            long l = rs.getLong(1);

            rs.close();

            return l;
        }
        else {
            return -1;
        }
    }

    /**
     * Returns a list of pfnid for a given pfn on any resource, and of any type.
     * @param pfn String The physical transformation to search for.
     * @param type TCType The type to search for. If <B>NULL</B> then all types are searched.
     * @param resourceid String The resource to search for. If <B>NULL</B> then all resources are searched.
     * @throws Exception
     * @return long[] Returns -1 is no entry exist.
     */
    private long[] getPhysicalIds(String pfn, TCType type, String resourceid) throws
        Exception {
        PreparedStatement ps = this.m_dbdriver.getPreparedStatement(
            "stmt.query.pfnid");
        String temp = (type == null) ? "%" : type.toString();
        ps.setString(1, pfn);
        ps.setString(2, temp);
        temp = (resourceid == null) ? "%" : resourceid;
        ps.setString(3, temp);
        ResultSet rs = ps.executeQuery();
        List resultlist = null;
        while (rs.next()) {
            if (resultlist == null) {
                resultlist = new ArrayList();
            }
            resultlist.add(new Long(rs.getLong(1)));
        }
        if (resultlist != null) {
            long[] result = new long[resultlist.size()];
            int count = 0;
            for (Iterator i = resultlist.iterator(); i.hasNext(); ) {
                result[count] = ( (Long) i.next()).longValue();
                count++;
            }
            return result;
        }
        else {
            long[] result = {
                 -1};
            return result;
        }
    }

    /**
     * Computes the maximum column lenght for pretty printing.
     * @param s String[]
     * @param count int[]
     */
    private static void columnLength(String[] s, int[] count) {
        for (int i = 0; i < count.length; i++) {
            if (s[i].length() > count[i]) {
                count[i] = s[i].length();
            }
        }

    }

}
