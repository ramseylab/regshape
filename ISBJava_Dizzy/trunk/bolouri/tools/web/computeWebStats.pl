#!/usr/bin/perl
#
# Copyright (C) 2003 by Institute for Systems Biology,
# Seattle, Washington, USA.  All rights reserved.
# 
# This source code is distributed under the GNU Lesser 
# General Public License, the text of which is available at:
#   http://www.gnu.org/copyleft/lesser.html
#
# Retrieves the  Apache web server "access" log file from the web server machine,
# copies the file to a repository directory, and runs the analysis software to generate
# a web page of statistics that is published on the localhost web server.
# The log file obtained is actually a fragment of the total web server's "access"
# log file, containing the word "bolouri".  This restricts the log file to only
# accesses to Bolouri Group-related web content.  The log file fragment is retrieved
# via FTP from the web server machine.  The analysis program that is run is called
# Webalizer.  The IP addresses that appear in the log file are translated by
# the Webalizer program into hostnames, using DNS lookups.  The IP addresses and
# hostnames are stored in a cache file in Berkeley database ("DB") format.
#
# Required programs that must be installed in order to use this software:
#  - NCFTP (specifically, we use the "ncftpget" program from this package)
#  - Webalizer (which also requires the Berkeley Database library "libdb" with the v1.85-compatible API)
#  - Perl
#
# Stephen Ramsey
# 2004/06/09
#
sub SCRATCH_DIR() {'/local/var/webalizer'}
sub DOCUMENT_ROOT() {'/local/apache/htdocs'}
sub BIN_DIR() {'/local/bin'}
sub FILE_PREFIX() {'BolouriWebStats'}
sub USERNAME() {'bolouri_group'}
sub PASSWORD() {'b2G4cIc'}
sub FTP_SERVER() {'labs.systemsbiology.net'}
sub FTP_DIR() {'privateRepository'}

my $curTime = localtime();
my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime(time);
my $week = ($yday + 1)/ 7;
$year += 1900;
my $suffix = sprintf("%4d-%2d", $year, $week);
my $logFilePrefix = SCRATCH_DIR . '/' . FILE_PREFIX;
my $logFile = $logFilePrefix . '-' . $suffix . '.log';
system(BIN_DIR . "/ncftpget -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " " . SCRATCH_DIR . " " . FTP_DIR . "/" . FILE_PREFIX . ".log") and die("unable to obtain web statistics file");
my $temp = SCRATCH_DIR . '/' . FILE_PREFIX . '.log ' . $logFile;
system("/bin/mv " . $temp); 
system(BIN_DIR . "/webalizer  -D " . SCRATCH_DIR . "/dns_cache.db -N 10 -o " . DOCUMENT_ROOT . "/webstats " . $logFilePrefix . '*.log') and die("unable to analyze log files");
 


