#!/usr/bin/perl -w
#
# configureMarvin.pl WHO MARVIN
#
# example: configureMarvin.pl DEV-2.2 jchem5.3.2/marvin
#
# puts in required symlinks to marvin-extra

use strict;
use File::Path;

# constants
my $base = '/home/aceorg/aceorg';

# move to known place
chdir $base or die("cannot change to $base/\n");

# get parameters
my ($who, $marvin) = @ARGV;

defined($marvin) or die("Usage: $0 WHO MARVIN\n");
-d "$base/$who" or die("There is no directory called $base/$who\n");
-d "$base/$marvin" or die("There is no directory called $base/$marvin\n");
my $extras = "$base/$who/marvin-extra";
-d $extras or die("I can't find directory $extras\n");

# symlinks to templates and abbreviated groups
for my $oldFile (<$extras/ACE*>) {
	# print "working on $oldFile\n";
	my $newdir = "$base/$marvin/chemaxon/marvin/templates";
	-d $newdir or die("I can't find directory $newdir\n");
	my $tail = $oldFile;
	$tail =~ s/(.*)\/([^\/]*)$/$2/ or die("can't run regexp\n");
	# print "tail is $tail\n";
	fixSymlink($oldFile, "$newdir/$tail");
} # all files in "identity"

# symlinks to marvin configuration files
for my $oldFile (<$extras/configuration*>) {
	# print "working on $oldFile\n";
	my $newdir = "$base/$marvin";
	-d $newdir or die("I can't find directory $newdir\n");
	my $tail = $oldFile;
	$tail =~ s/(.*)\/([^\/]*)$/$2/ or die("can't run regexp\n");
	# print "tail is $tail\n";
	fixSymlink($oldFile, "$newdir/$tail");
} # all files in "identity"

# symlink to license file
fixSymlink("/usr/share/tomcat8/.chemaxon/license.cxl", "$base/$marvin/license.cxl");

# symlink to new Marvin version in WHO/.../nosession
my $version = $marvin;
$version =~ s/jchem(.*)\/marvin/$1/ or die("can't run regexp on $marvin\n");
fixSymlink("$base/$marvin", "$base/$who/ace/web/nosession/marvin$version");

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
		if (readlink($new) eq $dest) {
			print "$new: good symlink already exists\n";
			return;
		} else {
			print "$new: bad symlink exists; fixing\n";
		if (unlink($new) != 1) {
				print "WARNING could not unlink $new\n";
			}
		}
	} elsif (-f $new) {
		print "$new: regular file already exists\n";
		return;
	}
	print "linking $dest $new\n";
	symlink $dest, $new;
} # fixSymlink
