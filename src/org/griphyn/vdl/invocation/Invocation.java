/*
 * This file or a portion of this file is licensed under the terms of
 * the Globus Toolkit Public License, found in file GTPL, or at
 * http://www.globus.org/toolkit/download/license.html. This notice must
 * appear in redistributions of this file, with or without modification.
 *
 * Redistributions of this Software, with or without modification, must
 * reproduce the GTPL in: (1) the Software, or (2) the Documentation or
 * some other similar material which is provided with the Software (if
 * any).
 *
 * Copyright 1999-2004 University of Chicago and The University of
 * Southern California. All rights reserved.
 */
package org.griphyn.vdl.invocation;

import org.griphyn.vdl.Chimera;
import java.io.Serializable;

/**
 * This abstract class defines a common base for all invocation record
 * related Java objects. Since all necessary functionality is described
 * in {@link Chimera}, this class is empty. It exists for grouping
 * purposes.
 *
 * @author Jens-S. Vöckler
 * @author Yong Zhao
 * @version $Revision$
 */
public abstract class Invocation extends Chimera implements Serializable
{
  // empty class, existence just for grouping purposes
}
