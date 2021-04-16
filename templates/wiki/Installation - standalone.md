# Install the standalone version

The software requires Java version 8 or above to run. A 64bit JVM is recommended. Java can be downloaded from [here](https://www.java.com/en/download/).

Choose the appropriate file to download:

Option | Link | Info
-------|------|------
Windows executable | [Nuclear_Morphology_Analysis_${project.version}.exe](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis_${project.version}.exe) | The software packed for Windows, so you don't need to specify memory settings.
Standalone jar file | [Nuclear_Morphology_Analysis_${project.version}_standalone.jar](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis_${project.version}_standalone.jar) | This jar contains all the dependencies needed to run the software in a single file, and can be run on Windows, MacOS or Linux. You may need to specify memory settings.

# MacOS

Due to security permissions in recent versions of MacOS (Catalina and above) the program won't have access to user folders if launched by double-clicking the jar file. You should launch from the Terminal; open the Terminal, navigate to the directory containing the jar file, and launch the jar using `java -jar Nuclear_Morphology_Analysis_x.y.z.jar`, changing x, y and z to the appropriate numbers for your version.
