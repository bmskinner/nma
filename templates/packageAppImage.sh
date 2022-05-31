#!/bin/bash
# Create a portable app image that does not require installation
# This should be run from the VM assuming the project folder is mounted under /mnt/nma

# i.e. invoke /mnt/nma/scripts/packageAppImage.sh

# Move the files to a local directory in the VM
cp -r /mnt/nma/target/linux ~/Documents/linux
cd ~/Documents/linux

# Package
jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ./res/icons/icon.png --input ./standalone --dest ./appimage --type app-image --main-jar ${jar.finalName}_standalone.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

# Make a tar
tar -czvf Nuclear_Morphology_Analysis_${project.version}.tar.gz ./appimage/Nuclear\ Morphology\ Analysis

cp Nuclear_Morphology_Analysis_${project.version}.tar.gz /mnt/nma/target/Nuclear_Morphology_Analysis-${project.version}-x86_64-linux.tar.gz