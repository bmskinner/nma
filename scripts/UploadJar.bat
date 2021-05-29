REM Upload files to NMA downloads

C:\Portable\curl-7.72.0-win64-mingw\bin\curl -s -u bmskinner -X POST https://api.bitbucket.org/2.0/repositories/bmskinner/nuclear_morphology/downloads -F files=@..\target\Nuclear_Morphology_Analysis_1.20.1_standalone.jar -F files=@..\target\Nuclear_Morphology_Analysis_1.20.1.exe   