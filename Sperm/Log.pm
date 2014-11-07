package Sperm::Log;

# Copyright (C) Ben Skinner 2014
# This package contains common functions for handling sperm
# morphology analysis


use strict;
use Exporter;
use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);

$VERSION     = 0.01;
@ISA         = qw(Exporter);
@EXPORT      = ();
@EXPORT_OK   = qw(read_log);
%EXPORT_TAGS = ( DEFAULT => [qw(&read_log)],
                 Both    => [qw(&read_log &func2)]);

# An example log file:
# ~ ------------------
# IN~01.tiff
# ID~J:\Protocols\Scripts and macros\
# LF~J:\Protocols\Scripts and macros\//sample_sperm_morph.log
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
# ...
# 98.452	6.0112
# # END OF FILE

sub read_log  {

	my $infile = shift;
	my @array; # the complete array of nuclei data
	my @profiles; # anything from the data block
	my %nucleus; # the hash combining the data block with the header block
	my @columns; # the column names within the data block
	my $data; # flag to confirm if we are in data block
		
	open IN, $infile or die "Error in Sperm::Log::read_log: $!";

	while (<IN>){
		
		chomp;
		$_ =~ s/\R//; # remove \r from windows file
		
		if($_ =~ m/# END OF FILE/){ # end of log
						
			$nucleus{profiles} = [@profiles];
			push @array, {%nucleus};
			undef %nucleus;
			undef @columns;
			undef @profiles;
			$data = 0;
			next;
		}
		
		if($_ =~ m/^~/){ # first line of new nucleus. Skip
			next;
		}
		
		if($_ =~ m/^#/){ # data block about to start. Get variable names
			$data = 1;
			(my $line = $_ ) =~ s/^# //; # remove the # and space
			@columns = split("\t",$line);			
			next;
		}
		
		if(!$data){ # header block still
			my ($field, $value) = split("~",$_);
			$nucleus{$field} = $value;
		}

		if($data) {
			
			# assign the values to hashes with the appropriate name
			my %hash;
			my @data = split("\t",$_);
			
			for(my $i=0;$i<scalar @data;$i++){
				$hash{$columns[$i]} = $data[$i];
			}
			# add to the profile array
			push @profiles, {%hash};
		}		
	}

	close IN;
	return \@array;
}
	
sub func2  { return map{ uc }@_ }

1;

