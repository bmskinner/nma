# Installation and configuration

This section contains information on installing, removing and customising the program.

## Installing

Nuclear Morphology Analysis is designed to be runnable on Windows, Linux and MacOs, though it has been primarily designed and used on Windows. Since it requires a Java runtime, there are two ways to run the software

- with an included packaged Java runtime
- with a user installed version of Java (16 or higher)

### Windows

Option | Link | Info
-------|------|------
Portable | [Nuclear Morphology Analysis-${project.version}-x86_64-windows.zip](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis-${project.version}-x86_64-windows.zip) | Software and Java runtime in a portable format. Unzip the folder wherever you like and run 'Nuclear_Morphology_Analysis.exe'.
Installer | [Nuclear_Morphology_Analysis-${project.version}-x86_64-windows.msi](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis-${project.version}-x86_64-windows.msi) | Software and Java runtime in an installer for Windows. This will add the software to your list of installed programs, and create a start menu entry. May require administrator privileges depending on where you install.
Standalone Jar file | [Nuclear_Morphology_Analysis_${project.version}_standalone.jar](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis_${project.version}_standalone.jar)  | The jar file with all dependencies for users who want to use their own Java install.

### Linux
 
Option | Link | Info
-------|------|------
Linux portable | [Nuclear_Morphology_Analysis-${project.version}-x86_64-linux.tar.gz](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis-${project.version}-x86_64-linux.tar.gz) | Software and Java runtime in a portable format. Unzip the folder wherever you like and run 'launch.sh'. This version was packaged on Debian 11 with glibc 2.31. If you have issues with the packaged version, use the standalne jar below.
Standalone Jar file | [Nuclear_Morphology_Analysis_${project.version}_standalone.jar](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis_${project.version}_standalone.jar)  | The jar file with all dependencies for users who want to use their own Java install.

### MacOS

The software is not currently available packaged with the Java runtime. You will need to:

- Install Java 16 or higher on your Mac; an open source JDK can be downloaded and installed from the [Eclipse Temurin website](https://adoptium.net/temurin/releases/)
- Download [Nuclear_Morphology_Analysis_${project.version}_standalone.jar](https://bitbucket.org/bmskinner/nuclear_morphology/downloads/Nuclear_Morphology_Analysis_${project.version}_standalone.jar) 

Due to security permissions in recent versions of MacOS (Catalina and above) the program won't have access to user folders if launched by double-clicking the jar file. You should launch from the Terminal; open the Terminal, navigate to the directory containing the jar file, and launch the jar using `java -jar Nuclear_Morphology_Analysis_${project.version}_standalone.jar`.

## Uninstalling

### Windows installer version

- Go to Programs and Features, find Nuclear Morphology Analysis in the list of installed programs, and click 'Uninstall'
- Nuclear Morphology Analysis also creates a folder in your home directory called `.nma`. Delete this folder too to remove all traces of the program.

### Portable versions - Windows, Linux and MacOS

- Delete the folder containing the program.
- Nuclear Morphology Analysis also creates a folder in your home directory called `.nma`. Delete this folder too to remove all traces of the program.

## Configuration and user data

Nuclear Morphology Analysis creates a folder in your home directory called `.nma` on first launch, if this folder does not already exist. The folder is used to store:

- log files
- built-in rulesets for shapes we can detect
- custom rulesets for novel shapes you want to analyse
- custom program options

You can open the configuration folder via `Help > Open config directory`.

### Logs

The system state is logged when NMA is started in case debugging is needed. None of the logged data leaves your computer unless you explicitly send it. If you do find a bug or have a problem, you can send the latest log file to me and it may help me track down the problem. There will be up to 5 log files, with `nma.0.log` containing the most recent logs. 

### Rulesets {#config-rulesets}

Rulesets are how we identify landmarks in nuclei. The program has default rulesets for mouse sperm, pig sperm and round nuclei.
These are stored in XML format. If you delete one of the default files, it will be recreated when the program is next launched. 

You can make your own ruleset files and save them in the `rulesets` folder; they will be included in the nucleus detection setup screen when the program is next launched. 

A ruleset needs to describe the landmarks in the nucleus, how we find them, which landmarks we use for orientation of the nucleus, and what measurements we should make. For example, the round nucleus ruleset is the simplest:

```{}
<?xml version="1.0" encoding="UTF-8"?>
<RuleSetCollection name="Round" application="VIA_MEDIAN" version="2.0.0" axis="Y">
  <Orient name="BOTTOM" value="Longest axis" />
  <Orient name="Y" value="Longest axis" />
  <Orient name="REFERENCE" value="Longest axis" />
  <Landmark name="Longest axis">
    <Ruleset type="Diameter profile">
      <Rule type="IS_MAXIMUM">
        <Value>1.0</Value>
      </Rule>
    </Ruleset>
  </Landmark>
  <Measurement>Perimeter</Measurement>
  <Measurement>Elongation</Measurement>
  <Measurement>Area</Measurement>
  <Measurement>Min diameter</Measurement>
  <Measurement>Aspect ratio</Measurement>
  <Measurement>Difference from median</Measurement>
  <Measurement>Bounding width</Measurement>
  <Measurement>Bounding height</Measurement>
  <Measurement>Regularity</Measurement>
  <Measurement>Ellipticity</Measurement>
  <Measurement>Circularity</Measurement>
</RuleSetCollection>
```

It contains one landmark, called `Longest axis`. This is defined as the point with the maximum value in the diameter profile. The landmark is used to specify three orientation marks: `BOTTOM`, `Y`, and `REFERENCE`. The `REFERENCE` point is the point from which profiles start. `BOTTOM` and `Y` indicate points that should be used to orient the nucleus, by rotating the nucleus such that this point is diretly below the centre of mass. 

The `Measurement` values show the standard measures that will be calculated when nuclei are detected; several of these (e.g. aspect ratio) rely on the nucleus being orientatble, which is why it is important to have at least one landmark that can be used for orientation.

### Custom options {#config-file}

Custom options are set in the `config.ini` file in the configuration directory. They are in the format `OPTION=VALUE`. The following options can be set, and will take effect when the program is next launched:

Key        | Allowed Values | Effect
-----------| ---------------|--------------
``DEFAULT_IMAGE_SCALE`` | A positive number e.g. ``18.0`` | Sets the default scale for converting pixels to microns
``DEFAULT_COLOUR_SWATCH`` | ``REGULAR_SWATCH`` ``ACCESSIBLE_SWATCH`` ``NO_SWATCH`` | Sets the default colouring of datasets and segments
``DEFAULT_DIR`` | *directory with backslashes escaped* e.g. `C:\\path\\to\\folder`    | Sets the default folder for exporting data
``DEFAULT_DISPLAY_SCALE`` | ``PIXELS`` ``MICRONS`` | Sets the default scale to display in charts and tables
``REFOLD_OVERRIDE`` |  ``true`` ``false``| If true, the 'best-fit nucleus' method will always be used instead of the position averaging method
``USE_ANTIALIASING`` | ``true`` ``false`` | Sets the default value for the 'Use anti-aliasing' option. False makes charts faster to render, but not as pretty.
``USE_DEBUG_INTERFACE`` | ``true`` ``false`` | If true, shows debugging information in some charts
``USE_GLCM_INTERFACE`` | ``true`` ``false`` | If true, allows GLCM measurements for nuclei
``FILL_CONSENSUS`` | ``true`` ``false`` | Sets the default value for the 'Fill consensus' checkbox in the view menu 
