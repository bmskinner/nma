REM Upload files to NMA downloads section on Bitbucket

curl -s -u bmskinner -X POST https://api.bitbucket.org/2.0/repositories/bmskinner/nuclear_morphology/downloads -F files=@..\packages\\${jar.finalName}-windows.msi  -F files=@..\packages\\${jar.finalName}-windows.zip -F files=@..\packages\\${jar.finalName}-linux.tar.gz -F files=@..\target\standalone\\${jar.finalName}.jar