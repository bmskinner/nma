@ECHO OFF
REM Upload files to NMA downloads section on Bitbucket

REM Check the executables are on the path
where /q curl || ECHO Could not find curl on the PATH. && EXIT /B

curl -X POST --url https://api.bitbucket.org/2.0/repositories/bmskinner/nuclear_morphology/downloads -F files=@packages/${jar.finalName}-windows.msi  -F files=@packages/${jar.finalName}-windows.zip -F files=@packages/${jar.finalName}-linux.tar.gz -F files=@packages/${jar.finalName}.jar --header 