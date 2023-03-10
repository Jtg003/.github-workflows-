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
#ifndef _CHIMERA_MAPTYPES_HH
#define _CHIMERA_MAPTYPES_HH

#include <string>
#include <map>

// normal LFN to SFN mapping, includes a flag
typedef std::pair< std::string, bool > FilenameBool;
typedef std::map< std::string, FilenameBool > FilenameMap;

// multiple SFN to TFN mapping
typedef std::multimap< std::string, std::string > FilenameMultiMap;

// range operator
typedef std::pair< FilenameMultiMap::const_iterator, 
		   FilenameMultiMap::const_iterator > FilenameMMRange;

#endif // _CHIMERA_MAPTYPES_HH
