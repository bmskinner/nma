REM Create the portable app image for Windows

REM Check the jlink path
where jlink

jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ..\res\icons\icon.ico --input ..\target\standalone --dest ..\target\appimage --type app-image --main-jar Nuclear_Morphology_Analysis_${project.version}_standalone.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis