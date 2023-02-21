#!/bin/bash

# JEBL is not distributed through Maven Central
# Download and add to local Maven repo
# This is a one-time action
# Note that this script should be adjusted if you use a non-default local
# Maven repo.

JEBL_REMOTE="https://downloads.sourceforge.net/project/jebl/jebl/jebl-0.4/jebl-0.4.jar"
curl -L -o jebl-0.4.jar $JEBL_REMOTE

if [[ -e "jebl-0.4.jar" ]]; then
	mvn install:install-file -Dfile="jebl-0.4.jar" -DgroupId=jebl -DartifactId=jebl -Dversion=0.4 -Dpackaging=jar
	
	# Clean up
	rm "jebl-0.4.jar"
fi

