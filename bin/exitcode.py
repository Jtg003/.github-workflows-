#!/usr/bin/env python
#
# Copyright 2010 University Of Southern California
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
# exitcode.py
#
# This program parses kickstart invocation records looking for failures.
# If failures are found, it prints a message and exits with a non-zero
# exit code. If no failures are found, it exits with 0.
#
# This program also renames the .out and .err file to .out.XXX and .err.XXX
# where XXX is a sequence number. This sequence number is incremented each
# time the program is run with the same kickstart.out argument.
#
# Since XML parsers are slow, this program doesn't parse the full invocation
# XML, but rather looks for the <status> tag in the XML and extracts the raw 
# exitcode using simple string manipulations. This turns out to be much
# faster than using an XML parser. On .out files with 1000 invocation
# records this program runs in about 30 milliseconds and uses less than 
# 4 MB of physical memory.
#
import sys
import re
import os
from optparse import OptionParser


__author__ = "Gideon Juve <juve@usc.edu>"


def fail(message=None):
	if message: print "fail: %s" % message
	sys.exit(1)
	
	
def rename(outfile):
	"""Rename .out and .err files to .out.XXX and .err.XXX where XXX
	is the next sequence number. Returns the new name, or fails with
	an error message and a non-zero exit code."""
	
	# This is just to prevent the file from being accidentally renamed 
	# again in testing.
	if re.search("\.out\.[0-9]{3}$", outfile):
		return outfile
	
	# Must end in .out
	if not outfile.endswith(".out"):
		fail("%s does not look like a kickstart .out file" % outfile)
	
	# Find next file in sequence
	retry = None
	for i in range(0,1000):
		candidate = "%s.%03d" % (outfile,i)
		if not os.path.isfile(candidate):
			retry = i
			break
	
	# unlikely to occur
	if retry is None:
		fail("%s has been renamed too many times!" % (outfile))
		
	basename = outfile[:-4]
	
	# rename .out to .out.000
	newout = "%s.out.%03d" % (basename,retry)
	os.rename(outfile,newout)
	
	# rename .err to .err.000 if it exists
	errfile = "%s.err" % (basename)
	if os.path.isfile(errfile):
		newerr = "%s.err.%03d" % (basename,retry)
		os.rename(errfile,newerr)
		
	return newout
	
	
def exitcode(outfile):
	"""Parse invocation records looking for status codes. Returns
	the number of successful invocations, or fails with an error
	message and a non-zero exit code."""
	
	# Read the file first
	f = open(outfile)
	txt = f.read()
	f.close()
	
	# Verify the length
	if len(txt) == 0:
		fail("kickstart produced no output")
	
	# Check the exitcode of all tasks
	regex = re.compile(r'raw="(-?[0-9]+)"')
	succeeded = 0
	e = 0
	while True:
		b = txt.find("<status", e)
		if b < 0: break
		e = txt.find("</status>", b) 
		if e < 0: fail("mismatched <status>")
		e = e + len("</status>")
		m = regex.search(txt[b:e])
		if m: raw = int(m.group(1))
		else: fail("<status> was missing valid 'raw' attribute")
		if raw != 0:
			fail("task exited with raw status %d" % raw)
		succeeded = succeeded + 1
		
	# Require at least one task to succeed
	if succeeded == 0:
		fail("no tasks succeeded")
	
	return succeeded
	
		
def main():
	usage = "Usage: %prog [options] kickstart.out"
	parser = OptionParser(usage)
	
	parser.add_option("-t", "--tasks", action="store", type="int",
		dest="tasks", metavar="N", 
		help="Number of tasks expected. If less than N tasks succeeded, then exitcode will fail.")
	parser.add_option("-r", "--return", action="store", type="int",
		dest="exitcode", default=0, metavar="R", 
		help="Return code reported by DAGMan. This can be specified in a DAG using the $RETURN variable.")
	parser.add_option("-n", "--no-rename", action="store_false",
		dest="rename", default=True, 
		help="Don't rename kickstart.out and .err to .out.XXX and .err.XXX. Useful for testing.")
		
	(options, args) = parser.parse_args()
	
	if len(args) != 1:
		parser.error("please specify kickstart.out")
	
	outfile = args[0]
	
	if not os.path.isfile(outfile):
		fail("%s does not exist" % outfile)
	
	# if we are renaming, then rename
	if options.rename:
		outfile = rename(outfile)
	
	# check supplied exitcode first
	if options.exitcode != 0:
		fail("dagman reported non-zero exitcode: %d" % options.exitcode)

	# check exitcodes of all tasks
	succeeded = exitcode(outfile)
	
	# if we know how many tasks to expect, check that they all succeeded
	if options.tasks and options.tasks>=0 and succeeded != options.tasks:
		fail("wrong number of successful tasks: wanted %d got %d" % \
			(options.tasks,succeeded))
	
	# If we reach this, then it was OK
	sys.exit(0)
	
	
if __name__ == "__main__":
	main()