@ECHO OFF

REM Check the executables are on the path
where /q jpackage || ECHO Could not find jpackage on the PATH. && EXIT /B
where /q 7z || ECHO Could not find 7z on the PATH. && EXIT /B

REM Create the portable app image for Windows
jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ..\res\icons\icon.ico --input ..\target\standalone --dest ..\target\appimage-win --type app-image --description "Morphometric analysis software" --vendor "Ben Skinner" --copyright "Ben Skinner 2015-${build.year}" --main-jar ${jar.finalName}.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

REM Zip the app image for Windows portable upload
7z a -tzip "..\packages\\${jar.finalName}-windows.zip" "..\target\appimage-win\Nuclear Morphology Analysis"

REM Create windows installer
jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ..\res\icons\icon.ico --input ..\target\standalone --dest ..\target\msi --type msi --description "Morphometric analysis software" --vendor "Ben Skinner" --copyright "Ben Skinner 2015-${build.year}" --win-dir-chooser --win-menu --win-per-user-install --win-menu-group "Nuclear Morphology Analysis" --main-jar ${jar.finalName}.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

copy "..\target\msi\\Nuclear Morphology Analysis-${project.version}.msi" "..\packages\\${jar.finalName}-windows.msi"

REM Copy the standalone jar to the packages directory
copy "..\target\\standalone\\${jar.finalName}.jar" "..\packages\\${jar.finalName}.jar"
