Scripts for the analysis of morphology and nuclear organisation in mouse sperm

-----------------------
sperm_NO.ImageJ.txt
-----------------------	
	ImageJ macros to detect nuclei in an image (or folder of images) and determine their orientation.
	Outputs the positions of signals detected
-----------------------
sperm_NO.analysis.pl
-----------------------
	Takes a log file from Sperm_NO.ImageJ.txt and produces a plot for each sperm head,
	plus aggregate plots for position and distance.


-----------------------
sperm_morph.ImageJ.txt
-----------------------
        ImageJ macro to detect mouse sperm hump curves and normalise them for comparisons between different males
-----------------------
sperm_morph.analysis.pl
-----------------------
        Perl script to analyse the normalised and raw acrosomal curves, and generate plots