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

package org.griphyn.vdl.invocation;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The Machine element. It is a collection of attributes and MachineInfo elements.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public  class Machine extends Invocation {
    
    /**
     * element name
     */
    public static final String ELEMENT_NAME = "machine";
    
    /**
     * An internal maps that is indexed by attribute keys.
     */
    private Map<String, String> mAttributeMap;
    

    /**
     * The List of <code>MachineInfo</code> elements associated with the machine
     */
    private List<MachineInfo>mMachineInfoList;
    
    
    /**
     * Default constructor.
     */
    public Machine( ){
        mAttributeMap = new HashMap<String,String>();
        mMachineInfoList = new LinkedList<MachineInfo>();
    }
    
    
    
    /**
     * Returns the name of the xml element corresponding to the object.
     * 
     * @return name
     */
    public String getElementName(){
        return ELEMENT_NAME;
    }
    
    /**
     * Adds an attribute.
     * 
     * @param key   the attribute key
     * @param value the attribute value
     */
    public void addAttribute( String key, String value ){
        mAttributeMap.put( key, value );
    }
    
    /**
     * Add multiple attributes to the machine info element.
     * 
     * @param keys   <code>List</code> of keys
     * @param values Corresponding <code>List</code> of values
     */
    public void addAttributes( List  keys, List values ){
        for ( int i=0; i< keys.size(); ++i ) {
            String name = (String) keys.get(i);
	    String value = (String) values.get(i);

            addAttribute( name, value );
	}
    }
    
    /**
     * Add a <code>MachineInfo</code> element.
     * 
     * @param info  the machine info element
     */
    public void addMachineInfo( MachineInfo info ){
        mMachineInfoList.add( info );
    }
    
    /**
     * Returns an iterator for the machine info objects
     * 
     * @return Iterator for <code>MachineInfo</code> objects.
     */
    public Iterator<MachineInfo> getMachineInfoIterator(  ){
        return this.mMachineInfoList.iterator();
    }
    
    /**
     * Converts the active state into something meant for human consumption.
     * The method will be called when recursively traversing the instance
     * tree. 
     *
     * @param stream is a stream opened and ready for writing. This can also
     * be a string stream for efficient output.
     */
    public void toString(Writer stream)
            throws IOException {
        throw new IOException( "method not implemented, please contact pegasus-support@isi.edu" );
    }

    /**
     * Dump the state of the current element as XML output. This function
     * traverses all sibling classes as necessary, and converts the data
     * into pretty-printed XML output. The stream interface should be able
     * to handle large output efficiently.
     *
     * @param stream is a stream opened and ready for writing. This can also
     * be a string stream for efficient output.
     * @param indent is a <code>String</code> of spaces used for pretty
     * printing. The initial amount of spaces should be an empty string.
     * The parameter is used internally for the recursive traversal.
     * If a <code>null</code> value is specified, no indentation nor
     * linefeeds will be generated. 
     * @param namespace is the XML schema namespace prefix. If neither
     * empty nor null, each element will be prefixed with this prefix,
     * and the root element will map the XML namespace. 
     * @exception IOException if something fishy happens to the stream.
     */
    public void toXML( Writer stream, String indent, String namespace )
            throws IOException {
       
        String tag = (namespace != null && namespace.length() > 0) ? namespace + ":" : "";
        tag = tag + getElementName() + " ";
        String newLine = System.getProperty("line.separator", "\r\n");


        if (indent != null && indent.length() > 0) {
            stream.write(indent);
        }
        stream.write('<');
        stream.write(tag);

        //write out all the attributes
        for ( Iterator it = mAttributeMap.entrySet().iterator(); it.hasNext() ;) {
            Map.Entry<String, String> entry = ( Map.Entry ) it.next();
            writeAttribute( stream, " " + entry.getKey() + "=\"", quote( entry.getValue(),true) );
        }

        //write out the machine info elements
        if ( mMachineInfoList.isEmpty() ) {
            stream.write("/>");
        } else {
            stream.write( ">" );
            //write out all the machine info
            String newIndent = ( indent == null ) ? null : indent + "  ";
            for( Iterator<MachineInfo> it = mMachineInfoList.iterator(); it.hasNext(); ){
                MachineInfo mi = ( MachineInfo )it.next();
                mi.toXML( stream, newIndent, namespace );
            }

            stream.write( "</" );
            stream.write( this.getElementName() );
            stream.write( ">" );
        }
        
        if (indent != null) {
            stream.write( newLine );
        }
    }

}
