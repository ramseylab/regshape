import sys
import pandas as pd

if len(sys.argv) < 1: 
	print "Usage: $0 ucsc_tss_metadata_file.txt"
	sys.exit(1)

inputFilename = sys.argv[1]

snps = pd.read_csv(inputFilename, sep='\t')
snps.columns

# assumes input comes in sorted by abs(tssDistance)
nearestTss = snps.dropna().groupby('name').head(1).reset_index(drop=True)

nearestTss[['name','tssDistance']].to_csv(inputFilename, header=True, index=False, sep='\t')


#fname="rsnps.tss_metadata.txt"; pd.read_csv(fname).to_csv(fname, header=True, index=False, sep='\t')