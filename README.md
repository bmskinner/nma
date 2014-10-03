Scripts for the analysis of nuclear organisation in mouse sperm

Measure_sperm_CoM.txt
	
	ImageJ macros to detect nuclei in an image (or folder of images) and determine their orientation.
	Outputs the positions of signals detected

plotResults.pl

	Takes a log file from Measure_sperm_CoM.txt and produces a plot for each sperm head,
	plus aggregate plots for position and distance.

sperm_morph.txt
        ImageJ macro to detect mouse sperm hump curves and normalise them for comparisons between different males
