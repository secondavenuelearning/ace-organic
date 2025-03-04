#!/usr/bin/perl -w
#
# cpSole FROM TO
#
# copy files that exist only in FROM to TO.
# Raphael Finkel 10/2008


use strict;
use File::Path;

my $real = 0;

my $ok = defined($ARGV[1]) and (-d $ARGV[0]) and (-d $ARGV[1]);
if (!$ok) {
	print "Usage: $0 FROMDIR TODIR\n";
	exit(1);
}

my @fromList = split(/\s+/, `find $ARGV[0] -type f`);
my @toList = split(/\s+/, `find $ARGV[1] -type f`);
my %toFiles;
for my $toFile (@toList) {
	$toFile =~ s/$ARGV[1]\///;
	next if $toFile =~ /\.svn|\.class/;
	$toFiles{$toFile} = 1;
	# print "I see $toFile\n"; # debug
}
for my $fromFile (@fromList) {
	my $shortFrom = $fromFile;
	$shortFrom =~ s/^$ARGV[0]\///;
	next if $shortFrom =~ /\.svn|\.class|\/public|\/logger\/|\/temp|\.tmp$/;
	next if defined($toFiles{$shortFrom});
	my $dir = "$ARGV[1]/$shortFrom";
	$dir =~ s/\/[^\/]*$//; # leave just the directory
	# print "directory $dir\n";
	if ($real) {
		if (! -d $dir) {
			# print "Creating $newDir\n";
			mkpath $dir;
		}
		system("cp $fromFile $ARGV[1]/$shortFrom");
	} else {
		print "cp $fromFile $ARGV[1]/$shortFrom\n";
	}
} # each from file
