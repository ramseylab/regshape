#!/usr/bin/python
import csv, sys
import os
print os.getcwd()

"""
Input: 
  <inputfile>   e.g. gwas_eligible.tsv

==> gwas_eligible.tsv <==
rs10007275  c   t   0.0333  0.000348    123843
rs10008064  a   g   0.5833  0.000214    123863
rs10009336  c   t   0.825   0.000579    123773

Output:
  <inputfile>.hg19.txt  e.g. gwas_eligible.hg19.txt

Required file: 
==> snp138.cut.txt <==
1   610467  610468  rs56138368
1   610515  610516  rs2259064
1   610638  610639  rs420839
"""

""" Create a dict with gwas-snp data, keyed by marker-name """

HG19FILE = '../data/snp138.cut.txt' # hg19 Reference Look-up-table (LARGE)

if len(sys.argv) < 2:
    print "Usage %s <inputfile> " % sys.argv[0]
    sys.exit(-1)
else:
    infile = sys.argv[1]
    outfile = infile + ".hg19.txt"

with open(infile, 'rb') as inpf:
    reader = csv.reader(inpf, delimiter='\t')
    snps = {row[0]: row for row in reader}

""" For each SNP, attach coordinate information from SNP138/HG19 file """
with open(HG19FILE, 'rb') as hg19f:
    with open(outfile, 'wd') as outf:
        reader = csv.reader(hg19f, delimiter='\t')
        writer = csv.writer(outf, delimiter='\t')
        line = 0
        for row in reader:
            key =  row[3] # marker-name 
            #print line, key
            line += 1
            if key not in snps:
                # no data for this row
                continue
            newrow = snps[key] + row
            print newrow
            writer.writerow(newrow)

