

-- DB: hgmd_pro

-- Get all regulatory SNPs
SELECT * FROM allmut WHERE base IN ('R') -- this has NULL dbSNP-id SNPs

SELECT * FROM allmut WHERE base IN ('R') AND dbsnp IS NOT NULL


SELECT a.*, h.chromosome, h.coordSTART, h.coordEND 
  FROM allmut a, hg19_coords h 
  WHERE 
    a.base IN ('R') 
    AND a.dbsnp IS NULL
    AND h.acc_num = a.acc_num

-- loaded hg19 > snp142 table per this page: http://hgdownload.cse.ucsc.edu/goldenpath/hg19/database/
cat snp142.txt | mysql hg19 -uroot --local-infile=1 -e 'LOAD DATA LOCAL INFILE "/dev/stdin" INTO TABLE snp142;'


--- get hg19 coordinates for all rSNPs with no dbSNP ID
SELECT a.acc_num, h.chromosome, CONCAT('chr', h.chromosome) as chrom, h.coordSTART, h.coordEND 
  FROM allmut a, hg19_coords h 
  WHERE 
    a.base IN ('R') 
    AND a.dbsnp IS NULL
    AND h.acc_num = a.acc_num 

--- get rsXXX dbSNP names for all allmut-SNPs whose dbSNP column is null
SELECT 
    s.name, s.chrom, r.acc_num, r.chromosome, r.coordSTART, r.coordEND 
  FROM 
	hg19.snp142 s 
   JOIN
    ( SELECT a.acc_num, h.chromosome, CONCAT('chr', h.chromosome) as chrom, h.coordSTART, h.coordEND 
	  FROM allmut a, hg19_coords h 
	  WHERE 
	    a.base IN ('R') 
	    AND a.dbsnp IS NULL
	    AND h.acc_num = a.acc_num ) r
  WHERE 
  r.chrom = s.chrom
  AND r.coordSTART = s.chromStart
  AND r.coordEND = s.chromEND

-- rs368942497	chr17	CR1110629	17	80797852	80797852




SELECT base, COUNT(1) FROM allmut GROUP BY 1 ORDER BY 2 DESC
/*
M	94860
D	25454
S	15476
G	12833
I	10617
R	3242  <------ regulatory SNPs
N	3086
X	2436
P	1638
E	476
*/

SELECT tag, COUNT(1) FROM allmut GROUP BY 1 ORDER BY 2 DESC
/* 
DM	147728
DM?	13779
FP	3595
DP	2993
DFP	1947
R	76  -- retired rows !(regulatory)
*/



SELECT reftag, COUNT(1) FROM allmut GROUP BY 1 ORDER BY 2 DESC
/*
PRI	168002
LSD	2116
*/

SELECT tag, COUNT(1) FROM mutation GROUP BY 1 ORDER BY 2 DESC
/* Same as allmut */

-- DB: hgmd_snp
SELECT tag, pmid, COUNT(1) FROM mutsnp GROUP BY 1,2 ORDER BY 3 DESC
/* DBS	DBS	49128 */


## Look for SNP144 info where HGMD.allmut dbSNP column is null
SELECT 
    s.snp, s.chrom, r.acc_num, r.chromosome, r.coordSTART, r.coordEND 
  FROM 
  hg19.dbSNP144 s 
   JOIN
    ( SELECT a.acc_num, h.chromosome, h.coordSTART, h.coordEND 
    FROM hgmd_pro.allmut a, hgmd_pro.hg19_coords h 
    WHERE 
      a.base IN ('R') 
      AND a.dbsnp IS NULL
      AND h.acc_num = a.acc_num ) r
  WHERE 
  r.chromosome = s.chrom
  AND r.coordSTART = s.startCoord
  AND r.coordEND = s.endCoord
  AND s.withdrawn = 0

DEBUG!!
SELECT 
    s.snp, s.chrom, r.acc_num, r.chromosome, r.coordSTART, r.coordEND 
  FROM 
  hg19.dbSNP144 s 
   JOIN
    ( SELECT a.acc_num, a.dbsnp, h.chromosome, h.coordSTART, h.coordEND 
    FROM hgmd_pro.allmut a, hgmd_pro.hg19_coords h 
    WHERE 
      a.base IN ('R') 
      AND h.acc_num = a.acc_num ) r
  WHERE 
  r.chromosome = s.chrom COLLATE utf8_unicode_ci
  AND r.coordSTART = s.startCoord 
  AND r.coordEND = s.endCoord 
  AND s.withdrawn = 0
  
        --AND a.dbsnp IS NULL

CM057468  rs6508  11  32460464  32460464
CR000229  rs1800775 16  56995236  56995236
CR000230  rs705379  7 94953895  94953895
CR000231  rs854571  7 94954619  94954619

SELECT * FROM  hg19.dbSNP144 WHERE snp IN ('rs6508', 'rs1800775', 'rs705379', 'rs854571')

-- How many cancer-related rSNPs?
SELECT * FROM hgmd_pro.allmut 
  WHERE 
    base IN ('R') 
    AND disease LIKE '%cancer%' -- ~280
    AND dbSNP NOT NULL -- ~255

