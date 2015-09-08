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
 

# With DNAase1 Master
# http://genome.ucsc.edu/cgi-bin/hgTables?db=hg19&hgta_group=regulation&hgta_track=wgEncodeAwgDnaseMasterSites&hgta_table=wgEncodeAwgDnaseMasterSites&hgta_doSchema=describe+table+schema
SELECT 
   	s.name, s.chrom, s.chromStart, s.chromStart, 
   	dh.name, dh.chrom, dh.chromStart, dh.chromStart
  FROM 
   	snp142 s
  LEFT JOIN
   	wgEncodeAwgDnaseMasterSites dh
  ON
        dh.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN dh.chromStart AND dh.chromEnd
    AND dh.chrom = s.chrom
  WHERE 
	s.name IN  ('rs376643643', # Intergenic 
				'rs713586', # Intergenic in Hypsensitive Site
				'rs2422136') # Genic

 
# + TF Binding Sites
# http://genome.ucsc.edu/cgi-bin/hgTables?db=hg19&hgta_group=regulation&hgta_track=wgEncodeRegTfbsClusteredV3&hgta_table=wgEncodeRegTfbsClusteredV3&hgta_doSchema=describe+table+schema
SELECT 
   	s.name, s.chrom, s.chromStart, s.chromStart, 
   	dh.name, dh.chrom, dh.chromStart, dh.chromEnd, dh.score, dh.sourceCount,
   	tf.chrom, tf.chromStart, tf.chromEnd, tf.name, tf.score   	
  FROM 
   	snp142 s
  LEFT OUTER JOIN
   	wgEncodeAwgDnaseMasterSites dh
  ON
        dh.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN dh.chromStart AND dh.chromEnd
    AND dh.chrom = s.chrom
  LEFT OUTER JOIN
    wgEncodeRegTfbsClusteredV3 tf
  ON 
        tf.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN tf.chromStart AND tf.chromEnd
    AND tf.chrom = s.chrom    
  WHERE 
	s.name IN  (
	-- 'rs376643643', # Intergenic 
	-- 'rs370180536', # Intergenic + Hypsensitive Site
	-- 'rs79420348', # Intergenic + Hypsensitive Site + TF				
	-- 'rs2422136') # Genic


