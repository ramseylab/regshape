# setwd("~/Desktop/BMI-GWAS-SummerProject")

## Preprocess eQTL data: trim and exclude "^chr" SNPs (newer than dbSNP?)
# cut -f1,4,9 GTEx_Analysis_V4_eQTLs/Adipose_Subcutaneous.portal.eqtl | head -n 1 > Adipose_Subcutaneous.eqtl.trimmed.txt # header
# cut -f1,4,9 GTEx_Analysis_V4_eQTLs/Adipose_Subcutaneous.portal.eqtl | grep "^rs" >> Adipose_Subcutaneous.eqtl.trimmed.txt
eqtl <- read.delim("Adipose_Subcutaneous.eqtl.trimmed.txt")
head(eqtl)
summary(eqtl)

# cut -f1,4,9 GTEx_Analysis_V4_eQTLs/Whole_Blood.portal.eqtl | head -n 1 > Whole_Blood.eqtl.trimmed.txt
# cut -f1,4,9 GTEx_Analysis_V4_eQTLs/Whole_Blood.portal.eqtl | grep "^rs" >> Whole_Blood.eqtl.trimmed.txt
eqtl <- read.delim("Whole_Blood.eqtl.trimmed.txt")
head(eqtl)
summary(eqtl)



# Subset/Aggregate to Gene with lowest p-value (highest confidence) per SNP 
heqtl <- eqtl[ order(eqtl$Gen_ID, eqtl$P_Val), ]
heqtl <- aggregate(heqtl, list(heqtl$Gen_ID), FUN=head, n=1)
heqtl <- subset(heqtl, select=c("SNP", "Gen_ID", "P_Val"))

# Is there any SNP-level overlap
# eqtl_snps <- as.character(heqtl$SNP)
# gwas_intergenic_snps <- as.character(gwas_ucsc[gwas_ucsc$Intergenic, "MarkerName"])
# intersect(eqtl_snps, gwas_intergenic_snps) # none 

# Get hg19 coordinates for aggregated eqtl data
write.table(heqtl, file="heqtl.tsv", sep="\t", row.names=F, col.names=F, quote=F)
system("python add_hg19_coordinates.py heqtl.tsv") # --> heqtl.tsv.hg19.txt

eqtl_ucsc <- read.table("heqtl.tsv.hg19.txt", sep="\t", header=F)
names(eqtl_ucsc) <- c(names(heqtl), "Chromosome", "Start", "Stop", "MarkerName")

nrow(heqtl) #1709
nrow(eqtl_ucsc) #1653


# scan for overlap in the vicinity of the GWAS-SNPs (+/- scan_width_bps)
# for each GWAS-SNP, find closest EQTL-SNP, mark if within range
gwas_ucsc <- read.csv("gwas_ucsc_genic.csv")
gwas_ucsc <- subset(gwas_ucsc, gwas_ucsc$Intergenic)
scan_width_bps <- 10000

gwas_ucsc$eqtl_snp <- NA
gwas_ucsc$eqtl_coord <- NA
gwas_ucsc$eqtl_gene <- NA
gwas_ucsc$eqtl_p <- NA

gwas_names <- c("MarkerName", "Chromosome", "Start", "Stop", "Intergenic", "GenicWith")
eqtl_names <- c("SNP", "Gen_ID", "P_Val", "Chromosome", "Start", "Stop")

for (i in 1:nrow(gwas_ucsc)) {
  gsnp_name <- gwas_ucsc$MarkerName[i]
  gsnp_coord <- gwas_ucsc$Start[i]
  gsnp_chrom <- gwas_ucsc$Chromosome[i]

  eligible_eqtl_snps <- subset(eqtl_ucsc, 
    (Chromosome == gsnp_chrom) & 
    (Start > gsnp_coord - scan_width_bps) &
    (Stop < gsnp_coord + scan_width_bps))

  if (nrow(eligible_eqtl_snps) > 0) {
    print(gwas_ucsc[i, gwas_names])
    print(eligible_eqtl_snps[, eqtl_names])
  }
}