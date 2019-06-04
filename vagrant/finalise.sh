#!/usr/local/bin/bash -e

echo
echo "Finalising"
echo "----------"

if grep -Fxq "finalise" $history
then
   echo "Seems to be already finalised"
   echo "Skip!"
else


echo "Script to start on boot"
sudo cp $script_dir/my.services /etc/rc.d
sudo chmod ugo+rx /etc/rc.d/my.services

echo "Restarting cron"
sudo /etc/rc.d/cron restart

echo -n "Waiting for services to be up [."
wait_port () {
  fed=0
  while [ $fed -lt 1 ]
  do
    if [ "$(netstat -a | grep LISTEN | grep $port)" ]; then
      fed=1
    else
      echo -n "."
      sleep 1
    fi
  done
}
port=7687 && wait_port
port=7474 && wait_port
port=8082 && wait_port
echo "....]"


echo "finalise" >> $history
echo "Done!"


fi


# ROLLBACK (remove 'finalise' from history file)
#   sudo rm /etc/rc.d/my.services
