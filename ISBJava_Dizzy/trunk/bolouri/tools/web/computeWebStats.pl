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
# Required programs that must be installed in order to use this software:
#  - NCFTP (specifically, we use the "ncftpget" program from this package)
#  - Webalizer (which also requires the Berkeley Database library "libdb" with the v1.85-compatible API)
#  - Perl
#  - Math::Trig (a Perl module available at CPAN)
#  - GD::Image (a Perl module available at CPAN)
#
# Stephen Ramsey
# 2004/06/09
#
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

sub main()
{
    my $curTime = localtime();
    my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime(time);
    my $week = ($yday + 1)/ 7;
    $year += 1900;
    my $suffix = sprintf("%4d-%2d", $year, $week);
    my $logFilePrefix = SCRATCH_DIR . '/' . LOG_FILE_PREFIX;
    my $logFile = $logFilePrefix . '-' . $suffix . '.log';
    system(BIN_DIR . "/ncftpget -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " " . SCRATCH_DIR . " " . REMOTE_FTP_DIR . "/" . LOG_FILE_PREFIX . ".log") and die("unable to obtain web statistics file");
    my $temp = SCRATCH_DIR . '/' . LOG_FILE_PREFIX . '.log ' . $logFile;
    system("/bin/mv " . $temp); 
    my $tempFile = TEMP_DIR . "/webstats-" . time() . "-" . $$ . ".log";
    system("/bin/cat " . $logFilePrefix . "*.log >> " . $tempFile) and die("unable to cat files to temp file: $tempFile");
    system(BIN_DIR . "/webalizer  -D " . SCRATCH_DIR . "/" . DNS_CACHE_FILE . " -N 10 -o " . DOCUMENT_ROOT . "/" . WEB_STATS_WEB_SUBDIR . " " . $tempFile) and die("unable to analyze log files");
    unlink($tempFile) or die("unable to delete temp file $tempFile");

    open(DIZZY_DOWNLOADS, "<" . SCRATCH_DIR . "/DizzyDownloads.txt") or die("unable to open Dizzy downloads file, for reading");
    my $numDizzyDownloads = <DIZZY_DOWNLOADS>;
    close(DIZZY_DOWNLOADS);
    if(! defined($numDizzyDownloads))
    {
        $numDizzyDownloads = 0;
    }
    $numDizzyDownloads += `/bin/grep insDizzy $logFile | /usr/bin/wc --lines`;
    open(DIZZY_DOWNLOADS, ">" . SCRATCH_DIR . "/DizzyDownloads.txt") or die("unable do open Dizzy downloads file, for writing");
    print DIZZY_DOWNLOADS $numDizzyDownloads . "\n";
    close(DIZZY_DOWNLOADS);
    text_to_jpg($numDizzyDownloads, SCRATCH_DIR . "/DizzyDownloads.jpg");
    system(BIN_DIR . "/ncftpput -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " software/Dizzy/images " . SCRATCH_DIR . "/DizzyDownloads.jpg") and die("unable to FTP DizzyDownloads.jpg file to FTP server");

    open(ISBJAVA_DOWNLOADS, "<" . SCRATCH_DIR . "/ISBJavaDownloads.txt") or die("unable to open ISBJava downloads file, for reading");
    my $numISBJavaDownloads = <ISBJAVA_DOWNLOADS>;
    close(ISBJAVA_DOWNLOADS);
    if(! defined($numISBJavaDownloads))
    {
        $numISBJavaDownloads = 0;
    }
    $numISBJavaDownloads += `/bin/grep insISBJ $logFile | /usr/bin/wc --lines`;
    open(ISBJAVA_DOWNLOADS, ">" . SCRATCH_DIR . "/ISBJavaDownloads.txt") or die("unable do open ISBJava downloads file, for writing");
    print ISBJAVA_DOWNLOADS $numISBJavaDownloads . "\n";
    close(ISBJAVA_DOWNLOADS);
    text_to_jpg($numISBJavaDownloads, SCRATCH_DIR . "/ISBJavaDownloads.jpg");
    system(BIN_DIR . "/ncftpput -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " software/ISBJava/images " . SCRATCH_DIR . "/ISBJavaDownloads.jpg") and die("unable to FTP ISBJavaDownloads.jpg file to FTP server");

    open(DIZZY_VISITORS, "<" . SCRATCH_DIR . "/DizzyVisitors.txt") or die("unable to open Dizzy visitors file, for reading");
    my $numDizzyVisitors = <DIZZY_VISITORS>;
    close(DIZZY_VISITORS);
    if(! defined($numDizzyVisitors))
    {
        $numDizzyVisitors = 0;
    }
    $numDizzyVisitors += `/bin/grep '/Dizzy/ HTTP' $logFile | /usr/bin/wc --lines`;
    open(DIZZY_VISITORS, ">" . SCRATCH_DIR . "/DizzyVisitors.txt") or die("unable do open Dizzy visitors file, for writing");
    print DIZZY_VISITORS $numDizzyVisitors . "\n";
    close(DIZZY_VISITORS);
    text_to_jpg($numDizzyVisitors, SCRATCH_DIR . "/DizzyVisitors.jpg");
    system(BIN_DIR . "/ncftpput -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " software/Dizzy/images " . SCRATCH_DIR . "/DizzyVisitors.jpg") and die("unable to FTP DizzyVisitors.jpg file to FTP server");

    open(ISBJAVA_VISITORS, "<" . SCRATCH_DIR . "/ISBJavaVisitors.txt") or die("unable to open ISBJava visitors file, for reading");
    my $numISBJavaVisitors = <ISBJAVA_VISITORS>;
    close(ISBJAVA_VISITORS);
    if(! defined($numISBJavaVisitors))
    {
        $numISBJavaVisitors = 0;
    }
    $numISBJavaVisitors += `/bin/grep '/ISBJava/ HTTP' $logFile | /usr/bin/wc --lines`;
    open(ISBJAVA_VISITORS, ">" . SCRATCH_DIR . "/ISBJavaVisitors.txt") or die("unable do open ISBJava visitors file, for writing");
    print ISBJAVA_VISITORS $numISBJavaVisitors . "\n";
    close(ISBJAVA_VISITORS);
    text_to_jpg($numISBJavaVisitors, SCRATCH_DIR . "/ISBJavaVisitors.jpg");
    system(BIN_DIR . "/ncftpput -u " . USERNAME . " -p " . PASSWORD . " " . FTP_SERVER . " software/ISBJava/images " . SCRATCH_DIR . "/ISBJavaVisitors.jpg") and die("unable to FTP ISBJavaVisitors.jpg file to FTP server");

}

main();
exit(0);

