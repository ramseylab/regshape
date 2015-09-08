sqldf("SELECT * FROM snps LIMIT 10")

sqldf("SELECT *, ROW_NUMBER() FROM snps LIMIT 10")

snps <- read.delim("ucsc_csnps.txt")
snps$rank <- 1
for (r in 2:nrow(snps)) {
	snps$rank[r] <- ifelse(snps$rSNPName[r] == snps$rSNPName[r-1], snps$rank[r-1] + 1, 0)
}


