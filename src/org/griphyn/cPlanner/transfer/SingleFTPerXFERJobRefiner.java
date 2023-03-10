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

package org.griphyn.cPlanner.transfer;

import org.griphyn.cPlanner.classes.ADag;
import org.griphyn.cPlanner.classes.SubInfo;
import org.griphyn.cPlanner.classes.PlannerOptions;

import org.griphyn.cPlanner.common.PegasusProperties;
import edu.isi.pegasus.common.logging.LogManager;

import org.griphyn.cPlanner.transfer.implementation.ImplementationFactory;
import org.griphyn.cPlanner.transfer.implementation.TransferImplementationFactoryException;

import java.util.Collection;

import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import org.griphyn.cPlanner.classes.PegasusBag;


/**
 * The refiner interface, that determines the functions that need to be
 * implemented to add various types of transfer nodes to the workflow.
 * The single in the name indicates that the refiner works with the
 * implementation that handles one file transfer per transfer job.
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 *
 * @version $Revision$
 */
public abstract class SingleFTPerXFERJobRefiner extends AbstractRefiner{

    /**
     * The overloaded constructor.
     *
     * @param dag        the workflow to which transfer nodes need to be added.
     * @param bag        the bag of initialization objects
     */
    public SingleFTPerXFERJobRefiner( ADag dag,
                                      PegasusBag bag  ){
          super( dag, bag );
    }



    /**
     * Loads the appropriate implementations that is required by this refinement
     * strategy for different types of transfer jobs. It calls to the factory
     * method to load the appropriate Implementor.
     *
     * Loads the implementing class corresponding to the mode specified by the user
     * at runtime in the properties file. The properties object passed should not
     * be null.
     *
     * 
     * @param bag        the bag of initialization objects
     *
     * @exception TransferImplementationFactoryException that nests any error that
     *            might occur during the instantiation.
     */
    public void loadImplementations( PegasusBag bag )
        throws TransferImplementationFactoryException{

        //this can work with any Implementation Factory
        this.mTXStageInImplementation = ImplementationFactory.loadInstance(
                                              bag,
                                              ImplementationFactory.TYPE_STAGE_IN);
        this.mTXStageInImplementation.setRefiner(this);
        this.mTXInterImplementation = ImplementationFactory.loadInstance(
                                              bag,
                                              ImplementationFactory.TYPE_STAGE_INTER);
        this.mTXInterImplementation.setRefiner(this);
        this.mTXStageOutImplementation = ImplementationFactory.loadInstance(
                                               bag,
                                               ImplementationFactory.TYPE_STAGE_OUT);
        this.mTXStageOutImplementation.setRefiner(this);
        //log config messages message
        super.logConfigMessages();

    }
}
