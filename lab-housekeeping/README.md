# Prerequisites

`cifs.utils` is needed for samba mounting; notifications are sent via `ssmtp` and `mailutils`

```
sudo apt-get install cifs.utils
sudo apt-get install ssmtp
sudo apt-get install mailutils
```

# Ssmtp Configuration

A gmail account `ramseylab.bot@gmail.com`, whose password is the same with the one used to access `128.193.214.204` shared folders, is already applied in the configuration file `ssmtp.conf`.

Please fill the `AuthPass` property with the gmail password mentioned above in `ssmtp.conf` and put this conf file under the folder `/etc/ssmtp` to .

# Bash Script Configuration

Please override the `backup_password` property in the bash script with the password used to access `128.193.214.204` shared folders. If the password contains special characters (e.g. a exclamation mark), wrap it with single quotes.

The `mailtoaddr` property indicates the email addresses that will receive the notifications when the backup is finished. Please override it appropriately.

# Crontab Configuration

Type `sudo crontab -e` in console to add a cron task as a root user like below:

```
# minute (0-59), hour (0-23, 0 = midnight), day (1-31), month (1-12), weekday (0-6, 0 = Sunday)
0 0 * * * /bin/bash ~/backup_to_macmini.sh
```

Crontab may fail to find `bash` in PATH (extra configuration may be required.), so use `/bin/bash` for the command line (check `whereis bash`). 
