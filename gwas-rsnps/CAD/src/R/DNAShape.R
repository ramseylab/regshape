
if (FALSE) { # R Multi-line comment hack! 

	GWAS SNPs:
		 rs7929725	
		 rs6485795	
	Proxy SNPs:
		 rs7111606	
		 rs11039377 

	grep -i "rs7929725\|rs6485795\|rs7111606\|rs11039377" snp138.cut.txt
	UCSC Data ----------------------------| Ensembl URL ----> 
	11	47881739	47881740	rs7929725	http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47881740-47881740 
	11	47929845	47929846	rs6485795	http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47929846-47929846

	11	47787537	47787538	rs7111606	http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47787538-47787538
	11	47789775	47789776	rs11039377	http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47789776-47789776

	UCSC --> ENSEMBL convert, add 1 to start
	Window of 10 on either side, so -9, +10
	11	47881730	47881750	rs7929725		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47881730-47881750	TODO
	11	47929836	47929856	rs6485795		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47929836-47929856	TODO

	11	47787528	47787548	rs7111606 		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47787528-47787548	TODO
	11	47789766	47789786	rs11039377		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47789766-47789786	TODO

	Window of 20 on either side, so -19, +20
	11	47881720	47881760	rs7929725		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47881720-47881760	TATCCTAAGGAAACAATCACAGATATGCTTCAAGATACAAG AATGGTCTTT
	11	47929826	47929866	rs6485795		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47929826-47929866	GCCCATTTGTCTGTCTATTTGGAGGCAGCAGGAAATAGTGG

	11	47787518	47787558	rs7111606 		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47787518-47787558	ATGAGTCACCGTGCCCTGGAGTCTTTCATCTGCTTACAAAC
	11	47789756	47789796	rs11039377		http://grch37.ensembl.org/Homo_sapiens/Location/View?r=11%3A47789756-47789796	CTGGGAAAAATCACGAGATACTCCACTACCTACAAGGAAAA
}


# library(devtools)
# install_github("ramseylab/regshape")
library(regshape)
options(width=512)

snps <- c("rs7929725", "rs6485795", "rs7111606", "rs11039377")
seqs <- c("TATCCTAAGGAAACAATCACAGATATGCTTCAAGATACAAG", "GCCCATTTGTCTGTCTATTTGGAGGCAGCAGGAAATAGTGG", "ATGAGTCACCGTGCCCTGGAGTCTTTCATCTGCTTACAAAC", "CTGGGAAAAATCACGAGATACTCCACTACCTACAAGGAAAA")
windowLengths <- c(9, 11, 13, 15) 

for (i in 1:length(windowLengths)) {
	wl <- windowLengths[i]
	print(wl)
	for (j in 1:length(seqs)) {
		shapeScoreVector <- getShapeScoresSlidingWindow(seqs[j], wl)
		print(shapeScoreVector)	
	}
}


#### Compare Major/Minor Allele Shape scores ####


# GIANT: Get alternative allele from GWAS-GIANT file
	# grep -i "rs7929725\|rs6485795\|rs7111606\|rs11039377" GIANT_BMI_Speliotes2010_publicrelease_HapMapCeuFreq.txt
	# rs7929725 a g 0.5583 3.62e-10 123863
	# rs6485795 g a 0.6917 1.45e-08 123861
	# [?]
	# rs11039377 c t 0.5583 3.41e-10 123864

# UCSC: Get alternative allele from SNP138 ---> Unclear how to extract
 # http://ucscbrowser.genap.ca/cgi-bin/hgTables?hgsid=1125556_giFKa2mywDespKTKxhPYOp04jKQA&hgta_doSchemaDb=hg19&hgta_doSchemaTable=snp138
 # grep -i "rs7929725\|rs6485795\|rs7111606\|rs11039377" snp138.txt | cut -f2-5,8-10,12
	# chr11	47881739	47881740	rs7929725	A	A	A/G	single
	# chr11	47929845	47929846	rs6485795	G	G	A/G	single
	# chr11	47787537	47787538	rs7111606	G	G	A/G	single
	# chr11	47789775	47789776	rs11039377	C	C	C/T	single

# dbSNP
# 	http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs=rs7929725
# 	 A/G G-minor
# 	http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs=rs6485795
# 	 A/G A-minor
# 	http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs=rs7111606
# 	 A/G A-minor
# 	http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs=rs11039377
# 	 C/T T-minor

# Substitute to alternate allele and see if scores differ
 # nchar(substr(seqs[1], 1, 20)) # 20
 # nchar(substr(seqs[1], 22, 41)) # 20

substr(seqs[1], 21, 21) # "A"
substr(seqs[2], 21, 21) # "G"
substr(seqs[3], 21, 21) # "G"
substr(seqs[4], 21, 21) # "C"

MASeqs <- seqs # MAJOR Allele @ SNP Location  
altSeqs <- seqs # Minor Allele @ SNP Location  
substr(altSeqs[1], 21, 21) <- "G"
substr(altSeqs[2], 21, 21) <- "A"
substr(altSeqs[3], 21, 21) <- "A"
substr(altSeqs[4], 21, 21) <- "T"

for (i in 1:length(windowLengths)) {
	wl <- windowLengths[i]
	print(wl)
	for (j in 1:length(altSeqs)) {
		shapeScoreVector <- getShapeScoresSlidingWindow(altSeqs[j], wl)
		print(shapeScoreVector)	
	}
}




# windowLengths <- c(9, 11, 13, 15) 
# for (wl in windowLengths) {
# 	print(wl)
# 	print(getShapeScoresSlidingWindow("TCACTTGAGGTCAGGAGTTTG", wl))
# 	print(getShapeScoresSlidingWindow("CACTGCACTCCAGCCCGGGTG", wl))
# 	print(getShapeScoresSlidingWindow("CAAGAGATTCTTGTGCCTAAG", wl))
# 	print(getShapeScoresSlidingWindow("GTATAGTTGACCCTTGAACAA", wl))
# }

