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


package org.griphyn.cPlanner.engine.createdir;

import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PegasusBag;

import java.util.List;

/**
 * The interface that defines how the create dir job is created.
 *
 * @author  Karan Vahi
 * @version $Revision$
 */
public interface Implementation {


    /**
     * The version number associated with this API.
     */
    public static final String VERSION = "1.0";
    
     /**
     * Intializes the class.
     *
     * @param bag      bag of initialization objects
     */
    public void initialize( PegasusBag bag ) ;
    
    /**
     * It creates a make directory job that creates a directory on the remote pool
     * using the perl executable that Gaurang wrote. It access mkdir underneath.
     * 
     *
     * @param site  the execution site for which the create dir job is to be
     *                  created.
     * @param name  the name that is to be assigned to the job.
     * @param directory  the directory to be created on the site.
     *
     * @return create dir job.
     */
    public SubInfo makeCreateDirJob( String site, String name, String directory );



}
