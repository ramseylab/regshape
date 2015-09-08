#cd ${PROJECT_HOME}/workspace

RSNPS=$(cat rsnps.rslist.txt | python -c "import sys;print '\',\''.join([line.replace('\n', '') for line in sys.stdin]),")

mysql --user=genome --host=genome-mysql.cse.ucsc.edu -A -e "
SET sql_mode = 'NO_UNSIGNED_SUBTRACTION';
SELECT 
	s.name as cSNPName, s.chrom as cSNPChrom, s.chromStart as cSNPChromStart, s.chromEnd as cSNPChromEnd,
	r.name as rSNPName, r.chrom as rSNPChrom, r.chromStart as rSNPChromStart, r.chromEnd as rSNPChromEnd,
	(r.chromEnd - s.chromEnd) as deltaEnd, (r.chromStart - s.chromStart) as deltaStart 	
  FROM 
    snp142 s LEFT OUTER JOIN snp142 r
   ON
    s.chromStart > r.chromStart - 1000
    AND s.chromEnd < r.chromEnd + 1000
    AND s.chrom = r.chrom
    AND s.bin = r.bin # speedup
    AND s.name != r.name    
  WHERE 
    r.name IN  ( '$RSNPS' )
  ORDER BY 
  	r.name, abs(deltaEnd)
    " hg19 > ucsc_csnps.txt

python ../src/python/subsetTopNrSNPs.py # creates csnps.rslist.txt

#cat ucsc_csnps.txt > ucsc_csnps.trimmed.txt

#cut -f15 hgmd_rsnps.txt | grep 'rs' > rsnps.rslist.txt


### Super-Unreliable query MySQL query to get top N cSNPs-per-rSNP ###

# mysql --user=genome --host=genome-mysql.cse.ucsc.edu -A -e "
# SET sql_mode = 'NO_UNSIGNED_SUBTRACTION'; # http://dev.mysql.com/doc/refman/5.6/en/sql-mode.html#sqlmode_no_unsigned_subtraction
# SET @snp = NULL; # http://www.xaprb.com/blog/2006/12/02/how-to-number-rows-in-mysql/
# SET @rnk = 1;

# SELECT sub.*, 
# 	@rnk := IF(@snp = sub.rSNPName, @rnk + 1, 1) as rank,
# 	@snp := sub.rSNPName as dummy
# 	FROM (

# SELECT 
# 	s.name as cSNPName, s.chrom as cSNPChrom, s.chromStart as cSNPChromStart, s.chromEnd as cSNPChromEnd,
# 	r.name as rSNPName, r.chrom as rSNPChrom, r.chromStart as rSNPChromStart, r.chromEnd as rSNPChromEnd,
# 	(r.chromEnd - s.chromEnd) as deltaEnd, (r.chromStart - s.chromStart) as deltaStart
#   FROM 
#     snp142 s LEFT OUTER JOIN snp142 r
#    ON
#     s.chromStart > r.chromStart - 1000
#     AND s.chromEnd < r.chromEnd + 1000
#     AND s.chrom = r.chrom
#     AND s.bin = r.bin # speedup
#     AND s.name != r.name    
#   WHERE 
#     r.name IN  ( 'rs2274263', 'rs3737717' )
#   ORDER BY 
#   	r.name, abs(deltaEnd)
# ) sub  	
# HAVING rank <= 10
#     " hg19 > ucsc_csnps.txt

