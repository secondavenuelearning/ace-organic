#!/usr/bin/perl -w
#
# Program to send email

use strict;

# email address of person to receive email
my $email = $ARGV[1];
$email =~ tr/@/\@/;
$email =~ s/[;&\(\)\s]//; # be careful about address: avoid secuity holes

# subject of email
my $subject = "ACE:_Your_New_Password";

# set the path below here to your mail transfer application
open(MAIL, "|/bin/mail -s $subject $email") || die ("can't open mail");

# contents of email
print MAIL "Student Name: $ARGV[0]\nNew Password: $ARGV[2]\n\nPlease login and change your password immediately.\n\nUsername and Passwords are case-sensative.";

close(MAIL);
