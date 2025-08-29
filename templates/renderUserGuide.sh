#!/bin/bash
Rscript -e "library(bookdown); library(tidyverse); bookdown::render_book('index.Rmd', clean = FALSE)"
if [ $? -ne 0 ]; then
	exit 1
fi

# Copy the user guide to the target folder for inclusion in the jar
mv "./_book" "${project.basedir}/target/classes/user-guide"
if [ $? -ne 0 ]; then
	exit 1
fi

# Delete the existing installed user guide in the home directory nma folder so we can
# test if the packaged version is loading properly
F="${user.home}/.nma/user_guide_v${project.version}"

if [ -e $F ]
	then
		rm -r $F
fi