#!/bin/bash
# Create a portable app image that does not require installation
jpackage --name "Nuclear Morphology Analysis" --app-version ${jar.finalName} --icon ./res/icons/icon.png --input ./standalone --dest ./appimage --type app-image --main-jar ${jar.finalName}_standalone.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

