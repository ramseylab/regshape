# This script does the following
# (1) subsets the GWAS data to rows meeting p-value threshold
# (2) Adds HG19 coordinates 
# (3) Subsets to intergenic SNPs after joining with ENSEMBL data

gene_buffer_bps <- 2000 # Number of base-pairs on either side of Gene to be considered within 'genic' region

# GWAS data, every SNP
setwd("~/Desktop/CAD-GWAS-Summer2015/data")
#fname = "CARDIoGRAM_GWAS_RESULTS.txt" # 2,420,360 rows
#   select=c("SNP", "reference_allele", "other_allele", "ref_allele_frequency", "pvalue"))

fname = "cardiogramplusc4d_data.txt" # 79128 rows
fname_cached = paste(fname,".RData",sep="")
if (file.exists(fname_cached)) {
  print("Loading Cached Data...")
  load(fname_cached)
} else {
  print("Loading Raw File...")
  GIANT <- read.table(fname, header=T, quote="\"")  
  save(GIANT, file=fname_cached)
}
nrow(GIANT) 

## Subset to GWAS SNPs p < gwas_threshold
gwas_threshold <- 1e-5 # ~937

gwas_eligible <- subset(GIANT, Fisher_Pvalue < gwas_threshold, 
  select=c("SNP_ID", "reference_allele", "other_allele", "eaf", "Fisher_Pvalue", "log_odds"))
nrow(gwas_eligible)

setwd("~/Desktop/CAD-GWAS-Summer2015/workspace")
write.table(gwas_eligible, file="gwas_eligible.tsv", sep="\t", row.names=F, col.names=F, quote=F)
rm(GIANT)






## Add HG19 coordinate data to gwas_eligible 
## time parallel --pipe --block 2M cut -f2-5 < snp138.txt | sed 's/chr//' > snp138.cut.txt
# EXTERNAL: Note: check if python-script recognizes column-order from cut/awk (no header used)
system("python ../src/python/add_hg19_coordinates.py gwas_eligible.tsv") # --> gwas_eligible.tsv.hg19.txt

# Postprocess/Cleanup
gwas_ucsc <- read.table("gwas_eligible.tsv.hg19.txt", sep="\t", header=F)
#GIANT_BMI_NAMES <- c("MarkerName", "Allele1", "Allele2", "Freq.Allele1.HapMapCEU", "p", "N") 
names(gwas_ucsc) <- c(names(gwas_eligible), "Chromosome", "Start", "Stop", "MarkerName")
head(gwas_ucsc)
summary(gwas_ucsc)
nrow(gwas_ucsc)






## Add Genic/Intergenic metadata
ensembl <- read.delim("../data/genes_and_coordinates_GRCh37_hg19.txt")
head(ensembl)
summary(ensembl)
nrow(ensembl) # 64102
ensembl$Chromosome.Name <- as.integer(as.character(ensembl$Chromosome.Name))
ensembl$Chromosome.Name[is.na(ensembl$Chromosome.Name)] <- 0 # Some chromosome #s are not ints (!)

gwas_ucsc$Intergenic <- TRUE
gwas_ucsc$GenicWith <- NA
for (i in 1:nrow(gwas_ucsc)) {
  snp_coord = gwas_ucsc$Start[i] + 1 #Note: UCSC vs. ensembl off-by-1     
  snp_chrom = gwas_ucsc$Chromosome[i]
  #print(i)
  for (j in 1:nrow(ensembl)) {
    chrom <- ensembl$Chromosome.Name[j]
    llimit <- ensembl$Gene.Start..bp.[j] - gene_buffer_bps
    rlimit <- ensembl$Gene.End..bp.[j] + gene_buffer_bps    
    #print(paste(i,j))    
    if ( (snp_chrom == chrom) & (snp_coord >= llimit) & (snp_coord <= rlimit) ) {
      print(paste(i, j, llimit, snp_coord, snp_chrom, rlimit, ensembl$Ensembl.Gene.ID[j], gwas_ucsc$MarkerName[i]))
      gwas_ucsc$Intergenic[i] <- FALSE
      gwas_ucsc$GenicWith[i] <- as.character(ensembl$Ensembl.Gene.ID[j])
      break
    }
  }  
}  

summary(gwas_ucsc)
write.csv(gwas_ucsc, file="gwas_ucsc_genic.csv")
#View(gwas_ucsc)
#rm(ensembl)




## NEXT STEP: Get all Proxy SNPs from SNAP Proxy Search
## https://www.broadinstitute.org/mpg/snap/ldsearch.php




