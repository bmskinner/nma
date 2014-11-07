#!/usr/bin/perl
#############################
# DESCRIPTION
#############################
# Copyright (C) Ben Skinner 2014
# This script takes output from ImageJ
# measurement of chromosome position in 
# mouse sperm and plots the positions in 
# each individual nucleus plus the 
# combined data.
#############################
# INFO
#############################
# Inputs:
# Log file from 'sperm_NO.ImageJ.txt'
#
# Outputs:
# 	For each nucleus:
		# <image>.plot
		# <image>.polar
		# <image>.profile.dat
		# <image>.profile.plot
#	For combined data:
		# all.polar
		# all.plot
		# angle.dat
		# angle.plot
		# histo.dat
		# histo.plot
#############################
# Expected input example
#############################
# ~ ------------------
# ID~J:\Protocols\Scripts and macros\4
# NN~0
# SP~0.5997
# SR~294
# NA~2238
# SA~216.3177
# DR~0.4237
# DS~0.5465
# # signal_position	signal_intensity	dapi_intensity	cumulative_signal	cumulative_dapi
# 0	0	0	0	0
# 0.03903	0	34	0	34
# 1.0149	1	195	3195	4055
# END OF FILE
#############################
# Header field codes
#############################
# ID	Image name
# NN	Nucleus number
# SP	Signal position
# SR	Signal area
# NA	Nuclear area
# SA	Signal angle
# DR	DAPI proportion
# DS	DAPI position
#############################
# TODO
#############################
# Confirm gnuplot version handling of radius column
# Gnuplot 4.4p3 - cannot handle circles on polar coordinates
# Spiral as single curve distorts centre point
# Single output file handle to DAT
#############################
# Setup
#############################
use warnings;
use strict;
use IO::File;
# use Getopt::Long;
use File::Spec;
use Statistics::Descriptive;
# use DBI;
use Data::Dumper;
use constant PI    => 4 * atan2(1, 1);
use lib 'J:\Protocols\Scripts and macros'; # path to the required modules
use Sperm::Log qw(&read_log); # the module function to use
#############################
# Variables
#############################

my $inputFile = $ARGV[0];
my $result = read_log($inputFile);

our $PLOT_TEXT_1 = qq{
	reset
	# set terminal png transparent truecolor
	set terminal wxt
	unset border
	set polar
	set angles degrees #set gnuplot on degrees instead of radians

	set style line 11 lt 1 lw 2 pt 2 ps 2
	set style fill transparent solid 0.1 noborder

	set label "" at 0,0 point pt 7 lc rgb "black"
};

our $PLOT_TEXT_2 = qq{
	
	unset grid
	#set style line 10 lt 1 lc 0 lw 0.3 #redefine a new line style for the grid

	#set grid polar 60 #set the grid to be displayed every 30 degrees
	#set grid ls 10

	set xrange [-2000:2000]
	set yrange [-2000:2000]

	unset xtics
	unset ytics
	set size square
	unset key

	set style line 11 lt 1 lw 2 pt 2 ps 2 #set the line style for the plot

	########################################
	# Ellipse
	########################################
	# ellipse
	a=1000
	b=800
	e=sqrt(a**2-b**2)/a
	phi=15*pi/180
	g(t) = (t >=  0  && t <= 270) ? sqrt( (a**2 * b**2)/(a**2*sin(t-phi)**2 + b**2*cos(t-phi)**2) ) : 1/0

	# mid hump
	#0.001019x3 - 0.829630x2 + 225.194444x - 19570.000000
	n(t) = (t >= 265 && t <= 360) ? (0.001014*(t**3)) - (0.825556*(t**2)) + (224.004167*t) - 19454.5 : 1/0

	#upper hump
 	#h(t) =  (t >= 330 && t <= 360 ) ? (7.7778*t) - 1300 : 1/0

 	# inner hook
	i(t) =   (t <= 15 && t >= 0) ? (-1.333*(t**2)+(53.333*t)+1000) : 1/0

	# top of hook
	f(t) =   (t >= 0 && t <= 15 ) ? (1500) : 1/0 

	# lower hump
	j(t) =  (t >= 180 && t <= 300) ? (0.00075*(t**3))  -  (0.5232*(t**2) + (108.47*t) - 6300.1) : 1/0


	plot g(t) with lines lw 2 lc rgb "black", \\
	i(t) with lines lw 2 lc rgb "black", \\
	f(t) with lines lw 2 lc rgb "black", \\
	j(t) with lines lw 2 lc rgb "black", \\
	n(t) with lines lw 2 lc rgb "black", \\
};
#############################
# Main
#############################

unlink 'all.polar' if -e 'all.polar';

my $list = "";

foreach my $nucleus ( @$result){
	# make a plot of the density profiles
	make_profile_plot($nucleus);

	# draw the polar plot
	$list .= make_polar_plot($nucleus);
}

