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


package org.griphyn.common.catalog.replica;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.griphyn.common.util.Boolean;
import org.griphyn.common.util.Escape;
import org.griphyn.common.util.Currently;
import org.griphyn.common.catalog.Catalog;
import org.griphyn.common.catalog.ReplicaCatalog;
import org.griphyn.common.catalog.ReplicaCatalogEntry;

/**
 * This class implements a replica catalog on top of a simple file which
 * contains two or more columns. It is neither transactionally safe, nor
 * advised to use for production purposes in any way. Multiple
 * concurrent instances <b>will clobber</b> each other!<p>
 *
 * The site attribute should be specified whenever possible. The
 * attribute key for the site attribute is "pool". For the shell
 * planner, its value will always be "local".<p>
 *
 * The class is permissive in what inputs it accepts. The LFN may or may
 * not be quoted. If it contains linear whitespace, quotes, backslash or
 * an equality sign, it must be quoted and escaped. Ditto for the PFN.
 * The attribute key-value pairs are separated by an equality sign
 * without any whitespaces. The value may be in quoted. The LFN
 * sentiments about quoting apply.<p>
 *
 * <pre>
 * LFN PFN
 * LFN PFN a=b [..]
 * LFN PFN a="b" [..]
 * "LFN w/LWS" "PFN w/LWS" [..]
 * </pre>
 *
 * The class is strict when producing (storing) results. The LFN and PFN
 * are only quoted and escaped, if necessary. The attribute values are
 * always quoted and escaped.
 *
 * @author Jens-S. V??ckler
 * @version $Revision$
 */
public class SimpleFile implements ReplicaCatalog
{
    
    /**
     * The name of the key that disables writing back to the cache file.
     * Designates a static file. i.e. read only
     */
    public static final String READ_ONLY_KEY = "read.only";
    
  /**
   * Records the quoting mode for LFNs and PFNs. If false, only quote as
   * necessary. If true, always quote all LFNs and PFNs.
   */
  protected boolean m_quote = false;

  /**
   * Records the name of the on-disk representation.
   */
  protected String m_filename = null;

  /**
   * Maintains a memory slurp of the file representation.
   */
  protected Map m_lfn = null;
  
  /**
   * A boolean indicating whether the catalog is read only or not.
   */
  boolean m_readonly;

  /**
   * Default empty constructor creates an object that is not yet connected
   * to any database. You must use support methods to connect before this
   * instance becomes usable.
   *
   * @see #connect( Properties )
   */
  public SimpleFile()
  {
    // make connection defunc
    m_lfn = null;
    m_filename = null;
    m_readonly = false;
  }

  /**
   * Provides the final states and associated messages.
   *
   * <pre>
   * ---+----+--------------------
   * F1 | 17 | final state, no record
   * F2 | 16 | final state, valid record
   * E1 | 18 | premature end
   * E2 | 19 | illegal character
   * E3 | 20 | incomplete record
   * E4 | 21 | unterminated string
   * </pre>
   */
  private static final String c_final[] =
  { "OK",
    "noop",
    "premature end of record",
    "illegal character @",
    "incomplete record",
    "missing closing quote" };

