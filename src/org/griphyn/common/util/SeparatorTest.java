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

package org.griphyn.common.util;

/**
 * This is the test program for the Separator class.
 *
 * @author Jens-S. Vöckler
 * @author Yong Zhao
 * @version $Revision$
 *
 * @see org.griphyn.vdl.classes.Definition
 */
public class SeparatorTest
{
  public static void show( String what )
  {
    System.out.print( what + " => [" );
    try {
      String[] x = Separator.split(what);
      for ( int i=0; i<x.length; ++i ) {
	System.out.print( Integer.toString(i) + ':' );
	System.out.print( x[i] == null ? "null" : "\"" + x[i] + "\"" );
	if ( i < x.length-1 ) System.out.print( ", " );
      }
    } catch ( IllegalArgumentException iae ) {
      System.out.print( iae.getMessage() );
    }
    System.out.println( ']' );
  }

  public static void main( String[] args )
  {
    if ( args.length > 0 ) {
      for ( int i=0; i<args.length; ++i )
	show( args[i] );
    } else {
      show( "test" );
      show( "test::me" );
      show( "test:me" );
      show( "test::me:too" );
      show( "illegal:::too" );

      show( "test::me:a,b" );
      show( "test::me:,b" );
      show( "test::me:a," );
      show( "il::legal:," ); // illegal spec

      show( "me:a,b" );
      show( "me:,b" );
      show( "me:a," );
      show( "illegal:," ); // illegal spec

      show( ":::," ); // illegal spec
    }
  }
}
