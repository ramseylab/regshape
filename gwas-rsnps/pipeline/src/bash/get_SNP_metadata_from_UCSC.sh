#!/bin/bash

# ../src/bash/get_SNP_metadata_from_UCSC.sh csnps.rslist.txt csnps.ucsc_metadata.txt
# ../src/bash/get_SNP_metadata_from_UCSC.sh rsnps.rslist.txt rsnps.ucsc_metadata.txt

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
SELECT 
    s.name, s.chrom, s.chromStart, s.chromEnd, s.alleles, s.refNCBI, s.refUCSC, s.observed, s.class, s.alleleFreqs, 
    dh.name as DHS_Name, dh.score as DHS_Score, dh.sourceCount as DHS_SourceCount,
    tf.tf_names, tf.num_tfs,    
    pc.validCount, pc.sumData, pc.lowerLimit, pc.dataRange, pc.sumData/pc.validCount as phastCons    
  FROM 
    snp142 s
  LEFT OUTER JOIN
    wgEncodeAwgDnaseMasterSites dh
  ON
        dh.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN dh.chromStart AND dh.chromEnd
    AND dh.chrom = s.chrom
  LEFT OUTER JOIN
    phyloP46wayPlacental pc
  ON 
        pc.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN pc.chromStart AND pc.chromEnd
    AND pc.chrom = s.chrom    
  LEFT OUTER JOIN
  ( 
    
    SELECT 
      s.name as snp,  GROUP_CONCAT(tf.name) as tf_names, COUNT(tf.name) as num_tfs
    FROM 
      snp142 s
    LEFT OUTER JOIN
      wgEncodeRegTfbsClusteredV3 tf
    ON 
          tf.bin = s.bin # Speeds up JOINs
      AND s.chromStart BETWEEN tf.chromStart AND tf.chromEnd
      AND tf.chrom = s.chrom
    WHERE 
      s.name IN  ( '${SNPS}' )
      AND tf.name != 'POLR2A'  
    GROUP BY s.name  

  ) tf
  ON 
    tf.snp = s.name 
  WHERE 
  s.name IN  ( '${SNPS}' );	" hg19 > ${OUTPUT_FILE}
