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

import edu.isi.pegasus.common.logging.LogManagerFactory;
import edu.isi.pegasus.common.logging.LogManager;

import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * Parses the input stream and generates site configuration map as
 * output.
 *
 * @author Jens Vöckler
 * @author Gaurang Mehta
 * @author Karan Vahi
 * @version $Revision$
 *
 * @see org.griphyn.cPlanner.classes.PoolConfigScanner
 * @see org.griphyn.cPlanner.classes.PoolConfigToken
 */
public class PoolConfigParser2 {

    /**
     * The access to the lexical scanner is stored here.
     */
    private PoolConfigScanner m_scanner = null;

    /**
     * Stores the look-ahead symbol.
     */
    private PoolConfigToken m_lookAhead = null;

    /**
     * The handle to the logger used to log messages.
     */
    private LogManager m_logger;


    /**
     * Initializes the parser with an input stream to read from.
     * @param r is the stream opened for reading.
     *
     * @throws IOException
     * @throws PoolConfigException
     */
    public PoolConfigParser2(Reader r) throws IOException, PoolConfigException {
        m_logger  = LogManagerFactory.loadSingletonInstance();
        m_scanner = new PoolConfigScanner(r);
        m_lookAhead = m_scanner.nextToken();
    }

    /**
     * Parses the complete input stream, into the PoolConfig data object that
     * holds the contents of all the sites referred to in the stream.
     *
     * @return a map indexed by the site handle strings.
     * @throws IOException
     * @throws PoolConfigException
     * @throws Exception
     * @see org.griphyn.cPlanner.classes.PoolConfig
     */
    public PoolConfig parse() throws IOException,
        PoolConfigException, Exception {
        //to check more
        PoolConfig sites = new PoolConfig();
        String handle   = null;
        do {
            if (m_lookAhead != null) {
                //get the site handle/id, that is parsed differently
                //compared to the rest of the attributes of the site.
                handle = getSiteHandle();

                SiteInfo site = new SiteInfo();
                site.setInfo(SiteInfo.HANDLE, handle);
                while (! (m_lookAhead instanceof PoolConfigCloseBrace)) {
                    //populate all the rest of the attributes
                    //associated with the site
                    populate(site);
                }

                if (! (m_lookAhead instanceof PoolConfigCloseBrace)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                                                  "expecting a closing brace");
                }
                //we have information about one complete site!
                m_logger.log("Site parsed is - " + site.toMultiLine(),
                             LogManager.DEBUG_MESSAGE_LEVEL);

                m_lookAhead = m_scanner.nextToken();

                // enter the site information.
                if (sites.contains(handle)) {
                    //Karan October 13, 2005
                    //NEEDS CLARIFICATION FROM GAURANG
                    //PROBABLY IS A MDS ARTIFACT. ALSO NEEDS
                    //TO BE MOVED TO PoolConfig.add(PoolConfig,boolean)
                    java.util.Date date = new java.util.Date();
                    sites.add(handle + "-" + date.getTime(),
                                       site);
                }
                else {
                    sites.add(handle, site);
                }

            }
        }
        while (m_scanner.hasMoreTokens());

