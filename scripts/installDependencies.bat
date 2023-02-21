REM JEBL is not distributed through Maven Central
REM Download and add to local Maven repo
REM This is a one-time action
REM Note that this script should be adjusted if you use a non-default local
REM Maven repo.

curl -L -o jebl-0.4.jar https://downloads.sourceforge.net/project/jebl/jebl/jebl-0.4/jebl-0.4.jar

IF EXIST "jebl-0.4.jar" (
	mvn install:install-file -Dfile="jebl-0.4.jar" -DgroupId=jebl -DartifactId=jebl -Dversion=0.4 -Dpackaging=jar
	REM Clean up
	rm jebl-0.4.jar
)	

