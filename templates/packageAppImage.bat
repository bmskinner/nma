REM Create the portable app image for Windows

jpackage --name "Nuclear Morphology Analysis" --app-version ${project.version} --icon ..\res\icons\icon.ico --input ..\target\standalone --dest ..\target\appimage --type app-image --main-jar ${jar.finalName}_standalone.jar --main-class com.bmskinner.nma.core.NuclearMorphologyAnalysis

REM Zip the app image for upload
7z a -tzip "..\target\appimage\Nuclear Morphology Analysis.zip" "..\target\appimage\Nuclear Morphology Analysis"

REM Make folder structure for packaging in Linux

mkdir ..\target\linux
mkdir ..\target\linux\standalone

copy ..\scripts\packageAppImage.sh ..\target\linux\packageAppImage.sh
copy ..\target\standalone\\${jar.finalName}_standalone.jar ..\target\linux\standalone\\${jar.finalName}_standalone.jar
Xcopy /E /I "../res" "../target/linux/res"