# + Placental Mammal Conservation Data
# http://genome.ucsc.edu/cgi-bin/hgTables?db=hg19&hgta_group=compGeno&hgta_track=cons46way&hgta_table=phyloP46wayPlacental&hgta_doSchema=describe+table+schema
SELECT 
   	s.name, s.chrom, s.chromStart, s.chromStart, 
   	dh.name, dh.chrom, dh.chromStart, dh.chromEnd, dh.score, dh.sourceCount,
   	tf.chrom, tf.chromStart, tf.chromEnd, tf.name, tf.score,   	
   	pc.chrom, pc.chromStart, pc.chromEnd, pc.validCount, pc.sumData, pc.lowerLimit, pc.dataRange   	
  FROM 
   	snp142 s
  LEFT OUTER JOIN
   	wgEncodeAwgDnaseMasterSites dh
  ON
        dh.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN dh.chromStart AND dh.chromEnd
    AND dh.chrom = s.chrom
  LEFT OUTER JOIN
    wgEncodeRegTfbsClusteredV3 tf
  ON 
        tf.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN tf.chromStart AND tf.chromEnd
    AND tf.chrom = s.chrom    
  LEFT OUTER JOIN
    phyloP46wayPlacental pc
  ON 
        pc.bin = s.bin # Speeds up JOINs
    AND s.chromStart BETWEEN pc.chromStart AND pc.chromEnd
    AND pc.chrom = s.chrom    
  WHERE 
	s.name IN  (
		-- 'rs376643643', # Intergenic 
		-- 'rs370180536', # Intergenic + Hypsensitive Site
		-- 'rs79420348', # Intergenic + Hypsensitive Site + TF				
		-- 'rs2422136') # Genic

	'rs4415546',
	'rs10899973',
	'rs11238817',
	'rs9332446',
	'rs11238818',
	'rs955584',
	'rs10899971',
	'rs12570314',
	'rs10899970',
	'rs1352999',
	'rs2624695',
	'rs2128363',
	'rs2624694',
	'rs2804029',
	'rs2047009',
	'rs16911227',
	'rs75809145',
	'rs16911231',
	'rs16911234',
	'rs79983988',
	'rs16911239',
	'rs76954857',
	'rs79146953',
	'rs74666499',
	'rs79186796',
	'rs74731734',
	'rs78972209',
	'rs79661893',
	'rs78423326',
	'rs60191114',
	'rs77521436',
	'rs79063450',
	'rs112096776',
	'rs75679197',
	'rs10491208',
	'rs10509079',
	'rs76554834',
	'rs78453336',
	'rs76784064',
	'rs75646233',
	'rs75104613',
	'rs79035860',
	'rs117259361',
	'rs76816775',
	'rs80242041',
	'rs74232486',
	'rs78096851',
	'rs116009975',
	'rs77667291',
	'rs80143739',
	'rs76240665',
	'rs79220738',
	'rs75905559',
	'rs7127880',
	'rs7105672',
	'rs7350481',
	'rs9326246',
	'rs1558861',
	'rs1558860',
	'rs11065987',
	'rs12828640',
	'rs2028005',
	'rs7957741',
	'rs7957842',
	'rs4766517',
	'rs7316015',
	'rs6489834',
	'rs756823',
	'rs886125',
	'rs7953254',
	'rs741334',
	'rs11067009',
	'rs11067008',
	'rs73390955',
	'rs79898725',
	'rs74888628',
	'rs11067011',
	'rs7980629',
	'rs74319386',
	'rs9603710',
	'rs7319344',
	'rs7335734',
	'rs2324535',
	'rs4482148',
	'rs6573987',
	'rs61133561',
	'rs10138249',
	'rs73284477',
	'rs59752154',
	'rs8018977',
	'rs1892225',
	'rs10139819',
	'rs967545',
	'rs8024939',
	'rs59934757',
	'rs7177143',
	'rs2869867',
	'rs7176492',
	'rs3743068',
	'rs12906835',
	'rs2622821',
	'rs11072800',
	'rs7182567',
	'rs7182694',
	'rs7182993',
	'rs11632672',
	'rs7495616',
	'rs12905116',
	'rs11871738',
	'rs12939297',
	'rs62066210',
	'rs12942708',
	'rs12951347',
	'rs35660942',
	'rs11650499',
	'rs12945496',
	'rs12947517',
	'rs12943416',
	'rs9911672',
	'rs12936587',
	'rs9913096',
	'rs12449964',
	'rs12602348',
	'rs9900673',
	'rs113347218',
	'rs111966500',
	'rs7212538',
	'rs8068296',
	'rs11655881',
	'rs7223430',
	'rs7222423',
	'rs8073683',
	'rs8073975',
	'rs1869771',
	'rs181247',
	'rs181253',
	'rs7578326',
	'rs13388242',
	'rs13405357',
	'rs11680332',
	'rs6758440',
	'rs6068963',
	'rs2682539',
	'rs664399',
	'rs588237',
	'rs681798',
	'rs600054',
	'rs6807945',
	'rs13095258',
	'rs13096479',
	'rs35623343',
	'rs4408839',
	'rs4679727',
	'rs4386452',
	'rs12491701',
	'rs13077097',
	'rs56615515',
	'rs7628771',
	'rs16846891',
	'rs59097659',
	'rs13073025',
	'rs7635034',
	'rs7635035',
	'rs7635064',
	'rs7634965',
	'rs11925809',
	'rs7649849',
	'rs4680117',
	'rs7616575',
	'rs7639011',
	'rs7641288',
	'rs6809873',
	'rs4515014',
	'rs35288962',
	'rs34774506',
	'rs36117336',
	'rs1357081',
	'rs4387932',
	'rs77298328',
	'rs34103896',
	'rs12486823',
	'rs1521333',
	'rs4679729',
	'rs34654591',
	'rs35690365',
	'rs11914514',
	'rs7617509',
	'rs73208098',
	'rs111354897',
	'rs41300373',
	'rs116082477',
	'rs1878406',
	'rs73855814',
	'rs72957606',
	'rs10305838',
	'rs6842241',
	'rs6841581',
	'rs10305839',
	'rs9307934',
	'rs12507760',
	'rs2221181',
	'rs17476297',
	'rs2104332',
	'rs12193946',
	'rs6905288',
	'rs13208076',
	'rs9351814',
	'rs9351816',
	'rs12190423',
	'rs9351817',
	'rs6453543',
	'rs504691',
	'rs182106',
	'rs591809',
	'rs199624',
	'rs199623',
	'rs476903',
	'rs199647',
	'rs199621',
	'rs199622',
	'rs199630',
	'rs199632',
	'rs199629',
	'rs169210',
	'rs514244',
	'rs565651',
	'rs512554',
	'rs478392',
	'rs199641',
	'rs4707922',
	'rs1486596',
	'rs16893523',
	'rs11969940',
	'rs56021267',
	'rs11961442',
	'rs16893518',
	'rs16893524',
	'rs6454240',
	'rs13214556',
	'rs16893491',
	'rs3125055',
	'rs3125054',
	'rs3125053',
	'rs3106167',
	'rs3127591',
	'rs3125052',
	'rs3125051',
	'rs3120140',
	'rs3103349',
	'rs3125050',
	'rs3120139',
	'rs3125057',
	'rs3106168',
	'rs3120142',
	'rs3127597',
	'rs3127598',
	'rs3106169',
	'rs3106170',
	'rs76725059',
	'rs3103348',
	'rs3106173',
	'rs3106174',
	'rs3120146',
	'rs3103347',
	'rs3106175',
	'rs3106171',
	'rs3106172',
	'rs3120145',
	'rs3127583',
	'rs2315065',
	'rs4921914',
	'rs4921915',
	'rs4921913',
	'rs35246381',
	'rs35570672',
	'rs1495741',
	'rs1495743',
	'rs1581675',
	'rs2103325',
	'rs34818360',
	'rs34932218',
	'rs17411168',
	'rs17489373',
	'rs17411133',
	'rs17411126',
	'rs34564316',
	'rs2410623',
	'rs2410625',
	'rs2410621',
	'rs2410620',
	'rs4320561',
	'rs2410619',
	'rs4628269',
	'rs4425772',
	'rs4644277',
	'rs920589',
	'rs920588',
	'rs2119689',
	'rs1822200',
	'rs4922117',
	'rs17489282',
	'rs1441763',
	'rs17411045',
	'rs1441762',
	'rs17411031',
	'rs17489268',
	'rs2083637',
	'rs894211',
	'rs765547',
	'rs1441758',
	'rs9644637',
	'rs9644638',
	'rs34327087',
	'rs34345068',
	'rs2009493',
	'rs1837843',
	'rs1837842',
	'rs765548',
	'rs2165556',
	'rs3844510',
	'rs1992443',
	'rs2410618',
	'rs80236974',
	'rs2083636',
	'rs2119690',
	'rs1992442',
	'rs1441760',
	'rs35495249',
	'rs1441757',
	'rs1441756',
	'rs1372344',
	'rs4523270',
	'rs2165558',
	'rs11986942',
	'rs6586891',
	'rs4637851',
	'rs7002680',
	'rs11993784',
	'rs12544096',
	'rs10738610',
	'rs1333046',
	'rs7857118',
	'rs1333048',
	'rs10733376',
	'rs10757277',
	'rs10811656',
	'rs10757278',
	'rs10757279',
	'rs1333049',
	'rs1537374',
	'rs2383207',
	'rs1004638',
	'rs944797',
	'rs2383206',
	'rs10738609',
	'rs1537376',
	'rs1537375',
	'rs1333047',
	'rs4977575',
	'rs651007',
	'rs579459',
	'rs649129',
	'rs495828',
	'rs635634',
	'rs600038',
	'rs532436',
	'rs115478735',
	'rs507666'
)





