# SNP Metadata
# http://ucscbrowser.genap.ca/cgi-bin/hgTables?hgsid=1186698_UHMlYABco6ZL3qYUtaaB41BV859c&hgta_doSchemaDb=hg19&hgta_doSchemaTable=snp142
SELECT 
   chrom, REPLACE(chrom, 'chr', '') AS chromNum, chromStart, name 
  FROM snp142 
  WHERE 
    name IN ('rs376643643') # Intergenic


# With UCSC Genes 
# http://genome.ucsc.edu/cgi-bin/hgTables?db=hg19&hgta_group=genes&hgta_track=knownGene&hgta_table=knownGene&hgta_doSchema=describe+table+schema
SELECT 
   	s.name, s.chrom, s.chromStart, s.chromEnd, 
   	g.name, g.chrom, g.txStart, g.txEnd
  FROM 
   	snp142 s,
   	knownGene g
  WHERE 
    (
      s.name IN ('rs376643643') # Intergenic 
      OR s.name IN ('rs2422136') # Genic
    ) 
    AND g.txStart <= s.chromStart
    AND g.txEnd >= s.chromEnd
    AND g.chrom = s.chrom
 

# With RefGene 
# http://ccp.arl.arizona.edu/dthompso/sql_workshop/mysql/ucscdatabase.html
SELECT 
   	s.name, s.chrom, s.chromStart, s.chromEnd, 
   	g.name, g.chrom, g.txStart, g.txEnd
  FROM 
   	snp142 s,
   	refGene g
  WHERE 
    (
      s.name IN ('rs376643643') # Intergenic 
      OR s.name IN ('rs2422136') # Genic
    ) 
    AND g.txStart <= s.chromStart
    AND g.txEnd >= s.chromEnd
    AND g.chrom = s.chrom
 

