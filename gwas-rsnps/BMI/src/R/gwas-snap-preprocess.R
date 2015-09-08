# Download file with warnings suppressed
snap <- read.delim("intergenic-snps-SNAPResults.txt")
head(snap)
summary(snap)
nrow(snap) #31395

snap <- subset(snap, Distance < 10000) 
nrow(snap) #11875
length(unique(snap$Proxy)) # 1932

snap <- subset(snap, select=c("Proxy"))
snap <- unique(snap)

write.table(snap, file="intergenic-snap-preprocessed.tsv", sep="\t", row.names=F, col.names=F, quote=F)

system("python add_hg19_coordinates.py intergenic-snap-preprocessed.tsv") # --> [].hg19.txt
