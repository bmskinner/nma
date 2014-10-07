#!/usr/bin/perl
# analysis of sperm_morph script

use warnings;
use strict;
use IO::File;
use Getopt::Long;
use File::Spec;
use Statistics::Descriptive;
use DBI;
use Data::Dumper;
# read file
# recognise headers
# get parameter data
# get coordinates

# make dat
# make plot

# Sample:
# ~ ------------------
# # LOGGED PARAMETERS
# # ------------------
# Nuclear Perimeter	728
# Nuclear Area	820
# Feret Diameter	47.8017
# Curve length	36.7686
# Max Y	6.6771
# # ------------------
# # J:\Images\2014-10-06_DAPI-lectin\WT\//01.tiff.1.txt
# # ------------------
# 0	6.162
# 2.5763	6.6771
# 7.2202	4.1056



my $inputFile = $ARGV[0];
my $result = read_input($inputFile);

# print Dumper $result;
my $datfile;
my $line = "";

foreach my $nucleus ( @$result){

	if(${$nucleus}{xr} > 0.7){
		$datfile = make_x_y_dat($nucleus);
		$line .= addCurveToLine($nucleus);
	}
}
make_plot_file($datfile, $line);

sub read_input {
	my $infile = shift;
	my @array;
	my @coordinates;
	my %nucleus;
	my $data; # flag to confirm we are in data block
		
	open IN, $infile or die "$!";

	my $line_within_log;
	
	while (<IN>){
		
		chomp;
		$_ =~ s/\R//; # remove \r from windows file
		# if($_ =~ m/~/){ # new image
			# $line_within_log = 1
		# }
		
		if($_ =~ m/# END OF FILE/){ # end of log
			$nucleus{coordinates} = [@coordinates];
			push @array, {%nucleus};
			undef %nucleus;
			undef @coordinates;
			$data = 0;
			next;
		}
		
		if($_ =~ m/^#/){ # filename
			$data = 1;
			next;
		}
		
		if($_ =~ m/^IN/){ # filename
			(undef, $nucleus{image}) = split(" ",$_);
		}
		if($_ =~ m/^ID/){ # filename
			(undef, $nucleus{dir}) = split(" ",$_);
		}
		if($_ =~ m/^LF/){ # filename
			(undef, $nucleus{log_file}) = split(" ",$_);
		}
		if($_ =~ m/^NP/){ # filename
			(undef, $nucleus{perimeter}) = split(" ",$_);
		}	
		if($_ =~ m/^NA/){ # filename
			(undef, $nucleus{nuclear_area}) = split(" ",$_);
		}	
		if($_ =~ m/^FD/){ # filename
			(undef, $nucleus{feret_diameter}) = split(" ",$_);
		}
		if($_ =~ m/^CL/){ # filename
			(undef, $nucleus{curve_length}) = split(" ",$_);
		}
		if($_ =~ m/^MY/){ # filename
			(undef, $nucleus{max_y}) = split(" ",$_);
		}
		if($_ =~ m/^XA/){ # filename
			(undef, $nucleus{xa}) = split(" ",$_);
		}
		if($_ =~ m/^XB/){ # filename
			(undef, $nucleus{xb}) = split(" ",$_);
		}
		if($_ =~ m/^XC/){ # filename
			(undef, $nucleus{xc}) = split(" ",$_);
		}
		if($_ =~ m/^XR/){ # filename
			(undef, $nucleus{xr}) = split(" ",$_);
		}

		if($data) {
			
			my %hash;
			my ($a, $b) = split("\t",$_);
			$hash{x} = $a;
			$hash{y} = $b;
			push @coordinates, {%hash};
			
		}
	}

	close IN;
	return \@array;
}

sub make_x_y_dat {
	my $nucleus = shift;
	
	my $name = ${$nucleus}{image};
	my $datfile  = ${$nucleus}{dir} . "data.dat";
	# my $pngfile  = ${$nucleus}{dir} . "data.png";
	# my $plotfile = ${$nucleus}{dir} . "data.plot";
	
	open DAT, ">>$datfile" or die "$!";

	foreach my $profile (${$nucleus}{coordinates}){

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
	
	my $equation = "(" . ${$nucleus}{xc} . "*x**2)+(" . ${$nucleus}{xb} . "*x)+" . ${$nucleus}{xa};
	
	my $text = ', \\' . "\n" . $equation . " with lines lc rgb 'light-grey' ";
	return $text;
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
	