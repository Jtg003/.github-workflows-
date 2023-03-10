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

package org.griphyn.cPlanner.selector;



import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.PegasusBag;

import org.griphyn.cPlanner.partitioner.graph.Graph;

import java.util.List;

/**
 *
 * The interface for the Site Selector. Allows us to maps the workflows
 * to different sites.
 *
 * @author Karan Vahi
 * @author Jens-S. Vöckler
 * @author Gaurang Mehta
 *
 *
 * @version $Revision$
 */
public interface SiteSelector {

    /**
     * The version of the API of the Site Selector.
     */
    public static final String VERSION = "2.0";

    /**
     * The value for the pool handle, when the pool is not found.
     */
    public static final String SITE_NOT_FOUND = "NONE";


    /**
     * Initializes the site selector.
     *
     * @param bag   the bag of objects that is useful for initialization.
     *
     */
    public void initialize( PegasusBag bag );

    /**
     * Maps the jobs in the workflow to the various grid sites.
     * The jobs are mapped by setting the site handle for the jobs.
     *
     * @param workflow   the workflow in a Graph form.
     *
     * @param sites     the list of <code>String</code> objects representing the
     *                  execution sites that can be used.
     */
    public void mapWorkflow( Graph workflow, List sites );

    /**
     * Maps the jobs in the workflow to the various grid sites.
     * The jobs are mapped by setting the site handle for the jobs.
     *
     * @param workflow   the workflow.
     *
     * @param sites     the list of <code>String</code> objects representing the
     *                  execution sites that can be used.
     */
    public void mapWorkflow( ADag workflow, List sites );



    /**
     * This method returns a String describing the site selection technique
     * that is being implemented by the implementing class.
     *
     * @return a short description
     */
    public  String description();



}
