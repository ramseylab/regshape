For dbSNP144, we use the "Chromosome Report" files from the following FTP location 

# Download FTP directory to local: 
  ftp://ftp.ncbi.nih.gov/snp//organisms/human_9606_b144_GRCh37p13/chr_rpts

# Preprocessing for loading into MySQL:
 gzip -d *.gz
 rm chr_AltOnly.txt chr_MT.txt chr_Multi.txt chr_NotOn.txt chr_PAR.txt chr_Un.txt
 sed -e '1,7d' -e 's/^/rs/' chr_*.txt | cut -f1,3,7,12-13,26 > dbsnp144.chr_rpts.txt

 sed -e '1,7d' -e 's/^/rs/' -e 's/:/\t/g' chr_*.txt | cut -f1,3,7,12-13,26-28 > dbsnp144.chr_rpts.2.txt

# Load into local MySQL
mysql hg19 -uroot -e 'CREATE TABLE dbSNP144 (
	snp VARCHAR(64) NOT NULL, 
	withdrawn BOOL,
	chrom CHAR(2),
	coordNCBI INT UNSIGNED,
	genes VARCHAR(256) DEFAULT NULL,
	minorAllele CHAR(1) DEFAULT NULL,
	minorAlleleCount INT UNSIGNED,
	minorAlleleFreq FLOAT );'

mysql hg19 -uroot --local-infile=1 -e 'LOAD DATA LOCAL INFILE "dbSNP144.chr_rpts.txt" INTO TABLE dbSNP144;'
mysql hg19 -uroot -e "SELECT COUNT(1) FROM dbSNP144" # 150,864,512

-- mysql hg19 -uroot --local-infile=1 -e 'LOAD DATA LOCAL INFILE "test.txt" INTO TABLE dbSNP144;'
-- mysql hg19 -uroot -e "SELECT * FROM dbSNP144"
-- mysql hg19 -uroot -e "DELETE FROM dbSNP144"
-- mysql hg19 -uroot -e "DROP TABLE dbSNP144"


# Excerpt of section on "Chromosome Report" format, from: ftp://ftp.ncbi.nih.gov/snp//00readme.txt

--------------------
CHROMOSOME REPORTS
--------------------

The Chromosome Reports format provides an ordered list of RefSNPs in approximate
chromosome coordinates (the same coordinate system used for the
NCBI genome MapViewer); it is a small file to download, and contains a great
deal of information about each SNP.

The lines of the Chromosome Report format give the following information
for a single RefSNP in tab-delimited columns:

