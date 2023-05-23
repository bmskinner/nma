#!/bin/bash
# Create a portable app image that does not require installation
# Package

jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ${project.basedir}/res/icons/icon.png --input ${project.basedir}/target/standalone --dest ${project.basedir}/target/appimage-linux --type app-image --main-jar ${jar.finalName}.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

# Create a launcher script
printf "#!/bin/bash\n./bin/Nuclear\ Morphology\ Analysis" > ${project.basedir}/target/appimage-linux/Nuclear\ Morphology\ Analysis/launch.sh
chmod +x ${project.basedir}/target/appimage-linux/Nuclear\ Morphology\ Analysis/launch.sh

# Make a zipped tar
tar -czf ${project.basedir}/packages/${jar.finalName}-linux.tar.gz -C ${project.basedir}/target/appimage-linux Nuclear\ Morphology\ Analysis 