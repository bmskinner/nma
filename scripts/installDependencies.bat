@ECHO OFF
REM JEBL is not distributed through Maven Central
REM Download and add to local Maven repo
REM This is a one-time action
REM Note that this script should be adjusted if you use a non-default local
REM Maven repo.

IF NOT EXIST "artifacts\jebl-0.4.jar" (
	REM Check the executables are on the path
	where /q curl || ECHO Could not find curl on the PATH. && EXIT /B
	where /q mvn  || ECHO Could not find mvn on the PATH. && EXIT /B

	curl -L -o artifacts\jebl-0.4.jar https://downloads.sourceforge.net/project/jebl/jebl/jebl-0.4/jebl-0.4.jar

	IF EXIST "artifacts\jebl-0.4.jar" (
		mvn install:install-file -Dfile="artifacts\jebl-0.4.jar" -DgroupId=jebl -DartifactId=jebl -Dversion=0.4 -Dpackaging=jar
	)
)

	

