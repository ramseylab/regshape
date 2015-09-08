#!/bin/bash

# ../src/bash/get_TSS_metadata_from_UCSC.sh csnps.rslist.txt csnps.tss_metadata.txt
# ../src/bash/get_TSS_metadata_from_UCSC.sh rsnps.rslist.txt rsnps.tss_metadata.txt

# Process Cmd Line Args
if [ $# -lt 2 ]    
then
  echo "Usage: $0 snp_list_file.txt output_file.txt"
  exit 1
fi  
INPUT_FILE=$1
OUTPUT_FILE=$2

SNPS=$(cat ${INPUT_FILE} | python -c "import sys;print '\',\''.join([line.replace('\n', '') for line in sys.stdin]),")

mysql --user=genome --host=genome-mysql.cse.ucsc.edu -A -e "\
SET sql_mode = 'NO_UNSIGNED_SUBTRACTION';
SELECT 
    s.name, s.chrom, s.chromStart, s.chromEnd, 
    g.name as tssGene, g.txStart, g.txEnd, g.strand,  
    CASE g.strand
      WHEN '+' THEN g.txStart - s.chromStart
      WHEN '-' THEN g.txEnd - s.chromStart
    END as tssDistance   
  FROM 
    snp142 s
  LEFT OUTER JOIN
    ensGene g
  ON
        g.bin = s.bin # Speeds up JOINs
    AND g.chrom = s.chrom
  WHERE 
  s.name IN  ( '${SNPS}' )
 ORDER BY 
  name, abs(tssDistance) " hg19 > ${OUTPUT_FILE}

python ../src/python/getClosestTSS.py ${OUTPUT_FILE} # transforms file in-place
