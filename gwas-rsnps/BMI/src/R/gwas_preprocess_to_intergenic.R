#setwd("~/Desktop/BMI-GWAS-SummerProject")
gene_buffer_bps <- 2000 # Number of base-pairs on either side of Gene to be considered within 'genic' region

# GWAS data, every SNP
fname = "GIANT_BMI_Speliotes2010_publicrelease_HapMapCeuFreq.txt"
fname_cached = paste(fname,".RData",sep="")
if (file.exists(fname_cached)) {
  print("Loading Cached Data...")
  load(fname_cached)
} else {
  print("Loading Raw File...")
  GIANT <- read.table(fname, header=T, quote="\"")  
  save(GIANT, file=fname_cached)
}
nrow(GIANT) # 2,471,516

## Subset to GWAS SNPs p < gwas_threshold
#gwas_threshold <- 5e-8 # ~800 SNPs
gwas_threshold <- 1e-6 # ~1200 SNPs
#gwas_threshold <- 1e-1 # ~276K SNPs
#gwas_threshold <- 1e-2 # ~43K SNPs
#gwas_threshold <- 1e-3 # ~10K SNPs

gwas_eligible <- subset(GIANT, p < gwas_threshold)
rm(GIANT)
nrow(gwas_eligible)
write.table(gwas_eligible, file="gwas_eligible.tsv", sep="\t", row.names=F, col.names=F, quote=F)

## Add coordinate data to gwas_eligible (output: gwas_ucsc.txt) 
## time parallel --pipe --block 2M cut -f2-5 < snp138.txt | sed 's/chr//' > snp138.cut.txt
# EXTERNAL: Note: check if python-script recognizes column-order from cut/awk (no header used)
system("python add_hg19_coordinates.py gwas_eligible.tsv") # --> gwas_eligible.tsv.hg19.txt

gwas_ucsc <- read.table("gwas_eligible.tsv.hg19.txt", sep="\t", header=F)
names(gwas_ucsc) <- c(names(gwas_eligible), "Chromosome", "Start", "Stop", "MarkerName")
head(gwas_ucsc)
summary(gwas_ucsc)
nrow(gwas_ucsc)
gwas_ucsc$Chromosome <- as.integer(as.character(gwas_ucsc$Chromosome))
gwas_ucsc$Chromosome[is.na(gwas_ucsc$Chromosome)] <- 0 # Some chromosome #s are not ints (!)

## Add Genic/Intergenic metadata
ensembl <- read.delim("genes_and_coordinates_GRCh37_hg19.txt")
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
rm(ensembl)