Column   Data
  1      RefSNP id (rs#)
  2      mapweight where
            1 = Unmapped
            2 = Mapped to single position in genome
            3 = Mapped to 2 positions on a single chromosome
            4 = Mapped to 3-10 positions in genome (possible paralog hits)
            5 = Mapped to >10 positions in genome.  
		Please note that the number used to code mapweight for the Chromosome Report Format
		is different from the database tables.  Please see the online FAQ at:
		https://www.ncbi.nlm.nih.gov/books/bv.fcgi?mapweight&rid=helpsnpfaq.
		section.Build.The_dbSNP_Mapping_Pr#Build.Mapping_Weight
  3      snp_type where
            0 = Not withdrawn.
            1 = Withdrawn. There are several reasons for withdrawn, the
                withdrawn status is fully defined in the asn1, flatfile,
                and XML descriptions of the RefSNP. See /specs/docsum_3.0.asn
                for a full definition of snp-type values.
  4      Total number of chromosomes hit by this RefSNP during mapping
  5      Total number of contigs hit by this RefSNP during mapping
  6      Total number of hits to genome by this RefSNP during mapping
  7      Chromosome for this hit to genome
  8      Contig accession for this hit to genome
  9      Version number of contig accession for this hit to genome
 10      Contig ID for this hit to genome
 11      Position of RefSNP in contig coordinates
 12      Position of RefSNP in chromosome coordinates (used to order report)
            Locations are specified in NCBI sequence location convention where:
                   x, a single number, indicates a feature at base position x
                   x..y, denotes a feature that spans from x to y inclusive.
                   x^y, denotes a feature that is inserted between bases x and y
 13      Genes at this same position on the chromosome
 14      Average heterozygosity of this RefSNP
 15      Standard error of average heterozygosity
 16      Maximum reported probability that RefSNP is real. (For computationally-
             predicted submissions)
 17      Validated status
             0 = No validation information
             1 = Cluster has 2+ submissions, with 1+ submission assayed 
                 with a non-computational method
             2 = At least one subsnp in cluster has frequency data submitted
             3 = Non-computational method in cluster and frequency data present
             4 = At lease one subsnp in cluster has been experimentally 
                 validated by submitter
                 for other validation status values, please see:
                 <a href="ftp://ftp.ncbi.nlm.nih.gov/snp/database/organism_shared_data/SnpValidationCode.bcp.gz">ftp://ftp.ncbi.nlm.nih.gov/snp/database/organism_shared_data
                 /SnpValidationCode.bcp.gz</a>
 18      Genotypes available in dbSNP for this RefSNP
             1 = yes
             0 = no
 19      Linkout available to submitter website for further data on the RefSNP
             1 = yes
             0 = no
 20      dbSNP build ID when the refSNP was first created (i.e. the create date)
 21      dbSNP build ID of the most recent change to the refSNP cluster. The
         date of the change is represented by the build ID which has an
         approximate date/time associated with it. (see:
         https://www.ncbi.nlm.nih.gov/projects/SNP/buildhistory.cgi)
 22      Mapped to a reference or alternate (e.g. Celera) assembly 
 23 	 Suspect false SNP (https://www.ncbi.nlm.nih.gov/projects/SNP/docs/rs_attributes.html#suspect)
			 1 = yes 
			 0 = no 
 24 	 Clinical Significance: comma delimited values
			(unknown; untested; non-pathogenic; probable-non-pathogenic; probable-pathogenic; pathogenic; other)
 25 	 Allele Origin: comma delimited values (https://www.ncbi.nlm.nih.gov/projects/SNP/docs/rs_attributes.html#origin)
			(unknown; germline; somatic; inherited; paternal; maternal; de-novo; bipaternal; unipaternal; not-tested; tested-inconclusive)
 26 	 Global Minor Allele Frequency (GMAF) [ie. G:0.262:330  <-- (allele:count:frequency)] (https://www.ncbi.nlm.nih.gov/projects/SNP/docs/rs_attributes.html#gmaf)
		    "G:0.262:330". This means that for this rs, minor allele is 'G' and has a frequency of 26.2% in the 1000Genome phase 1 population and
			that 'G' is observed 330 times in the sample population of 629 people (or 1258 chromosomes). 
			
Also included within the chr_rpt file are two additional files:
              multi/ contains a list (in chromosome report format) of SNPs that
              hit multiple chromosomes in the genome

              noton/ contains a list (in chromosome report format) of SNPs that
              didn't hit any chromosomes in the genome




## For the ~800 rSNPs for which HGMD does not have DBSNP ids, get metadata from dbSNP144 ##

# the ~800 SNPs
SELECT a.acc_num, a.dbsnp, h.chromosome, h.coordSTART, h.coordEND 
    FROM hgmd_pro.allmut a, hgmd_pro.hg19_coords h 
    WHERE 
      a.base IN ('R') 
      AND a.dbsnp IS NULL 
      AND h.acc_num = a.acc_num

# Metadata from SNP144 for above (about 200 were retrieved)
SELECT 
    s.*, r.acc_num, r.chromosome, r.coordSTART, r.coordEND 
  FROM 
  hg19.dbSNP144 s 
   JOIN
    ( SELECT a.acc_num, a.dbsnp, h.chromosome, h.coordSTART, h.coordEND 
    FROM hgmd_pro.allmut a, hgmd_pro.hg19_coords h 
    WHERE 
      a.base IN ('R') 
      AND a.dbsnp IS NULL 
      AND h.acc_num = a.acc_num ) r
  WHERE 
  r.chromosome = s.chrom COLLATE utf8_unicode_ci
  AND r.coordEND = s.endCoord 
  AND s.withdrawn = 0