        return sites;
    }



    /**
     * Remove potential leading and trainling quotes from a string.
     *
     * @param input is a string which may have leading and trailing quotes
     * @return a string that is either identical to the input, or a
     * substring thereof.
     */
    public String niceString(String input) {
        // sanity
        if (input == null) {
            return input;
        }
        int l = input.length();
        if (l < 2) {
            return input;
        }

        // check for leading/trailing quotes
        if (input.charAt(0) == '"' && input.charAt(l - 1) == '"') {
            return input.substring(1, l - 1);
        }
        else {
            return input;
        }
    }

    /**
     * Populates all the attributes except the handle, associated with the site
     * in the <code>SiteInfo</code> object.
     *
     * @param site the <code>SiteInfo<code> object that is to be populated.
     * @throws even more mystery
     */
    private void populate(SiteInfo site) throws IOException,
        PoolConfigException, Exception {

        if (! (m_lookAhead instanceof PoolConfigReservedWord)) {
            throw new PoolConfigException(m_scanner.getLineNumber(),
                "expecting a reserved word describing a site attribute instead of "+
                m_lookAhead);
        }
        int word = ( (PoolConfigReservedWord) m_lookAhead).getValue();
        m_lookAhead = m_scanner.nextToken();

        switch (word) {
            case PoolConfigReservedWord.UNIVERSE:
                if (! (m_lookAhead instanceof PoolConfigIdentifier)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"universe\" requires an identifier as first argument");
                }
                JobManager jbminfo = new JobManager();
                String universe = ( (PoolConfigIdentifier) m_lookAhead).
                                    getValue();
                m_lookAhead = m_scanner.nextToken();
                jbminfo.setInfo(JobManager.UNIVERSE, universe);

                // System.out.println("universe="+universe );
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"universe\" requires a quoted string as second argument");
                }

                // System.out.println("url="+((PoolConfigQuotedString) m_lookAhead).getValue() );
                jbminfo.setInfo(JobManager.URL,
                                niceString( ( (PoolConfigQuotedString)
                                             m_lookAhead).getValue()));
                m_lookAhead = m_scanner.nextToken();

                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"universe\" requires a quoted string for version as third argument");
                }
                jbminfo.setInfo(JobManager.GLOBUS_VERSION,
                                niceString( ( (PoolConfigQuotedString)
                                             m_lookAhead).getValue()));
                m_lookAhead = m_scanner.nextToken();
                site.setInfo(SiteInfo.JOBMANAGER, jbminfo);
                break;

            case PoolConfigReservedWord.LRC:
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"lrc\" requires a quoted string argument");
                }
                LRC lrc = new LRC(niceString( ( (PoolConfigQuotedString)
                                               m_lookAhead).getValue()));
                site.setInfo(SiteInfo.LRC, lrc);
                m_lookAhead = m_scanner.nextToken();
                break;

            case PoolConfigReservedWord.GRIDLAUNCH:
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"gridlaunch\" requires a quoted string argument");
                }
                site.setInfo(SiteInfo.GRIDLAUNCH,
                                 niceString( ( (PoolConfigQuotedString)
                                              m_lookAhead).getValue()));
                m_lookAhead = m_scanner.nextToken();
                break;

            case PoolConfigReservedWord.WORKDIR:
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"workdir\" requires a quoted string argument");
                }
                WorkDir gdw = new WorkDir();
                gdw.setInfo(WorkDir.WORKDIR,
                            niceString( ( (PoolConfigQuotedString) m_lookAhead).
                                       getValue()));
                site.setInfo(SiteInfo.WORKDIR, gdw);

                //System.out.println("workdir ="+((PoolConfigQuotedString) m_lookAhead).getValue() );
                m_lookAhead = m_scanner.nextToken();
                break;

            case PoolConfigReservedWord.GRIDFTP:
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"gridftp\" requires a quoted string argument for url");
                }
                GridFTPServer gftp = new GridFTPServer();
                String gftp_url = new String(niceString( ( (
                    PoolConfigQuotedString) m_lookAhead).getValue()));
                StringTokenizer stt = new StringTokenizer(gftp_url, "/");
                String gridftpurl = stt.nextToken() + "//" + stt.nextToken();
                String storagedir = "";
                while (stt.hasMoreTokens()) {
                    storagedir += "/" + stt.nextToken();
                }
                gftp.setInfo(GridFTPServer.GRIDFTP_URL, gridftpurl);
                gftp.setInfo(GridFTPServer.STORAGE_DIR, storagedir);

                // System.out.println(" gridftp url="+((PoolConfigQuotedString) m_lookAhead).getValue() );
                m_lookAhead = m_scanner.nextToken();
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"gridftp\" requires a quoted string argument for globus version");
                }
                gftp.setInfo(GridFTPServer.GLOBUS_VERSION,
                             niceString( ( (PoolConfigQuotedString) m_lookAhead).
                                        getValue()));

                // System.out.println("version="+((PoolConfigQuotedString) m_lookAhead).getValue() );
                site.setInfo(SiteInfo.GRIDFTP, gftp);
                m_lookAhead = m_scanner.nextToken();
                break;

            case PoolConfigReservedWord.PROFILE:
                if (! (m_lookAhead instanceof PoolConfigIdentifier)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"profile\" requires a namespace identifier as first argument");
                }
                String namespace = ( (PoolConfigIdentifier) m_lookAhead).
                    getValue();
                m_lookAhead = m_scanner.nextToken();

                //  System.out.println("profile namespace="+namespace );
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"profile\" requires a quoted string argument");
                }
                String key = ( (PoolConfigQuotedString) m_lookAhead).getValue();

                //   System.out.println("key="+((PoolConfigQuotedString) m_lookAhead).getValue() );
                m_lookAhead = m_scanner.nextToken();
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"profile\" requires a quoted string argument");
                }
                String value = ( (PoolConfigQuotedString) m_lookAhead).getValue();

                //   System.out.println("value="+((PoolConfigQuotedString) m_lookAhead).getValue() );
                m_lookAhead = m_scanner.nextToken();
                Profile profile = new Profile(namespace,
                                              niceString(key), niceString(value));
                site.setInfo(SiteInfo.PROFILE, profile);
                break;

            case PoolConfigReservedWord.SYSINFO:
                if (! (m_lookAhead instanceof PoolConfigQuotedString)) {
                    throw new PoolConfigException(m_scanner.getLineNumber(),
                        "the \"sysinfo\" requires a quoted string argument");
                }
                String sysinfo = ( (PoolConfigQuotedString) m_lookAhead).
                    getValue();

                //   System.out.println("key="+((PoolConfigQuotedString) m_lookAhead).getValue() );
                m_lookAhead = m_scanner.nextToken();
                site.setInfo(SiteInfo.SYSINFO, niceString(sysinfo));
                break;

            default:
                throw new PoolConfigException(m_scanner.getLineNumber(),
                    "invalid reserved word used to configure a site entry");
        }
    }

    /**
     * Returns the site handle for a site, and moves the scanner to hold the next
     * <code>PoolConfigReservedWord</code>.
     *
     * @return  the site handle for a site, usually the name of the site.
     *
     * @throws plenty
     */
    private String getSiteHandle() throws IOException,
        PoolConfigException {
        String handle = null;
        if (! (m_lookAhead instanceof PoolConfigReservedWord) ||
            ( (PoolConfigReservedWord) m_lookAhead).getValue() !=
            PoolConfigReservedWord.SITE) {
            throw new PoolConfigException(m_scanner.getLineNumber(),
                                          "expecting reserved word \"site\"");
        }
        m_lookAhead = m_scanner.nextToken();

        // proceed with next token
        if (! (m_lookAhead instanceof PoolConfigIdentifier)) {
            throw new PoolConfigException(m_scanner.getLineNumber(),
                "expecting the pool handle identifier");
        }

        handle = ( (PoolConfigIdentifier) m_lookAhead).getValue();
        m_lookAhead = m_scanner.nextToken();

        // proceed with next token
        if (! (m_lookAhead instanceof PoolConfigOpenBrace)) {
            throw new PoolConfigException(m_scanner.getLineNumber(),
                                          "expecting an opening brace");
        }
        m_lookAhead = m_scanner.nextToken();
        return handle;
    }

}