if(scalar @$result > 1) { # don't bother if there is only one nucleus in the log
	plot_all('all.plot', $list);

	# bins; for each 30 degrees, what is the average distance?
	# for each 30 degrees, what is the signal count?
	create_angle_bins($result);
}
########################################
# Subs
########################################
sub create_angle_bins {
	my $in = shift;
	my @data = @$in;
	my @ref;
	my $stat;
	
	my @bins = @{create_bins(30)};
	# print "Angle\tMean distance\n";

	my $anglefile = "angle.dat";
	unlink $anglefile if -e $anglefile;
	open ANG, ">$anglefile" or die "$!";

	foreach my $bin (@bins){

		my @hits;
		$stat = Statistics::Descriptive::Full->new();

		foreach my $nucleus (@data){
			
			my $angle = ${$nucleus}{angle};
			if ($angle >= $bin && $angle < ($bin+30)){

				push @hits, ${$nucleus}{position};
				push @ref,  ${$nucleus}{angle};
			}
		}
		$stat->add_data(@hits);
		my $mean = $stat->mean() ? $stat->mean : 0;
		my $sd = $stat->standard_deviation ? $stat->standard_deviation : 0;

		print ANG "$bin\t$mean\t$sd\n";
		# print sprintf("%3s",$bin) . " <= angle < " . sprintf("%3s",($bin+30))  . "\t" . sprintf("%.2f",$mean) . "\n";
		
	}
	close ANG;
	$stat = Statistics::Descriptive::Full->new();
	$stat->add_data(@ref);

	my $plotfile = "angle.plt";
	open PLOT, ">$plotfile" or die "$!";

	print PLOT qq{
			reset
			unset key
			set output "angle.png"
			set boxwidth 30
			set yrange [0:1]
			set xrange [0:360]
			set ylabel "Mean distance (+/-SD)" rotate
			set xlabel "Angle" norotate
			set ytics nomirror scale 0,0
			set xtics 0,30,360 nomirror rotate scale 0,0
			set border 31 lw 2

			set terminal png size 400,300 transparent truecolor

			plot '$anglefile' using (\$1+15):2:3 with errorbars lc rgb "dark-grey" lw 2
		};
	# print "\nAngle (up to)\tSignals\n";

	my $datfile = "histo.dat";
	unlink $datfile if -e $datfile;
	open DAT, ">$datfile" or die "$!";
	my $max = 0;
	my %f = $stat->frequency_distribution(\@bins);
	for (sort {$a <=> $b} keys %f) {
		print DAT "$_\t$f{$_}\n";
		$max = $f{$_} > $max ? $f{$_} : $max;
	}
	# add charting of histo
	$plotfile = "histo.plt";
	open PLOT, ">$plotfile" or die "$!";

	print PLOT qq{
			reset
			unset key
			set output "histo.png"
			set boxwidth 30
			set yrange [0:$max]
			set xrange [0:360]
			set ylabel "Signals" rotate
			set xlabel "Angle" norotate
			set ytics nomirror scale 0,0
			set xtics 0,30,360 nomirror rotate scale 0,0
			set border 31 lw 2

			set terminal png size 400,300 transparent truecolor

			plot '$datfile' using (\$1-15):2 with boxes fs solid lc rgb "dark-grey" lw 2, \\
			'' using (\$1-15):2 with boxes lc rgb "black" lw 2 notitle
		};
}

sub create_bins {
	my $step = shift;
	my @bins;
	for (my $i = 0; $i <= 360; $i += $step) {
	    push @bins, $i;
	}
	return \@bins;
}

sub make_profile_plot {

	my $nucleus = shift;
	# fields: signal_position\tsignal_intensity\tdapi\tcumulative_signal\tcumulative_dapi

	my $name = ${$nucleus}{ID} . "." . ${$nucleus}{NN};
	my $pos = ${$nucleus}{DS};
	my $propOuterPos = $pos / 2; # where to put the outer label
	my $propInnerPos = ((1-$pos)/2)+$pos; # where to put the inner label
	my $propOuter = sprintf("%.2f",${$nucleus}{DR});
	my $propInner = sprintf("%.2f",1- $propOuter);

	my $datfile = $name . ".profile.dat";
	my $pngfile = $name . ".profile.png";
	my $plotfile = $name . ".profile.plot";
	
	open DAT, ">$datfile" or die "$!";

	 # ${$nucleus}{profiles};
	foreach my $profile (${$nucleus}{profiles}){


		my @ref = @$profile;

		foreach my $line (@ref){
			# print ${$line}{position} . "\n";
			print DAT ${$line}{signal_position} . "\t" . ${$line}{signal_intensity} . "\t" . ${$line}{dapi_intensity} . "\n";
		}

	}
	close DAT;
	
	open PLOT, ">$plotfile" or die "$!";
	print PLOT qq{
		reset
		set terminal png size 300,200 transparent truecolor
		unset key
		set output "$pngfile"
		set yrange [0:260]
		set xrange [0:1]

		set label "$propOuter" at $propOuterPos,200 center
		set label "$propInner" at $propInnerPos,200 center
		set arrow from first $pos,0 to $pos, 255 lw 2 lc rgb "black" nohead 

		plot '$datfile' using 1:2 with lines  lc rgb 'red' lw 2, \\
		'' using 1:3 with lines  lc rgb 'blue' lw 2 notitle

	};
	close PLOT;
	# make .plot

}

