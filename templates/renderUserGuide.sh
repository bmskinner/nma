#!/bin/bash
Rscript -e "bookdown::render_book('index.Rmd', clean = FALSE)"

# Copy the user guide to the target folder for inclusion in the jar
mv "./_book" "../../target/classes/user-guide"

# Delete the existing installed user guide in the home directory nma folder so we can
# test if the packaged version is loading properly
F="${user.home}/.nma/user_guide_v${project.version}"

if [ -e $F ]
	then
		rm -r $F
fi