  /**
   * Contains the state transition tables. The notes a through c mark
   * similar states:
   * <pre>
   *      | EOS | lws |  =  | ""  | \\  | else|
   * -----+-----+-----+-----+-----+-----+-----+--------------
   *    0 | F1,-|  0,-|  E2 |  3,-|  E2 | 1,Sl| skip initial ws
   * a  1 |  E3 | 2,Fl|  E2 |  E2 |  E2 | 1,Sl| LFN w/o quotes
   *    2 |  E3 |  2,-|  E2 |  6,-|  E2 | 5,Sp| skip ws between LFN and PFN
   * b  3 |  E4 | 3,Sl| 3,Sl| 2,Fl|  4,-| 3,Sl| LFN in quotes
   * c  4 |  E4 | 3,Sl| 3,Sl| 3,Sl| 3,Sl| 3,Sl| LFN backslash escape
   * -----+-----+-----+-----+-----+-----+-----+--------------
   * a  5 |F2,Fp| 8,Fp|  E2 |  E2 |  E2 | 5,Sp| PFN w/o quotes
   * b  6 |  E4 | 6,Sp| 6,Sp| 8,Fp|  7,-| 6,Sp| PFN in quotes
   * c  7 |  E4 | 6,Sp| 6,Sp| 6,Sp| 6,Sp| 6,Sp| PFN backslash escape
   *    8 | F2,-|  8,-|  E2 |  E2 |  E2 | 9,Sk| skip ws before attributes
   *    9 |  E1 |  E2 |10,Fk|  E2 |  E2 | 9,Sk| attribute key
   *   10 |  E1 |  E2 |  E2 | 12,-|  E2 |11,Sv| equals sign
   * -----+-----+-----+-----+-----+-----+-----+--------------
   * a 11 |F2,Fv| 8,Fv|  E2 |  E2 |  E2 |11,Sv| value w/o quotes
   * b 12 |  E4 |12,Sv|12,Sv| 8,Fv| 13,-|12,Sv| value in quotes
   * c 13 |  E4 |12,Sv|12,Sv|12,Sv|12,Sv|12,Sv| value backslash escape
   * </pre>
   */
  private static final short c_state[][] =
  { { 17,  0, 19,  3, 19,  1 },   //  0
    { 20,  2, 19, 19, 19,  1 },   //  1
    { 20,  2, 19,  6, 19,  5 },   //  2
    { 21,  3,  3,  2,  4,  3 },   //  3
    { 21,  3,  3,  3,  3,  3 },   //  4

    { 16,  8, 19, 19, 19,  5 },   //  5
    { 21,  6,  6,  8,  7,  6 },   //  6
    { 21,  6,  6,  6,  6,  6 },   //  7
    { 16,  8, 19, 19, 19,  9 },   //  8
    { 18, 19, 10, 19, 19,  9 },   //  9
    { 18, 19, 19, 12, 19, 11 },   // 10

    { 16,  8, 19, 19, 19, 11 },   // 11
    { 21, 12, 12,  8, 13, 12 },   // 12
    { 21, 12, 12, 12, 12, 12 } }; // 13

  /**
   * Contains the actions to perform upon each state transition including
   * transition into self state.
   *
   * <pre>
   *    |   |
   * ---+---+-------------------------------------------
   *  - | 0 | no op
   *  S*| 1 | append to sb
   *  Fl| 2 | lfn := sb
   *  Fp| 3 | pfn := sb
   *  Fk| 4 | key := sb
   *  Fv| 5 | value := sb
   * </pre>
   */
  private static final short c_action[][] =
  { { 0, 0, 0, 0, 0, 1 },   //  0
    { 0, 2, 0, 0, 0, 1 },   //  1 a
    { 0, 0, 0, 0, 0, 1 },   //  2
    { 0, 1, 1, 2, 0, 1 },   //  3 b
    { 0, 1, 1, 1, 1, 1 },   //  4 c

    { 3, 3, 0, 0, 0, 1 },   //  5 a
    { 0, 1, 1, 3, 0, 1 },   //  6 b
    { 0, 1, 1, 1, 1, 1 },   //  7 c
    { 0, 0, 0, 0, 0, 1 },   //  8
    { 0, 0, 4, 0, 0, 1 },   //  9
    { 0, 0, 0, 0, 0, 1 },   // 10

    { 5, 5, 0, 0, 0, 1 },   // 11 a
    { 0, 1, 1, 5, 0, 1 },   // 12 b
    { 0, 1, 1, 1, 1, 1 } }; // 13 c

