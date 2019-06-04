#!/usr/local/bin/bash -e

echo
echo "Deploying Application"
echo "---------------------"

if grep -Fxq "deploy" $history
then
   echo "Seems to be already deployed"
   echo "Skip!"
else


echo "Copying files"
cd /usr/local/interactive-cv
cp $script_dir/app-cron.sh cron.sh
cp $script_dir/weblet-bg.sh weblet.sh
chmod u+x *.sh

# crontab
echo "Patching /etc/crontab"
file=/etc/crontab
echo "" | sudo tee -a $file
echo "# Interactive CV (every night at 1:10)" | sudo tee -a $file
echo "10    1    *    *    *    root    /usr/local/interactive-cv/cron.sh" | sudo tee -a $file
echo "" | sudo tee -a $file
sudo rm -f $file-e

echo "Starting weblet"
./weblet.sh

echo "Updating data"
./cron.sh


echo "deploy" >> $history
echo "Done!"


fi


# ROLLBACK (remove 'deploy' from history file)
#   revert /etc/crontab
#   revert /var/db/neo4j/databases/graph.db
#   stop weblet
