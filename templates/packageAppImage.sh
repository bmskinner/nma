#!/bin/bash
# Create a portable app image that does not require installation

# Package
jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ./res/icons/icon.png --input ./standalone --dest ./Nuclear\ Morphology\ Analysis/launch.sh --type app-image --main-jar ${jar.finalName}_standalone.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

# Create the launcher script
echo "./bin/Nuclear\ Morphology\ Analysis" > ./Nuclear\ Morphology\ Analysis/launch.sh
chmod +x ./Nuclear\ Morphology\ Analysis/launch.sh

# Make a zipped tar
tar -czf ./target/Nuclear_Morphology_Analysis-${project.version}-linux.tar.gz ./Nuclear\ Morphology\ Analysis