  /**
   * Parses a line from the file replica catalog
   *
   * @param line is the line to parse
   * @param lineno is the line number of this line
   * @return true if a valid element was generated
   */
  public boolean parse( String line, int lineno )
  {
    char ch = ' ';
    String lfn = null;
    String pfn = null;
    String key = null;
    Map attr = new TreeMap();
    short input, state = 0;
    int i = 0;
    StringBuffer sb = new StringBuffer();

    while ( state < 16 ) {
      if ( line.length() <= i ) { ch=' '; input=0; } // EOS
      else switch ( (ch=line.charAt(i)) ) {
	case ' ':  input=1; break;
	case '\t': input=1; break;
	case '=':  input=2; break;
	case '"':  input=3; break;
	case '\\': input=4; break;
	default:   input=5; break;
      }
      i++;

      // perform action
      switch ( c_action[state][input] ) {
      case 0: // noop
	break;
      case 1: // append to sb
	sb.append(ch);
	break;
      case 2: // sb to lfn
	lfn = sb.toString();
	sb = new StringBuffer();
	break;
      case 3: // sb to pfn
	pfn = sb.toString();
	sb = new StringBuffer();
	break;
      case 4: // sb to key
	key = sb.toString();
	sb = new StringBuffer();
	break;
      case 5: // sb to value
	attr.put( key, sb.toString() );
	sb = new StringBuffer();
	break;
      }

      // goto new state
      state = c_state[state][input];
    }

    if ( state > 17 ) {
      // error report
      sb = new StringBuffer(i+1);
      for ( int j=1; j<i; ++j ) sb.append(' ');
      sb.append('^');

      // FIXME: log it somewhere
      System.err.println( "While parsing line " + lineno + ": " +
			  c_final[state-16].replace('@',ch) +
			  ", ignoring line" );
      System.err.println( line );
      System.err.println( sb );
      return false;
    } else {
      // valid entry
      if ( state == 16 )
	insert( lfn, new ReplicaCatalogEntry( pfn, attr ) );
      return true;
    }
  }

  /**
   * Reads the on-disk map file into memory.
   *
   * @param filename is the name of the file to read.
   * @return true, if the in-memory data structures appear sound.
   */
  public boolean connect( String filename )
  {
    // sanity check
    if ( filename == null ) return false;
    m_filename = filename;
    m_lfn = new LinkedHashMap();

    try {
      File f = new File(filename);
      if ( f.exists() ) {
	LineNumberReader lnr = new LineNumberReader( new FileReader(f) );
	String line;
	while ( (line = lnr.readLine()) != null ) {
	  if ( line.length() == 0 || line.charAt(0) == '#' )
	    continue;
	  parse( line, lnr.getLineNumber() );
	}

	lnr.close();
      }
    } catch ( IOException ioe ) {
      m_lfn = null;
      m_filename = null;
      throw new RuntimeException(ioe); // re-throw
    }

    return true;
  }

  /**
   * Establishes a connection to the database from the properties.
   * You will need to specify a "file" property to point to the
   * location of the on-disk instance. If the property "quote" is
   * set to a true value, LFNs and PFNs are always quoted. By default,
   * and if false, LFNs and PFNs are only quoted as necessary.
   *
   * @param props is the property table with sufficient settings to
   * establish a link with the database.
   * @return true if connected, false if failed to connect.
   *
   * @throws Error subclasses for runtime errors in the class loader.
   */
  public boolean connect( Properties props )
  {
    // quote mode
    m_quote = Boolean.parse( props.getProperty("quote") );

    //update the m_writeable flag if specified
    if ( props.containsKey( SimpleFile.READ_ONLY_KEY ) ){
        m_readonly = Boolean.parse( props.getProperty( SimpleFile.READ_ONLY_KEY ),
                                     false );
    }
    
    if ( props.containsKey("file") )
      return connect( props.getProperty("file") );
    return false;
  }

  /**
   * Quotes a string only if necessary. This methods first determines,
   * if a strings requires quoting, because it contains whitespace, an
   * equality sign, quotes, or a backslash. If not, the string is not
   * quoted. If the input contains forbidden characters, it is placed
   * into quotes and quote and backslash are backslash escaped.<p>
   * However, if the property "quote" had a <code>true</code> value
   * when connecting to the database, output will always be quoted.
   *
   * @param e is the Escape instance used to escape strings.
   * @param s is the string that may require quoting
   * @return either the original string, or a newly allocated instance
   * to an escaped string.
   */
  public String quote( Escape e, String s )
  {
    String result = null;

    if ( s == null || s.length() == 0 ) {
      // empty string short-cut
      result = ( m_quote ? "\"\"" : s );
    } else {
      // string has content
      boolean flag = m_quote;
      for ( int i=0; i<s.length() && ! flag; ++i ) {
	// Note: loop will never trigger, if m_quote is true
	char ch = s.charAt(i);
	flag = ( ch == '"' || ch == '\\' || ch == '=' ||
		 Character.isWhitespace(ch) );
      }

      result = ( flag ? '"' + e.escape(s) + '"' : s );
    }

    // single point of exit
    return result;
  }

