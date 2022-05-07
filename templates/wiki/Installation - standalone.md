# Installation

The software requires Java version 16 or above to run. A 64bit JVM is recommended. You can download the software bundled with Java, or advanced users can download just the jar.

Choose the appropriate file to download:

Option | Link | Info
-------|------|------
Windows executable | [Nuclear_Morphology_Analysis_${project.version}.zip](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis_${project.version}.exe) | The software packed for Windows. Extract the zip file to wherever you wish.
Standalone jar file | [Nuclear_Morphology_Analysis_${project.version}_standalone.jar](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis_${project.version}_standalone.jar) | This jar contains all the dependencies needed to run the software in a single file, and can be run on Windows, MacOS or Linux. You may need to specify memory settings.

# MacOS

Due to security permissions in recent versions of MacOS (Catalina and above) the program won't have access to user folders if launched by double-clicking the jar file. You should launch from the Terminal; open the Terminal, navigate to the directory containing the jar file, and launch the jar:

```
cd /path/to/where/you/downloaded/nma
java -jar Nuclear_Morphology_Analysis__${project.version}_standalone.jar
```

# Specifying memory

The Java Virtual Machine uses only a small amount of your total system memory by default. If you run the standalone jar, you may need to increase the amount of memory available in order to run large numbers of images, or keep multiple datasets open at once. 

This requires launching Nuclear Morphology Analysis from the terminal (or command line), with the `-Xmx` parameter:

```
java -Xmx4096M -jar Nuclear_Morphology_Analysis__${project.version}_standalone.jar
```

In this example, `-Xmx4096M` tells Java to allow up to 4096Mb of memory to be used (about 4Gb). Adjust this number as needed for your own computer.

For frequent use, you can save these parameters as a script. In a Linux system at the terminal:

```
cd /path/to/where/you/downloaded/nma
echo java -Xmx4096M -jar Nuclear_Morphology_Analysis__${project.version}_standalone.jar > launchNMA.sh
chmod +x launchNMA.sh
```

Thereafter you can launch the software by invoking

```
cd /path/to/where/you/downloaded/nma
./launch.sh
```
