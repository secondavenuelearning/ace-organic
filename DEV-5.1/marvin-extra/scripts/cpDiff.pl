#!/usr/bin/perl -w
#
# cpDiff FROM TO
#
# copy only files that exist on both sides, that differ between the two.
# Raphael Finkel 10/2008


use strict;
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
		next if $toFile =~ /\.svn|\.class|\/temp/;
	$toFiles{$toFile} = 1;
	# print "I see $toFile\n"; # debug
}
for my $fromFile (@fromList) {
	my $shortFrom = $fromFile;
	$shortFrom =~ s/^$ARGV[0]\///;
	if (defined($toFiles{$shortFrom})) {
		# print "Checking $fromFile\n";
		my $diffCommand = "diff -q $fromFile $ARGV[1]/$shortFrom";
		# print "\t$diffCommand\n";
		if (`$diffCommand` =~ /differ/) {
			print "cp $fromFile $ARGV[1]/$shortFrom\n";
			if ($real) {
				system("cp $fromFile $ARGV[1]/$shortFrom");
			}
		}
	} # exists on both
} # each from file
