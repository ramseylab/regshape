#!/bin/bash

# ------------------------------- #
# prerequisites                   #
# ------------------------------- #
# sudo apt-get install cifs.utils #
# sudo apt-get install ssmtp      #
# sudo apt-get install mailutils  #
# ------------------------------- #


# ----------------------------------- #
# properties                          #
# ----------------------------------- #
  backup_dir_name=backups2
  backup_subdir_name=linux.backup
  backup_password=XXXXXXX
  backup_username=backups
  backup_host_ip=128.193.214.204
  # get the hostname of this computer
  myhostname=$(hostname)
# ----------------------------------- #


# ------------------------------------------------------------------------ #
# global variables                                                         #
# ------------------------------------------------------------------------ #
  # notification list
  mailtoaddr=('johndoe@foo.bar' 'janedoe@baz.qux')

  log="$(date +'%r, %m/%d/%Y'), @${myhostname}: backup script started."
# ------------------------------------------------------------------------ #


# ------------------------------------------------------------------------------ #
# util functions                                                                 #
# ------------------------------------------------------------------------------ #
  function sendSuccessNotification {
	for addr in "${mailtoaddr[@]}"; do
		echo -e "Ubuntu ${myhostname} backup succeeded on $(date +'%r, %m/%d/%Y') \n\n$1" | mail -s "Ubuntu Backup Succeeded." -a "From: ramseylab.bot <ramseylab.bot@gmail.com>" ${addr}
	done
  }

  function sendFailureNotification {
	for addr in "${mailtoaddr[@]}"; do
		echo -e "Ubuntu ${myhostname} backup failed on $(date +'%r, %m/%d/%Y') \n\n$1" | mail -s "Ubuntu Backup Failed." -a "From: ramseylab.bot <ramseylab.bot@gmail.com>" ${addr}
	done
  }

  function showMsgAndKeepLog {
	msg="$(date +'%r, %m/%d/%Y'), @${myhostname}: $1"
	echo $msg
	log="${log} \n${msg}"
  }

  function inspect {
	msg="$(date +'%r, %m/%d/%Y'), @${myhostname}: $1 exited with code $2."
	echo $msg
	log="${log} \n${msg}"
	
	if [ $2 -ne 0 ]
	then
		umount /mnt
		sendFailureNotification "${log}"
		exit $2
	fi
  }
# ------------------------------------------------------------------------------ #


# STEP 1: mount the network directory for backups

/sbin/mount.cifs //${backup_host_ip}/${backup_dir_name}/${backup_subdir_name} /mnt -o username=${backup_username},password=${backup_password},nounix,sec=ntlmssp,noperm,rw
inspect "mount.cifs" $?

# STEP 2: if a backup file already exists, move it to new filename with suffix "-temp.tar.gz"

if [ -e "/mnt/${myhostname}-backup.tar.gz" ] 
then
	showMsgAndKeepLog "/mnt/${myhostname}-backup.tar.gz detected."
    	mv /mnt/${myhostname}-backup.tar.gz /mnt/${myhostname}-backup-temp.tar.gz
	inspect "mv" $?
else
	showMsgAndKeepLog "/mnt/${myhostname}-backup.tar.gz not detected."
fi

# STEP 3: make the backup archive

tar -cpzf /mnt/${myhostname}-backup.tar.gz --one-file-system /home /opt /usr/local
inspect "tar" $?

# STEP 4: delete the temporary file (if any)

if [ -e "/mnt/${myhostname}-backup-temp.tar.gz" ] 
then
	rm -f /mnt/${myhostname}-backup-temp.tar.gz
	inspect "rm" $?
fi

# STEP 5: unmount the backup network directory

/bin/umount /mnt
inspect "umount" $?

# STEP 6: send notifications

sendSuccessNotification "${log}"

