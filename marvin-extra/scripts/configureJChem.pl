#!/usr/bin/perl -w
#
# configureJChem.pl WHO JCHEM
#
# example: configureJChem.pl DEV-4.6 jchem20.16
#

use strict;
use File::Path;

# constants
my $base = '/home/aceorg/aceorg';
my $tomcat = 'tomcat9';
my $debug = 0;

# move to known place
chdir $base or die("cannot change to $base/\n");

# get parameters
my ($who, $jchem) = @ARGV;

defined($jchem) or die("Usage: $0 WHO JCHEM\n");
-d "$base/$who" or die("There is no directory called $base/$who\n");
-d "$base/$jchem" or die("There is no directory called $base/$jchem\n");
my $extras = "$base/$who/marvin-extra";
-d $extras or die("I can't find directory $extras\n");
# Marvin folder no longer exists
# my $marvin = "$base/$jchem/marvin";
# -d "$marvin" or die("There is no directory called $marvin\n");

print "Debugging only; no action taken.\n" if $debug;
# symlinks
# fixSymlink($jchem, "$base/jchem$who"); # can manually redirect if needed
mkdir "$base/$jchem/config";
fixSymlink("$extras/evaluator.xml", "$base/$jchem/config/evaluator.xml");
my $command = "find $base/$who/ace/web/WEB-INF/lib/ -type l -delete";
print "$command\n";
system($command) unless $debug;
$command = "lndir $base/jchem$who/lib/ $base/$who/ace/web/WEB-INF/lib/";
print "$command\n";
system($command) unless $debug;
my $tomcat = "/home/tomcat9/.chemaxon";
mkdir $tomcat unless $debug;
fixSymlink("$extras/evaluator.xml", "$tomcat/evaluator.xml");
$jchem =~ /jchem([0-9\.]+)$/;
$tomcat .= "/$1";
mkdir $tomcat unless $debug;
fixSymlink("$extras/evaluator.xml", "$tomcat/evaluator.xml");


# Marvin -- no longer used, so no longer configured
# system("perl $base/scripts/configureMarvin.pl $who $jchem/marvin");
# Marvin4JS -- for now, not configured routinely with JChem
# my $version = $jchem;
# $version =~ s/jchem(.*)/$1/ or die("can't run regexp on $jchem\n");
# system("perl $base/scripts/configureM4JS.pl $who marvin4JS$version");

sub fixSymlink {
	# only if necessary, and using relative paths
	my ($old, $new) = @_;
	my $dest;
	if ($old =~ /^\//) {
		my @old = split('/', $old);
		my @new = split('/', $new);
		while (@old and  @new and $old[0] eq $new[0]) {
			shift @old;
			shift @new;
		}
		$dest = ("../" x ((scalar @new)-1)) . join('/', @old);
	} else {
		$dest = $old;
	}
	# print "file $new should point to $dest\n";
	if (-l $new ) {
		my $current = readlink($new);
		if ($current eq $dest) {
			print "$new : good symlink already exists\n";
			return;
		} else {
			print "$new : bad symlink exists to $current; should be to $dest\n";
			return if $debug;
			if (unlink($new) != 1) {
				print "WARNING could not unlink $new\n";
			}
		}
	} elsif (-f $new) {
		print "$new: regular file already exists\n";
		return;
	}
	print "linking $dest <- $new\n";
	return if $debug;
	my $success = symlink $dest, $new;
	print "linking failed!\n" unless $success;
} # fixSymlink
