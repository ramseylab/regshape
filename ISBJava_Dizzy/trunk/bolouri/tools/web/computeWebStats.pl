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
# hostnames are stored in a cache file in Berkeley database ("DB") format.  The
# script also computes the number of times that the Dizzy and ISBJava programs
# were downloaded, and creates JPEG images representing these counter values,
# and uploads the JPEG images of the web counters to the image directory on
# the web server.
#
# Usage:  computeWebStats.pl [app1] [installer1] [app2] [installer2] ...
# 
# where "app1" is the application name of the first application, and
# "installer1" is the name of the installer of the first application, and so on.
#
# Required programs that must be installed in order to use this software are:
#  - NCFTP (specifically, we use the "ncftpget" program from this package)
#  - Webalizer (which also requires the Berkeley Database library "libdb" with the v1.85-compatible API)
#  - Perl
#  - Math::Trig (a Perl module available at CPAN)
#  - GD::Image (a Perl module available at CPAN)
#
# Stephen Ramsey
# 2004/06/09
#
use strict;
use lib ("/local/lib/perl5/site_perl/5.8.0/i386-linux-thread-multi");
use GD;

sub SCRATCH_DIR() {'/local/var/webstats'}
sub TEMP_DIR() {'/tmp'}
sub DOCUMENT_ROOT() {'/local/apache/htdocs'}
sub BIN_DIR() {'/local/bin'}
sub LOG_FILE_PREFIX() {'BolouriWebStats'}
sub USERNAME() {'bolouri_group'}
sub PASSWORD() {'b2G4cIc'}
sub FTP_SERVER() {'labs.systemsbiology.net'}
sub REMOTE_FTP_DIR() {'privateRepository'}
sub DNS_CACHE_FILE() {'dns_cache.db'}
sub WEB_STATS_WEB_SUBDIR() {'webstats'}

sub text_to_jpg($$)
{
    my $value = shift(@_);
    my $outputFile = shift(@_);

    my $im = GD::Image->new(gdLargeFont->width*length($value),
                            gdLargeFont->height);
    $im->colorAllocate(255,255,255);
    $im->string(gdLargeFont,0,0,$value,$im->colorAllocate(0,0,0));
    open(OUTPUT_FILE, ">$outputFile") or die("unable to open output file: $outputFile");
    binmode OUTPUT_FILE;
    print OUTPUT_FILE $im->jpeg(100);
    close(OUTPUT_FILE);
}

sub showUsage()
{
    print "Usage:\ncomputeWebStats appName installerName\n";
}

sub computeWebStats($$$)
{
    my $logFile = $_[0];
    my $appName = $_[1];
    my $installerName = $_[2];
    
    # handle application downloads counter
    open(DOWNLOADS, "<" . SCRATCH_DIR . "/${appName}Downloads.txt") or die("unable to open ${appName} downloads file, for reading");
    my $numDownloads = <DOWNLOADS>;
    chomp($numDownloads);
    close(DOWNLOADS);
    if(! defined($numDownloads))
    {
        $numDownloads = 0;
    }
    $numDownloads += `/bin/grep '/${installerName}' $logFile | /bin/grep ${appName} | /usr/bin/cut -f1 -d\\ | /bin/sort -u | /usr/bin/wc --lines`;
    open(DOWNLOADS, ">" . SCRATCH_DIR . "/${appName}Downloads.txt") or die("unable do open ${appName} downloads file, for writing");
    print DOWNLOADS $numDownloads . "\n";
    close(DOWNLOADS);
    text_to_jpg($numDownloads, SCRATCH_DIR . "/${appName}Downloads.jpg");
    system(BIN_DIR . "/ncftpput -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " software/${appName}/images " . SCRATCH_DIR . "/${appName}Downloads.jpg") and die("unable to FTP ${appName}Downloads.jpg file to FTP server");
    
    # handle visitors counter
    open(VISITORS, "<" . SCRATCH_DIR . "/${appName}Visitors.txt") or die("unable to open ${appName} visitors file, for reading");
    my $numVisitors = <VISITORS>;
    chomp($numVisitors);
    close(VISITORS);
    if(! defined($numVisitors))
    {
        $numVisitors = 0;
    }
    $numVisitors += `/bin/grep '/${appName}/ HTTP' $logFile | /usr/bin/wc --lines`;
    open(VISITORS, ">" . SCRATCH_DIR . "/${appName}Visitors.txt") or die("unable do open ${appName} visitors file, for writing");
    print VISITORS $numVisitors . "\n";
    close(VISITORS);
    text_to_jpg($numVisitors, SCRATCH_DIR . "/${appName}Visitors.jpg");
    system(BIN_DIR . "/ncftpput -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " software/${appName}/images " . SCRATCH_DIR . "/${appName}Visitors.jpg") and die("unable to FTP ${appName}Visitors.jpg file to FTP server");
    
}

sub main()
{
    my $curTime = localtime();
    my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime(time);
    my $week = ($yday + 1)/ 7;
    $year += 1900;
    my $suffix = sprintf("%04d-%02d", $year, $week);
    my $logFilePrefix = SCRATCH_DIR . '/' . LOG_FILE_PREFIX;
    my $logFile = $logFilePrefix . '-' . $suffix . '.log';
    my $sysCall = BIN_DIR . "/ncftpget -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " " . SCRATCH_DIR . " " . REMOTE_FTP_DIR . "/" . LOG_FILE_PREFIX . ".log";
    system($sysCall) and die("unable to obtain web statistics file");
#    warn $sysCall . "\n";
    $sysCall = "/bin/mv " . SCRATCH_DIR . '/' . LOG_FILE_PREFIX . '.log ' . $logFile;
#    warn $sysCall . "\n";
    system($sysCall) and die("unable to move file\n"); 
    my $tempFile = TEMP_DIR . "/webstats-" . time() . "-" . $$ . ".log";
    system("/bin/cat " . $logFilePrefix . "*.log >> " . $tempFile) and die("unable to cat files to temp file: $tempFile");
    system(BIN_DIR . "/webalizer  -D " . SCRATCH_DIR . "/" . DNS_CACHE_FILE . " -N 10 -o " . DOCUMENT_ROOT . "/" . WEB_STATS_WEB_SUBDIR . " " . $tempFile) and die("unable to analyze log files");
    unlink($tempFile) or die("unable to delete temp file $tempFile");

    while(defined(my $appName = shift(@ARGV)))
    {
    	my $installerName = shift(@ARGV);
    	if(! defined($installerName))
    	{
	 		die("no installer defined for app name: ${appName}");
    	}
    	
	    computeWebStats($logFile, $appName, $installerName);
    }
}

main();
exit(0);

