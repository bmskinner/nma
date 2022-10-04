---
title: 'Nuclear Morphology Analysis 2.0.0: Improved image analysis software for measuring nuclear shape'
tags:
  - Java
  - morphometrics
  - sperm chromatin
  - nuclear organisation
authors:
  - name: Benjamin M. Skinner
    orcid: 0000-0002-7152-1167
    affiliation: 1
    corresponding: true
affiliations:
 - name: School of Life Sciences, University of Essex, Colchester, UK
   index: 1
date: 21 June 2022
bibliography: paper.bib
---

# Summary

We generally imagine cells to have spherical or ellipsoidal nuclei, but many cell types are far more interesting, with great variation in nuclear shapes and sizes [@skinner_nuclear_2017]. Measuring and comparing shapes of cell nuclei in esoteric cell types such as sperm has traditionally been performed by manual assessment. Semi-automated approaches have been developed: geometric morphometric methods allow distinction of subtle phenotypic differences, as in the software MorphoJ [@klingenberg_morphoj_2011], but still require manual landmark annotation in each image (_e.g._ @varea-sanchez_unraveling_2016). Decomposing nuclear outlines into elliptic fourier descriptors is simpler and higher throughput, as in the SHAPE software [@iwata_shape_2002], but does not describe sharp corners well (_e.g._ @ostermeier_measurement_2001, @mashiko_mouse_2017). Consequently, these methods have problems in reproducibility, scalability, or ease of use for biologists. At the other end of the spectrum are open source tools designed for high-throughput image processing such as CellProfiler [@stirling_cellprofiler_2021], with clear documentation and tutorials, but which do not have the ability to natively perform specialised morphometric analysis.

Nuclear Morphology Analysis is a tool for measuring and comparing the shapes of cell nuclei. The software uses a modified Zahn-Roskies transformation [@zahn_ct_fourier_1972] to convert the outlines of detected objects into linear profiles, with rules to identify landmarks of interest from these profiles. This approach allows high-throughput and reproducible analyses, detecting subtle variation in nuclear shape that is beyond the scope of manual assessment.

This article outlines improvements made for Nuclear Morphology Analysis v2.0.0, available at [https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Home](https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Home).

# Statement of Need

Nuclear Morphology Analysis was developed to study subtle shape differences in mouse sperm nuclei within and between species and strains [@skinner_high-throughput_2019], and extended to also study chromatin distribution from fluorescence _in-situ_ hybridisation (FISH) images [@skinner_automated_2019]. This has allowed the detection of subtle shape phenotypes, such as differences in the shapes of X-bearing and Y-bearing mouse sperm in a Yq-deletion model [@rathje_differential_2019]. Since initial publication, it is now being used by other research groups (_e.g._ @stark-dykema_x-linked_2022, @martinez_oligogenic_2022) to investigate the impact of genetic insults on sperm shape and fertility. 

The software has always been intended to be 'biologist-friendly', not requiring much computational experience to run a basic analysis, but enabling more powerful analyses as desired. As the range of functions in the software has grown, issues arose in extensibility, data access and user friendliness; this update has been developed to address these issues.

# New functionality

When version 1 was developed, it was a plugin for ImageJ [@schneider_nih_2012]. As it became more complex and the user interface grew it made more sense to embed ImageJ as a dependency and run the program as a standalone piece of software. That initial development as a plugin however constrained development to Java 8, to remain compatible with ImageJ. Version 2.0.0 is a purely self-contained program and now targets Java 16, allowing use of new language features. The `jlink` and `jpackage` tools included from Java 14 allow packaging of a Java runtime with the software, removing the need for separate Java installation. Portable packages of NMA are provided for Windows and Linux, and an msi installer for Windows. The jar files are also still provided for users who cannot or prefer not to use packaged versions, including MacOS users.

## File formats

Version 1 saved data files using Java serialisation. While simple to implement, this tied reading data to the class structure of the program, constraining the changes could be made to data classes. This has been replaced with an XML-based file format allowing the software to be updated independently, and making the data format more open and accessible, with the caveat that the old file format can no longer be read.

## Landmark detection

Previous versions used a fixed inbuilt set of rules for identifying landmarks in nuclei; mouse sperm, pig sperm and generic round nuclei. The fixed rules have been replaced with user-definable configuration files that are read at launch, allowing new rulesets can be defined and added if a user requires.

A further improvement is separation of the distinction between landmarks of interest, and how those landmarks can be used for orienting and aligning nuclei. Previously, landmarks were named by their function (e.g. _reference point_ defining where profiles begin; _orientation point_ defining a point that will be used to orient the nucleus). Now, landmarks are named for the structural features they reference (e.g. tip of hook in mouse sperm), and a separate map is maintained for how those landmarks should be used to orient nuclei. These provide greater flexibility in defining landmarks that can be used to align nuclei when making custom rules.

## User experience

The user interface has been modified as minimally as possible from previous versions. The primary changes have been:  

- simplification of landmark editing. Previously this required setting the position of a landmark with reference to the angle profile of a dataset. This is how the software directly implements such edits, but is not intuitive to users. Now, editing is performed on the consensus nucleus outline of the dataset, making landmark placement relate to the biological structures (\autoref{fig:fig1}).  
- consolidation of most functions into a single `Datasets` menu. Previously, buttons had proliferated across several tabs or right-click menus. These functions are now more discoverable and consistent.  
- integration of signal warping [@skinner_automated_2019] with the other nuclear signals display, simplifying the presentation of results and making the process of running new analyses easier.  
- A detailed user guide has been included in the download, to ensure it remains available if users cannot access the online wiki.

![Left: Landmark editing on mouse sperm dataset in v1.20.0 showing landmarks as vertical lines on the angle profile. Right: improved editing shows the landmarks as diamonds on the consensus nucleus outline, with the name shown on mouseover (here the tip of the hook).\label{fig:fig1}](Fig_1.png){ width=100% }

The morphological analysis has also been updated to run more efficiently. While performance is heavily dependent on the hardware used, detection and basic analysis of 16,000 nuclei completed in 5 minutes on a PC with a Ryzen 9 3900 processor with 32Gb DDR4 memory versus 40 minutes in the previous version 1.20.0. This should enable routine processing of larger image sets and understanding of subtle phenotypes.

# Acknowledgements

I thank all collaborators who have used and given feedback on the software as it developed. Particular thanks go to Peter Ellis for many helpful discussions and Ellie Watson for help testing the new functions before release. BMS was supported by UKRI funding to the University of Essex.

# References