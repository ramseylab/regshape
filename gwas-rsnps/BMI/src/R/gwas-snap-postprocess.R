# Download file with warnings suppressed
snap <- read.delim("intergenic-snps-SNAPResults.txt")
snap <- subset(snap, Distance < 10000) 
# > names(snap)
# "SNP"      "Proxy"    "Distance" "RSquared"
head(snap)
summary(snap)
nrow(snap) #31395


# Select closest Proxy per GWAS-SNP
osnap <- snap[ order(snap$Proxy, snap$Distance), ]
osnap <- aggregate(osnap, list(osnap$Proxy), FUN=head, n=1)
osnap <- subset(osnap, select=c("SNP", "Proxy", "Distance", "RSquared"))
head(osnap, n=25)


# Add P-value metadata back to GWAS-SNP
# Get p-values from original dataset 
gwas_ucsc <- read.table("gwas_eligible.tsv", sep="\t", header=F)
names(gwas_ucsc) <- c("SNP", "Allele1", "Allele2", "AlleleFrequency", "p", "N")
gwas_ucsc <- subset(gwas_ucsc, select=c("SNP", "AlleleFrequency", "p"))
head(gwas_ucsc)

# Merge with Proxy <--> GWAS-SNP Look Up Table to get 
# Look Up Table (GWAS-SNP, P-Val, Proxy-SNP, RSqr, Distance)
lut <- merge(gwas_ucsc, osnap)
nrow(gwas_ucsc)
nrow(osnap)
nrow(lut)
summary(lut)


# Load & Filter UCSC-Genome-Browser SQL-Query results (Proxy-SNPs should overlap TF & HypersensitiveSite)
#sqlout <- read.csv("GWAS-Hypersensitive-TF.csv")
sqlout <- read.csv("GWAS-Hypersensitive-TF-PlacentalCons.csv")
head(sqlout)
sqlout <- subset(sqlout, as.character(name.1) != "NULL" & 
						 as.character(name.2) != "NULL" &
						 as.character(name.2) != "POLR2A")
summary(sqlout)

# Merge LUT with UCSC-Genome-Browser SQL-Query results on Proxy-SNP 
lut$Proxy <- as.character(lut$Proxy)
sqlout$name <- as.character(sqlout$name)
final <- merge(lut, sqlout, by.x="Proxy", by.y="name")
head(final)
write.table(final, file="GWAS-Hypersensitive-TF-PlacentalCons.WithMetadata.txt", sep="\t", row.names=F)