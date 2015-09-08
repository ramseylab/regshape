snps = pd.read_csv("ucsc_csnps.txt", sep='\t')
snps.columns

topNsnps = snps.groupby('rSNPName').head(3).reset_index(drop=True)

topNsnpsUnique = topNsnps['cSNPName'].unique()

# topNsnps.to_csv("csnps.rslist.txt", columns=['cSNPName'], header=False, index=False)
pd.DataFrame(topNsnpsUnique).to_csv("csnps.rslist.txt", header=False, index=False)
