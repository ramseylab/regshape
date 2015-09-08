# Get rSNPs from HGMD
#  Exclude cancer-associated
#  Ignore ones with no dbSNP rs-IDs for now
#  Ignore existence/non-existence of MAF/Allele information for now

mysql -uroot -e "SELECT * FROM hgmd_pro.allmut 
				  WHERE base IN ('R') 
				   AND disease NOT LIKE '%cancer%'
				   AND dbSNP IS NOT NULL" hgmd_pro > hgmd_rsnps.txt

cut -f15 hgmd_rsnps.txt | grep 'rs' > rsnps.rslist.txt # grep rs to remove header/blanks


