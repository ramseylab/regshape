rSNP <- read.delim("SNAP.Regulome1x.refGeneIntergenic.csv", sep=',')
# snap <- read.delim("SNAPResults.rsq09.txt")

loci <- read.delim("eQTL-rSNP-loci.csv", sep=',')
# gwas <- read.table("gwas_eligible.tsv.hg19.txt", sep="\t", header=F)
# names(gwas) <- c("SNP_ID", "reference_allele", "other_allele", "eaf", "Fisher_Pvalue", "Chromosome", "Start", "Stop", "MarkerName")

gwas <- read.delim("gwas_ucsc_genic.csv", sep=',')
gwas <- subset(gwas, Intergenic)

search_window = 500000 # bp (width-of-a-region/2 i.e. SNAP-search-distance)

loci$maxGwasSnpName <- NA
loci$maxGwasSnpPVal <- NA
loci$maxGwasSnpEaf <- NA
loci$maxGwasSnpLogOdds <- NA
loci$eQTLGenes <- NA

#find "strongest marker variant" for each loci
for (i in 1:nrow(loci)) {
	chrom = substring(as.character(loci$locus[i]), 4) 
	loc = loci$location[i]
	rSNPs = strsplit(as.character(loci$rSNPs[i]), ',')

	#print(paste(chrom, loc, rSNPs))
	# find all markers within region
	gwas_region <- subset(gwas, 
		(Start > loc - search_window) & (Stop < loc + search_window) & (Chromosome == chrom))
	# print(nrow(gwas_region))

	gwas_region <- gwas_region[with(gwas_region, order(Fisher_Pvalue)), ]
	loci$maxGwasSnpName[i] <- as.character(gwas_region[1, "SNP_ID"])
	loci$maxGwasSnpPVal[i] <- gwas_region[1, "Fisher_Pvalue"]
	loci$maxGwasSnpEaf[i] <- gwas_region[1, "eaf"] 
	loci$maxGwasSnpLogOdds[i] <- gwas_region[1, "log_odds"] 

	eG <- paste(unique(strsplit(as.character( rSNP[as.character(rSNP$chrom) == as.character(loci$locus[i]), "eQTLs"] ),',')), sep=',')
	loci$eQTLGenes[i] <- eG 
	print( eG ) 
}

write.table(loci, file="loci.txt", sep="\t", row.names=F, col.names=T)


