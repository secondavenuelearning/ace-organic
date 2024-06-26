#!/usr/bin/perl -w
#
# copy relevant parts of a development version to a production version
#
# Raphael Finkel 11/2006
#

use strict;
use File::Path;

my ($fromName, $toName) = @ARGV;
if (!defined($toName)) {
	print "usage: $0 FROM TO\n";
	exit(1);
}

# constants
	my $base = '/home/aceorg/aceorg';
	my $u = "$base/$toName";
	warn("Warning: destination directory $u already exists.\n") if (-d $u);
	my $s = "$base/$fromName";
	die("Source directory $s does not exist.\n") unless (-d $s);
	my $ut = "$u/ace/src/com/epoch";
	my $st = "$s/ace/src/com/epoch";
	my $sj = "$s/ace/web";
	my %doNotCopy = ( # files that ought not be copied.  None at present.
	);

sub prepareDir {
	my ($newDir) = @_;
	$newDir =~ s/(.*)\/.*/$1/;
	# print "the dir is $newDir\n"; exit(0);
	if (! -d $newDir) {
		# print "Creating $newDir\n";
		mkpath $newDir;
	}
} # prepareDir

sub copyJava {
	# don't copy .svn files.  Need to copy file by file.
	print "Copying Java source\n";
	my $javaFiles =
		`find $st \\( -name '*.java' \\) -o \\( -name 'Makefile*' \\) -o \\( -name 'package.html' \\)`;
	for my $file (split /\s+/m, $javaFiles) {
		next if $file =~ /\.svn\b/;
		my $new = $file;
		$new =~ s/$fromName/$toName/;
		prepareDir($new);
		# system "cp -p $file $new";
		system "rsync $file $new";
	}
} # copyJava

sub copyLewis {
	return; # obsolete as of ACE 3.6.
	# don't copy .svn files.  Need to copy file by file.
	print "Copying Lewis sketch\n";
	my $lewisFiles = `find $s/lewissketch -type f`;
	for my $file (split /\s+/m, $lewisFiles) {
		next if $file =~ /\.svn\b/;
		my $new = $file;
		$new =~ s/$fromName/$toName/;
		prepareDir($new);
		system "cp -p $file $new";
	}
} # copyLewis

sub copyJchemLink {
	print "Copying jchem links\n";
	system("cp -d $base/jchem$fromName $base/jchem$toName")
		if (!-e "$base/jchem$toName");
	# all versions share the same jchemWebServices now.
	# system("cp -d $base/jchemWebServices$fromName $base/jchemWebServices$toName")
	# 	if (!-e "$base/jchemWebServices$toName");
	my $lib = "$base/$toName/ace/web/WEB-INF/lib";
	prepareDir("$lib/dummy");
	system("cd $lib; lndir -silent $base/jchem$toName/lib");
	# system("cp -d $base/lewis$fromName $base/lewis$toName");
} # copyJchemLink

sub convertFile {
	my ($file) = @_;
	open FILE, $file or die("cannot read $file");
	$/ = undef; # read in one go
	my $contents = <FILE>;
	close FILE;
	if ($contents !~ s/$fromName/$toName/g) {
		print "no reference to $fromName in \n\t$file\n";
		return;
	}
	print "Installing modified $file\n";
	unlink $file;
	open FILE, ">$file" or die("cannot write $file");
	print FILE $contents;
	close FILE;
} # convertFile

sub updateFiles {
	print "Updating some files\n";
	for my $file (
			"$ut/Makefile.master",
			"$u/ace/web/nosession/email.jsp",
			"$u/ace/web/course/deleteCourse.jsp",
			"$u/ace/web/enroll/enrollRegistered.jsp",
			"$u/ace/web/login.jsp",
			"$u/ace/web/discovery.jsp",
			# the following files are modified by setIdentity().
			# "$u/ace/web/WEB-INF/epoch.properties",
			# "$u/ace/web/userHomeFrm.html",
			) {
		convertFile($file);
	}
} # updateFiles

sub setIdentity {
	print "setting identity\n";
	system("$base/scripts/setIdentity.pl $toName");
} # setIdentity

sub compile {
	print "Compiling\n";
	mkdir "$u/ace/build";
	system ("cd $ut; make unlock lock compileAll publishAll");
} # compile

sub copyWeb {
	# avoid both .svn files and distribution-specific files.
	print "Copying web files\n";
	my $webFiles = `find $sj -type f `;
	for my $file (split /\s+/m, $webFiles) {
		next if $file =~ /\.svn/;
		next if $file =~ /ace\/web\/logger\//;
		next if $file =~ /ace\/web\/tempfiles\//;
		# next unless $file =~ /\.(js|html)/;
		my $tail = (split(/\//, $file))[-1];
		next if defined $doNotCopy{$tail};
		my $new = $file;
		$new =~ s/$fromName/$toName/;
		prepareDir($new);
		system "cp -p $file $new";
	}
} # copyWeb

sub copyOther {
	print "Copying some other files\n";
	system "cp -pr $s/marvin-extra $u";
} # copyOther

sub warnings {
	print "The following files mention $fromName (should be none!)\n";
	system ("find $u -follow -name '*.js*' -type f -exec grep -l $fromName {} \\;");
	system ("find $u -follow -name 'Makefile.master' -type f -exec grep -l $fromName {} \\;");
} # warnings

my $degrade = 0; # degrading to a previous version?
copyJava() unless $degrade;
# copyLewis(); # obsolete as of ACE 3.6.
copyJchemLink();
copyWeb() unless $degrade;
setIdentity();
updateFiles();
compile();
copyOther() unless $degrade;
warnings();
