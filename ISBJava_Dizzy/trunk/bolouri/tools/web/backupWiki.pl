#!/usr/bin/perl -w
#
# Copyright (C) 2003 by Institute for Systems Biology,
# Seattle, Washington, USA.  All rights reserved.
# 
# This source code is distributed under the GNU Lesser 
# General Public License, the text of which is available at:
#   http://www.gnu.org/copyleft/lesser.html
#
# Stephen Ramsey, Institute for Systems Biology
# Mar. 2006
#
# Backs up the Wiki on Jujube
#
use strict;

sub BACKUP_DIR() {'/proj/ilyalab/sramsey/Wiki/Backups'}
sub WIKI_DIR()  {'/local/var/www'}

my $curTime = localtime();
my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime(time);
my $week = ($yday + 1)/ 7;
$year += 1900;
my $numericVal = ($year * 10000) + ($mon + 1)*100 + $mday;

my $suffix = sprintf("%08d", $numericVal);

my $backupFile = BACKUP_DIR . '/' . 'Wiki-' . $suffix . '.tar.gz';
my $wikiDir = WIKI_DIR;
my $excludeFile = 'Wiki/data/cache/*';
my $cmd = "tar -C $wikiDir --exclude $excludeFile -p -c -z -f $backupFile Wiki";
system($cmd) and die("unable to backup wiki directory\n");

my $deleteFile = BACKUP_DIR . '/' . 'Wiki-' . sprintf("%08d", $numericVal - 100) . '.tar.gz';
my $deleteCmd = "rm -f $deleteFile";
system($deleteCmd) and die("unable to delete old backups of wiki directory\n");