  /**
   * This operation will dump the in-memory representation back onto
   * disk. The store operation is strict in what it produces. The LFN
   * and PFN records are only quoted, if they require quotes, because
   * they contain special characters. The attributes are <b>always</b>
   * quoted and thus quote-escaped.
   */
  public void close()
  {
    String newline = System.getProperty("line.separator", "\r\n");
    Escape e = new Escape( "\"\\", '\\' );

    // sanity check
    if ( m_lfn == null ) return;
    
    
    //check if the file is writeable or not
    if( m_readonly ){
      m_lfn.clear();
      m_lfn = null;
      m_filename = null;
      return;
    }

    try {
      
        
      // open
      Writer out = new BufferedWriter(new FileWriter(m_filename));

      // write header
      out.write( "# file-based replica catalog: " +
		 Currently.iso8601(false,true,true,new Date()) );
      out.write( newline );

      // write data
      for ( Iterator i=m_lfn.keySet().iterator(); i.hasNext(); ) {
	String lfn = (String) i.next();
	Collection c = (Collection) m_lfn.get(lfn);
	if ( c != null ) {
	  for ( Iterator j=c.iterator(); j.hasNext(); ) {
	    ReplicaCatalogEntry rce = (ReplicaCatalogEntry) j.next();
	    out.write( quote(e,lfn) );
	    out.write( ' ' );
	    out.write( quote(e,rce.getPFN()) );
	    for ( Iterator k=rce.getAttributeIterator(); k.hasNext(); ) {
	      String key = (String) k.next();
	      String value = (String) rce.getAttribute(key);
	      out.write( ' ' );
	      out.write( key );
	      out.write( "=\"" );
	      out.write( e.escape(value) );
	      out.write( '"' );
	    }

	    // finalize record/line
	    out.write( newline );
	  }
	}
      }

      // close
      out.close();
    } catch ( IOException ioe ) {
      // FIXME: blurt message somewhere sane
      System.err.println( ioe.getMessage() );
    } finally {
      m_lfn.clear();
      m_lfn = null;
      m_filename = null;
    }
  }

  /**
   * Predicate to check, if the connection with the catalog's
   * implementation is still active. This helps determining, if it makes
   * sense to call <code>close()</code>.
   *
   * @return true, if the implementation is disassociated, false otherwise.
   * @see #close()
   */
  public boolean isClosed()
  {
    return ( m_lfn == null );
  }

  /**
   * Retrieves the entry for a given filename and site handle from the
   * replica catalog.
   *
   * @param lfn is the logical filename to obtain information for.
   * @param handle is the resource handle to obtain entries for.
   * @return the (first) matching physical filename, or
   * <code>null</code> if no match was found.
   */
  public String lookup( String lfn, String handle )
  {
    Collection c = (Collection) m_lfn.get(lfn);
    if ( c == null ) return null;

    for ( Iterator i=c.iterator(); i.hasNext(); ) {
      ReplicaCatalogEntry rce = (ReplicaCatalogEntry) i.next();
      String pool = rce.getResourceHandle();
      if ( pool == null && handle == null ||
	   pool != null && handle != null && pool.equals(handle) )
	return rce.getPFN();
    }
    return null;
  }

  /**
   * Retrieves all entries for a given LFN from the replica catalog.
   * Each entry in the result set is a tuple of a PFN and all its
   * attributes.
   *
   * @param lfn is the logical filename to obtain information for.
   * @return a collection of replica catalog entries
   * @see ReplicaCatalogEntry
   */
  public Collection lookup( String lfn )
  {
    Collection c = (Collection) m_lfn.get(lfn);
    if ( c == null ) return new ArrayList();
    else return new ArrayList(c);
  }

