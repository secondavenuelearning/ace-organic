#!/usr/bin/perl -w
#
# [sudo] configureMJS.pl WHO MARVIN4JS
#
# example: [sudo] configureMJS.pl DEV-3.8 marvinJS14.11.20.0
#
# You need sudo if files in the marvinJS directory are not writable by this
# user.
#
# puts in required symlinks in marvinJS
# Modified 12/31/2014 Raphael, Bob
# Modified 3/31/2015 Raphael: more forgiving of errors, better messages

use strict;
use File::Path;

# constants
my $base = '/home/aceorg/aceorg';

# variables
my ($who, $m4js) = @ARGV;
defined($m4js) or die("Usage: $0 WHO MARVIN\n\t(eg: $0 DEV-3.8 marvinJS14.11.10)\n");
my $extras = "$base/$who/marvin-extra";

sub doAll {
	umask 02; # allow group read-write
	# current working directory
	chdir $base or die("cannot change to $base/\n");
	# parameters
	# verification
	-d "$base/$who" or die("There is no directory called $base/$who\n");
	-d "$base/$m4js" or die("There is no directory called $base/$m4js\n");
	# license file
	fixSymlink("/home/tomcat9/.chemaxon/license.cxl",
		"$base/$m4js/licenses/license.cxl");
	# symlinks in $m4js
	mkdir "$base/$m4js/templates/"; # in case it doesn't exist
	fixSymlink("$base/$who/marvin-extra/3DTemplatesMJS.json", 
		"$base/$m4js/templates/3DTemplates.json");
	# my $jquery = glob("$base/$m4js/js/lib/jquery-*.min.js"); # raphael
	my $jquery = glob("$base/jquery-*/jquery-*.min.js");
	die("no match for jquery\n") unless defined($jquery);
	# fixSymlink($jquery, "$base/$m4js/js/lib/jquery.min.js"); # raphael
	fixSymlink($jquery, "$base/$m4js/js/jquery.min.js");
	my $promiseLink = "$base/$m4js/js/promise.min.js";
	my $promiseTarget1 = "$base/$m4js/gui/lib/promise-*.min.js";
	my $promiseTarget2 = "$base/$m4js/js/promise-*.min.js";
	my @promise = glob("$promiseTarget1 $promiseTarget2");
	if (scalar @promise) {
		# there may be several versions; take the one with the highest number
		fixSymlink($promise[-1], $promiseLink);
	} else {
		warn("I can find neither $promiseTarget1 or $promiseTarget2\n");
	}
	# ACE version symlinks
	fixSymlink("$base/$m4js", "$base/marvinJS$who");
	fixSymlink("$base/marvinJS$who", 
		"$base/$who/ace/web/nosession/marvinJS");
	# file insertions
	fixHTML("editor.html");
	fixHTML("editorws.html");
	fixHTML("demo.html");
	fixJS();
	# CSS modifications
	fixCSS("css/doc.css");
} # doAll

sub fixHTML {
	my ($fileName) = @_;
	# add important line to file fileName
	my $fullName = "$base/marvinJS$who/$fileName";
	open FILE, $fullName or do {
		warn("Cannot read $fullName.  Does it still exist?\n");
		return;
	};
	$/ = undef; # slurp mode
	my $text;
	$text = <FILE>;
	close FILE;
	if ($text =~ /nosession/) { # already adjusted
		print "$fullName: already adjusted\n";
		return;
	}
	my @newText;
	for my $line (split /\n/, $text) {
		push @newText, $line;
		if ($line =~ /marvin\.Sketch\.isSupported/) {
			if ($fileName eq "demo.html") {
				push @newText,
					"\t\t\t\tmarvin.Sketch.license(" .
					"'licenses/license.cxl');";
			} else {
				push @newText,
					"\t\t\t\tmarvin.Sketch.license(" .
					"'/nosession/license.cxl');";
			} # demo.html vs. editor*.html
		} # add line
	} # each line
	open FILE, ">$fullName" or die("Cannot write $fullName; try sudo $0\n");
	print FILE join("\n", @newText);
	close FILE;
} # fixHTML

sub fixJS {
	# add important line to file js/webservices.js
	my ($fullName, $text, @newText);
	$fullName = "$base/marvinJS$who/js/webservices.js";
	open FILE, $fullName or die("Cannot read $fullName.  Does it still exist?\n");
	$/ = undef; # slurp mode
	$text = <FILE>;
	close FILE;
	if ($text =~ /webservices"/) { # already adjusted
		print "$fullName: already adjusted\n";
	} else {
		$text =~ s/\/webservices2/https:\/\/epoch.uky.edu\/webservices/;
		open FILE, ">$fullName" or die("Cannot write $fullName; try sudo $0\n");
		print FILE $text;
		close FILE;
	}
	# remove line from file js/lib/jquery.min.js
	# $fullName = "$base/marvinJS$who/js/lib/jquery.min.js"; # raphael
	$fullName = "$base/marvinJS$who/js/jquery.min.js";
	open FILE, $fullName or die("Cannot read $fullName.  Does it still exist?\n");
	$/ = undef; # slurp mode
	$text = <FILE>;
	close FILE;
	@newText = ();
	for my $line (split /\n/, $text) {
		next if $line =~ /\@ sourceMappingURL=jquery.min.map/;
		push @newText, $line;
	} # each line
	open FILE, ">$fullName" or die("Cannot write $fullName; try sudo $0\n");
	print FILE join("\n", @newText) . "\n";
	close FILE;
} # fixJS

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

sub fixCSS {
	my ($fileName) = @_;
	# remove lines from file fileName
	my $fullName = "$base/marvinJS$who/$fileName";
	open FILE, $fullName or die("Cannot read $fullName.  Does it still exist?\n");
	$/ = undef; # slurp mode
	my $text;
	$text = <FILE>;
	close FILE;
	my @newText;
	for my $line (split /\n/, $text) {
		next if $line =~ /background: #29999c;/;
		next if $line =~ /background: #f9f9f9;/;
		next if $line =~ /list-style-type: none;/;
		next if $line =~ /td \{ vertical-align: top;/;
		$line =~ s/\.resizable\{/.oldresizable{/;
		push @newText, $line;
	} # each line
	open FILE, ">$fullName" or die("Cannot write $fullName\n");
	print FILE join("\n", @newText);
	close FILE;
} # fixCSS

doAll();
