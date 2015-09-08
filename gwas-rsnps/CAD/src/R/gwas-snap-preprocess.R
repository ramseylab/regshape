## Process online SNAP-tool output to use in the UCSC SQL script

# Download file with warnings suppressed
snap <- read.delim("SNAPResults.rsq09.txt")
head(snap)
snap$Distance <- as.integer(as.character( snap$Distance ))

summary(snap)
nrow(snap) 

# snap <- subset(snap, Distance < 10000) 
# nrow(snap) 

length(unique(snap$Proxy)) 
snap <- subset(snap, select=c("Proxy"))
snap <- unique(snap)

write.table(snap, file="SNAPResults.EligibleProxySNPs.txt", sep="\t", row.names=F, col.names=F, quote=F)

#system("python add_hg19_coordinates.py intergenic-snap-preprocessed.tsv") # --> [].hg19.txt
## NEXT STEPs: 
##  Get Metadata for all of these SNPs via the UCSC SQL script
##  Subset in Excel to the most interesting SNPs



# regulomeDB <- read.table("../data/RegulomeDB.dbSNP132.Category1.txt", header=F, sep='\t')
# names(regulomeDB) <- c("chr", "coord", "snp", "metadata", "score")
# regulomeDB <- subset(regulomeDB, select=c("snp", "score"))

regulomeDB <- read.table("../data/rDB141.1x.txt", header=F, sep='\t')
names(regulomeDB) <- c("snp")

# regulomeDB$snp <- as.character(regulomeDB$snp)
# snap$Proxy <- as.character(snap$Proxy)
merged <- merge(regulomeDB, snap, by.x="snp", by.y="Proxy")
write.table(merged, file="SNAP.RegulomeDB1x.NonCoding.txt", sep="\t", row.names=F, col.names=F, quote=F)
