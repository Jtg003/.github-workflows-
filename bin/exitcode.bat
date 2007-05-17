@echo off
rem
rem parse the returned invocation record and optionally pass 
rem into the database. Make the parser return a non-zero exitcode
rem on remote application failures. 
rem $Id: exitcode.bat,v 1.2.0.1 2003/09/12 00:01:46 gmehta Exp $
rem
if "%JAVA_HOME%" == "" (
    echo "Error! Please set your JAVA_HOME variable"
    exit /b 10
)

if "%PEGASUS_HOME%" == "" (
    echo "Error! Please set your PEGASUS_HOME variable"
    exit /b 11
)

if "%CLASSPATH%" == "" (
    echo "Error! Your CLASSPATH variable is suspiciously empty"
    exit /b 12
)

rem grab initial CLI properties
set addon=
:redo
set has=%1
if "%has:~0,2%" == "-D" (
    if "%has%" == "-D" (
	set addon=%addon% -D%2
	shift
    ) else (
        set addon=%addon% %has%
    )
    shift
    goto redo
)
set has=

%JAVA_HOME%\bin\java "-pegasus.home=%PEGASUS_HOME%" %addon% org.griphyn.vdl.toolkit.ExitCode "%*"
