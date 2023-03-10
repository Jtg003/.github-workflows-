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


import org.griphyn.cPlanner.common.PegasusProperties;


import java.util.Map;
import java.util.List;
import java.util.Iterator;

/**
 * An example callback that prints out  the various elements in the DAX.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class ExampleDAXCallback implements Callback {
    private boolean mDone;




    /**
     * The overloaded constructor.
     *
     * @param properties  the properties passed to the planner.
     * @param dax         the path to the DAX file.
     */
    public ExampleDAXCallback( PegasusProperties properties, String dax ) {

    }


    /**
     * Callback when the opening tag was parsed. This contains all
     * attributes and their raw values within a map. It ends up storing
     * the attributes with the adag element in the internal memory structure.
     *
     * @param attributes is a map of attribute key to attribute value
     */
    public void cbDocument(Map attributes) {
        System.out.println( "The attributes in DAX header retrieved ");
        System.out.println( attributes );

    }

    /**
     * Callback for the job from section 2 jobs. These jobs are completely
     * assembled, but each is passed separately.
     *
     * @param job  the <code>SubInfo</code> object storing the job information
     *             gotten from parser.
     */
    public void cbJob(SubInfo job) {
        System.out.println();
        System.out.println( "Job parsed ");
        System.out.println( job );

    }

    /**
     * Callback for child and parent relationships from section 3.
     *
     * @param child is the IDREF of the child element.
     * @param parents is a list of IDREFs of the included parents.
     */
    public void cbParents(String child, List parents) {
        System.out.println();
        System.out.println( "Edges in the DAX " );
        for( Iterator it = parents.iterator() ; it.hasNext(); ){
            System.out.println( it.next() + " -> " + child );
        }

    }

    /**
     * Callback when the parsing of the document is done. It sets the flag
     * that the parsing has been done, that is used to determine whether the
     * ADag object has been fully generated or not.
     */
    public void cbDone() {
        mDone = true;
    }

    /**
     * Returns an ADag object corresponding to the abstract plan it has generated.
     * It throws a runtime exception if the method is called before the object
     * has been created fully.
     *
     * @return  ADag object containing the abstract plan referred in the dax.
     */
    public Object getConstructedObject(){
        //RETURN YOUR CONVERTED OBJECT HERE
        return new String( "Shallow Object" );
    }
}
