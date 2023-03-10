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

package org.griphyn.cPlanner.cluster;

/**
 * The baseclass of the exception that is thrown by all Clusterers.
 * It is a checked exception.
 *
 * @author Karan Vahi
 * @version $Revision$
 */

public class ClustererException extends Exception {

    /**
     * Constructs a <code>ClustererException</code> with no detail
     * message.
     */
    public ClustererException() {
        super();
    }

    /**
     * Constructs a <code>ClustererException</code> with the specified detailed
     * message.
     *
     * @param message is the detailled message.
     */
    public ClustererException(String message) {
        super(message);
    }

    /**
     * Constructs a <code>ClustererException</code> with the specified detailed
     * message and a cause.
     *
     * @param message is the detailled message.
     * @param cause is the cause (which is saved for later retrieval by the
     * {@link java.lang.Throwable#getCause()} method). A <code>null</code>
     * value is permitted, and indicates that the cause is nonexistent or
     * unknown.
     */
    public ClustererException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a <code>ClustererException</code> with the
     * specified just a cause.
     *
     * @param cause is the cause (which is saved for later retrieval by the
     * {@link java.lang.Throwable#getCause()} method). A <code>null</code>
     * value is permitted, and indicates that the cause is nonexistent or
     * unknown.
     */
    public ClustererException(Throwable cause) {
        super(cause);
    }

}
