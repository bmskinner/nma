#!/usr/bin/perl
#############################
# DESCRIPTION
#############################
# Copyright (C) Ben Skinner 2014
# analysis of sperm_morph script
# Reads the sperm_morph log file
# Creates dat file of xy points
# Creates unified plot file for gnuplot
# Integrates under fitted curve and exports area
#############################
# INFO
#############################
# Inputs:
# Log file from 'sperm_morph.ImageJ.txt'
#
# Outputs:
# 	For complete dataset:
		# data.dat - the xy coordinates for each nucleus
		# data.plot - gnuplot file including curve descriptions
		# data.area.dat - list of areas under the curves
#############################
# Expected input example
#############################
# ~ ------------------
# IN~01.tiff
# ID~J:\Images\2014-10-06_DAPI-lectin\WT\
# LF~J:\Images\2014-10-06_DAPI-lectin\WT\//01.tiff.1.txt
# NP~728
# NA~820
# FD~47.8017
# CL~36.7686
# MY~6.6771
# XA~6.4205
# XB~-0.2228
# XC~0.002138
# XR~0.9195
# # x	y
# 0	6.162
# 2.5763	6.6771
# 7.2202	4.1056
# END OF FILE
#############################
# Header field codes
#############################
# IN Image name
# ID Image directory
# LF Log file path
# NP Nuclear perimeter (pixels)
# NA Nuclear area (pixels)
# FD Feret diameter (max diameter) (pixels)
# CL Curve length (pixels)
# MY Maximum y value
# XA Curve fit parameter A (for curve y = a + bx + cx^2)
# XB Curve fit parameter B
# XC Curve fit parameter C
# XR Curve fit R^2 (how well the data matches the curve fit)
#############################
# Setup
#############################

use warnings;
use strict;
use IO::File;
# use Getopt::Long;
use File::Spec;
# use Statistics::Descriptive;k
# use DBI;
use Data::Dumper;
use Math::Integral::Romberg 'integral';
use lib 'C:\Users\ben\Dropbox\git\mmu_sperm_cartography'; # path to the required modules
use Sperm::Log qw(&read_log); # the module function to use

#############################
# Main
#############################

my $inputFile = $ARGV[0];
my $result = read_log($inputFile);


my $datfile;
my $line = "";

my @areas;

our $nuc; # global variable for nucleus hash to allow integration

foreach my $nucleus ( @$result){

	# print Dumper $nucleus;
	if(${$nucleus}{XR} > 0.7){ # R^2 of curve fit to data. Use only real curves.
		$datfile = make_x_y_dat($nucleus);
		$line .= addCurveToLine($nucleus);
		
		push @areas, integrate_under_curve($nucleus);	
	}
}
make_plot_file($datfile, $line); # datfile will be in the folder of the last $nucleus in @$result

make_area_file($datfile, \@areas);

#############################
# Subs
#############################

sub make_x_y_dat {
	my $nucleus = shift;
	
	my $name = ${$nucleus}{IN};
	my $datfile  = ${$nucleus}{ID} . "data.dat";
	# my $pngfile  = ${$nucleus}{dir} . "data.png";
	# my $plotfile = ${$nucleus}{dir} . "data.plot";
	
	open DAT, ">>$datfile" or die "$!";

	foreach my $profile (${$nucleus}{profiles}){

		my @ref = @$profile;

		foreach my $line (@ref){

			print DAT ${$line}{x} . "\t" . ${$line}{y} . "\n";
		}

	}
	print DAT "\n";
	close DAT;
	return $datfile;
}

sub addCurveToLine{
	my $nucleus = shift;
	# print Dumper $nucleus;
	
	# my $xc = ${$nucleus}{'xc'};
	# print "$xc\n";
	
	my $equation = "(" . ${$nucleus}{XC} . "*x**2)+(" . ${$nucleus}{XB} . "*x)+" . ${$nucleus}{XA};
	
	my $text = ', \\' . "\n" . $equation . " with lines lc rgb 'light-grey' ";
	return $text;
}

sub integrate_under_curve {

	local $nuc = shift; # allows sub f access to nuc
	
	sub f { my ($x) = @_; ( ${$nuc}{XC} *$x**2) + ( ${$nuc}{XB} *$x) + ${$nuc}{XA} } # equation of curve
	$result = integral(\&f, 0, 100);    # Short form
	return $result;
}

sub make_area_file {
	my ($datfile, $areas)  = @_;
	$datfile =~ s/\\/\\\\/g;
	$datfile =~ s/dat$/area.dat/;
	open DAT, ">$datfile" or die $!;
	foreach my $area (@$areas){
		print DAT $area . "\n";
	}
	close DAT;
}

sub make_plot_file {

	my ($datfile, $line)  = @_;
	$datfile =~ s/\\/\\\\/g;
	(my $pngfile  = $datfile) =~ s/dat$/png/;
	(my $plotfile = $datfile) =~ s/dat$/plt/;
	
	open PLOT, ">$plotfile" or die $!;
	
	print PLOT qq{
		
	reset
	# set terminal png transparent truecolor
	set terminal wxt
	unset border
		
	unset grid
	
	set xrange [0:100]
	#set size 800,400
	unset key
	
	plot "$datfile" with points lw 2 lc rgb "grey"	$line
		
	};
	close PLOT;
}
	