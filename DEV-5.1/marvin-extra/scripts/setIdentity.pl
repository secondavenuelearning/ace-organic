#!/usr/bin/perl -w
#
# setIdentity.pl WHO
#
# example: setIdentity.pl DEV-3.5
#
# modifies the files in identity/ to correspond to ACE version WHO.

use strict;
use File::Path;

# constants
my $startDir = '/home/aceorg/aceorg';
my $debug = 0; # set to 1 to merely announce, not perform

my $who;
sub init {
	# move to known place
	chdir $startDir or die("cannot change to $startDir/\n");
	# get parameters
	$who = $ARGV[0];
	defined($who) or die("Usage: $0 WHO");
	-d $who or die("There is no directory called $who");
} # init

sub copyFiles { # copy files with modifications as needed
	for my $file (split /\n/, `find identity/ -type f`) {
		# print "working on $file\n";
		if ($file =~ /\.jar$/) { # copy jar files
			my $destination = $file;
			$destination =~ s/identity/$who/;
			next if (-f $destination);
			print("copying $file to $destination\n");
			if (!$debug) {
				system("cp $file $destination");
			}
		} else {
			open FILE, $file or die("cannot read $file");
			$/ = undef; # read in one go
			my $contents = <FILE>;
			close FILE;
			$contents =~ s/WHO/$who/g or print "no reference to WHO in $file\n";
			$file =~ s/identity/$who/;
			print "Installing [$file]\n";
			if (!$debug) {
				prepareDir($file);
				-f $file or warn("no file $file to modify");
				open FILE, ">$file" or die("cannot write $file");
				print FILE $contents;
				close FILE;
			}
		} # not a jar file
	} # all files in "identity"
} # copyFiles

sub buildSymlinks {
	# establish symlinks with modifications as needed
	for my $file (split /\n/, `find identity/ -type l`) {
		# print "working on symlink $file\n";
		my $content = readlink $file;
		$content =~ s/WHO/$who/;
		$file =~ s/identity/$who/;
		if (-l $file) {
			my $oldContent = readlink $file;
			if ($oldContent eq $content) {
				print "$file already a good symlink; not modifying\n";
			} else { # exists, but wrong
				print "Modifying symlink $file -> $content\n";
				if (!$debug) {
					unlink $file;
					symlink $content, $file;
				}
			}
		} elsif (-f $file) {
			print "$file exists but is not a symlink!\n";
		} else {
			print "Installing symlink $file -> $content\n";
			if (!$debug) {
				prepareDir($file);
				symlink $content, $file;
			} # not debug
		}
	} # each symlink
} # buildSymlinks

sub buildDirectories { # create necessary directories
	for my $dir ("$who/ace/build", "$who/ace/web/WEB-INF/classes/com/epoch") {
		if (! -d $dir) {
			print "Creating $dir\n";
			if ($debug) {
				mkpath $dir;
			} # not debug
		} # dir does not exist
	} # create necessary directories
} # buildDirectories

sub setPermissions { # set writeability on necessary directories
	for my $dir (
		"$startDir/$who/ace/web/tempfiles",
		"$startDir/$who/ace/web/logger",
		"$startDir/$who/ace/web/figures",
		"$startDir/$who/ace/web/user_figures",
		) {
		if (!-d $dir) {
			print "creating $dir\n";
			if (!$debug) {
				mkpath $dir;
				print "setting $dir so tomcat9 may write\n";
				system("chgrp tomcat9 $dir");
				chmod 02775, $dir or warn('failed to set mode');
			} # not debug
		} # dir does not exist
	} # set writeability
} # setPermissions

sub prepareDir {
	my ($newDir) = @_;
	$newDir =~ s/(.*)\/.*/$1/;
	# print "the dir is $newDir\n"; exit(0);
	if (! -d $newDir) {
		print "Creating $newDir\n";
		mkpath $newDir;
	} # dir does not exist
} # prepareDir

# main program
init(); 
copyFiles();
buildSymlinks();
buildDirectories();
setPermissions();
