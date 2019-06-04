#!/usr/local/bin/bash -e

echo
echo "Installing Neo4j 3.3.2"
echo "----------------------"

if grep -Fxq "neo4j" $history
then
   echo "Seems to be already installed"
   echo "Skip!"
else


sudo pkg install -y neo4j-3.3.2

# rc.conf
echo "Patching /etc/rc.conf"
file=/etc/rc.conf
echo "" | sudo tee -a $file
echo '# enable Neo4j' | sudo tee -a $file
echo 'neo4j_enable="YES"' | sudo tee -a $file
echo "" | sudo tee -a $file
sudo rm -f $file-e

# neo4j.conf
echo "Patching /usr/local/etc/neo4j.conf"
file=/usr/local/etc/neo4j.conf
sudo sed -i -e "s;#dbms.security.auth_enabled=false;dbms.security.auth_enabled=false;g" $file
sudo sed -i -e "s;#dbms.read_only=false;dbms.read_only=true;g" $file
sudo sed -i -e "s;#dbms.allow_upgrade=true;dbms.allow_upgrade=true;g" $file
sudo sed -i -e "s;#dbms.connectors.default_listen_address=0.0.0.0;dbms.connectors.default_listen_address=0.0.0.0;g" $file
sudo rm -f $file-e

echo "Starting the database"
sudo service neo4j start
sleep 5


echo "neo4j" >> $history
echo "Done!"


fi


# ROLLBACK (remove 'neo4j' from history file)
#   sudo pkg remove -y neo4j-3.3.2
#   sudo rm -rf /var/db/neo4j
#   sudo rm -rf /usr/local/etc/neo4j.conf
#   sudo rm -rf /usr/local/etc/neo4j-certificates
#   revert /etc/rc.conf
