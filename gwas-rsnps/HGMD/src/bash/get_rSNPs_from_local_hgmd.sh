cd ${PROJECT_HOME}/workspace

mysql -uroot -e "SELECT * FROM allmut WHERE base IN ('R')" hgmd_pro > hgmd_rsnps.txt

cut -f15 hgmd_rsnps.txt | grep 'rs' > rsnps.rslist.txt