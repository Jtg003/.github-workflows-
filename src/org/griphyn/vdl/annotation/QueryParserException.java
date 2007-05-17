/*
 * This file or a portion of this file is licensed under the terms of
 * the Globus Toolkit Public License, found in file ../GTPL, or at
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
package org.griphyn.vdl.annotation;

import java.io.*;

/**
 * This class is used to signal errors while scanning only.
 * @see QueryScanner
 *
 * @author Jens-S. V�ckler
 * @version $Revision: 1.3 $
 */
public class QueryParserException 
  extends java.lang.RuntimeException
{
  /**
   * Constructs an exception that will contain the line number.
   * @param scanner is the lineno stream to obtain the line number from.
   * @param message is the message to print for the failed parse.
   */
  public QueryParserException( QueryScanner scanner, String message )
  {
    super("line " + scanner.getLineNumber() + ": " +message);
  }
}
