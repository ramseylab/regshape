
SELECT 
  x.chrom as locus, 
  AVG(x.chromStart) as location, 
  GROUP_CONCAT(x.name) as SNPs
 FROM (
  SELECT 
   	s.name, s.chrom, s.chromStart, s.chromEnd, g.name as gName
  FROM 
   	snp142 s LEFT JOIN refGene g
   	ON 
  	    g.txStart - 5000 <= s.chromStart
      AND g.txEnd + 5000 >= s.chromEnd
      AND g.chrom = s.chrom
  WHERE s.name IN (
	'rs1009',
	'rs10187424',
	'rs1033799',
	'rs11066089',
	'rs11167260',
	'rs12321677',
	'rs12423041',
	'rs12423126',
	'rs12679834',
	'rs1544396',
	'rs1569209',
	'rs16941759',
	'rs17092215',
	'rs17092456',
	'rs17410962',
	'rs17411031',
	'rs17411045',
	'rs17482753',
	'rs17489268',
	'rs17489282',
	'rs1837842',
	'rs1919484',
	'rs1981517',
	'rs2097701',
	'rs2158030',
	'rs2886722',
	'rs3213628',
	'rs326',
	'rs327',
	'rs3741998',
	'rs3742000',
	'rs4767068',
	'rs6006426',
	'rs6060244',
	'rs6060246',
	'rs6120843',
	'rs634389',
	'rs640783',
	'rs6757263',
	'rs7261167',
	'rs7273734',
	'rs7296651',
	'rs7402386',
	'rs8117847',
	'rs847898',
	'rs867186',
	'rs9971746'
  )
  HAVING g.name IS NULL
 ) x GROUP BY chrom
 ORDER BY locus
