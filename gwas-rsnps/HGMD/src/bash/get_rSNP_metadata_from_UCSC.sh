#cd ${PROJECT_HOME}/workspace

#RSNPS=$(cat rsnps.rslist.txt | sed "s/\n/\'\,\'/g" | head)
RSNPS=$(cat rsnps.rslist.txt | python -c "import sys;print '\',\''.join([line.replace('\n', '') for line in sys.stdin]),")
#echo \'$RSNPS\'


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
      s.name IN  ( '$RSNPS' )
      AND tf.name != 'POLR2A'  
    GROUP BY s.name  

  ) tf
  ON 
    tf.snp = s.name 
  WHERE 
  s.name IN  ( '$RSNPS' );	
" hg19 > rsnps.ucsc_metadata.txt
