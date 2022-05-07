REM Create the portable app image for Windows

REM Check the jlink path
REM where jlink

jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ..\res\icons\icon.ico --input ..\target\standalone --dest ..\target\appimage --type app-image --main-jar ${jar.finalName}_standalone.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

REM Zip the app image for upload
7z a -tzip "..\target\appimage\Nuclear Morphology Analysis.zip" "..\target\appimage\Nuclear Morphology Analysis"