sub make_polar_plot {

	# ULTIMATELY:
		# Draw a sperm head template;
		# Choose the distance to use for each degree of rotation from centre
		# Plot a circle at distance and angle. Area is proportional to original signal.

		# for the nucleus shape, make a simple nautilus to approximate the cutoff at the hook


	my $nucleus = shift;

	my $name = ${$nucleus}{ID} . "." . ${$nucleus}{NN};
	my $datfile = $name . ".polar";
	unlink $datfile if -e $datfile;
	my $allfile = "all.polar";
	my $pngfile = $name . ".polar.png";
	my $plotfile = $name . ".plot";

	open DAT, ">$datfile" or die "$!";
	open ALL, ">>$allfile" or die "$!";

	# where to put the circle?
	my $angle = ${$nucleus}{SA}; # angle from feret line
	my $max_distance = lookup_distance($angle); #  radius of nucleus at this point
	my $distance = (1 - ${$nucleus}{SP} ) * $max_distance; # reverse the scale (0 is cenrtre now)
	my $dapi_distance = (1 - ${$nucleus}{DR} ) * $max_distance;
	# how large to make the circle?
	# my $nucleus_integral = 445325; # as given by wolfram alpha integrating spiral from 0-360
	# my $nucleus_integral = 335325; # about 3/4 the spiral
	my $fraction_area = ${$nucleus}{SR} / ${$nucleus}{NA};
	
	my $circle_area = $fraction_area * 445325;
	my $radius = 2* (sqrt($circle_area / PI));

	# find the cartesian coordinates of the signal position
	my $x = get_cartesian_x($dapi_distance, $angle);
	my $y = get_cartesian_y($dapi_distance, $angle);
	$radius =sprintf("%.3f", $radius);

	# print "$angle\t$distance\t:\t$x\t$y\n";

	my $text = "set object circle at first $x,$y size first $radius fc rgb \"#0100FF00\"\n";
	
	print DAT "$angle\t$dapi_distance\t$radius\n";
	print ALL "$angle\t$dapi_distance\t$radius\n";

	close DAT;
	close ALL;
	
	open PLOT, ">$plotfile" or die "$!";
	print PLOT qq{
	
	$PLOT_TEXT_1
	set output "$pngfile"
	$text

	$PLOT_TEXT_2 "$datfile" u 1:2 w points pt 7 ps 1 lc rgb "dark-green"
	};
	return $text;
}

sub plot_all {
	my ($plotfile, $list)  = @_;

	open PLOT, ">$plotfile" or die "$!";
	print PLOT qq{
	$PLOT_TEXT_1
	set output "all.png"

	$list
	$PLOT_TEXT_2 "all.polar" u 1:2 w points pt 7 ps 1 lc rgb "red"
	};
	close PLOT;
}

sub lookup_distance {

	my $angle = shift;
	# sperm head coordinates by spiral - OLD
	# my $dist = (100*($angle+420) / (49 - (7*(cos(2*$angle)))));

	my $dist;

	if ($angle >= 0 && $angle < 270 ){ # ellipse equation
	 	
	 	my $a = 1000;
	 	my $b = 800;
	 	my $phi = 15 * PI/180;
	 	my $e= sqrt($a**2-$b**2)/$a;

	 	$dist = sqrt( ($a**2 * $b**2)/($a**2*sin(degrees_to_radians($angle-$phi))**2 + $b**2*cos(degrees_to_radians($angle-$phi))**2) );
	} else { # sperm hump

		$dist = (0.001014*($angle**3)) - (0.825556*($angle**2)) + (224.004167*$angle) - 19454.5 ;
		# TODO: convert this to hump equation
	}

	return $dist;
}

sub get_cartesian_x {

	my ($r, $theta) = @_;
	my $x = $r * cos(degrees_to_radians($theta));
	return sprintf("%.3f", $x);
}

sub get_cartesian_y {
	my ($r, $theta) = @_;
	
	my $y = $r * sin(degrees_to_radians($theta));
	return sprintf("%.3f", $y);
}

sub degrees_to_radians { 
    my ($degrees) = @_; 
    my ($radians); 
    $radians = atan2(1,1) * $degrees / 45; 
    return $radians;
} 