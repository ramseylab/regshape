SNP_THRESHOLD=0.05

isRare <- function(afs) { 
	# afs = UCSC alleleFreqs data e.g. "0.389077,0.110923,0.110923,0.389077,"
	#print(afs)
	afsv <- as.numeric( unlist( strsplit(afs, ',') ))	
	if (length(afsv) == 4) {
		afsv <- afsv[1:2]*2
	}
	if (length(afsv) != 2) {
	  return(NA)
	  #return(paste(length(afsv),"-allelic!",sep=""))
	}
	minorAF <- if (afsv[1] < afsv[2]) afsv[1] else afsv[2]
	return( minorAF < SNP_THRESHOLD)
}


# Load HGMD data
ucsc <- read.delim("rsnps.ucsc_metadata.txt")
nrow(ucsc) # 2708

# Preprocess some columns
ucsc$alleleFreqs <- as.character( ucsc$alleleFreqs )
ucsc$num_tfs <- as.numeric( as.character( ucsc$num_tfs ))
ucsc$num_tfs[is.na(ucsc$num_tfs)] <- 0

ucsc$DHS_Name <- as.numeric(as.character(ucsc$DHS_Name))
ucsc$DHS <- !is.na(ucsc$DHS_Name)

ucsc$phastCons <- as.numeric(as.character(ucsc$phastCons))
#ucsc$phastCons[is.na(ucsc$phastCons)] <- 0

# Add Rare annotation
ucsc$isRare <- unlist(lapply(ucsc$alleleFreqs, FUN=isRare))

# Summary Stats
by(ucsc[, c("DHS", "num_tfs", "phastCons")], ucsc[,"isRare"], summary)

summary(ucsc)
