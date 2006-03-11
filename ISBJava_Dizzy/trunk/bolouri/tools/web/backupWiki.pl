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
my $suffix = sprintf("%04d-%02d-%02d", $year, $mon + 1, $mday);

my $backupFile = BACKUP_DIR . '/' . 'Wiki-' . $suffix . '.tar.gz';
my $wikiDir = WIKI_DIR;
my $cmd = "tar -C $wikiDir -p -c -z -f $backupFile Wiki";
system($cmd) and die("unable to backup wiki directory\n");


