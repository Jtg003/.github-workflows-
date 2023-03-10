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

package org.griphyn.cPlanner.parser.dax;

import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PegasusBag;

/**
 * This interfaces defines the callback calls from DAX parsing. A slim
 * and memory-efficient parser of DAX is expected to implement these
 * callbacks, and generate its own information on the fly.
 *
 * @author Karan Vahi
 * @author Jens-S. Vöckler
 * @version $Revision$
 */
public interface Callback {

    /**
     * The version of the Callback api
     */
    public static final String VERSION = "1.2";


    /**
     * Return a object that is constructed during the parsing of the object.
     * The type of the object that is constructed is determined by the
     * implementing callback handler. For example, it could be an Adag object
     * used by Pegasus or a map containing the graph structure of the dax.
     * The implementing classes should keep a boolean flag that signifies whether
     * the corresponding object has been created by the implementing class or
     * not. The variable should be set when the implementing callback handler
     * deems that it has enough data to construct that object.
     *
     * @return  constructed object
     */
    public Object getConstructedObject();


    /**
     * Callback when the opening tag was parsed. This contains all
     * attributes and their raw values within a map. This callback can
     * also be used to initialize callback-specific resources.
     *
     * @param attributes is a map of attribute key to attribute value
     */
    public void cbDocument(java.util.Map attributes);

    /**
     * Callback for the job from section 2 jobs. These jobs are completely
     * assembled, but each is passed separately.
     *
     * @param job is the DAX-style job.
     */
    public void cbJob(SubInfo job);

    /**
     * Callback for child and parent relationships from section 3.
     *
     * @param child is the IDREF of the child element.
     * @param parents is a list of IDREFs of the included parents.
     */
    public void cbParents(String child, java.util.List parents);

    /**
     * Callback when the parsing of the document is done. While this state
     * could also be determined from the return of the invocation of the
     * parser, that return may be hidden in another place of the code.
     * This callback can be used to free callback-specific resources.
     */
    public void cbDone();

}