  /**
   * Retrieves all entries for a given LFN from the replica catalog.
   * Each entry in the result set is just a PFN string. Duplicates
   * are reduced through the set paradigm.
   *
   * @param lfn is the logical filename to obtain information for.
   * @return a set of PFN strings
   */
  public Set lookupNoAttributes( String lfn )
  {
    Set result = new TreeSet();
    Collection c = (Collection) m_lfn.get(lfn);

    if ( c != null ) {
      for ( Iterator i=c.iterator(); i.hasNext(); ) {
	result.add( ((ReplicaCatalogEntry) i.next()).getPFN() );
      }
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in an online display or portal.
   *
   * @param lfns is a set of logical filename strings to look up.
   * @return a map indexed by the LFN. Each value is a collection
   * of replica catalog entries for the LFN.
   * @see org.griphyn.common.catalog.ReplicaCatalogEntry
   */
  public Map lookup( Set lfns )
  {
    Map result = new HashMap();
    if ( lfns == null || lfns.size() == 0 ) return result;

    for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      Collection c = (Collection) m_lfn.get(lfn);
      if ( c == null ) result.put( lfn, new ArrayList() );
      else result.put( lfn, new ArrayList(c) );
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in an online display or portal.
   *
   * @param lfns is a set of logical filename strings to look up.
   * @return a map indexed by the LFN. Each value is a set
   * of PFN strings.
   */
  public Map lookupNoAttributes( Set lfns )
  {
    Map result = new HashMap();
    if ( lfns == null || lfns.size() == 0 ) return result;

    for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
      Set value = new TreeSet();
      String lfn = (String) i.next();
      Collection c = (Collection) m_lfn.get(lfn);
      if ( c != null ) {
	for ( Iterator j=c.iterator(); j.hasNext(); ) {
	  value.add( ((ReplicaCatalogEntry) j.next()).getPFN() );
	}
      }
      result.put( lfn, value );
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in online display or portal.<p>
   *
   * @param lfns is a set of logical filename strings to look up.
   * @param handle is the resource handle, restricting the LFNs.
   * @return a map indexed by the LFN. Each value is a collection
   * of replica catalog entries (all attributes).
   * @see ReplicaCatalogEntry
   */
  public Map lookup( Set lfns, String handle )
  {
    Map result = new HashMap();
    if ( lfns == null || lfns.size() == 0 ) return result;

    for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      Collection c = (Collection) m_lfn.get(lfn);
      if ( c != null ) {
	List value = new ArrayList();

	for ( Iterator j=c.iterator(); j.hasNext(); ) {
	  ReplicaCatalogEntry rce = (ReplicaCatalogEntry) j.next();
	  String pool = rce.getResourceHandle();
	  if ( pool == null && handle == null ||
	       pool != null && handle != null && pool.equals(handle) )
	    value.add( rce );
	}

	// only put found LFNs into result
	result.put( lfn, value );
      }
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in online display or portal.<p>
   *
   * @param lfns is a set of logical filename strings to look up.
   * @param handle is the resource handle, restricting the LFNs.
   * @return a map indexed by the LFN. Each value is a set of
   * physical filenames.
   */
  public Map lookupNoAttributes( Set lfns, String handle )
  {
    Map result = new HashMap();
    if ( lfns == null || lfns.size() == 0 ) return result;

    for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      Collection c = (Collection) m_lfn.get(lfn);
      if ( c != null ) {
	List value = new ArrayList();

	for ( Iterator j=c.iterator(); j.hasNext(); ) {
	  ReplicaCatalogEntry rce = (ReplicaCatalogEntry) j.next();
	  String pool = rce.getResourceHandle();
	  if ( pool == null && handle == null ||
	       pool != null && handle != null && pool.equals(handle) )
	    value.add( rce.getPFN() );
	}

	// only put found LFNs into result
	result.put( lfn, value );
      }
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in online display or portal.
   *
   * @param constraints is mapping of keys 'lfn', 'pfn', or any
   * attribute name, e.g. the resource handle 'pool', to a string that
   * has some meaning to the implementing system. This can be a SQL
   * wildcard for queries, or a regular expression for Java-based memory
   * collections. Unknown keys are ignored. Using an empty map requests
   * the complete catalog.
   * @return a map indexed by the LFN. Each value is a collection
   * of replica catalog entries.
   * @see ReplicaCatalogEntry
   */
  public Map lookup( Map constraints )
  {
    if ( constraints == null || constraints.size() == 0 ) {
      // return everything
      return Collections.unmodifiableMap(m_lfn);

    } else if ( constraints.size() == 1 && constraints.containsKey("lfn") ) {
      // return matching LFNs
      Pattern p = Pattern.compile( (String) constraints.get("lfn") );
      Map result = new HashMap();
      for ( Iterator i=m_lfn.entrySet().iterator(); i.hasNext(); ) {
	Map.Entry e = (Map.Entry) i.next();
	String lfn = (String) e.getKey();
	if ( p.matcher(lfn).matches() ) result.put( lfn, e.getValue() );
      }
      return result;

    } else {
      // FIXME: Implement!
      throw new RuntimeException( "method not implemented" );
    }
  }

  /**
   * Lists all logical filenames in the catalog.
   *
   * @return A set of all logical filenames known to the catalog.
   */
  public Set list()
  {
    return new TreeSet( m_lfn.keySet() );
  }

  /**
   * Lists a subset of all logical filenames in the catalog.
   *
   * @param constraint is a constraint for the logical filename only. It
   * is a string that has some meaning to the implementing system. This
   * can be a SQL wildcard for queries, or a regular expression for
   * Java-based memory collections.
   * @return A set of logical filenames that match. The set may be empty
   */
  public Set list( String constraint )
  {
    Set result = new TreeSet();
    Pattern p = Pattern.compile(constraint);

    for ( Iterator i=m_lfn.keySet().iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      if ( p.matcher(lfn).matches() ) result.add(lfn);
    }

    // done
    return result;
  }



  /**
   * Inserts a new mapping into the replica catalog. Any existing
   * mapping of the same LFN and PFN will be replaced, including all its
   * attributes.
   *
   * @param lfn is the logical filename under which to book the entry.
   * @param tuple is the physical filename and associated PFN attributes.
   *
   * @return number of insertions, should always be 1. On failure,
   * throw an exception, don't use zero.
   */
  public int insert( String lfn, ReplicaCatalogEntry tuple )
  {
    if ( lfn == null || tuple == null ) throw new NullPointerException();

    Collection c = null;
    if ( m_lfn.containsKey(lfn) ) {
      boolean seen = false;
      String pfn = tuple.getPFN();
      c = (Collection) m_lfn.get(lfn);
      for ( Iterator i=c.iterator(); i.hasNext() && ! seen; ) {
	ReplicaCatalogEntry rce = (ReplicaCatalogEntry) i.next();
	if ( (seen = pfn.equals(rce.getPFN())) ) {
	  try {
	    i.remove();
	  } catch ( UnsupportedOperationException uoe ) {
	    return 0;
	  }
	}
      }
    } else {
      c = new ArrayList();
      m_lfn.put( lfn, c );
    }
    c.add(tuple);

    return 1;
  }

  /**
   * Inserts a new mapping into the replica catalog. This is a
   * convenience function exposing the resource handle. Internally, the
   * <code>ReplicaCatalogEntry</code> element will be contructed, and
   * passed to the appropriate insert function.
   *
   * @param lfn is the logical filename under which to book the entry.
   * @param pfn is the physical filename associated with it.
   * @param handle is a resource handle where the PFN resides.
   * @return number of insertions, should always be 1. On failure,
   * throw an exception, don't use zero.
   * @see #insert( String, ReplicaCatalogEntry )
   * @see ReplicaCatalogEntry
   */
  public int insert( String lfn, String pfn, String handle )
  {
    if ( lfn == null || pfn == null || handle == null )
      throw new NullPointerException();
    return insert( lfn, new ReplicaCatalogEntry(pfn,handle) );
  }

  /**
   * Inserts multiple mappings into the replica catalog. The input is a
   * map indexed by the LFN. The value for each LFN key is a collection
   * of replica catalog entries. Note that this operation will replace
   * existing entries.
   *
   * @param x is a map from logical filename string to list of replica
   * catalog entries.
   * @return the number of insertions.
   * @see org.griphyn.common.catalog.ReplicaCatalogEntry
   */
  public int insert( Map x )
  {
    int result = 0;

    // shortcut sanity
    if ( x == null || x.size() == 0 ) return result;

    for ( Iterator i=x.keySet().iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      Object val = x.get(lfn);
      if ( val instanceof ReplicaCatalogEntry ) {
	// permit misconfigured clients
	result += insert( lfn, (ReplicaCatalogEntry) val );
      } else {
	// this is how it should have been
	for ( Iterator j=((Collection) val).iterator(); j.hasNext(); ) {
	  ReplicaCatalogEntry rce = (ReplicaCatalogEntry) j.next();
	  result += insert( lfn, rce );
	}
      }
    }

    return result;
  }

  /**
   * Deletes a specific mapping from the replica catalog. We don't care
   * about the resource handle. More than one entry could theoretically
   * be removed. Upon removal of an entry, all attributes associated
   * with the PFN also evaporate (cascading deletion).
   *
   * @param lfn is the logical filename in the tuple.
   * @param pfn is the physical filename in the tuple.
   * @return the number of removed entries.
   */
  public int delete( String lfn, String pfn )
  {
    int result = 0;
    if ( lfn == null || pfn == null ) return result;

    Collection c = (Collection) m_lfn.get(lfn);
    if ( c == null ) return result;

    List l = new ArrayList();
    for ( Iterator i=c.iterator(); i.hasNext(); ) {
      ReplicaCatalogEntry rce = (ReplicaCatalogEntry) i.next();
      if ( ! rce.getPFN().equals(pfn) ) l.add(rce);
    }

    // anything removed?
    if ( l.size() != c.size() ) {
      result = c.size() - l.size();
      m_lfn.put( lfn, l );
    }

    // done
    return result;
  }


  /**
   * Deletes multiple mappings into the replica catalog. The input is a
   * map indexed by the LFN. The value for each LFN key is a collection
   * of replica catalog entries. On setting matchAttributes to false, all entries
   * having matching lfn pfn mapping to an entry in the Map are deleted.
   * However, upon removal of an entry, all attributes associated with the pfn
   * also evaporate (cascaded deletion).
   *
   * @param x                is a map from logical filename string to list of
   *                         replica catalog entries.
   * @param matchAttributes  whether mapping should be deleted only if all
   *                         attributes match.
   *
   * @return the number of deletions.
   * @see ReplicaCatalogEntry
   */
  public int delete( Map x , boolean matchAttributes){
      throw new java.lang.UnsupportedOperationException
                               ("delete(Map,boolean) not implemented as yet");
  }


  /**
   * Attempts to see, if all keys in the partial replica catalog entry are
   * contained in the full replica catalog entry.
   *
   * @param full is the full entry to check against.
   * @param part is the partial entry to check with.
   * @return true, if contained, false if not contained.
   */
  private boolean matchMe( ReplicaCatalogEntry full, ReplicaCatalogEntry part )
  {
    if ( full.getPFN().equals( part.getPFN() ) ) {
      for ( Iterator i=part.getAttributeIterator(); i.hasNext(); ) {
	if ( ! full.hasAttribute((String) i.next()) ) return false;
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Deletes a very specific mapping from the replica catalog. The LFN
   * must be matches, the PFN, and all PFN attributes specified in the
   * replica catalog entry. More than one entry could theoretically be
   * removed. Upon removal of an entry, all attributes associated with
   * the PFN also evaporate (cascading deletion).
   *
   * @param lfn is the logical filename in the tuple.
   * @param tuple is a description of the PFN and its attributes.
   * @return the number of removed entries, either 0 or 1.
   */
  public int delete( String lfn, ReplicaCatalogEntry tuple )
  {
    int result = 0;
    if ( lfn == null || tuple == null ) return result;

    Collection c = (Collection) m_lfn.get(lfn);
    if ( c == null ) return result;

    List l = new ArrayList();
    for ( Iterator i=c.iterator(); i.hasNext(); ) {
      ReplicaCatalogEntry rce = (ReplicaCatalogEntry) i.next();
      if ( ! matchMe( rce, tuple ) ) l.add(rce);
    }

    // anything removed?
    if ( l.size() != c.size() ) {
      result = c.size() - l.size();
      m_lfn.put( lfn, l );
    }

    // done
    return result;
  }

  /**
   * Looks for a match of an attribute value in a replica catalog
   * entry.
   *
   * @param rce is the replica catalog entry
   * @param name is the attribute key to match
   * @param value is the value to match against
   * @return true, if a match was found.
   */
  private boolean hasMatchingAttr( ReplicaCatalogEntry rce,
				   String name, Object value )
  {
    if ( rce.hasAttribute(name) )
      return rce.getAttribute(name).equals(value);
    else
      return value==null;
  }

  /**
   * Deletes all PFN entries for a given LFN from the replica catalog
   * where the PFN attribute is found, and matches exactly the object
   * value. This method may be useful to remove all replica entries that
   * have a certain MD5 sum associated with them. It may also be harmful
   * overkill.
   *
   * @param lfn is the logical filename to look for.
   * @param name is the PFN attribute name to look for.
   * @param value is an exact match of the attribute value to match.
   * @return the number of removed entries.
   */
  public int delete( String lfn, String name, Object value )
  {
    int result = 0;
    if ( lfn == null || name == null ) return result;

    Collection c = (Collection) m_lfn.get(lfn);
    if ( c == null ) return result;

    List l = new ArrayList();
    for ( Iterator i=c.iterator(); i.hasNext(); ) {
      ReplicaCatalogEntry rce = (ReplicaCatalogEntry) i.next();
      if ( ! hasMatchingAttr(rce,name,value) ) l.add(rce);
    }

    // anything removed?
    if ( l.size() != c.size() ) {
      result = c.size() - l.size();
      m_lfn.put( lfn, l );
    }

    // done
    return result;
  }

  /**
   * Deletes all PFN entries for a given LFN from the replica catalog
   * where the resource handle is found. Karan requested this
   * convenience method, which can be coded like
   * <pre>
   *  delete( lfn, RESOURCE_HANDLE, handle )
   * </pre>
   *
   * @param lfn is the logical filename to look for.
   * @param handle is the resource handle
   * @return the number of entries removed.
   */
  public int deleteByResource( String lfn, String handle )
  {
    return delete( lfn, ReplicaCatalogEntry.RESOURCE_HANDLE, handle );
  }

  /**
   * Removes all mappings for an LFN from the replica catalog.
   *
   * @param lfn is the logical filename to remove all mappings for.
   * @return the number of removed entries.
   */
  public int remove( String lfn )
  {
    Collection c = (Collection) m_lfn.remove(lfn);
    if ( c == null ) return 0;
    else return c.size();
  }

  /**
   * Removes all mappings for a set of LFNs.
   *
   * @param lfns is a set of logical filename to remove all mappings for.
   * @return the number of removed entries.
   * @see #remove( String )
   */
  public int remove( Set lfns )
  {
    int result = 0;

    // sanity checks
    if ( lfns == null || lfns.size() == 0 ) return result;

    for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      result += remove(lfn);
    }

    // done
    return result;
  }

  /**
   * Removes all entries from the replica catalog where the PFN attribute
   * is found, and matches exactly the object value.
   *
   * @param name is the PFN attribute key to look for.
   * @param value is an exact match of the attribute value to match.
   * @return the number of removed entries.
   */
  public int removeByAttribute( String name, Object value )
  {
    int result = 0;

    for ( Iterator i=m_lfn.keySet().iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      Collection c = (Collection) m_lfn.get(lfn);
      if ( c != null ) {
	List l = new ArrayList();
	for ( Iterator j=c.iterator(); j.hasNext(); ) {
	  ReplicaCatalogEntry rce = (ReplicaCatalogEntry) j.next();
	  if ( ! hasMatchingAttr(rce,name,value) ) l.add(rce);
	}
	if ( l.size() != c.size() ) {
	  result += ( c.size() - l.size() );
	  m_lfn.put( lfn, l );
	}
      }
    }

    // done
    return result;
  }


  /**
   * Removes all entries associated with a particular resource handle.
   * This is useful, if a site goes offline. It is a convenience method,
   * which calls the generic <code>removeByAttribute</code> method.
   *
   * @param handle is the site handle to remove all entries for.
   * @return the number of removed entries.
   * @see #removeByAttribute( String, Object )
   */
  public int removeByAttribute( String handle )
  {
    return removeByAttribute( ReplicaCatalogEntry.RESOURCE_HANDLE, handle );
  }


  /**
   * Removes everything. Use with caution!
   *
   * @return the number of removed entries.
   */
  public int clear()
  {
    int result = m_lfn.size();
    m_lfn.clear();
    return result;
  }
}

