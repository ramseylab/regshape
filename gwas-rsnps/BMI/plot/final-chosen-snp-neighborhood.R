options(width=120)

#### (1) For the chosen SNP, get metadata for final presentation
# rs7929725
# - GWAS variants near this one, p-values
# - for each variant that neighbors it, 50 in both directions
# - eQTL p-value for this gene: regulomeDB --  
# - Columns: SNP, chrom, coords, GWAS LpVal, eQTL LpVal [GTEC data], MAF

# Extract data for neigbors of chosen
system("sed 's/ /       /g' GIANT_BMI_Speliotes2010_publicrelease_HapMapCeuFreq.txt > GIANT_BMI.tsv"
system("python add_hg19_coordinates.py GIANT_BMI.tsv") # --> [].hg19.txt


GWAS <- read.table("GIANT_BMI.tsv.hg19.txt", header=F, sep='\t')
nrow(GWAS) # 2528291
head(GWAS) 
names(GWAS) <- c("MarkerName", "Allele1", "Allele2", "FreqAllele1", "p", "N", "Chromosome", "Start", "Stop", "SNP")

GWAS <- GWAS[ with(GWAS, order(Chromosome,Start)), ]
row.names(GWAS) <- seq(1 : nrow(GWAS))

chosen_snp <- "rs7929725"
chosen_snp_row <- row.names( subset(GWAS, MarkerName == chosen_snp) )
chosen_snp_row <- as.numeric(as.character(chosen_snp_row))

neighborhood <- GWAS[ seq(chosen_snp_row - 400, chosen_snp_row + 400), ]

# Columns: SNP, chrom, coords, GWAS LpVal, eQTL LpVal [GTEC data], MAF
neighborhood$negLogPValue <- -log(neighborhood$p)
neighborhood$FreqAllele1 <- as.numeric(as.character(neighborhood$FreqAllele1))
neighborhood$MAF <- ifelse(neighborhood$FreqAllele1 > 0.5, 1 - neighborhood$FreqAllele1, neighborhood$FreqAllele1)


# Add SNAP LinkageDisequilibirum informaiton where available
SNAP <- read.delim("rs7929725-SNAPResults.txt")
nrow(SNAP) # 2814
head(SNAP) 
SNAP <- subset(SNAP, select=c("Proxy", "Distance", "RSquared"))

neighborhood <- merge(neighborhood, SNAP, by.x="SNP", by.y="Proxy", all.x=T)
neighborhood <- neighborhood[ with(neighborhood, order(Chromosome,Start)), ]

nrow(neighborhood)


# Output
write.table(neighborhood, "rs7929725.neighborhood.txt", sep='\t', row.names=F, col.names=T)







