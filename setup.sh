#!/bin/sh
#
# set-up environment to run Pegasus - source me
#
if [ "X${JAVA_HOME}" = "X" ]; then
    if [ "X${JDK_HOME}" = "X" ]; then
	echo "ERROR! Please set your JAVA_HOME variable" 1>&2
	return 1
    else
	test -t 2 && echo "INFO: Setting JAVA_HOME=${JDK_HOME}" 1>&2
	JAVA_HOME="${JDK_HOME}"
	export JAVA_HOME
   fi
fi

# define PEGASUS_HOME or die
if [ "X${PEGASUS_HOME}" = "X" ]; then
    if [ "X${VDS_HOME}" = "X" ]; then
	echo "ERROR! You must set PEGASUS_HOME env variable." 1>&2
	return 1
    else
	echo "WARNING! VDS_HOME is deprecated. You should set the PEGASUS_HOME env variable." 1>&2
	PEGASUS_HOME="${VDS_HOME}"
	export PEGASUS_HOME
    fi    
fi

# portable egrep -s -q
egrepq () {
    egrep "$1" >> /dev/null 2>&1
}

# add toolkit to PATH and MANPATH
if [ "X${PATH}" = "X" ]; then
    # no previous PATH -- very suspicious
    PATH=$PEGASUS_HOME/bin
else
    # previous PATH -- check for previous existence
    x=$PEGASUS_HOME/bin
    if ! echo $PATH | egrepq "(^|:)$x($|:)" ; then
	PATH="$x:$PATH"
    fi
    unset x
fi
export PATH

if [ "X${MANPATH}" = "X" ]; then
    MANPATH=$PEGASUS_HOME/man
else
    x=$PEGASUS_HOME/man
    if ! echo $MANPATH | egrepq "(^|:)$x($|:)" ; then
	MANPATH="$x:$MANPATH"
    fi
    unset x
fi
export MANPATH

if [ "X${PERL5LIB}" = "X" ]; then
    PERL5LIB=$PEGASUS_HOME/lib/perl
else
    x=$PEGASUS_HOME/lib/perl
    if ! echo $PERL5LIB | egrepq "(^|:)$x($|:)" ; then
	PERL5LIB="$x:$PERL5LIB"
    fi
    unset x
fi
export PERL5LIB

if [ "X${PYTHONPATH}" = "X" ]; then
    PYTHONPATH=$PEGASUS_HOME/lib/python
else
    x=$PEGASUS_HOME/lib/python
    if ! echo $PYTHONPATH | egrepq "(^|:)$x($|:)" ; then
	PYTHONPATH="$x:$PYTHONPATH"
    fi
    unset x
fi
export PYTHONPATH

#if [ -d "$VDS_HOME/contrib/gstar" ]; then
#    # add G* tools to environment
#    GSTAR_LOCATION="$VDS_HOME/contrib/gstar"
#    export GSTAR_LOCATION
#    if [ -r "$GSTAR_LOCATION/etc/gstar-setup-env.sh" ]; then
#	source "$GSTAR_LOCATION/etc/gstar-setup-env.sh"
#    fi
#fi

#
# just add all jars to the CLASSPATH. 
#
cp=`( find ${PEGASUS_HOME}/lib -perm -0500 -name '*.jar' | tr '\012' ':' ; echo "" ) | sed -e 's/::/:/g' -e 's/^://' -e 's/:$//'`

# merge CLASSPATH, avoid FQPN duplicates
if [ "X${CLASSPATH}" != "X" ]; then
    cp=`perl -e 'foreach ( split /:+/, join( ":", $ENV{CLASSPATH}, "@ARGV" ) ) { $t1 .= ":$_" if ( ++$x{$_} == 1 ); } print substr($t1,1), "\n";' $cp`
fi

# set things
CLASSPATH=$cp
export CLASSPATH
unset cp
