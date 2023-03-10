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

package org.griphyn.common.catalog.transformation.client;

/**
 * This is a helper class which all TC client components (like tcAdd, tcDelete and tcQuery) must  extend.
 *
 * @author Gaurang Mehta gmehta@isi.edu
 * @version $Revision$
 */

import edu.isi.pegasus.common.logging.LogManager;
import org.griphyn.common.catalog.TransformationCatalog;
import org.griphyn.common.classes.SysInfo;
import org.griphyn.common.util.ProfileParser;
import org.griphyn.common.util.ProfileParserException;
import org.griphyn.common.util.Separator;

import java.util.List;
import java.util.Map;

public class Client {
    protected int trigger = 0;

    protected String lfn = null;

    protected String pfn = null;

    protected String profile = null;

    protected String type = null;

    protected String resource = null;

    protected String systemstring = null;

    protected String namespace = null;

    protected String name = null;

    protected String version = null;

    protected List profiles = null;

    protected SysInfo system = null;

    protected String file = null;

    protected LogManager mLogger = null;

    protected TransformationCatalog tc = null;

    protected boolean isxml = false;

    public Client() {
    }

    /**
     * Takes the arguments from the TCClient and stores it for acess to the other TC Client modules.
     * @param argsmap Map
     */
    public void fillArgs( Map argsmap ) {
        lfn = ( String ) argsmap.get( "lfn" );
        pfn = ( String ) argsmap.get( "pfn" );
        resource = ( String ) argsmap.get( "resource" );
        type = ( String ) argsmap.get( "type" );
        profile = ( String ) argsmap.get( "profile" );
        systemstring = ( String ) argsmap.get( "system" );
        trigger = ( ( Integer ) argsmap.get( "trigger" ) ).intValue();
        file = ( String ) argsmap.get( "file" );
        isxml = ( ( Boolean ) argsmap.get( "isxml" ) ).booleanValue();
        if ( lfn != null ) {
            String[] logicalname = Separator.split( lfn );
            namespace = logicalname[ 0 ];
            name = logicalname[ 1 ];
            version = logicalname[ 2 ];
        }
        if ( profile != null ) {
            try {
                profiles = ProfileParser.parse( profile );
            } catch ( ProfileParserException ppe ) {
                mLogger.log( "Parsing profiles " + ppe.getMessage() +
                    "at position " + ppe.getPosition(), ppe,
                    LogManager.ERROR_MESSAGE_LEVEL );
            }
        }
        if ( systemstring == null ) {
            system = new SysInfo( systemstring );
        }
    }

}
